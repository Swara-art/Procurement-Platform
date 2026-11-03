import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  FileSpreadsheet,
  Layers,
  Clock,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  Plus,
  Eye,
  Building2,
  Calendar,
  Sparkles,
  Inbox
} from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { tendersApi } from '../api/tendersApi';
import { bidsApi } from '../api/bidsApi';
import { useAuth } from '../context/AuthContext';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const { data: tenders = [], isLoading: isLoadingTenders } = useQuery({
    queryKey: ['tenders'],
    queryFn: () => tendersApi.getAll(),
  });

  const { data: bids = [], isLoading: isLoadingBids } = useQuery({
    queryKey: ['bids'],
    queryFn: () => bidsApi.getAll(),
  });

  const activeTendersCount = tenders.filter((t) => t.status === 'PUBLISHED').length;
  const bidsReceivedCount = bids.length;
  const pendingEvalsCount = bids.filter((b) => b.status === 'SUBMITTED' || b.status === 'UNDER_REVIEW').length;
  const evalsCompletedCount = bids.filter((b) => b.status === 'ACCEPTED' || b.status === 'REJECTED' || b.status === 'EVALUATED').length;

  const isOfficerOrAdmin = user?.role === 'PROCUREMENT_OFFICER' || user?.role === 'SYSTEM_ADMIN';

  if (isLoadingTenders || isLoadingBids) {
    return <LoadingState message="Querying live procurement database..." />;
  }

  const recentTenders = tenders.slice(0, 5);
  const pendingBids = bids.filter((b) => b.status === 'SUBMITTED' || b.status === 'UNDER_REVIEW');

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title={user?.role === 'BIDDER' ? 'Bidder Dashboard' : user?.role === 'EVALUATION_OFFICER' ? 'Evaluation Dashboard' : user?.role === 'SYSTEM_ADMIN' ? 'System Administration Dashboard' : 'Procurement Dashboard'}
        subtitle={`Welcome back, ${user?.name}. Real-time statistics from connected PostgreSQL database.`}
        actions={
          isOfficerOrAdmin ? (
            <button
              onClick={() => navigate('/tenders/create')}
              className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              Create Tender
            </button>
          ) : null
        }
      />

      {/* 4 Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Active Tenders Card */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Active Tenders
            </span>
            <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <FileSpreadsheet className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold tracking-tight text-slate-900">
              {activeTendersCount}
            </span>
            <span className="text-[11px] font-semibold text-blue-700 bg-blue-50 px-1.5 py-0.5 rounded">
              Published
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2">
            {tenders.length} total tenders in DB
          </p>
        </div>

        {/* Bids Received Card */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Bids Received
            </span>
            <div className="w-8 h-8 rounded-lg bg-sky-50 text-sky-600 flex items-center justify-center">
              <Layers className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold tracking-tight text-slate-900">
              {bidsReceivedCount}
            </span>
            <span className="text-[11px] font-semibold text-sky-600 bg-sky-50 px-1.5 py-0.5 rounded">
              Submitted
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2">
            Submissions in PostgreSQL
          </p>
        </div>

        {/* Pending Evaluations Card */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Pending Evaluations
            </span>
            <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold tracking-tight text-slate-900">
              {pendingEvalsCount}
            </span>
            <span className="text-[11px] font-semibold text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded">
              Pending Action
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2">
            Awaiting AI checks & review
          </p>
        </div>

        {/* Evaluations Completed Card */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-card">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">
              Evaluations Completed
            </span>
            <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold tracking-tight text-slate-900">
              {evalsCompletedCount}
            </span>
            <span className="text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded">
              Completed
            </span>
          </div>
          <p className="text-[11px] text-slate-400 mt-2">
            Recorded decisions
          </p>
        </div>
      </div>

      {/* Main Grid: Recent Tenders Table & Pending Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent Tenders Table (2 cols) */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 shadow-card overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900">Recent Tenders</h2>
              <p className="text-xs text-slate-500">Tenders stored in the connected PostgreSQL database</p>
            </div>
            {tenders.length > 0 && (
              <button
                onClick={() => navigate('/tenders')}
                className="text-xs font-semibold text-navy-900 hover:text-blue-600 flex items-center gap-1 transition-colors cursor-pointer"
              >
                View all
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          <div className="overflow-x-auto">
            {recentTenders.length === 0 ? (
              <div className="p-12 text-center text-xs text-slate-500 space-y-2">
                <Inbox className="w-8 h-8 text-slate-300 mx-auto" />
                <p className="font-semibold text-slate-700">No tenders in the database yet.</p>
                <p className="text-slate-400">Create a tender to publish procurement notices.</p>
                {isOfficerOrAdmin && (
                  <button
                    onClick={() => navigate('/tenders/create')}
                    className="mt-2 inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Create First Tender
                  </button>
                )}
              </div>
            ) : (
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                    <th className="py-3 px-4">Tender ID / Ref</th>
                    <th className="py-3 px-4">Tender Title</th>
                    <th className="py-3 px-4">Deadline</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-xs">
                  {recentTenders.map((tender) => (
                    <tr key={tender.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3.5 px-4 font-mono font-medium text-slate-900 text-[11px]">
                        {tender.tenderNumber}
                      </td>
                      <td className="py-3.5 px-4 font-semibold text-slate-800 max-w-xs truncate">
                        {tender.title}
                      </td>
                      <td className="py-3.5 px-4 text-slate-500 whitespace-nowrap">
                        <div className="flex items-center gap-1 text-[11px]">
                          <Calendar className="w-3 h-3 text-slate-400" />
                          {new Date(tender.submissionDeadline).toLocaleDateString()}
                        </div>
                      </td>
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <StatusBadge status={tender.status} />
                      </td>
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <button
                          onClick={() => navigate(`/tenders/${tender.id}`)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 text-xs font-semibold text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-100 hover:text-navy-900 shadow-2xs transition-all cursor-pointer"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          Details
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        {/* Dynamic Pending Actions Panel */}
        <div className="space-y-4">
          <div className="bg-white rounded-xl border border-slate-200 shadow-card p-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h2 className="text-sm font-bold text-slate-900">Pending Actions</h2>
              <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-semibold text-[10px]">
                {pendingBids.length} Actionable
              </span>
            </div>

            <div className="mt-3.5 space-y-3">
              {pendingBids.length === 0 ? (
                <div className="p-6 text-center text-xs text-slate-400">
                  <CheckCircle2 className="w-6 h-6 text-emerald-500 mx-auto mb-1.5" />
                  <span>No bids awaiting evaluation.</span>
                </div>
              ) : (
                pendingBids.slice(0, 3).map((b) => (
                  <div key={b.id} className="p-3 rounded-lg bg-slate-50 border border-slate-200">
                    <div className="flex items-start gap-2.5">
                      <div className="w-7 h-7 rounded-md bg-amber-50 text-amber-600 flex items-center justify-center shrink-0 mt-0.5">
                        <AlertTriangle className="w-3.5 h-3.5" />
                      </div>
                      <div className="flex-1 min-w-0 text-xs">
                        <p className="font-semibold text-slate-900 truncate">
                          Bid #{b.id.slice(0, 8)} awaiting evaluation
                        </p>
                        <p className="text-[11px] text-slate-500 mt-0.5">
                          Tender: {b.tenderNumber || b.tenderTitle || 'Public Tender'}
                        </p>
                        <button
                          onClick={() => navigate(`/evaluations/${b.id}`)}
                          className="mt-2 inline-flex items-center gap-1.5 text-xs font-bold text-blue-600 hover:text-blue-800 cursor-pointer"
                        >
                          <Sparkles className="w-3 h-3" />
                          Open Evaluation Workspace &rarr;
                        </button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
