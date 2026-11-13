# Verificação local — issue #160

> Registro histórico **R1**, grupos 1–3. A extensão de autenticação dos grupos 4–6 foi planejada depois desta execução; estes resultados não a validam. Nenhum teste novo de produto foi executado naquela rodada de planejamento. A implementação/verificação R2 posterior está registrada separadamente em [verification-r2.md](verification-r2.md).

Data: 2026-11-09. Base e HEAD preservados: `f61d57d5d55d6d61609815a2faa4b415ed8dca4b`.
Grupos completos 1–3 autorizados pelo usuário e implementados neste worktree do aplicativo.
Estado: implementação/verificação local; **sem commit, PR, envio ao GitHub, integração,
aprovação editorial, carga em banco de uso ou deploy**. Não foram movidas refs existentes,
reorganizadas stacks ou alterados outros checkouts. A atualização pública da #160 fica
para envio autorizado; estes arquivos não são links publicados.

## Entrega por grupo

| Grupo | Implementação e verificação local |
| --- | --- |
| 1 | Pacote autossuficiente e estrito, SIMULADO e PRATICA_AULA, origens preservadas, digests/seleção, fontes/recursos por bytes, consulta restrita e exemplo sintético validado por Java. Política de segurança compartilhada com leitores antigos. |
| 2 | Aplicação na mesma sessão transacional de questões, recibo, manifesto de revisões com FK e arquivos/blobs privados. Migração aditiva V12, imutabilidade, replay, conflito e rollback real. |
| 3 | Prévia/aplicação HTTP, permissão por UUID externo, CSRF/CORS reais, confirmação HMAC expirada/vinculada, erros sanitizados, capacidade limitada, tratamento pré-MVC de indisponibilidade da sessão, OpenAPI e guia operacional. |

Limites entregues: 100 itens por pacote, sem teto acumulado; 16 MiB JSON, até 100 arquivos
de 1 MiB e 8 MiB decodificados somados; confirmação de 15 minutos; uma operação por
instância sem fila. Prática permanece separada por assunto/cargo e seleciona até cinco
questões por treino a partir de acervo ampliável. Templates e decisões editoriais HTTP
continuam fora do escopo.

## Comandos executados e resultados

