# Rate limiting

Bucket4j token buckets, distributed over Redis via Lettuce, applied to the
`/api/*` URL pattern only (actuator and Swagger UI are exempt).

## Configuration

```yaml
weather:
  rate-limit:
    enabled: true              # set false in tests so multi-request tests still pass
    burst:
      capacity: 5
      refill-period-seconds: 1
    sustained:
      capacity: 60
      refill-period-seconds: 60
```

Two-bandwidth limit per client IP:

| Bandwidth | Capacity | Refill | Why |
|---|---|---|---|
| **burst** | 5 | 5 tokens / second | Anti-hammer, Nominatim-friendly |
| **sustained** | 60 | 60 tokens / minute | Long-term cap |

A request consumes 1 token from *both* bandwidths; the more restrictive
decides. So a real user can burst 5 quick requests, but then must slow
to ~1/sec until the sustained bucket recovers (after ~60 seconds of
inactivity at full capacity).

## Client IP

Key is taken from `X-Forwarded-For` (first hop) → `request.getRemoteAddr()`
fallback. One bucket per IP, persisted in Redis with 10-minute TTL on
idle buckets so we don't accumulate one-off entries forever.

## 429 response

When a bucket is exhausted:

- HTTP `429 Too Many Requests`
- `Retry-After: <seconds>` header
- `X-RateLimit-Remaining: 0` header
- Body: RFC 9457 `ProblemDetail`

```json
{
  "type": "https://weather-wrapper-service.ythalorossy.io/errors/rate-limit-exceeded",
  "title": "Rate limit exceeded",
  "status": 429,
  "detail": "Rate limit exceeded. Try again in 1 seconds.",
  "properties": {
    "retryAfterSeconds": 1
  }
}
```

On allowed requests, the filter sets `X-RateLimit-Remaining: <count>`.

## Failure mode

If Redis is unreachable, the filter **fails open** (logs a warning and
allows the request through). Rate limiting is best-effort; a broken
limiter should not take down the API.

## Verification

```bash
$ for i in {1..7}; do curl -s -o /dev/null -w "req $i: HTTP %{http_code}\n" \
    "http://localhost:8080/api/v1/weather?city=Arlington,%20VA"; done
req 1: HTTP 200
req 2: HTTP 200
req 3: HTTP 200
req 4: HTTP 200
req 5: HTTP 200
req 6: HTTP 200        # burst refilled ~greedy during the loop
req 7: HTTP 429        # bucket exhausted, Retry-After: 1

$ curl -s -o /dev/null -w "%{http_code}\n" -H "X-Forwarded-For: 1.2.3.4" \
    "http://localhost:8080/api/v1/weather?city=Arlington,%20VA"
200                  # independent bucket per IP
```
