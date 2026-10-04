import { rest } from 'msw';

export const handlers = [
  rest.get('/api/auth/userinfo', (req, res, ctx) => {
    // Default: authenticated user with roles
    return res(
      ctx.status(200),
      ctx.json({
        username: 'testuser',
        email: 'test@example.com',
        name: 'Test User',
        roles: ['USER'],
        sub: '1234',
      })
    );
  }),
];

