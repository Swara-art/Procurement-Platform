import { apiClient } from './client';
import { 
  BidEvaluationSummary, 
  EvidenceSearchResponse, 
  RequirementEvaluationItem, 
  TurnoverEvaluationResponse,
  EvaluationStatus,
  Bid,
  Tender,
  TenderRequirement
} from '../types';

export const evaluationApi = {
  // Calls real backend Spring Boot endpoint
  async evaluateTurnover(bidId: string): Promise<TurnoverEvaluationResponse> {
    const response = await apiClient.post<TurnoverEvaluationResponse>(`/bids/${bidId}/evaluate/turnover`);
    return response.data;
  },

  // Calls real backend evidence search endpoint
  async searchEvidence(bidId: string, query: string): Promise<EvidenceSearchResponse[]> {
    const response = await apiClient.get<EvidenceSearchResponse[]>(`/bids/${bidId}/evidence/search`, {
      params: { query },
    });
    return response.data || [];
  },

  // Builds an evaluation summary from real DB records
  async getEvaluationSummary(bidId: string): Promise<BidEvaluationSummary> {
    // Check if there is an evaluation stored in localStorage for this bid
    const saved = localStorage.getItem(`eval_summary_${bidId}`);
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        // proceed to build from live API
      }
    }

    // Fetch real bid from backend
    const bidRes = await apiClient.get<Bid>(`/bids/${bidId}`);
    const bid = bidRes.data;

    // Fetch real tender from backend
    let tender: Tender | null = null;
    let requirements: TenderRequirement[] = [];
    try {
      const tenderRes = await apiClient.get<Tender>(`/tenders/${bid.tenderId}`);
      tender = tenderRes.data;
      const reqsRes = await apiClient.get<TenderRequirement[]>(`/tenders/${bid.tenderId}/requirements`);
      requirements = reqsRes.data || [];
    } catch {
      // tender or reqs error
    }

    // Map each real tender requirement into an evaluation item
    const requirementItems: RequirementEvaluationItem[] = requirements.map((req) => ({
      id: req.id,
      requirementCode: req.requirementCode,
      title: req.requirementCode.replace(/_/g, ' '),
      description: req.description,
      requiredValue: req.expectedValue,
      extractedValue: 'Pending Evaluation',
      status: 'PENDING' as EvaluationStatus,
      mandatory: req.mandatory,
      explanation: req.requirementCode === 'MIN_TURNOVER' 
        ? 'Click "Run AI Evaluation" to evaluate this condition against uploaded PDFs using the backend Turnover Evaluation Service.'
        : 'Backend AI evaluation for this specific requirement type is pending backend implementation. Manual review required.',
      sourceDocumentId: null,
      sourcePage: null,
    }));

    // If no requirements in DB yet, add a placeholder indicating so
    if (requirementItems.length === 0) {
      requirementItems.push({
        id: 'no-req',
        requirementCode: 'NO_REQUIREMENTS',
        title: 'No Requirements Configured',
        description: 'This tender does not have any eligibility requirements configured in the database yet.',
        requiredValue: 'N/A',
        extractedValue: 'N/A',
        status: 'PENDING',
        mandatory: false,
        explanation: 'Configure eligibility requirements on the tender page.',
        sourceDocumentId: null,
        sourcePage: null,
      });
    }

    const summary: BidEvaluationSummary = {
      bidId: bid.id,
      tenderId: bid.tenderId,
      tenderNumber: tender?.tenderNumber || 'N/A',
      tenderTitle: tender?.title || 'Tender',
      bidderName: `Bidder Profile #${bid.bidderProfileId.slice(0, 8)}`,
      bidderRegNumber: bid.bidderProfileId,
      quotedAmount: bid.quotedAmount,
      estimatedValue: tender?.estimatedValue || 0,
      overallStatus: 'PENDING',
      requirements: requirementItems,
    };

    localStorage.setItem(`eval_summary_${bidId}`, JSON.stringify(summary));
    return summary;
  },

  async runFullAiEvaluation(bidId: string): Promise<BidEvaluationSummary> {
    const summary = await this.getEvaluationSummary(bidId);

    // Call REAL backend turnover evaluation endpoint: POST /api/v1/bids/{bidId}/evaluate/turnover
    try {
      const turnoverRes = await this.evaluateTurnover(bidId);
      const turnoverReq = summary.requirements.find(r => r.requirementCode === 'MIN_TURNOVER');
      if (turnoverReq) {
        turnoverReq.status = (turnoverRes.status as EvaluationStatus) || 'REQUIRES_REVIEW';
        turnoverReq.explanation = turnoverRes.explanation;
        turnoverReq.extractedValue = turnoverRes.declaredAmount 
          ? `₹${Number(turnoverRes.declaredAmount).toLocaleString('en-IN')}` 
          : 'Not Found in Evidence';
        turnoverReq.sourceDocumentId = turnoverRes.sourceDocumentId;
        turnoverReq.sourceDocumentName = turnoverRes.sourceDocumentId ? 'Uploaded_Document.pdf' : undefined;
        turnoverReq.sourcePage = turnoverRes.sourcePage;
        turnoverReq.evidenceSnippet = turnoverRes.sourcePage !== null && turnoverRes.sourcePage !== undefined
          ? `Turnover declaration extracted from Page ${turnoverRes.sourcePage}: ${turnoverRes.explanation}` 
          : undefined;
      }
    } catch (err: any) {
      const turnoverReq = summary.requirements.find(r => r.requirementCode === 'MIN_TURNOVER');
      if (turnoverReq) {
        turnoverReq.status = 'INSUFFICIENT_EVIDENCE';
        turnoverReq.extractedValue = 'Not Available';
        turnoverReq.explanation = err.response?.data?.message || 'Backend evaluation service returned no evidence for this bid.';
      }
    }

    // Set overall status based on requirements
    const hasManualReview = summary.requirements.some(r => r.status === 'REQUIRES_REVIEW' || r.status === 'INSUFFICIENT_EVIDENCE');
    summary.overallStatus = hasManualReview ? 'MANUAL_REVIEW_REQUIRED' : 'ELIGIBLE';
    summary.evaluatedAt = new Date().toISOString();
    summary.evaluatedBy = 'Spring Boot Compliance Engine (Hybrid RAG)';

    localStorage.setItem(`eval_summary_${bidId}`, JSON.stringify(summary));
    return summary;
  },

  async applyManualOverride(
    bidId: string, 
    requirementId: string, 
    overrideData: {
      newStatus: EvaluationStatus;
      reason: string;
      notes?: string;
      overriddenBy: string;
    }
  ): Promise<BidEvaluationSummary> {
    const summary = await this.getEvaluationSummary(bidId);
    const req = summary.requirements.find(r => r.id === requirementId || r.requirementCode === requirementId);
    if (req) {
      req.status = overrideData.newStatus;
      req.manualOverride = {
        overriddenBy: overrideData.overriddenBy,
        overriddenAt: new Date().toISOString(),
        newStatus: overrideData.newStatus,
        reason: overrideData.reason,
        notes: overrideData.notes,
      };
    }

    const allMandatoryMet = summary.requirements
      .filter(r => r.mandatory)
      .every(r => r.status === 'MEETS_REQUIREMENT');

    const anyFailed = summary.requirements
      .filter(r => r.mandatory)
      .some(r => r.status === 'DOES_NOT_MEET');

    if (anyFailed) {
      summary.overallStatus = 'INELIGIBLE';
    } else if (allMandatoryMet) {
      summary.overallStatus = 'ELIGIBLE';
    } else {
      summary.overallStatus = 'MANUAL_REVIEW_REQUIRED';
    }

    localStorage.setItem(`eval_summary_${bidId}`, JSON.stringify(summary));
    return summary;
  },

  async recordFinalDecision(
    bidId: string, 
    decision: 'ACCEPTED' | 'REJECTED', 
    comments: string,
    authorizedBy: string
  ): Promise<BidEvaluationSummary> {
    const summary = await this.getEvaluationSummary(bidId);
    summary.overallStatus = decision;
    summary.reviewerComments = comments;
    summary.finalDecisionDate = new Date().toISOString();
    summary.finalApprovedBy = authorizedBy;

    localStorage.setItem(`eval_summary_${bidId}`, JSON.stringify(summary));
    return summary;
  }
};
