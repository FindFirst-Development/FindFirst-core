#syntax=docker/dockerfile:1.7-labs
FROM gradle:8.9-jdk21-alpine AS builder
WORKDIR /app
COPY --exclude=build/ . .
RUN gradle assemble --no-daemon

FROM openjdk:26-ea-slim  AS runner
WORKDIR /app
COPY --from=builder /app/build/libs/findfirst-1.0.0.jar findfirst.jar
COPY scripts/ scripts/

ENTRYPOINT ["scripts/entrypoint.sh"]
