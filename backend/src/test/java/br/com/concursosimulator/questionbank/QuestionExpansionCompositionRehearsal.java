package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.domain.*;
import br.com.concursosimulator.questionbank.infrastructure.importing.AdministrativePackageReader;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaEligibleRevisionPool;
import br.com.concursosimulator.questionbank.infrastructure.persistence.JpaQuestionInventory;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Ensaio deliberado #173 (grupo 8.2): composição completa dos pacotes finais em PostgreSQL
 * novo e exclusivo. Exercita prévia, aplicação DRAFT, replay, reaplicação, base desatualizada,
 * seleção/digest alterados, concorrência e rollback; nunca decide/habilita conteúdo e não
 * acessa banco do operador. Ferramenta somente de teste, fora da CI (pacotes reais).
 */
public final class QuestionExpansionCompositionRehearsal {
    private QuestionExpansionCompositionRehearsal() {}
    static final List<String> TABLES=List.of("qb_question","qb_revision","qb_alternative","qb_revision_role","qb_reference",
            "qb_decision","qb_import_receipt","qb_import_revision","qb_import_blob","qb_import_file");
    record Loaded(Path file,AdministrativePackageReader.Document document,List<String> keys,String packageDigest,String selectionDigest){
        QuestionImportPackage read(){return new AdministrativePackageReader().read(document,keys);}
    }
    static final Clock CLOCK=Clock.systemUTC();
    static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    static ImportConfirmation confirmation(QuestionImportPackage p,UUID actor){
        var now=CLOCK.instant();return new ImportConfirmation(UUID.randomUUID(),actor,"isolated-173-rehearsal",p.digest(),p.selectionDigest(),now,now.plusSeconds(900));
    }
    static Map<String,String> snapshot(QuestionDatabase db){
        var result=new LinkedHashMap<String,String>();
        for(var table:TABLES)result.put(table,db.jdbc.queryForObject(
                "select count(*)||':'||coalesce(md5(string_agg(to_jsonb(r)::text,'|' order by to_jsonb(r)::text)),'-') from "+table+" r",String.class));
        return result;
    }
    static int count(QuestionDatabase db,String table){return db.jdbc.queryForObject("select count(*) from "+table,Integer.class);}

