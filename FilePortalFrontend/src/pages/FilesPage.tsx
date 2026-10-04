import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { fileApi } from '../services/api';
import type { FileMetadata } from '../types';
import ConfirmDialog from '../components/ConfirmDialog';
import ShareDialog from '../components/ShareDialog';
import { useAuth } from '../context/AuthContext';

export const FilesPage: React.FC = () => {
  const { hasRole, user } = useAuth();
  const navigate = useNavigate();
  const isAdmin = hasRole('admin');
  const currentUserId = user?.sub; // L'ID dell'utente corrente

  const [allFiles, setAllFiles] = useState<FileMetadata[]>([]);
  const [sharedFilesData, setSharedFilesData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const notificationTimeoutRef = useRef<number | null>(null);

  const showNotification = (message: string, type: 'success' | 'error' = 'success', duration = 3000) => {
    setNotification({ message, type });
    if (notificationTimeoutRef.current) {
      window.clearTimeout(notificationTimeoutRef.current);
    }
    notificationTimeoutRef.current = window.setTimeout(() => {
      setNotification(null);
      notificationTimeoutRef.current = null;
    }, duration);
  };

  useEffect(() => {
    loadFiles();
    return () => {
      if (notificationTimeoutRef.current) {
        window.clearTimeout(notificationTimeoutRef.current);
      }
    };
  }, []);

  const loadFiles = async () => {
    try {
      setLoading(true);
      setError(null);

      // Carica i file principali e i file condivisi in parallelo
      const [filesResponse, sharedResponse] = await Promise.all([
        fileApi.listFiles(),
        currentUserId ? fileApi.getSharedWithMe().catch(() => []) : Promise.resolve([])
      ]);

      setAllFiles(filesResponse.content);
      setSharedFilesData(sharedResponse);
    } catch (err: any) {
      if (err.message === 'UNAUTHORIZED') {
        setError('Your session has expired. Please login again.');
      } else if (err.message === 'FORBIDDEN') {
        setError('You do not have permission to view files.');
      } else {
        setError('Failed to load files. Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Separa i file in base all'ownership calcolata localmente
  const myFiles = allFiles.filter(f => f.ownerId === currentUserId);

  // File condivisi: usa i dati dall'endpoint dedicato
  const sharedFiles = sharedFilesData;

  // Per admin: file che non sono suoi (tutti gli altri)
  const otherFiles = isAdmin ? allFiles.filter(f => f.ownerId !== currentUserId) : [];

  const handleDownload = async (file: FileMetadata) => {
    try {
      await fileApi.downloadFile(file.fileId, file.filename);
    } catch (err: any) {
      showNotification(`Failed to download file: ${err.message}`, 'error');
    }
  };

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [fileToDelete, setFileToDelete] = useState<FileMetadata | null>(null);

  const [showShareDialog, setShowShareDialog] = useState(false);
  const [fileToShare, setFileToShare] = useState<FileMetadata | null>(null);

  const openDeleteModal = (file: FileMetadata) => {
    setFileToDelete(file);
    setShowDeleteModal(true);
  };

  const cancelDelete = () => {
    setShowDeleteModal(false);
    setFileToDelete(null);
  };

  const confirmDelete = async () => {
    if (!fileToDelete) return;
    try {
      await fileApi.deleteFile(fileToDelete.fileId);
      showNotification('File deleted successfully', 'success');
      setShowDeleteModal(false);
      setFileToDelete(null);
      loadFiles();
    } catch (err: any) {
      if (err.message === 'FORBIDDEN') {
        showNotification('You do not have permission to delete this file.', 'error');
      } else {
        showNotification(`Failed to delete file: ${err.message}`, 'error');
      }
      setShowDeleteModal(false);
      setFileToDelete(null);
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

  const navigateToDetails = (file: FileMetadata) => {
    navigate(`/files/${file.fileId}`);
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
        <h1>My Files</h1>
        <p>Loading files...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.container}>
        <h1>My Files</h1>
        <div style={styles.error}>
          <p>❌ {error}</p>
          <button onClick={loadFiles} style={styles.retryButton}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.container}>
      {notification && (
        <div
          style={{
            ...styles.notification,
            ...(notification.type === 'success' ? styles.notificationSuccess : styles.notificationError),
          }}
          role="status"
          aria-live="polite"
        >
          {notification.message}
        </div>
      )}

      <div style={styles.header}>
        <h1>My Files</h1>
        <button onClick={loadFiles} style={styles.refreshButton}>
          Refresh
        </button>
      </div>

      {myFiles.length === 0 && sharedFiles.length === 0 && otherFiles.length === 0 ? (
        <div style={styles.emptyState}>
          <p>📂 No files found</p>
          {isAdmin ? (
            <p style={{ fontSize: '0.95rem', color: '#666' }}>
              The system has no files yet. Upload the first file to get started.
            </p>
          ) : (
            <>
              <p style={{ fontSize: '0.95rem', color: '#666' }}>
                Upload your first file to get started
              </p>
              <a href="/upload" style={styles.uploadLink}>
                Go to Upload
              </a>
            </>
          )}
        </div>
      ) : (
        <>
          {myFiles.length > 0 && (
            <div style={styles.section}>
              <h2 style={styles.sectionTitle}>📁 My Personal Files</h2>
              <div style={styles.fileList}>
                {myFiles.map(file => (
                  <div key={file.fileId} style={styles.fileCard}>
                    <div style={styles.fileIcon}>📄</div>
                    <div style={styles.fileInfo}>
                      <div style={styles.fileNameRow}>
                        <h3 style={styles.fileName}>{file.filename}</h3>
                        <span style={styles.ownerBadge}>👤 My File</span>
                      </div>
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
                    </div>
                    <div style={styles.fileActions}>
                      <button
                        onClick={() => navigateToDetails(file)}
                        style={styles.detailsButton}
                        title="View file details and AI analysis"
                      >
                        🤖 Details
                      </button>
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
                      <button
                        onClick={() => openDeleteModal(file)}
                        style={styles.deleteButton}
                        title="Delete file"
                      >
                        🗑️ Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {sharedFiles.length > 0 && (
            <div style={styles.section}>
              <h2 style={styles.sectionTitle}>🔗 Shared With Me</h2>
              <div style={styles.fileList}>
                {sharedFiles.map(file => (
                  <div key={file.shareId || file.fileId} style={styles.fileCard}>
                    <div style={styles.fileIcon}>📄</div>
                    <div style={styles.fileInfo}>
                      <div style={styles.fileNameRow}>
                        <h3 style={styles.fileName}>{file.filename}</h3>
                        <span style={styles.sharedBadge}>🔗 Shared</span>
                      </div>
                      <div style={styles.fileMeta}>
                        <span>Size: {formatFileSize(file.size)}</span>
                        <span>Type: {file.contentType}</span>
                        {file.permission && (
                          <span>Permission: {file.permission === 'READ' ? '👁️ Read' : '✏️ Write'}</span>
                        )}
                        {file.sharedAt && (
                          <span>Shared: {formatDate(file.sharedAt)}</span>
                        )}
                      </div>
                      <div style={styles.fileOwner}>
                        Author: {file.ownerName || file.ownerId || 'Unknown'}
                      </div>
                    </div>
                    <div style={styles.fileActions}>
                      <button
                        onClick={() => navigate(`/files/${file.fileId}`)}
                        style={styles.detailsButton}
                        title="View file details and AI analysis"
                      >
                        🤖 Details
                      </button>
                      <button
                        onClick={() => handleDownload({
                          fileId: file.fileId,
                          filename: file.filename,
                          ownerId: file.ownerId || '',
                          size: file.size,
                          contentType: file.contentType,
                          uploadedAt: file.sharedAt || ''
                        } as FileMetadata)}
                        style={styles.downloadButton}
                        title="Download file"
                      >
                        ⬇️ Download
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {isAdmin && otherFiles.length > 0 && (
            <div style={styles.section}>
              <h2 style={styles.sectionTitle}>🗂️ All System Files</h2>
              <p style={styles.sectionDescription}>
                Files from other users (admin view only)
              </p>
              <div style={styles.fileList}>
                {otherFiles.map(file => (
                  <div key={file.fileId} style={styles.fileCard}>
                    <div style={styles.fileIcon}>📄</div>
                    <div style={styles.fileInfo}>
                      <div style={styles.fileNameRow}>
                        <h3 style={styles.fileName}>{file.filename}</h3>
                        <span style={styles.adminBadge}>⚙️ System File</span>
                      </div>
                      <div style={styles.fileMeta}>
                        <span>Size: {formatFileSize(file.size)}</span>
                        <span>Type: {file.contentType}</span>
                        <span>Author: {file.ownerName || file.ownerId}</span>
                        {file.uploadedAt && (
                          <span>Uploaded: {formatDate(file.uploadedAt)}</span>
                        )}
                      </div>
                    </div>
                    <div style={styles.fileActions}>
                      <button
                        onClick={() => navigateToDetails(file)}
                        style={styles.detailsButton}
                        title="View file details and AI analysis"
                      >
                        🤖 Details
                      </button>
                      <button
                        onClick={() => handleDownload(file)}
                        style={styles.downloadButton}
                        title="Download file"
                      >
                        ⬇️ Download
                      </button>
                      <button
                        onClick={() => openDeleteModal(file)}
                        style={styles.deleteButton}
                        title="Delete file (admin)"
                      >
                        🗑️ Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      <ShareDialog
        open={showShareDialog && !!fileToShare}
        fileId={fileToShare?.fileId || ''}
        filename={fileToShare?.filename || ''}
        onClose={closeShareDialog}
      />

      <ConfirmDialog
        open={showDeleteModal && !!fileToDelete}
        title="Confirm delete"
        message={fileToDelete ? `Are you sure you want to delete the file "${fileToDelete.filename}"?` : ''}
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={confirmDelete}
        onCancel={cancelDelete}
      />
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
    alignItems: 'center',
    marginBottom: '2rem',
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
    backgroundColor: '#f9f9f9',
    borderRadius: '8px',
  },
  uploadLink: {
    display: 'inline-block',
    marginTop: '1rem',
    padding: '0.75rem 2rem',
    backgroundColor: '#1976d2',
    color: 'white',
    textDecoration: 'none',
    borderRadius: '4px',
  },
  section: {
    marginBottom: '2.5rem',
  },
  sectionTitle: {
    fontSize: '1.3rem',
    fontWeight: 600,
    marginBottom: '1rem',
    color: '#333',
  },
  sectionDescription: {
    fontSize: '0.9rem',
    color: '#666',
    marginTop: '-0.5rem',
    marginBottom: '1rem',
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
    border: '1px solid #e0e0e0',
    boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
  },
  fileIcon: {
    fontSize: '2.5rem',
    marginRight: '1.5rem',
  },
  fileInfo: {
    flex: 1,
  },
  fileNameRow: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    marginBottom: '0.5rem',
  },
  fileName: {
    margin: 0,
    fontSize: '1.2rem',
    color: '#333',
  },
  ownerBadge: {
    display: 'inline-block',
    padding: '0.25rem 0.75rem',
    backgroundColor: '#4caf50',
    color: 'white',
    fontSize: '0.75rem',
    fontWeight: 600,
    borderRadius: '12px',
  },
  sharedBadge: {
    display: 'inline-block',
    padding: '0.25rem 0.75rem',
    backgroundColor: '#2196f3',
    color: 'white',
    fontSize: '0.75rem',
    fontWeight: 600,
    borderRadius: '12px',
  },
  adminBadge: {
    display: 'inline-block',
    padding: '0.25rem 0.75rem',
    backgroundColor: '#ff9800',
    color: 'white',
    fontSize: '0.75rem',
    fontWeight: 600,
    borderRadius: '12px',
  },
  fileMeta: {
    display: 'flex',
    gap: '1.5rem',
    fontSize: '0.9rem',
    color: '#666',
  },
  fileOwner: {
    marginTop: '0.5rem',
    fontSize: '0.85rem',
    color: '#999',
  },
  fileActions: {
    display: 'flex',
    gap: '0.5rem',
  },
  detailsButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#9c27b0',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.9rem',
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
  deleteButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#f44336',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
  notification: {
    position: 'fixed',
    top: '1rem',
    left: '50%',
    transform: 'translateX(-50%)',
    padding: '0.75rem 1.25rem',
    borderRadius: '6px',
    zIndex: 1100,
    boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
    color: 'white',
    fontWeight: 600,
    minWidth: '200px',
    textAlign: 'center',
  },
  notificationSuccess: {
    backgroundColor: '#2e7d32',
  },
  notificationError: {
    backgroundColor: '#c62828',
  },
};
