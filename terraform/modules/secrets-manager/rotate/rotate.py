#!/usr/bin/env python3
"""Secrets Manager rotation Lambda for plain string secrets.

Implements createSecret / setSecret / testSecret / finishSecret for secrets
whose value is a single random string (e.g. the JWT signing key). Consumers
pick up the new value on their next configuration refresh/restart.
"""

import string
import random

import boto3

REGION = "us-east-1"
secrets = boto3.client("secretsmanager", region_name=REGION)

_CHARS = string.ascii_letters + string.digits + "!#$%&*()-_=+[]{}:?"


def _new_value():
    return "".join(random.SystemRandom().choice(_CHARS) for _ in range(48))


def lambda_handler(event, _context):
    arn = event.get("SecretId")
    token = event.get("ClientRequestToken")
    step = event.get("Step")

    metadata = secrets.describe_secret(SecretId=arn)
    if token not in metadata.get("VersionIdsToStages", {}):
        raise ValueError("Invalid request token")

    if step == "createSecret":
        secrets.put_secret_value(
            SecretId=arn,
            ClientRequestToken=token,
            SecretString=_new_value(),
            VersionStages=["AWSPENDING"],
        )
    elif step == "setSecret":
        # String secrets need no downstream apply.
        pass
    elif step == "testSecret":
        # Validate the pending version can be read back.
        secrets.get_secret_value(SecretId=arn, VersionStage="AWSPENDING")
    elif step == "finishSecret":
        secrets.update_secret_version_stage(
            SecretId=arn,
            VersionStage="AWSCURRENT",
            MoveToVersionId=token,
        )
    else:
        raise ValueError(f"Invalid step: {step}")

    return {"statusCode": 200}
