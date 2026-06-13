# Módulo 5 – Guia de Observabilidade

## Stack completa local com Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:

  # ── Redis ──────────────────────────────────────────────────
  redis:
    image: redis:7-alpine
    ports: [ "6379:6379" ]

  # ── Prometheus – coleta métricas ───────────────────────────
  prometheus:
    image: prom/prometheus:latest
    ports: [ "9090:9090" ]
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=7d'

  # ── Grafana – dashboards ────────────────────────────────────
  grafana:
    image: grafana/grafana:latest
    ports: [ "3000:3000" ]
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana_data:/var/lib/grafana

  # ── Zipkin – distributed tracing ───────────────────────────
  zipkin:
    image: openzipkin/zipkin:latest
    ports: [ "9411:9411" ]

  # ── Aplicação ───────────────────────────────────────────────
  app:
    image: ecommerce-observability:1.0.0
    ports: [ "8080:8080" ]
    environment:
      REDIS_HOST: redis
      REDIS_PORT: 6379
      SPRING_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans
    depends_on: [ redis, zipkin ]

volumes:
  grafana_data:
```

---

## prometheus.yml – configuração de scrape

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'ecommerce-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: [ 'app:8080' ]
    # Tags adicionadas pelo Prometheus (além das do Micrometer)
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
```

---

## Grafana – Dashboard JVM Micrometer

1. Acesse http://localhost:3000 (admin/admin)
2. Adicione Prometheus como datasource: http://prometheus:9090
3. Importe o Dashboard ID **4701** (JVM Micrometer – pronto para uso)
4. Importe o Dashboard ID **11378** (Spring Boot 3.x Statistics)

### Queries PromQL úteis para criar painéis customizados

```promql
# Requisições por segundo por endpoint
rate(http_server_requests_seconds_count{application="ecommerce-observability"}[5m])

# Latência p95 por endpoint
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{application="ecommerce-observability"}[5m])
)

# Taxa de erros (5xx)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
  /
rate(http_server_requests_seconds_count[5m])

# Uso de heap JVM
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Produtos ativos (Gauge customizado)
ecommerce_produtos_ativos_total

# Cache hit rate
rate(cache_gets_total{result="hit"}[5m])
  /
rate(cache_gets_total[5m])
```

---

## Alertmanager – SLOs de latência e erro

```yaml
# alert_rules.yml
groups:
  - name: ecommerce
    rules:
      # Alerta se p95 > 500ms nos últimos 5 minutos
      - alert: AltaLatencia
        expr: |
          histogram_quantile(0.95,
            rate(http_server_requests_seconds_bucket[5m])
          ) > 0.5
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Latência p95 acima de 500ms"

      # Alerta se taxa de erro > 5%
      - alert: AltaTaxaDeErro
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[5m])
            /
          rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Taxa de erros 5xx acima de 5%"
```

---

## Deploy com Spring Boot Buildpacks (sem Dockerfile)

```bash
# Gera imagem OCI sem precisar de Dockerfile
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=ecommerce-observability:1.0.0

# JVM flags para containers com memória limitada
# -XX:MaxRAMPercentage=75.0 usa 75% da memória do container
docker run \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport" \
  -m 512m \
  ecommerce-observability:1.0.0
```

---

## Endpoints disponíveis após subir a aplicação

| Endpoint | Descrição |
|----------|-----------|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/actuator/health | Status da aplicação |
| http://localhost:8080/actuator/prometheus | Métricas Prometheus |
| http://localhost:8080/actuator/metrics | Lista de métricas |
| http://localhost:8080/actuator/info | Informações da aplicação |
| http://localhost:8080/actuator/caches | Caches ativos |
| http://localhost:8080/actuator/loggers | Alterar log levels em runtime |
| http://localhost:8080/h2-console | Console H2 (dev) |
| http://localhost:9090 | Prometheus |
| http://localhost:3000 | Grafana |
| http://localhost:9411 | Zipkin (tracing) |
