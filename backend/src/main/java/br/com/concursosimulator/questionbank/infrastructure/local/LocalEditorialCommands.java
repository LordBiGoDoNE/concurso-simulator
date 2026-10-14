package br.com.concursosimulator.questionbank.infrastructure.local;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.domain.EditorialDecision;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.nio.file.Path;
import java.io.*;

final class LocalEditorialCommands {
    record Command(String format, EditorialDecision decision, UUID expectedEligible, int expectedEditorialSequence) {
        Command {
            if (!"question-decision/v1".equals(format) || decision==null || expectedEditorialSequence<0) throw new IllegalArgumentException("Comando editorial inválido");
        }
    }
    static int run(String[] args, Map<String,String> env, BufferedReader input, PrintStream output) throws Exception {
        var report = args[0].equals("report");
        if (args.length != (report ? 3 : 5) || !"--editorial-local".equals(args[1]) || !"true".equals(args[2])
                || (!report && !"--decision".equals(args[3]))) throw new IllegalArgumentException("Uso: report|decide --editorial-local true [--decision arquivo]");
        var destination = QuestionBankLocal.destination(env.get("DB_URL"));
        QuestionBankLocal.credentials(env);
        var json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES).build();
        Command command = null;
        if (!report) {
            var bytes = LocalFiles.bounded(Path.of(args[4]), 64_000);
            command = json.readValue(bytes, Command.class);
            if (command.decision().action()==EditorialDecision.Action.APPROVE
                    && ("true".equalsIgnoreCase(env.get("CI")) || "true".equalsIgnoreCase(env.get("GITHUB_ACTIONS"))))
                throw new IllegalArgumentException("Launcher não aprova conteúdo em CI; fixtures usam harness de teste isolado");
            var d = command.decision(); var digest = LocalFiles.sha(bytes);
            output.println("Ação: " + d.action() + " | Destino: " + destination + " | Questão: " + d.questionId() + " | Revisão: " + d.revisionId()
                    + " | Fingerprint: " + d.fingerprint() + " | Sequência esperada: " + command.expectedEditorialSequence());
            output.println("Checklist: " + d.checklist().stream().map(Enum::name).sorted().toList() + " | Elegível esperado: " + command.expectedEligible());
            var phrase = "DECIDE " + destination + " " + digest;
            output.println("Confirme digitando exatamente: " + phrase); output.flush();
            if (!phrase.equals(input.readLine())) { output.println("Cancelado, sem acesso ao banco."); return 2; }
        } else output.println("Ação: report (somente leitura) | Destino: " + destination);
        try (var context = QuestionBankLocal.open(env,destination)) {
            if (report) output.println(json.writeValueAsString(new QuestionEditorialReportUseCase(context.getBean(QuestionInventory.class)).execute()));
            else {
                new DecideQuestionUseCase(context.getBean(QuestionStore.class)).execute(command.decision(),command.expectedEligible(),command.expectedEditorialSequence());
                output.println("Decisão confirmada após commit. Evidência é rastreabilidade, não autenticação Google nem revisão automática.");
            }
        }
        return 0;
    }
}
