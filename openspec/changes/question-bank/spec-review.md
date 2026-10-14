# Gate cumulativo question-bank — revisão concluída, merge não executado

## Escopo e cobertura

PR **#63**, `[SPEC] Banco de questões versionado, curadoria e consulta pública`,
`epic/question-bank` → `epic/application-reformulation`. **44/44 tarefas** da spec,
todos os grupos 1–7; risco **crítico** por elegibilidade/conteúdo, persistência,
transações/concorrência, segurança/sessões e proteção de campos corretivos.

Modelo **openai/gpt-6.1-sol#xhigh**, dispatch `gpt-6.1-sol`/`xhigh`, sessão nova
`/root/spec_reviewer_sol`. Exceção temporária explícita do usuário em 2026-10-14:
usar SOL no Codex e suspender Astra nesta validação. Perfil/AGENTS/procedimento
atualizados; não restaurar nem ampliar a exceção automaticamente. Sem telemetria
independente do provider. Revalidação usa o mesmo contexto independente da implementação.

| Snapshot | Head | Digest canônico |
| --- | --- | --- |
| Inicial | `07c73e2ed772415d1a90e38f64c9c0e04f3619e8` | `4cf769cf0c3cd97ba62acb8dbd6979f24e42e5b41f81feb8001a8409df5dc537` |
| Revalidado | `a871a21aac6024e20da75c2c48c2a8361e1146b1` | `64759630b855a75feaf10e5c93cd3c8d80b2a2d5a3dc646d1fd169c54dab4659` |

Base em ambos: `f72fcc5d9c92c326ad789e5cf72eef54665913cf`. Revisor recalculou os
digests em memória e confrontou os **163 paths** alterados com os blobs do head.
Inicial: **21:49:12–21:59:05 UTC** (9min53s), análise técnica até21:56:47 e fase
documental posterior21:57:02–21:59:05. Revalidação: **22:06:13–22:07:28 UTC** (1min15s).

Examinou requisitos, domínio/UseCases/portas/DTOs e ADR; V4/JPA/FKs/triggers/travas/
rollback/idempotência/ABA; extrator/canonização/importação/editorial/CLI; segurança
stateless/bypass compartilhado de sessão/readiness/OIDC/CSRF; HTTP/OpenAPI/projeções;
UI/filtros/paginação/foco/timeouts/retornos tardios; testes/ArchUnit/fixtures/build/CI
e preservação das aulas. Não exigiu simulados/parametrização/deployment antecipados.

Leu **250 enunciados, alternativas, respostas internas, resoluções e notas**. Não
identificou erro concreto de gabarito, dupla resposta correta ou dependência indevida.
Conferiu 104 hashes de fontes, 250 fingerprints recalculados, UUIDv5 de alternativas,
intervenções/manifesto/elegibilidade e relatório sanitizado: nenhuma divergência.
São 25 lotes, 50 identidades por disciplina,51 assuntos; inventário510 preservado,
P05-Q01 inválida/não selecionada. Zero recursos de imagem nos250 selecionados.
JAR existente bate no hash da evidência e não contém fixtures/pacotes editoriais
inspecionados; bundle sem os marcadores privados/corretivos pesquisados.

## Achados e triagem

**Sem achados pendentes; triagem encerrada.** Não se inventou revisão Astra.

| ID | Severidade | Causa e correção | Situação |
| --- | --- | --- | --- |
| SOL-SPEC-QB-01 | Opcional | READMEbackend:7 ainda dizia sem endpoints/persistência/conteúdo aprovado. 84b12b8 atualiza estado, links e limite da evidência local. | **fixed**, confirmado em a871a21 |
| SOL-SPEC-QB-02, antes H1 | Recomendada | CLI herdava stdoutCP1252 do Windows e não produzia JSON. 84b12b8 fixa UTF8/LF na entradaCLI e testa subprocesso com CP1252 forçado. | **fixed**, confirmado em a871a21 |

H1 era hipótese não executada no parecer inicial. Probe do coordenador confirmou
UnicodeEncodeError/exit1/zeroJSON sem variáveis de encoding. O revisor classificou
como recomendada: falha operacional com rejeição segura, sem corrupção persistida ou
aprovação indevida demonstradas. Depois: exit0/JSONUTF8estrito/509candidatos510inventário.
Regressão exige bytesUTF8 e Unicode preservado sob PYTHONUTF8=0/PYTHONIOENCODING=cp1252;
sete testes Java reais de importação no Windows/PG também passaram sem essas variáveis.
Regra positiva: a CLI cumpre seu protocolo de bytes independentemente da locale.
A proteção não substitui curadoria pedagógica ou outras fronteiras da importação.

