# Tender classification, bidder onboarding, and documents

## Local MinIO setup

The `dev` Spring profile selects the S3-compatible storage adapter and points at MinIO; AWS credentials are not read in this mode. From the repository root:

```powershell
docker compose up -d postgres minio minio-init
```

MinIO console: `http://localhost:9001`. Local defaults are `minio-local` / `minio-local-secret-change-me`; change them in the shell environment for shared environments. Start the backend with `./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev,ai"`. The development profile creates/uses the private `procurement-private` bucket through the Compose initializer. Local uploads validate PDF signatures and enable download for workflow testing; this is not malware scanning. The default/non-dev configuration requires malware clearance and keeps objects quarantined until a scanner integration is configured.

The local round trip uses a bidder access token:

```powershell
curl.exe -H "Authorization: Bearer $TOKEN" -F "file=@registration.pdf" http://localhost:8080/api/v1/bidder/documents/registration
curl.exe -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/bidder/documents
curl.exe -H "Authorization: Bearer $TOKEN" -o downloaded.pdf http://localhost:8080/api/v1/bidder/documents/{documentId}/content
```

Bid attachments use `POST /api/v1/bids/{bidId}/documents/{technical|financial|compliance}` and authenticated download uses `GET /api/v1/bids/{bidId}/documents/{documentId}/content`. Officer access is tenant-scoped and withheld until the tender deadline; financial files are excluded before opening. Object names are generated from organization, category, document UUID, and immutable version UUID. User filenames are metadata only.

## AWS production setup (manual)

No AWS resource was created. An AWS account administrator must:

1. Choose the target account and region, register/prepare the existing workload role (ECS task role, EKS IRSA/workload identity, or EC2 instance profile), and ensure the deployment obtains credentials through that role. Do not put AWS access keys in application configuration.
2. Provide a globally unique bucket name and the existing role name and ARN. The Terraform principal needs permission to manage S3 bucket configuration/policy/versioning/lifecycle, KMS key/alias, IAM policy, and attach the policy to that existing role. Restrict Terraform access to the intended account and use an approved remote state backend with locking.
3. From `infra/aws-documents`, configure the AWS CLI profile/role, copy `terraform.tfvars.example` to `terraform.tfvars`, set the bucket and role values, then run `terraform init`, inspect `terraform plan`, and only after organizational approval run `terraform apply`. This repository does not run `apply`.
4. Set deployment environment values: `STORAGE_PROVIDER=s3`, `DOCUMENT_BUCKET` to the Terraform bucket output, `AWS_REGION`, `S3_KMS_KEY_ID` to the Terraform KMS key ARN, `S3_ENDPOINT` empty, `S3_ACCESS_KEY` empty, `S3_SECRET_KEY` empty, and `DOCUMENT_MALWARE_SCAN_REQUIRED=true`. Ensure the workload identity role is attached to the workload. `DefaultCredentialsProvider` uses the platform's role credentials.
5. Configure and operate an approved malware scanner before allowing document processing/download. At present production uploads remain `QUARANTINED`; an antivirus scanner and promotion worker are an explicit integration gap. Configure TLS at the service boundary and restrict network egress to the S3 regional endpoint.

Terraform provisions a private, versioned S3 bucket, all public-access blocks, bucket-owner enforced ownership, KMS encryption with rotation, TLS-only bucket policy, incomplete multipart cleanup, an application least-privilege policy scoped to `organizations/*`, and attaches it to the existing app role. The application still enforces organization and bid-stage checks before authenticated downloads; IAM prefix access alone does not authorize a user.

## Storage configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `STORAGE_PROVIDER` | `local` (default profile), `s3` (`dev`) | Select storage adapter without application code changes. |
| `APP_UPLOAD_DIR` | `./private-uploads` | Private filesystem adapter root; never serve this directory from the web server. |
| `DOCUMENT_BUCKET` | `procurement-private` | Bucket/container identifier. |
| `AWS_REGION` | `ap-south-1` (MinIO overrides to `us-east-1`) | S3 signing region. |
| `S3_ENDPOINT` | empty, MinIO in `dev` | Non-AWS S3-compatible endpoint; blank for AWS S3. |
| `S3_ACCESS_KEY`, `S3_SECRET_KEY` | empty; local MinIO defaults in `dev` | Used only when an endpoint override is configured. Leave unset in AWS. |
| `S3_KMS_KEY_ID` | empty | Optional KMS key for per-request SSE-KMS; AWS bucket default encryption is KMS regardless. |
| `DOCUMENT_MAX_FILE_SIZE_BYTES` | `26214400` | Maximum simple multipart upload size (25 MiB). |
| `DOCUMENT_MALWARE_SCAN_REQUIRED` | `true` | Quarantine uploaded PDFs unless a scanner promotes them; dev profile defaults false for local round-trip testing. |

## Classification API and operation

- `POST /api/v1/tenders/{id}/classify` enqueues an idempotent job and returns `202`.
- `GET /api/v1/tenders/{id}/classification` returns status, model version, score/relevance, and predictions.
- `PUT /api/v1/tenders/{id}/classification/confirm` accepts `{ "sectors": ["IT_SOFTWARE", "HEALTHCARE_IT"] }`. Admins and procurement officers scoped to the tender authority may confirm.
- `POST /api/v1/tenders/{id}/reclassify` creates a fresh job.
- `GET /api/v1/bidder/tenders/recommended?page=0&size=20&sector=IT_SOFTWARE&search=pacs` uses only confirmed sectors, bidder sectors, and related taxonomy links. A recommendation is not an eligibility determination.
- `GET/PUT /api/v1/bidder/onboarding` reads/saves company, sector, capability, turnover, signatory, and onboarding fields.
- `GET /api/v1/bidder/documents/checklist` reports required/conditional document categories.
- Registration document uploads are under `/api/v1/bidder/documents`; see `OrganizationDocumentController` for category-specific routes. Replacement creates a new immutable object and retains the old version metadata.

`CLASSIFICATION_PROVIDER=groq` enables the structured JSON Groq classifier and requires `GROQ_API_KEY`; default `rules` uses deterministic taxonomy rules. Model relevance scores are uncalibrated and require procurement-officer review. PostgreSQL jobs are claimed with `FOR UPDATE SKIP LOCKED`, retried with exponential backoff, and marked `FAILED` after five attempts. `app.classification.poll-ms` controls the single bounded worker interval.

## Current gaps

There is no malware scanning service, OCR workflow, direct-to-S3 presigned multipart endpoint, approved e-signature integration, sealed-envelope encryption/key-release workflow, retention policy, or automated cleanup for abandoned single-part objects. The app currently validates PDF signature, type, size, checksum, authenticated ownership, and storage stage. The production profile correctly blocks access until scanning is added. The classifier currently uses title, description, and structured tender requirements; extracting tender-document text for classification is not yet implemented. The onboarding schema includes configurable rule rows, but jurisdiction-specific rule administration and policy interpretation require a government-approved ruleset and still need a management API.
