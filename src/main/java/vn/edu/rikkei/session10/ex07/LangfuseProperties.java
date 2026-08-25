package vn.edu.rikkei.session10.ex07;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed binding for the {@code langfuse.*} properties in {@code application.yaml}.
 *
 * <p>Example YAML:
 * <pre>
 * langfuse:
 *   public-key: pk-lf-...
 *   secret-key:  sk-lf-...
 *   host:        http://localhost:3000
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "langfuse")
public class LangfuseProperties {

    /** Langfuse project public key (pk-lf-…). */
    private String publicKey;

    /** Langfuse project secret key (sk-lf-…). */
    private String secretKey;

    /** Base URL of the Langfuse server, e.g. {@code http://localhost:3000}. */
    private String host = "http://localhost:3000";

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
