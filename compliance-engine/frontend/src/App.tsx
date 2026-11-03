import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { UserRole } from './types';
import { AppLayout } from './components/layout/AppLayout';

import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { TendersPage } from './pages/TendersPage';
import { CreateTenderPage } from './pages/CreateTenderPage';
import { TenderDetailsPage } from './pages/TenderDetailsPage';
import { BidsPage } from './pages/BidsPage';
import { BidDetailsPage } from './pages/BidDetailsPage';
import { EvaluationPage } from './pages/EvaluationPage';
import { ReportsPage } from './pages/ReportsPage';
import { OrganizationsPage } from './pages/OrganizationsPage';
import { UserManagementPage } from './pages/UserManagementPage';
import { ProfilePage } from './pages/ProfilePage';
import { BidderOnboardingPage } from './pages/BidderOnboardingPage';
import { BidderRecommendedTendersPage } from './pages/BidderRecommendedTendersPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 1000 * 60 * 3, // 3 minutes
    },
  },
});

const ProtectedRoute: React.FC<{ children: React.ReactNode; roles?: UserRole[] }> = ({ children, roles }) => {
  const { isAuthenticated, isLoading, user } = useAuth();
  if (isLoading) return <div className="min-h-screen grid place-items-center text-sm text-slate-500">Restoring secure session…</div>;
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (roles && (!user || !roles.includes(user.role))) return <div className="min-h-screen grid place-items-center"><div className="text-center"><h1 className="text-lg font-semibold">Access denied</h1><p className="mt-2 text-sm text-slate-500">Your account does not have permission to view this page.</p><a className="mt-4 inline-block text-navy-900 underline" href="/dashboard">Return to dashboard</a></div></div>;
  return <>{children}</>;
};

const DashboardRedirect: React.FC = () => {
  const { user } = useAuth();
  const path = user?.role === 'SYSTEM_ADMIN' ? 'admin' : user?.role === 'BIDDER' ? 'bidder' : user?.role === 'EVALUATION_OFFICER' ? 'evaluation' : 'procurement';
  return <Navigate to={`/dashboard/${path}`} replace />;
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />

            <Route
              path="/"
              element={
                  <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<DashboardRedirect />} />
              <Route path="dashboard/admin" element={<ProtectedRoute roles={['SYSTEM_ADMIN']}><DashboardPage /></ProtectedRoute>} />
              <Route path="dashboard/procurement" element={<ProtectedRoute roles={['PROCUREMENT_OFFICER']}><DashboardPage /></ProtectedRoute>} />
              <Route path="dashboard/bidder" element={<ProtectedRoute roles={['BIDDER']}><DashboardPage /></ProtectedRoute>} />
              <Route path="dashboard/evaluation" element={<ProtectedRoute roles={['EVALUATION_OFFICER']}><DashboardPage /></ProtectedRoute>} />
              <Route path="tenders" element={<TendersPage />} />
              <Route path="tenders/create" element={<ProtectedRoute roles={['SYSTEM_ADMIN','PROCUREMENT_OFFICER']}><CreateTenderPage /></ProtectedRoute>} />
              <Route path="tenders/:id" element={<TenderDetailsPage />} />
              <Route path="bids" element={<ProtectedRoute roles={['SYSTEM_ADMIN','PROCUREMENT_OFFICER','BIDDER']}><BidsPage /></ProtectedRoute>} />
              <Route path="bidder/onboarding" element={<ProtectedRoute roles={['BIDDER']}><BidderOnboardingPage /></ProtectedRoute>} />
              <Route path="bidder/recommended" element={<ProtectedRoute roles={['BIDDER']}><BidderRecommendedTendersPage /></ProtectedRoute>} />
              <Route path="bids/:id" element={<BidDetailsPage />} />
              <Route path="evaluations" element={<ProtectedRoute roles={['SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER']}><EvaluationPage /></ProtectedRoute>} />
              <Route path="evaluations/:bidId" element={<ProtectedRoute roles={['SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER']}><EvaluationPage /></ProtectedRoute>} />
              <Route path="reports" element={<ProtectedRoute roles={['SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER']}><ReportsPage /></ProtectedRoute>} />
              <Route path="reports/:bidId" element={<ProtectedRoute roles={['SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER']}><ReportsPage /></ProtectedRoute>} />
              <Route path="organizations" element={<ProtectedRoute roles={['SYSTEM_ADMIN','PROCUREMENT_OFFICER']}><OrganizationsPage /></ProtectedRoute>} />
              <Route path="users" element={<ProtectedRoute roles={['SYSTEM_ADMIN']}><UserManagementPage /></ProtectedRoute>} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
