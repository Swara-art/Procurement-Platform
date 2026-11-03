import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Plus, Eye, Building2, Calendar, ChevronLeft, ChevronRight, FileSpreadsheet } from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { StatusBadge } from '../components/common/StatusBadge';
import { SearchAndFilters } from '../components/common/SearchAndFilters';
import { LoadingState } from '../components/common/LoadingState';
import { EmptyState } from '../components/common/EmptyState';
import { tendersApi } from '../api/tendersApi';
import { useAuth } from '../context/AuthContext';

export const TendersPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 5;

  const { data: tenders = [], isLoading, error } = useQuery({
    queryKey: ['tenders', statusFilter],
    queryFn: () => tendersApi.getAll(statusFilter),
  });

  const isOfficerOrAdmin = user?.role === 'PROCUREMENT_OFFICER' || user?.role === 'SYSTEM_ADMIN';

  // Filtered & searched list
  const filteredTenders = useMemo(() => {
    return tenders.filter((t) => {
      const matchesSearch =
        t.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        t.tenderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (t.authorityName && t.authorityName.toLowerCase().includes(searchQuery.toLowerCase()));

      const matchesStatus =
        statusFilter === 'ALL' || t.status.toUpperCase() === statusFilter.toUpperCase();

      return matchesSearch && matchesStatus;
    });
  }, [tenders, searchQuery, statusFilter]);

  const totalPages = Math.ceil(filteredTenders.length / pageSize) || 1;
  const paginatedTenders = filteredTenders.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const filterOptions = [
    { label: 'All Tenders', value: 'ALL', count: tenders.length },
    { label: 'Published', value: 'PUBLISHED', count: tenders.filter((t) => t.status === 'PUBLISHED').length },
    { label: 'Draft', value: 'DRAFT', count: tenders.filter((t) => t.status === 'DRAFT').length },
    { label: 'Closed', value: 'CLOSED', count: tenders.filter((t) => t.status === 'CLOSED').length },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Tender Management"
        subtitle="Gazetted procurement tenders, statutory eligibility criteria, and submission deadlines."
        actions={
          isOfficerOrAdmin ? (
            <button
              onClick={() => navigate('/tenders/create')}
              className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              Create New Tender
            </button>
          ) : null
        }
      />

      {/* Filter and Search Bar (Untitled UI Style) */}
      <SearchAndFilters
        searchPlaceholder="Search by Tender ID, title, or department..."
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

      {/* Content Table */}
      {isLoading ? (
        <LoadingState message="Fetching tenders from government repository..." />
      ) : error ? (
        <EmptyState
          icon={FileSpreadsheet}
          title="Could not load tenders"
          description="Failed to communicate with the tender registry service."
        />
      ) : filteredTenders.length === 0 ? (
        <EmptyState
          icon={FileSpreadsheet}
          title="No tenders found"
          description={
            searchQuery
              ? `No procurement notices matched "${searchQuery}". Try revising your search keywords.`
              : 'No tenders exist in this status category.'
          }
          action={
            isOfficerOrAdmin
              ? {
                  label: 'Create First Tender',
                  onClick: () => navigate('/tenders/create'),
                }
              : undefined
          }
        />
      ) : (
        <div className="bg-white rounded-xl border border-slate-200 shadow-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-5">Tender Ref Number</th>
                  <th className="py-3.5 px-5">Tender Title</th>
                  <th className="py-3.5 px-5">Procuring Authority</th>
                  <th className="py-3.5 px-5">Estimated Value</th>
                  <th className="py-3.5 px-5">Submission Deadline</th>
                  <th className="py-3.5 px-5">Status</th>
                  <th className="py-3.5 px-5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs">
                {paginatedTenders.map((tender) => (
                  <tr key={tender.id} className="hover:bg-slate-50/70 transition-colors">
                    {/* Tender Number */}
                    <td className="py-4 px-5 font-mono font-medium text-slate-900 text-xs">
                      {tender.tenderNumber}
                    </td>

                    {/* Title */}
                    <td className="py-4 px-5">
                      <div className="max-w-md">
                        <span className="font-semibold text-slate-900 block truncate">
                          {tender.title}
                        </span>
                        <span className="text-[11px] text-slate-400 line-clamp-1 mt-0.5">
                          {tender.description}
                        </span>
                      </div>
                    </td>

                    {/* Authority */}
                    <td className="py-4 px-5 text-slate-600">
                      <div className="flex items-center gap-1.5">
                        <Building2 className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                        <span className="truncate max-w-[160px]">
                          {tender.authorityName || 'State Health Authority'}
                        </span>
                      </div>
                    </td>

                    {/* Estimated Value */}
                    <td className="py-4 px-5 font-mono font-medium text-slate-900 whitespace-nowrap">
                      ₹{Number(tender.estimatedValue).toLocaleString('en-IN')}
                    </td>

                    {/* Deadline */}
                    <td className="py-4 px-5 text-slate-500 whitespace-nowrap">
                      <div className="flex items-center gap-1 text-[11px]">
                        <Calendar className="w-3 h-3 text-slate-400" />
                        {new Date(tender.submissionDeadline).toLocaleDateString('en-IN', {
                          day: '2-digit',
                          month: 'short',
                          year: 'numeric',
                        })}
                      </div>
                    </td>

                    {/* Status Badge */}
                    <td className="py-4 px-5 whitespace-nowrap">
                      <StatusBadge status={tender.status} />
                    </td>

                    {/* Actions */}
                    <td className="py-4 px-5 text-right whitespace-nowrap">
                      <button
                        onClick={() => navigate(`/tenders/${tender.id}`)}
                        className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-200 rounded-lg hover:bg-slate-100 hover:text-navy-900 shadow-2xs transition-all cursor-pointer"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Table Pagination (Matching Untitled UI screenshot) */}
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