    public static void main(String[] args) throws Exception {
        if(args.length!=4||!args[0].equals("--packages")||!args[2].equals("--output"))
            throw new IllegalArgumentException("Uso: --packages diretório --output relatório.json");
        var json=JsonMapper.builder().build();
        var root=Path.of(args[1]).toAbsolutePath().normalize();
        var loaded=new ArrayList<Loaded>();
        try(var walk=Files.walk(root)){
            for(var file:walk.filter(p->p.getFileName().toString().equals("preview-request.json")).sorted().toList()){
                var request=json.readTree(Files.readAllBytes(file));
                var digests=json.readTree(Files.readAllBytes(file.resolveSibling("digests.json")));
                loaded.add(new Loaded(file,json.treeToValue(request.get("package"),AdministrativePackageReader.Document.class),
                        request.get("keys").valueStream().map(n->n.asString()).toList(),
                        digests.path("packageDigest").asString(),digests.path("selectionDigest").asString()));
            }
        }
        require(!loaded.isEmpty(),"Nenhum pacote");
        var steps=new LinkedHashMap<String,Object>();
        var started=System.nanoTime();
        try(var pg=new PostgreSQLContainer("postgres:18.6")){
            pg.start();
            var actor=UUID.randomUUID();
            try(var db=new QuestionDatabase(pg)){
                var inventory=new JpaQuestionInventory(SharedEntityManagerCreator.createSharedEntityManager(db.factory),db.manager);
                var preview=new PreviewQuestionCandidatesUseCase(inventory);
                var apply=new ApplyAdministrativeQuestionsUseCase(db.store,CLOCK);
                var pool=new JpaEligibleRevisionPool(SharedEntityManagerCreator.createSharedEntityManager(db.factory));
                var packs=new ArrayList<QuestionImportPackage>();int items=0;
                for(var l:loaded){
                    var p=l.read();
                    require(p.digest().equals(l.packageDigest())&&p.selectionDigest().equals(l.selectionDigest()),"Digest divergente: "+l.file());
                    require(preview.execute(p.candidates()).stream().allMatch(r->r.change()==PreviewQuestionCandidatesUseCase.Change.NEW),"Prévia inicial não NEW");
                    packs.add(p);items+=l.keys().size();
                }
                require(snapshot(db).values().stream().allMatch(v->v.startsWith("0:")),"Prévia escreveu");
                steps.put("previewBeforeApply",Map.of("packages",packs.size(),"items",items,"change","NEW","writes",0));

                var confirmations=new ArrayList<ImportConfirmation>();var receipts=new ArrayList<ImportReceipt>();
                var applyStart=System.nanoTime();
                for(var p:packs){var c=confirmation(p,actor);confirmations.add(c);var o=apply.execute(p,c,actor,c.destination());
                    require(!o.replayed()&&o.receipt().results().stream().noneMatch(ApplyQuestionCandidatesUseCase.Result::reused),"Aplicação inicial reutilizou");receipts.add(o.receipt());}
                var applySeconds=(System.nanoTime()-applyStart)/1e9;
                require(count(db,"qb_question")==items&&count(db,"qb_revision")==items,"Questões/revisões divergentes");
                require(count(db,"qb_import_receipt")==packs.size()&&count(db,"qb_decision")==0,"Recibos/decisões divergentes");
                for(var purpose:QuestionPurpose.values())require(db.store.transaction(s->pool.capture(purpose)).isEmpty(),"Pool elegível não vazio após importação");
                steps.put("apply",Map.of("receipts",packs.size(),"questions",items,"revisions",items,"decisions",0,
                        "eligibleSimulado",0,"eligiblePratica",0,"seconds",Math.round(applySeconds*10)/10.0));
                var afterApply=snapshot(db);

                int identical=0;
                for(var p:packs){identical+=(int)preview.execute(p.candidates()).stream().filter(r->r.change()==PreviewQuestionCandidatesUseCase.Change.IDENTICAL).count();}
                require(identical==items,"Prévia após aplicação deveria ser IDENTICAL");
                for(int i=0;i<packs.size();i++){var o=apply.execute(packs.get(i),confirmations.get(i),actor,confirmations.get(i).destination());
                    require(o.replayed()&&o.receipt().equals(receipts.get(i)),"Replay sem mesmo recibo");}
                require(snapshot(db).equals(afterApply),"Replay alterou o banco");
                steps.put("previewAfterApplyAndReplay",Map.of("identical",identical,"replayedReceipts",packs.size(),"unchanged",true));

                var again=packs.getFirst();var reapplied=apply.execute(again,confirmation(again,actor),actor,"isolated-173-rehearsal");
                require(!reapplied.replayed()&&reapplied.receipt().results().stream().allMatch(ApplyQuestionCandidatesUseCase.Result::reused),"Reaplicação criou conteúdo");
                require(count(db,"qb_question")==items&&count(db,"qb_revision")==items&&count(db,"qb_import_receipt")==packs.size()+1,"Reaplicação divergente");
                steps.put("reapplyNewNonce",Map.of("reused",again.candidates().size(),"newQuestions",0,"newReceipts",1));
                var beforeNegative=snapshot(db);

                var base=loaded.getFirst();var first=base.document().items().getFirst();var content=first.content();
                var changed=new QuestionImportReader.Content(new TextContent(content.statement().text()+" (ensaio)"),content.alternatives(),
                        content.correctAlternativeId(),content.resolution(),content.classification(),content.references(),content.provenance());
                var domain=new QuestionContent(changed.statement(),changed.alternatives(),changed.correctAlternativeId(),changed.resolution(),
                        QuestionClassification.fromIds(changed.classification().disciplineId(),changed.classification().topicId(),changed.classification().roleIds()),
                        changed.references(),changed.provenance());
                var staleItems=new ArrayList<>(base.document().items());staleItems.set(0,new AdministrativePackageReader.Item(first.origin(),changed,domain.fingerprint(),UUID.randomUUID()));
                var stale=new AdministrativePackageReader().read(new AdministrativePackageReader.Document(base.document().format(),base.document().purpose(),staleItems,base.document().files()),base.keys());
                require(preview.execute(stale.candidates()).getFirst().change()==PreviewQuestionCandidatesUseCase.Change.STALE,"Prévia não sinalizou STALE");
                boolean staleRejected=false;
                try{apply.execute(stale,confirmation(stale,actor),actor,"isolated-173-rehearsal");}catch(StaleQuestionException expected){staleRejected=true;}
                require(staleRejected&&snapshot(db).equals(beforeNegative),"Base desatualizada não rejeitou o lote inteiro");

                var target=packs.get(1);var good=confirmation(target,actor);int rejected=0;
                for(var bad:List.of(new ImportConfirmation(good.nonce(),actor,good.destination(),"0".repeat(64),good.selectionDigest(),good.issuedAt(),good.expiresAt()),
                        new ImportConfirmation(good.nonce(),actor,good.destination(),good.packageDigest(),"0".repeat(64),good.issuedAt(),good.expiresAt()))){
                    try{apply.execute(target,bad,actor,bad.destination());}catch(ApplyAdministrativeQuestionsUseCase.ConfirmationFailure expected){rejected++;}
                }
                try{apply.execute(target,good,UUID.randomUUID(),good.destination());}catch(ApplyAdministrativeQuestionsUseCase.ConfirmationFailure expected){rejected++;}
                try{apply.execute(target,good,actor,"other-destination");}catch(ApplyAdministrativeQuestionsUseCase.ConfirmationFailure expected){rejected++;}
                require(rejected==4,"Confirmação alterada aceita");
                var smaller=loaded.get(1);boolean selectionRejected=false,tamperRejected=false;
                try{new AdministrativePackageReader().read(smaller.document(),List.of(smaller.keys().getFirst(),"admin.2026-11.inexistente"));}catch(RuntimeException expected){selectionRejected=true;}
                var files=new ArrayList<>(smaller.document().files());var f=files.getFirst();
                files.set(0,new AdministrativePackageReader.File(f.path(),f.mediaType(),"0".repeat(64),f.base64()));
                try{new AdministrativePackageReader().read(new AdministrativePackageReader.Document(smaller.document().format(),smaller.document().purpose(),smaller.document().items(),files),smaller.keys());}
                catch(RuntimeException expected){tamperRejected=true;}
                var subset=new AdministrativePackageReader().read(smaller.document(),smaller.keys().subList(0,5));
                require(selectionRejected&&tamperRejected&&!subset.selectionDigest().equals(smaller.selectionDigest()),"Seleção/arquivo alterados aceitos");
                require(snapshot(db).equals(beforeNegative),"Casos negativos alteraram o banco");
                steps.put("staleBaseAndAlteredSelection",Map.of("stalePreview","STALE","staleRejectedWholeSelection",true,
                        "confirmationMismatchesRejected",rejected,"unknownSelectionKeyRejected",true,"tamperedFileHashRejected",true,
                        "subsetChangesSelectionDigest",true,"databaseUnchanged",true));

                db.jdbc.execute("create function fail_rehearsal() returns trigger language plpgsql as $$ begin raise exception 'SYNTHETIC_REHEARSAL_FAILURE'; end $$");
                db.jdbc.execute("create trigger fail_rehearsal before insert on qb_import_file for each row execute function fail_rehearsal()");
                var extra=new AdministrativePackageReader().read(smaller.document(),smaller.keys());
                var extraItems=new ArrayList<AdministrativePackageReader.Item>();
                for(var item:smaller.document().items())extraItems.add(new AdministrativePackageReader.Item(
                        new QuestionOrigin(item.origin().namespace(),item.origin().key()+".rollback"),item.content(),item.fingerprint(),null));
                var fresh=new AdministrativePackageReader().read(new AdministrativePackageReader.Document(smaller.document().format(),smaller.document().purpose(),extraItems,smaller.document().files()),
                        extraItems.stream().map(i->i.origin().key()).toList());
                boolean rolledBack=false;
                try{apply.execute(fresh,confirmation(fresh,actor),actor,"isolated-173-rehearsal");}catch(RuntimeException expected){rolledBack=true;}
                require(rolledBack&&snapshot(db).equals(beforeNegative),"Falha não reverteu a transação");
                db.jdbc.execute("drop trigger fail_rehearsal on qb_import_file");
                require(extra.selectionDigest().equals(smaller.selectionDigest()),"Releitura alterou digest");
                steps.put("rollback",Map.of("failureInjectedAt","qb_import_file","databaseUnchanged",true,"newOrigins",extraItems.size()));
                steps.put("finalCounts",Map.of("questions",count(db,"qb_question"),"receipts",count(db,"qb_import_receipt"),"decisions",count(db,"qb_decision"),
                        "eligibleSimulado",db.store.transaction(s->pool.capture(QuestionPurpose.SIMULADO)).size(),
                        "eligiblePratica",db.store.transaction(s->pool.capture(QuestionPurpose.PRATICA_AULA)).size()));
            }
            try(var db=new QuestionDatabase(pg)){
                var apply=new ApplyAdministrativeQuestionsUseCase(db.store,CLOCK);
                var p=loaded.getLast().read();var c=confirmation(p,actor);
                try(var workers=Executors.newFixedThreadPool(4)){
                    var a=workers.submit(()->apply.execute(p,c,actor,c.destination()));var b=workers.submit(()->apply.execute(p,c,actor,c.destination()));
                    require(a.get(120,TimeUnit.SECONDS).receipt().equals(b.get(120,TimeUnit.SECONDS).receipt()),"Mesmo nonce concorrente divergiu");
                    var x=workers.submit(()->apply.execute(p,confirmation(p,actor),actor,"isolated-173-rehearsal"));
                    var y=workers.submit(()->apply.execute(p,confirmation(p,actor),actor,"isolated-173-rehearsal"));
                    var rx=x.get(120,TimeUnit.SECONDS).receipt().results();var ry=y.get(120,TimeUnit.SECONDS).receipt().results();
                    for(int i=0;i<rx.size();i++)require(rx.get(i).questionId().equals(ry.get(i).questionId()),"Identidade duplicada sob concorrência");
                }
                require(count(db,"qb_question")==p.candidates().size()&&count(db,"qb_revision")==p.candidates().size()&&count(db,"qb_import_receipt")==3,"Concorrência criou duplicatas");
                steps.put("concurrency",Map.of("schema","novo","items",p.candidates().size(),"sameNonceSingleReceipt",true,"differentNonceSameIdentity",true,"receipts",3));
            }
        }
        var report=new LinkedHashMap<String,Object>();
        report.put("format","question-expansion-composition-rehearsal/v1");
        report.put("destination","isolated-new-postgres-18.6-173-rehearsal");
        report.put("packages",loaded.size());
        report.put("steps",steps);
        report.put("seconds",Math.round((System.nanoTime()-started)/1e8)/10.0);
        report.put("limits","Ensaio em PostgreSQL novo e descartável: não decide nem habilita conteúdo, não consulta banco do operador e não substitui revisão editorial ou teste do usuário.");
        var output=Path.of(args[3]);Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output,json.writeValueAsString(report)+"\n");
        System.out.println("Ensaio concluído: "+loaded.size()+" pacotes; relatório "+output);
    }
}
