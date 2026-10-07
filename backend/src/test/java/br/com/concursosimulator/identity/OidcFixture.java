package br.com.concursosimulator.identity;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Provedor OIDC efêmero, exclusivamente no classpath de testes. */
public final class OidcFixture implements AutoCloseable {
    private final HttpServer server;
    private final RSAKey key;
    private final Map<String, Ticket> codes = new ConcurrentHashMap<>();
    private final Map<String, String> accessSubjects = new ConcurrentHashMap<>();
    public volatile String mode = "valid";
    public volatile String subject = "subject-one";
    public volatile boolean pkceVerified;
    public volatile String lastIdToken;

    public OidcFixture() { this(0); }

    public OidcFixture(int port) {
        try {
            key = new RSAKeyGenerator(2048).keyID("fixture-key").generate();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            server.createContext("/.well-known/openid-configuration", exchange -> json(exchange, 200, Map.of(
                    "issuer", issuer(), "authorization_endpoint", issuer() + "/authorize", "token_endpoint", issuer() + "/token",
                    "jwks_uri", issuer() + "/jwks", "userinfo_endpoint", issuer() + "/userinfo",
                    "response_types_supported", List.of("code"), "subject_types_supported", List.of("public"),
                    "id_token_signing_alg_values_supported", List.of("RS256"))));
            server.createContext("/jwks", exchange -> json(exchange, 200, new JWKSet(key.toPublicJWK()).toJSONObject()));
            server.createContext("/authorize", this::authorize);
            server.createContext("/token", this::token);
            server.createContext("/userinfo", exchange -> {
                String bearer = exchange.getRequestHeaders().getFirst("Authorization");
                String user = accessSubjects.get(bearer == null ? "" : bearer.replace("Bearer ", ""));
                json(exchange, user == null ? 401 : 200, user == null ? Map.of("error", "invalid_token")
                        : Map.of("sub", user, "name", "Google fixture profile"));
            });
            server.createContext("/fixture/control", exchange -> {
                if (!exchange.getRequestMethod().equals("POST")) {
                    json(exchange, 405, Map.of("error", "method_not_allowed"));
                    return;
                }
                var params = parameters(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                subject = params.getOrDefault("subject", "browser-subject-one");
                mode = params.getOrDefault("mode", "valid");
                json(exchange, 200, Map.of("ready", true));
            });
            server.start();
        } catch (Exception exception) { throw new IllegalStateException("OIDC fixture não iniciou", exception); }
    }

    public String issuer() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    private void authorize(HttpExchange exchange) throws java.io.IOException {
        var params = parameters(exchange.getRequestURI().getRawQuery());
        String state = params.get("state");
        String location;
        if (mode.equals("cancel")) location = params.get("redirect_uri") + "?error=access_denied&state=" + encode(state);
        else {
            String code = UUID.randomUUID().toString();
            codes.put(code, new Ticket(params.get("nonce"), params.get("code_challenge"), subject, mode));
            location = params.get("redirect_uri") + "?code=" + code + "&state=" + encode(state);
        }
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private void token(HttpExchange exchange) throws java.io.IOException {
        try {
            var params = parameters(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            Ticket ticket = codes.remove(params.getOrDefault("code", ""));
            if (ticket == null) { json(exchange, 400, Map.of("error", "invalid_grant")); return; }
            String verifier = params.getOrDefault("code_verifier", "");
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            pkceVerified = !verifier.isEmpty() && challenge.equals(ticket.challenge());
            if (!pkceVerified) { json(exchange, 400, Map.of("error", "invalid_grant")); return; }
            Instant now = Instant.now();
            var claims = new JWTClaimsSet.Builder().issuer(ticket.mode().equals("issuer") ? "https://wrong.example" : issuer())
                    .subject(ticket.subject()).audience(ticket.mode().equals("audience") ? "wrong-client" : "test-client")
                    .issueTime(Date.from(now.minusSeconds(10)))
                    .expirationTime(Date.from(ticket.mode().equals("expired") ? now.minusSeconds(600) : now.plusSeconds(300)))
                    .claim("nonce", ticket.mode().equals("nonce") ? "wrong-nonce" : ticket.nonce())
                    .claim("name", "Google fixture profile").build();
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
            RSAKey signingKey = ticket.mode().equals("signature") ? new RSAKeyGenerator(2048).generate() : key;
            jwt.sign(new RSASSASigner(signingKey));
            lastIdToken = jwt.serialize();
            String access = "fixture-access-" + UUID.randomUUID();
            accessSubjects.put(access, ticket.subject());
            json(exchange, 200, Map.of("access_token", access, "token_type", "Bearer", "expires_in", 300,
                    "scope", "openid profile", "id_token", lastIdToken));
        } catch (Exception exception) { json(exchange, 500, Map.of("error", "fixture_failure")); }
    }

    static Map<String, String> parameters(String query) {
        var values = new HashMap<String, String>();
        if (query != null && !query.isEmpty()) for (String item : query.split("&")) {
            var parts = item.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts.length == 2 ? parts[1] : "", StandardCharsets.UTF_8));
        }
        return values;
    }
    static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static void json(HttpExchange exchange, int status, Map<String, ?> body) throws java.io.IOException {
        byte[] bytes = JSONObjectUtils.toJSONString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
    @Override public void close() { server.stop(0); }
    private record Ticket(String nonce, String challenge, String subject, String mode) {}
}
