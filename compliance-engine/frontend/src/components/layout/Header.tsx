import React, { useState, useEffect } from 'react';
import { Menu, Server, ShieldAlert, WifiOff } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';

interface HeaderProps {
  onToggleSidebar: () => void;
}

export const Header: React.FC<HeaderProps> = ({ onToggleSidebar }) => {
  const { user } = useAuth();
  const [isBackendOnline, setIsBackendOnline] = useState<boolean | null>(null);

  // REAL LIVE ping to Spring Boot backend
  useEffect(() => {
    let isMounted = true;
    const checkBackend = async () => {
      try {
        await apiClient.get('/tenders', { timeout: 3000 });
        if (isMounted) setIsBackendOnline(true);
      } catch {
        if (isMounted) setIsBackendOnline(false);
      }
    };

    checkBackend();
    const interval = setInterval(checkBackend, 8000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-4 sm:px-6 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 md:hidden cursor-pointer"
          aria-label="Toggle Navigation"
        >
          <Menu className="w-5 h-5" />
        </button>

        {/* Portal & Scope indicator */}
        <div className="hidden sm:flex items-center gap-2">
          <span className="text-xs font-semibold text-slate-800">
            National Public Procurement & Compliance Portal
          </span>
          <span className="text-slate-300">•</span>
          <span className="text-xs text-slate-500">
            FY 2026–27
          </span>
        </div>
      </div>

      {/* Right status & disclaimers */}
      <div className="flex items-center gap-3">
        {/* Real Live Backend Health Check Status */}
        {isBackendOnline === true ? (
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-[11px] font-medium text-emerald-800">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <Server className="w-3 h-3 text-emerald-600" />
            <span className="hidden md:inline">Backend API:</span>
            <span className="font-semibold text-emerald-900">Online (:8080)</span>
          </div>
        ) : isBackendOnline === false ? (
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-rose-50 border border-rose-200 text-[11px] font-medium text-rose-800">
            <WifiOff className="w-3 h-3 text-rose-600" />
            <span className="hidden md:inline">Backend API:</span>
            <span className="font-semibold text-rose-900">Offline / Disconnected</span>
          </div>
        ) : (
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-slate-50 border border-slate-200 text-[11px] font-medium text-slate-500">
            <Server className="w-3 h-3 text-slate-400" />
            <span>Checking API...</span>
          </div>
        )}

        {/* AI Disclaimer badge */}
        <div className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-blue-50/70 border border-blue-100 text-[11px] font-medium text-blue-800">
          <ShieldAlert className="w-3.5 h-3.5 text-blue-600 shrink-0" />
          <span>Decision Support • Human Sign-Off</span>
        </div>

        {/* Active role badge */}
        <div className="px-2.5 py-1 rounded-lg bg-slate-100 border border-slate-200 text-xs font-semibold text-slate-700">
          {user?.role?.replace(/_/g, ' ') || 'OFFICER'}
        </div>
      </div>
    </header>
  );
};
