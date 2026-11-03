import React from 'react';
import { EvaluationStatus, TenderStatus, BidStatus } from '../../types';

interface StatusBadgeProps {
  status: EvaluationStatus | TenderStatus | BidStatus | string;
  size?: 'sm' | 'md';
  showDot?: boolean;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'sm', showDot = true }) => {
  const normalized = status.toUpperCase();

  let label = status;
  let bgClass = 'bg-slate-100 text-slate-700 border-slate-200';
  let dotClass = 'bg-slate-400';

  switch (normalized) {
    // AI Evaluation Statuses
    case 'MEETS_REQUIREMENT':
      label = 'Meets Requirement';
      bgClass = 'bg-emerald-50 text-emerald-700 border-emerald-200';
      dotClass = 'bg-emerald-500';
      break;
    case 'DOES_NOT_MEET':
      label = 'Does Not Meet Requirement';
      bgClass = 'bg-rose-50 text-rose-700 border-rose-200';
      dotClass = 'bg-rose-500';
      break;
    case 'INSUFFICIENT_EVIDENCE':
      label = 'Manual Review Required';
      bgClass = 'bg-amber-50 text-amber-700 border-amber-200';
      dotClass = 'bg-amber-500';
      break;
    case 'REQUIRES_REVIEW':
      label = 'Review Required';
      bgClass = 'bg-amber-50 text-amber-700 border-amber-200';
      dotClass = 'bg-amber-500';
      break;
    case 'PENDING':
      label = 'Pending Evaluation';
      bgClass = 'bg-slate-100 text-slate-600 border-slate-200';
      dotClass = 'bg-slate-400';
      break;

    // Tender Statuses
    case 'PUBLISHED':
    case 'ACTIVE':
      label = 'Published';
      bgClass = 'bg-blue-50 text-blue-700 border-blue-200';
      dotClass = 'bg-blue-500';
      break;
    case 'DRAFT':
      label = 'Draft';
      bgClass = 'bg-slate-100 text-slate-600 border-slate-200';
      dotClass = 'bg-slate-400';
      break;
    case 'EVALUATING':
      label = 'Evaluating';
      bgClass = 'bg-indigo-50 text-indigo-700 border-indigo-200';
      dotClass = 'bg-indigo-500';
      break;
    case 'CLOSED':
      label = 'Closed';
      bgClass = 'bg-slate-100 text-slate-500 border-slate-200';
      dotClass = 'bg-slate-400';
      break;

    // Bid Statuses
    case 'SUBMITTED':
      label = 'Submitted';
      bgClass = 'bg-sky-50 text-sky-700 border-sky-200';
      dotClass = 'bg-sky-500';
      break;
    case 'UNDER_REVIEW':
      label = 'Under Review';
      bgClass = 'bg-amber-50 text-amber-700 border-amber-200';
      dotClass = 'bg-amber-500';
      break;
    case 'ACCEPTED':
    case 'APPROVED':
    case 'ELIGIBLE':
      label = 'Eligible / Accepted';
      bgClass = 'bg-emerald-50 text-emerald-700 border-emerald-200';
      dotClass = 'bg-emerald-500';
      break;
    case 'REJECTED':
    case 'INELIGIBLE':
      label = 'Ineligible / Rejected';
      bgClass = 'bg-rose-50 text-rose-700 border-rose-200';
      dotClass = 'bg-rose-500';
      break;
    default:
      label = status.replace(/_/g, ' ');
  }

  const paddingClass = size === 'sm' ? 'px-2.5 py-0.5 text-xs' : 'px-3 py-1 text-sm';

  return (
    <span
      className={`inline-flex items-center gap-1.5 font-medium rounded-full border ${paddingClass} ${bgClass} transition-colors`}
    >
      {showDot && <span className={`w-1.5 h-1.5 rounded-full ${dotClass}`} />}
      {label}
    </span>
  );
};
