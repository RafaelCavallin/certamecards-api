# CertameCards — API

Backend do CertameCards, um app de flashcards com repetição espaçada para quem estuda para concursos públicos.

Esta API é a **fonte de verdade**: contas e autenticação, autorização, catálogo de matérias, decks e cartões, biblioteca oficial, histórico de revisões e sincronização incremental.

O agendamento **FSRS-6 não roda aqui** — ele roda no frontend ([certamecards-web](https://github.com/RafaelCavallin/certamecards-web)), que envia os estados já calculados. A API apenas os aceita, resolvendo conflitos por `reviewCount`.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 LTS (Eclipse Temurin) |
| Framework | Spring Boot 4.1 (Web MVC, Security 7, OAuth2, Data JPA, Validation, Mail, Actuator) |
| Banco | PostgreSQL 18, schema versionado com Flyway |
| Build | Maven (wrapper `./mvnw`) |
| Testes | JUnit 6, Mockito, AssertJ, Testcontainers, JaCoCo |
| Proxy | Caddy 2 |
| E-mail (dev) | Mailpit |

## Pré-requisitos

- JDK 25 e Docker com Compose v2 (para o Modo 1)
- Apenas Docker com Compose v2 (para o Modo 2)

O repositório [certamecards-web](https://github.com/RafaelCavallin/certamecards-web) precisa estar clonado **na mesma pasta que este**:

```
seu-workspace/
├── certamecards-api/
└── certamecards-web/
```

O serviço `web` do `compose.yaml` usa `../certamecards-web` como contexto de build. Sem isso, o Modo 2 falha.

## Configuração

```bash
cp .env.example .env
```

Preencha o `.env`. Em `DB_HOST` e `SMTP_HOST` use `localhost`, **não** `postgres`/`mailpit` — esses nomes só existem dentro da rede do Compose, e o serviço `api` já define os seus por conta própria.

| Variável | Observação |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Credenciais com que o container do Postgres é criado |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão da aplicação ao banco |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` | `localhost:1025` no desenvolvimento (Mailpit) |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Opcionais; sem eles, só o login com Google fica indisponível |
| `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` | Par de chaves EC P-256 em base64 (veja abaixo) |
| `COOKIE_ENCRYPTION_KEY` | Chave AES-256 em base64, para o cookie de refresh |
| `CERTAME_DOMAIN`, `CERTAME_FRONTEND_ORIGIN` | `certamecards.localhost` e `https://certamecards.localhost` no desenvolvimento |
| `ADMIN_BOOTSTRAP_EMAIL` | E-mail promovido a admin no primeiro login |

Gerando as chaves:

```bash
openssl ecparam -genkey -name prime256v1 -noout -out ec.pem
openssl pkcs8 -topk8 -nocrypt -in ec.pem -outform DER | base64 -w0   # JWT_PRIVATE_KEY
openssl ec -in ec.pem -pubout -outform DER | base64 -w0              # JWT_PUBLIC_KEY
openssl rand -base64 32                                              # COOKIE_ENCRYPTION_KEY
rm ec.pem
```

O `.env` é ignorado pelo git e nunca deve ser versionado.

## Subindo o ambiente

### Modo 1 — desenvolvimento com recarregamento

```bash
docker compose up -d postgres mailpit

set -a && . ./.env && set +a    # o Maven/Spring não lê o .env sozinho; só o Compose lê
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Com o frontend em `npm start` (porta 4200), o `proxy.conf.json` encaminha `/api` para a 8080.

### Modo 2 — ambiente completo em containers

```bash
docker compose --profile full up -d --build
docker compose ps          # todos "healthy"
docker compose logs -f api
```

Use este modo para PWA, cookies `__Host-` e testes E2E. Para encerrar sem perder o banco:

```bash
docker compose --profile full down    # sem -v
```

## Endereços (desenvolvimento)

| Serviço | Endereço |
|---|---|
| App completo via Caddy | `https://certamecards.localhost` |
| API | `http://localhost:8080/api/*` |
| Actuator | `http://localhost:8080/actuator/health` (bloqueado no Caddy) |
| PostgreSQL | `localhost:5432` |
| Mailpit | `http://localhost:8025` (interface), `localhost:1025` (SMTP) |

> O cookie `__Host-` exige HTTPS. Em `http://localhost:4200` o perfil `dev` emite o cookie sem `Secure` e sem o prefixo, então fluxos de login, PWA e E2E devem ser validados em `https://certamecards.localhost`.

## Validação

Rode nesta ordem antes de concluir qualquer alteração:

```bash
./mvnw spotless:check          # formatação e lint (corrigir: ./mvnw spotless:apply)
./mvnw verify                  # unidade + integração (Testcontainers) + cobertura JaCoCo
./mvnw -DskipTests package     # build do JAR
```

Auxiliares: `./mvnw test` (só unidade), `./mvnw test -Dtest=NomeDaClasse`, `./mvnw flyway:info`.

`./mvnw verify` sobe um PostgreSQL via Testcontainers — o Docker precisa estar ativo.

### Cobertura

Piso de **80%** global e **90%** em `review`, `sync`, `auth`, `common/security`, `officialdeck` e `library`. Ficam de fora `CertameCardsApplication`, records de `@ConfigurationProperties` e configuração sem lógica.

## Estrutura

Pacotes por funcionalidade em `br.com.certamecards`, cada um com as camadas `web/` → `service/` → `domain/` e `persistence/`. Os testes espelham o pacote (`*Test` para unidade, `*IT` para integração).

| Pacote | Responsabilidade |
|---|---|
| `auth`, `user`, `settings` | Login por e-mail e senha, login com Google, cookies, redefinição de senha, preferências |
| `subject`, `deck`, `card` | Catálogo de matérias e conteúdo do usuário |
| `review`, `sync` | Histórico só-acrescenta de revisões e sincronização incremental |
| `officialdeck`, `library` | Biblioteca oficial: CRUD administrativo, catálogo, inscrições, atualização de conteúdo |
| `auditlog` | Registro só-acrescenta das ações administrativas (o banco recusa `UPDATE`/`DELETE`) |
| `admin`, `events`, `mail`, `common`, `testsupport` | Apoio |

**Desvio aceito:** escritas em lote e o pull de sincronização usam `JdbcClient` com SQL explícito, e não JPA.

### Principais endpoints

`/api/auth/*` · `/api/me`, `/api/me/settings`, `/api/me/terms` · `/api/decks/*`, `/api/cards/*` · `/api/sync/reviews`, `/api/sync/changes` · `/api/library/*` · `/api/admin/*`

## Migrations

Em `src/main/resources/db/migration/`, aplicadas pelo Flyway na subida. **Nunca edite uma migration já aplicada** — crie a próxima versão.

## Repositórios

- API (este): https://github.com/RafaelCavallin/certamecards-api
- Web: https://github.com/RafaelCavallin/certamecards-web
