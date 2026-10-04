import React from 'react';
import { render, screen } from '@testing-library/react';
import { UserTable } from '../components/UserTable';

describe('UserTable conditional rendering', () => {
  it('renders empty state if no users', () => {
    render(<UserTable users={[]} onEdit={() => {}} onDeactivate={() => {}} />);
    expect(screen.queryByText(/no users available/i)).toBeInTheDocument();
  });

  it('renders user rows if users are present', () => {
    render(
      <UserTable
        users={[{ id: '1', username: 'foo', email: 'foo@bar.com', roles: ['USER'] }]}
        onEdit={() => {}}
        onDeactivate={() => {}}
      />
    );
    expect(screen.getByText('foo')).toBeInTheDocument();
    expect(screen.getByText('foo@bar.com')).toBeInTheDocument();
  });

  it('renders skeleton loading state when loading is true', () => {
    render(
      <UserTable
        users={[]}
        onEdit={() => {}}
        onDeactivate={() => {}}
        loading={true}
      />
    );
    // Il skeleton non mostra "No users available"
    expect(screen.queryByText(/no users available/i)).not.toBeInTheDocument();
  });

  it('renders "No users match your search" when highlight is set and no results', () => {
    render(
      <UserTable
        users={[{ id: '1', username: 'foo', email: 'foo@bar.com', roles: ['USER'] }]}
        onEdit={() => {}}
        onDeactivate={() => {}}
        highlight="xyz"
      />
    );
    expect(screen.getByText(/no users match your search/i)).toBeInTheDocument();
  });

  it('renders active status for active users', () => {
    render(
      <UserTable
        users={[{ id: '1', username: 'foo', email: 'foo@bar.com', roles: ['USER'], active: true }]}
        onEdit={() => {}}
        onDeactivate={() => {}}
      />
    );
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('renders deactivated status for inactive users', () => {
    render(
      <UserTable
        users={[{ id: '1', username: 'foo', email: 'foo@bar.com', roles: ['USER'], active: false }]}
        onEdit={() => {}}
        onDeactivate={() => {}}
      />
    );
    expect(screen.getByText('Deactivated')).toBeInTheDocument();
  });
});

