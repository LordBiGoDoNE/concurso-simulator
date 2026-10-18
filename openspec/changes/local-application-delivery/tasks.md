# Tasks

Proposta#124 aprovada;grupos completos1–4 autorizados pelo usuário em2026-10-18,acompanhamento#123. **15/16 implementadas/verificadas e aprovadas/integradas na épica da spec pelos PRs#125–#128**, após revisão SOL6.1xHigh e aprovação explícita. Merge atômico0ae60a410fd9cd8c235a5a5d198d1128cb54ce40,árvore idêntica ao topo aprovadoa060427. Item4.4/gate cumulativo em execução,grupo4 ainda aberto.
Usuário escolheu Docker local e todos os16 itens. Integração da spec na épica geral exige gate cumulativo e aprovação própria;main/publicação/arquivamento fora desta autorização.
Cada grupo inclui código, testes, documentação e revisão SOL6.1xHigh. Dependências
reais usam Stacks da mesma spec, com PRs coesos; merge exige aprovação própria.

## 1. Imagens e pacote executável

- [x] 1.1 Empacotar backend Java25/JAR em imagem de runtime sem Gradle/fixtures/launchers de teste; verificar execução e identidade do artefato; documentar versão/requisitos.
- [x] 1.2 Empacotar SPA compilada e material público em servidor HTTP, sem Vite no runtime; testar arquivos/51 aulas/encoding e independência da API; documentar origem e parâmetros públicos.
- [x] 1.3 Restringir contexto/camadas/usuários/segredos dos builds; testar sentinelas, ausência de arquivos privados/fixtures e separação de material/bundle; documentar alcance e limites.
- [x] 1.4 Demonstrar build em exportação limpa com versões fixadas e registrar commit/IDs das imagens; testar execução dos artefatos obtidos, sem alegar determinismo não medido; documentar comandos Windows/Linux.

## 2. Compose local, origem e segurança — depende de 1

- [x] 2.1 Criar projeto Compose próprio com rede/volume isolados, web em127.0.0.1 e API/PG sem portas no host; testar isolamento, colisão e stop/start sem apagar dados; documentar preservação do ambiente existente.
- [x] 2.2 Encaminhar API/OAuth/callback na mesma origem e configurar base pública do material/porta; testar métodos/query/status/no-store, paths negados e ausência de fallback HTML em erros; documentar proprietários conforme checklist HTTP.
- [x] 2.3 Fornecer configuração externa/segredos por arquivo e perfil local explícito, Google desligado; testar falta de configuração, cookies/CSRF/CORS e manutenção das proteções padrão/produção; documentar habilitação opcional sem credenciais versionadas.
- [x] 2.4 Integrar healthchecks/prontidão e falhas limitadas; testar banco indisponível antes/depois do startup, recuperação e aulas acessíveis, sem falso UP ou vazamento; documentar diagnóstico sem dados privados.

## 3. Persistência e recuperação — depende de 2

- [x] 3.1 Demonstrar FlywayV1–V10,validate e volume persistente sem seed/importação automática; testar reinício e banco vazio honesto, preservando CLIs editoriais e banco do operador; documentar autorização específica para cópia real.
- [x] 3.2 Implementar backup lógico local deliberado com credencial fora de args/logs, artefato privado e identificação de origem/versões; testar dump consistente com fixtures sintéticas; documentar papéis/configuração fora do dump.
- [x] 3.3 Restaurar somente em destino novo/vazio com confirmação de identidade, API parada e invalidação das sessões restauradas antes de uso; testar backup→logout→restore mantendo cookie antigo401, igualdade de negócio/Flyway/capturas/comandos e histórico/retomada após nova sessão, rejeitando destino ocupado; documentar validação e diferença de reinício comum.
- [x] 3.4 Ensaiar stop/restart/retorno a imagem compatível sem apagar volume ou migrar para trás; testar V5/formato2/índiceV10,geração desligada e leitores compatíveis; documentar plano local e limites de recuperação.

## 4. Verificação integrada e gate — depende de 1–3

- [x] 4.1 Verificar imagens reais via navegador/proxy/JAR/PG, visitante e conta com sessão sintética criada fora do runtime em banco descartável; cobrir composição/retomada/conclusão/revisão/prática,CSRF/retry/duas sessões e executar OIDC existente separadamente; documentar matriz e limites, sem alegar Google real.
- [x] 4.2 Executar regressões Java/ArchUnit/PG/HTTP/frontend/material/offline e inspeção de artefatos; conferir51 aulas/111 variantes sem fixtures/segredos/solver no cliente; ajustar CI somente para verificar, preservando Pages e proteções.
- [x] 4.3 Executar o procedimento local em exportação limpa, reunindo build/start/stop/backup/restore/reinício e falhas, com evidências reproduzíveis e limites Windows/Linux; documentar pendências públicas da etapa7 sem declará-la concluída.
- [ ] 4.4 Consolidar PRs/SHAs/checks/triagem/cota/tokens e gate cumulativo independente SOL6.1xHigh; manter [SPEC] para épica geral e solicitar aprovação própria, sem main/deploy/arquivamento ou autorização presumida da próxima spec.
