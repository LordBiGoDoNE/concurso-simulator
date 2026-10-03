"""Single editorial source for lessons and question resolutions, excluding M04."""

# title, explanation, illustrated route, linear formula, warning
LESSONS = {
1: [
("Números e sinais, sem decorar primeiro", "Um número pode contar objetos, indicar uma dívida ou representar parte de um inteiro. Na soma, pense no saldo; na multiplicação, separe sinal e tamanho.", "Saldo −8 e recebimento +3 → −8 + 3 = −5|Saldo −8 e despesa de 3 → −8 − 3 = −11|Produto de sinais iguais → (−4) × (−2) = +8|Produto de sinais diferentes → (−4) × 2 = −8", "multiplicação e divisão: sinais iguais → positivo; diferentes → negativo", "A regra de sinais da multiplicação não serve para a soma. −4 − 2 é −6."),
("Frações e decimais: partes do mesmo tamanho", "Em 2/3, dividimos o inteiro em três partes iguais e pegamos duas. Antes de somar frações, as partes precisam ter o mesmo tamanho.", "Divida cada terço em duas partes → 2/3 = 4/6|Some partes iguais → 4/6 + 1/6 = 5/6|Multiplique → (2/3) × (1/4) = 2/12 = 1/6|Divida invertendo só a segunda → (2/3) ÷ (1/4) = (2/3) × (4/1) = 8/3", "decimal finito = número sem vírgula ÷ potência de 10 correspondente às casas", "0,25 tem duas casas: 25/100 = 1/4. Para simplificar, divida cima e baixo pelo mesmo número não nulo; não some os denominadores."),
("A ordem das contas e a conferência", "Parênteses mandam resolver um trecho antes. Potência é multiplicação repetida; raiz quadrada procura o número não negativo cujo quadrado é o valor indicado.", "20 − (6 + 4) ÷ 2 → resolva 6 + 4 = 10|20 − 10 ÷ 2 → faça 10 ÷ 2 = 5|20 − 5 → resultado 15|2³ = 2 × 2 × 2 = 8; √81 = 9, pois 9 × 9 = 81", "agrupamentos → potências/raízes → multiplicações/divisões → adições/subtrações", "No mesmo nível, siga da esquerda para a direita. Estime: 49,8 × 2,1 deve ficar perto de 50 × 2 = 100, não de 1.000.")],
2: [
("Múltiplo é repetir; divisor é repartir", "Múltiplos aparecem na tabuada. Divisores repartem uma quantidade sem sobra. O MMC procura o primeiro múltiplo positivo comum; o MDC, o maior divisor comum.", "Múltiplos de 4 → 4, 8, 12, 16, 20, 24…|Múltiplos de 6 → 6, 12, 18, 24…|Primeiro positivo comum → MMC(4,6) = 12|Divisores de 12 → 1, 2, 3, 4, 6, 12", "MMC = menor múltiplo comum positivo; MDC = maior divisor comum", "Aqui trabalhamos com inteiros positivos. O zero é múltiplo, mas não é o MMC positivo procurado."),
("Fatorar: desmontar em multiplicações", "Primos têm exatamente dois divisores positivos: 1 e o próprio número. Fatorar é escrever um número como produto desses pequenos blocos.", "12 ÷ 2 = 6 → 6 ÷ 2 = 3 → 3 ÷ 3 = 1|12 = 2 × 2 × 3 = 2² × 3; 18 = 2 × 3²|MMC: todos os primos com maior expoente → 2² × 3² = 36|MDC: só comuns com menor expoente → 2 × 3 = 6", "expoente conta repetições: 2³ = 2 × 2 × 2", "Coprimos têm MDC 1 e não precisam ser primos. Para coprimos positivos, o MMC é o produto."),
("Escolher MMC ou MDC pela situação", "Ciclos que começam juntos e se repetem pedem reencontro: MMC. Cortes iguais do maior tamanho, sem sobras, pedem MDC.", "Evento a cada 6 min → 6, 12, 18, 24…|Evento a cada 8 min → 8, 16, 24…; reencontro em 24 min|Fitas de 24 e 36 cm → maior corte comum de 12 cm|Confira os cortes → 24 ÷ 12 = 2 peças; 36 ÷ 12 = 3 peças", "reencontro = MMC dos intervalos; maior corte comum = MDC dos comprimentos", "Para somar 1/6 + 1/8, use MMC(6,8) = 24 como denominador mínimo. Não some intervalos para achar reencontros.")],
3: [
("Razão: comparar mantendo a ordem", "Uma razão é uma comparação por divisão; uma proporção é a igualdade entre duas razões. Confira as unidades antes de comparar.", "15 para 20 → 15:20|Divida os dois por 5 → 3:4|3 cadernos por R$ 24 → cada um custa 24 ÷ 3 = R$ 8|5 ao mesmo preço → 5 × 8 = R$ 40", "a/b = c/d → a × d = b × c, com b e d não nulos", "3:4 não é 4:3. Escala 1:100 significa 1 unidade no desenho para 100 reais, na mesma unidade."),
("Direta ou inversa: imagine dobrar", "Ao mesmo preço unitário, dobrar a quantidade dobra o preço: relação direta. No mesmo serviço, dobrar pessoas igualmente produtivas reduz o tempo à metade: inversa.", "6 pessoas × 10 dias → 60 pessoas-dia|12 pessoas para o mesmo serviço → 60 ÷ 12 = 5 dias|Receita para 4 pessoas: 300 g → por pessoa 300 ÷ 4 = 75 g|Para 10 pessoas → 75 × 10 = 750 g", "direta: razão constante; inversa: produto constante", "Crescer ou diminuir não basta para provar proporcionalidade. O enunciado precisa permitir preço, ritmo ou produtividade constantes."),
("Mais grandezas: uma mudança por vez", "Na regra de três composta, explique separadamente o efeito de cada mudança. Só depois combine os fatores.", "2 máquinas fazem 100 peças em 5 h|4 máquinas iguais em 5 h → dobram: 200 peças|4 máquinas em 10 h → dobram outra vez: 400 peças|Conta conjunta → 100 × (4 ÷ 2) × (10 ÷ 5) = 400", "produção = produção inicial × fator de máquinas × fator de tempo", "Se pedir duração, mais máquinas podem diminuir o tempo. Verifique se a resposta faz sentido antes de finalizar.")],
5: [
("Média simples: repartir o total igualmente", "A média é quanto cada posição receberia se repartíssemos a soma igualmente. Não precisa ser um valor já existente na lista.", "4, 6 e 8 → três valores|Some → 4 + 6 + 8 = 18|Divida pela quantidade → 18 ÷ 3 = 6|Confira → 6 + 6 + 6 mantém o total 18", "média = soma dos valores ÷ quantidade de valores", "A média de 10 e 20 é 15, mesmo sem 15 na lista. Se todos aumentarem 3, a média também aumenta 3."),
("Peso: uma nota pode contar mais", "Peso 2 significa que uma nota conta duas vezes. Dividimos pelo total de pesos, não necessariamente pelo número de notas.", "Nota 5, peso 1 → contribuição 5 × 1 = 5|Nota 8, peso 2 → contribuição 8 × 2 = 16|Contribuições → 5 + 16 = 21; pesos → 1 + 2 = 3|Média ponderada → 21 ÷ 3 = 7", "média ponderada = soma de (valor × peso) ÷ soma dos pesos", "Dividir por 2 daria uma conta errada neste caso. A nota 8 tem mais influência que a nota 5."),
("Total, valor ausente e outras medidas", "Média simples e quantidade permitem recuperar o total. Não confunda média com mediana (centro da lista ordenada) ou moda (valor mais frequente).", "3 números têm média 20 → total 3 × 20 = 60|Conhecidos 15 e 25 → somam 40|Desconhecido → 60 − 40 = 20|Lista 2, 2, 8 → média 4; mediana 2; moda 2", "total = média simples × quantidade; ausente = total − soma dos conhecidos", "Na média ponderada, trabalhe com contribuições e pesos. Um valor muito alto pode puxar a média simples para cima.")],
6: [
("Equação é uma balança", "A letra x é o valor desconhecido. Para manter os dois lados iguais, faça a mesma operação nos dois lados: não é uma troca de sinal sem motivo.", "2x + 4 = 14 → tire 4 dos dois lados|2x = 10 → divida os dois lados por 2|x = 5 → substitua para conferir|2 × 5 + 4 = 14 → igualdade confirmada", "ax + b = c → x = (c − b) ÷ a, com a diferente de zero", "Isolar x significa deixá-lo sozinho. x/3 = 6 pede multiplicar ambos os lados por 3: x = 18."),
("Parênteses e frases: cada operação tem motivo", "Distribuir é multiplicar cada termo do parêntese. Antes de traduzir uma frase, diga o que x representa.", "3(x + 2) = 15 → 3x + 6 = 15|Tire 6 → 3x = 9; divida por 3 → x = 3|‘Dobro de um número mais 7 é 19’ → 2x + 7 = 19|Tire 7 e divida por 2 → x = 6", "a(b + c) = a × b + a × c", "−(x − 2) = −x + 2. Dobro do número mais 7 não é o mesmo que dobro da soma do número com 7."),
("Sistema: duas pistas ao mesmo tempo", "Os valores devem funcionar nas duas equações. Podemos somá-las para cancelar uma letra, ou substituir uma expressão equivalente na outra.", "x + y = 8 e x − y = 2|Some os lados correspondentes → 2x = 10|x = 5 → na primeira, 5 + y = 8 → y = 3|Confira nas duas → 5 + 3 = 8 e 5 − 3 = 2", "eliminação: combinar equações para cancelar uma variável; substituição: usar um valor ou expressão equivalente", "Acertar uma equação só não resolve o sistema. Verifique também a unidade e o contexto do problema.")],
7: [
("Ler antes de fazer contas", "Título, eixos, unidades, legenda e período fazem parte dos dados. A legenda identifica categorias; os eixos dizem o que uma posição representa.", "Título → atendimentos por mês|Eixo horizontal → meses; vertical → atendimentos|Unidade ‘milhares’ → 12 representa 12 × 1.000 = 12.000|Legenda → identifique a categoria de cada cor ou símbolo", "valor real = valor mostrado × multiplicador da unidade", "Não misture milhares com unidades, nem meses com anos. Um gráfico não prova sozinho a causa de uma mudança."),
("Unidades e porcentagens são comparações diferentes", "De 80 para 100, há 20 unidades de aumento. O percentual compara essas 20 com o valor inicial, 80.", "Diferença → 100 − 80 = 20|Proporção → 20 ÷ 80 = 0,25|Porcentagem → 0,25 × 100 = 25%|Conclusão → aumento de 20 unidades, ou de 25%", "variação absoluta = final − inicial; variação (%) = ((final − inicial) ÷ inicial) × 100", "A base percentual não pode ser zero. De 50 para 40, a queda tem magnitude 10 unidades ou 20%; a variação é −10 ou −20%."),
("Escalas e tabelas: conferir o que os olhos sugerem", "Um eixo começando em 80 pode fazer uma diferença parecer enorme. Os números reais não mudam. Em tabelas de dupla entrada, leia a célula no cruzamento da linha com a coluna.", "Leia as marcas → 80, 90, 100: passos de 10|Calcule pelos valores → 100 − 80 = 20, não pela altura aparente|Tabela → linha ‘Manhã’, coluna ‘Segunda’: procure o cruzamento|40 de um total de 200 → (40 ÷ 200) × 100 = 20%", "participação (%) = parte ÷ total × 100", "Eixo truncado não impede toda leitura, mas pode amplificar a impressão visual. Conclusões precisam ser sustentadas pelos dados.")],
8: [
("Comprimento: o objeto não muda de tamanho", "Um metro e cem centímetros são o mesmo comprimento. Uma unidade menor exige mais unidades para medir o mesmo objeto.", "Escada → km, hm, dam, m, dm, cm, mm|Para a direita → multiplique por 10 a cada passo|m para cm: dois passos → 2 × 10 × 10 = 200 cm|cm para m: volte dividindo → 200 ÷ 100 = 2 m", "unidade menor: multiplicar; maior: dividir; comprimento usa fator 10 por passo", "De km a m são três passos: fator 1.000. Massa: 1 kg = 1.000 g; 3 kg = 3.000 g."),
("Área e volume multiplicam mais dimensões", "Área mede duas dimensões; volume, três. Por isso não podemos usar o mesmo fator linear sem elevar ao quadrado ou ao cubo.", "Quadrado de 1 m por 1 m → 100 cm por 100 cm|Área → 100 × 100 = 10.000 cm²|Cubo de 1 m por 1 m por 1 m → 10 dm em cada direção|Volume → 10 × 10 × 10 = 1.000 dm³ = 1.000 L", "fator de área = fator linear²; fator de volume = fator linear³", "Cada passo de área vale 100; de volume, 1.000. 1 dm³ = 1 L; 1 cm³ = 1 mL; 1 L = 1.000 mL."),
("Tempo e capacidade: veja a relação certa", "Hora usa sessenta minutos, não cem. Litro usa mil mililitros. Dividir ou multiplicar depende de ir para uma unidade maior ou menor.", "2 horas → 2 × 60 = 120 min|2h30min → 120 + 30 = 150 min|90 min → 90 ÷ 60 = 1,5 h = 1h30min|500 mL → 500 ÷ 1.000 = 0,5 L", "minutos = horas × 60; horas = minutos ÷ 60; litros = mililitros ÷ 1.000", "1,5 h não é 1h50min: 0,5 hora é metade de 60 minutos, ou 30 minutos.")],
9: [
("Cerca ou piso? Perímetro e área", "Perímetro é o contorno (uma cerca); área é a superfície interna (o piso). Base b e altura perpendicular h devem usar a mesma unidade.", "Retângulo 7 por 4 → contorno 7 + 4 + 7 + 4 = 22|Superfície → 4 fileiras de 7 quadradinhos = 28|Quadrado de lado 5 → 5 × 5 = 25|Triângulo base 10, altura 6 → metade do retângulo: 10 × 6 ÷ 2 = 30", "retângulo: P = 2(b + h), A = b × h; quadrado: A = l²; triângulo: A = b × h ÷ 2", "Perímetro usa unidade linear; área, quadrada. A altura do triângulo é perpendicular à base, não qualquer lado."),
("Círculo e caixa: identificar a medida pedida", "Raio r vai do centro à borda; diâmetro é 2r. π (pi) é aproximadamente 3,14. Para volume, conte uma camada de cubinhos e multiplique pelas camadas.", "Raio 3 → comprimento da borda 2 × π × 3 = 6π|Parte interna → área π × 3 × 3 = 9π|Caixa 2 por 5 por 3 → uma camada tem 2 × 5 = 10|Três camadas → 10 × 3 = 30 unidades cúbicas", "circunferência C = 2πr; círculo A = πr²; bloco retangular V = comprimento × largura × altura", "Se vier o diâmetro, divida por 2 para obter o raio. Use a aproximação de π pedida, ou mantenha π na resposta."),
("Ângulo reto e Pitágoras", "Um quarto de volta mede 90°, meia volta 180°, volta completa 360°. Pitágoras vale para triângulos retângulos: catetos formam o ângulo reto, hipotenusa fica oposta a ele.", "Complementares → somam 90°; suplementares → somam 180°|Catetos 3 e 4 → quadrados 9 e 16|Some → hipotenusa² = 9 + 16 = 25|Extraia a raiz → hipotenusa = √25 = 5", "h² = a² + b²; h = √(a² + b²), com a e b catetos", "Não some 3 + 4 para achar a hipotenusa. Ela é o maior lado. Não aplique Pitágoras sem um ângulo de 90°.")]
}

