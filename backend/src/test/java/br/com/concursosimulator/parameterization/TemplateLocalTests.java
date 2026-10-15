package br.com.concursosimulator.parameterization;

import br.com.concursosimulator.parameterization.domain.*;
import br.com.concursosimulator.parameterization.application.TemplateUseCases;
import br.com.concursosimulator.parameterization.infrastructure.local.TemplateLocal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static br.com.concursosimulator.parameterization.ParameterFixtures.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class TemplateLocalTests {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:18.6");
    @TempDir Path root;
    final JsonMapper json=JsonMapper.builder().build();
    final ByteArrayOutputStream output=new ByteArrayOutputStream();
    PrintStream out(){return new PrintStream(output,true,StandardCharsets.UTF_8);}
    String[] args(String action,String file){return new String[]{action,"--editorial-local","true","--root",root.toString(),action.equals("decide")?"--decision":"--definition",file};}
    Path file(String name,Object value)throws Exception{var f=root.resolve(name);Files.writeString(f,json.writeValueAsString(value));return f;}
    Map<String,String> env(ParameterDatabase db){return Map.of("DB_URL","jdbc:postgresql://127.0.0.1:"+PG.getMappedPort(5432)+"/"+PG.getDatabaseName()+"?currentSchema="+db.schema,"DB_USERNAME",PG.getUsername(),"DB_PASSWORD",PG.getPassword());}
    String confirmation(String action,Map<String,String> env,Path file)throws Exception{return action.toUpperCase(Locale.ROOT)+" "+env.get("DB_URL")+" "+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)))+"\n";}
    int run(String action,String name,Map<String,String> env,String input)throws Exception{return TemplateLocal.run(args(action,name),env,new BufferedReader(new StringReader(input)),out());}
    @Test void previewEnumeratesWholeDomainWithoutDatabaseCredentialsOrWrites()throws Exception{
        var d=definition(Family.DIRECT_PROPORTION);file("template.json",new TemplateLocal.DefinitionInput("parameter-template/v1",d,0));
        assertThat(run("preview","template.json",Map.of(),"")).isZero();
        var tree=json.readTree(output.toString(StandardCharsets.UTF_8));assertThat(tree.get("variants").size()).isEqualTo(64);
        assertThat(tree.get("manifestDigest").asString()).isEqualTo(TemplateUseCases.preview(d).manifest().digest());
        try(var files=Files.list(root)){assertThat(files.map(Path::getFileName).toList()).containsExactly(Path.of("template.json"));}
    }
    @Test void exactConfirmationRegistersOnlyDraftAndExplicitDecisionsApproveAndWithdrawInIsolatedPostgres()throws Exception{
        try(var db=new ParameterDatabase(PG)){
            var d=db.seed(Family.PERCENTAGE);var env=env(db);var f=file("template.json",new TemplateLocal.DefinitionInput("parameter-template/v1",d,0));
            assertThat(run("register","template.json",env,"yes\n")).isEqualTo(2);
            assertThat(db.jdbc.queryForObject("select count(*) from pq_family",Integer.class)).isZero();
            assertThat(run("register","template.json",env,confirmation("register",env,f))).isZero();
            assertThat(db.jdbc.queryForObject("select status from pq_version",String.class)).isEqualTo("DRAFT");
            var v=TemplateUseCases.preview(d);int sequence=0;
            for(var action:List.of(TemplateDecision.Action.SUBMIT,TemplateDecision.Action.APPROVE,TemplateDecision.Action.WITHDRAW)){
                var decision=decision(v,action);var df=file("decision.json",new TemplateLocal.DecisionInput("parameter-decision/v1",decision,sequence==2?1:null,sequence));
                assertThat(run("decide","decision.json",env,confirmation("decide",env,df))).isZero();sequence++;
            }
            assertThat(run("register","template.json",env,confirmation("register",env,f))).isZero();
            assertThat(db.jdbc.queryForObject("select eligible_version from pq_family",Integer.class)).isNull();
            assertThat(db.jdbc.queryForObject("select count(*) from pq_decision",Integer.class)).isEqualTo(3);
        }
    }
    @Test void ciCannotApproveOrActivateAndRemoteDestinationsAreRejectedBeforeConnection()throws Exception{
        var v=TemplateUseCases.preview(definition(Family.PERCENTAGE));
        for(var action:List.of(TemplateDecision.Action.APPROVE,TemplateDecision.Action.ACTIVATE)){
            file("decision.json",new TemplateLocal.DecisionInput("parameter-decision/v1",decision(v,action),null,0));
            var env=Map.of("DB_URL","jdbc:postgresql://127.0.0.1:1/unreachable","DB_USERNAME","fixture","DB_PASSWORD","never-logged","CI","true");
            assertThatThrownBy(() -> run("decide","decision.json",env,"")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CI");
            var remote=new HashMap<>(env);remote.put("DB_URL","jdbc:postgresql://remote.example/production");
            assertThatThrownBy(() -> run("decide","decision.json",remote,"")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("local");
        }
        assertThat(output.size()).isZero();
    }
    @Test void malformedUnknownDuplicateTrailingAndPartialSelectionsFailBeforeAnyWrite()throws Exception{
        var v=new TemplateLocal.DefinitionInput("parameter-template/v1",definition(Family.PERCENTAGE),0);var valid=json.writeValueAsString(v);
        for(var invalid:List.of(valid.substring(0,valid.length()-1)+",\"extra\":true}","{\"format\":\"parameter-template/v1\","+valid.substring(1),valid+" {}","["+valid+",{}]",valid.replace("\"expectedLatest\":0","\"expectedLatest\":null"))){
            Files.writeString(root.resolve("invalid.json"),invalid);
            assertThatThrownBy(() -> run("register","invalid.json",Map.of(),"")).isInstanceOf(RuntimeException.class);
        }
        assertThat(output.size()).isZero();
    }
    @Test void traversalOversizeAndSymlinkInputsCannotEscapeTheChosenRoot()throws Exception{
        assertThatThrownBy(() -> run("preview","../outside.json",Map.of(),"")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run("preview","C:/private.json",Map.of(),"")).isInstanceOf(IllegalArgumentException.class);
        Files.write(root.resolve("large.json"),new byte[65537]);
        assertThatThrownBy(() -> run("preview","large.json",Map.of(),"")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void symbolicLinkIsRejectedWhenTheFilesystemSupportsCreatingOne()throws Exception{
        var source=file("source.json",new TemplateLocal.DefinitionInput("parameter-template/v1",definition(Family.PERCENTAGE),0));
        try{Files.createSymbolicLink(root.resolve("link.json"),source);}
        catch(UnsupportedOperationException | FileSystemException unavailable){
            if(System.getProperty("os.name").startsWith("Windows"))Assumptions.abort("Windows sem permissão de symlink; Linux CI exige a rejeição");
            else throw unavailable;
        }
        assertThatThrownBy(() -> run("preview","link.json",Map.of(),"")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Symlink");
    }
    @Test void fileChangedDuringConfirmationIsRejectedAndLaunchNeverPrintsInputOrCredentials()throws Exception{
        var f=file("template.json",new TemplateLocal.DefinitionInput("parameter-template/v1",definition(Family.PERCENTAGE),0));
        var env=Map.of("DB_URL","jdbc:postgresql://127.0.0.1:1/unreachable","DB_USERNAME","fixture","DB_PASSWORD","never-logged");
        var phrase=confirmation("register",env,f).stripTrailing();
        var changing=new BufferedReader(new StringReader("")){@Override public String readLine()throws IOException{Files.writeString(f,"{}");return phrase;}};
        assertThatThrownBy(() -> TemplateLocal.run(args("register","template.json"),env,changing,out())).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mudou");
        Files.writeString(f,"{\"secret\":\"never-print-input\"}");var errors=new ByteArrayOutputStream();
        assertThat(TemplateLocal.launch(args("register","template.json"),env,new BufferedReader(new StringReader("")),out(),new PrintStream(errors))).isEqualTo(1);
        assertThat(errors.toString(StandardCharsets.UTF_8)).doesNotContain("never-logged","never-print-input","SQLException");
        assertThat(output.toString(StandardCharsets.UTF_8)).doesNotContain("never-logged","never-print-input");
    }
}
