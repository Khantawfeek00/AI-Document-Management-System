import React from 'react';
import { useAuth } from '../context/AuthContext';
import { UserRole } from '../types';
import { formatRole } from '../components/Navigation';

export const DashboardPage: React.FC = () => {
  const { user, roles, hasRole } = useAuth();

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Welcome, {user?.name || user?.username}!</h1>
        <p style={styles.subtitle}>
          You are logged in as: <strong>{roles.map(formatRole).join(', ')}</strong>
        </p>
      </div>

      <div style={styles.grid}>
        <div style={styles.card}>
          <h2>📂 My Files</h2>
          <p>View and manage your personal documents</p>
          <a href="/files" style={styles.cardLink}>
            Go to Files →
          </a>
        </div>

        <div style={styles.card}>
          <h2>⬆️ Upload</h2>
          <p>Upload new documents to the system</p>
          <a href="/upload" style={styles.cardLink}>
            Upload Files →
          </a>
        </div>

        {hasRole(UserRole.MANAGER) && (
          <div style={{ ...styles.card, ...styles.managerCard }}>
            <h2>👥 Team Files</h2>
            <p>Access departmental and shared documents</p>
            <a href="/team-files" style={styles.cardLink}>
              View Team Files →
            </a>
          </div>
        )}

        {hasRole(UserRole.ADMIN) && (
          <div style={{ ...styles.card, ...styles.adminCard }}>
            <h2>⚙️ Admin Panel</h2>
            <p>System administration and user management</p>
            <a href="/admin" style={styles.cardLink}>
              Open Admin Panel →
            </a>
          </div>
        )}
      </div>

      <div style={styles.infoSection}>
        <h3>Your Permissions</h3>
        <ul style={styles.permissionList}>
          <li>✓ Upload and manage personal files</li>
          <li>✓ View shared documents</li>
          {hasRole(UserRole.MANAGER) && (
            <>
              <li>✓ Access team and departmental files</li>
              <li>✓ Share documents with team members</li>
            </>
          )}
          {hasRole(UserRole.ADMIN) && (
            <>
              <li>✓ Full system access</li>
              <li>✓ User management capabilities</li>
              <li>✓ View all documents</li>
            </>
          )}
        </ul>
      </div>

      <div style={styles.systemInfo}>
        <h4>System Information</h4>
        <div style={styles.infoGrid}>
          <div>
            <strong>User ID:</strong> {user?.sub}
          </div>
          <div>
            <strong>Email:</strong> {user?.email}
          </div>
          <div>
            <strong>Authentication:</strong> OAuth2/OIDC via Keycloak
          </div>
          <div>
            <strong>Session:</strong> Server-side (API Gateway)
          </div>
        </div>
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    maxWidth: '1200px',
    margin: '0 auto',
  },
  header: {
    marginBottom: '2rem',
    paddingBottom: '1.5rem',
    borderBottom: '2px solid #e0e0e0',
  },
  title: {
    fontSize: '2.5rem',
    color: '#333',
    marginBottom: '0.5rem',
  },
  subtitle: {
    fontSize: '1.1rem',
    color: '#666',
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
    gap: '1.5rem',
    marginBottom: '3rem',
  },
  card: {
    backgroundColor: 'white',
    padding: '2rem',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
    border: '1px solid #e0e0e0',
    transition: 'transform 0.2s',
  },
  managerCard: {
    borderLeft: '4px solid #ff9800',
  },
  adminCard: {
    borderLeft: '4px solid #f44336',
  },
  cardLink: {
    display: 'inline-block',
    marginTop: '1rem',
    color: '#1976d2',
    textDecoration: 'none',
    fontWeight: '500',
  },
  infoSection: {
    backgroundColor: '#f9f9f9',
    padding: '2rem',
    borderRadius: '8px',
    marginBottom: '2rem',
  },
  permissionList: {
    listStyle: 'none',
    padding: 0,
    fontSize: '1rem',
    lineHeight: '2',
  },
  systemInfo: {
    backgroundColor: '#e3f2fd',
    padding: '1.5rem',
    borderRadius: '8px',
    fontSize: '0.95rem',
  },
  infoGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
    gap: '1rem',
    marginTop: '1rem',
  },
};
