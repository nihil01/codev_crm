import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Students } from './pages/Students';
import { Teachers } from './pages/Teachers';
import { Groups } from './pages/Groups';
import { Payments } from './pages/Payments';
import { Expenses } from './pages/Expenses';
import { Reports } from './pages/Reports';
import { Certificates } from './pages/Certificates';
import { Forecast } from './pages/Forecast';
import { Role } from './types';
import { Managers } from './pages/Managers';
import { Leads } from './pages/Leads';
import { Audit } from './pages/Audit';
import { Reminders } from './pages/Reminders';
import { ScenarioForecast } from './pages/ScenarioForecast';
import { Analytics } from './pages/Analytics';
import { StudentFreezes } from './pages/StudentFreezes';
import { RecurringRules } from './pages/RecurringRules';
import { TeacherPortal } from './pages/TeacherPortal';
import { TeacherAccounts } from './pages/TeacherAccounts';
import { ReminderDispatches } from './pages/ReminderDispatches';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const ProtectedRoute: React.FC<{
  children: React.ReactNode; adminOnly?: boolean; teacherOnly?: boolean
}> = ({ children, adminOnly, teacherOnly }) => {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (teacherOnly && user?.role !== Role.TEACHER)
    return <Navigate to={user?.role === Role.ADMIN ? "/" : "/students"} replace />;
  if (!teacherOnly && user?.role === Role.TEACHER)
    return <Navigate to="/teacher-portal" replace />;
  if (adminOnly && user?.role !== Role.ADMIN) return <Navigate to="/students" replace />;
  return <Layout>{children}</Layout>;
};

const AppRoutes: React.FC = () => {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/" replace /> : <Login />}
      />
      <Route path="/" element={<ProtectedRoute adminOnly><Dashboard /></ProtectedRoute>} />
      <Route path="/students" element={<ProtectedRoute><Students /></ProtectedRoute>} />
      <Route path="/teachers" element={<ProtectedRoute><Teachers /></ProtectedRoute>} />
      <Route path="/groups" element={<ProtectedRoute><Groups /></ProtectedRoute>} />
      <Route path="/payments" element={<ProtectedRoute><Payments /></ProtectedRoute>} />
      <Route path="/expenses" element={<ProtectedRoute adminOnly><Expenses /></ProtectedRoute>} />
      <Route path="/expense-rules" element={<ProtectedRoute adminOnly><RecurringRules /></ProtectedRoute>} />
      <Route path="/teacher-accounts" element={<ProtectedRoute adminOnly><TeacherAccounts /></ProtectedRoute>} />
      <Route path="/reminder-dispatches" element={<ProtectedRoute adminOnly><ReminderDispatches /></ProtectedRoute>} />
      <Route path="/teacher-portal" element={<ProtectedRoute teacherOnly><TeacherPortal /></ProtectedRoute>} />
      <Route path="/reports" element={<ProtectedRoute adminOnly><Reports /></ProtectedRoute>} />
      <Route path="/forecast" element={<ProtectedRoute adminOnly><Forecast /></ProtectedRoute>} />
      <Route path="/forecast/scenarios" element={<ProtectedRoute adminOnly><ScenarioForecast /></ProtectedRoute>} />
      <Route path="/analytics" element={<ProtectedRoute adminOnly><Analytics /></ProtectedRoute>} />
      <Route path="/certificates" element={<ProtectedRoute adminOnly><Certificates /></ProtectedRoute>} />
      <Route path="/managers" element={<ProtectedRoute adminOnly><Managers /></ProtectedRoute>} />
      <Route path="/audit" element={<ProtectedRoute adminOnly><Audit /></ProtectedRoute>} />
      <Route path="/leads" element={<ProtectedRoute><Leads /></ProtectedRoute>} />
      <Route path="/reminders" element={<ProtectedRoute><Reminders /></ProtectedRoute>} />
      <Route path="/freezes" element={<ProtectedRoute><StudentFreezes /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <AuthProvider>
          <BrowserRouter>
            <AppRoutes />
            <Toaster
              position="top-right"
              toastOptions={{
                style: {
                  background: '#1e293b',
                  color: '#f1f5f9',
                  border: '1px solid #334155',
                },
              }}
            />
          </BrowserRouter>
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
};

export default App;
