# 01 — Create the AWS Account

**Purpose.** To create a brand-new, billing-ready AWS account that will host Integrity Pro. This
is the very first thing you do; nothing else works without it.

## Prerequisites

- A valid email address you can access.
- A credit/debit card for billing (you will be billed only for what you use; Integrity Pro in
  `dev` can run for a few dollars per day).
- A phone number for verification.

## Estimated Time

10–20 minutes.

## Steps

1. Open your browser and go to <https://aws.amazon.com>.

2. Click **Create an AWS account** (top right).

3. Enter a **root user email address** — this becomes your *root* login. Use an operational alias
   like `aws-root@yourcompany.com` that is monitored, not a personal address. Then choose an
   account name (e.g. `integrity-pro-platform`).

4. Click **Continue** and verify your email with the code AWS sends you.

5. Set a strong **root password** (16+ characters, unique, store in your password manager).
   The root account is the *only* account that can delete the account or change billing — treat it
   like a vault key, never use it day-to-day.

6. **Contact info**: choose *Business* (even for a lab) and fill in the company name and address.

7. **Billing**: enter the payment card and billing address.

8. **Confirm**: accept the AWS Customer Agreement, then click **Create account and continue**.

9. AWS verifies your phone number with an automated call or SMS. Enter the code.

10. Wait for the confirmation email ("Your account is ready"). This can take a few minutes.
    Then click **Sign in to the console**.

## Expected output

- You can sign in to <https://console.aws.amazon.com> with your root email/password.
- The console shows a fresh account: no resources, no IAM users, no billing alarms.

## Verification steps

1. Sign in as root.
2. Go to **Billing → Billing Preferences** and turn on:
   - **PDF invoices** (yes)
   - **Alert preferences** (yes) — AWS will email you on unusual spend.
3. Go to **Cost Explorer → Budgets** and create a budget (e.g. **$50/month**, alert at 80%).
   This is not cosmetic — it is your tripwire against surprise charges from step 08 onward.

## Common errors and troubleshooting

| Error | Meaning | Fix |
|---|---|---|
| "Email already in use" | An account exists for that address | Use another address or sign in |
| Card verification fails | Card issuer blocked the charge | Use a different card or contact your bank |
| Phone verification loop | Region/network issue | Retry after 10 minutes; use a different phone if it persists |
| Confirmation email never arrives | Delay in account creation | Wait 30 minutes; check spam; retry sign-in |

## Rollback procedure

If you created the account in error, you can close it only after deleting all paid resources.
Since you have created nothing yet, close via **Account → Close account** after completing step 02
and confirming the platform runs — do **not** close while infrastructure exists.

## Best practices

- Record the root email, the password's location, and the MFA device in your team's credential
  vault immediately.
- Set up a **root account billing alarm** the same day.
- Use a **company email alias**, not a personal one, so access survives employee departures.

## Security notes

- The root user is the most powerful identity in AWS. From here on, **never** sign in as root for
  daily work — step 02 creates your working identity.
- Enable MFA on root before doing anything else: **IAM → Your security credentials → Assign MFA**.
