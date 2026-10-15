package br.com.concursosimulator.parameterization.infrastructure.local;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.parameterization.application.TemplateUseCases;
import br.com.concursosimulator.parameterization.infrastructure.persistence.*;
import br.com.concursosimulator.questionbank.infrastructure.persistence.QuestionRow;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.json.JsonMapper;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

/** Entrada deliberada não web; não é component/configuration/runner da aplicação. */
public final class TemplateLocal {
    private static final JsonMapper JSON=JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .withCoercionConfig(LogicalType.Textual,c -> c.setCoercion(CoercionInputShape.Integer,CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float,CoercionAction.Fail).setCoercion(CoercionInputShape.Boolean,CoercionAction.Fail))
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public record DefinitionInput(String format,TemplateDefinition definition,Integer expectedLatest) {
        public DefinitionInput {
            if(!"parameter-template/v1".equals(format) || definition==null || expectedLatest==null || expectedLatest<0
                    || definition.version()!=(long)expectedLatest+1)throw new IllegalArgumentException("Seleção/versão-base inválidas");
        }
    }
    public record DecisionInput(String format,TemplateDecision decision,Integer expectedEligible,int expectedSequence) {
        public DecisionInput {
            if(!"parameter-decision/v1".equals(format) || decision==null || expectedSequence<0 || expectedEligible!=null && expectedEligible<1)
                throw new IllegalArgumentException("Decisão/estado esperado inválidos");
        }
    }
    public record Preview(TemplateVersion version,String definitionDigest,String domainDigest,String manifestDigest,List<GeneratedVariant> variants) {}
    // Sem @Configuration/@Component: somente SpringApplication explícita abaixo registra esta classe.
    @EnableAutoConfiguration @EntityScan(basePackageClasses={TemplateFamilyRow.class,QuestionRow.class}) @Import(JpaTemplateStore.class)
    public static class PersistenceOnly {}
    public static void main(String[] args) {
        var result=launch(args,System.getenv(),new BufferedReader(new InputStreamReader(System.in)),System.out,System.err);
        if(result!=0)System.exit(result);
    }
    public static int launch(String[] args,Map<String,String> env,BufferedReader input,PrintStream output,PrintStream errors) {
        try{return run(args,env,input,output);}
        catch(Exception error){errors.println("Operação recusada/falhou; nenhum sucesso confirmado. Confira arquivo, digests, base e configuração local.");return 1;}
    }
    public static int run(String[] args,Map<String,String> env,BufferedReader input,PrintStream output) throws Exception {
        if(args.length!=7 || !Set.of("preview","register","decide").contains(args[0]))throw new IllegalArgumentException("Uso: preview|register|decide --editorial-local true --root raiz --definition|--decision arquivo-relativo");
        var options=new HashMap<String,String>();var fileKey=args[0].equals("decide")?"--decision":"--definition";
        for(int i=1;i<args.length;i+=2)
            if(!Set.of("--editorial-local","--root",fileKey).contains(args[i]) || options.put(args[i],args[i+1])!=null)throw new IllegalArgumentException("Opções desconhecidas/repetidas");
        if(!"true".equals(options.get("--editorial-local")))throw new IllegalArgumentException("Modo local explícito obrigatório");
        var root=Path.of(options.get("--root"));var relative=options.get(fileKey);var bytes=ControlledInput.read(root,relative);var digest=ControlledInput.sha(bytes);
        DefinitionInput definition=null;DecisionInput decision=null;TemplateVersion version=null;
        if(args[0].equals("decide"))decision=JSON.readValue(bytes,DecisionInput.class);
        else {definition=JSON.readValue(bytes,DefinitionInput.class);version=TemplateUseCases.preview(definition.definition());}
        if(args[0].equals("preview")) {
            var d=version.definition();output.println(JSON.writeValueAsString(new Preview(version,d.digest(),d.family().domainDigest(),version.manifest().digest(),d.family().domain().stream().map(p -> NumericGenerator.render(d,p)).toList())));
            return 0; // Nenhuma leitura de DB_URL/credencial nem contexto Spring.
        }
        var destination=ControlledInput.destination(env.get("DB_URL"));
        if(env.get("DB_USERNAME")==null || env.get("DB_PASSWORD")==null)throw new IllegalArgumentException("Credenciais externas obrigatórias");
        if(decision!=null && Set.of(TemplateDecision.Action.APPROVE,TemplateDecision.Action.ACTIVATE).contains(decision.decision().action())
                && ("true".equalsIgnoreCase(env.get("CI")) || "true".equalsIgnoreCase(env.get("GITHUB_ACTIONS"))))throw new IllegalArgumentException("CI não aprova/ativa conteúdo real");
        output.println("Ação: "+args[0]+" | Destino: "+destination+" | SHA-256: "+digest);
        if(version!=null)output.println("Template: "+version.definition().id()+" | Versão: "+version.definition().version()+" | Definição: "+version.definition().digest()+" | Manifesto: "+version.manifest().digest());
        else output.println("Template: "+decision.decision().templateId()+" | Versão: "+decision.decision().version()+" | Manifesto: "+decision.decision().manifestDigest());
        var phrase=args[0].toUpperCase(Locale.ROOT)+" "+destination+" "+digest;
        output.println("Confirme digitando exatamente: "+phrase);output.flush();
        if(!phrase.equals(input.readLine())){output.println("Cancelado, sem acesso ao banco.");return 2;}
        if(!digest.equals(ControlledInput.sha(ControlledInput.read(root,relative))))throw new IllegalArgumentException("Arquivo mudou durante confirmação");
        try(var context=open(env,destination)) {
            var uc=new TemplateUseCases(context.getBean(JpaTemplateStore.class));
            if(definition!=null) {var result=uc.register(definition.definition(),definition.expectedLatest());output.println("Commit confirmado; registro "+(result.replayed()?"reutilizado sem alterar elegibilidade":"em rascunho")+".");}
            else {uc.decide(decision.decision(),decision.expectedEligible(),decision.expectedSequence());output.println("Decisão confirmada após commit; evidência editorial não é autenticação Google.");}
        }
        return 0;
    }
    private static org.springframework.context.ConfigurableApplicationContext open(Map<String,String> env,String destination) {
        var app=new SpringApplication(PersistenceOnly.class);app.setWebApplicationType(WebApplicationType.NONE);
        app.setEnvironment(new org.springframework.core.env.StandardEnvironment(){
            @Override protected void customizePropertySources(org.springframework.core.env.MutablePropertySources sources) {
                sources.addLast(new org.springframework.core.env.MapPropertySource("local-only",Map.of("DB_URL",destination,"DB_USERNAME",env.get("DB_USERNAME"),"DB_PASSWORD",env.get("DB_PASSWORD"))));
            }
        });
        return app.run("--spring.config.location=classpath:/application.yaml","--spring.flyway.enabled=false","--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.jpa.open-in-view=false","--spring.main.web-application-type=none","--spring.main.banner-mode=off","--logging.level.root=OFF");
    }
}
