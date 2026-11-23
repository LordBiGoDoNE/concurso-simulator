# Revisão inicial independente do lote 1–4

Revisor: `/root/spec_reviewer_batch_161`, contexto novo (`fork_turns=none`), seleção explícita `gpt-6.1-sol`, esforço `xhigh`, correspondente ao perfil aprovado `openai/gpt-6.1-sol#xhigh`.

Base: `16ca8aef382d64e7652b280b413122c357775ead`.
Head: `020fea0a8f1080a1d7c730fa0711e40e80a4d4a5`.
Digest canônico: `33e6069bbb608f2e9cda4ff9bc12db9d6309d2501cc9816b32eac17a322a00c9`.
47 arquivos no diff completo e combinação resultante. Risco crítico mantido. Sem PR de implementação publicado.

## Resultado reportado pelo revisor

“Nenhum bloqueador ou defeito funcional verificável identificado.” Melhoria documental opcional T161-O1 em `docs/lesson-practice.md:41`: o parágrafo descrevia genericamente prática formato 3, embora o requisito e o código usem formato 4 sem composição nas novas práticas e preservem formato 3 histórico. Sugestão de distinguir explicitamente os dois formatos; impacto exclusivamente documental, não impeditivo à integração.

## Cobertura

- Requisitos: seleção por assunto sem cargo; pool aprovado exclusivo/fixo; 1–5 bases distintas; zero sem persistência parcial.
- Arquitetura: núcleo Java puro, invariantes no domínio, portas de aplicação, JPA/transações na infraestrutura, DTOs distintos.
- Integração: OpenAPI/backend/frontend; simulados seguem exigindo cargo; painel, histórico e retorno por assunto.
- Segurança: Spring Security permanece responsável pela segurança técnica; posse/expiração/modalidade precedem acesso e replay; sem correção antecipada.
- Persistência: V14 aditiva; condições anteriores preservadas; composição nula no formato 4; vínculo diferido captura/itens; codec/JPA históricos.
- Testes: inspeção de regressões de domínio/aplicação, HTTP, PostgreSQL, concorrência, legado/recuperação, React, navegador e ArchUnit.
- Manutenção: inventários V10–V14, fixtures isoladas e ausência de alterações editoriais.

## Independência e limites

A análise semântica inicial terminou antes de ler verification.md, local-delivery.md e local-verification.json. A conferência documental posterior não trouxe novo defeito de produto. Consultas locais de leitura conferiram os 37 hashes e o manifesto agregado, a árvore editorial idêntica, o hash do jar e a exclusão de TopicPracticeBrowserFixture, ancestralidade de base/implementação e existência do commit documental histórico. Isso demonstra coerência, não execução de testes.

O revisor não executou Gradle/build/aplicação/PG/Vitest/Playwright/Python unittest/OpenSpec, não consultou rede/CI/PR/runtime, não leu logs privados e não escreveu. As contagens de testes/aceite são registros do coordenador, não certificação independente. O conflito de navegador é injetado; concorrência real é coberta pelos testes PG inspecionados.

A ferramenta não retornou atestado adicional de ID de modelo efetivo nem telemetria de tokens; a seleção explícita está registrada, sem estimar consumo. Esta revisão de lote não é aprovação de merge nem gate cumulativo final.
