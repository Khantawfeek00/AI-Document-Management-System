import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { fileApi } from '../services/api';

export const UploadPage: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError(null);
      setSuccess(false);
    }
  };

  const handleDrop = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    const file = event.dataTransfer.files?.[0];
    if (file) {
      setSelectedFile(file);
      setError(null);
      setSuccess(false);
    }
  };

  const handleDragOver = (event: React.DragEvent<HTMLDivElement>) => {
    event.preventDefault();
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a file first');
      return;
    }

    try {
      setUploading(true);
      setError(null);
      setUploadProgress(0);

      const progressInterval = setInterval(() => {
        setUploadProgress((prev) => Math.min(prev + 10, 90));
      }, 200);

      await fileApi.uploadFile(selectedFile);

      clearInterval(progressInterval);
      setUploadProgress(100);
      setSuccess(true);
      setSelectedFile(null);

      setTimeout(() => {
        setSuccess(false);
        setUploadProgress(0);
      }, 2000);
    } catch (err: any) {
      if (err.message === 'UNAUTHORIZED') {
        setError('Your session has expired. Please login again.');
      } else if (err.message === 'FORBIDDEN') {
        setError('You do not have permission to upload files.');
      } else {
        setError(`Upload failed: ${err.message}`);
      }
    } finally {
      setUploading(false);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const openFileDialog = () => {
    fileInputRef.current?.click();
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div style={styles.container}>
      <h1>Upload File</h1>
      <p style={styles.description}>
        Upload documents to your personal storage. Files are secured and accessible only to authorized users.
      </p>

      <div
        style={styles.dropZone}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onClick={openFileDialog}
      >
        <div style={styles.dropZoneContent}>
          {!selectedFile ? (
            <>
              <div style={styles.uploadIcon}>📤</div>
              <p style={styles.dropZoneText}>
                Drag and drop a file here, or click to select
              </p>
              <input
                type="file"
                onChange={handleFileSelect}
                style={styles.fileInput}
                id="fileInput"
                ref={fileInputRef}
              />
              <label htmlFor="fileInput" style={styles.fileInputLabel} onClick={(e) => e.stopPropagation()}>
                Choose File
              </label>
            </>
          ) : (
            <div style={styles.dropZoneFileSummary}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div style={styles.uploadIcon}>📄</div>
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontSize: '1.05rem', fontWeight: 600 }}>{selectedFile.name}</div>
                  <div style={{ color: '#666', marginTop: '0.25rem' }}>{formatFileSize(selectedFile.size)} • {selectedFile.type || 'Unknown'}</div>
                </div>
              </div>

              <div style={{ marginTop: '1rem', display: 'flex', gap: '0.5rem', justifyContent: 'center' }}>
                <label htmlFor="fileInput" style={{ ...styles.fileInputLabel, padding: '0.5rem 1rem' }} onClick={(e) => e.stopPropagation()}>
                  Change
                </label>
                <button type="button" onClick={(e) => { e.stopPropagation(); handleRemoveFile(); }} style={styles.removeButton}>
                  Remove
                </button>
              </div>

            </div>
          )}
        </div>
      </div>

      {uploading && (
        <div style={styles.progressContainer}>
          <div style={styles.progressBar}>
            <div
              style={{
                ...styles.progressFill,
                width: `${uploadProgress}%`,
              }}
            />
          </div>
          <p style={styles.progressText}>Uploading... {uploadProgress}%</p>
        </div>
      )}

      {error && (
        <div style={styles.error}>
          ❌ {error}
        </div>
      )}

      {success && (
        <div style={styles.success}>
          ✓ File uploaded successfully!
        </div>
      )}

      <div style={styles.actions}>
        <button
          onClick={handleUpload}
          disabled={!selectedFile || uploading}
          style={{
            ...styles.uploadButton,
            opacity: !selectedFile || uploading ? 0.5 : 1,
            cursor: !selectedFile || uploading ? 'not-allowed' : 'pointer',
          }}
        >
          {uploading ? 'Uploading...' : 'Upload File'}
        </button>

        <button
          onClick={() => navigate('/files')}
          style={styles.viewFilesButton}
        >
          View My Files
        </button>
      </div>

      <div style={styles.info}>
        <h3>Upload Guidelines</h3>
        <ul>
          <li>Maximum file size: 100 MB</li>
          <li>Supported formats: All file types</li>
          <li>Files are scanned for security</li>
          <li>You can manage your files from the "My Files" page</li>
        </ul>
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    maxWidth: '800px',
    margin: '0 auto',
  },
  description: {
    fontSize: '1rem',
    color: '#666',
    marginBottom: '2rem',
  },
  dropZone: {
    border: '3px dashed #1976d2',
    borderRadius: '12px',
    padding: '3rem',
    textAlign: 'center',
    backgroundColor: '#f5f9ff',
    marginBottom: '2rem',
    transition: 'background-color 0.2s',
    cursor: 'pointer',
  },
  dropZoneContent: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  uploadIcon: {
    fontSize: '4rem',
    marginBottom: '1rem',
  },
  dropZoneText: {
    fontSize: '1.1rem',
    color: '#666',
    marginBottom: '1rem',
  },
  fileInput: {
    display: 'none',
  },
  fileInputLabel: {
    padding: '0.75rem 2rem',
    backgroundColor: '#1976d2',
    color: 'white',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '1rem',
  },
  dropZoneFileSummary: {
    width: '100%',
    textAlign: 'center',
  },
  removeButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#e0e0e0',
    color: '#333',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.95rem',
  },
  progressContainer: {
    marginBottom: '2rem',
  },
  progressBar: {
    width: '100%',
    height: '30px',
    backgroundColor: '#e0e0e0',
    borderRadius: '15px',
    overflow: 'hidden',
    marginBottom: '0.5rem',
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#4caf50',
    transition: 'width 0.3s ease',
  },
  progressText: {
    textAlign: 'center',
    fontSize: '1rem',
    color: '#666',
  },
  error: {
    backgroundColor: '#ffebee',
    border: '1px solid #f44336',
    padding: '1rem',
    borderRadius: '4px',
    marginBottom: '2rem',
    color: '#c62828',
  },
  success: {
    backgroundColor: '#e8f5e9',
    border: '1px solid #4caf50',
    padding: '1rem',
    borderRadius: '4px',
    marginBottom: '2rem',
    color: '#2e7d32',
    fontSize: '1.1rem',
  },
  actions: {
    display: 'flex',
    gap: '1rem',
    marginBottom: '3rem',
  },
  uploadButton: {
    flex: 1,
    padding: '1rem',
    backgroundColor: '#4caf50',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    fontSize: '1.1rem',
    fontWeight: 'bold',
  },
  viewFilesButton: {
    flex: 1,
    padding: '1rem',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    fontSize: '1.1rem',
    cursor: 'pointer',
  },
  info: {
    backgroundColor: '#fff3e0',
    padding: '1.5rem',
    borderRadius: '8px',
    fontSize: '0.95rem',
  },
};
