docker run -d -p 4317:4317 -p 14250:14250 -p 14268:14268 -p 16686:16686 \
--name jaeger-all-in-one -e COLLECTOR_OTLP_ENABLED=true --replace \
quay.io/jaegertracing/all-in-one:latest 