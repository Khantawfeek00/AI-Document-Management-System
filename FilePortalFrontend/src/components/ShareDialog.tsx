import React, { useState, useEffect } from 'react';
import { fileApi } from '../services/api';

interface ShareDialogProps {
  open: boolean;
  fileId: string;
  filename: string;
  onClose: () => void;
}

interface FileShare {
  id: string;
  sharedWithUserId: string;
  permission: 'READ' | 'WRITE';
  createdAt: string;
}

export const ShareDialog: React.FC<ShareDialogProps> = ({ open, fileId, filename, onClose }) => {
  const [shares, setShares] = useState<FileShare[]>([]);
  const [userEmailOrId, setUserEmailOrId] = useState<string>('');
  const [selectedPermission, setSelectedPermission] = useState<'READ' | 'WRITE'>('READ');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      loadShares();
    }
  }, [open, fileId]);

  const loadShares = async () => {
    try {
      const data = await fileApi.getFileShares(fileId);
      setShares(data);
    } catch (err: any) {
      console.error('Failed to load shares:', err);
    }
  };

  const handleAddShare = async () => {
    if (!userEmailOrId.trim()) {
      setError('Please enter a user email or ID');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await fileApi.createShare(fileId, userEmailOrId.trim(), selectedPermission);
      await loadShares();
      setUserEmailOrId('');
      setSelectedPermission('READ');
      setError(null);
    } catch (err: any) {
      if (err.message.includes('400') || err.message.includes('Bad Request')) {
        setError('User not found or share already exists');
      } else if (err.message === 'FORBIDDEN') {
        setError('You do not have permission to share this file');
      } else {
        setError(err.message || 'Failed to create share');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveShare = async (shareId: string) => {
    setLoading(true);
    try {
      await fileApi.deleteShare(fileId, shareId);
      await loadShares();
    } catch (err: any) {
      setError(err.message || 'Failed to remove share');
    } finally {
      setLoading(false);
    }
  };

  if (!open) return null;

  return (
    <div style={styles.overlay} onClick={onClose}>
      <div style={styles.dialog} onClick={(e) => e.stopPropagation()}>
        <div style={styles.header}>
          <h2 style={styles.title}>Share File: {filename}</h2>
          <button onClick={onClose} style={styles.closeButton}>✕</button>
        </div>

        <div style={styles.content}>
          <div style={styles.section}>
            <h3 style={styles.sectionTitle}>Currently shared with:</h3>
            {shares.length === 0 ? (
              <p style={styles.emptyText}>This file is not shared with anyone yet.</p>
            ) : (
              <div style={styles.sharesList}>
                {shares.map((share) => (
                  <div key={share.id} style={styles.shareItem}>
                    <div>
                      <div style={styles.userName}>User ID: {share.sharedWithUserId}</div>
                      <div style={styles.permission}>
                        Permission: {share.permission === 'READ' ? '👁️ Read' : '✏️ Write'}
                      </div>
                    </div>
                    <button
                      onClick={() => handleRemoveShare(share.id)}
                      style={styles.removeButton}
                      disabled={loading}
                    >
                      Remove
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div style={styles.section}>
            <h3 style={styles.sectionTitle}>Add new share:</h3>

            <div style={styles.formGroup}>
              <label style={styles.label}>User Email or ID:</label>
              <input
                type="text"
                placeholder="Enter user email or ID (e.g., 037e908a-f967-498d-a42a-4e80999dc0cd)..."
                value={userEmailOrId}
                onChange={(e) => setUserEmailOrId(e.target.value)}
                style={styles.input}
                disabled={loading}
              />
              <p style={styles.hint}>
                💡 Enter the UUID or email address of the user you want to share with
              </p>
            </div>

            <div style={styles.formGroup}>
              <label style={styles.label}>Permission:</label>
              <select
                value={selectedPermission}
                onChange={(e) => setSelectedPermission(e.target.value as 'READ' | 'WRITE')}
                style={styles.select}
                disabled={loading}
              >
                <option value="READ">👁️ Read Only</option>
                <option value="WRITE">✏️ Read & Write</option>
              </select>
            </div>

            {error && <div style={styles.error}>{error}</div>}

            <button
              onClick={handleAddShare}
              disabled={loading || !userEmailOrId.trim()}
              style={{
                ...styles.addButton,
                ...(loading || !userEmailOrId.trim() ? styles.addButtonDisabled : {}),
              }}
            >
              {loading ? 'Adding...' : 'Add Share'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  dialog: {
    backgroundColor: 'white',
    borderRadius: '8px',
    width: '90%',
    maxWidth: '600px',
    maxHeight: '80vh',
    overflow: 'auto',
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.15)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1.5rem',
    borderBottom: '1px solid #e0e0e0',
  },
  title: {
    margin: 0,
    fontSize: '1.25rem',
    fontWeight: 600,
    color: '#333',
  },
  closeButton: {
    background: 'none',
    border: 'none',
    fontSize: '1.5rem',
    cursor: 'pointer',
    color: '#666',
    padding: '0.25rem 0.5rem',
  },
  content: {
    padding: '1.5rem',
  },
  section: {
    marginBottom: '2rem',
  },
  sectionTitle: {
    fontSize: '1rem',
    fontWeight: 600,
    marginBottom: '1rem',
    color: '#333',
  },
  emptyText: {
    color: '#666',
    fontStyle: 'italic',
  },
  sharesList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  shareItem: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '1rem',
    backgroundColor: '#f5f5f5',
    borderRadius: '6px',
    border: '1px solid #e0e0e0',
  },
  userName: {
    fontWeight: 500,
    marginBottom: '0.25rem',
  },
  permission: {
    fontSize: '0.875rem',
    color: '#666',
  },
  removeButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#dc3545',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.875rem',
    fontWeight: 500,
  },
  formGroup: {
    marginBottom: '1rem',
  },
  label: {
    display: 'block',
    marginBottom: '0.5rem',
    fontWeight: 500,
    color: '#333',
  },
  input: {
    width: '100%',
    padding: '0.75rem',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '1rem',
    boxSizing: 'border-box',
  },
  hint: {
    fontSize: '0.875rem',
    color: '#666',
    marginTop: '0.5rem',
    marginBottom: 0,
  },
  select: {
    width: '100%',
    padding: '0.75rem',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '1rem',
    backgroundColor: 'white',
    cursor: 'pointer',
    boxSizing: 'border-box',
  },
  error: {
    padding: '0.75rem',
    backgroundColor: '#f8d7da',
    color: '#721c24',
    borderRadius: '4px',
    marginBottom: '1rem',
    fontSize: '0.875rem',
  },
  addButton: {
    width: '100%',
    padding: '0.75rem',
    backgroundColor: '#007bff',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '1rem',
    fontWeight: 500,
    transition: 'background-color 0.2s',
  },
  addButtonDisabled: {
    backgroundColor: '#6c757d',
    cursor: 'not-allowed',
  },
};

export default ShareDialog;

