package com.myfitness.api.common.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class CorsAllowedOrigins {

    private static final String LOCAL_FRONTEND_ORIGIN = "http://localhost:3000";

    private CorsAllowedOrigins() {
    }

    static List<String> resolve(String configuredValue, boolean production) {
        String value = configuredValue == null ? "" : configuredValue.trim();
        if (value.isEmpty()) {
            if (production) {
                throw new IllegalArgumentException(
                        "MYFITNESS_CORS_ALLOWED_ORIGINS must be set when the prod or production profile is active");
            }
            return List.of(LOCAL_FRONTEND_ORIGIN);
        }

        LinkedHashSet<String> origins = new LinkedHashSet<>();
        Arrays.stream(value.split(",", -1))
                .map(String::trim)
                .map(origin -> normalize(origin, production))
                .forEach(origins::add);
        return List.copyOf(origins);
    }

    private static String normalize(String value, boolean production) {
        if (value.isEmpty() || value.contains("*")) {
            throw new IllegalArgumentException("CORS origins must be explicit and must not contain wildcards");
        }

        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid CORS origin", exception);
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null) {
            throw new IllegalArgumentException("CORS origins must be absolute http or https origins");
        }

        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))) {
            throw new IllegalArgumentException("CORS origins must not include credentials, paths, queries, or fragments");
        }

        if (production && !scheme.equals("https")) {
            throw new IllegalArgumentException("Production CORS origins must use https");
        }

        if (production && isLoopbackHost(uri.getHost())) {
            throw new IllegalArgumentException("Production CORS origins must not target localhost");
        }

        try {
            return new URI(scheme, null, uri.getHost().toLowerCase(Locale.ROOT), uri.getPort(), null, null, null)
                    .toString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid CORS origin", exception);
        }
    }

    private static boolean isLoopbackHost(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return normalizedHost.equals("localhost")
                || normalizedHost.endsWith(".localhost")
                || normalizedHost.startsWith("127.")
                || normalizedHost.equals("::1")
                || normalizedHost.equals("0:0:0:0:0:0:0:1");
    }
}
