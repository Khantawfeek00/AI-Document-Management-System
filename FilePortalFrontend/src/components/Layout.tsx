import React from 'react';
import { Outlet } from 'react-router-dom';
import { Navigation } from './Navigation';

export const Layout: React.FC = () => {
  return (
    <div style={styles.container}>
      <Navigation />
      <main style={styles.main}>
        <Outlet />
      </main>
      <footer style={styles.footer}>
        <p>File Portal - Web Applications II</p>
        <p style={{ fontSize: '0.85rem', opacity: 0.8 }}>
          Gateway-centric architecture • OAuth2/OIDC • Role-based access control
        </p>
      </footer>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    minHeight: '100vh',
  },
  main: {
    flex: 1,
    maxWidth: '1400px',
    width: '100%',
    margin: '0 auto',
    padding: '2rem',
  },
  footer: {
    backgroundColor: '#f5f5f5',
    padding: '1.5rem',
    textAlign: 'center',
    color: '#666',
    borderTop: '1px solid #ddd',
  },
};
