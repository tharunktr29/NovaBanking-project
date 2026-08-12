import { Navigate, createBrowserRouter } from 'react-router-dom';
import { useAppSelector } from './hooks';
import { DashboardPage } from './pages/DashboardPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { LoginPage } from './pages/LoginPage';
import { MfaPage } from './pages/MfaPage';
import { LogoutPage } from './pages/LogoutPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { RegisterPage } from './pages/RegisterPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';

function ProtectedRoute({ children }: { children: JSX.Element }) {
  const isAuthenticated = useAppSelector((state) => state.auth.status === 'authenticated');
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/dashboard" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  { path: '/mfa', element: <MfaPage /> },
  {
    path: '/dashboard',
    element: (
      <ProtectedRoute>
        <DashboardPage />
      </ProtectedRoute>
    )
  },
  { path: '/logout', element: <LogoutPage /> },
  { path: '*', element: <NotFoundPage /> }
]);
