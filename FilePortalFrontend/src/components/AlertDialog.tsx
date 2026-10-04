import React from 'react';

interface AlertDialogProps {
  open: boolean;
  title?: string;
  message: string;
  okText?: string;
  onClose: () => void;
}

export const AlertDialog: React.FC<AlertDialogProps> = ({
  open,
  title = 'Notice',
  message,
  okText = 'OK',
  onClose,
}) => {
  if (!open) return null;

  return (
    <div style={styles.modalOverlay} onClick={onClose}>
      <div style={styles.modalContent} onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true">
        <h3 style={styles.modalTitle}>{title}</h3>
        <p style={styles.modalMessage}>{message}</p>
        <div style={styles.modalButtons}>
          <button onClick={onClose} style={styles.okButton}>{okText}</button>
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
    padding: '1.25rem',
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
  },
  okButton: {
    padding: '0.5rem 1rem',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
  },
};

export default AlertDialog;
