import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fileApi } from '../services/api';
import type { FileDetailDTO, DocumentQueryRequest, DocumentQueryResponse, DocumentSource } from '../types';

// AI Processing Status Badge Component
const AiStatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const getStatusStyle = () => {
    switch (status) {
      case 'COMPLETED':
        return { backgroundColor: '#4caf50', color: 'white' };
      case 'PENDING':
        return { backgroundColor: '#ff9800', color: 'white' };
      case 'FAILED':
        return { backgroundColor: '#f44336', color: 'white' };
      default:
        return { backgroundColor: '#9e9e9e', color: 'white' };
    }
  };

  const getStatusIcon = () => {
    switch (status) {
      case 'COMPLETED':
        return '✓';
      case 'PENDING':
        return '⏳';
      case 'FAILED':
        return '✗';
      default:
        return '?';
    }
  };

  return (
    <span
      style={{
        ...styles.statusBadge,
        ...getStatusStyle(),
      }}
    >
      {getStatusIcon()} {status}
    </span>
  );
};

// Sensitivity Badge Component
const SensitivityBadge: React.FC<{ sensitivity: string | null }> = ({ sensitivity }) => {
  const getSensitivityStyle = () => {
    switch (sensitivity) {
      case 'PUBLIC':
        return { backgroundColor: '#4caf50', color: 'white' };
      case 'INTERNAL':
        return { backgroundColor: '#2196f3', color: 'white' };
      case 'CONFIDENTIAL':
        return { backgroundColor: '#ff9800', color: 'white' };
      case 'HIGHLY_CONFIDENTIAL':
        return { backgroundColor: '#f44336', color: 'white' };
      default:
        return { backgroundColor: '#9e9e9e', color: 'white' };
    }
  };

  const getIcon = () => {
    switch (sensitivity) {
      case 'PUBLIC':
        return '🌐';
      case 'INTERNAL':
        return '🏢';
      case 'CONFIDENTIAL':
        return '🔒';
      case 'HIGHLY_CONFIDENTIAL':
        return '🔐';
      default:
        return '❓';
    }
  };

  return (
    <span
      style={{
        ...styles.sensitivityBadge,
        ...getSensitivityStyle(),
      }}
    >
      {getIcon()} {sensitivity?.replace('_', ' ') || 'Unknown'}
    </span>
  );
};

// Tags Component
const TagsList: React.FC<{ tags: string[] }> = ({ tags }) => {
  if (!tags || tags.length === 0) {
    return <span style={styles.noData}>No tags available</span>;
  }

  return (
    <div style={styles.tagsContainer}>
      {tags.map((tag, index) => (
        <span key={index} style={styles.tag}>
          #{tag}
        </span>
      ))}
    </div>
  );
};

