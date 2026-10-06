# Frontend

Node **24 LTS**, React **19.3.0**, TypeScript **5.9.3**, Vite **8.3.3**, Vitest **5.0.3**. As versões resolvidas das dependências estão em `package-lock.json`.

```sh
cd frontend
npm ci
cp .env.example .env.local
npm run dev
```

Abra http://127.0.0.1:5173. Se o backend estiver parado, o link das aulas continua acessível e a interface informa indisponibilidade. Use o botão para consultar novamente.

`VITE_API_URL`, `VITE_MATERIAL_URL` e `VITE_STATUS_TIMEOUT_MS` são configurações **públicas**, incorporadas no build. Nunca coloque senhas ou tokens nelas. Para estudar offline, configure o link das aulas para um servidor local com o pacote estático montado; o material também pode ser aberto diretamente por arquivo.

```sh
npm test
npm run build
```

O build fica em `dist/`; não é publicado automaticamente. O GitHub Pages atual continua servindo o material original.

Testes de navegador: `npx playwright install chromium` e `npm run test:e2e`. Para incluir a regressão offline das aulas, monte o pacote com `scripts/build_pages.py` e defina `STUDY_ROOT=/caminho/absoluto/do/pacote` ao executar os testes. Sem essa variável, o teste offline é explicitamente ignorado; o CI sempre a define.
