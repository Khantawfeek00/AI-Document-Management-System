import * as React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { authApi } from '../services/api';

function UserInfo() {
  const [error, setError] = React.useState<string | null>(null);
  const [user, setUser] = React.useState<any>(null);
  React.useEffect(() => {
    authApi.getUserInfo()
      .then(setUser)
      .catch(e => setError(e.message));
  }, []);
  if (error) return <div data-testid="error">{error}</div>;
  if (!user) return <div>Loading...</div>;
  return <div data-testid="username">{user.username}</div>;
}

describe('Error handling for 401/403', () => {
  beforeEach(() => {
    // Reset fetch mock before each test
    global.fetch = jest.fn();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('shows error for 401', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 401,
      text: async () => 'Unauthorized',
    });

    render(<UserInfo />);
    await waitFor(() => expect(screen.getByTestId('error')).toHaveTextContent('UNAUTHORIZED'));
  });

  it('shows error for 403', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: false,
      status: 403,
      text: async () => 'Forbidden',
    });

    render(<UserInfo />);
    await waitFor(() => expect(screen.getByTestId('error')).toHaveTextContent('FORBIDDEN'));
  });
});
