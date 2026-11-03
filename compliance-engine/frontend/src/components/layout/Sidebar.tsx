import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  FileSpreadsheet,
  Layers,
  Sparkles,
  FileText,
  Building2,
  Users,
  LogOut,
  Shield,
  User as UserIcon,
  UploadCloud,
  Target,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const allNavItems = [
    { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { label: 'Tenders', path: '/tenders', icon: FileSpreadsheet },
    { label: 'Bids', path: '/bids', icon: Layers },
    { label: 'Evaluations', path: '/evaluations', icon: Sparkles },
    { label: 'Reports', path: '/reports', icon: FileText },
  ];
  const mainNavItems = allNavItems.filter((item) => {
    if (user?.role === 'BIDDER') return ['Dashboard','Tenders','Bids'].includes(item.label);
    if (user?.role === 'EVALUATION_OFFICER') return ['Dashboard','Tenders','Bids','Evaluations','Reports'].includes(item.label);
    return true;
  });
  if(user?.role==='BIDDER') mainNavItems.splice(2,0,{label:'Recommended',path:'/bidder/recommended',icon:Target},{label:'Onboarding & Documents',path:'/bidder/onboarding',icon:UploadCloud});

  const adminNavItems = [
    { label: 'Organizations', path: '/organizations', icon: Building2 },
    { label: 'User Management', path: '/users', icon: Users },
  ];

  const isOfficerOrAdmin = user?.role === 'PROCUREMENT_OFFICER' || user?.role === 'SYSTEM_ADMIN';

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          onClick={onClose}
          className="fixed inset-0 z-40 bg-slate-900/30 backdrop-blur-xs md:hidden"
        />
      )}

      <aside
        className={`fixed md:sticky top-0 left-0 z-50 h-screen w-64 bg-white border-r border-slate-200 flex flex-col justify-between transition-transform duration-200 ease-in-out md:translate-x-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand & Portal Header */}
        <div>
          <div className="h-16 px-5 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-navy-900 flex items-center justify-center text-white shadow-xs">
                <Shield className="w-4 h-4 stroke-[2.2]" />
              </div>
              <div>
                <span className="text-sm font-bold tracking-tight text-slate-900 block leading-tight">
                  GovProcure<span className="text-blue-600">.ai</span>
                </span>
                <span className="text-[10px] text-slate-400 font-medium block">
                  Tender Compliance System
                </span>
              </div>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="px-3 py-4 space-y-6 overflow-y-auto max-h-[calc(100vh-170px)]">
            {/* Main Section */}
            <div>
              <p className="px-3 text-[11px] font-semibold tracking-wider text-slate-400 uppercase mb-1.5">
                Main
              </p>
              <nav className="space-y-0.5">
                {mainNavItems.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.path}
                      to={item.path}
                      onClick={() => onClose()}
                      className={({ isActive }) =>
                        `flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium transition-colors ${
                          isActive
                            ? 'bg-slate-100 text-navy-900 font-semibold'
                            : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                        }`
                      }
                    >
                      <Icon className="w-4 h-4 shrink-0 text-slate-500" />
                      <span>{item.label}</span>
                    </NavLink>
                  );
                })}
              </nav>
            </div>

            {/* Administration Section */}
            {user?.role === 'SYSTEM_ADMIN' && (
              <div>
                <p className="px-3 text-[11px] font-semibold tracking-wider text-slate-400 uppercase mb-1.5">
                  Administration
                </p>
                <nav className="space-y-0.5">
                  {adminNavItems.map((item) => {
                    const Icon = item.icon;
                    return (
                      <NavLink
                        key={item.path}
                        to={item.path}
                        onClick={() => onClose()}
                        className={({ isActive }) =>
                          `flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium transition-colors ${
                            isActive
                              ? 'bg-slate-100 text-navy-900 font-semibold'
                              : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                          }`
                        }
                      >
                        <Icon className="w-4 h-4 shrink-0 text-slate-500" />
                        <span>{item.label}</span>
                      </NavLink>
                    );
                  })}
                </nav>
              </div>
            )}

            {/* Account / Settings */}
            <div>
              <p className="px-3 text-[11px] font-semibold tracking-wider text-slate-400 uppercase mb-1.5">
                Account
              </p>
              <nav className="space-y-0.5">
                <NavLink
                  to="/profile"
                  onClick={() => onClose()}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium transition-colors ${
                      isActive
                        ? 'bg-slate-100 text-navy-900 font-semibold'
                        : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                    }`
                  }
                >
                  <UserIcon className="w-4 h-4 shrink-0 text-slate-500" />
                  <span>Official Profile</span>
                </NavLink>

                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium text-slate-600 hover:bg-rose-50 hover:text-rose-700 transition-colors text-left cursor-pointer"
                >
                  <LogOut className="w-4 h-4 shrink-0" />
                  <span>Sign Out</span>
                </button>
              </nav>
            </div>
          </div>
        </div>

        {/* User Card at bottom (Untitled UI style) */}
        <div className="p-3 border-t border-slate-100 bg-slate-50/60 relative">
          <div className="flex items-center p-2 rounded-lg bg-white border border-slate-200 shadow-subtle">
            <div className="flex items-center gap-2.5 overflow-hidden">
              <div className="w-8 h-8 rounded-full bg-navy-900 text-white flex items-center justify-center font-semibold text-xs shrink-0 shadow-xs">
                {user?.name?.slice(0, 2).toUpperCase() || 'GO'}
              </div>
              <div className="truncate text-left">
                <p className="text-xs font-semibold text-slate-900 truncate leading-tight">
                  {user?.name || 'Authorized Officer'}
                </p>
                <p className="text-[10px] text-slate-500 truncate">
                  {user?.role?.replace(/_/g, ' ') || 'Government Staff'}
                </p>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
};
