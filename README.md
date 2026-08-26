# Java atrankos užduotis

Sprendimas įgyvendina REST API bendram pokalbių kambariui, SQL užduoties atsakymus ir atskirus automatizuotus API testus.

## Ką Projektas Daro

`chatroom` modulis yra pagrindinė Spring Boot aplikacija. Ji leidžia vartotojams prisijungti su JWT, rašyti žinutes į vieną bendrą public room ir skaityti žinutes nuo naujausios iki seniausios. Sistemoje yra dvi rolės: `USER` ir `ADMIN`.

`ADMIN` gali kurti vartotojus, šalinti vartotojus, šalinti žinutes, peržiūrėti vartotojus ir gauti vartotojo žinučių statistiką. Pašalinus vartotoją, jo žinutės lieka istorijoje, bet yra rodomos kaip `anonymous`.

Projektas taip pat turi:

- `automated-tests` - atskirą Spock/REST Assured modulį automatizuotiems API testams prieš veikiančią aplikaciją;
- `sql-task` - SQL užduoties atsakymus;
- `Dockerfile` ir `automated-tests.Dockerfile` lokaliam paleidimui per Docker.

Pagrindiniai endpoint'ai:

- `POST /api/auth/login`
- `GET /api/messages`
- `POST /api/messages`
- `POST /api/admin/users`
- `GET /api/admin/users`
- `GET /api/admin/users/{userId}`
- `DELETE /api/admin/users/{userId}`
- `DELETE /api/admin/messages/{messageId}`
- `GET /api/admin/statistics/{userId}`

OpenAPI dokumentacija paleidus aplikaciją:

- `http://localhost:8080/swagger-ui.html`

Pradinis administratorius:

- username: `admin`
- password: `admin-password`

## Paleidimas

Pagrindinė aplikacija ir automatizuoti testai paleidžiami atskirai. Taip aplikacija gali likti veikianti ant `8080` porto, o testai gali būti paleidžiami prieš jau veikiančią aplikaciją.

Sukurti aplikacijos Docker image:

```bash
docker build -t chatroom .
```

Paleisti aplikaciją:

```bash
docker run --rm --name chatroom -p 8080:8080 chatroom
```

Aplikacija pasiekiama:

- `http://localhost:8080/swagger-ui.html`

Sukurti automatizuotų API testų Docker image:

```bash
docker build -f automated-tests.Dockerfile -t chatroom-automated-tests .
```

Paleisti automatizuotus API testus prieš jau veikiančią aplikaciją:

```bash
docker run --rm chatroom-automated-tests
```

Pagal nutylėjimą testų konteineris jungiasi į `http://host.docker.internal:8080`, todėl pagrindinė aplikacija turi būti paleista prieš testų konteinerį.

Paleisti tik aplikacijos testus be Docker:

```bash
./gradlew :chatroom:test
```

Windows:

```bat
gradlew.bat :chatroom:test
```

## Kaip Įgyvendinta Specifikacija

Funkciniai reikalavimai:

- Bendras pokalbių kambarys įgyvendintas viena `chat_message` lentele be atskiro kambarių modelio.
- `USER` gali gauti žinutes per `GET /api/messages`; naudojama cursor pagination, kad vienu kvietimu nebūtų kraunama visa žinučių istorija ir puslapiavimas išliktų stabilus atsirandant naujoms žinutėms.
- `USER` gali parašyti žinutę per `POST /api/messages`.
- Žinutės rūšiuojamos nuo naujausios iki seniausios. 
- `ADMIN` gali kurti vartotojus per `POST /api/admin/users`.
- Jei vartotojas jau egzistuoja, grąžinama klaida.
- `ADMIN` gali pašalinti vartotoją per `DELETE /api/admin/users/{userId}`.
- Pašalinto vartotojo žinutės anonimizuojamos.
- `ADMIN` gali gauti statistiką per `GET /api/admin/statistics/{userId}`.

Techniniai reikalavimai:

- Java 25.
- Gradle projektas.
- Spring Boot.
- H2 duomenų bazė.
- Liquibase migracijos.
- jOOQ duomenų prieigai, be JPA/Hibernate entity modelių.
- Springdoc OpenAPI 3.
- JWT autentifikacija.
- Spock testai.
- Atskiri automatizuoti API testai prieš veikiančią aplikaciją.

SQL užduoties atsakymai:

- `sql-task/sql-1.sql`
- `sql-task/sql-2.sql`
- `sql-task/sql-3.sql`

## Tobulinimai (turint daugiau laiko)

- JWT autentifikaciją ikelti į tam skirtą identity provider, pavyzdžiui Keycloak.
- Pridėti refresh token arba trumpesnio galiojimo access token strategiją.
- Public chat WebSocket/SSE, jei reikėtų realaus laiko žinučių gavimo.
- Aiškesnis healthcheck endpoint'as su Spring Actuator.
