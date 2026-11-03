variable "aws_region" {
  type    = string
  default = "ap-south-1"
}
variable "name_prefix" {
  type    = string
  default = "procurement"
}
variable "bucket_name" {
  type        = string
  description = "Globally unique S3 bucket name for this environment."
}
variable "application_role_name" {
  type        = string
  description = "Existing IAM role used by the deployed Spring Boot workload."
}
variable "application_role_arn" {
  type        = string
  description = "ARN of the same existing workload role; granted KMS key use."
}
variable "tags" {
  type    = map(string)
  default = { Application = "government-procurement", DataClassification = "sensitive" }
}
