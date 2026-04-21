# Makefile — skroty do operowania prod stackiem ethereal-charm.pl.
# Wszystkie komendy odpalaj z roota repo (tam gdzie docker-compose.yml).
#
# Szybki start:
#   make deploy   — pierwszy deploy, sekwencyjny start z cooldownami (Eureka -> Gateway -> reszta)
#   make up       — `docker compose up -d` (rownolegle, Compose sam ogarnia kolejnosc przez depends_on)
#   make down     — zatrzymaj wszystko
#   make logs     — tail logow ze wszystkich serwisow
#   make ps       — status kontenerow
#   make update   — przebuduj zmienione obrazy i podmien tylko je (bez downtime db/eureki)
#   make restart-<serwis> — restart pojedynczego serwisu, np. `make restart-auth`
#   make backup   — pg_dump do ./backups/

.DEFAULT_GOAL := help
COMPOSE := docker compose
BACKUP_DIR := ./backups
TIMESTAMP := $(shell date +%Y%m%d-%H%M%S)

.PHONY: help deploy up down stop logs ps build update backup shell-db clean

help: ## Pokaz dostepne komendy
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

deploy: ## Pierwszy deploy — sekwencyjny start z cooldownami (uzyj po `make down` albo na swiezym VPS-ie)
	@./deploy.sh

up: ## Start wszystkiego rownolegle (szybsze niz deploy, Compose sam ogarnia kolejnosc przez depends_on)
	$(COMPOSE) up -d

down: ## Zatrzymaj wszystko (kontenery zniszczone, volumes zachowane)
	$(COMPOSE) down

stop: ## Zatrzymaj wszystko bez niszczenia kontenerow (szybszy restart przez `make up`)
	$(COMPOSE) stop

logs: ## Tail logow ze wszystkich serwisow (Ctrl+C zeby wyjsc)
	$(COMPOSE) logs -f --tail=100

ps: ## Status kontenerow + healthcheck
	$(COMPOSE) ps

build: ## Przebuduj obrazy bez startowania
	$(COMPOSE) build

update: ## Przebuduj zmienione obrazy i zrestartuj tylko je (backend rolling update)
	$(COMPOSE) build
	$(COMPOSE) up -d --no-deps --build auth product cart order file gateway
	@echo "Update done. `make ps` zeby sprawdzic."

restart-%: ## Restart pojedynczego serwisu, np. `make restart-auth`
	$(COMPOSE) restart $*

backup: ## Dump bazy do ./backups/ethereal-<timestamp>.sql.gz
	@mkdir -p $(BACKUP_DIR)
	@echo "Dump bazy do $(BACKUP_DIR)/ethereal-$(TIMESTAMP).sql.gz ..."
	@$(COMPOSE) exec -T db sh -c 'pg_dump -U $$POSTGRES_USER $$POSTGRES_DB' | gzip > $(BACKUP_DIR)/ethereal-$(TIMESTAMP).sql.gz
	@ls -lh $(BACKUP_DIR)/ethereal-$(TIMESTAMP).sql.gz

shell-db: ## Otworz psql w kontenerze bazy
	$(COMPOSE) exec db psql -U $$(grep POSTGRES_USER .env | cut -d= -f2) -d $$(grep POSTGRES_DB .env | cut -d= -f2)

clean: ## DESTRUKCYJNE: zatrzymaj wszystko i skasuj volumes (baza!). Uzywaj tylko na localu/devie.
	@read -p "Na pewno? To skasuje DANE W BAZIE. Wpisz 'yes' zeby kontynuowac: " ans; \
	if [ "$$ans" = "yes" ]; then $(COMPOSE) down -v; else echo "Anulowane."; fi
