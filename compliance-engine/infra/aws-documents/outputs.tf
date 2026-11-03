output "bucket_name" {
  value = aws_s3_bucket.documents.bucket
}
output "kms_key_arn" {
  value = aws_kms_key.documents.arn
}
output "application_policy_arn" {
  value = aws_iam_policy.application_documents.arn
}
