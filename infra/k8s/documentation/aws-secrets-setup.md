# AWS Secrets Manager Setup for Sub-Engine Project

This comprehensive guide documents the integration between AWS Secrets Manager and your Kubernetes cluster using External Secrets Operator. It provides step-by-step instructions for setting up secure secret management across all microservices in the Sub-Engine platform.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Required Secrets Structure](#required-secrets-structure)
- [Prerequisites](#prerequisites)
- [Detailed Setup Instructions](#detailed-setup-instructions)
- [How External Secrets Work](#how-external-secrets-work)
- [Security Best Practices](#security-best-practices)
- [Troubleshooting Guide](#troubleshooting-guide)
- [Cost Optimization](#cost-optimization)
- [Operational Procedures](#operational-procedures)

## Overview

The Sub-Engine project uses AWS Secrets Manager as central, secure storage for all sensitive configuration data. The External Secrets Operator (ESO) running in your Kubernetes cluster synchronizes these secrets, making them available as standard Kubernetes Secrets that your applications can consume securely. This approach ensures that sensitive information such as database passwords, API keys, and JWT signing keys never need to be stored in your Git repositories or exposed in your deployment manifests.

## Architecture

The secret management flow follows this pattern:

- **AWS Secrets Manager** serves as the source of truth, storing all secrets in encrypted form
- **External Secrets Operator** running in the Kubernetes cluster periodically fetches secrets from AWS
- **Kubernetes Secrets** are automatically created and updated by the operator in the sub-engine namespace
- **Application Pods** consume these secrets through environment variables or mounted volumes

This architecture ensures that:

- Secrets are never stored in Git repositories
- Access to secrets can be audited through AWS CloudTrail
- Secrets can be rotated centrally without updating Kubernetes manifests
- Different environments (dev/staging/prod) can have different secret values

## Required Secrets Structure

Before proceeding with setup, you must create the following secrets in AWS Secrets Manager. Each secret follows a specific JSON structure that the External Secrets Operator expects to find. The property names in these JSON structures must match exactly what is referenced in the ExternalSecret configurations.

### 1. Subscription Database Secret

This secret contains credentials for the PostgreSQL database used by the subscription service.

| Property | Description |
|-----------|-------------|
| Secret Name | `sub-engine/subscription-db` |
| Purpose | Used by subscription-service deployment to authenticate with its dedicated PostgreSQL instance |
| Consumed By | subscription-service deployment, init containers waiting for PostgreSQL |

The secret must be created with the following JSON structure:

```json
{
  "username": "subscription",
  "password": "your-secure-subscription-password"
}
```

**Password Requirements:**
- Minimum 12 characters
- Mix of uppercase, lowercase, numbers, and special characters
- Avoid common words or patterns

### 2. Billing Database Secret

This secret stores access credentials for the billing service's PostgreSQL database.

| Property | Description |
|-----------|-------------|
| Secret Name | `sub-engine/billing-db` |
| Purpose | Provides authentication credentials for billing-service to connect to its PostgreSQL database |
| Consumed By | billing-service deployment, init containers waiting for PostgreSQL |

Required JSON structure:

```json
{
  "username": "billing",
  "password": "your-secure-billing-password"
}
```

### 3. Keycloak Client Secret

This secret holds the client secret for the Keycloak authentication service. This secret is critical for the OAuth2/OIDC authentication flow.

| Property | Description |
|-----------|-------------|
| Secret Name | `sub-engine/keycloak` |
| Purpose | Used by API Gateway and other services to authenticate with Keycloak when validating OAuth2 tokens |
| Consumed By | api-gateway deployment, any service validating JWTs |

Required JSON structure:

```json
{
  "client-secret": "your-keycloak-client-secret"
}
```

**Important:** This client secret must match the secret configured in your Keycloak realm for the api-gateway client. If they don't match, token validation will fail.

### 4. JWT Signature Key

This secret contains the cryptographic key used for signing and validating JSON Web Tokens across all services.

| Property | Description |
|-----------|-------------|
| Secret Name | `sub-engine/jwt` |
| Purpose | Ensures consistent JWT validation across all microservices |
| Consumed By | All microservices (api-gateway, subscription-service, billing-service, worker-service) |

Required JSON structure:

```json
{
  "signature-key": "your-256-bit-secret-key-for-jwt-signature"
}
```

**Critical Requirements:**
- The key must be exactly 256 bits (32 bytes) for the HS256 algorithm
- For production, generate this using a cryptographically secure random generator
- Example generation command: `openssl rand -base64 32`
- Never use a predictable string or dictionary word

## Prerequisites

Before beginning setup, ensure you have the following prerequisites in place:

### AWS Requirements

- Active AWS account with appropriate permissions
- AWS CLI installed and configured (`aws configure`)
- Permissions to create IAM policies and users
- Permissions to create and manage Secrets Manager secrets

### Kubernetes Requirements

- Running Kubernetes cluster (EKS recommended for production)
- kubectl configured with cluster admin access
- Helm 3 installed on your local machine
- Basic understanding of Kubernetes Secrets and custom resources

### Local Development Tools

```bash
# Verify AWS CLI is installed and configured
aws sts get-caller-identity

# Verify kubectl is configured
kubectl cluster-info

# Verify Helm is installed
helm version
```

## Detailed Setup Instructions

### Step 1: Create Secrets in AWS Secrets Manager

First, create all required secrets in AWS Secrets Manager. You can do this through the AWS Console, AWS CLI, or infrastructure as code tools like Terraform. The examples below use the AWS CLI.

**Important:** Choose the appropriate AWS region based on your infrastructure. Replace `us-east-1` with your region in all commands.

#### Create Subscription Database Secret:

```bash
aws secretsmanager create-secret \
  --name sub-engine/subscription-db \
  --description "PostgreSQL credentials for subscription service" \
  --secret-string '{"username":"subscription","password":"your-secure-subscription-password"}' \
  --region us-east-1
```

#### Create Billing Database Secret:

```bash
aws secretsmanager create-secret \
  --name sub-engine/billing-db \
  --description "PostgreSQL credentials for billing service" \
  --secret-string '{"username":"billing","password":"your-secure-billing-password"}' \
  --region us-east-1
```

#### Create Keycloak Client Secret:

```bash
aws secretsmanager create-secret \
  --name sub-engine/keycloak \
  --description "Client secret for Keycloak authentication" \
  --secret-string '{"client-secret":"your-keycloak-client-secret"}' \
  --region us-east-1
```

#### Create JWT Signature Key:

```bash
# First generate a cryptographically secure random key
JWT_KEY=$(openssl rand -base64 32)

aws secretsmanager create-secret \
  --name sub-engine/jwt \
  --description "256-bit key for JWT signing and validation" \
  --secret-string "{\"signature-key\":\"${JWT_KEY}\"}" \
  --region us-east-1
```

### Step 2: Install External Secrets Operator

The External Secrets Operator must be installed in your Kubernetes cluster before any secrets can be synchronized. It creates custom resource definitions (CRDs) that extend the Kubernetes API.

#### Add External Secrets Helm repository:

```bash
helm repo add external-secrets https://charts.external-secrets.io
```

#### Update your Helm repositories to ensure you have the latest chart versions:

```bash
helm repo update
```

#### Install External Secrets Operator in a dedicated namespace:

```bash
helm install external-secrets \
  external-secrets/external-secrets \
  --namespace external-secrets \
  --create-namespace \
  --set installCRDs=true \
  --set webhook.port=9443
```

#### Verify installation by checking that all pods are running:

```bash
kubectl get pods -n external-secrets
```

Expected output:

```
NAME                                                READY   STATUS    RESTARTS   AGE
external-secrets-xxx-yyy                            1/1     Running   0          2m
external-secrets-cert-controller-xxx-yyy             1/1     Running   0          2m
external-secrets-webhook-xxx-yyy                     1/1     Running   0          2m
```

### Step 3: Create IAM Policy

The External Secrets Operator needs permission to read secrets from AWS Secrets Manager. Create an IAM policy with the minimum required permissions. This follows the principle of least privilege, granting access only to the specific secrets needed by your application.

Create a file named `external-secrets-policy.json` with the following content. Replace the account ID `123456789012` with your actual AWS account ID:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "arn:aws:secretsmanager:us-east-1:123456789012:secret:sub-engine/subscription-db*",
        "arn:aws:secretsmanager:us-east-1:123456789012:secret:sub-engine/billing-db*",
        "arn:aws:secretsmanager:us-east-1:123456789012:secret:sub-engine/keycloak*",
        "arn:aws:secretsmanager:us-east-1:123456789012:secret:sub-engine/jwt*"
      ]
    }
  ]
}
```

**Important Notes about the Policy:**

- The asterisk (`*`) at the end of each secret ARN is crucial as it grants access to all versions of the secret, which is necessary for AWS Secrets Manager's versioning system
- Replace `us-east-1` with your specific AWS region if different
- Replace the account ID with your actual AWS account number
- The policy grants only read-only access (`GetSecretValue` and `DescribeSecret`), which is all the operator needs

#### Create the IAM policy using the AWS CLI:

```bash
aws iam create-policy \
  --policy-name ExternalSecretsPolicy \
  --policy-document file://external-secrets-policy.json
```

**Note:** Save the Policy ARN returned by this command. It will look like `arn:aws:iam::123456789012:policy/ExternalSecretsPolicy`.

### Step 4: Create an IAM User and Generate Access Keys

For environments where you cannot use IAM roles (such as non-EKS clusters), create a dedicated IAM user for the External Secrets Operator:

```bash
aws iam create-user --user-name external-secrets-sync
```

#### Attach the policy you created earlier to this user:

```bash
aws iam attach-user-policy \
  --user-name external-secrets-sync \
  --policy-arn arn:aws:iam::123456789012:policy/ExternalSecretsPolicy
```

#### Generate access keys for this user:

```bash
aws iam create-access-key --user-name external-secrets-sync
```

This command returns a JSON response similar to:

```json
{
  "AccessKey": {
    "UserName": "external-secrets-sync",
    "AccessKeyId": "AKIAIOSFODNN7EXAMPLE",
    "Status": "Active",
    "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
    "CreateDate": "2024-01-01T00:00:00Z"
  }
}
```

**CRITICAL SECURITY WARNING:**

- Save both the Access Key ID and Secret Access Key immediately in a secure password manager
- The Secret Access Key cannot be retrieved again after you close this session
- Never share these keys or commit them to version control
- Restrict access to these keys to only the personnel who need them

### Step 5: Create AWS Credentials Secret in Kubernetes

The External Secrets Operator needs AWS credentials to authenticate with Secrets Manager. These credentials must be stored as a Kubernetes Secret in your cluster.

#### First, encode your AWS credentials to base64. Run these commands on your local machine, replacing the placeholder values with your actual credentials:

```bash
# Encode Access Key ID
echo -n "YOUR_AWS_ACCESS_KEY_ID" | base64
# Output: YOUR_AWS_ACCESS_KEY_ID=

# Encode Secret Access Key
echo -n "YOUR_AWS_SECRET_ACCESS_KEY" | base64
# Output: YOUR_AWS_SECRET_ACCESS_KEY=
```

#### Create the Kubernetes secret using either method below:

**Method 1: Using kubectl create (recommended for first-time setup)**

```bash
kubectl create secret generic aws-credentials \
  --namespace sub-engine \
  --from-literal=access-key-id=AKIAIOSFODNN7EXAMPLE \
  --from-literal=secret-access-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

**Method 2: Using a YAML file (for documentation or GitOps)**

Create a file named `aws-credentials-secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: aws-credentials
  namespace: sub-engine
type: Opaque
data:
  access-key-id: YOUR_AWS_ACCESS_KEY_ID
  secret-access-key: YOUR_AWS_SECRET_ACCESS_KEY
```

Apply the secret:

```bash
kubectl apply -f aws-credentials-secret.yaml
```

**IMPORTANT SECURITY NOTE:**

- Never commit the `aws-credentials-secret.yaml` file to Git, even with dummy values
- Add this file pattern to your `.gitignore`: `*aws-credentials*.yaml`
- The base64 encoding is not encryption; anyone with access to your cluster can decode these values

### Step 6: Apply External Secrets Configuration

With the operator installed and credentials in place, you can now apply the External Secrets configuration. The `external-secrets.yaml` file contains:

- A `SecretStore` that tells ESO how to connect to AWS
- Four `ExternalSecret` resources that define which secrets to synchronize

Ensure the `external-secrets.yaml` file exists in your current directory, then apply it:

```bash
kubectl apply -f external-secrets.yaml
```

This command creates all the necessary resources in the sub-engine namespace.

### Step 7: Verify Secret Synchronization

After applying the configuration, verify that all resources were created successfully and that secrets are being synchronized.

#### Check the ExternalSecret resources to ensure they are ready:

```bash
kubectl get externalsecret -n sub-engine
```

Expected output:

```
NAME                      READY   STATUS         AGE
subscription-db-secret     True    SecretSynced   1m
billing-db-secret          True    SecretSynced   1m
keycloak-client-secret     True    SecretSynced   1m
jwt-signature-secret       True    SecretSynced   1m
```

#### List all Kubernetes secrets in the namespace to confirm they were created:

```bash
kubectl get secrets -n sub-engine | grep -E "db-secret|jwt|keycloak"
```

#### For debugging purposes, you can examine the contents of a specific secret (note that values are base64-encoded):

```bash
kubectl get secret subscription-db-secret -n sub-engine -o yaml
```

#### To decode and verify a specific value:

```bash
# Decode username
kubectl get secret subscription-db-secret -n sub-engine \
  -o jsonpath='{.data.username}' | base64 -d

# Decode password
kubectl get secret subscription-db-secret -n sub-engine \
  -o jsonpath='{.data.password}' | base64 -d
```

These commands should output the exact values you stored in AWS Secrets Manager.

### Step 8: Deploy Applications

With the secrets now available in your cluster, you can deploy the applications that consume them. The deployment manifests reference these secrets using the `valueFrom.secretKeyRef` syntax.

Apply your deployment configuration:

```bash
kubectl apply -f deployment.yaml
```

Verify that all pods start successfully:

```bash
kubectl get pods -n sub-engine -w
```

## How External Secrets Work

Understanding the flow of secrets helps with troubleshooting and security auditing. Here's a detailed explanation of the synchronization process:

### The Synchronization Cycle

#### Initial Creation Phase:

1. You manually create secrets in AWS Secrets Manager with the specified JSON structures
2. You define `ExternalSecret` custom resources in Kubernetes that reference these AWS secrets
3. The External Secrets Operator detects the new `ExternalSecret` resources

#### Authentication Phase:

1. The operator authenticates with AWS using the credentials provided in the `SecretStore`
2. It assumes the IAM role or uses the access keys to establish a secure connection
3. All communication is encrypted via TLS

#### Fetch Phase:

1. The operator calls `secretsmanager:GetSecretValue` for each referenced secret
2. AWS returns the secret value (still encrypted in transit)
3. The operator extracts the specific properties requested (like `username`, `password`)

#### Kubernetes Secret Creation Phase:

1. The operator creates or updates standard Kubernetes Secrets in your cluster
2. Values are base64-encoded as required by the Kubernetes Secret format
3. The secrets are created in the specified namespace

#### Consumption Phase:

1. Your application pods reference these Kubernetes Secrets in their deployment configuration
2. When pods start, kubelet injects the secret values as environment variables or mounted files
3. Applications never need to know about AWS or the External Secrets Operator

#### Refresh Phase:

1. The operator periodically refreshes secrets based on the `refreshInterval` (default 1 hour)
2. If a secret changes in AWS, the Kubernetes Secret is automatically updated
3. **Note:** Running pods continue using the old values until restarted, unless your application supports dynamic reloading

### Resource Relationships Diagram

```
AWS Secrets Manager
       ↑
   │ (1) References
   │
ExternalSecret (CRD) ──┐
   │               │
   │ (2) Uses       │ (3) References
   ↓               ↓
SecretStore (CRD)   Kubernetes Secret
   │                   ↑
   │ (4) Provides       │ (5) Mounts/Injects
   │   credentials      │
   ↓                   │
aws-credentials         Pod (application)
(K8s Secret)
```

## Security Best Practices

### Protecting AWS Credentials

The AWS credentials used by the External Secrets Operator are highly sensitive. Follow these practices to maintain security:

#### 1. Use IAM Roles when possible:

In EKS environments, use IAM Roles for Service Accounts (IRSA) instead of static credentials. This eliminates the need to store any AWS credentials in your cluster:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ClusterSecretStore
metadata:
  name: aws-secretsmanager
spec:
  provider:
    aws:
      service: SecretsManager
      region: us-east-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets-sa
            namespace: external-secrets
```

#### 2. Rotate credentials regularly:

Implement a process to rotate IAM user access keys every 90 days:

```bash
# Create new access key
aws iam create-access-key --user-name external-secrets-sync

# Update Kubernetes secret with new key
kubectl edit secret aws-credentials -n sub-engine

# Verify the new key works
kubectl rollout restart deployment/external-secrets -n external-secrets

# Deactivate and remove old key
aws iam update-access-key --user-name external-secrets-sync \
  --access-key-id OLD_ACCESS_KEY_ID --status Inactive
aws iam delete-access-key --user-name external-secrets-sync \
  --access-key-id OLD_ACCESS_KEY_ID
```

#### 3. Limit network exposure:

- Use VPC endpoints for Secrets Manager to keep all traffic within your VPC
- Never allow Secrets Manager access from the public internet
- Implement network policies in Kubernetes to restrict pod-to-AWS communication

### Secret Rotation Strategy

Automatic secret rotation enhances security by limiting the lifespan of any compromised credential:

#### 1. Enable automatic rotation for database secrets:

```bash
aws secretsmanager rotate-secret \
  --secret-id sub-engine/subscription-db \
  --rotation-lambda-arn arn:aws:lambda:us-east-1:123456789012:function:rotate-postgres-secret \
  --rotation-rules AutomaticallyAfterDays=30
```

#### 2. For JWT signing keys:

JWT keys require careful rotation to avoid invalidating existing tokens:

- Implement key versioning in your application
- Support multiple valid keys during rotation periods
- Rotate keys every 90-180 days depending on security requirements

#### 3. For Keycloak client secrets:

- Coordinate rotation with Keycloak configuration updates
- Implement a grace period where both old and new secrets are accepted
- Update client applications to use the new secret before removing the old one

### Audit and Monitoring

#### Enable CloudTrail logging:

```bash
aws cloudtrail create-trail \
  --name secrets-manager-trail \
  --s3-bucket-name your-audit-logs-bucket \
  --is-multi-region-trail
```

#### Monitor for suspicious access:

Set up CloudWatch alarms for:

- Failed secret access attempts
- Access from unexpected IP addresses
- Unusual access patterns (high volume, odd times)

#### Kubernetes audit logging:

Enable Kubernetes audit logging to track access to secrets:

```bash
# In your kube-apiserver configuration
--audit-policy-file=/etc/kubernetes/audit-policy.yaml
--audit-log-path=/var/log/kubernetes/audit.log
```

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue 1: ExternalSecret Not Syncing

**Symptoms:**
- `kubectl get externalsecret` shows `READY=False`
- Secrets not appearing in the namespace

**Diagnostic Steps:**

##### Check the ExternalSecret status:

```bash
kubectl describe externalsecret subscription-db-secret -n sub-engine
```

Look for events and conditions at the bottom of the output. Common messages include:

- `Secret does not exist in AWS` - The secret name in AWS doesn't match
- `AccessDeniedException` - IAM permissions issue
- `context deadline exceeded` - Network connectivity issue

##### Check the operator logs:

```bash
kubectl logs -n external-secrets -l app.kubernetes.io/name=external-secrets
```

**Solutions:**

##### If the secret is not found in AWS:

- Verify the exact secret name in the AWS Console
- Check the AWS region configuration in the `SecretStore`
- Ensure the secret ARN in the IAM policy matches exactly

##### If access is denied:

- Verify the IAM policy is attached to the user/role
- Check that the policy includes the correct resource ARNs
- Test AWS CLI access manually

#### Issue 2: Pods Cannot Read Secrets

**Symptoms:**
- Pods fail to start with `Error: secret "xyz" not found`
- Environment variables are empty

**Diagnostic Steps:**

##### Verify the secret exists:

```bash
kubectl get secret subscription-db-secret -n sub-engine
```

##### Check if the secret has the expected keys:

```bash
kubectl describe secret subscription-db-secret -n sub-engine
```

##### Verify the deployment references the correct secret:

```bash
kubectl get deployment subscription-service -n sub-engine -o yaml | grep -A5 -B5 secretKeyRef
```

**Solutions:**

##### If the secret doesn't exist:

- Check the ExternalSecret status
- Wait for synchronization (up to the `refreshInterval`)
- Force an immediate sync by deleting and recreating the ExternalSecret

##### If the secret has wrong keys:

- Verify the JSON structure in AWS Secrets Manager
- Ensure the property names match (e.g., `username` vs `user`)

#### Issue 3: AWS Authentication Issues

**Symptoms:**
- ExternalSecret status shows `AuthenticationFailed`
- Operator logs contain AWS authentication errors

**Diagnostic Steps:**

##### Verify the AWS credentials secret exists and has the correct keys:

```bash
kubectl get secret aws-credentials -n sub-engine -o yaml
```

##### Test credentials manually (from a pod with AWS CLI):

```bash
kubectl run -it --rm aws-test --image=amazon/aws-cli --restart=Never -- \
  aws secretsmanager get-secret-value --secret-id sub-engine/subscription-db --region us-east-1
```

**Solutions:**

##### If the credentials are invalid:

- Generate new access keys in AWS IAM
- Update the Kubernetes secret with new base64-encoded values
- Restart the external-secrets pod

##### If there's a region mismatch:

- Verify the region in the `SecretStore` matches where the secrets are stored
- Update the `SecretStore` region if necessary

## Cost Optimization

Understanding the costs associated with this solution helps you optimize for your specific use case.

### External Secrets Operator Costs

The External Secrets Operator is open-source and completely free to use:
- No licensing costs, regardless of the number of secrets or clusters

### AWS Secrets Manager Costs

AWS Secrets Manager pricing is based on two factors:

#### Secret Storage Cost: $0.40 per secret per month

- Each secret you create incurs this cost
- With 4 secrets, monthly cost: $1.60

#### API Call Costs: $0.05 per 10,000 API calls

- Each `GetSecretValue` call counts as one API call
- With a `refreshInterval` of 1 hour and 4 secrets:
  - Daily calls: 4 secrets × 24 hours = 96 calls
  - Monthly calls: 2,880 calls
  - Monthly cost: ~$0.014

**Total Estimated Monthly Cost: ~$1.62**

### Cost Optimization Strategies

#### Adjust the refreshInterval for non-critical secrets:

```yaml
spec:
  refreshInterval: 24h  # Check daily instead of hourly
```

#### Use secret versioning instead of creating new secrets:

- Update existing secrets rather than creating new ones

#### Consider development vs production:

- **Production:** Use AWS Secrets Manager with appropriate rotation
- **Development:** Use Kubernetes secrets directly to save costs

## Operational Procedures

### Routine Maintenance Tasks

#### Weekly:

- Verify all ExternalSecrets are synchronized
- Check operator logs for errors or warnings
- Review AWS CloudTrail for unusual access patterns

#### Monthly:

- Review IAM policies for least privilege
- Check secret rotation status
- Verify backup and recovery procedures

#### Quarterly:

- Rotate AWS access keys for the external-secrets-sync user
- Review and update incident response procedures
- Conduct a security audit of secret access

### Disaster Recovery

If you lose access to your cluster or need to recover in a new environment:

#### Reinstall External Secrets Operator:

```bash
helm install external-secrets external-secrets/external-secrets -n external-secrets
```

#### Recreate AWS credentials secret:

```bash
kubectl create secret generic aws-credentials -n sub-engine \
  --from-literal=access-key-id=YOUR_ACCESS_KEY \
  --from-literal=secret-access-key=YOUR_SECRET_KEY
```

#### Reapply ExternalSecret configurations:

```bash
kubectl apply -f external-secrets.yaml
```

#### Verify synchronization:

```bash
kubectl get externalsecret -n sub-engine
```

### Upgrading External Secrets Operator

When upgrading the operator:

#### Backup existing configurations:

```bash
kubectl get externalsecret -A -o yaml > externalsecrets-backup.yaml
kubectl get secretstore -A -o yaml > secretstore-backup.yaml
```

#### Upgrade via Helm:

```bash
helm upgrade external-secrets external-secrets/external-secrets \
  -n external-secrets \
  --version <new-version>
```

#### Verify upgrade:

```bash
kubectl get pods -n external-secrets
kubectl get crd | grep external-secrets
```

#### Test synchronization:

Force a refresh and verify secrets are still accessible:

```bash
kubectl annotate externalsecret subscription-db-secret force-sync=true
```

## Support and Resources

- [External Secrets Operator Documentation](https://external-secrets.io/)
- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/)
- [IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [Kubernetes Secrets Documentation](https://kubernetes.io/docs/concepts/configuration/secret/)