Ambiente desta execução: Windows, Gradle Wrapper 9.8.0, JDK 25 disponível localmente,
Docker Desktop e PostgreSQL **18.6** em Testcontainers exclusivos, com portas aleatórias
e schemas privados. O Java padrão do terminal era 21; `JAVA_HOME` foi configurado somente
no processo de execução para o JDK 25. Nenhum resultado antigo foi presumido nesta máquina.

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-25.0.1.8-hotspot'
./backend/gradlew.bat -p backend --offline --no-daemon check bootJar
./backend/gradlew.bat -p backend --offline --no-daemon test --tests '*AdministrativeImportHttpTests'
$env:OPENSPEC_TELEMETRY='0'
# CLI @fission-ai/openspec 1.14.1 do cache local, sem instalação/upgrade global:
openspec validate administrative-question-import --strict
git diff --check
```

- `check bootJar`: **BUILD SUCCESSFUL**, 279 casos, 278 aprovados, zero falhas/erros,
  um teste preexistente de symlink ignorado no Windows
  (`TemplateLocalTests.symbolicLinkIsRejectedWhenTheFilesystemSupportsCreatingOne`).
- Após a suíte completa, acrescentou-se **somente teste**, sem mudança de produção:
  contratos 500/503 por falha injetada no caso de uso, pela cadeia HTTP real e captura
  de logs. Os seis testes de `AdministrativeImportHttpTests` passaram. Esse caso novo
  foi verificado nessa execução complementar; não foi contado como parte dos 279.
- No conjunto, **22 casos novos**: sete de pacote/limites, seis HTTP, cinco de
  persistência, um de arquitetura, dois de capacidade/encoding e um de sessão indisponível.
- A suíte completa inclui regressões executadas de identidade/OIDC, sessão, tentativas,
  banco público, leitores locais (509 fingerprints e lote de 255), práticas, geração
  parametrizada e revisão por assunto. Não houve execução de frontend, pois não há
  alteração de interface/frontend nesta entrega.
- OpenSpec **1.14.1**, `validate --strict`: válido. A CLI confirmou a raiz deste worktree.
- OpenAPI: YAML lido com sucesso e **178 referências locais resolvidas**; exemplos e
  respostas principais também exercitados por HTTP. Isso não é certificação semântica
  por validador externo ou revisão independente.
- `git diff --check`: sem erros; arquivos novos também conferidos quanto a whitespace
  e UTF-8. Nenhuma alteração de AGENTS.md, openspec/config.yaml, workflows ou V1–V11.
- JAR gerado: `backend/build/libs/concurso-simulator-0.1.0-SNAPSHOT.jar` (artefato local,
  não versionado nem publicado).

Os regressores antigos leem material versionado e podem importá-lo como DRAFT em bancos
descartáveis. Isso não é carga em banco existente/de uso, curadoria ou aprovação de
conteúdo real. A aprovação usada para verificar crescimento do acervo foi de fixtures
sintéticas exclusivas: seis questões aprovadas de teste, disponibilidade seis e treino
de cinco, com assunto/cargo e separação de coleção preservados.

## Observações de recursos

Uma execução complementar dos sete testes de pacote passou em 11 s e registrou:

| Fixture sintética | Tempo observado do trecho | Heap antes / depois, bytes |
| --- | --- | --- |
| 100 itens e 8.388.608 bytes de evidência | 117 ms | 63.396.224 / 212.294.016 |
| Corpo JSON com 16.777.216 bytes | 83 ms | 48.384.944 / 143.645.088 |

São leituras do MemoryMXBean antes/depois, **não pico de memória, alocação atribuível,
benchmark de throughput ou garantia de latência**. GC, aquecimento e uso concorrente da
máquina limitam a comparação. Não somar as leituras nem atribuí-las a custo de assinatura.
Tentativa de obter dados de processo por CIM estava indisponível no sandbox; a observação
acima veio do próprio JVM de testes. Casos acima do teto foram recusados; nenhuma hipótese
de capacidade autorizou elevar limites, criar job ou relaxar timeout/transação.

## Correções encontradas durante a execução

- A primeira versão acoplava web aos adaptadores e uma annotation de erro JPA. Regra
  positiva: controller usa contratos/portas da aplicação; persistência e sua tradução
  ficam fora da web. `ArchitectureTests` detectou e passou após a correção; não foi
  relaxado. A proteção verifica dependências, não todas as decisões semânticas.
- Um teto de 100 na consulta genérica do inventário quebrava a CLI histórica de 255.
  O teto pertence ao formato administrativo, não à porta compartilhada. O leitor HTTP
  continua limitado e as duas operações antigas foram revalidadas integralmente.
- A nova V12 exige nove migrações após V3 e doze em banco vazio. Atualizou-se a contagem
  esperada no teste existente, mantendo a comparação de usuários/sessões e acrescentando
  confirmação de que as quatro tabelas novas começam vazias.
- Indisponibilidade do banco de sessões ocorre antes do MVC. Filtro de infraestrutura
  limitado ao prefixo administrativo agora traduz falhas conhecidas para 503 JSON/no-store,
  com CORS explícito. Teste real desliga exclusivamente seu PostgreSQL descartável.

## Risco, revisão e próxima etapa

Classificação **crítica**, conforme `docs/architecture/spec-review.md`: autorização/CSRF,
entrada não confiável, persistência/schema, concorrência/idempotência e privacidade.
Os checks locais não certificam revisão semântica ou integração.

Não foi executado revisor independente, gate cumulativo ou medição de cota/tokens de
revisão. Modelo/sessão/SHAs de PR/checks remotos: não aplicáveis nesta rodada sem envio.
Isso é **não executado/não medido**, não consumo ou achados zero.

Pendências próprias da etapa seguinte: teste/validação do usuário, autorização de
commit/envio e separação em branches/PRs coesos; coordenação central dos contratos
compartilhados (OpenAPI, segurança e número V12) com main então conferida; registro
público da #160 somente quando autorizado. Revisões futuras usam SOL
`openai/gpt-6.1-sol#xhigh`, com modelo efetivo confirmado, contexto novo e protocolo
de cota. Aprovar a implementação local não autoriza merge, deploy ou carga real.

Para teste manual, seguir `docs/question-administrative-import.md`. A funcionalidade
nega acesso por padrão: o operador precisa da sessão Google e de concessão externa
do UUID, destino lógico e chave de confirmação. Não se inferiu a conta do usuário
nem foram lidos segredos para habilitar permissões automaticamente.
