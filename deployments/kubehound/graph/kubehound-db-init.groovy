:remote connect tinkerpop.server conf/remote.yaml session
:remote console
:remote config timeout none

//
// Graph schema and index definition for the KubeHound graph mode
// See details of the janus graph APIs here https://docs.janusgraph.org/schema/
//

graph.tx().rollback()
mgmt = graph.openManagement();

System.out.println("[KUBEHOUND] Creating graph schema and indexes");

// Create our vertex labels
container = mgmt.makeVertexLabel('Container').make();
identity = mgmt.makeVertexLabel('Identity').make();
node = mgmt.makeVertexLabel('Node').make();
pod = mgmt.makeVertexLabel('Pod').make();
permissionSet = mgmt.makeVertexLabel('PermissionSet').make();
volume = mgmt.makeVertexLabel('Volume').make();
endpoint = mgmt.makeVertexLabel('Endpoint').make();

// Create our edge labels and connections
permissionDiscover = mgmt.makeEdgeLabel('PERMISSION_DISCOVER').multiplicity(MULTI).make();
mgmt.addConnection(permissionDiscover, identity, permissionSet);

volumeDiscover = mgmt.makeEdgeLabel('VOLUME_DISCOVER').multiplicity(MULTI).make();
mgmt.addConnection(volumeDiscover, container, volume);

volumeAccess = mgmt.makeEdgeLabel('VOLUME_ACCESS').multiplicity(MULTI).make();
mgmt.addConnection(volumeAccess, node, volume);

hostWrite = mgmt.makeEdgeLabel('EXPLOIT_HOST_WRITE').multiplicity(MULTI).make();
mgmt.addConnection(hostWrite, volume, node);

hostRead = mgmt.makeEdgeLabel('EXPLOIT_HOST_READ').multiplicity(MULTI).make();
mgmt.addConnection(hostRead, volume, node);

hostTraverse = mgmt.makeEdgeLabel('EXPLOIT_HOST_TRAVERSE').multiplicity(MULTI).make();
mgmt.addConnection(hostTraverse, volume, volume);

sharedPsNamespace = mgmt.makeEdgeLabel('SHARE_PS_NAMESPACE').multiplicity(MULTI).make();
mgmt.addConnection(sharedPsNamespace, container, container);

containerAttach = mgmt.makeEdgeLabel('CONTAINER_ATTACH').multiplicity(ONE2MANY).make();
mgmt.addConnection(containerAttach, pod, container);

idAssume = mgmt.makeEdgeLabel('IDENTITY_ASSUME').multiplicity(MANY2ONE).make();
mgmt.addConnection(idAssume, container, identity);
mgmt.addConnection(idAssume, node, identity);

idImpersonate = mgmt.makeEdgeLabel('IDENTITY_IMPERSONATE').multiplicity(MANY2ONE).make();
mgmt.addConnection(idImpersonate, permissionSet, identity);

roleBind = mgmt.makeEdgeLabel('ROLE_BIND').multiplicity(MULTI).make();
mgmt.addConnection(roleBind, permissionSet, permissionSet);

podAttach = mgmt.makeEdgeLabel('POD_ATTACH').multiplicity(ONE2MANY).make();
mgmt.addConnection(podAttach, node, pod);


podCreate = mgmt.makeEdgeLabel('POD_CREATE').multiplicity(MULTI).make();
mgmt.addConnection(podCreate, permissionSet, node);
mgmt.addConnection(podCreate, permissionSet, permissionSet); // self-referencing for large cluster optimizations

podPatch = mgmt.makeEdgeLabel('POD_PATCH').multiplicity(MULTI).make();
mgmt.addConnection(podPatch, permissionSet, pod);
mgmt.addConnection(podPatch, permissionSet, permissionSet); // self-referencing for large cluster optimizations

podExec = mgmt.makeEdgeLabel('POD_EXEC').multiplicity(MULTI).make();
mgmt.addConnection(podExec, permissionSet, pod);
mgmt.addConnection(podExec, permissionSet, permissionSet); // self-referencing for large cluster optimizations

tokenSteal = mgmt.makeEdgeLabel('TOKEN_STEAL').multiplicity(MULTI).make();
mgmt.addConnection(tokenSteal, volume, identity);

tokenBruteforce = mgmt.makeEdgeLabel('TOKEN_BRUTEFORCE').multiplicity(MULTI).make();
mgmt.addConnection(tokenBruteforce, permissionSet, identity);

