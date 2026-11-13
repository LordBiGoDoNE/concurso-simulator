# Gate cumulativo — new-student-experience

**Revisão cumulativa concluída, sem achado confirmado.** PR
[[SPEC] #163](https://github.com/LordBiGoDoNE/concurso-simulator/pull/163),
epic/new-student-experience → main. A análise não autoriza merge.

- Base real: f61d57d5d55d6d61609815a2faa4b415ed8dca4b.
- Head revisado: dd2086e5cbd0b93b18f54c4641252b1041db5df0.
- Digest canônico: 524253d4ef586a755689f4b52e9310c045b0a7cea562b6b1a71351bfc7a001d5.
- Modelo selecionado no disparo e confirmado no perfil: openai/gpt-6.1-sol#xhigh
  (gpt-6.1-sol, esforço xhigh), disponível no catálogo do harness; sem substituição.
- Contexto novo: /root/spec_final_reviewer, separado da revisão de lote.
- Parecer recebido e registrado pelo coordenador em 2026-11-09. Não houve
  reutilização do resultado anterior como conclusão deste gate.

## Escopo e cobertura

Risco conservador crítico pela convivência entre navegação pública, identidade,
filas, comandos idempotentes e respostas privadas. O revisor conferiu os commits,
merge-base e o digest completo, excluindo somente spec-review.json/md.

| Área | Análise independente realizada |
| --- | --- |
| requirements | Proposta, design, requisitos e grupos 1–8 confrontados com entrada por cargo, exploração livre, filtros, rotas restauráveis, disponibilidade, início explícito, painel recolhível, índice e leitura nativa; sem divergência funcional confirmada. |
| architecture | Navegação pública separada dos executores privados; reutilização de useAttemptRun e transportes existentes, sem segunda máquina de gravação. ADR 0001, checklist HTTP e backend relacionado consultados; sem alteração Java ou transferência de responsabilidades de segurança ao frontend. |
| integration | App, biblioteca, executor inline/standalone e histórico examinados em conjunto. Instância privada preservada ao navegar/recolher; incompatibilidade de assunto/cargo controla visibilidade. Criação pendente, retry do mesmo comando e confirmação de substituição permanecem no executor. |
| security | Parâmetros permitidos, canonicalização, ausência de IDs privados nas rotas, chave por identidade e descarte de respostas obsoletas conferidos. Disponibilidade valida quatro campos e não cria realização. Leitor usa vocabulário permitido, sem HTML bruto; âncoras preservam rota e links externos isolam nova aba. |
| persistence | Sem migrações, schema, repositories, elegibilidade ou importação. Continuidade dos contratos existentes de posse, CSRF, idempotência e separação prática/simulado conferida. Compilador produz dados públicos de autoria, fora da persistência. |
| tests | Leitura dos testes novos/alterados: contratos inválidos, zero/erro/retry, respostas tardias, histórico, navegação, filas, recolhimento, isolamento, conteúdo e layout. Descoberta Python do workflow inclui a nova regressão do compilador. O revisor não executou testes nem presumiu seu sucesso. |
| maintenance | Compilador, manifesto, renderer, documentação de uso, protótipo histórico e processo examinados. Sem alteração de dependências, workflows ou regras de deploy. Procedência dos dados e regeneração documentadas. |

### Conferência dos dados gerados pelo revisor

- 51 documentos, correspondência com catálogo/títulos e hashes LF dos documentos
  e das 115 fontes registradas conferidos, sem divergências.
- Árvores/referências internas percorridas: sem IDs duplicados, referências
  quebradas ou elementos ativos/legados proibidos. 340 links: 235 referências
  internas e 105 URLs HTTPS sem credenciais; disponibilidade externa não consultada.
- 50 SVGs comparados às fontes: geometria/texto didático preservados, exceto
  remoções previstas de marcas de edição.
- 2433 fragmentos textuais comparados às fontes. As 118 diferenças identificadas
  pertencem às oito aulas que reutilizam enriquecimento de Matemática; sem perdas
  inesperadas nas demais. 192 trechos do enriquecimento, oito árvores MathML e
  cinco fórmulas preservadas de M04 conferidos, sem divergências inesperadas.

Foram leituras e cálculos sobre dados inertes com biblioteca padrão. O revisor
não executou compilador ou código do produto.

## Achados e triagem

findings: []. Nenhum item blocking, recommended ou optional confirmado nesta
revisão cumulativa. Triagem encerrada sem alteração adicional de código ou requisito.
Não se inferiu essa conclusão do parecer anterior do lote.

## Evidências e limitações

- O revisor não executou testes, build, aplicação, navegador, rede, GitHub ou banco.
  Base/head reais do PR, checks e rascunho foram conferidos pelo coordenador.
- O revisor não leu handoff, verification.md, reviews/ ou spec-review.json/md.
  Seus bytes integram a identidade quando aplicável, mas não há certificação
  semântica independente integral desses documentos de evidência. O coordenador
  conferiu os registros de sua execução com os respectivos resultados.
- Tarefas e documentos de arquitetura/processo mencionam revisões anteriores.
  O revisor declarou exposição incidental, tratada como histórico sem fundamentar
  este parecer.
- Conferência de conteúdo demonstra conversão/preservação; não é inspeção visual
  real, reauditoria editorial/jurídica ou verificação de disponibilidade dos links.
- O aceite visual do usuário foi de leitura/texto, num preview sem API. As
  regressões de prática/identidade foram automatizadas com fixtures sintéticas;
  não constituem teste manual de acervo ou Google real.
- O harness não forneceu UUID adicional do provedor, telemetria independente do
  modelo, tokens ou tempo total da sessão; não foram estimados.

O coordenador conferiu 12 checks aprovados no PR #163 para o head revisado dd2086e:
backend, frontend, identity-browser e local-delivery nas execuções push/PR,
study-material, static-pages e spec-review-tests. Os jobs incluíram testes/build e
o pacote Docker; os resultados pertencem ao GitHub Actions, não ao revisor.
Seleção --check, OpenSpec 1.14.1 strict e diff --check também passaram na preparação.
Logs/checks reais ficam nos registros locais ignorados; não são copiados cookies,
dumps ou relatórios privados para a evidência pública.

## Revalidação

Apenas este arquivo e spec-review.json são atualizados depois da análise. O digest
permanece o mesmo; o head revisado deve continuar ancestral do head final. O
coordenador executa validate_report com base/head/número reais e lê a evidência do
commit exato, pois spec-review-tests em main não certifica o gate semântico.

Qualquer mudança relevante fora desses dois arquivos, inclusive de base, invalida
esta evidência e exige revalidação. Acompanhar o CI do commit de evidência final.
Merge em main permanece dependente de autorização específica; sem deploy ou arquivamento.
