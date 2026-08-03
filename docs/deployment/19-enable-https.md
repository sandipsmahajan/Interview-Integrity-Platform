# 19 — Enable HTTPS (TLS)

**Purpose.** To encrypt all traffic end to end with valid, auto-renewing TLS certificates, so the
platform is only reachable over `https://` and browsers show no warnings.

## Prerequisites

- Step 18 completed (domain resolves to the ALB).
- ACM certificate(s) issued for your hosts (Terraform's `acm` module does this when
  `tls_domain`/`dns_domain` are set; it also validates via DNS).
- The chart's TLS secret names set in `values-<env>.yaml` (`ingress.apiTlsSecret`,
  `ingress.portalTlsSecret`).

## Estimated Time

20 minutes (certificate validation takes ~5 minutes after DNS changes).

## Required AWS permissions

`acm:RequestCertificate`, `acm:DescribeCertificate`, `acm:GetCertificate`, and the DNS change
permissions from step 18. Terraform already has these via `platform-admins`.

## Concepts

| Term | Meaning |
|---|---|
| **ACM** | AWS Certificate Manager — issues and *automatically renews* public TLS certificates |
| **DNS validation** | ACM proves you own the domain by asking you to publish a special `CNAME _xxxx.<domain>` |
| **ALB listener** | The ALB's `:443` listener; it terminates TLS and forwards plain HTTP to the cluster |
| **TLS secret** | The `kubernetes.io/tls` Secret referenced by the Ingress when TLS terminates inside the cluster |

**Where TLS terminates.** In this architecture the **ALB terminates TLS** (ACM certificate on the
`:443` listener). The `ingress.apiTlsSecret` values still exist so the same chart works in
situations where TLS terminates at ingress-nginx instead (e.g. a non-ALB front door). Keep both
consistent for simplicity: point the Ingress TLS secret at a Secret whose certificate matches the
domain.

## Steps

### 1. Confirm the ACM certificate is issued

```bash
aws acm list-certificates --region us-east-1 --query \
  'CertificateSummaryList[].{Domain:DomainName,Status:Status}' --output table
```

You want `Status = ISSUED` for `*.integritypro.example.com` (or your exact hosts). If it says
`PENDING_VALIDATION`, Terraform's `acm` module created the DNS validation CNAME — make sure it
was published (Route 53 does this automatically when you manage DNS in Terraform). Wait a few
minutes; ACM re-checks periodically.

### 2. (Alternative) Request + validate manually

```bash
aws acm request-certificate --region us-east-1 \
  --domain-name "api.dev.integritypro.example.com" \
  --validation-method DNS \
  --subject-alternative-names "portal.dev.integritypro.example.com"
# note the CertificateArn

# ACM tells you the CNAME to publish:
aws acm describe-certificate --certificate-arn <arn> --query 'Certificate.DomainValidationOptions'
# publish the CNAME (as an alias record) in your DNS, then wait for ISSUED
```

### 3. Ensure the TLS secret exists in the cluster

If TLS terminates at ingress-nginx (not at the ALB), create the referenced secrets:

```bash
# Reuse the ACM cert by exporting it, or use cert-manager to manage this automatically.
# Simple path for ingress-level TLS:
kubectl -n integrity create secret tls integrity-dev-api-tls \
  --cert=<path>/tls.crt --key=<path>/tls.key
kubectl -n integrity create secret tls integrity-dev-portal-tls \
  --cert=<path>/tls.crt --key=<path>/tls.key
```

> With the ALB front door you can skip this — the ALB holds the cert. But the values already name
> the secrets; providing them keeps the chart self-consistent and enables `HSTS` on the ingress.

### 4. Configure the ALB listener for :443

If Terraform's `alb` module is configured with the ACM ARN, the `:443` listener already exists
and redirects `:80 → :443`. Verify:

```bash
aws elbv2 describe-listeners --load-balancer-arn <alb-arn> \
  --query 'Listeners[].{Port:Port,Protocol:Protocol}' --output table
# 443  HTTPS
# 80   HTTP  (with a redirect action to https)
```

### 5. Apply the chart with TLS hosts

```bash
helm upgrade integrity infra/helm/interview-integrity \
  --namespace integrity \
  -f infra/helm/interview-integrity/values-dev.yaml \
  --reuse-values
```

Expected output: release upgraded; Ingress objects now carry a `tls:` block (from
`apiTlsSecret`/`portalTlsSecret` in the values).

## Expected output

```bash
curl -sI https://api.dev.integritypro.example.com/actuator/health
# HTTP/2 200
# strict-transport-security: max-age=...   (if HSTS annotation enabled)
```

## Verification steps

```bash
# 1. HTTPS works
curl -sI https://api.dev.integritypro.example.com/actuator/health | head -3

# 2. Certificate chain is valid
echo | openssl s_client -connect api.dev.integritypro.example.com:443 \
  -servername api.dev.integritypro.example.com 2>/dev/null | \
  openssl x509 -noout -subject -issuer -dates

# 3. HTTP redirects to HTTPS
curl -sI http://api.dev.integritypro.example.com/actuator/health | head -1
# HTTP/1.1 301 Moved Permanently (Location: https://...)

# 4. No TLS version below 1.2 is accepted
echo | openssl s_client -connect api.dev.integritypro.example.com:443 \
  -tls1_1 -servername api.dev.integritypro.example.com 2>&1 | grep -i 'protocol'
```

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `PENDING_VALIDATION` forever | DNS validation CNAME missing/not propagated | Publish the CNAME; verify with `dig +short <_cname>`; wait |
| `ERR_CERT_AUTHORITY_INVALID` | Using a self-signed cert | Replace with the ACM cert; never ship self-signed to prod |
| Certificate mismatch warning | Cert for wrong host | Cert must cover `api.*` and `portal.*` (SAN or wildcard) |
| `curl: (35) SSL connect error` | Listener is still HTTP | Check the ALB listener table; re-apply the `alb` module |
| Browsers warn "connection not private" but `openssl` is fine | Local clock/chain cache | Check system time; `openssl s_client -showcerts` to inspect chain |

## Rollback procedure

- Temporarily revert to HTTP by removing the `:443` listener / disabling the redirect in the
  `alb` module and re-applying — use only as an emergency, it weakens security.
- To point at a previous certificate, update the `acm` module's ARN and re-apply; ACM handles the
  switch gracefully.

## Best practices

- **Rely on ACM's automatic renewal** — do not hand-manage certs in prod. Set a CloudWatch alarm
  on `certificate_expiration < 30 days` as a belt-and-braces check.
- Enforce `TLS1.2+` on the ALB listener policy; prod can use `TLS1.3`.
- Add `HSTS` (`Strict-Transport-Security`) at the ingress so browsers never fall back to HTTP.

## Security notes

- DNS validation proves domain ownership without opening `:80` — keep using it.
- Add a **CAA record** (`CAA 0 issue "amazonaws.com"`) so only ACM can issue certs for your
  domain — this prevents rogue cert issuance.
- Never downgrade the ALB policy to support old clients; `TLS1.2` is the floor.
