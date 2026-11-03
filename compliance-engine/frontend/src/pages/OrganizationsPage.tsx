import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Building2, Plus, CheckCircle2, ShieldCheck, X } from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { LoadingState } from '../components/common/LoadingState';
import { organizationsApi } from '../api/organizationsApi';
import { CreateOrganizationRequest, OrganizationType } from '../types';

export const OrganizationsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [showAddModal, setShowAddModal] = useState(false);
  const [name, setName] = useState('');
  const [organizationType, setOrganizationType] = useState<OrganizationType>('BIDDER');
  const [registrationNumber, setRegistrationNumber] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const { data: organizations = [], isLoading } = useQuery({
    queryKey: ['organizations'],
    queryFn: () => organizationsApi.getAll(),
  });

  const createMutation = useMutation({
    mutationFn: (payload: CreateOrganizationRequest) => organizationsApi.create(payload),
    onSuccess: (newOrg) => {
      queryClient.invalidateQueries({ queryKey: ['organizations'] });
      setSuccessMsg(`Organization "${newOrg.name}" registered successfully.`);
      setName('');
      setRegistrationNumber('');
      setShowAddModal(false);
      setTimeout(() => setSuccessMsg(''), 4000);
    },
  });

  const profileMutation = useMutation({
    mutationFn: (orgId: string) => organizationsApi.createBidderProfile(orgId),
    onSuccess: () => {
      setSuccessMsg('Bidder profile generated and linked successfully.');
      setTimeout(() => setSuccessMsg(''), 4000);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !registrationNumber) return;
    createMutation.mutate({
      name: name.trim(),
      organizationType,
      registrationNumber: registrationNumber.trim(),
    });
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Organizations & Entities"
        subtitle="Registered government procurement departments and verified commercial bidder entities."
        actions={
          <button
            onClick={() => setShowAddModal(true)}
            className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm transition-colors cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Register Organization
          </button>
        }
      />

      {successMsg && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-xs flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
          <span>{successMsg}</span>
        </div>
      )}

      {isLoading ? (
        <LoadingState message="Fetching registered organizations..." />
      ) : (
        <div className="bg-white rounded-xl border border-slate-200 shadow-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50/70 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
                  <th className="py-3.5 px-5">Organization Name</th>
                  <th className="py-3.5 px-5">Organization Type</th>
                  <th className="py-3.5 px-5">Registration / CIN</th>
                  <th className="py-3.5 px-5">Registered Date</th>
                  <th className="py-3.5 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {organizations.map((org) => {
                  const isAuthority = org.organizationType === 'PROCUREMENT_AUTHORITY';
                  return (
                    <tr key={org.id} className="hover:bg-slate-50/60 transition-colors">
                      <td className="py-4 px-5">
                        <div className="flex items-center gap-2.5">
                          <div
                            className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 ${
                              isAuthority
                                ? 'bg-navy-50 text-navy-900'
                                : 'bg-blue-50 text-blue-600'
                            }`}
                          >
                            <Building2 className="w-3.5 h-3.5" />
                          </div>
                          <div>
                            <span className="font-semibold text-slate-900 block">{org.name}</span>
                            <span className="text-[10px] text-slate-400 font-mono">
                              ID: {org.id.slice(0, 8)}...
                            </span>
                          </div>
                        </div>
                      </td>

                      <td className="py-4 px-5">
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-medium border ${
                            isAuthority
                              ? 'bg-purple-50 text-purple-700 border-purple-200'
                              : 'bg-blue-50 text-blue-700 border-blue-200'
                          }`}
                        >
                          <ShieldCheck className="w-3 h-3" />
                          {isAuthority ? 'Procurement Authority' : 'Commercial Bidder'}
                        </span>
                      </td>

                      <td className="py-4 px-5 font-mono text-slate-700">
                        {org.registrationNumber}
                      </td>

                      <td className="py-4 px-5 text-slate-500 text-[11px]">
                        {new Date(org.createdAt).toLocaleDateString()}
                      </td>

                      <td className="py-4 px-5 text-right">
                        {!isAuthority && (
                          <button
                            onClick={() => profileMutation.mutate(org.id)}
                            className="px-2.5 py-1 text-[11px] font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
                          >
                            Create Bidder Profile
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Add Organization Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="w-full max-w-md bg-white rounded-xl shadow-2xl border border-slate-200 overflow-hidden">
            <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-slate-50/70">
              <h3 className="text-sm font-bold text-slate-900">Register Organization</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="text-slate-400 hover:text-slate-600"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-slate-800 mb-1">
                  Entity Legal Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. National Informatics Directorate"
                  className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
                  required
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-800 mb-1">
                  Organization Classification <span className="text-rose-500">*</span>
                </label>
                <select
                  value={organizationType}
                  onChange={(e) => setOrganizationType(e.target.value as OrganizationType)}
                  className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900 bg-white"
                >
                  <option value="BIDDER">Commercial Bidder (Supplier)</option>
                  <option value="PROCUREMENT_AUTHORITY">Government Procurement Authority</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-800 mb-1">
                  Registration / CIN / Dept Code <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={registrationNumber}
                  onChange={(e) => setRegistrationNumber(e.target.value.toUpperCase())}
                  placeholder="e.g. GOI-DEPT-2026 or U72900MH2020PTC123456"
                  className="w-full p-2.5 border border-slate-300 rounded-lg text-slate-900 font-mono text-xs focus:ring-2 focus:ring-navy-900/10 focus:border-navy-900"
                  required
                />
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3.5 py-2 text-xs font-medium text-slate-600 hover:bg-slate-100 rounded-lg cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="px-4 py-2 text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 rounded-lg shadow-sm cursor-pointer disabled:opacity-50"
                >
                  {createMutation.isPending ? 'Registering...' : 'Register Organization'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
