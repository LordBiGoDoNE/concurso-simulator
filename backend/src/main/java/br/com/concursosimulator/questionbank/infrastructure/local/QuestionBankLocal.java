package br.com.concursosimulator.questionbank.infrastructure.local;

import br.com.concursosimulator.questionbank.application.*;
import br.com.concursosimulator.questionbank.application.port.*;
import br.com.concursosimulator.questionbank.infrastructure.persistence.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/** Launcher separado e deliberado. Nenhum bean/runner é carregado pelo startup web normal. */
public final class QuestionBankLocal {
    // Sem stereotype @Configuration/Component: não é descoberto pelo component scan da aplicação web.
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = QuestionRow.class)
    @Import({JpaQuestionStore.class, JpaQuestionInventory.class})
    public static class PersistenceOnly {}

    public static void main(String[] args) {
        int code;
        try { code = run(args, System.getenv(), new BufferedReader(new InputStreamReader(System.in)), System.out); }
        catch (Exception error) {
            // Não ecoar exceções JDBC/Jackson/args: podem conter credenciais, payloads ou valores privados.
            System.err.println("Operação recusada/falhou; nenhum sucesso foi confirmado. Confira seleção, base, snapshot e configuração local.");
            code = 1;
        }
        if (code != 0) System.exit(code);
    }

    public static int run(String[] args, Map<String,String> env, BufferedReader input, PrintStream output) throws Exception {
        if (args.length>0 && Set.of("report", "decide").contains(args[0])) return LocalEditorialCommands.run(args,env,input,output);
        var options = options(args);
        var action = options.get("action");
        var destination = destination(env.get("DB_URL"));
        credentials(env);
        var keys = new LinkedHashSet<>(List.of(options.get("keys").split(",", -1)));
        if (keys.contains("") || keys.size() != options.get("keys").split(",", -1).length) throw new IllegalArgumentException("Seleção vazia/repetida");
        var file = Path.of(options.get("package"));
        var bytes = LocalFiles.bounded(file, 32_000_000);
        // Leitura e validação integral antes de abrir banco, inclusive no modo import.
        var selected = new QuestionPackageReader().read(bytes, Path.of(options.get("root")), keys);
        var digest = LocalFiles.sha(bytes);
        output.println("Ação: " + action + " | Destino: " + destination + " | Seleção: " + String.join(",", keys.stream().sorted().toList()) + " | SHA-256: " + digest);
        output.println("Inventário: " + selected.inventory().size() + "; selecionados: " + selected.candidates().size()
                + "; pendências: " + selected.issues().size() + "; suspeitas: " + selected.possibleDuplicates().size() + ". Importação não aprova.");
        if (action.equals("import")) {
            var phrase = "APPLY " + destination + " " + digest;
            output.println("Confirme digitando exatamente: " + phrase); output.flush();
            if (!phrase.equals(input.readLine())) { output.println("Cancelado, sem acesso ao banco."); return 2; }
            if (!LocalFiles.sha(LocalFiles.bounded(file, 32_000_000)).equals(digest)) throw new IllegalArgumentException("Pacote mudou durante confirmação");
        }
        try (var context = open(env,destination)) {
            var json = JsonMapper.builder().build();
            if (action.equals("preview")) {
                output.println(json.writeValueAsString(new PreviewQuestionCandidatesUseCase(context.getBean(QuestionInventory.class)).execute(selected.candidates())));
            } else {
                output.println(json.writeValueAsString(new ApplyQuestionCandidatesUseCase(context.getBean(QuestionStore.class)).execute(selected.candidates())));
                output.println("Commit confirmado; novos conteúdos permanecem em rascunho.");
            }
        }
        return 0;
    }
    static void credentials(Map<String,String> env) {
        if (!env.containsKey("DB_USERNAME") || !env.containsKey("DB_PASSWORD")) throw new IllegalArgumentException("Credenciais externas obrigatórias");
    }
    static org.springframework.context.ConfigurableApplicationContext open(Map<String,String> env, String destination) {
        var app = new SpringApplication(PersistenceOnly.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setDefaultProperties(Map.of("spring.flyway.enabled", "false", "spring.main.banner-mode", "off",
                "logging.level.root", "OFF", "spring.jpa.hibernate.ddl-auto", "validate", "spring.jpa.open-in-view", "false"));
        // Não aceitar SPRING_* ou argumentos externos que habilitem DDL/migration/web nesta execução.
        app.setEnvironment(new org.springframework.core.env.StandardEnvironment() {
            @Override protected void customizePropertySources(org.springframework.core.env.MutablePropertySources sources) {
                sources.addLast(new org.springframework.core.env.MapPropertySource("local-only", Map.of(
                        "DB_URL", destination, "DB_USERNAME", env.get("DB_USERNAME"), "DB_PASSWORD", env.get("DB_PASSWORD"))));
            }
        });
        return app.run("--spring.config.location=classpath:/application.yaml", "--spring.flyway.enabled=false",
                "--spring.jpa.hibernate.ddl-auto=validate", "--spring.jpa.open-in-view=false", "--spring.main.web-application-type=none");
    }
    static Map<String,String> options(String[] args) {
        if (args.length != 9 || !Set.of("preview", "import").contains(args[0])) throw new IllegalArgumentException("Uso: preview|import --editorial-local true --package arquivo --root raiz --keys ID,ID");
        var options = new HashMap<String,String>(); options.put("action", args[0]);
        for (int i=1; i<args.length; i+=2) {
            if (!Set.of("--editorial-local", "--package", "--root", "--keys").contains(args[i]) || options.put(args[i].substring(2), args[i+1]) != null)
                throw new IllegalArgumentException("Argumentos desconhecidos/repetidos");
        }
        if (!"true".equals(options.get("editorial-local"))) throw new IllegalArgumentException("Modo local explícito obrigatório");
        return Map.copyOf(options);
    }
    static String destination(String url) {
        if (url == null || !url.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException("DB_URL explícita obrigatória");
        var uri = URI.create(url.substring(5));
        if (!Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost()) || uri.getUserInfo()!=null || uri.getFragment()!=null
                || uri.getPath()==null || !uri.getPath().matches("/[a-zA-Z0-9_]+") || uri.getPort() < -1 || uri.getPort() == 0)
            throw new IllegalArgumentException("Destino deve ser PostgreSQL local sem credenciais em URL");
        if (uri.getQuery()!=null) for (var part : uri.getQuery().split("&", -1)) {
            if (!part.matches("(currentSchema=[a-zA-Z0-9_]+|connectTimeout=[0-9]+|socketTimeout=[0-9]+|logServerErrorDetail=false)"))
                throw new IllegalArgumentException("Parâmetro JDBC não permitido");
        }
        return url;
    }
}
