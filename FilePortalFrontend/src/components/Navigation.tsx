import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { UserRole } from '../types';
import ConfirmDialog from './ConfirmDialog';

export const ROLE_DISPLAY_NAMES: Record<string, string> = {
    'admin': 'Admin',
    'manager': 'Manager',
    'staff': 'Staff',
    'user': 'User'
};

export const formatRole = (role: string) => {
    return ROLE_DISPLAY_NAMES[role] || role;
};

export const Navigation: React.FC = () => {
  const { isAuthenticated, user, roles, logout, hasRole } = useAuth();
  const [showLogoutModal, setShowLogoutModal] = useState(false);

  const handleLogout = () => {
    setShowLogoutModal(true);
  };

  const confirmLogout = () => {
    setShowLogoutModal(false);
    logout();
  };

  const cancelLogout = () => {
    setShowLogoutModal(false);
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <nav style={styles.nav}>
      <div style={styles.navContainer}>
        <div style={styles.navBrand}>
          <Link to="/" style={styles.brandLink}>
            📁 File Portal
          </Link>
        </div>

        <div style={styles.navLinks}>
          <Link to="/dashboard" style={styles.navLink}>
            Dashboard
          </Link>

          <Link to="/files" style={styles.navLink}>
            My Files
          </Link>

          <Link to="/upload" style={styles.navLink}>
            Upload
          </Link>

          <Link to="/search" style={styles.navLink}>
            🔍 Search
          </Link>

          {hasRole(UserRole.MANAGER) && (
            <Link to="/team-files" style={styles.navLink}>
              Team Files
            </Link>
          )}

          {hasRole(UserRole.ADMIN) && (
            <Link to="/admin" style={styles.navLink}>
              Admin Panel
            </Link>
          )}
        </div>

        <div style={styles.navUser}>
          <span style={styles.userInfo}>
            {user?.name || user?.username}
            <span style={styles.userRole}>
              ({roles.map(formatRole).join(', ')})
            </span>
          </span>
          <button onClick={handleLogout} style={styles.logoutButton}>
            Logout
          </button>
        </div>
      </div>

      <ConfirmDialog
        open={showLogoutModal}
        title="Confirm Logout"
        message="Are you sure you want to logout?"
        confirmText="Logout"
        cancelText="Cancel"
        onConfirm={confirmLogout}
        onCancel={cancelLogout}
      />
    </nav>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  nav: {
    backgroundColor: '#1976d2',
    color: 'white',
    padding: '0',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  navContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    maxWidth: '1400px',
    margin: '0 auto',
    padding: '1rem 2rem',
  },
  navBrand: {
    fontSize: '1.5rem',
    fontWeight: 'bold',
  },
  brandLink: {
    color: 'white',
    textDecoration: 'none',
  },
  navLinks: {
    display: 'flex',
    gap: '2rem',
    flex: 1,
    justifyContent: 'center',
  },
  navLink: {
    color: 'white',
    textDecoration: 'none',
    padding: '0.5rem 1rem',
    borderRadius: '4px',
    transition: 'background-color 0.2s',
    fontSize: '1rem',
  },
  navUser: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
  },
  userInfo: {
    fontSize: '0.95rem',
  },
  userRole: {
    fontSize: '0.85rem',
    opacity: 0.8,
    marginLeft: '0.5rem',
  },
  logoutButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#f44336',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.95rem',
    fontWeight: '500',
    transition: 'background-color 0.2s',
  },
};
