import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Check, ArrowRight, ArrowLeft, Plus, Trash2, FileSpreadsheet, ShieldAlert } from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { organizationsApi } from '../api/organizationsApi';
import { tendersApi } from '../api/tendersApi';
import { CreateTenderRequest, RequirementOperator } from '../types';

interface DynamicRequirement {
  code: string;
  description: string;
  operator: RequirementOperator;
  expectedValue: string;
  mandatory: boolean;
}

export const CreateTenderPage: React.FC = () => {
  const navigate = useNavigate();

  const [currentStep, setCurrentStep] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Fetch registered procurement authority organizations from Spring Boot backend
  const { data: organizations = [] } = useQuery({
    queryKey: ['organizations'],
    queryFn: () => organizationsApi.getAll(),
  });

  const authorities = organizations.filter(
    (o) => o.organizationType === 'PROCUREMENT_AUTHORITY'
  );

  // Step 1: Basic Info
  const [tenderNumber, setTenderNumber] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [procurementAuthorityId, setProcurementAuthorityId] = useState('');

  // Step 2: Financial & Schedule
  const [estimatedValue, setEstimatedValue] = useState<string>('5000000');
  const [submissionDeadline, setSubmissionDeadline] = useState(
    new Date(Date.now() + 45 * 24 * 60 * 60 * 1000).toISOString().slice(0, 16)
  );

  // Step 3: Eligibility Rules
  const [requirements, setRequirements] = useState<DynamicRequirement[]>([
    {
      code: 'MIN_TURNOVER',
      description: 'Minimum average annual turnover for last 3 financial years',
      operator: 'GREATER_THAN_OR_EQUAL',
      expectedValue: '5000000',
      mandatory: true,
    },
    {
      code: 'EXPERIENCE_YEARS',
      description: 'Minimum 5 years of verified enterprise implementation experience',
      operator: 'GREATER_THAN_OR_EQUAL',
      expectedValue: '5',
      mandatory: true,
    },
    {
      code: 'ISO_27001_CERT',
      description: 'Valid ISO/IEC 27001 Information Security Management Certificate',
      operator: 'IS_VALID',
      expectedValue: 'ISO/IEC 27001',
      mandatory: true,
    },
  ]);

  // Set default authority if available
  React.useEffect(() => {
    if (authorities.length > 0 && !procurementAuthorityId) {
      setProcurementAuthorityId(authorities[0].id);
    }
  }, [authorities, procurementAuthorityId]);

  const addRequirementRow = () => {
    setRequirements([
      ...requirements,
      {
        code: '',
        description: '',
        operator: 'GREATER_THAN_OR_EQUAL',
        expectedValue: '',
        mandatory: true,
      },
    ]);
  };

  const removeRequirementRow = (index: number) => {
    setRequirements(requirements.filter((_, i) => i !== index));
  };

  const updateRequirement = (index: number, field: keyof DynamicRequirement, value: any) => {
    const updated = [...requirements];
    updated[index] = { ...updated[index], [field]: value };
    setRequirements(updated);
  };

  const handleNext = () => {
    setErrorMessage('');
    if (currentStep === 1) {
      if (!tenderNumber.trim() || !title.trim() || !procurementAuthorityId) {
        setErrorMessage('Tender Reference Number, Title, and Procuring Authority are mandatory.');
        return;
      }
    } else if (currentStep === 2) {
      const val = parseFloat(estimatedValue);
      if (isNaN(val) || val <= 0) {
        setErrorMessage('Please enter a valid positive estimated contract amount in INR.');
        return;
      }
      if (!submissionDeadline) {
        setErrorMessage('Submission deadline is required.');
        return;
      }
    }
    setCurrentStep((s) => s + 1);
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const payload: CreateTenderRequest = {
        tenderNumber: tenderNumber.trim(),
        title: title.trim(),
        description: description.trim(),
        procurementAuthorityId,
        estimatedValue: parseFloat(estimatedValue),
        submissionDeadline: new Date(submissionDeadline).toISOString(),
      };

      const createdTender = await tendersApi.create(payload);

      // Create all defined eligibility requirements
      for (const req of requirements) {
        if (req.code && req.description && req.expectedValue) {
          await tendersApi.createRequirement(createdTender.id, {
            requirementCode: req.code.trim().toUpperCase(),
            description: req.description.trim(),
            operator: req.operator,
            expectedValue: req.expectedValue.trim(),
            mandatory: req.mandatory,
          });
        }
      }

      setIsSubmitting(false);
      navigate(`/tenders/${createdTender.id}`);
    } catch (err: any) {
      setErrorMessage(err?.response?.data?.message || err?.message || 'Failed to gazette tender.');
      setIsSubmitting(false);
    }
  };

  const steps = [
    { number: 1, label: 'Basic Details' },
    { number: 2, label: 'Value & Timeline' },
    { number: 3, label: 'Eligibility Rules' },
    { number: 4, label: 'Review & Publish' },
  ];

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <PageHeader
        title="Create Procurement Tender"
        subtitle="Define statutory qualification criteria, submission windows, and automated evaluation rules."
        backUrl="/tenders"
        backLabel="Back to Tenders"
      />

      {/* Stepper Progress Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-card">
        <div className="flex items-center justify-between">
          {steps.map((s, idx) => {
            const isCompleted = currentStep > s.number;
            const isCurrent = currentStep === s.number;
            return (
              <React.Fragment key={s.number}>
                <div className="flex items-center gap-3">
                  <div
                    className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs transition-all ${
                      isCompleted
                        ? 'bg-emerald-600 text-white'
                        : isCurrent
                        ? 'bg-navy-900 text-white ring-4 ring-navy-900/10'
                        : 'bg-slate-100 text-slate-500 border border-slate-200'
                    }`}
                  >
                    {isCompleted ? <Check className="w-4 h-4" /> : s.number}
                  </div>
                  <div className="hidden sm:block text-left">
                    <span className="text-[10px] text-slate-400 uppercase font-semibold block">
                      Step {s.number}
                    </span>
                    <span
                      className={`text-xs font-semibold ${
                        isCurrent ? 'text-navy-900' : 'text-slate-600'
                      }`}
                    >
                      {s.label}
                    </span>
                  </div>
                </div>
                {idx < steps.length - 1 && (
                  <div
                    className={`flex-1 h-0.5 mx-3 ${
                      isCompleted ? 'bg-emerald-500' : 'bg-slate-200'
                    }`}
                  />
                )}
              </React.Fragment>
            );
          })}
        </div>
      </div>

      {errorMessage && (
        <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Step Container Card */}
      <div className="bg-white p-6 sm:p-8 rounded-xl border border-slate-200 shadow-card">
        {/* Step 1: Basic Information */}
        {currentStep === 1 && (
          <div className="space-y-4 text-xs">
            <h2 className="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3">
              1. Tender Identification & Authority
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block font-semibold text-slate-800 mb-1">
                  Tender Reference Number <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={tenderNumber}
                  onChange={(e) => setTenderNumber(e.target.value)}
                  placeholder="e.g. MH-HEALTH-2026-004"
                  className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 font-mono text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
                />
                <span className="text-[10px] text-slate-400 mt-1 block">
                  Must be unique across state or central procurement gazettes.
                </span>
              </div>

              <div>
                <label className="block font-semibold text-slate-800 mb-1">
                  Procuring Department / Authority <span className="text-rose-500">*</span>
                </label>
                <select
                  value={procurementAuthorityId}
                  onChange={(e) => setProcurementAuthorityId(e.target.value)}
                  className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900 bg-white"
                >
                  {authorities.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name} ({a.registrationNumber})
                    </option>
                  ))}
                  {authorities.length === 0 && (
                    <option value="">
                      -- No Procurement Authority Registered in DB (Create one in Organizations) --
                    </option>
                  )}
                </select>
              </div>
            </div>

            <div>
              <label className="block font-semibold text-slate-800 mb-1">
                Tender Title <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Hospital Management System and Electronic Medical Records"
                className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-800 mb-1">
                Detailed Procurement Scope & Description
              </label>
              <textarea
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Comprehensive scope of work, technical specifications, and delivery schedule..."
                className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
              />
            </div>
          </div>
        )}

        {/* Step 2: Financial & Schedule */}
        {currentStep === 2 && (
          <div className="space-y-4 text-xs">
            <h2 className="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3">
              2. Contract Valuation & Timelines
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block font-semibold text-slate-800 mb-1">
                  Estimated Contract Value (INR ₹) <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 font-semibold">
                    ₹
                  </span>
                  <input
                    type="number"
                    step="0.01"
                    value={estimatedValue}
                    onChange={(e) => setEstimatedValue(e.target.value)}
                    placeholder="5000000"
                    className="w-full pl-8 pr-3 py-2.5 border border-slate-300 rounded-lg text-slate-900 font-mono text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
                  />
                </div>
                <span className="text-[10px] text-slate-400 mt-1 block">
                  Amount in Indian Rupees: ₹{Number(estimatedValue || 0).toLocaleString('en-IN')}
                </span>
              </div>

              <div>
                <label className="block font-semibold text-slate-800 mb-1">
                  Submission Deadline (Date & Time) <span className="text-rose-500">*</span>
                </label>
                <input
                  type="datetime-local"
                  value={submissionDeadline}
                  onChange={(e) => setSubmissionDeadline(e.target.value)}
                  className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
                />
                <span className="text-[10px] text-slate-400 mt-1 block">
                  Submissions close automatically at this scheduled timestamp.
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Step 3: Eligibility Rules */}
        {currentStep === 3 && (
          <div className="space-y-4 text-xs">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div>
                <h2 className="text-sm font-bold text-slate-900">
                  3. Automated Eligibility Criteria
                </h2>
                <p className="text-slate-500 text-[11px]">
                  These conditions will be automatically verified against uploaded bidder PDFs using AI evidence extraction.
                </p>
              </div>
              <button
                type="button"
                onClick={addRequirementRow}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-navy-900 text-white font-semibold text-xs hover:bg-navy-800 transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                Add Rule
              </button>
            </div>

            <div className="space-y-3">
              {requirements.map((req, idx) => (
                <div
                  key={idx}
                  className="p-4 bg-slate-50/70 rounded-xl border border-slate-200 space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-700 text-xs">Rule #{idx + 1}</span>
                    <button
                      type="button"
                      onClick={() => removeRequirementRow(idx)}
                      className="text-slate-400 hover:text-rose-600 transition-colors cursor-pointer"
                      title="Delete rule"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div>
                      <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                        Requirement Code
                      </label>
                      <input
                        type="text"
                        value={req.code}
                        onChange={(e) => updateRequirement(idx, 'code', e.target.value.toUpperCase())}
                        placeholder="e.g. MIN_TURNOVER"
                        className="w-full p-2 border border-slate-300 rounded-lg text-slate-900 font-mono text-xs bg-white"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                        Operator
                      </label>
                      <select
                        value={req.operator}
                        onChange={(e) => updateRequirement(idx, 'operator', e.target.value as RequirementOperator)}
                        className="w-full p-2 border border-slate-300 rounded-lg text-slate-900 text-xs bg-white"
                      >
                        <option value="GREATER_THAN_OR_EQUAL">GREATER_THAN_OR_EQUAL (&ge;)</option>
                        <option value="GREATER_THAN">GREATER_THAN (&gt;)</option>
                        <option value="EQUALS">EQUALS (=)</option>
                        <option value="IS_VALID">IS_VALID (Valid Certificate)</option>
                        <option value="EXISTS">EXISTS (Document Present)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                        Expected Value
                      </label>
                      <input
                        type="text"
                        value={req.expectedValue}
                        onChange={(e) => updateRequirement(idx, 'expectedValue', e.target.value)}
                        placeholder="e.g. 5000000 or ISO 27001"
                        className="w-full p-2 border border-slate-300 rounded-lg text-slate-900 text-xs bg-white"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                      Statutory Description
                    </label>
                    <input
                      type="text"
                      value={req.description}
                      onChange={(e) => updateRequirement(idx, 'description', e.target.value)}
                      placeholder="e.g. Minimum average annual turnover over 3 years must be Rs. 50 Lakhs"
                      className="w-full p-2 border border-slate-300 rounded-lg text-slate-900 text-xs bg-white"
                    />
                  </div>

                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      id={`mand-${idx}`}
                      checked={req.mandatory}
                      onChange={(e) => updateRequirement(idx, 'mandatory', e.target.checked)}
                      className="rounded border-slate-300 text-navy-900 focus:ring-navy-900"
                    />
                    <label htmlFor={`mand-${idx}`} className="text-slate-700 text-xs font-semibold cursor-pointer">
                      Mandatory Condition
                    </label>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Step 4: Review & Publish */}
        {currentStep === 4 && (
          <div className="space-y-5 text-xs">
            <h2 className="text-sm font-bold text-slate-900 border-b border-slate-100 pb-3">
              4. Review Gazette Information Before Publishing
            </h2>

            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-slate-400 text-[11px] block">Tender Number</span>
                  <span className="font-mono font-bold text-slate-900 text-xs">{tenderNumber}</span>
                </div>
                <div>
                  <span className="text-slate-400 text-[11px] block">Estimated Value</span>
                  <span className="font-mono font-bold text-slate-900 text-xs">
                    ₹{Number(estimatedValue).toLocaleString('en-IN')}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 text-[11px] block">Title</span>
                  <span className="font-semibold text-slate-900 text-xs">{title}</span>
                </div>
                <div>
                  <span className="text-slate-400 text-[11px] block">Submission Deadline</span>
                  <span className="font-semibold text-slate-900 text-xs">
                    {new Date(submissionDeadline).toLocaleString()}
                  </span>
                </div>
              </div>
              {description && (
                <div className="pt-2 border-t border-slate-200">
                  <span className="text-slate-400 text-[11px] block">Description</span>
                  <p className="text-slate-700 text-xs mt-0.5 leading-relaxed">{description}</p>
                </div>
              )}
            </div>

            <div>
              <span className="font-bold text-slate-800 block mb-2">
                Configured Requirements ({requirements.length}):
              </span>
              <div className="space-y-1.5">
                {requirements.map((r, i) => (
                  <div
                    key={i}
                    className="p-2.5 rounded-lg border border-slate-200 bg-white flex items-center justify-between text-xs"
                  >
                    <div>
                      <span className="font-mono font-semibold text-slate-900 mr-2">
                        {r.code}
                      </span>
                      <span className="text-slate-600">{r.description}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-slate-700 bg-slate-100 px-2 py-0.5 rounded text-[11px]">
                        {r.operator} {r.expectedValue}
                      </span>
                      {r.mandatory && (
                        <span className="px-1.5 py-0.2 rounded text-[10px] bg-rose-50 text-rose-600 border border-rose-200 font-bold">
                          Mandatory
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-amber-900 text-[11px] leading-relaxed">
              Upon submission, this tender will be registered in the system. Procurement officers can proceed to publish it to allow public bidder submissions.
            </div>
          </div>
        )}

        {/* Stepper Buttons */}
        <div className="flex items-center justify-between pt-6 mt-6 border-t border-slate-100">
          {currentStep > 1 ? (
            <button
              type="button"
              onClick={() => setCurrentStep((s) => s - 1)}
              className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors cursor-pointer"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              Previous Step
            </button>
          ) : (
            <div />
          )}

          {currentStep < 4 ? (
            <button
              type="button"
              onClick={handleNext}
              className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
            >
              Continue to Step {currentStep + 1}
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          ) : (
            <button
              type="button"
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="inline-flex items-center gap-2 px-5 py-2 text-xs font-bold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors disabled:opacity-50 cursor-pointer"
            >
              <FileSpreadsheet className="w-4 h-4" />
              {isSubmitting ? 'Registering Tender...' : 'Complete & Register Tender'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
