import React from 'react';

interface ConfirmDialogProps {
  open: boolean;
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  open,
  title = 'Confirm',
  message,
  confirmText = 'Yes',
  cancelText = 'Cancel',
  onConfirm,
  onCancel,
}) => {
  if (!open) return null;

  return (
    <div style={styles.modalOverlay} onClick={onCancel}>
      <div style={styles.modalContent} onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <h3 style={styles.modalTitle}>{title}</h3>
        <p style={styles.modalMessage}>{message}</p>
        <div style={styles.modalButtons}>
          <button onClick={onCancel} style={styles.cancelButton}>{cancelText}</button>
          <button onClick={onConfirm} style={styles.confirmButton}>{confirmText}</button>
        </div>
      </div>
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  modalOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1200,
  },
  modalContent: {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: 8,
    boxShadow: '0 6px 24px rgba(0,0,0,0.2)',
    maxWidth: 480,
    width: '90%',
  },
  modalTitle: {
    margin: '0 0 0.5rem 0',
    fontSize: '1.25rem',
  },
  modalMessage: {
    margin: '0 0 1rem 0',
    color: '#444',
  },
  modalButtons: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '0.5rem',
  },
  cancelButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#e0e0e0',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
  },
  confirmButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
  },
};

export default ConfirmDialog;
