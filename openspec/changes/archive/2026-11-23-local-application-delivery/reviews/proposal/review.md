# Revisão documental da proposta local-application-delivery

**Concluída: LAD-SOL-001 corrigido na proposta, nenhum novo achado.**
SOL6.1xHigh (`openai/gpt-6.1-sol#xhigh`), contexto independente
`/root/local_delivery_proposal_sol`. Risco crítico pelos requisitos de segredos,
rede/cookies/CSRF, persistência/restauração e controles de entrega.

## Escopo e snapshots históricos

Base `3715219a02dfe77262ce0af4bcbb858fbc08fca7`.
Inicial `a288e84ef51b2b107bb0e61ffa5c682555692f7a`, digest
`d56cc96daf0746199c3c9723c63a9583e794db337a0d6cb5378df65ca8b536a8`.
Revalidação `7c668076dbaeee1eb6dda9e99a9dd8a4c1948f96`, digest
`c5a30f0c71bea991bb82f47e7dfee708ae2974f10db5bbe26e469a4dc56999d4`.
Ambos recalculados pelo revisor, excluindo somente spec-review.json/md.
Este registro é acrescentado após o source revalidado e não altera requisitos/produto.

## Achado e triagem

**LAD-SOL-001 blocking → fixed documental.** Dump com sessão válida, logout e
restore antes da expiração podia reativar cookie revogado: faltava política de
invalidação, em conflito com user-auth/spec.md:74–76. Inferência dos requisitos e
armazenamento existente, não falha dinâmica de implementação inexistente.

Design, requisito e tarefa3.3 agora exigem API parada, invalidação das sessões antes
de liberar destino, cookie antigo401 e histórico/retomada após nova autenticação.
Backup continua completo; igualdade de negócio exclui somente sessões invalidadas.
Reinício comum pode conservar sessões válidas. Revisor confirmou a correção e
compatibilidade com infraestrutura de sessões sem FK de contas/tentativas.

## Cobertura, evidências e limites

Examinou oito arquivos semânticos, AGENTS, perfil/ADR/checklist/roadmap e código
relacionado: configuração Spring,segurança/OIDC/CSRF/sessões,consumidores de API,
material/Compose/migrações/V5/formato2/CLIs/harness/workflows. Cobriu requisitos,
arquitetura,integração,segurança,persistência,testes previstos e manutenção.
Não leu resultados de outros revisores, arquivos spec-review ou verification;
revalidação usou próprio parecer. Não executou testes/build/scripts/rede/DB/escritas.
Coordenador:OpenSpec7/7 estrito e diffcheck limpo; validação intermediária detectou
posição de cenário, corrigida antes do source final. Nenhum teste de imagem/runtime
foi executado ou alegado. Cota5h/semanal e tokens indisponíveis em ambas as fases;
observações mínimas privadas mantidas separadamente,sem conversão financeira.

**0/16 tarefas implementadas/verificadas;0/16 integradas/aprovadas.** Proposta,
grupos e merges exigem decisões próprias; sem publicação/editorial/main/arquivamento.
A revisão desta proposta não é o gate cumulativo da implementação, que segue pending.
