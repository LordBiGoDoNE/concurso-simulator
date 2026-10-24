# study-experience

## MODIFIED Requirements

### Requirement: Aulas completas na produção
A integração React SHALL oferecer as51 aulas pelo mapa conhecido, busca/filtro,
navegação e leitura completa com figuras/fórmulas/exercícios. A prática iniciada
na aula SHALL ter fluxo próprio, sem preencher ou abrir o compositor de simulados.
O acesso ao original SHALL continuar independente da API de questões.

#### Scenario: Ler teoria e praticar o assunto
- **WHEN** abre uma aula, confere cargo/assunto e escolhe Praticar
- **THEN** inicia prática própria de até cinco questões fixas exclusivas de PRATICA_AULA daquele assunto/cargo
- **AND** a correção aparece somente ao encerrar a prática, sem substituir um simulado

### Requirement: Navegação de produção preserva a realização e a identidade
A interface SHALL preservar independentemente simulado e prática ao navegar entre
início, simulados, aulas/prática e consulta. SHALL ocultar conteúdo durante verificação
de identidade e limpar ambos ao trocar conta/logout. MUST NOT guardar respostas no
navegador ou anunciar salvamento antes de confirmação.

#### Scenario: Alternar entre um simulado e uma prática
- **WHEN** inicia prática de aula tendo simulado em andamento e alterna entre eles
- **THEN** ambos mantêm escolhas/filas/comandos/estados próprios e nenhum é substituído

#### Scenario: Trocar de conta com resposta tardia
- **WHEN** muda de identidade enquanto uma das realizações está oculta e responde depois
- **THEN** nenhum contexto privado anterior reaparece em qualquer das duas realizações
