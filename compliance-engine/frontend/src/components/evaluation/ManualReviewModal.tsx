import React, { useState } from 'react';
import { X, ShieldAlert, CheckCircle2 } from 'lucide-react';
import { EvaluationStatus, RequirementEvaluationItem } from '../../types';
import { useAuth } from '../../context/AuthContext';

interface ManualReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  requirement: RequirementEvaluationItem | null;
  onSubmitOverride: (
    requirementId: string,
    data: {
      newStatus: EvaluationStatus;
      reason: string;
      notes?: string;
      overriddenBy: string;
    }
  ) => void;
}

export const ManualReviewModal: React.FC<ManualReviewModalProps> = ({
  isOpen,
  onClose,
  requirement,
  onSubmitOverride,
}) => {
  const { user } = useAuth();
  const [newStatus, setNewStatus] = useState<EvaluationStatus>('MEETS_REQUIREMENT');
  const [reason, setReason] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');

  if (!isOpen || !requirement) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!reason.trim()) {
      setError('A written justification is strictly required for official audit trails.');
      return;
    }

    onSubmitOverride(requirement.id, {
      newStatus,
      reason: reason.trim(),
      notes: notes.trim(),
      overriddenBy: `${user?.name || 'Authorized Officer'} (${user?.designation || 'Procurement Authority'})`,
    });
    setReason('');
    setNotes('');
    setError('');
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-xl bg-white rounded-xl shadow-2xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-navy-900 text-white flex items-center justify-center">
              <ShieldAlert className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">Official Requirement Manual Review</h3>
              <p className="text-[11px] text-slate-500 font-mono">
                {requirement.requirementCode} • {requirement.title}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-md text-slate-400 hover:text-slate-600 cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
          {/* Original AI Finding Context */}
          <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-200 space-y-1.5">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Original AI Finding
            </span>
            <div className="flex items-center gap-2">
              <span className="font-semibold text-slate-900">Status: {requirement.status}</span>
              <span className="text-slate-300">•</span>
              <span className="text-slate-600">Extracted: {requirement.extractedValue}</span>
            </div>
            <p className="text-slate-600 line-clamp-2 leading-relaxed">
              {requirement.explanation}
            </p>
          </div>

          {/* New Review Status */}
          <div>
            <label className="block font-semibold text-slate-900 mb-1.5">
              Decided Evaluation Status <span className="text-rose-500">*</span>
            </label>
            <div className="grid grid-cols-3 gap-2.5">
              {[
                { value: 'MEETS_REQUIREMENT', label: 'Meets Requirement', color: 'border-emerald-300 bg-emerald-50/50 text-emerald-800' },
                { value: 'DOES_NOT_MEET', label: 'Does Not Meet', color: 'border-rose-300 bg-rose-50/50 text-rose-800' },
                { value: 'INSUFFICIENT_EVIDENCE', label: 'Manual Review Req.', color: 'border-amber-300 bg-amber-50/50 text-amber-800' },
              ].map((opt) => (
                <button
                  type="button"
                  key={opt.value}
                  onClick={() => setNewStatus(opt.value as EvaluationStatus)}
                  className={`p-2.5 rounded-lg border text-center transition-all cursor-pointer ${
                    newStatus === opt.value
                      ? `${opt.color} ring-2 ring-navy-900 font-bold`
                      : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Mandatory Justification */}
          <div>
            <label className="block font-semibold text-slate-900 mb-1">
              Justification for Evaluation Decision / Override <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows={3}
              value={reason}
              onChange={(e) => {
                setReason(e.target.value);
                if (error) setError('');
              }}
              placeholder="Cite supporting original gazette notifications, offline attested certificates, or administrative verification basis..."
              className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900 transition-all text-xs"
            />
            {error && <p className="text-[11px] text-rose-600 mt-1 font-medium">{error}</p>}
          </div>

          {/* Internal Committee Notes */}
          <div>
            <label className="block font-semibold text-slate-900 mb-1">
              Additional Committee Notes (Optional)
            </label>
            <input
              type="text"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g. Verified by Technical Subcommittee on 02-Nov-2026"
              className="w-full p-2 border border-slate-200 rounded-lg text-slate-800 text-xs focus:outline-none focus:ring-2 focus:ring-navy-900/10"
            />
          </div>

          {/* Officer Attribution Notice */}
          <div className="p-3 bg-blue-50/50 rounded-lg border border-blue-100 flex items-start gap-2 text-slate-600">
            <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
            <p className="text-[11px] leading-relaxed">
              This action will be permanently recorded in the immutable audit trail under officer{' '}
              <span className="font-semibold text-slate-900">{user?.name}</span> ({user?.designation}).
            </p>
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-3.5 py-2 text-xs font-medium text-slate-600 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
            >
              Record Official Decision
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
