import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Eye, Sparkles, Layers, FileText, ChevronLeft, ChevronRight } from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { StatusBadge } from '../components/common/StatusBadge';
import { SearchAndFilters } from '../components/common/SearchAndFilters';
import { LoadingState } from '../components/common/LoadingState';
import { EmptyState } from '../components/common/EmptyState';
import { bidsApi } from '../api/bidsApi';
import { useAuth } from '../context/AuthContext';

export const BidsPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 5;

  const { data: bids = [], isLoading, error } = useQuery({
    queryKey: ['bids'],
    queryFn: () => bidsApi.getAll(),
  });

  const filteredBids = useMemo(() => {
    return bids.filter((b) => {
      const matchesSearch =
        b.id.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (b.bidderName && b.bidderName.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (b.tenderTitle && b.tenderTitle.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (b.tenderNumber && b.tenderNumber.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesStatus =
        statusFilter === 'ALL' || b.status.toUpperCase() === statusFilter.toUpperCase();

      return matchesSearch && matchesStatus;
    });
  }, [bids, searchQuery, statusFilter]);

  const totalPages = Math.ceil(filteredBids.length / pageSize) || 1;
  const paginatedBids = filteredBids.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const filterOptions = [
    { label: 'All Bids', value: 'ALL', count: bids.length },
    { label: 'Submitted', value: 'SUBMITTED', count: bids.filter((b) => b.status === 'SUBMITTED').length },
    { label: 'Under Review', value: 'UNDER_REVIEW', count: bids.filter((b) => b.status === 'UNDER_REVIEW').length },
    { label: 'Evaluated', value: 'EVALUATED', count: bids.filter((b) => b.status === 'EVALUATED' || b.status === 'ACCEPTED').length },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Bid Submissions Management"
        subtitle="Review supplier bids, verify submitted qualification documents, and initiate AI-assisted evaluations."
      />

      {/* Filter and Search */}
      <SearchAndFilters
        searchPlaceholder="Search by Bid ID, bidder organization, or tender..."
        searchValue={searchQuery}
        onSearchChange={(q) => {
          setSearchQuery(q);
          setCurrentPage(1);
        }}
        filters={{
          current: statusFilter,
          options: filterOptions,
          onChange: (val) => {
            setStatusFilter(val);
            setCurrentPage(1);
          },
        }}
      />

      {isLoading ? (
        <LoadingState message="Fetching supplier bids..." />
      ) : error ? (
        <EmptyState
          icon={Layers}
          title="Could not load bids"
          description="Failed to communicate with the bid repository."
        />
      ) : filteredBids.length === 0 ? (
        <EmptyState
          icon={Layers}
          title="No bids found"
          description={
            searchQuery
              ? `No bid records matched "${searchQuery}".`
              : 'No bids exist in this status filter.'
          }
        />
      ) : (
        <div className="bg-white rounded-xl border border-slate-200 shadow-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-5">Bid ID</th>
                  <th className="py-3.5 px-5">Bidder Organization</th>
                  <th className="py-3.5 px-5">Tender Reference</th>
                  <th className="py-3.5 px-5">Quoted Amount</th>
                  <th className="py-3.5 px-5">Documents</th>
                  <th className="py-3.5 px-5">Submission Date</th>
                  <th className="py-3.5 px-5">Status</th>
                  <th className="py-3.5 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {paginatedBids.map((bid) => (
                  <tr key={bid.id} className="hover:bg-slate-50/70 transition-colors">
                    {/* Bid ID */}
                    <td className="py-4 px-5 font-mono font-medium text-slate-900 text-xs">
                      {bid.id.slice(0, 8)}...
                    </td>

                    {/* Bidder Organization */}
                    <td className="py-4 px-5">
                      <span className="font-semibold text-slate-900 block">
                        {bid.bidderName || 'Bidder Organization'}
                      </span>
                      <span className="text-[11px] text-slate-400 font-mono">
                        Profile: {bid.bidderProfileId.slice(0, 8)}...
                      </span>
                    </td>

                    {/* Tender */}
                    <td className="py-4 px-5">
                      <span className="font-medium text-slate-800 block truncate max-w-xs">
                        {bid.tenderTitle || 'Government Procurement'}
                      </span>
                      <span className="text-[11px] text-slate-400 font-mono">
                        {bid.tenderNumber || ''}
                      </span>
                    </td>

                    {/* Quoted Amount */}
                    <td className="py-4 px-5 font-mono font-semibold text-slate-900 whitespace-nowrap">
                      ₹{Number(bid.quotedAmount).toLocaleString('en-IN')}
                    </td>

                    {/* Documents Uploaded */}
                    <td className="py-4 px-5 text-slate-600 whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <FileText className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        <span className="font-medium">{bid.documentsCount ?? 0} files</span>
                      </div>
                    </td>

                    {/* Submission Date */}
                    <td className="py-4 px-5 text-slate-500 whitespace-nowrap text-[11px]">
                      {new Date(bid.submittedAt).toLocaleDateString()}
                    </td>

                    {/* Status Badge */}
                    <td className="py-4 px-5 whitespace-nowrap">
                      <StatusBadge status={bid.status} />
                    </td>

                    {/* Actions */}
                    <td className="py-4 px-5 text-right whitespace-nowrap">
                      <div className="inline-flex items-center gap-2">
                        {user?.role !== 'BIDDER' && <button
                          onClick={() => navigate(`/evaluations/${bid.id}`)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-2xs transition-colors cursor-pointer"
                        >
                          <Sparkles className="w-3.5 h-3.5 text-blue-300" />
                          Evaluate Bid
                        </button>}
                        <button
                          onClick={() => navigate(`/bids/${bid.id}`)}
                          className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-100 rounded-lg shadow-2xs transition-colors cursor-pointer"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          Details
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="px-5 py-3.5 border-t border-slate-200 bg-white flex items-center justify-between text-xs text-slate-500">
            <button
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 transition-colors disabled:opacity-40 cursor-pointer"
            >
              <ChevronLeft className="w-4 h-4" />
              Previous
            </button>
            <span className="font-medium text-slate-700">
              Page {currentPage} of {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-slate-200 text-slate-700 hover:bg-slate-50 transition-colors disabled:opacity-40 cursor-pointer"
            >
              Next
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