tokenList = mgmt.makeEdgeLabel('TOKEN_LIST').multiplicity(MULTI).make();
mgmt.addConnection(tokenList, permissionSet, identity);

nsenter = mgmt.makeEdgeLabel('CE_NSENTER').multiplicity(MANY2ONE).make();
mgmt.addConnection(nsenter, container, node);

moduleLoad = mgmt.makeEdgeLabel('CE_MODULE_LOAD').multiplicity(MANY2ONE).make();
mgmt.addConnection(moduleLoad, container, node);

umhCorePattern = mgmt.makeEdgeLabel('CE_UMH_CORE_PATTERN').multiplicity(MANY2ONE).make();
mgmt.addConnection(umhCorePattern, container, node);

privMount = mgmt.makeEdgeLabel('CE_PRIV_MOUNT').multiplicity(MANY2ONE).make();
mgmt.addConnection(privMount, container, node);

sysPtrace = mgmt.makeEdgeLabel('CE_SYS_PTRACE').multiplicity(MANY2ONE).make();
mgmt.addConnection(sysPtrace, container, node);

varLogSymLink = mgmt.makeEdgeLabel('CE_VAR_LOG_SYMLINK').multiplicity(MULTI).make();
mgmt.addConnection(varLogSymLink, container, node);

endpointExploit = mgmt.makeEdgeLabel('ENDPOINT_EXPLOIT').multiplicity(MULTI).make();
mgmt.addConnection(endpointExploit, endpoint, container);

// All properties we will index on
cls = mgmt.makePropertyKey('class').dataType(String.class).cardinality(Cardinality.SINGLE).make();
cluster = mgmt.makePropertyKey('cluster').dataType(String.class).cardinality(Cardinality.SINGLE).make();
runID = mgmt.makePropertyKey('runID').dataType(String.class).cardinality(Cardinality.SINGLE).make();
storeID = mgmt.makePropertyKey('storeID').dataType(String.class).cardinality(Cardinality.SINGLE).make();
app = mgmt.makePropertyKey('app').dataType(String.class).cardinality(Cardinality.SINGLE).make();
team = mgmt.makePropertyKey('team').dataType(String.class).cardinality(Cardinality.SINGLE).make();
service = mgmt.makePropertyKey('service').dataType(String.class).cardinality(Cardinality.SINGLE).make();
name = mgmt.makePropertyKey('name').dataType(String.class).cardinality(Cardinality.SINGLE).make();
namespace = mgmt.makePropertyKey('namespace').dataType(String.class).cardinality(Cardinality.SINGLE).make();
type = mgmt.makePropertyKey('type').dataType(String.class).cardinality(Cardinality.SINGLE).make();
critical = mgmt.makePropertyKey('critical').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
port = mgmt.makePropertyKey('port').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
portName = mgmt.makePropertyKey('portName').dataType(String.class).cardinality(Cardinality.SINGLE).make();
serviceEndpoint = mgmt.makePropertyKey('serviceEndpoint').dataType(String.class).cardinality(Cardinality.SINGLE).make();
serviceDns = mgmt.makePropertyKey('serviceDns').dataType(String.class).cardinality(Cardinality.SINGLE).make();
exposure = mgmt.makePropertyKey('exposure').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();

