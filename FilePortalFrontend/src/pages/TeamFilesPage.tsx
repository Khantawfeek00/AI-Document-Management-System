import React, { useEffect, useState } from 'react';
import { fileApi } from '../services/api';
import type { FileMetadata } from '../types';
import AlertDialog from '../components/AlertDialog';
import ShareDialog from '../components/ShareDialog';

export const TeamFilesPage: React.FC = () => {
  const [files, setFiles] = useState<FileMetadata[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [alertOpen, setAlertOpen] = useState(false);
  const [alertMessage, setAlertMessage] = useState('');

  const [showShareDialog, setShowShareDialog] = useState(false);
  const [fileToShare, setFileToShare] = useState<FileMetadata | null>(null);

  useEffect(() => {
    loadTeamFiles();
  }, []);

  const loadTeamFiles = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fileApi.listFiles('department');
      setFiles(response.content);
    } catch (err: any) {
      if (err.message === 'UNAUTHORIZED') {
        setError('Your session has expired. Please login again.');
      } else if (err.message === 'FORBIDDEN') {
        setError('You do not have permission to view team files.');
      } else {
        setError('Failed to load team files. Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (file: FileMetadata) => {
    try {
      await fileApi.downloadFile(file.fileId, file.filename);
    } catch (err: any) {
      setAlertMessage(`Failed to download file: ${err.message}`);
      setAlertOpen(true);
    }
  };

  const openShareDialog = (file: FileMetadata) => {
    setFileToShare(file);
    setShowShareDialog(true);
  };

  const closeShareDialog = () => {
    setShowShareDialog(false);
    setFileToShare(null);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleString();
  };

  if (loading) {
    return (
      <div style={styles.container}>
        <h1>Team Files</h1>
        <p>Loading team files...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.container}>
        <h1>Team Files</h1>
        <div style={styles.error}>
          <p>❌ {error}</p>
          <button onClick={loadTeamFiles} style={styles.retryButton}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div>
          <h1>Team Files</h1>
          <p style={styles.subtitle}>
            Departmental and shared documents accessible to your team
          </p>
        </div>
        <button onClick={loadTeamFiles} style={styles.refreshButton}>
          Refresh
        </button>
      </div>

      {files.length === 0 ? (
        <div style={styles.emptyState}>
          <p>👥 No team files available</p>
          <p style={{ fontSize: '0.95rem', color: '#666' }}>
            Team files will appear here when shared by team members
          </p>
        </div>
      ) : (
        <div style={styles.fileList}>
          {files.map((file) => (
            <div key={file.fileId} style={styles.fileCard}>
              <div style={styles.fileIcon}>📄</div>
              <div style={styles.fileInfo}>
                <h3 style={styles.fileName}>{file.filename}</h3>
                <div style={styles.fileMeta}>
                  <span>Size: {formatFileSize(file.size)}</span>
                  <span>Type: {file.contentType}</span>
                  {file.uploadedAt && (
                    <span>Uploaded: {formatDate(file.uploadedAt)}</span>
                  )}
                </div>
                <div style={styles.fileOwner}>
                  Author: {file.ownerName || file.ownerId || 'Unknown'}
                </div>
                {file.sharedWith && file.sharedWith.length > 0 && (
                  <div style={styles.sharedWith}>
                    Shared with: {file.sharedWith.join(', ')}
                  </div>
                )}
              </div>
              <div style={styles.fileActions}>
                <button
                  onClick={() => handleDownload(file)}
                  style={styles.downloadButton}
                  title="Download file"
                >
                  ⬇️ Download
                </button>
                <button
                  onClick={() => openShareDialog(file)}
                  style={styles.shareButton}
                  title="Share file"
                >
                  👥 Share
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ShareDialog
        open={showShareDialog && !!fileToShare}
        fileId={fileToShare?.fileId || ''}
        filename={fileToShare?.filename || ''}
        onClose={closeShareDialog}
      />

      <AlertDialog open={alertOpen} message={alertMessage} onClose={() => setAlertOpen(false)} />
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    maxWidth: '1200px',
    margin: '0 auto',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '2rem',
  },
  subtitle: {
    fontSize: '1rem',
    color: '#666',
    marginTop: '0.5rem',
  },
    refreshButton: {
        padding: '0.75rem 1.5rem',
        backgroundColor: '#1976d2',
        color: 'white',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer',
        fontSize: '1rem',
    },
  error: {
    backgroundColor: '#ffebee',
    border: '1px solid #f44336',
    padding: '2rem',
    borderRadius: '8px',
    textAlign: 'center',
  },
  retryButton: {
    padding: '0.75rem 2rem',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '1rem',
    marginTop: '1rem',
  },
  emptyState: {
    textAlign: 'center',
    padding: '4rem 2rem',
    backgroundColor: '#fff3e0',
    borderRadius: '8px',
  },
  fileList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  fileCard: {
    display: 'flex',
    alignItems: 'center',
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '8px',
    border: '1px solid #ff9800',
    borderLeft: '4px solid #ff9800',
    boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
  },
  fileIcon: {
    fontSize: '2.5rem',
    marginRight: '1.5rem',
  },
  fileInfo: {
    flex: 1,
  },
  fileName: {
    margin: '0 0 0.5rem 0',
    fontSize: '1.2rem',
    color: '#333',
  },
  fileMeta: {
    display: 'flex',
    gap: '1.5rem',
    fontSize: '0.9rem',
    color: '#666',
    marginBottom: '0.5rem',
  },
  fileOwner: {
    fontSize: '0.9rem',
    color: '#ff9800',
    fontWeight: '500',
  },
  sharedWith: {
    marginTop: '0.5rem',
    fontSize: '0.85rem',
    color: '#999',
  },
  fileActions: {
    display: 'flex',
    gap: '0.5rem',
  },
  downloadButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#4caf50',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
  shareButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#2196f3',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
};
