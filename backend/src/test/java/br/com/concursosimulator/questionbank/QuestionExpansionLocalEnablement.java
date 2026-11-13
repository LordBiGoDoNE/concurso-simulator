package br.com.concursosimulator.questionbank;

import br.com.concursosimulator.ConcursoSimulatorApplication;
import br.com.concursosimulator.identity.infrastructure.AdministrativeSecurity;
import br.com.concursosimulator.identity.web.UserPrincipal;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Ferramenta manual (#173 grupo 8 / #177): sobe o ambiente local paralelo da épica combinada a partir de um
 * backup privado, importa os pacotes versionados pela importação administrativa HTTP e registra SUBMIT e
 * APPROVE pelo lote editorial HTTP da #177. Depois confere a cobertura elegível pelo banco (somente leitura)
 * e pelos contratos públicos e mantém o ambiente no ar para o teste do usuário.
 *
 * <p>Somente classpath de teste; nada é habilitado por startup/Flyway/CI. O backup, a chave de confirmação
 * (gerada em memória) e as sessões nunca são gravados no relatório. O banco é efêmero (Testcontainers):
 * encerrar o processo descarta o destino; a recuperação é repetir esta ferramenta sobre o mesmo backup.
 *
 * <p>Uso: {@code --dump <backup> --sha256 <hex> --packages <dir> --report <json> [--ports ui,api]}
 */
public final class QuestionExpansionLocalEnablement {
    static final List<String> ALL_CHECKS = List.of("ACCESSIBILITY", "CLARITY", "CLASSIFICATION", "CORRECTNESS", "DISTRACTORS", "SOURCES", "STEP_BY_STEP");
    static final JsonMapper JSON = JsonMapper.builder().build();
    final HttpClient http = HttpClient.newHttpClient();
    int apiPort;
    String cookie, csrf;

    public static void main(String[] args) throws Exception {
        var options = new HashMap<String, String>();
        for (int i = 0; i + 1 < args.length; i += 2) options.put(args[i], args[i + 1]);
        for (var required : List.of("--dump", "--sha256", "--packages", "--report"))
            if (!options.containsKey(required)) throw new IllegalArgumentException("Uso: --dump <backup> --sha256 <hex> --packages <dir> --report <json> [--ports ui,api]");
        var ports = options.getOrDefault("--ports", "5371,19160").split(",");
        new QuestionExpansionLocalEnablement().run(Path.of(options.get("--dump")), options.get("--sha256"), Path.of(options.get("--packages")),
                Path.of(options.get("--report")), Integer.parseInt(ports[0].trim()), Integer.parseInt(ports[1].trim()));
    }

    void run(Path dump, String expectedSha, Path packages, Path reportPath, int uiPort, int apiPort) throws Exception {
        this.apiPort = apiPort;
        var dumpBytes = Files.readAllBytes(dump.toAbsolutePath().normalize());
        if (!sha256(dumpBytes).equals(expectedSha)) throw new IllegalArgumentException("Backup com SHA-256 divergente");
        var manifest = JSON.readTree(Files.readAllBytes(packages.resolve("manifest.json")));
        var started = Instant.now();

        var pg = new PostgreSQLContainer("postgres:18.6"); pg.start();
        ConfigurableApplicationContext app = null;
        try {
            pg.copyFileToContainer(org.testcontainers.utility.MountableFile.forHostPath(dump.toAbsolutePath().normalize().toString()), "/tmp/local-restore.dump");
            var restored = pg.execInContainer("pg_restore", "-U", pg.getUsername(), "-d", pg.getDatabaseName(), "--exit-on-error", "--no-owner", "--no-privileges", "/tmp/local-restore.dump");
            if (restored.getExitCode() != 0) throw new IllegalStateException("Restauração falhou");
            pg.execInContainer("rm", "--", "/tmp/local-restore.dump");

            var key = new byte[32]; new SecureRandom().nextBytes(key);
            var destination = "question-expansion-local-" + UUID.randomUUID().toString().replace("-", "");
            app = new SpringApplicationBuilder(ConcursoSimulatorApplication.class).run(
                    "--spring.profiles.active=local", "--server.port=" + apiPort, "--server.address=127.0.0.1",
                    "--spring.datasource.url=" + pg.getJdbcUrl(), "--spring.datasource.username=" + pg.getUsername(), "--spring.datasource.password=" + pg.getPassword(),
                    "--app.cors-origins=http://127.0.0.1:" + uiPort, "--spring.session.timeout=30m", "--app.google.enabled=false",
                    "--app.question-import.destination-id=" + destination, "--app.question-import.confirmation-key=" + Base64.getEncoder().encodeToString(key));
            Arrays.fill(key, (byte) 0);
            var jdbc = app.getBean(JdbcTemplate.class);
            var flyway = jdbc.queryForList("select version from flyway_schema_history where success and version is not null order by installed_rank", String.class);
            var before = counts(jdbc);

            // Operador ADM do backup. Sessão criada no repositório JDBC do Spring Session, como nas fixtures
            // de teste: nenhuma senha é digitada nem lida.
            var admin = jdbc.queryForMap("select owner_id, auth_version, enabled, must_change from admin_registration where id=1");
            boolean provisioned = false;
            if (admin.get("owner_id") == null || !Boolean.TRUE.equals(admin.get("enabled")) || Boolean.TRUE.equals(admin.get("must_change"))) {
                var operator = admin.get("owner_id") == null ? UUID.randomUUID() : (UUID) admin.get("owner_id");
                jdbc.update("insert into app_user(id) values (?) on conflict do nothing", operator);
                jdbc.update("update admin_registration set owner_id=?, enabled=true, must_change=false where id=1", operator);
                provisioned = true;
                admin = jdbc.queryForMap("select owner_id, auth_version from admin_registration where id=1");
            }
            cookie = session(app, (UUID) admin.get("owner_id"), ((Number) admin.get("auth_version")).longValue());
            csrf = JSON.readTree(get("/api/v1/csrf").body()).get("token").asString();

            var enabled = new ArrayList<Map<String, Object>>();
            for (var entry : manifest.get("packages")) {
                var topic = entry.get("topicId").asString(); var purpose = entry.get("purpose").asString();
                var requestBytes = Files.readAllBytes(packages.resolve(purpose).resolve(topic).resolve("preview-request.json"));
                if (!sha256(requestBytes).equals(entry.get("requestSha256").asString())) throw new IllegalStateException(topic + "/" + purpose + ": SHA-256 diverge do manifesto");
                var request = (ObjectNode) JSON.readTree(requestBytes);
                var preview = ok(post("/api/v1/admin/question-imports/preview", JSON.writeValueAsString(request)));
                if (!preview.get("packageDigest").asString().equals(entry.get("packageDigest").asString())
                        || !preview.get("selectionDigest").asString().equals(entry.get("selectionDigest").asString())
                        || !preview.get("confirmable").asBoolean())
                    throw new IllegalStateException(topic + "/" + purpose + ": prévia com digest divergente ou não confirmável");
                request.put("confirmationToken", preview.get("confirmationToken").asString());
                var receipt = ok(post("/api/v1/admin/question-imports/apply", JSON.writeValueAsString(request))).path("receipt");
                var nonce = receipt.path("confirmation").path("nonce").asString();
                var submit = batch(nonce, "SUBMIT", null, List.of(), "Habilitação local #173 (pacotes corrigidos de 13/11/2026): envio à revisão");
                var approve = batch(nonce, "APPROVE", "AI_ONLY", ALL_CHECKS,
                        "Habilitação local #173 (pacotes corrigidos de 13/11/2026): revisão individual assistida por IA, sem revisão humana");
                var row = new LinkedHashMap<String, Object>();
                row.put("topicId", topic); row.put("purpose", purpose); row.put("items", entry.get("items").asInt());
                row.put("requestSha256", entry.get("requestSha256").asString());
                row.put("packageDigest", entry.get("packageDigest").asString()); row.put("selectionDigest", entry.get("selectionDigest").asString());
                row.put("importReceipt", nonce); row.put("submitBatch", submit); row.put("approveBatch", approve);
                enabled.add(row);
            }

            var report = new LinkedHashMap<String, Object>();
            report.put("format", "question-expansion-destination-coverage/v2");
            report.put("observedAt", Instant.now().toString());
            report.put("destination", Map.of("logical", destination,
                    "environment", "ambiente local paralelo da épica combinada (UI " + uiPort + ", API " + apiPort + "), PostgreSQL 18.6 efêmero restaurado do backup privado pré-#177",
                    "sourceBackupSha256", expectedSha, "flyway", flyway, "operatorProvisionedInEphemeralCopy", provisioned));
            report.put("before", before);
            report.put("enablement", Map.of("mechanism", "#177: importação administrativa HTTP e decisões editoriais em lote HTTP (SUBMIT e APPROVE AI_ONLY)",
                    "packages", enabled.size(), "elapsedSeconds", Duration.between(started, Instant.now()).toSeconds()));
            report.put("database", database(jdbc, manifest, packages));
            report.put("http", http(manifest));
            report.put("after", counts(jdbc));
            report.put("packages", enabled);
            report.put("notCounted", "Questões sintéticas pré-existentes do backup; o banco do operador/VPS não foi consultado.");
            Files.createDirectories(reportPath.toAbsolutePath().getParent());
            Files.write(reportPath, JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(report));
            System.out.println("QUESTION_EXPANSION_LOCAL_READY: UI " + uiPort + " / API " + apiPort + " / relatório " + reportPath);
        } catch (Exception failure) { if (app != null) app.close(); pg.stop(); throw failure; }
        var running = app;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { running.close(); pg.stop(); }));
        new java.util.concurrent.CountDownLatch(1).await();
    }

    /** Prévia e aplicação de um lote pelo recibo de importação; devolve o nonce do recibo do lote. */
    String batch(String importNonce, String action, String kind, List<String> checks, String rationale) throws Exception {
        var envelope = JSON.createObjectNode().put("action", action);
        if (kind == null) envelope.putNull("reviewKind"); else envelope.put("reviewKind", kind);
        var list = envelope.putArray("checklist"); checks.forEach(list::add);
        envelope.put("rationale", rationale); envelope.putNull("items"); envelope.put("importReceiptId", importNonce);
        var preview = ok(post("/api/v1/admin/editorial-decisions/preview", JSON.writeValueAsString(envelope)));
        if (!preview.get("confirmable").asBoolean()) throw new IllegalStateException("Lote " + action + " não confirmável: " + preview);
        var apply = JSON.createObjectNode().put("action", action);
        apply.set("reviewKind", preview.get("reviewKind")); apply.set("checklist", preview.get("checklist"));
        apply.put("rationale", preview.get("rationale").asString()); apply.set("items", preview.get("expected"));
        apply.set("confirmationToken", preview.get("confirmationToken"));
        var applied = ok(post("/api/v1/admin/editorial-decisions/apply", JSON.writeValueAsString(apply)));
        if (applied.get("replayed").asBoolean()) throw new IllegalStateException("Lote inesperadamente repetido");
        return applied.path("receipt").path("confirmation").path("nonce").asString();
    }

    Map<String, Object> database(JdbcTemplate jdbc, JsonNode manifest, Path packages) throws Exception {
        return jdbc.execute((java.sql.Connection connection) -> {
            connection.setReadOnly(true);
            try (var statement = connection.prepareStatement("""
                    select r.fingerprint, r.topic_id, r.status from qb_question q join qb_revision r on r.id = q.eligible_revision_id
                    where q.origin_namespace = ? and q.origin_key = ?""")) {
                int missing = 0, mismatched = 0, cellMismatch = 0, originals = 0, simulado = 0, pratica = 0, minItems = Integer.MAX_VALUE, minOriginals = Integer.MAX_VALUE;
                var expected = new HashSet<String>();
                for (var entry : manifest.get("packages")) {
                    var topic = entry.get("topicId").asString(); var purpose = entry.get("purpose").asString();
                    var dir = packages.resolve(purpose).resolve(topic);
                    var request = readJson(dir.resolve("preview-request.json"));
                    var originalKeys = new HashSet<String>();
                    for (var d : readJson(dir.resolve("dossier.json"))) if ("ORIGINAL_DE_PROVA".equals(d.path("originKind").asString(""))) originalKeys.add(d.get("key").asString());
                    int items = 0, cellOriginals = 0;
                    for (var item : request.get("package").get("items")) {
                        var namespace = item.get("origin").get("namespace").asString(); var key = item.get("origin").get("key").asString();
                        expected.add(namespace + "\u0000" + key);
                        statement.setString(1, namespace); statement.setString(2, key);
                        try (var rows = statement.executeQuery()) {
                            if (!rows.next()) { missing++; continue; }
                            if (!rows.getString(1).equals(item.get("fingerprint").asString()) || !"APPROVED".equals(rows.getString(3))) mismatched++;
                            if (!rows.getString(2).equals(topic)) cellMismatch++;
                        }
                        items++; if (originalKeys.contains(key)) cellOriginals++;
                    }
                    if ("SIMULADO".equals(purpose)) simulado += items; else pratica += items;
                    originals += cellOriginals; minItems = Math.min(minItems, items); minOriginals = Math.min(minOriginals, cellOriginals);
                }
                int extra = 0, eligibleExpansion = 0;
                try (var rows = connection.createStatement().executeQuery("select origin_namespace, origin_key from qb_question where eligible_revision_id is not null and origin_key like 'admin.2026-11.%'")) {
                    while (rows.next()) { eligibleExpansion++; if (!expected.contains(rows.getString(1) + "\u0000" + rows.getString(2))) extra++; }
                }
                var result = new LinkedHashMap<String, Object>();
                result.put("eligibleTotal", jdbc.queryForObject("select count(*) from qb_question where eligible_revision_id is not null", Integer.class));
                result.put("eligibleExpansion", eligibleExpansion);
                result.put("missingFromPackages", missing); result.put("extraBeyondPackages", extra);
                result.put("fingerprintOrStatusMismatches", mismatched); result.put("cellMismatches", cellMismatch);
                result.put("cells", manifest.get("packages").size()); result.put("minItemsPerCell", minItems); result.put("minOriginalsPerCell", minOriginals);
                result.put("simulado", simulado); result.put("pratica", pratica); result.put("originals", originals);
                return result;
            } finally { connection.setReadOnly(false); }
        });
    }

    Map<String, Object> http(JsonNode manifest) throws Exception {
        var topics = new TreeSet<String>(); for (var entry : manifest.get("packages")) topics.add(entry.get("topicId").asString());
        var perTopic = new ArrayList<Map<String, Object>>(); int maximumMismatch = 0; boolean noStore = true;
        for (var topic : topics) {
            var availability = get("/api/v1/lesson-practices/availability?topicId=" + topic);
            var questions = get("/api/v1/question-bank/questions?topicId=" + topic + "&size=1");
            noStore &= availability.headers().firstValue("Cache-Control").orElse("").contains("no-store");
            var a = JSON.readTree(availability.body()); var q = JSON.readTree(questions.body());
            if (a.get("maximum").asInt() != 5) maximumMismatch++;
            perTopic.add(Map.of("topicId", topic, "practiceAvailable", a.get("available").asInt(), "practiceMaximum", a.get("maximum").asInt(), "simuladoTotal", q.get("total").asLong()));
        }
        return Map.of("endpoints", List.of("GET /api/v1/lesson-practices/availability?topicId=…", "GET /api/v1/question-bank/questions?topicId=…"),
                "topics", topics.size(), "maximumMismatches", maximumMismatch, "availabilityNoStore", noStore, "perTopic", perTopic);
    }

    static Map<String, Object> counts(JdbcTemplate jdbc) {
        var result = new LinkedHashMap<String, Object>();
        for (var table : List.of("qb_question", "qb_revision", "qb_decision", "qb_import_receipt", "qb_decision_batch", "app_user", "me_attempt"))
            result.put(table, jdbc.queryForObject("select count(*) from " + table, Integer.class));
        result.put("eligible", jdbc.queryForObject("select count(*) from qb_question where eligible_revision_id is not null", Integer.class));
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static String session(ConfigurableApplicationContext app, UUID owner, long version) {
        SessionRepository<Session> repository = (SessionRepository) app.getBean(JdbcIndexedSessionRepository.class);
        var session = repository.createSession();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(new UserPrincipal(owner), null, List.of());
        authentication.setDetails(new AdministrativeSecurity.Evidence(version));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(authentication));
        repository.save(session);
        return "SESSION=" + session.getId();
    }

    HttpResponse<String> get(String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + apiPort + path)).timeout(Duration.ofSeconds(60)).GET();
        if (cookie != null) request.header("Cookie", cookie);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> post(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + apiPort + path)).timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json").header("Cookie", cookie).header("X-CSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(body));
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    static JsonNode ok(HttpResponse<String> response) {
        if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body().substring(0, Math.min(500, response.body().length())));
        return JSON.readTree(response.body());
    }

    static JsonNode readJson(Path path) {
        try { return JSON.readTree(Files.readAllBytes(path)); } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }

    static String sha256(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
}