# Routes are question-specific. Every route has three explanatory steps.
SOLUTIONS = {
1: [
"Multiplicação antes da subtração.|3 × 4 = 12.|18 − 12 = 6; não faça (18 − 3) × 4.",
"Iguale os denominadores.|2/3 = 4/6, multiplicando cima e baixo por 2.|4/6 + 1/6 = 5/6; não some denominadores.",
"Separe sinal e tamanho.|Sinais iguais no produto dão positivo.|4 × 7 = 28, então (−4) × (−7) = 28.",
"Alinhe a vírgula: 2,5 = 2,50.|Some 2,50 + 0,75 = 3,25.|Confira: a resposta deve ser maior que 2,5 e menor que 3,5.",
"Dividir por fração é multiplicar pela inversa dela.|(3/5) × (10/9) = 30/45.|Simplifique dividindo ambos por 15: 2/3.",
"A raiz principal é não negativa.|Procure um número cujo quadrado seja 81.|9 × 9 = 81, logo √81 = 9. A equação x² = 81, diferentemente, aceita ±9.",
"Potência antes da soma.|2³ = 2 × 2 × 2 = 8.|8 + 5 = 13, não 2 × 3 + 5.",
"Há três casas decimais: 0,125 = 125/1.000.|Divida cima e baixo por 125.|125/1.000 = 1/8; confira 1 ÷ 8 = 0,125.",
"Sinais diferentes na divisão dão negativo.|12 ÷ 3 = 4.|−12 ÷ 3 = −4; confira −4 × 3 = −12.",
"Parêntese: 6 + 4 = 10.|Divisão: 10 ÷ 2 = 5.|Subtração: 20 − 5 = 15."],
2: [
"Múltiplos de 4: 4, 8, 12, 16…|Múltiplos de 6: 6, 12, 18…|Primeiro comum positivo: 12; 24 não é o menor.",
"Divisores de 18: 1, 2, 3, 6, 9, 18.|Divisores de 24: 1, 2, 3, 4, 6, 8, 12, 24.|O maior presente nas duas listas é 6.",
"Eventos começando juntos pedem o MMC dos intervalos.|Dias: 10, 20, 30… e 15, 30…|Primeiro reencontro positivo: 30 dias, não 10 + 15.",
"Pedaços iguais sem sobra precisam dividir ambas as medidas.|Queremos o maior divisor, portanto MDC.|MDC(24,36) = 12 cm: 2 peças e 3 peças.",
"12 = 2 × 6 = 2 × 2 × 3.|Dois fatores 2 são escritos 2².|Logo 12 = 2² × 3. O 6 da primeira decomposição ainda pode ser fatorado.",
"8 = 2³; 12 = 2² × 3.|Pegue os maiores expoentes: 2³ × 3.|Resultado 24; confira 24 ÷ 8 = 3 e 24 ÷ 12 = 2.",
"30 = 2 × 3 × 5; 45 = 3² × 5.|Pegue só comuns com menores expoentes: 3 × 5.|MDC = 15, divisor dos dois números.",
"O denominador mínimo é MMC(6,8).|6 = 2 × 3; 8 = 2³; MMC = 2³ × 3 = 24.|1/6 = 4/24 e 1/8 = 3/24. 48 serve, mas não é mínimo.",
"Coprimos têm somente 1 como divisor positivo comum.|8 e 9 são um exemplo; não precisam ser ambos primos.|Pela definição, o MDC é 1.",
"5 e 7 são primos distintos.|Sem fatores comuns maiores que 1, o MMC é o produto.|5 × 7 = 35; 70 é comum, mas maior."],
3: [
"Um caderno custa 24 ÷ 3 = R$ 8.|Cinco custam 5 × 8 = R$ 40.|Mais cadernos ao mesmo preço unitário implicam maior total.",
"Mesmo serviço e produtividade: relação inversa.|6 × 10 = 60 pessoas-dia.|60 ÷ 12 = 5 dias, metade do tempo.",
"Preserve a ordem 15 para 20.|Divida os dois por 5.|15:20 = 3:4; inverter mudaria a comparação.",
"Parta de a/b = c/d, com b e d não nulos.|Multiplique ambos os lados por b × d.|Cancelando denominadores, a × d = b × c.",
"Tempo = distância ÷ velocidade.|A distância é a mesma; aumentar o divisor diminui o tempo.|100 km a 50 km/h levam 2 h; a 100 km/h levam 1 h.",
"R$ 75 é o preço de dez unidades.|Preço de uma: 75 ÷ 10 = R$ 7,50.|Confira: 10 × 7,50 = 75.",
"Por pessoa: 300 ÷ 4 = 75 g.|Para 10 pessoas: 75 × 10 = 750 g.|Mais pessoas, mantendo a porção, exigem mais ingredientes.",
"Identifique o que cada número mede e as unidades.|Determine se a relação é direta ou inversa.|Depois monte a proporção; multiplicar sem interpretar pode inverter a conta.",
"Mesmo tanque e torneiras iguais: relação inversa.|Dobrar de 5 para 10 reduz o tempo à metade.|8 ÷ 2 = 4 h; confira 5 × 8 = 10 × 4.",
"Escala compara desenho e realidade na mesma unidade.|1:100 significa 1 unidade no desenho para 100 reais.|1 cm representa 100 cm = 1 m, não o contrário."],
5: [
"Some 4 + 6 + 8 = 18.|Conte três valores.|18 ÷ 3 = 6: a soma sozinha não é a média.",
"Some 10 + 20 = 30.|São dois valores: divida por 2.|30 ÷ 2 = 15, entre 10 e 20.",
"Média é total dividido pela quantidade.|Recupere o total multiplicando.|12 × 5 = 60; confira 60 ÷ 5 = 12.",
"Contribuições: 5 × 1 = 5 e 8 × 2 = 16.|Some contribuições 21 e pesos 3.|21 ÷ 3 = 7. A nota 8 tem influência maior.",
"Total: média 20 × 3 números = 60.|Conhecidos: 15 + 25 = 40.|Ausente: 60 − 40 = 20; confira a média dos três.",
"Média é calculada, não escolhida da lista.|10 e 20 têm média (10 + 20) ÷ 2 = 15.|15 não aparece: sim, a média pode estar ausente.",
"O peso indica quanto cada valor conta.|Some valor × peso no numerador.|Divida pela soma dos pesos; pesos 1 e 2 dão denominador 3.",
"Soma: 2 + 2 + 8 + 8 = 20.|Quantidade: quatro valores.|Média: 20 ÷ 4 = 5.",
"Com n valores, somar 3 a cada um aumenta o total em 3n.|Na média dividimos por n.|O aumento é 3n ÷ n = 3; por exemplo 2 e 4 viram 5 e 7.",
"n valores iguais a 7 somam 7n.|A média divide por n, para n positivo.|7n ÷ n = 7, qualquer que seja a quantidade."],
6: [
"Tire 4 dos dois lados de 2x + 4 = 14.|Fica 2x = 10: divida por 2.|x = 5; confira 2 × 5 + 4 = 14.",
"5x é 5 multiplicado por x.|Divida ambos os lados por 5.|x = 35 ÷ 5 = 7; confira 5 × 7 = 35.",
"x/3 significa x dividido por 3.|Multiplique os dois lados por 3.|x = 18; confira 18 ÷ 3 = 6.",
"Distribua: 3x + 6 = 15.|Tire 6: 3x = 9.|Divida por 3: x = 3; confira 3 × (3 + 2) = 15.",
"Some x + y = 8 e x − y = 2.|y cancela com −y: 2x = 10.|x = 5; na primeira, y = 3.",
"Use x = 5 da questão anterior.|5 + y = 8, então y = 8 − 5.|y = 3; confira também 5 − 3 = 2.",
"Defina x como o número desconhecido.|Dobro é 2x; mais 7 é 2x + 7.|‘É 19’ dá 2x + 7 = 19; 2(x + 7) dobraria também o 7.",
"Em x − 9 = 4, some 9 aos dois lados.|x = 4 + 9 = 13.|Confira: 13 − 9 = 4.",
"Em 4x − 8 = 0, some 8 aos dois lados.|4x = 8; divida por 4.|x = 2; confira 4 × 2 − 8 = 0.",
"A manipulação algébrica pode conter erros.|Substitua a solução na equação original.|Verifique igualdade, unidade e contexto; não arredonde sem necessidade."],
7: [
"O pedido é aumento absoluto, em unidades.|Subtraia o início do final: 100 − 80.|Resultado: 20 unidades, não 25%.",
"A diferença é 100 − 80 = 20.|Compare com o início: 20 ÷ 80 = 0,25.|Multiplique por 100: aumento de 25%, não 20%.",
"Leia o título para identificar o assunto.|Confira eixos e unidades para interpretar os valores.|Depois use legenda e período; não suponha que o eixo começa em zero.",
"Eixo truncado começa acima de zero.|Isso pode aumentar a diferença aparente entre barras.|Os valores reais não mudam: calcule pelos números da escala.",
"A parte é 40 e o total é 200.|Divida: 40 ÷ 200 = 0,20.|Multiplique por 100: 20% do total.",
"Queda em unidades: 50 − 40 = 10.|Magnitude percentual: (10 ÷ 50) × 100 = 20%.|São corretas B (10 unidades) e C (20%); D reúne ambas. A variação assinada é −20%.",
"Leia o rótulo da linha procurada.|Leia o rótulo da coluna procurada.|A célula no cruzamento contém o valor; cores não substituem os rótulos.",
"Cores e símbolos podem representar séries diferentes.|A legenda associa cada marca à sua categoria.|Use essa identificação antes de comparar; ela não muda os dados ou a escala.",
"A unidade indicada é milhares.|Cada 1 mostrado vale 1.000 unidades.|12 × 1.000 = 12.000, não 1.200.",
"Verifique o que foi medido e o período.|Sustente a conclusão nos valores disponíveis.|Não invente causa para uma correlação nem use apenas impressão visual."],
8: [
"1 m = 100 cm.|Multiplique 2 por 100.|2 m = 200 cm; unidade menor pede mais unidades.",
"1 kg = 1.000 g.|Multiplique 3 por 1.000.|3 kg = 3.000 g, o mesmo peso em outra unidade.",
"Uma hora tem 60 minutos.|Duas horas: 2 × 60 = 120 min.|Some 30: 150 min; não trate horas como base 100.",
"Um quadrado de 1 m tem lados de 100 cm.|Área em cm²: 100 × 100.|1 m² = 10.000 cm², pois mudaram duas dimensões.",
"Um cubo de 1 dm por 1 dm por 1 dm tem 1 dm³.|Esse volume equivale a 1 litro.|Logo 1 L = 1 dm³; 1 m³ é muito maior.",
"1 L = 1.000 mL.|Converta para unidade maior dividindo: 500 ÷ 1.000.|Resultado 0,5 L, metade de um litro.",
"1 km = 1.000 m.|Multiplique 2,5 por 1.000.|2,5 km = 2.500 m; são três passos métricos.",
"Uma hora tem 60 minutos.|90 ÷ 60 = 1,5 h.|A meia hora é 30 min: 1,5 h = 1h30min, não 1h50min.",
"1 m = 10 dm, em cada uma das três dimensões.|1 m³ = 10 × 10 × 10 = 1.000 dm³.|Como 1 dm³ = 1 L, temos 1.000 L.",
"Área multiplica duas medidas de comprimento.|Se cada medida muda por fator k, a área muda por k × k.|O fator é k²; de m para cm, 100² = 10.000."],
9: [
"Área mede superfície, não contorno.|Há quatro fileiras de sete unidades.|7 × 4 = 28 unidades quadradas.",
"Perímetro soma os quatro lados.|7 + 4 + 7 + 4 = 22.|Ou 2 × (7 + 4) = 22 unidades lineares.",
"Quadrado tem base e altura iguais a 5.|Área = lado × lado.|5 × 5 = 25 unidades quadradas; 20 seria o perímetro.",
"O triângulo ocupa metade do retângulo com mesma base e altura.|10 × 6 = 60.|Divida por 2: 30 unidades quadradas; altura deve ser perpendicular à base.",
"Uma volta completa tem 360°.|Um ângulo reto é um quarto dessa volta.|360 ÷ 4 = 90°.",
"Complementares completam um ângulo reto.|O ângulo reto mede 90°.|Portanto somam 90°; suplementares somam 180°.",
"Pitágoras: hipotenusa² = 3² + 4².|9 + 16 = 25.|Hipotenusa = √25 = 5; não some apenas 3 + 4.",
"O pedido é área do círculo: A = πr².|Raio 3: r² = 3 × 3 = 9.|A = 9π unidades quadradas; 6π mede a borda, não a área.",
"O pedido é comprimento da borda: C = 2πr.|Substitua raio 3: 2 × π × 3.|C = 6π unidades lineares; não use r².",
"Volume multiplica as três dimensões.|Uma camada: 2 × 5 = 10.|Três camadas: 10 × 3 = 30 unidades cúbicas."]
}