// All properties that we want to be able to search on
isNamespaced = mgmt.makePropertyKey('isNamespaced').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
compromised = mgmt.makePropertyKey('compromised').dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
sourcePath = mgmt.makePropertyKey('sourcePath').dataType(String.class).cardinality(Cardinality.SINGLE).make();
mountPath = mgmt.makePropertyKey('mountPath').dataType(String.class).cardinality(Cardinality.SINGLE).make();
readonly = mgmt.makePropertyKey('readonly').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
nodeName = mgmt.makePropertyKey('node').dataType(String.class).cardinality(Cardinality.SINGLE).make();
sharedPs = mgmt.makePropertyKey('shareProcessNamespace').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
serviceAccount = mgmt.makePropertyKey('serviceAccount').dataType(String.class).cardinality(Cardinality.SINGLE).make();
image = mgmt.makePropertyKey('image').dataType(String.class).cardinality(Cardinality.SINGLE).make();
podName = mgmt.makePropertyKey('pod').dataType(String.class).cardinality(Cardinality.SINGLE).make();
hostNetwork = mgmt.makePropertyKey('hostNetwork').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
hostPid = mgmt.makePropertyKey('hostPid').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
hostIpc = mgmt.makePropertyKey('hostIpc').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
privesc = mgmt.makePropertyKey('privesc').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
privileged = mgmt.makePropertyKey('privileged').dataType(Boolean.class).cardinality(Cardinality.SINGLE).make();
runAsUser = mgmt.makePropertyKey('runAsUser').dataType(Long.class).cardinality(Cardinality.SINGLE).make();
rules = mgmt.makePropertyKey('rules').dataType(String.class).cardinality(Cardinality.LIST).make();
command = mgmt.makePropertyKey('command').dataType(String.class).cardinality(Cardinality.LIST).make();
args = mgmt.makePropertyKey('args').dataType(String.class).cardinality(Cardinality.LIST).make();
capabilities = mgmt.makePropertyKey('capabilities').dataType(String.class).cardinality(Cardinality.LIST).make();
ports = mgmt.makePropertyKey('ports').dataType(String.class).cardinality(Cardinality.LIST).make();
identityName = mgmt.makePropertyKey('identity').dataType(String.class).cardinality(Cardinality.SINGLE).make();
addressType = mgmt.makePropertyKey('addressType').dataType(String.class).cardinality(Cardinality.SINGLE).make();
addresses = mgmt.makePropertyKey('addresses').dataType(String.class).cardinality(Cardinality.LIST).make();
protocol = mgmt.makePropertyKey('protocol').dataType(String.class).cardinality(Cardinality.SINGLE).make();
role = mgmt.makePropertyKey('role').dataType(String.class).cardinality(Cardinality.SINGLE).make();
roleBinding = mgmt.makePropertyKey('roleBinding').dataType(String.class).cardinality(Cardinality.SINGLE).make();

// All edge properties
attckTechniqueID = mgmt.makePropertyKey('attckTechniqueID').dataType(String.class).cardinality(Cardinality.SINGLE).make();
attckTacticID = mgmt.makePropertyKey('attckTacticID').dataType(String.class).cardinality(Cardinality.SINGLE).make();

// Define properties for each vertex 
mgmt.addProperties(container, cls, cluster, runID, storeID, app, team, service, isNamespaced, namespace, name, image, privileged, privesc, hostPid, hostIpc, hostNetwork, runAsUser, podName, nodeName, compromised, command, args, capabilities, ports);
mgmt.addProperties(identity, cls, cluster, runID, storeID, app, team, service, name, isNamespaced, namespace, type, critical);
mgmt.addProperties(node, cls, cluster, runID, storeID, app, team, service, name, isNamespaced, namespace, compromised, critical);
mgmt.addProperties(pod, cls, cluster, runID, storeID, app, team, service, name, isNamespaced, namespace, sharedPs, serviceAccount, nodeName, compromised, critical);
mgmt.addProperties(permissionSet, cls, cluster, runID, storeID, app, team, service, name, isNamespaced, namespace, role, roleBinding, rules, critical);
mgmt.addProperties(volume, cls, cluster, runID, storeID, app, team, service, name, isNamespaced, namespace, type, sourcePath, mountPath, readonly);
mgmt.addProperties(endpoint, cls, cluster, runID, storeID, app, team, service, name, isNamespaced, namespace, serviceEndpoint, serviceDns, addressType, addresses, port, portName, protocol, exposure, compromised);

