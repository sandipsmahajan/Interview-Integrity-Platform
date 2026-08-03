# Account bootstrap

Creates the GitHub Actions OIDC provider. This is **account-scoped**: AWS only
allows a single OIDC provider for `token.actions.githubusercontent.com` per
account, so it must be created exactly once, before any environment root.

## Usage

```bash
cd terraform/account
terraform init
terraform apply -var="github_organisation=sandipsmahajan" \
                -var="github_repository=Interview-Integrity-Platform"
```

Every environment's `iam` module then imports this provider with a data
source. The state bucket bootstrap (terraform/bootstrap) and this root are
the only two one-time, manual steps (both CLI-only, no console required).
