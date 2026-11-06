#!/usr/bin/env bash
# End-to-end smoke test against a running stack (docker compose up).
# Goes through nginx -> gateway -> each service, exactly like the browser does.
#   Usage: scripts/smoke-test.sh [base-url]   (default http://localhost:3000)
set -euo pipefail

BASE="${1:-http://localhost:3000}"
PASSWORD="${APP_SEED_DEMO_PASSWORD:-Demo@1234}"
TIMEOUT_S="${SMOKE_TIMEOUT_S:-240}"

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31mFAIL\033[0m %s\n' "$*" >&2; exit 1; }

login() { # prints the access token for $1
  curl -fsS -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' \
       -d "{\"username\":\"$1\",\"password\":\"$PASSWORD\"}" \
    | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

expect_status() { # expect_status <code> <method> <path> <token> [body]
  local want=$1 method=$2 path=$3 token=$4 body=${5:-}
  local got
  got=$(curl -s -o /dev/null -w '%{http_code}' -X "$method" "$BASE$path" \
        -H "Authorization: Bearer $token" -H 'Content-Type: application/json' ${body:+-d "$body"})
  [[ "$got" == "$want" ]] || fail "$method $path as token-holder: expected $want, got $got"
  printf '  %-6s %-45s %s\n' "$method" "$path" "$got"
}

log "Waiting for $BASE to accept a login (up to ${TIMEOUT_S}s)"
deadline=$(( $(date +%s) + TIMEOUT_S ))
until NURSE=$(login nurse 2>/dev/null) && [[ -n "$NURSE" ]]; do
  (( $(date +%s) < deadline )) || fail "stack did not come up in ${TIMEOUT_S}s"
  sleep 5
done
log "Signed in as nurse"

DOCTOR=$(login doctor);   [[ -n "$DOCTOR" ]]  || fail "doctor login"
LABTECH=$(login labtech); [[ -n "$LABTECH" ]] || fail "labtech login"

log "Role checks through the gateway"
expect_status 200 GET  /api/v1/auth/doctors              "$NURSE"
expect_status 200 GET  /api/v1/opd/visits/triage-queue   "$NURSE"
expect_status 403 GET  /api/v1/opd/visits/doctor/queue   "$NURSE"
expect_status 200 GET  /api/v1/opd/visits/doctor/queue   "$DOCTOR"
expect_status 200 GET  /api/v1/lab/tests                 "$DOCTOR"
expect_status 200 GET  /api/v1/lab/orders/queue          "$LABTECH"
expect_status 403 POST /api/v1/lab/orders                "$LABTECH" '{"visitId":1,"patientMrn":"JB-000001","testCodes":["CBC"]}'
expect_status 401 GET  /api/v1/opd/visits/today          "not-a-token"

log "Registering a patient end to end (OPD -> auth doctor lookup)"
PHONE="98$(date +%s | tail -c 9)"
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/opd/patients" \
  -H "Authorization: Bearer $NURSE" -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Smoke Test\",\"dateOfBirth\":\"1990-01-01\",\"gender\":\"OTHER\",\"phone\":\"$PHONE\",
       \"chiefComplaint\":\"CI smoke test\",\"priority\":\"NORMAL\",\"assignedDoctorUsername\":\"doctor\"}")
[[ "$code" == "201" ]] || fail "patient registration returned $code"
echo "  POST   /api/v1/opd/patients                      201"

log "Smoke test passed"
