import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Building2,
  Calendar,
  Globe,
  Plus,
  Eye,
  Sparkles,
  Layers,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { ConfirmationDialog } from '../components/common/ConfirmationDialog';
import { CreateRequirementModal } from '../components/tenders/CreateRequirementModal';
import { tendersApi } from '../api/tendersApi';
import { bidsApi } from '../api/bidsApi';
import { organizationsApi } from '../api/organizationsApi';
import { useAuth } from '../context/AuthContext';

export const TenderDetailsPage: React.FC = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const [activeTab, setActiveTab] = useState<'overview' | 'requirements' | 'bids' | 'results'>('overview');
  const [showPublishDialog, setShowPublishDialog] = useState(false);
  const [showAddReqModal, setShowAddReqModal] = useState(false);
  const [quotedAmount, setQuotedAmount] = useState('');

  // Fetch Tender
  const { data: tender, isLoading: isLoadingTender } = useQuery({
    queryKey: ['tender', id],
    queryFn: () => tendersApi.getById(id),
    enabled: !!id,
  });

  // Fetch Requirements
  const { data: requirements = [], isLoading: isLoadingReqs } = useQuery({
    queryKey: ['requirements', id],
    queryFn: () => tendersApi.getRequirements(id),
    enabled: !!id,
  });

  // Fetch Bids
  const { data: bids = [], isLoading: isLoadingBids } = useQuery({
    queryKey: ['bids', id],
    queryFn: () => bidsApi.getBidsByTender(id),
    enabled: !!id,
  });

  // Fetch Authority details
  const { data: authority } = useQuery({
    queryKey: ['authority', tender?.procurementAuthorityId],
    queryFn: () =>
      tender?.procurementAuthorityId
        ? organizationsApi.getById(tender.procurementAuthorityId)
        : Promise.resolve(null),
    enabled: !!tender?.procurementAuthorityId,
  });

  // Publish Tender Mutation
  const publishMutation = useMutation({
    mutationFn: () => tendersApi.publish(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tender', id] });
      queryClient.invalidateQueries({ queryKey: ['tenders'] });
      setShowPublishDialog(false);
    },
  });
  const bidMutation = useMutation({ mutationFn: () => bidsApi.submitBid(id, { quotedAmount: Number(quotedAmount) }), onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['bids', id] }); queryClient.invalidateQueries({ queryKey: ['bids'] }); setQuotedAmount(''); } });

  if (isLoadingTender) {
    return <LoadingState message="Loading tender details from government registry..." />;
  }

  if (!tender) {
    return (
      <div className="p-8 text-center text-xs text-slate-500">
        Tender not found.
      </div>
    );
  }

  const isOfficerOrAdmin = user?.role === 'PROCUREMENT_OFFICER' || user?.role === 'SYSTEM_ADMIN';
  const hasSubmittedBid = bids.length > 0;

  const tabs = [
    { id: 'overview', label: 'Overview' },
    { id: 'requirements', label: `Requirements (${requirements.length})` },
    { id: 'bids', label: `Submitted Bids (${bids.length})` },
    { id: 'results', label: 'Evaluation Results' },
  ];

  return (
    <div className="space-y-6">
      {/* Top Banner / PageHeader */}
      <PageHeader
        title={tender.title}
        subtitle={`Reference Number: ${tender.tenderNumber}`}
        backUrl="/tenders"
        backLabel="Back to Tenders"
        badge={<StatusBadge status={tender.status} size="md" />}
        actions={
          <div className="flex items-center gap-2.5">
            {tender.status === 'DRAFT' && isOfficerOrAdmin && (
              <button
                onClick={() => setShowPublishDialog(true)}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-lg shadow-sm transition-colors cursor-pointer"
              >
                <Globe className="w-3.5 h-3.5" />
                Publish Tender to Gazette
              </button>
            )}
            {isOfficerOrAdmin && (
              <button
                onClick={() => setShowAddReqModal(true)}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg shadow-xs transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                Add Requirement
              </button>
            )}
            {user?.role === 'BIDDER' && tender.status === 'PUBLISHED' && new Date(tender.submissionDeadline) > new Date() && !hasSubmittedBid && <button onClick={() => document.getElementById('submit-bid-form')?.scrollIntoView({ behavior: 'smooth' })} className="inline-flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold text-white bg-emerald-700 hover:bg-emerald-800 rounded-lg">Submit a Bid</button>}
          </div>
        }
      />

      {/* Tender Metadata Ribbon */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-card grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs">
        <div>
          <span className="text-slate-400 text-[11px] block">Procurement Authority</span>
          <div className="flex items-center gap-1.5 font-semibold text-slate-900 mt-0.5">
            <Building2 className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <span className="truncate">{authority?.name || 'Registered Procurement Authority'}</span>
          </div>
        </div>
        <div>
          <span className="text-slate-400 text-[11px] block">Estimated Value</span>
          <span className="font-mono font-bold text-slate-900 mt-0.5 block">
            ₹{Number(tender.estimatedValue).toLocaleString('en-IN')}
          </span>
        </div>
        <div>
          <span className="text-slate-400 text-[11px] block">Submission Deadline</span>
          <div className="flex items-center gap-1 text-slate-800 mt-0.5 font-medium">
            <Calendar className="w-3.5 h-3.5 text-slate-400 shrink-0" />
            <span>{new Date(tender.submissionDeadline).toLocaleDateString()}</span>
          </div>
        </div>
        <div>
          <span className="text-slate-400 text-[11px] block">Created Date</span>
          <span className="text-slate-600 mt-0.5 block">
            {new Date(tender.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>

      {/* Tabs Navigation (Untitled UI style) */}
      <div className="border-b border-slate-200">
        <nav className="flex space-x-6">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`pb-3 text-xs font-semibold border-b-2 transition-colors cursor-pointer ${
                activeTab === tab.id
                  ? 'border-navy-900 text-navy-900'
                  : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab 1: Overview */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {user?.role === 'BIDDER' && tender.status === 'PUBLISHED' && new Date(tender.submissionDeadline) > new Date() && !hasSubmittedBid && <div id="submit-bid-form" className="md:col-span-3 bg-white p-5 rounded-xl border border-emerald-200 shadow-card"><h3 className="font-semibold text-slate-900 text-sm">Submit your bid</h3><p className="mt-1 text-xs text-slate-500">Your bidder organization will be linked automatically.</p><form className="mt-4 flex flex-wrap gap-3" onSubmit={(e)=>{e.preventDefault();bidMutation.mutate();}}><label className="text-xs text-slate-600">Quoted amount (₹)<input type="number" min="1" step="0.01" required value={quotedAmount} onChange={e=>setQuotedAmount(e.target.value)} className="block mt-1 p-2 border border-slate-300 rounded-lg"/></label><button disabled={bidMutation.isPending} className="self-end px-4 py-2 rounded-lg bg-emerald-700 text-white text-xs font-semibold disabled:opacity-50">{bidMutation.isPending?'Submitting…':'Publish Bid'}</button>{bidMutation.isError&&<p className="basis-full text-xs text-rose-600">{(bidMutation.error as {response?:{data?:{message?:string}}}).response?.data?.message||'Bid submission failed.'}</p>}</form></div>}
          <div className="md:col-span-2 space-y-6">
            <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-card space-y-3">
              <h3 className="text-sm font-bold text-slate-900">Procurement Scope</h3>
              <p className="text-xs leading-relaxed text-slate-700">
                {tender.description || 'No detailed scope description provided.'}
              </p>
            </div>

            <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-card space-y-3">
              <h3 className="text-sm font-bold text-slate-900">Statutory Notice</h3>
              <p className="text-xs leading-relaxed text-slate-600">
                All submissions are subject to the Public Procurement (Preference to Make in India) Order and statutory CVC guidelines. Bidder documents must be submitted in searchable PDF format for machine-readable verification.
              </p>
            </div>
          </div>

          <div className="space-y-4">
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card text-xs space-y-3">
              <h4 className="font-bold text-slate-900">Submission Timeline</h4>
              <div className="space-y-2 border-t border-slate-100 pt-3">
                <div className="flex justify-between">
                  <span className="text-slate-500">Notice Published:</span>
                  <span className="font-semibold text-slate-800">
                    {new Date(tender.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Deadline:</span>
                  <span className="font-semibold text-slate-800">
                    {new Date(tender.submissionDeadline).toLocaleDateString()}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Technical Bid Opening:</span>
                  <span className="font-semibold text-slate-800">Next Business Day</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Requirements */}
      {activeTab === 'requirements' && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-card overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Configured Eligibility Requirements</h3>
              <p className="text-xs text-slate-500">
                AI evaluation models check bidder PDF evidence against these exact conditions.
              </p>
            </div>
            {isOfficerOrAdmin && (
              <button
                onClick={() => setShowAddReqModal(true)}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                Add Requirement
              </button>
            )}
          </div>

          {isLoadingReqs ? (
            <LoadingState message="Loading requirements..." />
          ) : requirements.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-500">
              No eligibility requirements defined yet. Add the first rule.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                    <th className="py-3 px-4">Requirement Code</th>
                    <th className="py-3 px-4">Description</th>
                    <th className="py-3 px-4">Operator</th>
                    <th className="py-3 px-4">Expected Value</th>
                    <th className="py-3 px-4">Mandatory</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {requirements.map((req) => (
                    <tr key={req.id} className="hover:bg-slate-50/60 transition-colors">
                      <td className="py-3.5 px-4 font-mono font-semibold text-slate-900">
                        {req.requirementCode}
                      </td>
                      <td className="py-3.5 px-4 text-slate-700 max-w-sm">
                        {req.description}
                      </td>
                      <td className="py-3.5 px-4 font-mono text-slate-600">
                        <span className="px-2 py-0.5 rounded bg-slate-100 text-[11px]">
                          {req.operator}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-900">
                        {req.expectedValue}
                      </td>
                      <td className="py-3.5 px-4">
                        {req.mandatory ? (
                          <span className="px-2 py-0.5 bg-rose-50 text-rose-700 border border-rose-200 text-[10px] font-bold rounded">
                            Mandatory
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 bg-slate-100 text-slate-600 text-[10px] rounded">
                            Optional
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Submitted Bids */}
      {activeTab === 'bids' && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-card overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Bids Submitted by Suppliers</h3>
              <p className="text-xs text-slate-500">
                Official bids registered for tender {tender.tenderNumber}
              </p>
            </div>
          </div>

          {isLoadingBids ? (
            <LoadingState message="Loading submitted bids..." />
          ) : bids.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-500">
              No bids have been submitted yet for this tender.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                    <th className="py-3 px-4">Bid ID</th>
                    <th className="py-3 px-4">Bidder Organization</th>
                    <th className="py-3 px-4">Quoted Amount</th>
                    <th className="py-3 px-4">Submitted At</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {bids.map((bid) => (
                    <tr key={bid.id} className="hover:bg-slate-50/60 transition-colors">
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-900 text-[11px]">
                        {bid.id.slice(0, 8)}...
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-900">
                        {bid.bidderName || 'ABC Technologies Pvt Ltd'}
                      </td>
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-900">
                        ₹{Number(bid.quotedAmount).toLocaleString('en-IN')}
                      </td>
                      <td className="py-3.5 px-4 text-slate-500 text-[11px]">
                        {new Date(bid.submittedAt).toLocaleString()}
                      </td>
                      <td className="py-3.5 px-4">
                        <StatusBadge status={bid.status} />
                      </td>
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <div className="inline-flex items-center gap-2">
                          <button
                            onClick={() => navigate(`/evaluations/${bid.id}`)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-2xs transition-colors cursor-pointer"
                          >
                            <Sparkles className="w-3 h-3 text-blue-300" />
                            AI Evaluation
                          </button>
                          <button
                            onClick={() => navigate(`/bids/${bid.id}`)}
                            className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-100 rounded-lg shadow-2xs transition-colors cursor-pointer"
                          >
                            <Eye className="w-3 h-3" />
                            Bid File
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 4: Evaluation Results */}
      {activeTab === 'results' && (
        <div className="bg-white rounded-xl border border-slate-200 shadow-card p-6 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div>
              <h3 className="text-sm font-bold text-slate-900">Tender Evaluation Summary Matrix</h3>
              <p className="text-xs text-slate-500">
                Comparative eligibility verification status across all participating suppliers.
              </p>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3 px-4">Bidder</th>
                  <th className="py-3 px-4">Turnover Check</th>
                  <th className="py-3 px-4">Experience Check</th>
                  <th className="py-3 px-4">ISO 27001 Check</th>
                  <th className="py-3 px-4">Overall Compliance</th>
                  <th className="py-3 px-4 text-right">Audit Workspace</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {bids.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-slate-400">
                      No bids submitted yet for this tender to evaluate.
                    </td>
                  </tr>
                ) : (
                  bids.map((b) => (
                    <tr key={b.id} className="hover:bg-slate-50/60">
                      <td className="py-3.5 px-4 font-semibold text-slate-900">
                        {b.bidderName || `Bidder Profile #${b.bidderProfileId.slice(0, 8)}`}
                      </td>
                      <td className="py-3.5 px-4">
                        <StatusBadge status={b.status} />
                      </td>
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-800">
                        ₹{Number(b.quotedAmount).toLocaleString('en-IN')}
                      </td>
                      <td className="py-3.5 px-4 text-slate-500 text-[11px]">
                        {new Date(b.submittedAt).toLocaleDateString()}
                      </td>
                      <td className="py-3.5 px-4">
                        <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-700 text-[11px] font-mono">
                          {b.status}
                        </span>
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <button
                          onClick={() => navigate(`/evaluations/${b.id}`)}
                          className="text-xs font-semibold text-blue-600 hover:text-blue-800"
                        >
                          Open Workspace &rarr;
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Confirmation Dialog for Publishing Tender */}
      <ConfirmationDialog
        isOpen={showPublishDialog}
        onClose={() => setShowPublishDialog(false)}
        onConfirm={() => publishMutation.mutate()}
        title="Publish Tender to Gazette?"
        description="Publishing this tender will make it immediately active for public supplier bids. Requirement conditions cannot be modified after publication."
        confirmLabel="Publish Tender"
        isLoading={publishMutation.isPending}
      />

      {/* Modal for Adding Eligibility Requirement */}
      <CreateRequirementModal
        isOpen={showAddReqModal}
        onClose={() => setShowAddReqModal(false)}
        tenderId={id}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['requirements', id] });
        }}
      />
    </div>
  );
};
