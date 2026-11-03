export type TenderStatus = 'DRAFT' | 'PUBLISHED' | 'EVALUATING' | 'CLOSED';

export interface Tender {
  id: string;
  tenderNumber: string;
  title: string;
  description: string;
  procurementAuthorityId: string;
  estimatedValue: number;
  status: TenderStatus | string;
  submissionDeadline: string;
  createdAt: string;
  // UI helpers
  authorityName?: string;
}

export interface CreateTenderRequest {
  tenderNumber: string;
  title: string;
  description: string;
  procurementAuthorityId: string;
  estimatedValue: number;
  submissionDeadline: string;
}

export type RequirementOperator = 
  | 'EQUALS'
  | 'GREATER_THAN'
  | 'GREATER_THAN_OR_EQUAL'
  | 'LESS_THAN'
  | 'LESS_THAN_OR_EQUAL'
  | 'EXISTS'
  | 'IS_VALID';

export interface TenderRequirement {
  id: string;
  tenderId: string;
  requirementCode: string;
  description: string;
  operator: RequirementOperator | string;
  expectedValue: string;
  mandatory: boolean;
  createdAt: string;
}

export interface CreateTenderRequirementRequest {
  requirementCode: string;
  description: string;
  operator: RequirementOperator | string;
  expectedValue: string;
  mandatory: boolean;
}

export type BidStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'EVALUATED' | 'ACCEPTED' | 'REJECTED';

export interface Bid {
  id: string;
  tenderId: string;
  bidderProfileId: string;
  quotedAmount: number;
  status: BidStatus | string;
  submittedAt: string;
  // UI enriched helpers
  bidderName?: string;
  tenderTitle?: string;
  tenderNumber?: string;
  documentsCount?: number;
}

export interface SubmitBidRequest {
  bidderProfileId?: string;
  quotedAmount: number;
}

export interface BidDocument {
  id: string;
  bidId: string;
  originalFilename: string;
  fileSize: number;
  contentType: string;
  uploadedAt: string;
  indexed?: boolean;
}

export type EvaluationStatus = 
  | 'MEETS_REQUIREMENT'
  | 'DOES_NOT_MEET'
  | 'INSUFFICIENT_EVIDENCE'
  | 'REQUIRES_REVIEW'
  | 'PENDING';

export interface TurnoverEvaluationResponse {
  bidId: string;
  requirementCode: string;
  requiredAmount: number | null;
  declaredAmount: number | null;
  numericConditionMet: boolean | null;
  status: EvaluationStatus | string;
  explanation: string;
  sourceDocumentId: string | null;
  sourcePage: number | null;
}

export interface EvidenceSearchResponse {
  documentId: string;
  pageNumber: number;
  chunkIndex: number;
  content: string;
  similarityScore: number;
}

export type OrganizationType = 'BIDDER' | 'PROCUREMENT_AUTHORITY';

export interface Organization {
  id: string;
  name: string;
  organizationType: OrganizationType;
  registrationNumber: string;
  createdAt: string;
}

export interface CreateOrganizationRequest {
  name: string;
  organizationType: OrganizationType;
  registrationNumber: string;
}

export interface BidderProfileResponse {
  id: string;
  organizationId: string;
}

export type UserRole = 'SYSTEM_ADMIN' | 'PROCUREMENT_OFFICER' | 'BIDDER' | 'EVALUATION_OFFICER';

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  organizationId?: string;
  organizationName?: string;
  designation: string;
  department: string;
}

export interface RequirementEvaluationItem {
  id: string;
  requirementCode: string;
  title: string;
  description: string;
  requiredValue: string;
  extractedValue: string;
  status: EvaluationStatus;
  mandatory: boolean;
  explanation: string;
  sourceDocumentId: string | null;
  sourceDocumentName?: string;
  sourcePage: number | null;
  evidenceSnippet?: string;
  confidenceScore?: number;
  manualOverride?: {
    overriddenBy: string;
    overriddenAt: string;
    newStatus: EvaluationStatus;
    reason: string;
    notes?: string;
  };
}

export interface BidEvaluationSummary {
  bidId: string;
  tenderId: string;
  tenderNumber: string;
  tenderTitle: string;
  bidderName: string;
  bidderRegNumber: string;
  quotedAmount: number;
  estimatedValue: number;
  evaluatedAt?: string;
  overallStatus: 'PENDING' | 'ELIGIBLE' | 'INELIGIBLE' | 'MANUAL_REVIEW_REQUIRED' | 'ACCEPTED' | 'REJECTED';
  evaluatedBy?: string;
  requirements: RequirementEvaluationItem[];
  reviewerComments?: string;
  finalDecisionDate?: string;
  finalApprovedBy?: string;
}
