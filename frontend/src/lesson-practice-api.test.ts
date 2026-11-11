import { afterEach, expect, it, vi } from 'vitest';
import { createLessonPractice, lessonPracticeAvailability, lessonPracticeHistory, lessonPracticeTransport } from './lesson-practice-api';
import { validateAttempt } from './attempt-api';
import { view, result } from '../test/practice-fixtures';
import { response } from '../test/question-fixtures';
const signal = () => new AbortController().signal;
afterEach(() => vi.unstubAllGlobals());
function api(value: unknown) {
  const fetchMock = vi.fn(async (url: string, _options?: RequestInit) => url.endsWith('/csrf') ? response({ token: 'csrf-test', headerName: 'X-CSRF-TOKEN' }) : response(value));
  vi.stubGlobal('fetch', fetchMock); return fetchMock;
}
it('envia somente comando/assunto e recusa projeção de prática no validador de simulado', async () => {
  const fetchMock = api(view);
  expect(await createLessonPractice({ topicId: 'P01' }, 'same-command', signal())).toEqual(view);
  const write = fetchMock.mock.calls.find(([url]) => url.endsWith('/lesson-practices'))!;
  expect(JSON.parse(String(write[1]?.body))).toEqual({ topicId: 'P01', commandId: 'same-command' });
  expect(write[1]).toMatchObject({ credentials: 'include', cache: 'no-store', headers: { 'X-CSRF-TOKEN': 'csrf-test' } });
  expect(() => validateAttempt(view)).toThrow();
});
it.each([
  { ...view, roleId: 'assistente-administrativo' },
  { ...view, topicId: 'P02' },
  { ...view, items: [{ ...view.items[0], resolution: result.items[0].explanation }] },
  { ...view, items: [view.items[0], view.items[0]] },
])('nega cargo implícito, assunto divergente ou vazamento antes de encerrar', async value => {
  api(value); await expect(lessonPracticeTransport.read(view.id, signal())).rejects.toThrow();
});
it('classificações editoriais diferentes continuam aceitas e não criam cargo da realização', async () => {
  const value = { ...view, items: [{ ...view.items[0], classification: { ...view.items[0].classification, roleIds: ['secretario-de-escola'] } }] };
  api(value); expect(await lessonPracticeTransport.read(view.id, signal())).toEqual(value);
});
it('disponibilidade não envia cargo e o histórico rejeita contratos antigos com cargo', async () => {
  const fetchMock = api({ topicId: 'P01', available: 7, maximum: 5 });
  await lessonPracticeAvailability('P01', signal());
  expect(fetchMock.mock.calls[0][0]).toMatch(/availability\?topicId=P01$/);
  api({ page: 0, size: 20, items: [{ id: view.id, topicId: 'P01', roleId: 'assistente-administrativo', createdAt: view.createdAt, finishedAt: null, total: 1, percentage: null }] });
  await expect(lessonPracticeHistory(0, signal())).rejects.toThrow();
});
it('encerramento valida contrato por assunto e conserva correção calculada pelo servidor', async () => {
  const closed = { ...view, version: 1, finishedAt: '2026-11-10T12:00:00Z' };
  const wire = { ...result, items: [{ item: view.items[0], correctAlternativeId: view.items[0].alternatives[0].id, resolution: result.items[0].explanation }] };
  api({ attempt: closed, result: wire });
  expect(await lessonPracticeTransport.finish(view.id, 'command', 0, signal())).toEqual({ attempt: closed, result });
});
