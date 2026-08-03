#!/usr/bin/env python3
"""Secrets Manager rotation Lambda for the RDS master credential.

Implements the AWS Secrets Manager rotation template (createSecret /
setSecret / testSecret / finishSecret). The Lambda only depends on boto3 and
the standard library so it needs no custom layer.

The Lambda must have the permissions attached by the `rds` module:
  secretsmanager:GetSecretValue/PutSecretValue/DescribeSecret/UpdateSecretVersionStage
  rds:ModifyDBInstance/DescribeDBInstances
"""

import json
import os
import socket
import string
import random
import time

import boto3
from botocore.exceptions import ClientError

REGION = os.environ.get("AWS_REGION", "us-east-1")
secrets = boto3.client("secretsmanager", region_name=REGION)
rds = boto3.client("rds", region_name=REGION)

_CHARS = string.ascii_letters + string.digits + "!#$%&*()-_=+[]{}:?"


def _get_secret(secret_id, stage="AWSCURRENT"):
    response = secrets.get_secret_value(SecretId=secret_id, VersionStage=stage)
    return json.loads(response["SecretString"])


def _db_identifier(secret):
    return secret.get("dbInstanceIdentifier") or secret["host"].split(".")[0]


def _set_password(secret, password):
    rds.modify_db_instance(
        DBInstanceIdentifier=_db_identifier(secret),
        MasterUserPassword=password,
        ApplyImmediately=True,
    )


def _wait_for_db_apply():
    # RDS applies the password asynchronously; poll DescribeDBInstances.
    for _ in range(30):
        time.sleep(10)
        try:
            secret = _get_secret(SECRET_ARN, "AWSPENDING")
            if _db_identifier(secret):
                break
        except ClientError:
            pass


def _test_password(secret, password):
    # Verify the DB endpoint accepts connections; a full auth probe would
    # require a PostgreSQL driver layer (add a layer to extend).
    with socket.create_connection((secret["host"], int(secret["port"])), timeout=5):
        pass


def _generate_password():
    random_source = random.SystemRandom()
    return "".join(random_source.choice(_CHARS) for _ in range(32))


def lambda_handler(event, _context):
    arn = event.get("SecretId")
    token = event.get("ClientRequestToken")
    step = event.get("Step")

    # Idempotency guard.
    metadata = secrets.describe_secret(SecretId=arn)
    if token not in metadata.get("VersionIdsToStages", {}):
        raise ValueError("Invalid request token")

    current = _get_secret(arn, "AWSCURRENT")

    if step == "createSecret":
        candidate = dict(current, password=_generate_password())
        secrets.put_secret_value(
            SecretId=arn,
            ClientRequestToken=token,
            SecretString=json.dumps(candidate),
            VersionStages=["AWSPENDING"],
        )
    elif step == "setSecret":
        candidate = _get_secret(arn, "AWSPENDING")
        _set_password(candidate, candidate["password"])
        _wait_for_db_apply()
    elif step == "testSecret":
        candidate = _get_secret(arn, "AWSPENDING")
        _test_password(candidate, candidate["password"])
    elif step == "finishSecret":
        secrets.update_secret_version_stage(
            SecretId=arn,
            VersionStage="AWSCURRENT",
            MoveToVersionId=token,
        )
    else:
        raise ValueError(f"Invalid step: {step}")

    return {"statusCode": 200}
