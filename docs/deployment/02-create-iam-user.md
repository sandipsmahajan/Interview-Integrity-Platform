# 02 — Create the IAM User and Enable MFA

**Purpose.** To create the *human* identity that will deploy and operate Integrity Pro. AWS
strongly recommends you never use the root account for daily work — root is for billing and
account-level recovery only.

## Prerequisites

- Step 01 completed (you can sign in to the AWS console as root).
- Your root account has MFA enabled.

## Estimated Time

15 minutes.

## Required AWS permissions

Performed as **root** — root can do everything. After this step you should *stop using root*.

## Concepts (no external tutorial needed)

| Term | Meaning |
|---|---|
| **IAM** | Identity and Access Management — AWS's user/role/permission system |
| **User** | A permanent identity with its own credentials |
| **Group** | A container of users that share permissions — put users in groups, never attach policies to users directly |
| **Policy** | A JSON document granting (or denying) permissions on AWS resources |
| **MFA** | Multi-factor authentication — a second factor (authenticator app / hardware key) |
| **Access key** | The `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` pair the CLI uses |
| **Least privilege** | Grant only the permissions the identity actually needs |

## Steps

1. **Sign in as root** at <https://console.aws.amazon.com> and ensure MFA is enabled:
   **IAM → Your security credentials → Multi-factor authentication (MFA) → Assign MFA**.
   Choose *Authenticator app* and scan the QR code with your phone's authenticator app.

2. **Create a group for administrators.**

   - IAM → **Access management → Groups → Create group**.
   - Name: `platform-admins`.
   - Attach the AWS managed policy **`AdministratorAccess`** (needed while the platform is being
     bootstrapped; see "Least privilege later" below).

3. **Create the IAM user.**

   - IAM → **Users → Create user**.
   - User name: `integrity-deployer`.
   - Check *Provide user access to the AWS Management Console* and choose *IAM user will create a
     password* — set one (or enable *User must create a password at next sign-in*).
   - Next → **Add user to group** → select `platform-admins` → Create user.
   - Copy the **Console sign-in URL** (it looks like `https://<account-id>.signin.aws.amazon.com/console`).

4. **Create an access key for the CLI.**

   - Open the user → **Security credentials** tab → **Create access key**.
   - Use case: *Command Line Interface (CLI)* → Next.
   - Download the `.csv` or copy both values **now** — the secret is shown only once.

5. **Enable MFA on the user.**

   - Still on the user → **Security credentials → Assign MFA → Authenticator app**.
   - Scan with the same authenticator app (or a second device).

6. **Test the new user.**

   - Sign out of root, sign in as `integrity-deployer` via the sign-in URL, confirm MFA prompt.
   - If MFA is not yet available you can use a *virtual MFA device* on the same phone.

## Expected output

- A `platform-admins` group containing `integrity-deployer`.
- An access key (`AKIA...` + secret) safely stored in your password manager.
- You can sign in as `integrity-deployer` with MFA, and the console shows the full AWS dashboard.

## Verification steps

```bash
# (After step 03 configures the CLI) confirm the user identity
aws sts get-caller-identity
```

Expected:

```json
{
  "UserId": "AIDAXXXXXXXXXXXXXXXXXX",
  "Account": "123456789012",
  "Arn": "arn:aws:iam::123456789012:user/integrity-deployer"
}
```

The `Arn` ending in `user/integrity-deployer` proves you are operating as the intended identity.

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| "You are not authorized to perform iam:CreateUser" | Not using an admin identity | Sign in as root or grant `iam:*` to your group |
| "Your account is not signed up for MFA" | MFA not configured on the account | Assign MFA to root first |
| Access key download failed | Pop-up blocked | Recreate the key and store it immediately |

## Rollback procedure

- To undo a user: **IAM → Users → `integrity-deployer` → Delete** (only if no infrastructure
  depends on it yet). You cannot delete the last MFA device on a user without resetting it.

## Best practices

- **Put users in groups, attach policies to groups** — never inline-edit user permissions.
- Name access keys clearly (e.g. `laptop-2026-02`) so they are identifiable when rotated.
- **Least privilege later**: after the platform is live (step 20), replace
  `AdministratorAccess` on `platform-admins` with scoped policies, or — better — move all
  *deployment* work to the GitHub OIDC roles and reduce this user to read-only + secrets
  administration.
- Rotate the access key every 90 days: create new, update CLI, delete old.

## Security notes

- The secret access key is equivalent to a password — **never** commit it, never paste it into
  chat/tickets, never put it in `terraform.tfvars`.
- The CLI key you created is used by step 03 only. CI/CD uses **OIDC** (no static keys at all).
- Keep root MFA and user MFA on **different** devices where possible, so losing one device does
  not lock you out.
