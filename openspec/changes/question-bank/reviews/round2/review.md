# Lote 3 + 4 + 5 — revisão independente e revalidação concluídas

**Estado: implementação/verificação e revisão de produto concluídas; nenhum merge aprovado.**
Inclui todas as tarefas 3.1–3.4, 4.1–4.3 e 5.1–5.3 e revisão documental do #49 atualizado.
Não implementa grupos 6/7, curadoria real, simulação, deployment ou gate cumulativo `[SPEC]`.
No conjunto, 16/44 tarefas implementadas/verificadas; somente 6/44 integradas à épica.

## Composição e criticidade

- Base comum: `0fb8fc2d11b33dd2edd8213454ddd46238909ce0` (`epic/question-bank`).
- #49 é documental, independente, sem integração presumida.
- Stack #52: épica ← #50 ← #51 ← #53 ← #59.
- Stack #56: épica ← #54 ← #55 ← #57 ← #58 ← #60.
- Trunk de ambas é `epic/question-bank`, nunca main/épica geral/outra spec.
- Classificação **crítica**: escrita editorial/versionamento/idempotência/transações,
  e fronteira pública de segurança/sessão/projeção. SOL xhigh e Astra medium sequenciais.
- Heads anteriores preservados; correções acrescentadas nos topos. Não houve rebase,
  troca manual de base, fechamento, merge GitHub, sync pós-merge ou deploy/preview.
- Não integrar prefixos sem os corretivos: conclusões sobre a combinação não corrigem
  retroativamente os heads antigos isolados. Aprovação do topo não aprova inferiores.

`review.json` registra SHAs/digests dos PRs e snapshots locais. Composição local de
verificação **não representa integração na épica**. O digest usa bytes de `git diff
--binary <base> <head>` capturados pelo coordenador; revisores sem shell não recalcularam
hashes nem reconferiram Git/CI ao vivo. Este artefato documental separado não altera
o código dos snapshots descritos e precisa de revisão/aprovação própria.

## Revisões executadas

Contextos independentes novos, sem conversa/defesas ou achados do outro modelo na
rodada inicial; decisões versionadas e ADRs fornecidos integralmente. Modelos/variantes
disponíveis e IDs efetivos conferidos. Revalidações retomaram somente o contexto do
respectivo revisor; não compartilharam relatórios entre modelos. Nenhum teste executado
pelos revisores, cujo perfil permite somente leitura.

1. SOL `openai/gpt-6.1-sol#xhigh`, sessão `ses_ecef7938affeLGiC6phNdWmI7g`:
   snapshot inicial `577df78190c846c426df67841053ffe930532331`, oito diffs completos e
   combinação. 3 bloqueadores + 2 recomendações. #49 sem achado documental adicional.
2. Tentativa Astra `ses_eceee07fbffe1NADEilKp3a9Kp` falhou por limite de uso:
   **não é revisão concluída**, nem ausência de achados. Não reutilizada como aprovação.
3. Após autorização explícita para continuar com Astra neste lote, contexto novo
   `ses_ece0feb87ffexxwRcVBSehJfEU`, `openai/gpt-6-astra#medium`, mesmo snapshot inicial:
   oito diffs completos e combinação. 2 bloqueadores + 1 recomendação.
4. SOL revalidou `7c2ffae95c48138f76c79a8cab7dbd0d1bee3e9f`: dez diffs completos,
   combinação e delta. 3 resolvidos, 1 bloqueador/1 recomendado parcialmente resolvidos.
5. SOL revalidou `ede2f7e3cf886761f6c6c7d5eda04296cbd64ab6`: conferiu base/head/digests
   dos oito PRs inalterados, reutilizou esse exame, leu delta integral de 162 linhas,
   corretivos atuais e contexto/interações. Sem bloqueador; 1 recomendação remanescente.
6. Em 2026-10-13, SOL revalidou `1359a55`: nove partes imutáveis conferidas, delta completo
   de 69 linhas e #59 (449 linhas); cinco achados próprios resolvidos. Astra revalidou
   esse mesmo snapshot lendo delta desde inicial (607 linhas) e #59/#60 completos:
   bloqueadores resolvidos, diagnóstico de origem duplicada/namespace ainda recomendado.
7. Correção `6f1117d` acrescentou códigos/chaves de origem e regressão pelo launcher.
   Ambos revalidaram o conjunto final `872c8018ca96895a1ddd8d125198a0b79de6dbd7`, digest
   `ee7f05f87a87dff4f6d9fc0262b902c208ba4dd76818fe6d06ba328c3fd3a701`: conferiram
   base/head/digests das nove partes imutáveis, leram delta completo de 68 linhas e
   #59 atualizado (484 linhas), código/contexto/interações. Astra e depois SOL,
   sequencialmente, **todos achados próprios resolvidos, nenhum novo/remanescente**.
8. Limite de isolamento: SOL leu incidentalmente atribuição da correção ao Astra e
   descrição da rejeição no próprio documento versionado. Não recebeu relatórios atuais
   nem usou a atribuição como prova; isso limita cegamento completo e fica declarado.
   Astra não recebeu os resultados atuais SOL. Nenhum revisor leu PR61 durante exame de produto.

