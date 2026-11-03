import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Sparkles,
  ShieldCheck,
  Building2,
  FileSpreadsheet,
  AlertTriangle,
  UserCheck,
  CheckCircle2,
  XCircle,
  FileText,
  RotateCcw,
  Loader2,
  Info
} from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { RequirementRow } from '../components/evaluation/RequirementRow';
import { EvidencePanel } from '../components/evaluation/EvidencePanel';
import { ManualReviewModal } from '../components/evaluation/ManualReviewModal';
import { ConfirmationDialog } from '../components/common/ConfirmationDialog';
import { evaluationApi } from '../api/evaluationApi';
import { RequirementEvaluationItem, EvaluationStatus } from '../types';
import { useAuth } from '../context/AuthContext';

export const EvaluationPage: React.FC = () => {
  const { bidId: routeBidId } = useParams<{ bidId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  // Default to primary sample bid if accessed directly
  const activeBidId = routeBidId || 'f5e84a31-bbe9-4971-91a3-28bb24263f7e';

  const [selectedRequirement, setSelectedRequirement] = useState<RequirementEvaluationItem | null>(null);
  const [manualReviewReq, setManualReviewReq] = useState<RequirementEvaluationItem | null>(null);
  const [showFinalDecisionDialog, setShowFinalDecisionDialog] = useState(false);
  const [finalDecisionType, setFinalDecisionType] = useState<'ACCEPTED' | 'REJECTED'>('ACCEPTED');
  const [finalDecisionComments, setFinalDecisionComments] = useState('');
  const [evaluationProgressMsg, setEvaluationProgressMsg] = useState('');

  // Fetch Evaluation Summary
  const {
    data: evaluation,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['evaluation-summary', activeBidId],
    queryFn: () => evaluationApi.getEvaluationSummary(activeBidId),
    enabled: !!activeBidId,
  });

  // Run Full AI Evaluation Mutation (hits backend /api/v1/bids/{bidId}/evaluate/turnover)
  const runEvaluationMutation = useMutation({
    mutationFn: async () => {
      setEvaluationProgressMsg('Querying hybrid vector index & statutory turnover rules...');
      await new Promise((r) => setTimeout(r, 600));
      return evaluationApi.runFullAiEvaluation(activeBidId);
    },
    onSuccess: (data) => {
      queryClient.setQueryData(['evaluation-summary', activeBidId], data);
      setEvaluationProgressMsg('');
    },
    onError: () => {
      setEvaluationProgressMsg('');
    },
  });

  // Manual Override Mutation
  const overrideMutation = useMutation({
    mutationFn: ({
      reqId,
      data,
    }: {
      reqId: string;
      data: {
        newStatus: EvaluationStatus;
        reason: string;
        notes?: string;
        overriddenBy: string;
      };
    }) => evaluationApi.applyManualOverride(activeBidId, reqId, data),
    onSuccess: (data) => {
      queryClient.setQueryData(['evaluation-summary', activeBidId], data);
      setManualReviewReq(null);
    },
  });

  // Final Decision Mutation
  const finalDecisionMutation = useMutation({
    mutationFn: () =>
      evaluationApi.recordFinalDecision(
        activeBidId,
        finalDecisionType,
        finalDecisionComments,
        `${user?.name || 'Authorized Officer'} (${user?.designation || 'Director'})`
      ),
    onSuccess: (data) => {
      queryClient.setQueryData(['evaluation-summary', activeBidId], data);
      setShowFinalDecisionDialog(false);
      setFinalDecisionComments('');
    },
  });

  if (isLoading) {
    return <LoadingState message="Connecting to AI Bid Compliance Engine..." />;
  }

  if (isError || !evaluation) {
    return (
      <div className="bg-white rounded-xl border border-rose-200 p-8 text-center space-y-3">
        <AlertTriangle className="w-8 h-8 text-rose-500 mx-auto" />
        <h3 className="font-bold text-slate-900 text-sm">Failed to retrieve bid evaluation record</h3>
        <p className="text-xs text-slate-500 max-w-sm mx-auto">
          The compliance service may be temporarily undergoing maintenance.
        </p>
        <button
          onClick={() => refetch()}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          Retry Request
        </button>
      </div>
    );
  }

  const isOfficerOrAdmin = user?.role === 'PROCUREMENT_OFFICER' || user?.role === 'SYSTEM_ADMIN' || user?.role === 'EVALUATION_OFFICER';

  // Count criteria statuses
  const meetsCount = evaluation.requirements.filter((r) => r.status === 'MEETS_REQUIREMENT').length;
  const requiresReviewCount = evaluation.requirements.filter(
    (r) => r.status === 'REQUIRES_REVIEW' || r.status === 'INSUFFICIENT_EVIDENCE'
  ).length;
  const failedCount = evaluation.requirements.filter((r) => r.status === 'DOES_NOT_MEET').length;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="AI-Assisted Bid Eligibility Evaluation"
        subtitle="Explainable decision-support workspace comparing bidder documents against statutory requirements."
        backUrl={`/bids/${activeBidId}`}
        backLabel="Back to Bid File"
        badge={<StatusBadge status={evaluation.overallStatus} size="md" />}
        actions={
          <div className="flex items-center gap-2.5">
            <button
              onClick={() => runEvaluationMutation.mutate()}
              disabled={runEvaluationMutation.isPending}
              className="inline-flex items-center gap-2 px-4 py-2 text-xs font-bold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors disabled:opacity-50 cursor-pointer"
            >
              {runEvaluationMutation.isPending ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-blue-300" />
                  Running AI Evaluation...
                </>
              ) : (
                <>
                  <Sparkles className="w-3.5 h-3.5 text-blue-300" />
                  Run AI Evaluation
                </>
              )}
            </button>
            <button
              onClick={() => navigate(`/reports/${activeBidId}`)}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg shadow-2xs transition-colors cursor-pointer"
            >
              <FileText className="w-3.5 h-3.5" />
              View Official Report
            </button>
          </div>
        }
      />

      {/* Progress banner during evaluation execution */}
      {runEvaluationMutation.isPending && (
        <div className="p-4 rounded-xl bg-blue-50 border border-blue-200 flex items-center gap-3 text-blue-900 text-xs animate-pulse">
          <Loader2 className="w-4 h-4 animate-spin text-blue-600 shrink-0" />
          <span className="font-medium">{evaluationProgressMsg}</span>
        </div>
      )}

      {/* Tender & Bidder Workspace Header Card */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
        <div>
          <span className="text-slate-400 text-[11px] block">Tender Reference</span>
          <div className="flex items-center gap-1.5 font-bold text-slate-900 mt-0.5">
            <FileSpreadsheet className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <span className="truncate">{evaluation.tenderNumber}</span>
          </div>
          <span className="text-[11px] text-slate-500 truncate block mt-0.5">
            {evaluation.tenderTitle}
          </span>
        </div>

        <div>
          <span className="text-slate-400 text-[11px] block">Bidder Organization</span>
          <div className="flex items-center gap-1.5 font-bold text-slate-900 mt-0.5">
            <Building2 className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <span className="truncate">{evaluation.bidderName}</span>
          </div>
          <span className="text-[11px] text-slate-500 font-mono block mt-0.5">
            Reg: {evaluation.bidderRegNumber}
          </span>
        </div>

        <div>
          <span className="text-slate-400 text-[11px] block">Quoted vs Estimated Value</span>
          <div className="font-mono font-bold text-slate-900 mt-0.5">
            ₹{Number(evaluation.quotedAmount).toLocaleString('en-IN')}
          </div>
          <span className="text-[11px] text-slate-500 font-mono block mt-0.5">
            Est: ₹{Number(evaluation.estimatedValue).toLocaleString('en-IN')}
          </span>
        </div>

        <div>
          <span className="text-slate-400 text-[11px] block">Evaluation Authority</span>
          <div className="flex items-center gap-1 text-slate-900 font-semibold mt-0.5">
            <ShieldCheck className="w-3.5 h-3.5 text-blue-600 shrink-0" />
            <span className="truncate">Hybrid RAG + Human Oversight</span>
          </div>
          <span className="text-[11px] text-slate-500 block mt-0.5">
            {evaluation.evaluatedAt ? `Last run ${new Date(evaluation.evaluatedAt).toLocaleTimeString()}` : 'Not run yet'}
          </span>
        </div>
      </div>

      {/* Compliance Health Stats Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="p-3.5 bg-white rounded-xl border border-slate-200 flex items-center justify-between shadow-2xs">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <CheckCircle2 className="w-4 h-4" />
            </div>
            <div>
              <span className="text-xs font-semibold text-slate-900 block">Meets Requirement</span>
              <span className="text-[11px] text-slate-400">Verified via extracted evidence</span>
            </div>
          </div>
          <span className="text-lg font-bold text-emerald-700">{meetsCount}</span>
        </div>

        <div className="p-3.5 bg-white rounded-xl border border-slate-200 flex items-center justify-between shadow-2xs">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <AlertTriangle className="w-4 h-4" />
            </div>
            <div>
              <span className="text-xs font-semibold text-slate-900 block">Requires Manual Review</span>
              <span className="text-[11px] text-slate-400">Discrepancy or missing citation</span>
            </div>
          </div>
          <span className="text-lg font-bold text-amber-700">{requiresReviewCount}</span>
        </div>

        <div className="p-3.5 bg-white rounded-xl border border-slate-200 flex items-center justify-between shadow-2xs">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
              <XCircle className="w-4 h-4" />
            </div>
            <div>
              <span className="text-xs font-semibold text-slate-900 block">Does Not Meet</span>
              <span className="text-[11px] text-slate-400">Non-compliant declarations</span>
            </div>
          </div>
          <span className="text-lg font-bold text-rose-700">{failedCount}</span>
        </div>
      </div>

      {/* Statutory Guidance Notice */}
      <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl flex items-start gap-3 text-xs text-slate-600">
        <Info className="w-4 h-4 text-slate-400 shrink-0 mt-0.5" />
        <p className="leading-relaxed">
          <strong className="text-slate-900">Statutory Notice on AI Evaluations:</strong> The AI extracts and synthesizes figures from uploaded documents. As per General Financial Rules (GFR), AI findings serve exclusively as decision support. Any finding of <em className="text-amber-800 font-semibold">"Manual Review Required"</em> or <em className="text-slate-800">"Insufficient Evidence"</em> must be verified by the procurement officer before disqualification.
        </p>
      </div>

      {/* Main Requirement Evaluation Matrix Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-card overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-900">Requirement-by-Requirement Verification Matrix</h3>
            <p className="text-xs text-slate-500">
              Click "View Evidence" on any criteria to inspect citations, page numbers, and reasoning.
            </p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                <th className="py-3.5 px-4">Tender Requirement</th>
                <th className="py-3.5 px-4">Required Target</th>
                <th className="py-3.5 px-4">Extracted Declaration</th>
                <th className="py-3.5 px-4">AI Verification Status</th>
                <th className="py-3.5 px-4 text-right">Evidence Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {evaluation.requirements.map((item) => (
                <RequirementRow
                  key={item.id}
                  item={item}
                  onViewEvidence={(req) => setSelectedRequirement(req)}
                />
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Human Decision & Final Approval Section */}
      <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-card space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-slate-100 pb-4">
          <div>
            <div className="flex items-center gap-2">
              <UserCheck className="w-4 h-4 text-navy-900" />
              <h3 className="text-sm font-bold text-slate-900">
                Authorized Officer Review & Final Decision
              </h3>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Final acceptance or rejection of this bid must remain under authorized human control.
            </p>
          </div>

          {evaluation.finalApprovedBy && (
            <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200 text-right">
              <span className="text-[10px] text-slate-400 block uppercase font-bold">
                Officially Decided
              </span>
              <span className="text-xs font-semibold text-slate-900 block">
                {evaluation.overallStatus} by {evaluation.finalApprovedBy}
              </span>
              <span className="text-[11px] text-slate-500">
                {new Date(evaluation.finalDecisionDate || '').toLocaleString()}
              </span>
            </div>
          )}
        </div>

        {evaluation.reviewerComments && (
          <div className="p-3.5 rounded-lg bg-slate-50 border border-slate-200 text-xs">
            <span className="font-semibold text-slate-800 block mb-1">
              Official Reviewer Observations:
            </span>
            <p className="text-slate-600 leading-relaxed italic">
              "{evaluation.reviewerComments}"
            </p>
          </div>
        )}

        {isOfficerOrAdmin && (
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-2">
            <p className="text-xs text-slate-500">
              Review all requirement evidence before recording the statutory bid decision.
            </p>
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => {
                  setFinalDecisionType('REJECTED');
                  setShowFinalDecisionDialog(true);
                }}
                className="px-4 py-2 text-xs font-bold text-rose-700 bg-rose-50 border border-rose-200 hover:bg-rose-100 rounded-lg transition-colors cursor-pointer"
              >
                Disqualify / Reject Bid
              </button>
              <button
                type="button"
                onClick={() => {
                  setFinalDecisionType('ACCEPTED');
                  setShowFinalDecisionDialog(true);
                }}
                className="px-4 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg shadow-sm transition-colors cursor-pointer"
              >
                Approve & Qualify Bid
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Side Drawer: Evidence Panel */}
      <EvidencePanel
        isOpen={!!selectedRequirement}
        onClose={() => setSelectedRequirement(null)}
        requirement={selectedRequirement}
        onOpenManualReview={(req) => {
          setSelectedRequirement(null);
          setManualReviewReq(req);
        }}
      />

      {/* Modal: Manual Review & Override */}
      <ManualReviewModal
        isOpen={!!manualReviewReq}
        onClose={() => setManualReviewReq(null)}
        requirement={manualReviewReq}
        onSubmitOverride={(reqId, data) => {
          overrideMutation.mutate({ reqId, data });
        }}
      />

      {/* Confirmation Modal: Final Bid Acceptance / Rejection */}
      <ConfirmationDialog
        isOpen={showFinalDecisionDialog}
        onClose={() => setShowFinalDecisionDialog(false)}
        onConfirm={() => finalDecisionMutation.mutate()}
        title={
          finalDecisionType === 'ACCEPTED'
            ? 'Approve Bid as Technically Qualified?'
            : 'Formally Disqualify / Reject Bid?'
        }
        description={
          finalDecisionType === 'ACCEPTED'
            ? `You are certifying that "${evaluation.bidderName}" satisfies all statutory eligibility requirements for tender "${evaluation.tenderNumber}". This action will be recorded in the official procurement audit log.`
            : `You are rejecting "${evaluation.bidderName}" for non-compliance with tender requirements. A formal disqualification notification will be generated for the bidder.`
        }
        variant={finalDecisionType === 'ACCEPTED' ? 'primary' : 'danger'}
        confirmLabel={
          finalDecisionType === 'ACCEPTED' ? 'Confirm Approval' : 'Confirm Disqualification'
        }
        isLoading={finalDecisionMutation.isPending}
      />
    </div>
  );
};
