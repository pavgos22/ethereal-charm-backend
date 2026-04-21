#!/usr/bin/env bash
# deploy.sh — sekwencyjny start całego stacka ethereal-charm.pl.
# Uzywaj na prod VPS-ie do pierwszego uruchomienia albo po pelnym `make down`.
# Do aktualizacji pojedynczego serwisu wystarczy `docker compose up -d --build <nazwa>`.
#
# Kroki:
#   1. db + ftp (infrastruktura, nie zaleza od niczego)
#   2. eureka + COOLDOWN (service discovery musi byc gotowa zanim reszta sprobuje sie zarejestrowac)
#   3. gateway + COOLDOWN (reverse proxy + actuator health)
#   4. auth + product + cart + order + file (aplikacyjne mikroserwisy, startuja rownolegle)
#   5. frontend (na koncu, bo zaleza od gatewaya)

set -euo pipefail

# ─── Konfiguracja cooldownow (sekundy) ──────────────────────────────────
COOLDOWN_DB=10
COOLDOWN_EUREKA=45
COOLDOWN_GATEWAY=25
COOLDOWN_SERVICES=30

# ─── Kolor w terminalu (tylko jak ma TTY) ───────────────────────────────
if [[ -t 1 ]]; then
  GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
else
  GREEN=''; YELLOW=''; RED=''; NC=''
fi

log()  { printf "${GREEN}[deploy]${NC} %s\n" "$*"; }
warn() { printf "${YELLOW}[deploy]${NC} %s\n" "$*"; }
die()  { printf "${RED}[deploy]${NC} %s\n" "$*" >&2; exit 1; }

# ─── Walidacja srodowiska ────────────────────────────────────────────────
command -v docker >/dev/null 2>&1 || die "Docker nie zainstalowany. Zainstaluj i sproboj ponownie."
docker compose version >/dev/null 2>&1 || die "Docker Compose v2 nie dostepny (potrzebne 'docker compose', nie 'docker-compose')."
[[ -f .env ]] || die "Brak pliku .env w biezacym katalogu. Skopiuj .env.example i uzupelnij."
[[ -f docker-compose.yml ]] || die "Brak docker-compose.yml. Odpalaj skrypt z roota projektu."

# ─── Deploy ─────────────────────────────────────────────────────────────
log "Budowa obrazow (moze potrwac przy pierwszym razie)..."
docker compose build

log "1/5 → Infrastruktura: db + ftp"
docker compose up -d db ftp
log "    Cooldown ${COOLDOWN_DB}s (czekam az Postgres bedzie healthy)..."
sleep "${COOLDOWN_DB}"

log "2/5 → Service discovery: eureka"
docker compose up -d eureka
log "    Cooldown ${COOLDOWN_EUREKA}s (czekam az Eureka wstanie i przyjmie rejestracje)..."
sleep "${COOLDOWN_EUREKA}"

log "3/5 → Reverse proxy: gateway"
docker compose up -d gateway
log "    Cooldown ${COOLDOWN_GATEWAY}s (czekam az gateway zarejestruje sie w eurece)..."
sleep "${COOLDOWN_GATEWAY}"

log "4/5 → Mikroserwisy aplikacyjne: auth product cart order file"
docker compose up -d auth product cart order file
log "    Cooldown ${COOLDOWN_SERVICES}s (czekam az wszystkie sie zarejestruja)..."
sleep "${COOLDOWN_SERVICES}"

log "5/5 → Frontend"
if docker compose config --services | grep -q '^frontend$'; then
  docker compose up -d frontend || warn "Frontend nie wstal — sprawdz czy ../ethereal-frontend istnieje i ma Dockerfile. Backend dziala niezaleznie."
else
  warn "Frontend nie zdefiniowany w compose — pomijam."
fi

echo
log "Status kontenerow:"
docker compose ps

echo
log "Gotowe. Sprawdz:"
echo "  - Eureka dashboard: ssh -L 8761:localhost:8761 user@vps  i potem http://localhost:8761"
echo "  - Gateway health:   curl http://localhost:8888/actuator/health (z VPS)"
echo "  - Logi:             docker compose logs -f <serwis>"
