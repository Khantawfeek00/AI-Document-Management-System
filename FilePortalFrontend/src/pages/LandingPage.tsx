import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export const LandingPage: React.FC = () => {
  const { isAuthenticated, login, isLoading } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      navigate('/dashboard');
    }
  }, [isAuthenticated, isLoading, navigate]);

  if (isLoading) {
    return (
      <div style={styles.loadingContainer}>
        <p>Loading...</p>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.hero}>
        <h1 style={styles.title}>📁 File Portal</h1>
        <p style={styles.subtitle}>
          Enterprise Document Management System
        </p>


        {!isAuthenticated && (
          <>
            <p style={styles.description}>
              Secure, role-based file management powered by OAuth2/OIDC authentication
            </p>

            <button onClick={login} style={styles.loginButton}>
                🔑 Sign In with Keycloak
            </button>

            <div style={styles.features}>
              <div style={styles.feature}>
                <h3>🔒 Secure Authentication</h3>
                <p>OAuth2 Authorization Code Flow via API Gateway</p>
              </div>
              <div style={styles.feature}>
                <h3>👥 Role-Based Access</h3>
                <p>Admin, Manager, and Staff roles with distinct permissions</p>
              </div>
              <div style={styles.feature}>
                <h3>📂 Document Management</h3>
                <p>Upload, share, and manage files with version control</p>
              </div>
            </div>

            <div style={styles.architecture}>
              <h3> 🧩 Architecture</h3>
              <ul style={styles.archList}>
                <li>Gateway-centric microservices architecture</li>
                <li>Token management handled server-side only</li>
                <li>Session-based frontend authentication</li>
                <li>RP-initiated logout with Keycloak</li>
              </ul>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '100vh',
    backgroundColor: '#81a0ce',
    padding: '2rem',
  },
  loadingContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh',
  },
  hero: {
    minWidth: '1000px',
    maxWidth: '2000px',
    textAlign: 'center',
    backgroundColor: 'white',
    padding: '3rem',
    borderRadius: '12px',
    boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
  },
  title: {
    fontSize: '3rem',
    color: '#1976d2',
    marginBottom: '1rem',
  },
  subtitle: {
    fontSize: '1.5rem',
    color: '#666',
    marginBottom: '2rem',
  },
  description: {
    fontSize: '1.1rem',
    color: '#555',
    marginBottom: '2rem',
    lineHeight: '1.6',
  },
  loginButton: {
    padding: '1rem 2rem',
    fontSize: '1.2rem',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: '10px',
    cursor: 'pointer',
    fontWeight: 'bold',
    transition: 'background-color 0.2s',
    marginBottom: '1rem',
  },
  features: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
    gap: '2rem',
    marginTop: '3rem',
    marginBottom: '3rem',
  },
  feature: {
    padding: '1.5rem',
    backgroundColor: '#f9f9f9',
    borderRadius: '8px',
  },
  architecture: {
    marginTop: '3rem',
    padding: '2rem',
    backgroundColor: '#f0f4f8',
    borderRadius: '8px',
    textAlign: 'left',
  },
  archList: {
    listStyle: 'disc',
    marginLeft: '1.5rem',
    padding: 0,
    fontSize: '0.95rem',
    lineHeight: '2',
  },
};
