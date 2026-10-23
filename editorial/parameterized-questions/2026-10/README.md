# Conteúdo parametrizado — três famílias v1

**111/111 variantes verificadas e três versões v1 aprovadas editorialmente na cópia
local isolada**, após aprovação explícita do usuário da [seleção exata](approval-selection.json).
Aprovação editorial não integra PRs nem substitui revisão independente do lote.

| Família | Base/revisão real | Tuplas | Estado observado na cópia |
| --- | --- | --- | --- |
| Regra de três direta | M03-Q01 / `632ee809-9a3b-4e97-8144-9539f6b951bb` | 64 | Base e template aprovados |
| Porcentagem | M04-Q01 / `f4cfd6d7-96a5-4268-b257-638f4d6ae494` | 20 | Base e template aprovados |
| Juros simples | M04-Q05 / `80f32d25-9f88-4616-8ba0-8afafbeea9ab` | 27 | Base e template aprovados |

## Evidência e alcance

[content-approval.md](content-approval.md) apresenta conteúdo, IDs e digests exatos.
[eligibility-after.json](eligibility-after.json) registra os três ponteiros elegíveis
observados, seis decisões de template, duas decisões da nova base e replay sem alteração.
Os arquivos reais de decisões estão em `decisions/`, com responsável, timestamp,
checklist e digests. A intenção em `approval-selection.json` foi preservada com seu
estado original de solicitação; não deve ser reescrita como se já tivesse sido um ato.

O banco original permanece com 250 revisões elegíveis. A cópia aprovada tem 251:
uma nova identidade editorial M04-Q05, cuja revisão foi preparada/importada/reaplicada
em DRAFT antes da autorização. Esse histórico está em `base-preparation.json` e no
commit `86ba135`. O pacote tem inventário completo e somente M04-Q05 selecionada.
Os IDs vieram da importação real; não são fixtures nem placeholders.

`bases.json` contém a observação posterior às decisões, com revisões APPROVED e
ponteiros iguais. É fotografia do banco, não promessa de elegibilidade futura.
Não se afirma que o JAR foi implantado ou que esse banco configura a aplicação pública.

## Geração, autoria e revisão

`direct-proportion.json`, `percentage.json` e `simple-interest.json` são entradas
exatas do launcher, expectedLatest 0. Os UUIDs de template foram planejados antes do
registro; as bases são reais. `*-preview.json`, `variants.md`, `audit.json`, pacote
de base e observações são **arquivos gerados** e contêm material editorial privado
dos contratos HTTP. Não são endpoints, bundle ou recursos do pacote estático.

As [111 variantes](variants.md) mostram enunciados, alternativas, correta, resolução,
cálculo independente e digest de cada saída. O auditor Python usa somas de frações
por unidade/centésimo/juros mensais e recompõe digests binários, sem chamar gerador
ou oráculo Java. Ele não concede aprovação: `NOT_GRANTED_BY_AUDIT` remete às decisões
reais separadas. Testes/CI/importação não aprovam conteúdo implicitamente.

Responsável: Codex, revisão editorial assistida por IA, sem revisão humana independente.
As variações são adaptações autorais; IMPA/OBMEP é apoio temático, sem autoria/validação
oficial alegada. A proveniência aponta ao código exato dos textos/resoluções e seu
SHA-256. Bases preservam autoria e hashes dos HTML originais do projeto [Gaby].

Regra de três explicita preço unitário constante; distratores usam custo inicial,
soma de quantidades ou produto sem divisão. Porcentagem pede apenas a parte em reais;
distratores omitem divisão por cem ou respondem aumento/restante. Juros simples pede
juros, taxa e prazo em meses; distratores respondem montante, só um mês ou omitem cem.
Todas as opções são positivas/distintas; resoluções têm etapas, substituições e unidades.
Texto puro, sem depender de figura, cor ou áudio. Ordem editorial A correta não é
a ordem da tentativa, que atribui UUIDs opacos e embaralha alternativas no servidor.

## Reprodução e retirada

```text
./backend/gradlew -p backend parameterizationLocal -PtemplateAction=preview -PtemplateRoot=<raiz-absoluta> -PtemplateFile=editorial/parameterized-questions/2026-10/simple-interest.json
python scripts/audit_parameterized_content.py --directory editorial/parameterized-questions/2026-10 --output editorial/parameterized-questions/2026-10
```

Repetir a prévia para os outros dois arquivos. Windows: `backend\gradlew.bat`.
Prévia não usa banco nem grava. O auditor padrão rejeita famílias incompletas/bases
pendentes; flags de preparação mantêm essas lacunas explícitas, sem liberar versões.
Decisões locais exigem destino/seleção/digest e estados esperados; não automatizar
novas aprovações a partir destes arquivos.

[withdrawal-probe.json](withdrawal-probe.json) comprova captura real de três famílias/
111 tuplas, retirada das três somente em segunda cópia e zero elegíveis após reimportar
e repetir as aprovações antigas. As decisões `probe-*` pertencem exclusivamente ao
clone de ensaio. A cópia aprovada permaneceu com três elegíveis/seis decisões; o banco
original manteve 250. Retirada não reativa versões históricas nem muda instâncias.

Variantes acrescentam **zero identidades** à meta e não duplicam uma base por simulado;
a nova base acrescenta uma identidade editorial separada. Os testes do núcleo de
tentativas preservam seleção por identidade sem reposição. HTML/URLs originais M03/M04
não foram editados; hashes conferidos. Montagem offline: 9 módulos/90 respostas,
108 páginas/1.757 referências válidas, sem API/login e sem cópia das prévias editoriais.

Grupos 3/4 implementados/verificados; revisão independente SOL 6.1 xhigh ainda pendente.
PRs #94/#95/#97 em rascunho, sem merge, deploy, arquivamento ou gate cumulativo presumidos.