## Evidências e limitações

Execuções atribuídas ao **coordenador/CI**, não ao revisor:

- **50 Python PASS**, zero falhas/erros/skips, containerLinux e repo somente leitura.
- **7 QuestionLocalImportTests PASS** nativos Windows/PG18.6, rerun-tasks e sem
  variáveis de encoding; execução direcionada, não clean build local novo.
- **112 backend PASS/zero skips** no clean build remoto do head a871a21,
  run[38645196866](https://github.com/LordBiGoDoNE/concurso-simulator/actions/runs/38645196866),
  contagem das linhas PASSED conferida. Frontend/identity-browser/study-material SUCCESS.
- Quatro changesOpenSpec estritas PASS: três versionadas e um rascunho local da etapa4
  fora deste PR, não analisado/aprovado pelo revisor. Não houve código da etapa4.
- Registros operacionais anteriores:250 detalhes/3páginas/102combinaçõesAND,
  browser390/1280 e retirada/reaplicação em cópia isolada, sem somar fixtures ao acervo.
  Esses ensaios não foram repetidos pelo revisor cumulativo.

Primeira tentativa de testeLinux falhou porque Docker estava parado. Desktop/engine
29.8 recuperados; suíte executada novamente PASS. Container do acervo local reiniciado,
volume preservado. O primeiro commit de registro falhou por keyboxd/GPG indisponível;
daemons/Kleopatra recuperados, assinatura mantida e commit posterior concluído.

**Revisor não executou testes/build/aplicação/extrator/HTTP/DB/rede/escritas/Graphify**;
fez leituras/hashes em memória. Restrição seguida como orientação do perfil, sem
garantia fictícia de sandbox tecnicamente incapaz de escrever. Nenhum outro agente
ou modelo foi disparado por ele. Fontes externas não abertas nesta revisão cumulativa:
sem certificação de disponibilidade/vigência normativa atual, leitura integral das
obras, revisão humana pedagógica ou cobertura integral do edital. Sem benchmark de
triggers, leitor de tela ou demonstração de todos os interleavings concorrentes.

Isolamento: primeiro formou parecer técnico sem reviews/,verification.md ou os dois
arquivos deste gate. Padrões versionados necessários expuseram incidentalmente IDs/
causas históricas. Depois examinou 38 arquivos históricos estritamente como documentos,
conferindo snapshots/triagem/autoria/totais de tokens já registrados; não os usou como
certificação do código nem consultou GitHub ao vivo. Não leu segredos/logs/cota privados.

Cota **antes/depois** inicial e revalidação:5h/weekly **unavailable**, sem delta
atribuível. Observações mínimas privadas validadas/comparadas por review_quota.py;
isolar a conta não foi verificado. Coordenador sem geração durante revisões; correção
ficou em intervalo próprio. Tokens entrada nova/cache read-write/saída/raciocínio
indisponíveis em ambas as fases e coordenação; não estimados nem convertidos em cobrança.
Tentativa Astra/OpenCode falhou com401 antes da análise: sem parecer/consumo medido,
não revisão com zero findings. Nenhuma comparação nova entre modelos.

## Revalidação

O revisor leu integralmente 84b12b8/a871a21: **dez paths** alterados, **153 preservados**.
Conferiu o novo conjunto/digest e suas interações; não apenas a correção isolada.
Conteúdo/editorial/Java/schema/HTTP/UI/workflows permanecem idênticos. Os dois achados
foram confirmados fixed, sem blocking/recommended/optional novos ou remanescentes.

Perfil versionado atualSHA256:
`1a99cd5d094895fa20865ea86089955657f57eb2841b8e30a23e8750d167c723`.
PerfilCodex inicial preservado:
`378c0cd216842c048fb511b2043f2cbb168d84830e78db0b53ce38b5c1df863a`.

Somente spec-review.json/md registram agora o parecer recebido; são as duas exclusões
previstas do digest. Não se afirma que o revisor examinou o commit de sua gravação.
Conferir o gate/checks do head publicado antes de solicitar aprovação do **#63**.
Seu merge exige decisão própria; aprovar #62 não o integra. Não houve integração na
épica geral/main, arquivamento, publicação ou implementação da etapa4. A proposta
revisável está na issue#64, dependente da épica geral atualizada e aprovação própria.
