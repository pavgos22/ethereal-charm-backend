# Deploy na prod VPS

Ten dokument opisuje, jak postawić `ethereal-charm.pl` na świeżym VPS-ie (albo przenieść istniejący) używając Docker Compose + skrypt `deploy.sh`. Profil lokalny (IntelliJ + `application-local.yml`) opisany jest w `SETUP_LOCAL.md` — tam zostajesz dla codziennego developmentu.

## 1. Wymagania na VPS

- Ubuntu 22.04+ / Debian 12+ (dowolny inny z Dockerem też zadziała)
- Min. **2 vCPU, 4 GB RAM, 40 GB SSD** (Hetzner CX22 wystarczy, CX32 z zapasem)
- Otwarte porty: `80`, `443` (nginx/traefik), `21` + `21000-21010` (FTP), `22` (SSH)
- Docker Engine + Docker Compose v2: `curl -fsSL https://get.docker.com | sh`
- Użytkownik w grupie `docker`: `sudo usermod -aG docker $USER` (relogin)

## 2. Pierwszy deploy

```bash
# 1. Sklonuj repo na VPS
ssh user@vps
git clone git@github.com:<twoj-user>/e-commerce.git
cd e-commerce

# 2. Uzupełnij .env (skopiuj z lokalnego backupu albo wklej z 1Password/Bitwarden)
nano .env

# 3. Ustaw GH_PACKAGES_TOKEN jeśli używasz prywatnych pakietów Maven
export GH_PACKAGES_TOKEN=ghp_...

# 4. Jeśli frontend jest w osobnym repo — sklonuj go obok:
cd ..
git clone git@github.com:<twoj-user>/ethereal-frontend.git
cd e-commerce

# 5. Odpal deploy
./deploy.sh
```

`deploy.sh` robi dokładnie to, o co prosiłeś:
1. `db` + `ftp` → cooldown 10s
2. `eureka` → cooldown **45s**
3. `gateway` → cooldown **25s**
4. `auth` + `product` + `cart` + `order` + `file` równolegle → cooldown 30s
5. `frontend`

Cały pipeline zajmuje ~3-4 minuty przy pierwszym buildzie (potem szybciej, bo obrazy w cache).

## 3. Codzienne operacje

Wszystko chodzi przez `make`. Lista dostępnych komend: `make help`.

| Komenda | Co robi |
|---|---|
| `make deploy` | Pełny sekwencyjny start (pierwszy raz / po `make down`) |
| `make up` | Szybki start równoległy (Compose sam ogarnia kolejność przez `depends_on: service_healthy`) |
| `make down` | Zatrzymaj i usuń kontenery (volumes zostają!) |
| `make stop` | Tylko zatrzymaj, nie niszcz kontenerów |
| `make logs` | `tail -f` wszystkich logów |
| `make ps` | Status + healthcheck każdego kontenera |
| `make update` | Po `git pull` — przebuduj i zrestartuj tylko serwisy aplikacyjne (db i eureka nie ruszane) |
| `make restart-auth` | Restart pojedynczego serwisu |
| `make backup` | `pg_dump` do `./backups/ethereal-<timestamp>.sql.gz` |
| `make shell-db` | Otwórz `psql` w kontenerze bazy |
| `make clean` | **DESTRUKCYJNE** — kasuje volumes (czyli bazę!). Pyta o potwierdzenie. |

## 4. Aktualizacja kodu na prod

```bash
ssh user@vps
cd e-commerce
git pull
make update
```

`make update` przebuduje obrazy i restartuje **tylko serwisy aplikacyjne** (gateway, auth, product, cart, order, file). Eureka i baza nie są ruszane — downtime ograniczony do krótkiego restartu każdego mikroserwisu (sekundy).

Jeśli zmienił się schemat bazy w sposób, który Hibernate nie ogarnie sam przez `ddl-auto: update` (np. `NOT NULL` na istniejącej kolumnie, patrz `SETUP_LOCAL.md` → sekcja 10), **zrób backup przed update-em**: `make backup`.

## 5. Frontend

Domyślnie `docker-compose.yml` spodziewa się, że frontend jest w `../ethereal-frontend` (czyli obok katalogu `e-commerce`). Frontend musi mieć własny `Dockerfile` (multi-stage: `node:20-alpine` build → `nginx:alpine` serwuje `dist/`).

