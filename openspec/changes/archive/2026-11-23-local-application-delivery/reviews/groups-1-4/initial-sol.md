# Revisão inicial independente — grupos1–4

Revisor `/root/local_delivery_batch_sol`, contexto novo, somente leitura,
modelo solicitado/configurado `openai/gpt-6.1-sol#xhigh`. Risco crítico mantido.
Heads/digests por PR125–128 e combinação constam em initial-sol.json.

**Resultado:1 bloqueador e1 recomendado, ambos confirmados na triagem.**

- **B1, #126/#127:** owned() buscava somente recursos com rótulo de projeto.
  Volume homônimo sem esse rótulo escapava da conferência e poderia ser adotado
  pelo Compose. Viola isolamento/recusa de adoção do design2 e runtime spec.
  Correção exige conferir nomes exatos e ambos os rótulos antes de mutação.
- **R1, #125/#126:** DOCKER_CONTEXT prevalece sobre DOCKER_HOST no CLI, mas a
  validação seguia ordem diferente; build só verificava Linux. Viola escopo local.
  Correção exige seleção única validada e usada explicitamente em todas as chamadas.

O revisor examinou diffs completos de implementação das quatro camadas e combinação,
consumidores de configuração, segurança/sessão Google, prontidão, Gradle/material e
Playwright. Não confirmou outros achados em artefatos, invalidação de sessões,
proxy ou CI. Não leu pareceres anteriores, verification.md, spec-review.* nem
arquivos privados. Os três arquivos de evidência foram excluídos da análise
semântica inicial, sem exclusão indevida do digest.

Não executou testes/build/Docker/aplicação/rede/DB e não atesta CI remoto.
Correções e revalidação do novo conjunto permanecem necessárias; nenhum merge
aprovado. Item4.4/gate cumulativo continua separado e pendente.

Cota5h/semanal indisponível antes/depois; observações mínimas privadas validadas e
comparadas por scripts/review_quota.py, sem delta atribuível. Isolamento não
declarado; houve interação do coordenador com o usuário durante o intervalo.
Tokens não fornecidos pelo harness; não são zero nem convertidos em assinatura.
