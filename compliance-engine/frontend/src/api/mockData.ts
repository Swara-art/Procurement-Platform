import { Organization, Tender, TenderRequirement, Bid, BidDocument, BidEvaluationSummary } from '../types';

export const INITIAL_ORGANIZATIONS: Organization[] = [];
export const INITIAL_TENDERS: Tender[] = [];
export const INITIAL_REQUIREMENTS: Record<string, TenderRequirement[]> = {};
export const INITIAL_BIDS: Bid[] = [];
export const INITIAL_DOCUMENTS: Record<string, BidDocument[]> = {};
export const INITIAL_EVALUATIONS: Record<string, BidEvaluationSummary> = {};
