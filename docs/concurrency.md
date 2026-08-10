# Concurrency model: virtual threads

Enabled via `spring.threads.virtual.enabled=true` in `application.yml`. Spring
Boot 3.2+ swaps Tomcat's request thread pool from platform threads to Java 21
virtual threads (Project Loom).

**What this means:**
- Blocking I/O on `RestClient` (Nominatim, NWS) parks the virtual thread cheaply (~1 KB stack) instead of holding an OS thread
- Same imperative code (controllers, use cases, providers) — no `Mono<T>` / `Flux<T>` rewriting
- Throughput for I/O-bound paths approaches reactive levels with zero code change
- `jvm_threads_live_threads` stays flat even under concurrent load (peak 23 observed during a 20-request burst)

**What we *don't* get:**
- Streaming responses (SSE) — none of our endpoints need it
- Full backpressure across the stack — `Bucket4j` + Nominatim's rate limits already do that

**Tradeoff accepted:** virtual threads require Java 21, which is already this
project's reference version.