FORMULAS = {
1: ('frações equivalentes', '<mfrac><mn>2</mn><mn>3</mn></mfrac><mo>=</mo><mfrac><mn>4</mn><mn>6</mn></mfrac>', '2 ÷ 3 = 4 ÷ 6'),
2: ('fatoração prima', '<mn>12</mn><mo>=</mo><msup><mn>2</mn><mn>2</mn></msup><mo>×</mo><mn>3</mn>', '12 = 2 × 2 × 3'),
3: ('proporção', '<mfrac><mi>a</mi><mi>b</mi></mfrac><mo>=</mo><mfrac><mi>c</mi><mi>d</mi></mfrac>', 'a ÷ b = c ÷ d; a e c são numeradores, b e d denominadores não nulos'),
5: ('média ponderada do exemplo', '<mfrac><mrow><mn>5</mn><mo>×</mo><mn>1</mn><mo>+</mo><mn>8</mn><mo>×</mo><mn>2</mn></mrow><mrow><mn>1</mn><mo>+</mo><mn>2</mn></mrow></mfrac><mo>=</mo><mn>7</mn>', '((5 × 1) + (8 × 2)) ÷ (1 + 2) = 7'),
6: ('isolar a incógnita', '<mi>x</mi><mo>=</mo><mfrac><mrow><mi>c</mi><mo>−</mo><mi>b</mi></mrow><mi>a</mi></mfrac>', 'x = (c − b) ÷ a; em ax + b = c, a é o coeficiente de x, b é a parcela constante e c é o valor do outro lado; a ≠ 0'),
7: ('participação percentual', '<mfrac><mtext>parte</mtext><mtext>total</mtext></mfrac><mo>×</mo><mn>100</mn>', '(parte ÷ total) × 100, com total diferente de zero'),
8: ('conversão de área', '<mn>1</mn><mtext> m²</mtext><mo>=</mo><msup><mn>100</mn><mn>2</mn></msup><mtext> cm²</mtext>', '1 m² = 100 × 100 cm² = 10.000 cm²'),
9: ('hipotenusa no triângulo retângulo', '<mi>h</mi><mo>=</mo><msqrt><mrow><msup><mi>a</mi><mn>2</mn></msup><mo>+</mo><msup><mi>b</mi><mn>2</mn></msup></mrow></msqrt>', 'h = raiz quadrada de (a × a + b × b); a e b são catetos, h é hipotenusa')
}

