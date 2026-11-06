/** Builds an unsigned JWT-shaped token: the UI only reads the payload, it never verifies the signature. */
export function fakeToken(payload: Record<string, unknown>): string {
  const b64 = (o: unknown) => btoa(JSON.stringify(o)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64(payload)}.signature`;
}

export const inOneHour = () => Math.floor(Date.now() / 1000) + 3600;
export const anHourAgo = () => Math.floor(Date.now() / 1000) - 3600;
