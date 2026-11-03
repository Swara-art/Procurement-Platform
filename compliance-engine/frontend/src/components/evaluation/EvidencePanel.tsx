import React from 'react';
import { X, FileText, AlertCircle, Sparkles, UserCheck, ShieldCheck, HelpCircle } from 'lucide-react';
import { RequirementEvaluationItem } from '../../types';
import { StatusBadge } from '../common/StatusBadge';

interface EvidencePanelProps {
  isOpen: boolean;
  onClose: () => void;
  requirement: RequirementEvaluationItem | null;
  onOpenManualReview: (req: RequirementEvaluationItem) => void;
}

export const EvidencePanel: React.FC<EvidencePanelProps> = ({
  isOpen,
  onClose,
  requirement,
  onOpenManualReview,
}) => {
  if (!isOpen || !requirement) return null;

  const isInsufficient = requirement.status === 'INSUFFICIENT_EVIDENCE';
  const hasSource = requirement.sourceDocumentId && requirement.sourcePage !== null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="absolute inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity duration-200"
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <aside className="w-screen max-w-lg bg-white shadow-2xl flex flex-col border-l border-slate-200 animate-in slide-in-from-right duration-200">
          {/* Header */}
          <div className="px-6 py-5 border-b border-slate-200 bg-slate-50/70 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-navy-900 text-white flex items-center justify-center">
                <Sparkles className="w-4 h-4" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-slate-900">AI Evidence Verification</h2>
                <p className="text-[11px] text-slate-500 font-mono">
                  Code: {requirement.requirementCode}
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200/50 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {/* Requirement Summary */}
            <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-700">{requirement.title}</span>
                <StatusBadge status={requirement.status} />
              </div>
              <p className="text-xs text-slate-600 leading-relaxed">
                {requirement.description}
              </p>
              <div className="grid grid-cols-2 gap-3 pt-2 border-t border-slate-200/70 text-xs">
                <div>
                  <span className="text-slate-400 text-[11px] block">Required Threshold</span>
                  <span className="font-semibold text-slate-900">{requirement.requiredValue}</span>
                </div>
                <div>
                  <span className="text-slate-400 text-[11px] block">Extracted Declaration</span>
                  <span className="font-semibold text-slate-900">{requirement.extractedValue}</span>
                </div>
              </div>
            </div>

            {/* Insufficient Evidence Warning Banner */}
            {isInsufficient && (
              <div className="p-4 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 space-y-2">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
                  <span className="text-xs font-bold uppercase tracking-wide">
                    Manual Verification Required
                  </span>
                </div>
                <p className="text-xs leading-relaxed text-amber-800">
                  The uploaded documents do not contain sufficient or explicitly verifiable information to satisfy this tender condition. As per procurement regulations, missing citations are never automatically deemed non-compliant; procurement officers must examine physical files or request official clarification.
                </p>
              </div>
            )}

            {/* AI-Generated Explanation */}
            <div>
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                AI Reasoning & Compliance Explanation
              </h3>
              <div className="p-4 rounded-xl bg-blue-50/40 border border-blue-100 text-xs text-slate-700 leading-relaxed space-y-2">
                <p>{requirement.explanation}</p>
                {requirement.confidenceScore && (
                  <div className="pt-2 border-t border-blue-100/60 flex items-center justify-between text-[11px] text-slate-500">
                    <span>Evidence extraction confidence</span>
                    <span className="font-mono font-semibold text-slate-700">
                      {(requirement.confidenceScore * 100).toFixed(0)}%
                    </span>
                  </div>
                )}
              </div>
            </div>

            {/* Source Document Citation */}
            <div>
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                <FileText className="w-3.5 h-3.5 text-slate-500" />
                Document Citation Reference
              </h3>
              {hasSource ? (
                <div className="p-3.5 rounded-xl border border-slate-200 bg-white shadow-subtle space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-medium text-slate-800 truncate">
                      {requirement.sourceDocumentName || 'Bidder_Submitted_Document.pdf'}
                    </span>
                    <span className="px-2 py-0.5 rounded bg-slate-100 text-[11px] font-mono text-slate-700 font-semibold shrink-0">
                      Page {requirement.sourcePage}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-400 font-mono">
                    Document ID: {requirement.sourceDocumentId}
                  </p>
                </div>
              ) : (
                <div className="p-3.5 rounded-xl border border-slate-200 bg-slate-50 text-slate-500 text-xs flex items-center gap-2">
                  <HelpCircle className="w-4 h-4 text-slate-400 shrink-0" />
                  <span>Source not available (No direct document citation identified)</span>
                </div>
              )}
            </div>

            {/* Retrieved Evidence Text Chunk */}
            {requirement.evidenceSnippet && (
              <div>
                <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2">
                  Extracted Evidence Snippet
                </h3>
                <blockquote className="p-3.5 rounded-xl bg-slate-50 border-l-4 border-navy-900 text-xs text-slate-700 font-mono leading-relaxed overflow-x-auto">
                  "{requirement.evidenceSnippet}"
                </blockquote>
              </div>
            )}

            {/* Manual Review History / Current State */}
            {requirement.manualOverride && (
              <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-xs space-y-1.5">
                <div className="flex items-center gap-2 text-emerald-800 font-bold">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Officer Manual Override Applied</span>
                </div>
                <p className="text-slate-700">
                  <span className="font-semibold">Reason:</span> {requirement.manualOverride.reason}
                </p>
                <div className="text-[11px] text-slate-500 pt-1">
                  Recorded by {requirement.manualOverride.overriddenBy} on{' '}
                  {new Date(requirement.manualOverride.overriddenAt).toLocaleString()}
                </div>
              </div>
            )}
          </div>

          {/* Footer Action */}
          <div className="p-5 border-t border-slate-200 bg-slate-50 flex items-center justify-between gap-3">
            <button
              onClick={onClose}
              className="px-3.5 py-2 text-xs font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer"
            >
              Close Drawer
            </button>
            <button
              onClick={() => {
                onClose();
                onOpenManualReview(requirement);
              }}
              className="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
            >
              <UserCheck className="w-3.5 h-3.5" />
              Manual Review & Override
            </button>
          </div>
        </aside>
      </div>
    </div>
  );
};