// Define properties for each edge
mgmt.addProperties(permissionDiscover, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(volumeDiscover, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(volumeAccess, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(hostWrite, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(hostRead, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(hostTraverse, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(sharedPsNamespace, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(containerAttach, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(idAssume, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(idImpersonate, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(roleBind, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(podAttach, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(podCreate, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(podPatch, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(podExec, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(tokenSteal, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(tokenBruteforce, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(tokenList, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(nsenter, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(moduleLoad, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(umhCorePattern, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(privMount, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(sysPtrace, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(varLogSymLink, runID, attckTechniqueID, attckTacticID);
mgmt.addProperties(endpointExploit, runID, attckTechniqueID, attckTacticID);

// Create the indexes on vertex properties
// NOTE: labels cannot be indexed so we create the class property to mirror the vertex label and allow indexing
mgmt.buildIndex('byClass', Vertex.class).addKey(cls).buildCompositeIndex();
mgmt.buildIndex('byCluster', Vertex.class).addKey(cluster).buildCompositeIndex();
mgmt.buildIndex('byRun', Vertex.class).addKey(runID).buildCompositeIndex();
mgmt.buildIndex('byStoreIDUnique', Vertex.class).addKey(storeID).unique().buildCompositeIndex();
mgmt.buildIndex('byApp', Vertex.class).addKey(app).buildCompositeIndex();
mgmt.buildIndex('byTeam', Vertex.class).addKey(team).buildCompositeIndex();
mgmt.buildIndex('byService', Vertex.class).addKey(service).buildCompositeIndex();
mgmt.buildIndex('byName', Vertex.class).addKey(name).buildCompositeIndex();
mgmt.buildIndex('byNamespace', Vertex.class).addKey(namespace).buildCompositeIndex();
mgmt.buildIndex('byType', Vertex.class).addKey(type).buildCompositeIndex();
mgmt.buildIndex('byCritical', Vertex.class).addKey(critical).buildCompositeIndex();
mgmt.buildIndex('byPort', Vertex.class).addKey(port).buildCompositeIndex();
mgmt.buildIndex('byPortName', Vertex.class).addKey(portName).buildCompositeIndex();
mgmt.buildIndex('byServiceEndpoint', Vertex.class).addKey(serviceEndpoint).buildCompositeIndex();
mgmt.buildIndex('byServiceDns', Vertex.class).addKey(serviceDns).buildCompositeIndex();
mgmt.buildIndex('byExposure', Vertex.class).addKey(exposure).buildCompositeIndex();

// Create composite indices for the properties we want to search on
mgmt.buildIndex('byClusterAndRunIDComposite', Vertex.class).addKey(cluster).addKey(runID).buildCompositeIndex();
mgmt.buildIndex('byClassAndRunIDComposite', Vertex.class).addKey(cls).addKey(runID).buildCompositeIndex();
mgmt.buildIndex('byClassAndClusterComposite', Vertex.class).addKey(cls).addKey(cluster).buildCompositeIndex();
mgmt.buildIndex('byClassAndTypeComposite', Vertex.class).addKey(cls).addKey(type).buildCompositeIndex();
mgmt.buildIndex('byClassAndExposureComposite', Vertex.class).addKey(cls).addKey(exposure).buildCompositeIndex();
mgmt.buildIndex('byTypeAndNameComposite', Vertex.class).addKey(type).addKey(name).buildCompositeIndex();
mgmt.buildIndex('byImageAndRunIDComposite', Vertex.class).addKey(image).addKey(runID).buildCompositeIndex();
mgmt.buildIndex('byAppAndRunIDComposite', Vertex.class).addKey(app).addKey(runID).buildCompositeIndex();
mgmt.buildIndex('byNamespaceAndRunIDComposite', Vertex.class).addKey(namespace).addKey(runID).buildCompositeIndex();

// Create the indexes on edge properties
mgmt.buildIndex('edgesByAttckTechniqueID', Edge.class).addKey(attckTechniqueID).buildCompositeIndex();
mgmt.buildIndex('edgesByAttckTacticID', Edge.class).addKey(attckTacticID).buildCompositeIndex();
mgmt.buildIndex('edgesByRunID', Edge.class).addKey(runID).buildCompositeIndex();

mgmt.commit();

// Wait for indexes to become available
ManagementSystem.awaitGraphIndexStatus(graph, 'byClass').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byCluster').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byRun').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byStoreIDUnique').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byApp').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byTeam').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byService').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byName').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byNamespace').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byType').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byCritical').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byPort').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byPortName').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byServiceEndpoint').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byServiceDns').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byExposure').status(SchemaStatus.ENABLED).call();

ManagementSystem.awaitGraphIndexStatus(graph, 'byClusterAndRunIDComposite').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byClassAndRunIDComposite').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byClassAndClusterComposite').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byClassAndTypeComposite').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byClassAndExposureComposite').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byTypeAndNameComposite').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byImageAndRunIDComposite').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byAppAndRunIDComposite').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'byNamespaceAndRunIDComposite').status(SchemaStatus.ENABLED).call();

ManagementSystem.awaitGraphIndexStatus(graph, 'edgesByAttckTechniqueID').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'edgesByAttckTacticID').status(SchemaStatus.ENABLED).call();
ManagementSystem.awaitGraphIndexStatus(graph, 'edgesByRunID').status(SchemaStatus.ENABLED).call();

System.out.println("[KUBEHOUND] graph schema and indexes ready");
mgmt.close();

// Close the open connection
:remote close
