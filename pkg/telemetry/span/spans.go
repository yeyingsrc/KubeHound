package span

import (
	"context"

	"github.com/DataDog/KubeHound/pkg/telemetry/log"
	"github.com/DataDog/KubeHound/pkg/telemetry/tag"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace"
	"gopkg.in/DataDog/dd-trace-go.v1/ddtrace/tracer"
)

// Top level spans
const (
	IngestData = "kubehound.ingestData"
	BuildGraph = "kubehound.buildGraph"
	Launch     = "kubehound.launch"
)

// JanusGraph provider spans
const (
	JanusGraphFlush      = "kubehound.janusgraph.flush"
	JanusGraphBatchWrite = "kubehound.janusgraph.batchwrite"
)

// MongoDB provider spans
const (
	MongoDBFlush      = "kubehound.mongo.flush"
	MongoDBBatchWrite = "kubehound.mongo.batchwrite"
)

// Collector/dumper component spans
const (
	CollectorStream = "kubehound.collector.stream"
	CollectorDump   = "kubehound.collector.dump"

	IngestorLaunch      = "kubehound.ingestor.launch"
	IngestorStartJob    = "kubehound.ingestor.startjob"
	IngestorBlobPull    = "kubehound.ingestor.blob.pull"
	IngestorBlobPut     = "kubehound.ingestor.blob.put"
	IngestorBlobExtract = "kubehound.ingestor.blob.extract"
	IngestorBlobClose   = "kubehound.ingestor.blob.close"
	IngestorClean       = "kubehound.ingestor.clean"

	DumperLaunch = "kubehound.dumper.launch"

	DumperNodes               = "kubehound.dumper.nodes"
	DumperPods                = "kubehound.dumper.pods"
	DumperEndpoints           = "kubehound.dumper.endpoints"
	DumperRoles               = "kubehound.dumper.roles"
	DumperClusterRoles        = "kubehound.dumper.clusterroles"
	DumperRoleBindings        = "kubehound.dumper.rolebindings"
	DumperClusterRoleBindings = "kubehound.dumper.clusterrolebindings"
	DumperMetadata            = "kubehound.dumper.metadata"

	DumperReadFile    = "kubehound.dumper.readFile"
	DumperS3Push      = "kubehound.dumper.s3_push"
	DumperS3Download  = "kubehound.dumper.s3_download"
	DumperWriterWrite = "kubehound.dumper.write"
	DumperWriterFlush = "kubehound.dumper.flush"
	DumperWriterClose = "kubehound.dumper.close"
)

// Graph builder spans
const (
	BuildEdge = "kubehound.graph.builder.edge"
)

func convertTag(value any) string {
	val, err := value.(string)
	if !err {
		return ""
	}

	return val
}

func StartSpanFromContext(runCtx context.Context, operationName string, opts ...tracer.StartSpanOption) (tracer.Span, context.Context) {
	spanJob, runCtx := tracer.StartSpanFromContext(runCtx, operationName, opts...)
	spanIngestRunSetDefaultTag(runCtx, spanJob)

	return spanJob, runCtx
}

func SpanRunFromContext(runCtx context.Context, spanName string) (ddtrace.Span, context.Context) {
	spanJob, runCtx := tracer.StartSpanFromContext(runCtx, spanName, tracer.ResourceName(convertTag(runCtx.Value(log.ContextFieldCluster))), tracer.Measured())
	spanIngestRunSetDefaultTag(runCtx, spanJob)

	return spanJob, runCtx
}

func spanIngestRunSetDefaultTag(ctx context.Context, span ddtrace.Span) {
	span.SetTag(tag.CollectorClusterTag, convertTag(ctx.Value(log.ContextFieldCluster)))
	span.SetTag(tag.RunIdTag, convertTag(ctx.Value(log.ContextFieldRunID)))
}
