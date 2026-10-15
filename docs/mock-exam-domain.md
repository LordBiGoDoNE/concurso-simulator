# Núcleo de simulados — grupos 1 e 2

`mockexam.domain` é Java puro. `Composition` valida cargo, quantidades e assuntos e
seleciona sem reposição do pool elegível fornecido pela porta. Quantidades entre1–100
no total; insuficiência informa disponíveis por disciplina e não gera preenchimento.
`uniform-v1` é política de estudo, não distribuição ou pontuação oficial do edital.

`Attempt` protege posse, versão esperada, resposta/limpeza e conclusão irreversível.
Cada item captura questionId/revisionId/fingerprint e o VO imutável `QuestionContent`
do banco, mais ordem final e parâmetros vazios. A dependência é mockexam→questionbank;
o banco não conhece o módulo de simulados. Não são DTOs/JPA compartilhados.

Resposta é ID de alternativa, nunca letra/posição. Correção ocorre no núcleo do
backend após concluir:1por acerto,0por erro/em branco; percentual HALF_UP em duas casas.
Consulta em andamento usa `View`, sem resposta correta/resolução/proveniência.
`Snapshot` é privado para persistência; não deve ser serializado na web.

Visitante vence em24h pelo relógio do servidor; a validade da sessão é responsabilidade
do futuro adaptador. Conta não vence com logout. Ator incorreto e expirado são
indistinguíveis de ausente. Uma tentativa concluída não aceita novas respostas.
Estes grupos não criam endpoints/UI ou alteram login/consulta pública do banco.

`AttemptDomainTests` testa critérios, seleção, snapshot/ordens/respostas, correção,
posse/expiração/conflitos e projeção negativa. Não prova seleção SQL/concorrência,
cadeia HTTP, acessibilidade ou salvamento no navegador; cada fronteira terá seus testes.
