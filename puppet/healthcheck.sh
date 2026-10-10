#!/bin/bash
URL=${1:-http://localhost:8084/produce}
code=$(curl -s -o /dev/null -w "%{http_code}" "$URL")
if [ "$code" = "200" ]; then
  echo "HEALTH CHECK PASSED (HTTP $code) - $URL"
else
  echo "HEALTH CHECK FAILED (HTTP $code) - $URL"
  exit 1
fi
