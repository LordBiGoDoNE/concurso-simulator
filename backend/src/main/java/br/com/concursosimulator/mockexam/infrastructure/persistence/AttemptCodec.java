package br.com.concursosimulator.mockexam.infrastructure.persistence;

import br.com.concursosimulator.mockexam.domain.*;
import br.com.concursosimulator.questionbank.domain.QuestionPurpose;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Os formatos1/2 continuam com os bytes imutáveis de origem; prática utiliza formato3. */
final class AttemptCodec {
    private final JsonMapper json=JsonMapper.builder().build();
    Attempt.Snapshot decode(String payload) {
        var node=(ObjectNode)json.readTree(payload);
        int format;
        if (!node.has("format")) {
            if (node.get("composition").has("useParameterizedQuestions")) throw new IllegalStateException("Composição nova sem versão");
            format=1;node.put("format",1);((ObjectNode)node.get("composition")).put("useParameterizedQuestions",false);
            for (var item:node.get("items")) legacyQuestion((ObjectNode)item.get("question"));
        } else {
            if (!node.get("format").isIntegralNumber()) throw new IllegalStateException("Formato inválido");
            format=node.get("format").asInt();
            if (format!=2 && format!=3) throw new IllegalStateException("Formato de snapshot desconhecido");
        }
        if (format<3) {
            if (node.has("purpose") || node.has("practiceTopic")) throw new IllegalStateException("Modalidade nova em formato antigo");
            node.put("purpose","SIMULADO");node.putNull("practiceTopic");
            for (var item:node.get("items")) oldPurpose((ObjectNode)item.get("question"));
        }
        return Attempt.restore(json.treeToValue(node,Attempt.Snapshot.class)).snapshot();
    }
    Attempt.Item item(String payload,int format) {
        var node=(ObjectNode)json.readTree(payload);
        if (format==1) legacyQuestion((ObjectNode)node.get("question"));
        if (format<3) oldPurpose((ObjectNode)node.get("question"));
        return json.treeToValue(node,Attempt.Item.class);
    }
    private void oldPurpose(ObjectNode question) {
        if (question.has("purpose")) throw new IllegalStateException("Destinação nova em formato antigo");
        question.put("purpose","SIMULADO");
    }
    private void legacyQuestion(ObjectNode question) {
        if (question.has("source")) throw new IllegalStateException("Fonte nova em snapshot antigo");
        question.set("source",json.valueToTree(QuestionSource.fixed()));
    }
    String encode(Attempt.Snapshot snapshot,String original) {
        if (snapshot.format()==3) return json.writeValueAsString(snapshot);
        ObjectNode node;
        if (original!=null) {
            if (decode(original).format()!=snapshot.format()) throw new IllegalStateException("Formato alterado");
            node=(ObjectNode)json.readTree(original);
        } else {
            if (snapshot.format()!=2) throw new IllegalStateException("Origem V5 ausente");
            node=(ObjectNode)json.valueToTree(snapshot);node.remove("purpose");node.remove("practiceTopic");
            for (var item:node.get("items")) ((ObjectNode)item.get("question")).remove("purpose");
        }
        node.set("answers",json.valueToTree(snapshot.answers()));node.set("finishedAt",json.valueToTree(snapshot.finishedAt()));node.put("version",snapshot.version());
        var encoded=json.writeValueAsString(node);
        if (!decode(encoded).equals(snapshot)) throw new IllegalStateException("Mudança imutável no snapshot");
        return encoded;
    }
    String item(Attempt.Item item) {
        var node=(ObjectNode)json.valueToTree(item);
        if (item.question().purpose()==QuestionPurpose.SIMULADO) ((ObjectNode)node.get("question")).remove("purpose");
        return json.writeValueAsString(node);
    }
}
