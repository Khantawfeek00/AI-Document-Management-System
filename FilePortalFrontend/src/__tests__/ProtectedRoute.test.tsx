import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { AuthContext } from '../context/AuthContext';

describe('ProtectedRoute', () => {
  function renderWithAuth(value: any, children: React.ReactNode) {
    return render(
      <AuthContext.Provider value={value}>
        <MemoryRouter>
          <ProtectedRoute>{children}</ProtectedRoute>
        </MemoryRouter>
      </AuthContext.Provider>
    );
  }

  it('redirects if not authenticated', () => {
    renderWithAuth({ isAuthenticated: false, isLoading: false, hasAnyRole: () => false }, <div>Private</div>);
    expect(screen.queryByText('Private')).not.toBeInTheDocument();
  });

  it('renders children if authenticated', () => {
    renderWithAuth({ isAuthenticated: true, isLoading: false, hasAnyRole: () => true }, <div>Private</div>);
    expect(screen.getByText('Private')).toBeInTheDocument();
  });

  it('shows loading if isLoading', () => {
    renderWithAuth({ isAuthenticated: false, isLoading: true, hasAnyRole: () => false }, <div>Private</div>);
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });
});

