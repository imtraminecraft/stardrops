package dev.fancydrops;

import java.net.http.HttpClient;

/** One web client for the whole mod: each new HttpClient starts its own threads and connection pool, so they are shared, not made per request. */
final class Net {
    private Net() {}
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
}