// RAG Query Component
const RagQuerySection: React.FC<{ fileId: string }> = ({ fileId }) => {
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState<DocumentQueryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;

    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      const request: DocumentQueryRequest = {
        question: question.trim(),
        fileId: fileId,
        topK: 5,
      };
      const result = await fileApi.queryDocuments(request);
      setResponse(result);
    } catch (err: any) {
      setError(err.message || 'Failed to query documents');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.ragSection}>
      <h3 style={styles.sectionTitle}>🤖 Ask a Question About Your Documents</h3>
      <form onSubmit={handleSubmit} style={styles.ragForm}>
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a question about this file..."
          style={styles.ragInput}
          disabled={loading}
        />
        <button type="submit" style={styles.ragButton} disabled={loading || !question.trim()}>
          {loading ? '⏳ Asking...' : '🔍 Ask'}
        </button>
      </form>

      {error && (
        <div style={styles.ragError}>
          ❌ {error}
        </div>
      )}

      {response && (
        <div style={styles.ragResponse}>
          <h4 style={styles.answerTitle}>📝 Answer</h4>
          <p style={styles.answerText}>{response.answer}</p>

          {response.sources && response.sources.length > 0 && (
            <div style={styles.sourcesSection}>
              <h5 style={styles.sourcesTitle}>📚 Sources</h5>
              {response.sources.map((source: DocumentSource, index: number) => (
                <div key={index} style={styles.sourceItem}>
                  <div style={styles.sourceHeader}>
                    <span style={styles.sourceFilename}>{source.filename}</span>
                    <span style={styles.sourceSimilarity}>
                      {(source.similarity * 100).toFixed(1)}% match
                    </span>
                  </div>
                  <p style={styles.sourceContent}>"{source.chunkContent}"</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// Main File Detail Page
export const FileDetailPage: React.FC = () => {
  const { fileId } = useParams<{ fileId: string }>();
  const navigate = useNavigate();
  const [file, setFile] = useState<FileDetailDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (fileId) {
      loadFileDetails();
    }
  }, [fileId]);

  const loadFileDetails = async () => {
    if (!fileId) return;

    try {
      setLoading(true);
      setError(null);
      const details = await fileApi.getFileDetails(fileId);
      setFile(details);
    } catch (err: any) {
      if (err.message === 'UNAUTHORIZED') {
        setError('Your session has expired. Please login again.');
      } else if (err.message === 'FORBIDDEN') {
        setError('You do not have permission to view this file.');
      } else {
        setError('Failed to load file details. Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async () => {
    if (!file) return;
    try {
      await fileApi.downloadFile(file.id, file.filename || 'file');
    } catch (err: any) {
      alert(`Failed to download file: ${err.message}`);
    }
  };

  const formatFileSize = (bytes: number | null): string => {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleString();
  };

  if (loading) {
    return (
      <div style={styles.container}>
        <div style={styles.loadingSpinner}>
          <div style={styles.spinner}></div>
          <p>Loading file details...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div style={styles.container}>
        <div style={styles.error}>
          <p>❌ {error}</p>
          <button onClick={loadFileDetails} style={styles.retryButton}>
            Retry
          </button>
          <button onClick={() => navigate('/files')} style={styles.backButton}>
            Back to Files
          </button>
        </div>
      </div>
    );
  }

  if (!file) {
    return (
      <div style={styles.container}>
        <p>File not found</p>
        <button onClick={() => navigate('/files')} style={styles.backButton}>
          Back to Files
        </button>
      </div>
    );
  }

  const latestVersion = file.versions?.[0];

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <button onClick={() => navigate('/files')} style={styles.backButton}>
          ← Back to Files
        </button>
        <h1 style={styles.title}>📄 {file.filename || 'Unknown File'}</h1>
      </div>

      {/* File Info Card */}
      <div style={styles.card}>
        <h2 style={styles.cardTitle}>📋 File Information</h2>
        <div style={styles.infoGrid}>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>File ID:</span>
            <span style={styles.infoValue}>{file.id}</span>
          </div>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>Type:</span>
            <span style={styles.infoValue}>{file.contentType || 'Unknown'}</span>
          </div>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>Size:</span>
            <span style={styles.infoValue}>{formatFileSize(file.size)}</span>
          </div>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>Created:</span>
            <span style={styles.infoValue}>{formatDate(file.createdAt)}</span>
          </div>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>Updated:</span>
            <span style={styles.infoValue}>{formatDate(file.updatedAt)}</span>
          </div>
          <div style={styles.infoItem}>
            <span style={styles.infoLabel}>Versions:</span>
            <span style={styles.infoValue}>{file.versions?.length || 0}</span>
          </div>
        </div>
        <button onClick={handleDownload} style={styles.downloadButton}>
          ⬇️ Download File
        </button>
      </div>

      {/* AI Metadata Card */}
      {latestVersion && (
        <div style={styles.card}>
          <div style={styles.cardHeader}>
            <h2 style={styles.cardTitle}>🤖 AI Analysis</h2>
            <AiStatusBadge status={latestVersion.aiProcessingStatus} />
          </div>

          {latestVersion.aiProcessingStatus === 'PENDING' && (
            <div style={styles.pendingMessage}>
              <div style={styles.spinnerSmall}></div>
              <p>AI analysis is in progress. This may take a few moments...</p>
              <button onClick={loadFileDetails} style={styles.refreshButton}>
                🔄 Refresh Status
              </button>
            </div>
          )}

          {latestVersion.aiProcessingStatus === 'FAILED' && (
            <div style={styles.failedMessage}>
              <p>❌ AI analysis failed. Please try uploading the file again.</p>
            </div>
          )}

          {latestVersion.aiProcessingStatus === 'COMPLETED' && (
            <>
              <div style={styles.aiSection}>
                <h3 style={styles.sectionTitle}>📊 Sensitivity Level</h3>
                <SensitivityBadge sensitivity={latestVersion.sensitivity} />
              </div>

              <div style={styles.aiSection}>
                <h3 style={styles.sectionTitle}>📝 Summary</h3>
                <p style={styles.summaryText}>
                  {latestVersion.summary || 'No summary available'}
                </p>
              </div>

              <div style={styles.aiSection}>
                <h3 style={styles.sectionTitle}>🏷️ Tags</h3>
                <TagsList tags={latestVersion.tags} />
              </div>
            </>
          )}
        </div>
      )}

      {/* RAG Query Section */}
      <div style={styles.card}>
        <RagQuerySection fileId={file.id} />
      </div>

      {/* Version History */}
      {file.versions && file.versions.length > 1 && (
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>📚 Version History</h2>
          <div style={styles.versionList}>
            {file.versions.map((version) => (
              <div key={version.id} style={styles.versionItem}>
                <div style={styles.versionHeader}>
                  <span style={styles.versionNumber}>v{version.versionNumber}</span>
                  <AiStatusBadge status={version.aiProcessingStatus} />
                </div>
                <div style={styles.versionDetails}>
                  <span>Size: {formatFileSize(version.size)}</span>
                  <span>Uploaded: {formatDate(version.uploadDate)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    maxWidth: '1000px',
    margin: '0 auto',
    padding: '2rem',
  },
  header: {
    marginBottom: '2rem',
  },
  title: {
    fontSize: '1.8rem',
    fontWeight: 600,
    color: '#333',
    marginTop: '1rem',
  },
  backButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#f5f5f5',
    color: '#333',
    border: '1px solid #ddd',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
  card: {
    backgroundColor: 'white',
    borderRadius: '8px',
    padding: '1.5rem',
    marginBottom: '1.5rem',
    boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
    border: '1px solid #e0e0e0',
  },
  cardHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1rem',
  },
  cardTitle: {
    fontSize: '1.3rem',
    fontWeight: 600,
    color: '#333',
    margin: 0,
  },
  infoGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))',
    gap: '1rem',
    marginBottom: '1.5rem',
  },
  infoItem: {
    display: 'flex',
    flexDirection: 'column',
  },
  infoLabel: {
    fontSize: '0.85rem',
    color: '#666',
    marginBottom: '0.25rem',
  },
  infoValue: {
    fontSize: '1rem',
    color: '#333',
    fontWeight: 500,
  },
  downloadButton: {
    padding: '0.75rem 1.5rem',
    backgroundColor: '#4caf50',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '1rem',
  },
  statusBadge: {
    display: 'inline-block',
    padding: '0.35rem 0.75rem',
    borderRadius: '16px',
    fontSize: '0.85rem',
    fontWeight: 600,
  },
  sensitivityBadge: {
    display: 'inline-block',
    padding: '0.5rem 1rem',
    borderRadius: '20px',
    fontSize: '0.95rem',
    fontWeight: 600,
  },
  aiSection: {
    marginTop: '1.5rem',
  },
  sectionTitle: {
    fontSize: '1rem',
    fontWeight: 600,
    color: '#555',
    marginBottom: '0.75rem',
  },
  summaryText: {
    fontSize: '1rem',
    lineHeight: 1.6,
    color: '#444',
    backgroundColor: '#f9f9f9',
    padding: '1rem',
    borderRadius: '6px',
    margin: 0,
  },
  tagsContainer: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: '0.5rem',
  },
  tag: {
    display: 'inline-block',
    padding: '0.35rem 0.75rem',
    backgroundColor: '#e3f2fd',
    color: '#1976d2',
    borderRadius: '16px',
    fontSize: '0.9rem',
  },
  noData: {
    color: '#999',
    fontStyle: 'italic',
  },
  pendingMessage: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '2rem',
    backgroundColor: '#fff8e1',
    borderRadius: '8px',
    textAlign: 'center',
  },
  failedMessage: {
    padding: '1rem',
    backgroundColor: '#ffebee',
    borderRadius: '8px',
    color: '#c62828',
  },
  loadingSpinner: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '4rem',
  },
  spinner: {
    width: '40px',
    height: '40px',
    border: '4px solid #f3f3f3',
    borderTop: '4px solid #1976d2',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  spinnerSmall: {
    width: '24px',
    height: '24px',
    border: '3px solid #f3f3f3',
    borderTop: '3px solid #ff9800',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
    marginBottom: '1rem',
  },
  refreshButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#ff9800',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    marginTop: '1rem',
  },
  retryButton: {
    padding: '0.75rem 1.5rem',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    marginRight: '0.5rem',
  },
  error: {
    backgroundColor: '#ffebee',
    border: '1px solid #f44336',
    padding: '2rem',
    borderRadius: '8px',
    textAlign: 'center',
  },
  // RAG Query Styles
  ragSection: {
    marginTop: '0.5rem',
  },
  ragForm: {
    display: 'flex',
    gap: '0.5rem',
    marginBottom: '1rem',
  },
  ragInput: {
    flex: 1,
    padding: '0.75rem 1rem',
    fontSize: '1rem',
    border: '1px solid #ddd',
    borderRadius: '4px',
    outline: 'none',
  },
  ragButton: {
    padding: '0.75rem 1.5rem',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '1rem',
    whiteSpace: 'nowrap',
  },
  ragError: {
    padding: '1rem',
    backgroundColor: '#ffebee',
    borderRadius: '6px',
    color: '#c62828',
    marginBottom: '1rem',
  },
  ragResponse: {
    backgroundColor: '#f5f5f5',
    borderRadius: '8px',
    padding: '1.5rem',
  },
  answerTitle: {
    fontSize: '1.1rem',
    fontWeight: 600,
    color: '#333',
    marginBottom: '0.75rem',
  },
  answerText: {
    fontSize: '1rem',
    lineHeight: 1.6,
    color: '#444',
    marginBottom: '1.5rem',
  },
  sourcesSection: {
    borderTop: '1px solid #ddd',
    paddingTop: '1rem',
  },
  sourcesTitle: {
    fontSize: '0.95rem',
    fontWeight: 600,
    color: '#555',
    marginBottom: '0.75rem',
  },
  sourceItem: {
    backgroundColor: 'white',
    padding: '1rem',
    borderRadius: '6px',
    marginBottom: '0.75rem',
    border: '1px solid #e0e0e0',
  },
  sourceHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '0.5rem',
  },
  sourceFilename: {
    fontWeight: 600,
    color: '#1976d2',
  },
  sourceSimilarity: {
    fontSize: '0.85rem',
    color: '#4caf50',
    fontWeight: 500,
  },
  sourceContent: {
    fontSize: '0.9rem',
    color: '#666',
    fontStyle: 'italic',
    margin: 0,
    lineHeight: 1.5,
  },
  // Version History Styles
  versionList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  versionItem: {
    padding: '1rem',
    backgroundColor: '#f9f9f9',
    borderRadius: '6px',
    border: '1px solid #e0e0e0',
  },
  versionHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '0.5rem',
  },
  versionNumber: {
    fontWeight: 600,
    color: '#333',
    fontSize: '1rem',
  },
  versionDetails: {
    display: 'flex',
    gap: '1.5rem',
    fontSize: '0.9rem',
    color: '#666',
  },
};

export default FileDetailPage;

