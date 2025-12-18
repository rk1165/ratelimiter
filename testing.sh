#!/usr/bin/env bash

for _ in {1..11}; do
  curl -s -w "\n" http://localhost:8080/api/v1/test
done

curl -H "X-User-Id: user123" http://localhost:8080/api/v1/test

curl -v -H "X-User-Id: user123" -H "X-User-Tier: enterprise" http://localhost:8080/api/v1/test

curl "http://localhost:8080/api/v1/limit/status?key=ip:127.0.0.1"

for _ in {1..11}; do
  curl -s -w "\n" -H "X-User-Id: user123" http://localhost:8080/api/v1/test
done

curl "http://localhost:8080/api/v1/limit/status?key=ip:127.0.0.1"

curl http://localhost:8080/api/v1/limit/config

curl -X POST "http://localhost:8080/api/v1/limit/reset?key=ip:127.0.0.1"

curl -4 http://localhost:8080/api/v1/jokes/pun
curl -4 http://localhost:8080/api/v1/jokes/knock-knock
