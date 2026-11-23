import { afterEach, expect, it, vi } from 'vitest';
import { apiStatus } from './status-api';

afterEach(() => vi.unstubAllGlobals());

const respond = (body: unknown, status = 200) => vi.stubGlobal('fetch', vi.fn(async () => new Response(JSON.stringify(body), { status })));

it('mostra a API disponível só com resposta OK e status UP', async () => {
  respond({ status: 'UP' });
  await expect(apiStatus(new AbortController().signal)).resolves.toBe('up');
});

it.each([
  ['status diferente', { status: 'DOWN' }, 200],
  ['corpo sem status', { ok: true }, 200],
  ['corpo nulo', null, 200],
  ['corpo em lista', ['UP'], 200],
  ['status como texto solto', 'UP', 200],
  ['resposta de erro com UP', { status: 'UP' }, 503],
])('mostra a API indisponível com corpo fora do contrato: %s', async (_name, body, status) => {
  respond(body, status);
  await expect(apiStatus(new AbortController().signal)).resolves.toBe('down');
});

it('mostra a API indisponível quando o corpo não é JSON ou a rede falha', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => new Response('<html>', { status: 200 })));
  await expect(apiStatus(new AbortController().signal)).resolves.toBe('down');
  vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('network'); }));
  await expect(apiStatus(new AbortController().signal)).resolves.toBe('down');
});
