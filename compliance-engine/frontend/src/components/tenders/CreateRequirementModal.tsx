import React, { useState } from 'react';
import { X, Plus, Sparkles } from 'lucide-react';
import { CreateTenderRequirementRequest, RequirementOperator } from '../../types';
import { tendersApi } from '../../api/tendersApi';

interface CreateRequirementModalProps {
  isOpen: boolean;
  onClose: () => void;
  tenderId: string;
  onSuccess: () => void;
}

export const CreateRequirementModal: React.FC<CreateRequirementModalProps> = ({
  isOpen,
  onClose,
  tenderId,
  onSuccess,
}) => {
  const [requirementCode, setRequirementCode] = useState('MIN_TURNOVER');
  const [description, setDescription] = useState('');
  const [operator, setOperator] = useState<RequirementOperator>('GREATER_THAN_OR_EQUAL');
  const [expectedValue, setExpectedValue] = useState('');
  const [mandatory, setMandatory] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleQuickPreset = (code: string, desc: string, op: RequirementOperator, val: string) => {
    setRequirementCode(code);
    setDescription(desc);
    setOperator(op);
    setExpectedValue(val);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!requirementCode || !description || !expectedValue) {
      setError('All fields are required to establish an automated verification rule.');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      const payload: CreateTenderRequirementRequest = {
        requirementCode: requirementCode.trim().toUpperCase(),
        description: description.trim(),
        operator,
        expectedValue: expectedValue.trim(),
        mandatory,
      };

      await tendersApi.createRequirement(tenderId, payload);
      setIsSubmitting(false);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err?.message || 'Failed to create requirement.');
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-2xl border border-slate-200 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/70">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-navy-900 text-white flex items-center justify-center">
              <Plus className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">Add Eligibility Requirement</h3>
              <p className="text-[11px] text-slate-500 font-mono">
                Tender ID: {tenderId.slice(0, 8)}...
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

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
          {/* Quick Preset Selector */}
          <div>
            <span className="text-[11px] font-semibold text-slate-500 block mb-1.5 flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-blue-500" />
              Quick Standard Templates:
            </span>
            <div className="flex flex-wrap gap-1.5">
              {[
                { label: 'Turnover (₹50 Lakh)', code: 'MIN_TURNOVER', desc: 'Minimum average annual turnover over 3 years must be Rs. 50 Lakhs', op: 'GREATER_THAN_OR_EQUAL' as RequirementOperator, val: '5000000' },
                { label: 'Experience (5 Years)', code: 'EXPERIENCE_YEARS', desc: 'Minimum 5 years of verified delivery experience in enterprise public software', op: 'GREATER_THAN_OR_EQUAL' as RequirementOperator, val: '5' },
                { label: 'ISO 27001 Security', code: 'ISO_27001_CERT', desc: 'Valid ISO/IEC 27001 Information Security Management Certificate', op: 'IS_VALID' as RequirementOperator, val: 'ISO/IEC 27001' },
                { label: 'Active GSTIN', code: 'GST_COMPLIANCE', desc: 'Valid state GSTIN tax registration certificate', op: 'IS_VALID' as RequirementOperator, val: 'ACTIVE_GSTIN' },
              ].map((p) => (
                <button
                  type="button"
                  key={p.label}
                  onClick={() => handleQuickPreset(p.code, p.desc, p.op, p.val)}
                  className="px-2.5 py-1 text-[11px] rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium transition-colors cursor-pointer"
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-semibold text-slate-900 mb-1">
                Requirement Code <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={requirementCode}
                onChange={(e) => setRequirementCode(e.target.value.toUpperCase())}
                placeholder="e.g. MIN_TURNOVER"
                className="w-full p-2 border border-slate-300 rounded-lg text-slate-900 font-mono text-xs focus:outline-none focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
                required
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-900 mb-1">
                Operator <span className="text-rose-500">*</span>
              </label>
              <select
                value={operator}
                onChange={(e) => setOperator(e.target.value as RequirementOperator)}
                className="w-full p-2 border border-slate-300 rounded-lg text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900 bg-white"
              >
                <option value="GREATER_THAN_OR_EQUAL">GREATER_THAN_OR_EQUAL (&ge;)</option>
                <option value="GREATER_THAN">GREATER_THAN (&gt;)</option>
                <option value="EQUALS">EQUALS (=)</option>
                <option value="LESS_THAN_OR_EQUAL">LESS_THAN_OR_EQUAL (&le;)</option>
                <option value="LESS_THAN">LESS_THAN (&lt;)</option>
                <option value="EXISTS">EXISTS (Document Present)</option>
                <option value="IS_VALID">IS_VALID (Valid Certificate)</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block font-semibold text-slate-900 mb-1">
              Expected Target / Threshold Value <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={expectedValue}
              onChange={(e) => setExpectedValue(e.target.value)}
              placeholder="e.g. 5000000 or 5 or ISO/IEC 27001"
              className="w-full p-2 border border-slate-300 rounded-lg text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
              required
            />
          </div>

          <div>
            <label className="block font-semibold text-slate-900 mb-1">
              Description / Statutory Clause <span className="text-rose-500">*</span>
            </label>
            <textarea
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide exact qualification criteria as specified in the tender document..."
              className="w-full p-2 border border-slate-300 rounded-lg text-slate-900 text-xs focus:outline-none focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
              required
            />
          </div>

          <div className="flex items-center gap-2 pt-1">
            <input
              type="checkbox"
              id="mandatory-chk"
              checked={mandatory}
              onChange={(e) => setMandatory(e.target.checked)}
              className="w-4 h-4 text-navy-900 rounded border-slate-300 focus:ring-navy-900"
            />
            <label htmlFor="mandatory-chk" className="text-xs font-semibold text-slate-800 cursor-pointer">
              Mandatory Requirement (Failure disqualifies bidder eligibility)
            </label>
          </div>

          {error && <p className="text-xs text-rose-600 font-medium">{error}</p>}

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
              disabled={isSubmitting}
              className="px-4 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors disabled:opacity-50 cursor-pointer"
            >
              {isSubmitting ? 'Saving...' : 'Add Requirement'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
