# AWS reference deployment (no resources provisioned)

Use Route 53 and ACM in front of an AWS WAF-protected Application Load Balancer. The ALB reaches only the web and API Gateway workloads in Amazon EKS private application subnets. Internal services remain ClusterIP-only. Use IAM roles for service accounts and least-privilege security groups.

Use Multi-AZ Amazon RDS for PostgreSQL with separate service databases, Amazon MSK for Kafka, and ElastiCache for Redis. Store statement objects in private versioned S3 buckets encrypted with customer-managed KMS keys. Resolve runtime credentials from Secrets Manager through External Secrets or the Secrets Store CSI driver; never place them in images or Helm values. Use ECR immutable tags and vulnerability scanning.

Export telemetry to the OpenTelemetry Collector and then CloudWatch, Amazon Managed Service for Prometheus/Grafana, or another approved backend. Encrypt databases, MSK, Redis, EBS, S3, backups, and network traffic. Retain tested point-in-time recovery and cross-region backup copies according to policy.

Deployment sequence: provision network/endpoints, managed data services, secrets/KMS, EKS add-ons and observability, databases/migrations, internal services, gateway/web, then DNS. Deploy with rolling updates and readiness gates. Roll back to the prior immutable ECR tag; prefer forward-only database fixes. Development can use a single-AZ small RDS instance, serverless MSK alternatives, and shorter telemetry retention, but must retain encryption and private networking.
