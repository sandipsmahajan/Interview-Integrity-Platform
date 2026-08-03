# Runbook: Certificate Renewal

**Symptom.** Certificate expiry alarm (`certmanager_certificate_expiration_timestamp_seconds -
time() < 1209600`), browser "not private" warnings, or `ERR_CERT_DATE_INVALID`.

**Severity.** P2 (browsers hard-fail on expired certs — treat as urgent once < 7 days).

**Impact.** HTTPS becomes unusable for the affected hostname (API + portals).

## Prerequisites

- `aws` CLI (ACM), `kubectl`, Terraform access.
- 15 minutes.

## Diagnosis

```bash
# Which certs exist and their status
aws acm list-certificates --region us-east-1 \
  --query 'CertificateSummaryList[].{Domain:DomainName,Status:Status,InUse:InUseBy}' \
  --output table

# Is one nearly expired?
aws acm describe-certificate --certificate-arn <arn> \
  --query 'Certificate.{Domain:DomainName,NotAfter:NotAfter,Status:Status}'

# In-cluster secrets (if TLS terminates at ingress)
kubectl -n integrity get secret -l kubernetes.io/tls-type=...
```

## Resolution

### The normal case: ACM-managed (recommended, no action)

ACM **automatically renews** certificates. If the alarm fires, verify the cert's domain matches
an ACM cert and that DNS validation still resolves:

```bash
# The CNAME ACM uses for validation must still exist
aws acm describe-certificate --certificate-arn <arn> \
  --query 'Certificate.DomainValidationOptions[].ResourceRecord'
# If missing, recreate the DNS record (deployment/19-enable-https.md)
```

ACM revalidates on a schedule; a renewed cert is swapped in automatically. Just confirm
`NotAfter` moved forward within a few days.

### If the cert is near expiry and NOT auto-renewing

1. **Confirm domain ownership** — the DNS CNAME must exist and resolve:
   ```bash
   dig +short _<hash>.<domain> CNAME
   ```
2. **Force revalidation**:
   ```bash
   aws acm resend-validation-email --certificate-arn <arn> --domain <domain> \
     --region us-east-1
   ```
3. If the cert is **issuing a new one** (wildcard, subdomain changes), request it and attach to
   the ALB listener:
   ```bash
   aws acm request-certificate --region us-east-1 \
     --domain-name "api.<env>.integritypro.example.com" \
     --subject-alternative-names "portal.<env>.integritypro.example.com" \
     --validation-method DNS
   # publish the returned CNAME, wait for ISSUED, then update the ALB listener
   aws elbv2 modify-listener --listener-arn <arn> \
     --certificates CertificateArn=<new-arn> --protocol HTTPS --port 443
   ```

### If TLS terminates at ingress (chart secrets)

```bash
# The secret must contain a valid cert+key. Update it and rollout:
kubectl -n integrity create secret tls integrity-<env>-api-tls \
  --cert=tls.crt --key=tls.key --dry-run=client -o yaml | kubectl apply -f -
kubectl -n integrity rollout restart deployment --all
```

## Verification

```bash
echo | openssl s_client -connect api.<env>.integritypro.example.com:443 \
  -servername api.<env>.integritypro.example.com 2>/dev/null | \
  openssl x509 -noout -dates
# notAfter must be > today + 30 days (or renewed by ACM)
curl -sI https://api.<env>.integritypro.example.com/actuator/health | head -1
# HTTP/2 200
```

## Rollback

- A wrongly-issued cert: point the ALB listener back at the previous valid ARN and verify.
- ACM renewal cannot be "undone" — if a renewal produced a broken cert, re-request with the
  correct SANs.

## Prevention

- Rely on ACM auto-renewal; keep the alarm at 14 days as a tripwire.
- Never self-sign for prod.
- When adding a subdomain, request it as a SAN or wildcard *before* pointing DNS at it.
