package edge

import (
	"context"
	"fmt"

	"github.com/DataDog/KubeHound/pkg/kubehound/graph/adapter"
	"github.com/DataDog/KubeHound/pkg/kubehound/graph/types"
	"github.com/DataDog/KubeHound/pkg/kubehound/models/converter"
	"github.com/DataDog/KubeHound/pkg/kubehound/storage/cache"
	"github.com/DataDog/KubeHound/pkg/kubehound/storage/storedb"
	"github.com/DataDog/KubeHound/pkg/kubehound/store/collections"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

func init() {
	Register(&TokenBruteforceNamespace{}, RegisterDefault)
}

type TokenBruteforceNamespace struct {
	BaseEdge
}

type tokenBruteforceNSGroup struct {
	Role     primitive.ObjectID `bson:"_id" json:"role"`
	Identity primitive.ObjectID `bson:"identity" json:"identity"`
}

func (e *TokenBruteforceNamespace) Label() string {
	return "TOKEN_BRUTEFORCE"
}

func (e *TokenBruteforceNamespace) Name() string {
	return "TokenBruteforceNamespace"
}

func (e *TokenBruteforceNamespace) AttckTechniqueID() AttckTechniqueID {
	return AttckTechniqueStealApplicationAccessTokens
}

func (e *TokenBruteforceNamespace) AttckTacticID() AttckTacticID {
	return AttckTacticCredentialAccess
}

func (e *TokenBruteforceNamespace) Processor(ctx context.Context, oic *converter.ObjectIDConverter, entry any) (any, error) {
	typed, ok := entry.(*tokenBruteforceNSGroup)
	if !ok {
		return nil, fmt.Errorf("invalid type passed to processor: %T", entry)
	}

	return adapter.GremlinEdgeProcessor(ctx, oic, e.Label(), typed.Role, typed.Identity, map[string]any{
		"attckTechniqueID": string(e.AttckTechniqueID()),
		"attckTacticID":    string(e.AttckTacticID()),
	})
}

// Stream finds all roles that are namespaced and have secrets/get or equivalent wildcard permissions and matching identities.
// Matching identities are defined as namespaced identities that share the role namespace or non-namespaced identities.
func (e *TokenBruteforceNamespace) Stream(ctx context.Context, store storedb.Provider, _ cache.CacheReader,
	callback types.ProcessEntryCallback, complete types.CompleteQueryCallback) error {

	var verbMatcher bson.M
	if e.cfg.LargeClusterOptimizations {
		// For large clusters do not create a redundant edge already covered by the TOKEN_LIST attack as this technique is much more complex
		verbMatcher = bson.M{"verbs": "get"}
	} else {
		verbMatcher = bson.M{"$or": bson.A{
			bson.M{"verbs": "get"},
			bson.M{"verbs": "*"},
		}}
	}

	permissionSets := adapter.MongoDB(ctx, store).Collection(collections.PermissionSetName)
	pipeline := []bson.M{
		{
			"$match": bson.M{
				"is_namespaced":   true,
				"runtime.runID":   e.runtime.RunID.String(),
				"runtime.cluster": e.runtime.ClusterName,
				"rules": bson.M{
					"$elemMatch": bson.M{
						"$and": bson.A{
							bson.M{"$or": bson.A{
								bson.M{"apigroups": ""},
								bson.M{"apigroups": "*"},
							}},
							bson.M{"$or": bson.A{
								bson.M{"resources": "secrets"},
								bson.M{"resources": "*"},
							}},
							verbMatcher,
							bson.M{"resourcenames": nil}, // TODO: handle resource scope
						},
					},
				},
			},
		},
		{
			"$lookup": bson.M{
				"as":   "idsInNamespace",
				"from": "identities",
				"let": bson.M{
					"roleNamespace": "$namespace",
				},
				"pipeline": []bson.M{
					{
						"$match": bson.M{
							"$and": bson.A{
								bson.M{"$or": bson.A{
									bson.M{"$expr": bson.M{
										"$eq": bson.A{
											"$namespace", "$$roleNamespace",
										},
									}},
									bson.M{"is_namespaced": false},
								}},
								bson.M{"type": "ServiceAccount"},
							},
							"runtime.runID":   e.runtime.RunID.String(),
							"runtime.cluster": e.runtime.ClusterName,
						},
					},
					{
						"$project": bson.M{
							"_id": 1,
						},
					},
				},
			},
		},
		{
			"$unwind": "$idsInNamespace",
		},
		{
			"$project": bson.M{
				"_id":      1,
				"identity": "$idsInNamespace._id",
			},
		},
	}

	cur, err := permissionSets.Aggregate(ctx, pipeline)
	if err != nil {
		return err
	}
	defer cur.Close(ctx)

	return adapter.MongoCursorHandler[tokenBruteforceNSGroup](ctx, cur, callback, complete)
}
