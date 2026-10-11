package br.com.concursosimulator.questionbank.domain;

import static br.com.concursosimulator.questionbank.domain.Discipline.*;

/** IDs preservados dos módulos; o vínculo não é inferido de prefixos ou de HTML. */
public enum Topic {
    P01(PORTUGUESE, "Interpretação de Textos"),
    P02(PORTUGUESE, "Sinônimos, Antônimos e Relações de Sentido"),
    P03(PORTUGUESE, "Sentido Próprio e Figurado"),
    P04(PORTUGUESE, "Pontuação"),
    P05(PORTUGUESE, "Ortografia e Acentuação"),
    P06(PORTUGUESE, "Classes de Palavras"),
    P07(PORTUGUESE, "Concordância Verbal e Nominal"),
    P08(PORTUGUESE, "Regência Verbal e Nominal"),
    P09(PORTUGUESE, "Colocação Pronominal"),
    P10(PORTUGUESE, "Crase"),
    M01(MATHEMATICS_LOGIC, "Operações com Números Reais"),
    M02(MATHEMATICS_LOGIC, "MMC e MDC"),
    M03(MATHEMATICS_LOGIC, "Razão, Proporção e Regra de Três"),
    M04(MATHEMATICS_LOGIC, "Porcentagem e Juros Simples"),
    M05(MATHEMATICS_LOGIC, "Média Aritmética e Ponderada"),
    M06(MATHEMATICS_LOGIC, "Equações e Sistemas do 1º Grau"),
    M07(MATHEMATICS_LOGIC, "Tabelas e Gráficos"),
    M08(MATHEMATICS_LOGIC, "Sistemas de Medidas"),
    M09(MATHEMATICS_LOGIC, "Geometria- Perímetro, Área, Volume, Ângulos e Pitágoras"),
    M10(MATHEMATICS_LOGIC, "Relações Lógicas, Dedução e Sequências"),
    M11(MATHEMATICS_LOGIC, "Estruturas e Argumentação Lógica"),
    M12(MATHEMATICS_LOGIC, "Diagramas Lógicos e Situações-Problema"),
    I01(COMPUTING, "Windows - Arquivos, Pastas e Diretórios"),
    I02(COMPUTING, "Windows - Atalhos, Área de Trabalho e Área de Transferência"),
    I03(COMPUTING, "Word - Edição e Formatação"),
    I04(COMPUTING, "Word - Tabelas, Páginas, Índices e Objetos"),
    I05(COMPUTING, "Excel - Células, Linhas, Colunas e Referências"),
    I06(COMPUTING, "Excel - Fórmulas e Funções"),
    I07(COMPUTING, "Excel - Tabelas, Gráficos, Classificação e Dados"),
    I08(COMPUTING, "Excel - Macros"),
    I09(COMPUTING, "PowerPoint - Apresentações, Edição, Animações e Transições"),
    I10(COMPUTING, "E-mail, Internet, URL, Links e Busca"),
    A01(ADMINISTRATIVE_KNOWLEDGE, "Princípios da Administração Pública"),
    A02(ADMINISTRATIVE_KNOWLEDGE, "Rotinas Administrativas e Redação Técnica"),
    A03(ADMINISTRATIVE_KNOWLEDGE, "Processos Administrativos- Autuação, Registro e Encaminhamento"),
    A04(ADMINISTRATIVE_KNOWLEDGE, "Gestão de Materiais e Suprimentos"),
    A05(ADMINISTRATIVE_KNOWLEDGE, "Arquivística e Gestão Documental"),
    A06(ADMINISTRATIVE_KNOWLEDGE, "Atendimento ao Público e Comunicação"),
    A07(ADMINISTRATIVE_KNOWLEDGE, "Ética e Sigilo Profissional"),
    A08(ADMINISTRATIVE_KNOWLEDGE, "Orçamento, Cálculos e Prestação de Contas"),
    S01(SCHOOL_KNOWLEDGE, "LDB - Lei 9.394-1996"),
    S02(SCHOOL_KNOWLEDGE, "Escrituração Escolar"),
    S03(SCHOOL_KNOWLEDGE, "Registros Individuais, Ficha e Histórico Escolar"),
    S04(SCHOOL_KNOWLEDGE, "Matrícula, Frequência e Transferência"),
    S05(SCHOOL_KNOWLEDGE, "Protocolo e Arquivo Escolar"),
    S06(SCHOOL_KNOWLEDGE, "Redação Oficial - Atas, Ofícios e Requerimentos"),
    S07(SCHOOL_KNOWLEDGE, "Constituição Federal - Educação - arts. 205 a 214"),
    S08(SCHOOL_KNOWLEDGE, "ECA - arts. 1 a 6 e 53 a 69"),
    S09(SCHOOL_KNOWLEDGE, "Atendimento ao Público e Comunicação Escolar"),
    S10(SCHOOL_KNOWLEDGE, "Relações Humanas, Ética, Sigilo e Assiduidade"),
    S11(SCHOOL_KNOWLEDGE, "Censo Escolar e Informações Educacionais");

    private final Discipline discipline;
    private final String title;

    Topic(Discipline discipline, String title) {
        this.discipline = discipline;
        this.title = title;
    }

    public String id() { return name(); }
    public Discipline discipline() { return discipline; }
    public String title() { return title; }

    public static Topic fromId(String id) {
        for (var topic : values()) {
            if (topic.id().equals(id)) return topic;
        }
        throw new IllegalArgumentException("Assunto/módulo fora do catálogo: " + id);
    }
}
