# Troubleshooting: Ingress Issues

**Symptom.** Requests through the front door fail: `404`, `502`, `503`, or the ALB target is
unhealthy.

## 1. `404 Not Found` from nginx

**Cause:** no Ingress rule matches the request's `Host` header.

**Diagnose:**

```bash
kubectl -n integrity get ingress
kubectl -n integrity get ingress integrity-api -o yaml   # hosts + backend
# ALB forwards the Host header; the rule must match it exactly
```

**Fix:**

- Send the right Host: `curl -H 'Host: api.<env>...' http://<alb-dns>/...`.
- If the host changed, update `values-<env>.yaml` (`ingress.apiHost`) and re-upgrade.

## 2. `502 Bad Gateway`

**Cause:** the backend Service has no ready endpoints, or the pod isn't passing readiness.

**Diagnose:**

```bash
kubectl -n integrity get endpoints api-gateway        # empty ADDRESSES = no ready pods
kubectl -n integrity get pods | grep api-gateway      # 1/1?
kubectl -n integrity logs deploy/api-gateway --tail=30
```

**Fix:** resolve the pod readiness (`kubernetes-failures.md` §1); once ready, 502 clears.

## 3. `503 Service Temporarily Unavailable`

**Cause:** all endpoints removed (during rolling update) or the controller can't reach the
backend port (NetworkPolicy blocking ingress-nginx → pod).

**Diagnose:**

```bash
kubectl -n integrity get netpol
kubectl -n integrity describe pod <backend-pod> | grep -i policy
```

**Fix:** ensure the NetworkPolicy allows ingress from `ingress-nginx` on the service port
(the chart ships this rule — verify it wasn't deleted).

## 4. ALB target group unhealthy

**Cause:** ALB can't reach NodePort `30080` — SG or listener mismatch.

**Diagnose:**

```bash
# ALB target health
aws elbv2 describe-target-health --target-group-arn <arn> --query 'TargetHealthDescriptions[].TargetHealth'
# TargetHealth: healthy | unhealthy
kubectl -n ingress-nginx get svc ingress-nginx-controller   # NodePort 30080
```

**Fix:**

- Security group: allow ALB SG → node SG on `30080` (`networking.md` §3).
- Confirm the target group uses port `30080` and the right VPC.
- Confirm the controller is `Running` (`kubectl -n ingress-nginx get pods`).

## 5. `Default backend - 404` page

**Cause:** the request matched no rule (see §1) or no Ingress exists at all.

**Diagnose:**

```bash
kubectl -n ingress-nginx get ingress -A
kubectl -n ingress-nginx logs -l app.kubernetes.io/name=ingress-nginx --tail=20
```

**Fix:** apply/upgrade the chart so the Ingress objects exist; verify hosts.

## 6. Slow first byte / timeouts

**Cause:** proxy buffer defaults or the gateway is slow to respond on first request (lazy init).

**Diagnose:** compare latency inside the cluster vs through the ALB:

```bash
kubectl -n integrity run curl-test --rm -it --image=curlimages/curl -- \
  curl -s -o /dev/null -w '%{time_total}\n' http://api-gateway:8080/actuator/health
```

**Fix:** if in-cluster is fast, it's the proxy path — tune `proxy-read-timeout`/buffer
annotations on the Ingress; if in-cluster is slow too, it's the gateway (JVM warm-up) — check
`monitoring.md` latency.

## Prevention

- Keep Host names in DNS, values files, and ingress rules in one place (drift = 404s).
- Alarms on 4xx/5xx ratios through the ALB and target-group health.
- Test path routing with explicit `Host` headers in CI (the pipeline smoke test does).
