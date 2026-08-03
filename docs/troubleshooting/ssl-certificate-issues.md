# Troubleshooting: SSL / TLS Certificate Issues

**Symptom.** Browser warnings ("Your connection is not private"), `ERR_CERT_*` errors, or the
deploy-time `openssl` checks fail.

## 1. `ERR_CERT_DATE_INVALID`

**Cause:** the certificate is expired (or the client clock is wrong).

**Diagnose:**

```bash
echo | openssl s_client -connect api.<env>...:443 \
  -servername api.<env>... 2>/dev/null | openssl x509 -noout -dates
# notAfter must be in the future
date -u   # confirm server/local clock
```

**Fix:** ACM auto-renews; if it didn't, follow `runbooks/certificate-renewal.md`. If the client
clock is skewed (dev machine), fix the machine clock — not the cert.

## 2. `ERR_CERT_AUTHORITY_INVALID`

**Cause:** self-signed or untrusted chain (dev used a self-signed cert somewhere, or a
MITM/proxy intercepts TLS).

**Diagnose:**

```bash
echo | openssl s_client -connect api.<env>...:443 -servername api.<env>... 2>/dev/null \
  | openssl x509 -noout -subject -issuer
# issuer must be a public CA (e.g. Amazon, Let's Encrypt), not "CN=..." of yourself
```

**Fix:** replace the cert with an ACM-issued one (`deployment/19-enable-https.md`). For
proxy/MITM: fix the proxy trust store.

## 3. `ERR_SSL_VERSION_OR_CIPHER_MISMATCH`

**Cause:** the ALB policy rejects the client's TLS version/cipher (policy is TLS 1.2+).

**Fix:** update the client to support TLS 1.2+; don't weaken the server policy.

## 4. Hostname mismatch (`ERR_CERT_COMMON_NAME_INVALID`)

**Cause:** the cert doesn't cover the hostname.

**Diagnose:**

```bash
echo | openssl s_client -connect api.<env>...:443 -servername api.<env>... 2>/dev/null \
  | openssl x509 -noout -ext subjectAltName
# DNS: must include api.<env>... (or a wildcard that covers it)
```

**Fix:** request the cert with the correct SANs / wildcard (ACM), and keep
`ingress.apiHost`/`portalHost` in sync with the cert's SANs.

## 5. `PENDING_VALIDATION` forever in ACM

**Cause:** the DNS validation CNAME isn't published, or was published in the wrong zone.

**Diagnose:**

```bash
aws acm describe-certificate --certificate-arn <arn> \
  --query 'Certificate.DomainValidationOptions[].ResourceRecord'
dig +short <record-name> CNAME   # must return the ACM target
```

**Fix:** publish the exact CNAME (`deployment/19-enable-https.md` §3); Route 53 does this
automatically when managed by Terraform. Wait; ACM rechecks periodically.

## 6. `curl: (60) SSL certificate problem` from a pod or CI

**Cause:** the client can't verify the chain (missing CA bundle in a slim image, or it's hitting
the ALB by IP with no SNI).

**Fix:** hit the **hostname** (not the ALB IP) so SNI works; ensure the image has
`ca-certificates`. The base images already do.

## 7. Mixed content after HTTPS

**Symptom:** page loads but browser blocks resources on `http://`.

**Cause:** assets linked via `http://` (portals, client calls).

**Fix:** use relative URLs in portals; the gateway/proxy must be accessed via `https://`. Check
the browser console for the blocked URLs.

## Prevention

- ACM + auto-renewal everywhere; keep the 14-day expiry alarm.
- No self-signed certs outside a scratch dev box.
- CAA record restricting issuance to ACM.
- After any cert change, run the `openssl` verification block from `deployment/19-*` §Verification.
