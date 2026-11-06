import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import { TOKEN_KEY, USER_KEY } from '../lib/api';
import type { Role } from '../lib/types';
import { anHourAgo, fakeToken, inOneHour } from '../test/jwt';
import { RequireRole } from './RequireRole';

function signIn(role: Role, exp = inOneHour()) {
  localStorage.setItem(TOKEN_KEY, fakeToken({ sub: role.toLowerCase(), roles: role, exp }));
  localStorage.setItem(USER_KEY, JSON.stringify({ username: role.toLowerCase(), fullName: 'Test User', role }));
}

function renderAt(path: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<p>login page</p>} />
          <Route path="/forbidden" element={<p>forbidden page</p>} />
          <Route element={<RequireRole roles={['DOCTOR']} />}>
            <Route path="/doctor" element={<p>doctor workspace</p>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

describe('RequireRole', () => {
  it('sends signed-out users to the login page', () => {
    renderAt('/doctor');
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('lets the right role through', () => {
    signIn('DOCTOR');
    renderAt('/doctor');
    expect(screen.getByText('doctor workspace')).toBeInTheDocument();
  });

  it('shows the forbidden page to other roles', () => {
    signIn('NURSE');
    renderAt('/doctor');
    expect(screen.getByText('forbidden page')).toBeInTheDocument();
  });

  it('does not restore a session whose token has expired, and clears it', () => {
    signIn('DOCTOR', anHourAgo());
    renderAt('/doctor');
    expect(screen.getByText('login page')).toBeInTheDocument();
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('treats a corrupted stored session as signed out', () => {
    localStorage.setItem(TOKEN_KEY, 'not-a-jwt');
    localStorage.setItem(USER_KEY, '{broken');
    renderAt('/doctor');
    expect(screen.getByText('login page')).toBeInTheDocument();
  });
});
