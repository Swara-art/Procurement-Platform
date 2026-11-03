import React from 'react';
import { User, Shield, Building2, Key, CheckCircle2, ShieldCheck, Mail } from 'lucide-react';
import { PageHeader } from '../components/common/PageHeader';
import { useAuth } from '../context/AuthContext';

export const ProfilePage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <PageHeader
        title="Official Identity & Credentials"
        subtitle="Government official directory, digital signature accreditation, and authorization scope."
      />

      <div className="bg-white rounded-xl border border-slate-200 shadow-card p-6 sm:p-8 space-y-6 text-xs">
        {/* User Card */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 pb-6 border-b border-slate-100">
          <div className="w-16 h-16 rounded-2xl bg-navy-900 text-white flex items-center justify-center font-bold text-xl shadow-md shrink-0">
            {user?.name?.slice(0, 2).toUpperCase() || 'GO'}
          </div>
          <div className="space-y-1">
            <h2 className="text-lg font-bold text-slate-900">{user?.name}</h2>
            <p className="text-slate-600 font-medium">{user?.designation}</p>
            <div className="flex flex-wrap items-center gap-2 pt-1">
              <span className="px-2.5 py-0.5 rounded-full bg-slate-100 border border-slate-200 text-slate-700 font-semibold text-[11px]">
                {user?.role?.replace(/_/g, ' ')}
              </span>
              <span className="inline-flex items-center gap-1 text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-0.5 rounded-full font-semibold text-[11px]">
                <ShieldCheck className="w-3.5 h-3.5" />
                Class-3 Digital Signature Active
              </span>
            </div>
          </div>
        </div>

        {/* Credentials Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Official Email
            </span>
            <div className="flex items-center gap-2 font-mono text-slate-800 font-semibold">
              <Mail className="w-3.5 h-3.5 text-slate-400" />
              <span>{user?.email}</span>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Department / Ministry
            </span>
            <div className="flex items-center gap-2 text-slate-800 font-semibold">
              <Building2 className="w-3.5 h-3.5 text-slate-400" />
              <span>{user?.department}</span>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Statutory Jurisdiction
            </span>
            <div className="flex items-center gap-2 text-slate-800 font-semibold">
              <Shield className="w-3.5 h-3.5 text-slate-400" />
              <span>State Procurement & IT Infrastructure</span>
            </div>
          </div>

          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-1">
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
              Session Encryption
            </span>
            <div className="flex items-center gap-2 font-mono text-slate-800 font-semibold">
              <Key className="w-3.5 h-3.5 text-slate-400" />
              <span>TLS 1.3 • AES-256-GCM Session Token</span>
            </div>
          </div>
        </div>

        {/* Legal Authority Note */}
        <div className="p-4 rounded-xl bg-blue-50/60 border border-blue-100 text-blue-900 space-y-1 leading-relaxed">
          <div className="flex items-center gap-2 font-bold text-blue-950">
            <CheckCircle2 className="w-4 h-4 text-blue-600" />
            <span>Authorized Signatory Delegation</span>
          </div>
          <p className="text-[11px] text-blue-800">
            As a designated procurement official, your actions on bid approvals, requirement overrides, and disqualifications are authenticated and preserved for vigilance and CAG auditing.
          </p>
        </div>
      </div>
    </div>
  );
};
