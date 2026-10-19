import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** Container readiness; no session, credentials, database driver or diagnostic payload. */
public final class Health {
    public static void main(String[] args) {
        try {
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/api/v1/status"))
                    .timeout(Duration.ofSeconds(3)).build();
            System.exit(client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200 ? 0 : 1);
        } catch (Exception unavailable) { System.exit(1); }
    }
}
