// Peças de contrato realmente idênticas entre os módulos de API. Validadores com mensagens ou
// semânticas diferentes (por exemplo, texto vazio aceito ou não) continuam em cada módulo.

export const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

/** Campo de um corpo JSON ainda não validado; `undefined` quando o corpo não é objeto. */
export function field(body: unknown, key: string): unknown {
  return isObject(body) ? body[key] : undefined;
}
