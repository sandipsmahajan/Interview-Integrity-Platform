# Troubleshooting: DNS Issues

**Symptom.** Hostnames don't resolve, records go stale, or the wrong host answers.

## 1. `NXDOMAIN` — the name doesn't resolve

**Diagnose:**

```bash
dig +short api.dev.integritypro.example.com
# empty = NXDOMAIN
dig +short api.dev.integritypro.example.com @8.8.8.8   # bypass local cache
```

| Cause | Fix |
|---|---|
| Record never created | Create it (`deployment/18-configure-domain.md`) |
| Delegation wrong (NS records don't match Route 53) | Set the registrar's NS records to the zone's four NS |
| Propagation delay | Up to 24–48 h at TLDs; usually < 1 h |
| Typo (dev vs dev.) | Verify the FQDN exactly |

## 2. Record exists but resolves to the wrong thing

```bash
dig +short <host>          # what the world sees
dig +short <host> @<ns>    # what your NS thinks
```

**Cause:** a cached value, or ALIAS pointing at an old ALB.

**Fix:** ALIAS records track the ALB automatically (Route 53). If you used a CNAME to a raw ALB
that was replaced, update the CNAME target. Wait for TTL (usually 60–300 s).

## 3. Intermittent resolution inside the cluster

**Symptom:** pods sometimes get `getaddrinfo: Name or service not known` for service names.

**Cause:** CoreDNS (cluster DNS) pod restarting, or a NetworkPolicy blocking egress to `kube-dns`.

**Diagnose:**

```bash
kubectl -n kube-system get pods | grep coredns
kubectl -n kube-system logs -l k8s-app=kube-dns --tail=20
kubectl -n integrity get netpol   # must allow egress DNS
```

**Fix:** scale CoreDNS (`kubectl -n kube-system scale deployment/coredns --replicas=2`); ensure
the platform NetworkPolicy has the DNS egress rule (it does).

## 4. `SERVFAIL`

**Cause:** an authoritative server refused or timed out (often DNSSEC or zone issue).

**Diagnose:**

```bash
dig <host> +trace @8.8.8.8 | tail -20
```

**Fix:** for Route 53, check the zone's health; for external registrars, verify DNSSEC isn't
half-configured. This is usually a registrar-side issue.

## 5. Correct record but browser fails

**Symptom:** `dig` resolves fine; browser says server not found.

**Cause:** OS/network DNS cache, VPN split-tunnel, or the browser's secure-DNS override.

**Fix:** `dscacheutil -flushcache` (macOS) / `ipconfig /flushdns` (Windows) / `systemd-resolve
--flush-caches` (Linux); try another network.

## 6. Apex domain (`example.com`) won't work as a CNAME

**Cause:** DNS standards forbid CNAME at the zone apex for many providers.

**Fix:** use an ALIAS/ANAME (Route 53 ALIAS works at the apex) or an A record to the ALB IP.

## Prevention

- Use ALIAS in Route 53 so records track the ALB.
- Set sane TTLs (300 s for non-prod, 300–3600 for stable prod records).
- Keep a DNS-change log; most "DNS is broken" reports are a changed-but-not-propagated record.
