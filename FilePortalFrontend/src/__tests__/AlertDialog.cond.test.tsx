import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { AlertDialog } from '../components/AlertDialog';

describe('AlertDialog conditional rendering', () => {
  it('does not render when open is false', () => {
    render(<AlertDialog open={false} message="Test" onClose={() => {}} />);
    expect(screen.queryByText('Test')).not.toBeInTheDocument();
  });

  it('renders when open is true', () => {
    render(<AlertDialog open={true} message="Test" onClose={() => {}} />);
    expect(screen.getByText('Test')).toBeInTheDocument();
  });

  it('calls onClose when OK is clicked', () => {
    const onClose = jest.fn();
    render(<AlertDialog open={true} message="Test" onClose={onClose} />);
    fireEvent.click(screen.getByText('OK'));
    expect(onClose).toHaveBeenCalled();
  });
});

