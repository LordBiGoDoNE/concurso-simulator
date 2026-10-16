package br.com.concursosimulator.mockexam.infrastructure.persistence;

import br.com.concursosimulator.mockexam.domain.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Codec de origem: nunca converte campos imutáveis V5 durante uma atualização. */
final class AttemptCodec {
    private final JsonMapper json=JsonMapper.builder().build();
    Attempt.Snapshot decode(String payload) {
        var node=(ObjectNode)json.readTree(payload);
        if (!node.has("format")) {
            if (node.get("composition").has("useParameterizedQuestions"))
                throw new IllegalStateException("Composição nova sem versão");
            node.put("format",1);
            ((ObjectNode)node.get("composition")).put("useParameterizedQuestions",false);
            for (var item:node.get("items")) legacyQuestion((ObjectNode)item.get("question"));
        } else if (!node.get("format").isIntegralNumber() || node.get("format").asInt()!=2) {
            throw new IllegalStateException("Formato de snapshot desconhecido");
        }
        return Attempt.restore(json.treeToValue(node,Attempt.Snapshot.class)).snapshot();
    }
    Attempt.Item item(String payload, int format) {
        var node=(ObjectNode)json.readTree(payload);
        if (format==1) legacyQuestion((ObjectNode)node.get("question"));
        return json.treeToValue(node,Attempt.Item.class);
    }
    private void legacyQuestion(ObjectNode question) {
        if (question.has("source")) throw new IllegalStateException("Fonte nova em snapshot antigo");
        question.set("source",json.valueToTree(QuestionSource.fixed()));
    }
    String encode(Attempt.Snapshot snapshot, String original) {
        if (snapshot.format()==2) return json.writeValueAsString(snapshot);
        if (original==null || decode(original).format()!=1) throw new IllegalStateException("Origem V5 ausente");
        var node=(ObjectNode)json.readTree(original);
        // Reutiliza composição/itens/política originais, protegidos pelos triggers V5.
        node.set("answers",json.valueToTree(snapshot.answers()));
        node.set("finishedAt",json.valueToTree(snapshot.finishedAt())); node.put("version",snapshot.version());
        var encoded=json.writeValueAsString(node);
        if (!decode(encoded).equals(snapshot)) throw new IllegalStateException("Mudança imutável na origem V5");
        return encoded;
    }
    String item(Attempt.Item item) { return json.writeValueAsString(item); }
}