## Achados, triagem e regressões

| Causa | Revisores / severidade inicial | Correção / situação |
| --- | --- | --- |
| Parser da UI removia índices/underscores e delimitadores incompletos | SOL-QB-01 blocking; ASTRA-02 blocking | #60 texto integral; SOL/Astra confirmaram no conjunto final. |
| Leitor impedia reclassificar mantendo origem/proveniência | SOL-QB-02 blocking | #59 separa origem/classificação; SOL confirmou, Astra conferiu interação final. |
| Comparações matemáticas confundidas com markup | SOL-QB-03 blocking | #59/#60 detector estrutural; SOL confirmou, Astra conferiu interação final. |
| HTML sup/sub aceito com perda silenciosa | SOL-QB-04 recommended; ASTRA-01 blocking | #59 conserva estrutura/aninhamento; SOL/Astra confirmaram no conjunto final. |
| Diagnóstico da CLI genérico/sem chave | SOL-QB-05 recommended; ASTRA-03 recommended | #59 cobre conversão/invariantes/origem; SOL/Astra confirmaram no conjunto final. |

Todos os achados foram confirmados/corrigidos/revalidados, sem refutação ou adiamento;
triagem de produto encerrada, **não aprovação do usuário**. Na inicial Astra confirmou
três causas comuns e classificou sup/sub como bloqueador; SOL identificou duas causas
adicionais distintas. Na revalidação Astra apontou o caso adicional de origem dentro
da causa de diagnóstico, corrigido depois de SOL considerar seus próprios achados encerrados.
Essa comparação deste lote não prova equivalência ou qualidade futura dos modelos.

Negativos antes das correções reproduziram `sup/sub` → `23/H2O/x1n`, quatro perdas de
texto no DOM e recusa/503 de desigualdades. Positivos/regressões protegem texto exato,
índices/expoentes/ênfase textual explícita, reclassificação preservando identidade e
ponteiro antigo, diagnóstico por item/exit1/stdout/stderr sem payload/credenciais.
Casos finais enunciado/alternativa/resolução vazios passam pelo mesmo `launch` usado
pelo main, antes da conexão; domínio continua dono das invariantes, sem duplicação.
Também passaram casos de origem duplicada/namespace/ID fora/não candidato/ausente,
com códigos fixos e chave sanitizada antes de inserir no mapa/abrir DB. O teste falhou
antes do fix exibindo mensagem genérica de namespace; não se afirma que todos os cinco
casos foram reproduzidos antes da correção, pois a primeira falha interrompeu o método.

## Verificações do coordenador e limites

- Clean build + **110 testes backend**, PostgreSQL18.6 isolado, sem skips/falhas.
- **73 frontend**, **33 Python**, **6 browser reais identidade/questões**,
  **6 browser estáticos/offline** e **3 OpenSpec estritos** passaram, sem skips.
- Browser real: 390/1280px, teclado/foco/rótulos/fórmulas/fontes, filtros após OIDC local,
  falha de transporte com aulas acessíveis, JSON/DOM/bundle sem dados corretivos.
- JAR sem fixtures; nenhuma aula/manifesto/CSV/asset foi alterado.
- Todos os checks dos dez heads de produto/processo atuais passaram na última consulta.
- Última alteração é CLI/origem/regressão/docs; clean build backend, frontend/Python,
  browser real e strict reexecutados após ela. HTML e suíte offline não mudaram desde
  a execução completa do mesmo dia. JAR sem fixtures e aulas intactas reconferidos.
- Uma tentativa inválida de compilar e executar o harness Gradle simultaneamente causou
  disputa dos classfiles. Foi descartada como evidência; clean build sequencial passou.
  Não repetir paralelismo entre build backend e harness que recompila o mesmo diretório.
- Fixtures não aprovam conteúdo real. P05-Q01 inválida permanece inventariada; pacote
  completo não pode tratá-la como válida. Meta de curadoria do grupo6 não foi preenchida.
- Sem Google real, leitor de tela real, ensaio operacional de volume ou garantia semântica
  de todo texto/recurso. Não relaxar regras/CI para acomodar esses limites.

## Cota, tokens e decisão

Observações reais mínimas antes/depois por execução e fase, privadas e fora do Git,
validadas/comparadas por `scripts/review_quota.py`. Sem geração concorrente do coordenador
durante revisões; isolamento externo da conta não confirmado. Tentativa falha/reset
separados; correções/coordenador não atribuídos ao revisor. Tokens reais por mensagens
obtidos da API local, sem somar raciocínio à saída ou converter em custo da assinatura.
Percentuais/diagnósticos privados não publicados; números apresentados somente ao usuário.

Pausa anterior por cota foi encerrada após renovação e novas medições reais. Comparações
iniciais/revalidações não atravessam reset; o comparador invalidou o último intervalo
SOL porque ambos os resets mudaram. Essa observação não atesta consumo zero nem delta válido.
Revisões de produto concluídas; registro #61 tem revisão documental própria de risco
padrão (evidência histórica, sem controle executável/código/requisitos alterados), não
certifica semântica adicional de produto nem o gate final. Sem aprovação explícita,
nenhum PR é integrado. Não modificar os dois arquivos exclusivos do gate cumulativo final.