PRACTICE = {
1: ('Quanto vale 12 − 2 × (1 + 3)?', 'Parêntese: 1 + 3 = 4.|Multiplicação: 2 × 4 = 8.|Subtração: 12 − 8 = 4.'),
2: ('Dois alarmes começam juntos e tocam a cada 4 e 10 minutos. Quando voltam a tocar juntos?', 'Múltiplos de 4: 4, 8, 12, 16, 20…|Múltiplos de 10: 10, 20…|MMC = 20 minutos após o início.'),
3: ('Se 4 canetas custam R$ 12, quanto custam 7 ao mesmo preço unitário?', 'Uma caneta custa 12 ÷ 4 = R$ 3.|Sete custam 7 × 3.|Total R$ 21; a relação é direta.'),
5: ('Uma nota 6 tem peso 2 e uma nota 9 tem peso 1. Qual a média?', 'Contribuições: 6 × 2 = 12 e 9 × 1 = 9.|Soma 21; soma dos pesos 3.|Média 21 ÷ 3 = 7.'),
6: ('Resolva 3x + 6 = 18 e confira o resultado.', 'Tire 6 dos dois lados: 3x = 12.|Divida por 3: x = 4.|Confira: 3 × 4 + 6 = 18.'),
7: ('Uma tabela passa de 40 para 50 atendimentos. Qual o aumento absoluto e percentual?', 'Diferença: 50 − 40 = 10 atendimentos.|Compare com o início: 10 ÷ 40 = 0,25.|Aumento de 10 atendimentos, ou 25%.'),
8: ('Converta 1h45min para minutos e depois para horas decimais.', 'Uma hora vale 60 min.|Some 45: 105 min.|105 ÷ 60 = 1,75 h. Não é 1,45 h.'),
9: ('Um retângulo mede 6 m por 2 m. Qual o perímetro e qual a área?', 'Contorno: 6 + 2 + 6 + 2 = 16 m.|Superfície: 6 × 2 = 12 m².|As unidades finais diferem: linear no perímetro, quadrada na área.')
}
