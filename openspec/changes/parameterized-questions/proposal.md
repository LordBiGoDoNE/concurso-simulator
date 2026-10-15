# Proposal

**Em proposta, sem autorização de implementação.** Etapa 5 do roadmap. O usuário
escolheu três famílias em 2026-10-15: regra de três direta, porcentagem e juros simples.
Esse recorte não aprova automaticamente o restante desta proposta, conteúdo ou merges.
Pré-requisito integrado: simulados e tentativas, #84, `0d0fc48` na épica geral.

## Why

O aluno já pode realizar e retomar simulados, mas recebe apenas versões fixas das
questões. Variações numéricas controladas permitem praticar o mesmo raciocínio com
outros valores, mantendo respostas verificáveis e explicações específicas de cada caso.

## What Changes

- Entregar três famílias autorais: regra de três direta (M03), porcentagem de uma
  quantidade e juros simples com taxa mensal/prazo em meses (M04), para os dois cargos
  atuais de Limeira 01/2026. Domínios finitos propostos: 64, 20 e 27 combinações.
- Versionar definição, domínio, resolução/distratores e vínculo à revisão-base. Validar
  todas as 111 combinações antes de habilitar uma versão, sem confundir teste com
  aprovação editorial explícita do conjunto exato.
- Oferecer opção de usar variações na composição existente, desligada por padrão.
  Sortear parâmetros no backend somente para bases com template elegível; preservar
  as demais questões fixas e não repetir a mesma identidade-base na tentativa.
- Capturar conteúdo final, alternativa correta privada, resolução, parâmetros,
  versão/digests e ordens por tentativa. Retry, retomada e correção não regeneram itens.
- Preservar posse, visitante temporário, histórico da conta e ausência de gabarito
  antecipado; parâmetros/metadados internos não entram nos contratos públicos.
- Fora: IA em tempo real, linguagem de fórmulas executável, parâmetros enviados pelo
  aluno, outros assuntos/famílias, dificuldade adaptativa, revisão por conceito,
  sincronização offline, novos cargos, editor web e deployment.

## Capabilities

### New Capabilities

- `parameterized-questions`: famílias versionadas/validadas/editorialmente elegíveis,
  geração numérica no servidor e preservação da instância exata nos simulados.

### Modified Capabilities

Não há specs consolidadas em `openspec/specs/`. A nova capability complementa os
contratos de `question-bank` e `mock-exams`, nas changes ainda não arquivadas, sem
reescrever sua evidência histórica. A opção de composição será uma extensão compatível;
consulta pública de questões, login e histórico mantêm suas responsabilidades.

## Impact

Módulo funcional `parameterization` em Java puro com adaptadores JPA/local; integração
privada com o banco e criação de tentativas; Flyway aditivo após V5 e atualização
controlada do formato interno de snapshot. Contrato OpenAPI de criação e compositor
React, testes PostgreSQL/segurança/browser e verificação editorial exaustiva.

Branch de integração `epic/parameterized-questions`, criada de
`epic/application-reformulation` em `0d0fc4848f55fc93c736aa76dcfbb73a7269eb39`.
Proposta em PR próprio para essa branch, sem código de produto. Tarefas dependentes
usarão Stacks v0.2.1 após aprovação da proposta e escolha de grupos completos. Revisões
e gate cumulativo SOL 6.1 xhigh; `[SPEC]` final para a épica geral, nunca direto a main.
