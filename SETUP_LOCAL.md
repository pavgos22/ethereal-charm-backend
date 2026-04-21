# Uruchomienie projektu lokalnie (profil `local`)

Ten dokument opisuje, jak postawić backend e-commerce'u na localu z bazą PostgreSQL skonfigurowaną wg screena (`ethereal@localhost:5433`, user `postgres`, hasło `root`). Profil produkcyjny (`prod`) zakładał Docker Compose z hostami `db`, `eureka`, `gateway` itp. — profil `local` zastępuje je `localhost`.

## 1. Wymagania

- Java 17 (temurin/corretto — cokolwiek)
- Maven 3.9+ (albo wbudowany wrapper `./mvnw`)
- PostgreSQL 17 z bazą `ethereal` (user `postgres` / hasło `root`) na porcie **5433** — dokładnie jak na screenie w IntelliJ Data Sources
- IntelliJ IDEA z zainstalowaną wtyczką Spring Boot (domyślnie w Ultimate)
- Opcjonalnie: Docker (dla kontenera FTP, jeśli chcesz testować upload obrazków)

## 2. Inicjalizacja bazy (jednorazowo)

Baza została skasowana razem z VPS-em, ale **nic ręcznie tworzyć nie trzeba**. Hibernate (`ddl-auto: update`) sam wygeneruje wszystkie tabele w schemacie `public` przy pierwszym starcie serwisów. Wszystkie mikroserwisy korzystają z tej samej bazy `ethereal` i tego samego schematu — tak jak działało na prodzie.

> Rozważane było rozdzielenie na osobne schematy (`auth`, `product`, `cart`, ...), ale niektóre serwisy (np. `product-service/AdminOrdersService`) robią natywne SQL-e cross-service (np. `SELECT ... FROM orders JOIN deliver ...` i `FROM users`). Przy osobnych schematach te zapytania by się wywalały, więc zostaje jedno wspólne `public`.

## 3. Import w IntelliJ

1. `File → Open` → wskaż folder `e-commerce` (ten z głównym `pom.xml`)
2. IntelliJ wykryje projekt Maven typu `pom` z modułami — poczekaj aż zaindeksuje i zaciągnie zależności
3. Zaakceptuj Java 17 jako SDK projektu (`File → Project Structure → Project SDK`)

## 4. Uruchomienie serwisów

W `.run/` są gotowe Run Configuration dla każdego serwisu z aktywnym profilem `local`. Po zaimportowaniu projektu powinny pojawić się w rozwijanej liście konfiguracji u góry okna IDE.

**Kolejność uruchamiania ma znaczenie** (Eureka musi być pierwsza, żeby reszta mogła się zarejestrować):

1. `1 - Eureka [local]` — port **8761**, dashboard: <http://localhost:8761>
2. `2 - Gateway [local]` — port **8888** (reverse proxy do reszty)
3. `3 - Auth [local]` — port **9898**
4. `4 - Product [local]` — port **8080**
5. `5 - Cart [local]` — port **9999**
6. `6 - Order [local]` — port **8881**
7. `7 - File [local]` — port **8088**

Dla wygody jest też compound config `0 - All Services [local]`, który odpala wszystko naraz — ale przy pierwszym starcie polecam uruchamiać kolejno i patrzeć w logi.

Przy pierwszym starcie każdego serwisu z bazą Hibernate wygeneruje tabele w odpowiednim schemacie (`auth.*`, `product.*` itd.).

## 5. Alternatywnie — uruchomienie z linii poleceń

