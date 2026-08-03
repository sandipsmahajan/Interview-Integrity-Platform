# 18 — Configure Your Domain

**Purpose.** To make your public domain (e.g. `example.com`) resolve to the Integrity Pro
platform, so users reach it by a real hostname instead of a raw ALB DNS name.

## Prerequisites

- Step 17 passed (platform verified).
- You own a domain and control its DNS (your domain registrar, or Route 53 as DNS host).
- The Terraform root for the environment ran with `dns_domain` set (e.g.
  `integritypro.example.com`) — see `terraform/environments/<env>/variables.tf`.

## Estimated Time

30 minutes (plus DNS propagation, usually under an hour).

## Required AWS permissions

`route53:ChangeResourceRecordSets`, `route53:GetHostedZone` on the hosted zone. If you use
another registrar, permissions are not needed — you just set CNAME/A records there.

## Concepts

| Term | Meaning |
|---|---|
| **A record** | Maps a hostname to an IPv4 address |
| **ALIAS record** | Route 53-specific A record that can point to an AWS resource (ALB) by name and stays in sync when the ALB changes |
| **CNAME** | Maps a hostname to another hostname (e.g. `api.example.com` → ALB DNS) |
| **Hosted zone** | Route 53's container for all records of a domain |
| **NS records** | The name servers that answer DNS for your domain — you delegate by pointing the registrar at them |

## The target records

| Record | Type | Value |
|---|---|---|
| `api.<env>.integritypro.example.com` | A (ALIAS) | ALB DNS name |
| `portal.<env>.integritypro.example.com` | A (ALIAS) | ALB DNS name |
| `admin.<env>.integritypro.example.com` | A (ALIAS) | ALB DNS name (if used) |

These match `ingress.apiHost` / `ingress.portalHost` in `values-<env>.yaml` and the ALB wiring in
`terraform/modules/route53`.

## Steps

### 1. Get the ALB DNS name

```bash
# From the environment's Terraform outputs (or find the load balancer by name)
cd terraform/environments/<env>
terraform output alb_dns_name
# e.g. k8s-integrity-xxxx-xxxxxxxx.us-east-1.elb.amazonaws.com
```

### 2a. DNS is hosted in Route 53

If `create_hosted_zone = true` was set (first time), Terraform created a public hosted zone for
your domain. Get its name servers:

```bash
aws route53 get-hosted-zone --id <zone-id> --query 'DelegationSet.NameServers'
# ["ns-1234.awsdns-01.org", ...]
```

Then, **at your registrar**, set the domain's NS records to exactly those four name servers. This
*delegates* the domain to Route 53. (If the domain was already registered via Route 53, this is
automatic.)

Create the alias records:

```bash
aws route53 change-resource-record-sets --hosted-zone-id <zone-id> --change-batch '{
  "Changes": [
    {"Action":"UPSERT","ResourceRecordSet":{
      "Name":"api.dev.integritypro.example.com","Type":"A",
      "AliasTarget":{"HostedZoneId":"<alb-zone-id>","DNSName":"<alb-dns-name>","EvaluateTargetHealth":false}}},
    {"Action":"UPSERT","ResourceRecordSet":{
      "Name":"portal.dev.integritypro.example.com","Type":"A",
      "AliasTarget":{"HostedZoneId":"<alb-zone-id>","DNSName":"<alb-dns-name>","EvaluateTargetHealth":false}}}
  ]}'
```

> `HostedZoneId` here is the **ALB's** hosted zone ID (a fixed value per region, e.g.
> `Z35SXDOTRQ7X7K` for `us-east-1`), **not** your domain's hosted zone ID. Look it up once for
> your region in the ALB docs, or use the `zone_id` output of the Terraform `alb` module.

> **Tip:** the `route53` Terraform module already declares these records; applying the env root
> with the module enabled does this for you. The manual `change-resource-record-sets` above is for
> the case where you manage DNS outside Terraform.

### 2b. DNS is hosted at another registrar (e.g. Cloudflare, GoDaddy)

Do not use Route 53. Add a **CNAME** at your registrar:

```text
api.dev.integritypro.example.com  CNAME  k8s-integrity-...us-east-1.elb.amazonaws.com
portal.dev.integritypro.example.com CNAME k8s-integrity-...us-east-1.elb.amazonaws.com
```

CNAME works for HTTP(S) to the ALB. (You would only need A records if you need the domain apex
`example.com` itself to point at the ALB.)

## Expected output

```bash
dig +short api.dev.integritypro.example.com
# k8s-integrity-...us-east-1.elb.amazonaws.com.
```

The hostname resolves to the ALB.

## Verification steps

1. `dig +short <api-host>` returns the ALB DNS name (or the ALB's IP).
2. With HTTP still enabled (HTTPS comes in step 19):
   ```bash
   curl -sI http://api.dev.integritypro.example.com/actuator/health
   # HTTP/1.1 200 OK
   ```
3. `curl -sI http://portal.dev.integritypro.example.com` returns the portal's HTML (200/301).

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `NXDOMAIN` | Hostname not published / wrong NS delegation | Verify NS records at the registrar match Route 53; wait up to 24 h |
| `curl: (6) Could not resolve host` | DNS not propagated | `dig @8.8.8.8` to bypass local cache; wait |
| Record resolves but connection refused | Host header mismatch | The ALB/nginx needs the `Host` header matching an ingress rule; if you changed hosts, `helm upgrade` the values |
| ALIAS record "not found" | Wrong hosted zone ID used | Use the ALB's zone ID for the region, not the domain's |

## Rollback procedure

- To undo, delete the records you created:
  ```bash
  aws route53 change-resource-record-sets --hosted-zone-id <zone-id> \
    --change-batch '{"Changes":[{"Action":"DELETE","ResourceRecordSet":{...same as created...}}]}'
  ```
- Or simply change the registrar's CNAME back to what it was. Traffic fails closed (no DNS → no
  access) until you fix it, which is the safe direction.

## Best practices

- Use **ALIAS** records when you can (Route 53) so DNS follows the ALB automatically.
- Keep `dns_domain` and the values-file hosts in sync; drift between them is the #1 "domain
  doesn't work" cause.
- Add a CAA record (`example.com. CAA 0 issue "amazonaws.com"`) so only AWS ACM can issue
  certificates for your domain (step 19).

## Security notes

- Only publish the records you need (`api`, `portal`, `admin`). Do not publish wildcard records
  to the internet.
- The ALB has no public security by itself at this point — HTTPS + WAF arrive in step 19; do not
  advertise the URL until TLS is on.
