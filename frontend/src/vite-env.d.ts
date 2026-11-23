/// <reference types="vite/client" />

// Variáveis lidas pelo frontend; todas opcionais, com padrão no código.
interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
  readonly VITE_QUESTION_TIMEOUT_MS?: string;
  readonly VITE_STATUS_TIMEOUT_MS?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
