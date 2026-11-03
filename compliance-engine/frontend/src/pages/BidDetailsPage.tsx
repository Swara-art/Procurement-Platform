import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  FileText,
  Upload,
  Sparkles,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  ShieldAlert,
  ArrowRight
} from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { DocumentUploadModal } from '../components/bids/DocumentUploadModal';
import { bidsApi } from '../api/bidsApi';
import { tendersApi } from '../api/tendersApi';

export const BidDetailsPage: React.FC = () => {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [showUploadModal, setShowUploadModal] = useState(false);
  const [indexingId, setIndexingId] = useState<string | null>(null);

  // Fetch Bid
  const { data: bid, isLoading: isLoadingBid } = useQuery({
    queryKey: ['bid', id],
    queryFn: () => bidsApi.getById(id),
    enabled: !!id,
  });

  // Fetch Documents
  const { data: documents = [], isLoading: isLoadingDocs } = useQuery({
    queryKey: ['bid-documents', id],
    queryFn: () => bidsApi.getDocuments(id),
    enabled: !!id,
  });

  // Fetch Tender
  const { data: tender } = useQuery({
    queryKey: ['tender', bid?.tenderId],
    queryFn: () => (bid?.tenderId ? tendersApi.getById(bid.tenderId) : Promise.resolve(null)),
    enabled: !!bid?.tenderId,
  });

  // Index Document Mutation
  const indexMutation = useMutation({
    mutationFn: (documentId: string) => bidsApi.indexDocument(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bid-documents', id] });
      setIndexingId(null);
    },
  });

  if (isLoadingBid) {
    return <LoadingState message="Loading bid submission details..." />;
  }

  if (!bid) {
    return <div className="p-8 text-center text-xs text-slate-500">Bid not found.</div>;
  }

  const handleIndexClick = (docId: string) => {
    setIndexingId(docId);
    indexMutation.mutate(docId);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Bid Submission #${bid.id.slice(0, 8)}`}
        subtitle={`Submitted by ${bid.bidderName || 'Bidder Organization'}`}
        backUrl="/bids"
        backLabel="Back to Bids"
        badge={<StatusBadge status={bid.status} size="md" />}
        actions={
          <div className="flex items-center gap-2.5">
            <button
              onClick={() => setShowUploadModal(true)}
              className="inline-flex items-center gap-1.5 px-3 py-2 text-xs font-semibold text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg shadow-xs transition-colors cursor-pointer"
            >
              <Upload className="w-3.5 h-3.5" />
              Upload Document PDF
            </button>
            <button
              onClick={() => navigate(`/evaluations/${bid.id}`)}
              className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
            >
              <Sparkles className="w-4 h-4 text-blue-300" />
              AI Evaluation Workspace
            </button>
          </div>
        }
      />

      {/* Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
        {/* Bidder Card */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card space-y-2">
          <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
            Bidder Organization
          </span>
          <div className="flex items-center gap-2 font-bold text-slate-900 text-sm">
            <Building2 className="w-4 h-4 text-navy-900 shrink-0" />
            <span>{bid.bidderName || 'ABC Technologies Pvt Ltd'}</span>
          </div>
          <p className="text-slate-500 font-mono text-[11px]">
            Profile ID: {bid.bidderProfileId}
          </p>
        </div>

        {/* Tender Reference Card */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card space-y-2">
          <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
            Tender Notice
          </span>
          <p className="font-bold text-slate-900 text-sm truncate">
            {tender?.title || bid.tenderTitle || 'Hospital Management System'}
          </p>
          <p className="text-slate-500 font-mono text-[11px]">
            Ref: {tender?.tenderNumber || bid.tenderNumber || 'MH-HEALTH-2026-001'}
          </p>
        </div>

        {/* Quoted Amount Card */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card space-y-2">
          <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
            Commercial Quote
          </span>
          <p className="font-bold font-mono text-slate-900 text-lg">
            ₹{Number(bid.quotedAmount).toLocaleString('en-IN')}
          </p>
          <div className="flex items-center gap-1 text-[11px] text-slate-400">
            <Calendar className="w-3.5 h-3.5" />
            <span>Submitted {new Date(bid.submittedAt).toLocaleDateString()}</span>
          </div>
        </div>
      </div>

      {/* Submitted Verification Documents Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-card overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-900">Submitted Verification Documents</h3>
            <p className="text-xs text-slate-500">
              PDF files parsed and ingested into vector index for evidence retrieval.
            </p>
          </div>
          <button
            onClick={() => setShowUploadModal(true)}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
          >
            <Upload className="w-3.5 h-3.5" />
            Upload PDF
          </button>
        </div>

        {isLoadingDocs ? (
          <LoadingState message="Loading documents..." />
        ) : documents.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500">
            No verification documents uploaded yet. Upload bidder financial statements or certificates.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3 px-5">Document Name</th>
                  <th className="py-3 px-5">File Size</th>
                  <th className="py-3 px-5">Content Type</th>
                  <th className="py-3 px-5">Uploaded Date</th>
                  <th className="py-3 px-5">Vector Index Status</th>
                  <th className="py-3 px-5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {documents.map((doc) => (
                  <tr key={doc.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="py-3.5 px-5">
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-rose-500 shrink-0" />
                        <span className="font-semibold text-slate-900">{doc.originalFilename}</span>
                      </div>
                    </td>
                    <td className="py-3.5 px-5 font-mono text-slate-600">
                      {(doc.fileSize / (1024 * 1024)).toFixed(2)} MB
                    </td>
                    <td className="py-3.5 px-5 font-mono text-slate-500 text-[11px]">
                      {doc.contentType}
                    </td>
                    <td className="py-3.5 px-5 text-slate-500 text-[11px]">
                      {new Date(doc.uploadedAt).toLocaleString()}
                    </td>
                    <td className="py-3.5 px-5">
                      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-[11px] font-medium">
                        <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                        Indexed in Vector Store
                      </span>
                    </td>
                    <td className="py-3.5 px-5 text-right">
                      <button
                        onClick={() => handleIndexClick(doc.id)}
                        disabled={indexingId === doc.id}
                        className="px-2.5 py-1 text-[11px] font-medium text-slate-700 bg-white border border-slate-200 rounded hover:bg-slate-50 transition-colors cursor-pointer"
                      >
                        {indexingId === doc.id ? 'Re-indexing...' : 'Re-index'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* AI Evaluation Shortcut Card */}
      <div className="p-6 bg-gradient-to-r from-navy-900 to-slate-900 rounded-xl text-white shadow-lg flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-blue-300" />
            <h4 className="text-sm font-bold">AI-Assisted Eligibility Verification</h4>
          </div>
          <p className="text-xs text-slate-300 max-w-xl leading-relaxed">
            Run hybrid semantic & keyword RAG analysis against the uploaded balance sheets and certifications to inspect turnover thresholds, years of operation, and statutory registrations.
          </p>
        </div>
        <button
          onClick={() => navigate(`/evaluations/${bid.id}`)}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-500 font-semibold text-xs transition-colors shrink-0 shadow-md cursor-pointer"
        >
          <span>Open Evaluation Workspace</span>
          <ArrowRight className="w-4 h-4" />
        </button>
      </div>

      {/* Upload Document Modal */}
      <DocumentUploadModal
        isOpen={showUploadModal}
        onClose={() => setShowUploadModal(false)}
        bidId={id}
        onUploadSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['bid-documents', id] });
        }}
      />
    </div>
  );
};
