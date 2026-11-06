import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import { TOKEN_KEY } from '../lib/api';
import { AuthApi } from '../lib/services';
import { fakeToken, inOneHour } from '../test/jwt';
import Login from './Login';

vi.mock('../lib/services', () => ({ AuthApi: { login: vi.fn() } }));
const login = vi.mocked(AuthApi.login);

function renderLogin(path = '/login') {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/doctor" element={<p>doctor workspace</p>} />
          <Route path="/registration" element={<p>registration desk</p>} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

describe('Login', () => {
  beforeEach(() => { login.mockReset(); });

  it('signs in and lands each role on its own workspace', async () => {
    login.mockResolvedValue({
      accessToken: fakeToken({ sub: 'doctor', exp: inOneHour() }),
      username: 'doctor', fullName: 'Dr. Ramesh Sharma', role: 'DOCTOR',
    });
    renderLogin();

    await userEvent.type(screen.getByLabelText('Username'), '  doctor ');
    await userEvent.type(screen.getByLabelText('Password'), 'Demo@1234');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('doctor workspace')).toBeInTheDocument();
    expect(login).toHaveBeenCalledWith('doctor', 'Demo@1234');
    expect(localStorage.getItem(TOKEN_KEY)).not.toBeNull();
  });

  it('demo account buttons fill the form', async () => {
    renderLogin();

    await userEvent.click(screen.getByRole('button', { name: /lab tech/i }));

    expect(screen.getByLabelText('Username')).toHaveValue('labtech');
    expect(screen.getByLabelText('Password')).toHaveValue('Demo@1234');
  });

  it('shows the server message when sign-in fails', async () => {
    const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;
    const rejected = new AxiosError('401', 'ERR_BAD_REQUEST', config, {}, {
      status: 401, statusText: '', headers: {}, config, data: { detail: 'Invalid username or password.' },
    });
    login.mockRejectedValue(rejected);
    renderLogin();

    await userEvent.type(screen.getByLabelText('Username'), 'nurse');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('Invalid username or password.')).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toBeEnabled();
  });

  it('explains why the user was signed out', () => {
    renderLogin('/login?expired=1');
    expect(screen.getByText(/your session expired/i)).toBeInTheDocument();
  });
});
