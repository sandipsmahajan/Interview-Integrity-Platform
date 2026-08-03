# 03 — Configure the AWS CLI

**Purpose.** To install and configure the AWS Command Line Interface (CLI) so that every later
step (`terraform`, `docker`, `kubectl`, `aws`) authenticates as your `integrity-deployer` user.

## Prerequisites

- Steps 01–02 completed (account + IAM user with an access key).
- Git installed (`git --version`).

## Estimated Time

10 minutes.

## Required AWS permissions

Only the access key you created in step 02. The CLI itself needs no special permission to be
*configured*; API calls it makes will enforce the user's permissions.

## What the AWS CLI is

The AWS CLI is the official tool for calling AWS APIs from the terminal. All other tools in this
guide (Terraform, docker ECR login, kubectl) ultimately use it to authenticate.

## Steps

### 1. Install

**macOS (Homebrew):**

```bash
brew install awscli
```

**Linux (Ubuntu/Debian) — official installer:**

```bash
# Download the bundled installer
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -o awscliv2.zip
# Run the installer (prefix isolates it from the system Python)
sudo ./aws/install
```

**Windows:** download the MSI from <https://awscli.amazonaws.com/AWSIV2.msi> and install.

Verify:

```bash
aws --version
# aws-cli/2.x.x Python/3.x.x Linux/...
```

### 2. Configure the default profile

```bash
aws configure
```

The tool prompts for four values:

| Prompt | Value | Why |
|---|---|---|
| AWS Access Key ID | from step 02 | your identity |
| AWS Secret Access Key | from step 02 | your credential |
| Default region name | `us-east-1` | matches every Terraform root's provider default |
| Default output format | `json` | machine-readable output for scripts |

These are stored in `~/.aws/credentials` and `~/.aws/config`. **These files are secret** — never
share them and never let them be committed to a repository.

### 3. (Recommended) Set a session-friendly profile name

If you manage multiple AWS environments, give the config an explicit profile:

```bash
aws configure --profile integrity
```

Then prefix all later commands with `--profile integrity`, or export it for the shell session:

```bash
export AWS_PROFILE=integrity
```

## Expected output

- `~/.aws/credentials` contains the access key.
- `aws --version` prints version 2.x.

## Verification steps

```bash
# Who am I?
aws sts get-caller-identity

# Can I see the region?
aws ec2 describe-regions --region us-east-1 --output table
```

Both must succeed. The first prints the `integrity-deployer` ARN; the second lists AWS regions.

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| `Unable to locate credentials` | `aws configure` never ran / wrong profile | Re-run `aws configure`, check `AWS_PROFILE` |
| `InvalidClientTokenId` / `SignatureDoesNotMatch` | Wrong or truncated access key | Re-enter the key; regenerate in IAM if lost |
| `ExpiredToken` | Key was rotated/deleted | Create a new key in IAM, update `aws configure` |
| `You must specify a region` | Region not set | `aws configure set region us-east-1` |
| `Proxy` errors | Corporate proxy blocks HTTPS | Set `HTTPS_PROXY` for your shell |

## Rollback procedure

- To stop using the CLI credentials: delete the access key in IAM
  (**IAM → Users → `integrity-deployer` → Security credentials → Deactivate/Delete**) and remove
  the matching lines from `~/.aws/credentials`. All AWS API calls then fail cleanly, which is
  exactly what you want if a key is compromised.

## Best practices

- Use a named profile (`--profile integrity`) so you never accidentally target the wrong
  account.
- Never paste access keys into shell history you share; prefer `aws configure` over inline
  `export AWS_ACCESS_KEY_ID=...`.
- When running in CI, **do not** use this user's key — GitHub Actions uses OIDC
  (configured in step 07/08 and the `terraform/account` module).

## Security notes

- `~/.aws/credentials` should be readable only by you:
  `chmod 600 ~/.aws/credentials`.
- The CLI is the doorway to your account. Pair every console session with MFA where possible
  (the CLI key itself is MFA-independent, which is why it must be stored carefully and rotated).
