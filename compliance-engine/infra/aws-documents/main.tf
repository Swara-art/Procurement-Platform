terraform {
  required_version = ">= 1.6.0"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.80" }
  }
}

provider "aws" { region = var.aws_region }

resource "aws_kms_key" "documents" {
  description             = "Encryption key for private procurement documents"
  enable_key_rotation     = true
  deletion_window_in_days = 30
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      { Sid = "AccountAdministration", Effect = "Allow", Principal = { AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root" }, Action = "kms:*", Resource = "*" },
      { Sid = "ApplicationUse", Effect = "Allow", Principal = { AWS = var.application_role_arn }, Action = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey", "kms:DescribeKey"], Resource = "*" }
    ]
  })
}
resource "aws_kms_alias" "documents" {
  name          = "alias/${var.name_prefix}-documents"
  target_key_id = aws_kms_key.documents.key_id
}
data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "documents" {
  bucket        = var.bucket_name
  force_destroy = false
  tags          = var.tags
}
resource "aws_s3_bucket_public_access_block" "documents" {
  bucket                  = aws_s3_bucket.documents.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
resource "aws_s3_bucket_ownership_controls" "documents" {
  bucket = aws_s3_bucket.documents.id
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}
resource "aws_s3_bucket_versioning" "documents" {
  bucket = aws_s3_bucket.documents.id
  versioning_configuration {
    status = "Enabled"
  }
}
resource "aws_s3_bucket_server_side_encryption_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.documents.arn
    }
    bucket_key_enabled = true
  }
}
resource "aws_s3_bucket_lifecycle_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id
  rule {
    id = "abort-incomplete-multipart-uploads"
    status = "Enabled"
    filter {
      prefix = ""
    }
    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}
resource "aws_s3_bucket_policy" "documents" {
  bucket = aws_s3_bucket.documents.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      { Sid = "DenyInsecureTransport", Effect = "Deny", Principal = "*", Action = "s3:*", Resource = [aws_s3_bucket.documents.arn, "${aws_s3_bucket.documents.arn}/*"], Condition = { Bool = { "aws:SecureTransport" = "false" } } }
    ]
  })
  depends_on = [aws_s3_bucket_public_access_block.documents]
}
resource "aws_iam_policy" "application_documents" {
  name = "${var.name_prefix}-documents"
  description = "Least-privilege object access for procurement document service"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      { Sid = "ListOrganizationObjects", Effect = "Allow", Action = ["s3:ListBucket"], Resource = aws_s3_bucket.documents.arn, Condition = { StringLike = { "s3:prefix" = ["organizations/*"] } } },
      { Sid = "ManageOrganizationObjects", Effect = "Allow", Action = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:AbortMultipartUpload"], Resource = "${aws_s3_bucket.documents.arn}/organizations/*" },
      { Sid = "UseDocumentEncryptionKey", Effect = "Allow", Action = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey", "kms:DescribeKey"], Resource = aws_kms_key.documents.arn }
    ]
  })
}
resource "aws_iam_role_policy_attachment" "application_documents" {
  role       = var.application_role_name
  policy_arn = aws_iam_policy.application_documents.arn
}
