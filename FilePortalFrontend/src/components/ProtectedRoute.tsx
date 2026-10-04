import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: string[];
  redirectTo?: string;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRoles,
  redirectTo = '/',
}) => {
  const { isAuthenticated, isLoading, hasAnyRole } = useAuth();

  if (isLoading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        fontSize: '1.2rem',
        color: '#666'
      }}>
        Loading...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to={redirectTo} replace />;
  }

  if (requiredRoles && requiredRoles.length > 0) {
    if (!hasAnyRole(requiredRoles)) {
      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          alignItems: 'center',
          height: '100vh',
          padding: '2rem',
          textAlign: 'center'
        }}>
          <h1 style={{ color: '#d32f2f', marginBottom: '1rem' }}>Access Denied</h1>
          <p style={{ fontSize: '1.1rem', color: '#666', marginBottom: '2rem' }}>
            You do not have the required permissions to access this page.
          </p>
          <p style={{ color: '#999' }}>
            Required roles: {requiredRoles.join(', ')}
          </p>
          <button
            onClick={() => window.history.back()}
            style={{
              marginTop: '2rem',
              padding: '0.75rem 2rem',
              fontSize: '1rem',
              backgroundColor: '#1976d2',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            Go Back
          </button>
        </div>
      );
    }
  }

  return <>{children}</>;
};