Jeśli frontend jest gdzie indziej — zmień `build.context` w sekcji `frontend:` w `docker-compose.yml`.

Jeśli nie chcesz frontendu w kontenerze (np. preferujesz build w CI i serwowanie przez hostowy nginx) — usuń serwis `frontend` z `docker-compose.yml` i w `deploy.sh` skrypt sam to wykryje (krok 5/5 pominie się z ostrzeżeniem).

## 6. Reverse proxy + HTTPS (nginx na hoście)

Compose wystawia **tylko**:
- `gateway` na `127.0.0.1:8888` (API)
- `frontend` na `127.0.0.1:4200` (UI)
- `ftp` na `0.0.0.0:21 + 21000-21010` (FTP musi być publiczny dla klientów FTP, ale credentiale pilnują dostępu)

Baza, Eureka, serwisy aplikacyjne **nie są wystawione** poza sieć Dockera. Dostęp tylko przez SSH tunnel jeśli potrzebny debug.

Minimalny nginx na hoście (`/etc/nginx/sites-available/ethereal-charm.pl`):

```nginx
server {
    listen 443 ssl http2;
    server_name ethereal-charm.pl www.ethereal-charm.pl;

    ssl_certificate     /etc/letsencrypt/live/ethereal-charm.pl/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ethereal-charm.pl/privkey.pem;

    # Frontend (Angular)
    location / {
        proxy_pass http://127.0.0.1:4200;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Backend API
    location /api/ {
        proxy_pass http://127.0.0.1:8888;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}

server {
    listen 80;
    server_name ethereal-charm.pl www.ethereal-charm.pl;
    return 301 https://$host$request_uri;
}
```

Let's Encrypt: `sudo certbot --nginx -d ethereal-charm.pl -d www.ethereal-charm.pl`.

## 7. Backup (minimum dyscypliny)

Cron na VPS-ie:

```bash
# Codziennie o 3:00 — backup + retencja 14 dni
0 3 * * * cd /home/user/e-commerce && make backup && find backups/ -name "ethereal-*.sql.gz" -mtime +14 -delete
```

Na prawdę bezpieczny backup powinien jeszcze iść na zewnątrz VPS-a (Backblaze B2, rsync na drugi serwer, S3 z Hetznera). Bez tego jeden kaboom = koniec sklepu.

```bash
# Przykład: rsync backupow na inny serwer raz dziennie
30 3 * * * rsync -az /home/user/e-commerce/backups/ backup-host:/srv/ethereal-backups/
```

## 8. Monitoring (niekonieczne, ale warto)

Polecam dwie rzeczy w minimum, obie **darmowe**:

- **healthchecks.io** — cron wysyła ping co 5 min, jeśli nie dojdzie → mail/Discord. Darmowe dla 20 checków.
- **Uptime Kuma** — self-hosted monitoring, własny kontener w compose, dashboard z historią uptime. Dobry do oczu, żeby widzieć że wszystko zielone.

## 9. Troubleshooting

**`deploy.sh` utknął na cooldownie i serwis nie wstał** — `make logs` i patrz co się pali. Najczęściej:
- Eureka: port 8761 zajęty na hoście (nie powinien być, ale sprawdź `lsof -i :8761`)
- Gateway: nie zarejestrował się w Euroce → patrz logi gatewaya na `TransportException`, 45s cooldownu zwykle wystarcza
- Serwisy aplikacyjne: baza nie wstała na czas → zwiększ `COOLDOWN_DB` w `deploy.sh`

**`make update` zrobił downtime dłuższy niż chciałem** — to normalne, bo Spring Boot startuje 15-30s per serwis. Jeśli potrzebujesz zero-downtime, musisz wejść w rolling update (dwie instancje per serwis + load balancer) — a to już skala Kubernetes. W Twojej skali prosty restart jest OK.

**Po `git pull` Hibernate wywala błąd przy starcie (`NOT NULL column ...`)** — masz nową kolumnę, której PG nie dodał bo tabela już ma wiersze. Patrz `SETUP_LOCAL.md` → sekcja 10 (`two_factor_enabled`), wzorzec ten sam:
```sql
ALTER TABLE xxx ADD COLUMN IF NOT EXISTS new_col <typ> DEFAULT <default>;
UPDATE xxx SET new_col = <default> WHERE new_col IS NULL;
ALTER TABLE xxx ALTER COLUMN new_col SET NOT NULL;
```
