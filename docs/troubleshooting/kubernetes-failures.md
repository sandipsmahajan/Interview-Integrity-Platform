# Troubleshooting: Kubernetes Failures

**Symptom.** Pods not ready, unexpected events, RBAC denials, HPA issues, or PVC problems.

## 1. Pod not becoming Ready

```bash
kubectl -n integrity get pods
kubectl -n integrity describe pod <pod-name>      # conditions + events are the answer
kubectl -n integrity get events --sort-by=.lastTimestamp | tail -30
```

| Condition | Cause | Fix |
|---|---|---|
| `Unschedulable` | No node fits (CPU/mem/PVC AZ) | Scale nodes (`runbooks/scaling.md`); check PVC binding |
| `ContainerCreating` stuck | Image pull or volume attach | `kubectl describe pod` for the error; check EBS CSI/PVC |
| `CrashLoopBackOff` | App exits | `kubectl logs <pod> --previous`; see `runbooks/pod-crash.md` |
| `ImagePullBackOff` | Image missing / ECR auth | `runbooks/pod-crash.md` §A |
| Readiness probe failing | Dependency down (DB/Kafka/Redis) | Check the probe URL: `kubectl exec -it <pod> -- curl localhost:8080/actuator/health/readiness` |

## 2. Readiness vs liveness probe confusion

**Symptom:** pods restart in a loop even though the app "seems fine".

**Cause:** the liveness probe depends on an external system (DB), so a DB blip kills pods.

**Fix:** liveness must check only process health (`/actuator/health/liveness`); readiness checks
dependencies (`/actuator/health/readiness`). Correct the chart if needed.

## 3. `forbidden: User "system:serviceaccount:..." cannot ...`

**Cause:** RBAC. The pod or user lacks the permission.

**Diagnose:**

```bash
kubectl auth can-i get pods --as=system:serviceaccount:integrity:<sa> -n integrity
kubectl -n integrity get role,rolebinding
```

**Fix:** add the Role/RoleBinding (see `security.md` §7). For pod-to-AWS calls, check IRSA role
policy instead.

## 4. `Forbidden` from the kubelet / EKS auth

**Cause:** your IAM identity isn't mapped to a Kubernetes RBAC identity (the EKS aws-auth
ConfigMap or an access entry).

**Fix:**

```bash
# ensure kubeconfig points at the right cluster
kubectl config current-context
# add your IAM role/user to the EKS access entries if missing:
aws eks create-access-entry --cluster-name integrity-<env>-eks --principal-arn <your-arn>
```

## 5. HPA not scaling

```bash
kubectl -n integrity describe hpa <service>
# Events: FailedGetResourceMetric / unable to fetch metrics
kubectl top nodes && kubectl top pods   # metrics-server working?
```

Fix metrics-server (`kubernetes.md` §9); confirm the HPA target matches `resources.requests.cpu`.

## 6. PVC stuck `Pending`

```bash
kubectl -n integrity get pvc
kubectl -n integrity describe pvc <pvc>
kubectl get sc   # gp3-encrypted must be present (deploy infra/k8s/storageclass.yaml)
```

| Cause | Fix |
|---|---|
| StorageClass missing | `kubectl apply -f infra/k8s/storageclass.yaml` |
| `WaitForFirstConsumer` pending | The pod must be scheduled first; check pod is not `Unschedulable` |
| EBS CSI driver missing | Install the driver (EKS add-on) |

## 7. Node NotReady / pressure

See `runbooks/node-failure.md`.

## 8. Namespace confusion

**Symptom:** `kubectl get pods` shows nothing but the platform works.

**Cause:** you forgot `-n integrity` (all platform objects live there).

**Fix:**

```bash
kubectl -n integrity get all
# or switch your default: kubectl config set-context --current --namespace=integrity
```

## Prevention

- Alerts on `kube_deployment_status_replicas_available < desired` and pod restart counts.
- Run `kubectl -n integrity get events` after every deploy.
- Keep a `kubectl get all -n integrity` habit — a 10-second scan catches most drift.
