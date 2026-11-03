import React from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Printer, Download, Shield, CheckCircle2, AlertTriangle, FileText } from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { evaluationApi } from '../api/evaluationApi';

export const ReportsPage: React.FC = () => {
  const { bidId: routeBidId } = useParams<{ bidId: string }>();
  const activeBidId = routeBidId || 'f5e84a31-bbe9-4971-91a3-28bb24263f7e';

  const { data: evaluation, isLoading } = useQuery({
    queryKey: ['evaluation-summary', activeBidId],
    queryFn: () => evaluationApi.getEvaluationSummary(activeBidId),
  });

  const handlePrint = () => {
    window.print();
  };

  if (isLoading) {
    return <LoadingState message="Compiling official audit evaluation report..." />;
  }

  if (!evaluation) {
    return <div className="p-8 text-center text-xs text-slate-500">No report available.</div>;
  }

  return (
    <div className="space-y-6">
      {/* Top Controls (Hidden during print) */}
      <div className="no-print">
        <PageHeader
          title="Tender Evaluation & Compliance Audit Report"
          subtitle="Statutory verification dossier suitable for administrative, financial, and vigilance inspection."
          backUrl={`/evaluations/${activeBidId}`}
          backLabel="Back to Evaluation Workspace"
          actions={
            <div className="flex items-center gap-2.5">
              <button
                onClick={handlePrint}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg shadow-xs transition-colors cursor-pointer"
              >
                <Printer className="w-3.5 h-3.5" />
                Print / Save PDF
              </button>
              <button
                onClick={handlePrint}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
              >
                <Download className="w-3.5 h-3.5" />
                Download Report Dossier
              </button>
            </div>
          }
        />
      </div>

      {/* Official Government Printable Document */}
      <div className="bg-white p-8 sm:p-12 rounded-xl border border-slate-200 shadow-card max-w-4xl mx-auto space-y-8 font-sans text-slate-900 print:border-none print:shadow-none print:p-0">
        {/* Document Header */}
        <div className="border-b-2 border-slate-900 pb-5 text-center space-y-1.5">
          <div className="flex items-center justify-center gap-2 text-navy-900 font-bold uppercase tracking-wider text-xs">
            <Shield className="w-4 h-4" />
            <span>Government Procurement & Compliance Verification Directorate</span>
          </div>
          <h1 className="text-xl font-bold tracking-tight text-slate-900">
            TECHNICAL BID ELIGIBILITY EVALUATION DOSSIER
          </h1>
          <p className="text-xs text-slate-500 font-mono">
            Document No: GOV-AUDIT/{new Date().getFullYear()}/{activeBidId.slice(0, 8).toUpperCase()}
          </p>
        </div>

        {/* Section 1: Executive Meta */}
        <div className="grid grid-cols-2 gap-6 text-xs bg-slate-50 p-4 rounded-lg border border-slate-200">
          <div className="space-y-1.5">
            <div>
              <span className="text-slate-400 text-[11px] block font-semibold">Tender Title:</span>
              <span className="font-bold text-slate-900">{evaluation.tenderTitle}</span>
            </div>
            <div>
              <span className="text-slate-400 text-[11px] block font-semibold">Tender Reference:</span>
              <span className="font-mono text-slate-800">{evaluation.tenderNumber}</span>
            </div>
            <div>
              <span className="text-slate-400 text-[11px] block font-semibold">Estimated Contract Value:</span>
              <span className="font-mono font-bold text-slate-800">
                ₹{Number(evaluation.estimatedValue).toLocaleString('en-IN')}
              </span>
            </div>
          </div>

          <div className="space-y-1.5">
            <div>
              <span className="text-slate-400 text-[11px] block font-semibold">Bidder Organization:</span>
              <span className="font-bold text-slate-900">{evaluation.bidderName}</span>
            </div>
            <div>
              <span className="text-slate-400 text-[11px] block font-semibold">Registration Number:</span>
              <span className="font-mono text-slate-800">{evaluation.bidderRegNumber}</span>
            </div>
            <div>
              <span className="text-slate-400 text-[11px] block font-semibold">Commercial Quoted Amount:</span>
              <span className="font-mono font-bold text-slate-800">
                ₹{Number(evaluation.quotedAmount).toLocaleString('en-IN')}
              </span>
            </div>
          </div>
        </div>

        {/* Section 2: Requirement Compliance Summary Matrix */}
        <div className="space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700 border-b border-slate-200 pb-1.5">
            1. Statutory Eligibility Evaluation Matrix
          </h3>
          <table className="w-full text-left border-collapse text-xs border border-slate-200">
            <thead>
              <tr className="bg-slate-100 text-[11px] font-bold text-slate-700 border-b border-slate-200">
                <th className="p-2.5">Requirement Criteria</th>
                <th className="p-2.5">Tender Target</th>
                <th className="p-2.5">Extracted Value</th>
                <th className="p-2.5">Status</th>
                <th className="p-2.5">Document Citation</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {evaluation.requirements.map((req) => (
                <tr key={req.id}>
                  <td className="p-2.5">
                    <span className="font-bold block">{req.title}</span>
                    <span className="font-mono text-[10px] text-slate-500">{req.requirementCode}</span>
                  </td>
                  <td className="p-2.5 font-medium">{req.requiredValue}</td>
                  <td className="p-2.5 font-mono">{req.extractedValue}</td>
                  <td className="p-2.5">
                    <StatusBadge status={req.status} showDot={false} />
                  </td>
                  <td className="p-2.5 text-slate-600 font-mono text-[11px]">
                    {req.sourcePage !== null ? (
                      `Page ${req.sourcePage} (${req.sourceDocumentName || 'Document.pdf'})`
                    ) : (
                      'Source not available'
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Section 3: AI Explanations and Evidence References */}
        <div className="space-y-4">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700 border-b border-slate-200 pb-1.5">
            2. Detailed Evidence & AI Reasoning
          </h3>
          <div className="space-y-3">
            {evaluation.requirements.map((req) => (
              <div key={req.id} className="p-3.5 bg-slate-50 rounded-lg border border-slate-200 text-xs space-y-1.5">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-slate-900">{req.title} ({req.requirementCode})</span>
                  <StatusBadge status={req.status} size="sm" />
                </div>
                <p className="text-slate-700 leading-relaxed">{req.explanation}</p>
                {req.evidenceSnippet && (
                  <blockquote className="p-2 bg-white rounded border border-slate-200 text-slate-600 font-mono text-[11px] mt-1 italic">
                    "{req.evidenceSnippet}"
                  </blockquote>
                )}
                {req.manualOverride && (
                  <div className="pt-2 border-t border-slate-200 text-emerald-800 text-[11px] font-medium">
                    Manual Review Decision: Overridden to "{req.manualOverride.newStatus}" by {req.manualOverride.overriddenBy}.
                    <span className="block italic text-slate-600 mt-0.5">
                      Justification: "{req.manualOverride.reason}"
                    </span>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Section 4: Authorized Reviewer Observation & Sign-Off */}
        <div className="space-y-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-700 border-b border-slate-200 pb-1.5">
            3. Final Statutory Certification & Approval
          </h3>
          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 text-xs space-y-2">
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Overall Determination:</span>
              <span className="font-bold text-slate-900 font-mono text-sm">
                {evaluation.overallStatus}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Evaluation Mechanism:</span>
              <span className="text-slate-800 font-medium">
                AI Hybrid RAG (pgvector) + Authorized Procurement Officer Verification
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-slate-500">Timestamp:</span>
              <span className="font-mono text-slate-800">
                {new Date().toISOString()}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-8 pt-8 text-center text-xs">
            <div className="space-y-1">
              <div className="border-b border-slate-400 pb-8 font-semibold text-slate-700">
                {evaluation.finalApprovedBy || 'Authorized Procurement Officer'}
              </div>
              <span className="text-slate-500 text-[11px] block">Procurement Authority Signature</span>
            </div>
            <div className="space-y-1">
              <div className="border-b border-slate-400 pb-8 font-semibold text-slate-700">
                Technical Committee Representative
              </div>
              <span className="text-slate-500 text-[11px] block">Committee Member Sign-Off</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
