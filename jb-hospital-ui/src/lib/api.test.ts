import { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import api, { apiError, TOKEN_KEY, USER_KEY } from './api';

function httpError(status: number, data: unknown, url = '/api/v1/opd/visits/today') {
  const config = { url, headers: new AxiosHeaders() } as InternalAxiosRequestConfig;
  return new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, {}, {
    status, statusText: '', data, headers: {}, config,
  });
}

describe('apiError', () => {
  it('prefers the problem+json detail', () => {
    expect(apiError(httpError(409, { title: 'Conflict', detail: 'Ram Bahadur already has an open visit today.' })))
      .toBe('Ram Bahadur already has an open visit today.');
  });

  it('falls back to a legacy message field, then to the status code', () => {
    expect(apiError(httpError(400, { message: 'Bad thing' }))).toBe('Bad thing');
    expect(apiError(httpError(502, ''), 'Could not save')).toBe('Could not save (HTTP 502)');
  });

  it('explains a network failure', () => {
    const offline = new AxiosError('Network Error', 'ERR_NETWORK');
    expect(apiError(offline)).toMatch(/Cannot reach the server/);
  });

  it('uses the fallback for anything that is not an HTTP error', () => {
    expect(apiError(new Error('boom'), 'Sign-in failed')).toBe('Sign-in failed');
  });
});

describe('api client', () => {
  const originalAdapter = api.defaults.adapter;
  afterEach(() => { api.defaults.adapter = originalAdapter; });

  it('sends the stored token as a Bearer header', async () => {
    localStorage.setItem(TOKEN_KEY, 'abc.def.ghi');
    const adapter = vi.fn(async (config: InternalAxiosRequestConfig) =>
      ({ data: [], status: 200, statusText: 'OK', headers: {}, config }));
    api.defaults.adapter = adapter;

    await api.get('/api/v1/opd/visits/today');

    expect(adapter.mock.calls[0][0].headers.Authorization).toBe('Bearer abc.def.ghi');
  });

  it('signs the user out when a request comes back 401', async () => {
    localStorage.setItem(TOKEN_KEY, 'expired');
    localStorage.setItem(USER_KEY, '{"username":"nurse"}');
    const assign = vi.fn();
    vi.spyOn(window, 'location', 'get').mockReturnValue({ ...window.location, pathname: '/triage', assign });
    api.defaults.adapter = async (config) => { throw httpError(401, {}, config.url); };

    await expect(api.get('/api/v1/opd/visits/today')).rejects.toBeInstanceOf(AxiosError);

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(USER_KEY)).toBeNull();
    expect(assign).toHaveBeenCalledWith('/login?expired=1');
  });

  it('does not treat a failed login (401) as an expired session', async () => {
    const assign = vi.fn();
    vi.spyOn(window, 'location', 'get').mockReturnValue({ ...window.location, pathname: '/login', assign });
    api.defaults.adapter = async (config) => { throw httpError(401, { detail: 'Invalid username or password.' }, config.url); };

    await expect(api.post('/api/v1/auth/login', {})).rejects.toBeInstanceOf(AxiosError);

    expect(assign).not.toHaveBeenCalled();
  });

  it('keeps the session on a 403', async () => {
    localStorage.setItem(TOKEN_KEY, 'still-valid');
    api.defaults.adapter = async (config) => { throw httpError(403, { detail: 'Your role is not allowed to do this.' }, config.url); };

    await expect(api.get('/api/v1/opd/patients')).rejects.toBeInstanceOf(AxiosError);

    expect(localStorage.getItem(TOKEN_KEY)).toBe('still-valid');
  });
});