Z każdego modułu (np. `cd auth`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Lub ustaw zmienną środowiskową:

```bash
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

## 6. Co z FTP, SMTP, PayU?

Te usługi nie są wymagane do uruchomienia serwisów (beany tworzą się leniwie). Ale jeśli chcesz przetestować konkretny przepływ:

### FTP (file-service) — upload obrazków produktów

Postaw kontener z danymi z produkcyjnego `.env` (te same, które są w `application-local.yml`):

```bash
docker run -d --name ftp -p 21:21 -p 21011-21020:21011-21020 \
  -e USERS="sklep|4o1n3k2JWc41|/home/sklep|10000" \
  delfer/alpine-ftp-server
```

> Wcześniejsza wersja README.md proponowała user `ethereal/ethereal_pass/home/ethereal` — te dane są nieaktualne. Profil `local` używa realnych credentiali z `.env`, żeby nie musieć migrować ścieżek w `image_data.pathfile` (tam jest pełny prefix `/home/sklep/...`).

### SMTP (auth, order) — maile, 2FA, reset hasła

Profil `local` używa tego samego SMTP co prod: `serwer2528012.home.pl:465` (SSL), konto `shop@ethereal-charm.pl`. Czyli **maile wyjdą na realnie**, do prawdziwych skrzynek — uważaj podczas testów. Jeśli chcesz lokalnie łapać je w sandbox, podmień wartości `notification.*` w `auth/application-local.yml` i `order/application-local.yml` na MailHoga (`localhost:1025`, `ssl: false`) i odpal:

```bash
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

Maile będziesz wtedy oglądał na <http://localhost:8025>.

### PayU (order) — płatności

Zostawione jako placeholdery (`dev/dev/dev`). Żeby testować integrację — załóż konto sandbox na <https://developers.payu.com/> i podstaw wartości w `order/src/main/resources/application-local.yml`.

## 7. Weryfikacja, że działa

Po wystartowaniu wszystkiego:

- Eureka dashboard: <http://localhost:8761> — powinno być 6 zarejestrowanych instancji (gateway, auth, product, cart, order, file)
- Health gatewaya: <http://localhost:8888/actuator/health>
- Endpoint publiczny przez gateway, np. `GET http://localhost:8888/api/v1/product` (w zależności od kontrolerów)

Tabele powinny być widoczne w IntelliJ Database tool po rozwinięciu `ethereal → schemas → auth/product/cart/orders/files`.

## 8. Co zostało zmienione względem profilu `prod`

| Kategoria | prod | local |
|---|---|---|
| Host DB | `db` | `localhost:5433` |
| Host Eureki | `eureka:8761` | `localhost:8761` |
| Host Gatewaya | `gateway:8888` | `localhost:8888` |
| Host Order | `order:8881` | `localhost:8881` |
| Host FTP | `ftp:21` | `localhost:21` |
| CORS frontu | `https://ethereal-charm.pl` | `http://localhost:4200` |
| Flyway | disabled | disabled |
| Hibernate DDL | (brak) | `update` — generuje schemat |
| Schemat DB | `public` | `public` (bez zmian) |

Konfiguracje `application-prod.yml` **nie zostały ruszone** — produkcja (gdybyś kiedyś stawiał nowy VPS) dalej działa po staremu przez Docker Compose i `.env`.

## 9. Struktura nowych plików

```
e-commerce/
├── SETUP_LOCAL.md                                ← ten dokument
├── .run/                                         ← Run Configurations IntelliJ
│   ├── 0 - All Services [local].run.xml
│   ├── 1 - Eureka [local].run.xml
│   ├── 2 - Gateway [local].run.xml
│   ├── 3 - Auth [local].run.xml
│   ├── 4 - Product [local].run.xml
│   ├── 5 - Cart [local].run.xml
│   ├── 6 - Order [local].run.xml
│   └── 7 - File [local].run.xml
├── auth/src/main/resources/application-local.yml
├── cart/src/main/resources/application-local.yml
├── eureka/src/main/resources/application-local.yml
├── file-service/src/main/resources/application-local.yml
├── gateway/src/main/resources/application-local.yml
├── order/src/main/resources/application-local.yml
└── product-service/src/main/resources/application-local.yml
```

## 10. Częste problemy

**`Connection refused (5433)`** — upewnij się, że PostgreSQL nasłuchuje na porcie **5433**, nie domyślnym 5432 (tak ustawił to IntelliJ na screenie). Sprawdź `postgresql.conf` → `port` albo przemapuj kontener (`-p 5433:5432`).

**`FATAL: password authentication failed for user "postgres"`** — user to `postgres`, hasło `root`. Jeśli masz inne hasło, zmień w każdym `application-local.yml` albo nadpisz zmienną `spring.datasource.password` w Run Configuration.

**Serwis nie widzi Eureki** — Eureka musi już działać, zanim wystartuje Gateway i mikroserwisy. Compound config ignoruje tę kolejność (Spring Cloud ma własny retry), ale jeśli widzisz `com.netflix.discovery.shared.transport.TransportException` w logach — odczekaj 30s i serwis sam się dopnie, albo zrestartuj.

**Port zajęty** — domyślne porty mogą kolidować z Twoim lokalnym stosem (zwłaszcza 8080 dla product-service). Zmień w odpowiednim `application-local.yml` → `server.port`.

**FTP: `Connection refused: getsockopt` przy pobieraniu obrazka** — mimo że kontener `ftp` działa, file-service nie dobija się do portu. Dwie typowe przyczyny:

1. **Java na Windows resolwuje `localhost` do IPv6 (`::1`)**, a Docker słucha tylko IPv4 → stąd `ftp.server: 127.0.0.1` w configu (nie `localhost`).
2. **Kontener ma inne mapowanie niż myślisz.** Nasza produkcja / starszy lokal miał `-p 8001:21` (control FTP na hoście na 8001) i `-p 21000-21010:21000-21010` (passive). Sprawdź:

```powershell
docker ps
Test-NetConnection 127.0.0.1 -Port 8001   # albo 21, zaleznie od mapowania
```

Jeśli Twój kontener wystawia `0.0.0.0:8001->21/tcp` — zostaw `ftp.port: 8001` w `file-service/application-local.yml`. Jeśli stawiasz od nowa komendą z sekcji 6 (standardowo `-p 21:21`) — zmień na `21`.

Jeśli `Test-NetConnection` zwraca `TcpTestSucceeded: False` na obu portach — Windows Defender Firewall blokuje. Port 21 bywa zablokowany domyślnie, 8001 zwykle nie, ale warto sprawdzić regułą:

```powershell
New-NetFirewallRule -DisplayName "FTP local dev" -Direction Inbound -Protocol TCP -LocalPort 8001 -Action Allow
```

**FTP: obrazki nie wyświetlają się, ale plik leci pusty / zły** — sprawdź, czy kontener FTP został postawiony z userem `sklep` i originem `/home/sklep`. Ścieżki w tabeli `image_data.pathfile` są zapisane jako pełne `/home/sklep/YYYY-MM-DD/...` (z produkcji) — jeśli user/origin się nie zgadza, `STOR/RETR` zwróci `550 Failed to change directory`.

**Auth: `The column name two_factor_enabled was not found in this ResultSet`** — występuje, gdy tabela `users` zaciągnięta ze starego dumpu nie ma kolumny `two_factor_enabled`, a encja już ją wymaga. Hibernate przy starcie próbuje dodać `ADD COLUMN two_factor_enabled BOOLEAN NOT NULL` i wywala się na istniejących wierszach (PG wymaga wartości domyślnej dla NOT NULL). Objaw po stronie frontu: `/register` i `/password-recovery` wracają na `/` (bo `auto-login` zwraca 500 i guard interpretuje to jako fail). Napraw ręcznie:

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN DEFAULT FALSE;
UPDATE users SET two_factor_enabled = FALSE WHERE two_factor_enabled IS NULL;
ALTER TABLE users ALTER COLUMN two_factor_enabled SET NOT NULL;
```

Podobny wzorzec stosuj przy każdej nowej kolumnie NOT NULL, która wejdzie do encji, a baza ma już starsze dane (PG nie pozwoli dodać `NOT NULL` do niepustej tabeli bez `DEFAULT`).

**Favourites: `ClassCastException: class java.lang.Float cannot be cast to class java.math.BigDecimal`** — w `auth-service/UserService.getFavouriteProducts` leci natywne SQL `JOIN` na tabeli `products` i wynik castowany jest na `BigDecimal`. Problem: `ProductEntity.price` był zadeklarowany jako `float` bez `columnDefinition`, więc Hibernate wygenerował kolumnę `real` (float4) — JDBC zwraca wtedy `Float`, a nie `BigDecimal`, co wywala cast. Po stronie frontu objawia się jako 403 na `/favourites` (produkt migra się na chwilę i znika).

W kodzie już to naprawione — `ProductEntity.price` i `discountedPrice` mają teraz `columnDefinition = "numeric(10,2)"`. Ale `ddl-auto: update` **nie zmienia typu istniejących kolumn** (tylko dodaje nowe), więc na starej bazie trzeba ręcznie:

```sql
ALTER TABLE products ALTER COLUMN price TYPE numeric(10,2) USING price::numeric;
ALTER TABLE products ALTER COLUMN discounted_price TYPE numeric(10,2) USING discounted_price::numeric;
```

Po ALTER-ach restart `product-service` + `auth` i ulubione działają.

**Checkout: `401 Unauthorized` od PayU + `cart doesn't exist` przy drugim kliknięciu "Zamów"** — PayU sandbox odrzuca placeholder-owe credentiale (`client-id: dev`). Przy pierwszym kliknięciu order-service zdąży zapisać zamówienie, zrobić HTTP-call do `cart-service` i wyczyścić koszyk, zanim pada na PayU → `@Transactional` rollbackuje order (inna baza), ale koszyk w cart-service **zostaje pusty** (to inna baza, poza lokalną transakcją). Drugi klik leci na pusty koszyk → `EmptyCartException` / "cart doesn't exist".

Dwa fixy w kodzie (już zaaplikowane):

1. `PayuService.createOrder` ma LOCAL BYPASS — jeśli `payu.client-id = dev`, cała integracja z PayU jest pomijana, a na front zwracany jest stub URL (`http://localhost:4200/payment-success?extOrderId=...`). Dzięki temu lokalnie da się przetestować cały checkout bez zakładania konta PayU sandbox.
2. `OrderService.createOrder` — `cartService.removeCart` przeniesione **po** sukcesie PayU. Jeśli PayU jednak padnie, koszyk zostaje — użytkownik może ponowić.

Żeby testować realną integrację z PayU lokalnie — załóż konto na <https://developers.payu.com/>, wygeneruj `client_id` i `client_secret`, podstaw je w `order/src/main/resources/application-local.yml` (sekcja `payu:`). Bypass aktywuje się **tylko** gdy `client-id` to dokładnie `dev`.
