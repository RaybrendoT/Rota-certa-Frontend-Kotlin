# Plano de desenvolvimento — App Rota Certa (Kotlin, motorista + colaborador)

> Origem: migração do app `rota_em_flutter` para Kotlin, unificando com o app
> do motorista já existente (`Rota-certa-Android-Kotlin`) e adicionando tempo
> real (WebSocket) e GPS no backend Django.
>
> Data do plano: 27/10/2026.

---

## 1. Diagnóstico

### 1.1 O que existe hoje

| Peça | Estado |
|------|--------|
| `rota_em_flutter` | 21 arquivos Dart (~4,5 mil linhas, 80% em 2 telas). Login, recuperação de senha, dashboard do motorista e do colaborador. Muito mock. |
| `Rota-certa-Android-Kotlin` | Compose + Hilt + Retrofit + DataStore, refresh automático de token, só **motorista**: minhas rotas, detalhe, passageiros, eventos, navegação. Código limpo. |
| Backend Django | DRF + JWT próprio. Endpoints do motorista prontos. **Sem WebSocket**, **sem nenhum endpoint para `EMPLOYEE`** (403 em tudo). |
| Android Studio da máquina | 2026.2 (JBR 25), SDK `android-37`. |

### 1.2 Problemas que bloqueiam

1. **Toolchain do app Kotlin está velho demais para o Android Studio atual**:
   Gradle 8.7 não roda em Java 25. É obrigatório atualizar para Gradle 9 / AGP 9 / Kotlin 2.4.
2. **Colaborador não tem API**: o dashboard do Flutter chama `route-passengers/`, `routes/{id}/`
   e `route-stops/{id}/`, que retornam 403 para `EMPLOYEE`. Hoje só funcionaria logado como gestor.
3. **Sem tempo real**: avisos são constantes na tela e o motorista faz polling a cada 10 s.
4. **GPS falso**: o "progresso" do ônibus no Flutter é um `Timer` que soma 0,003 a cada 5 s.

### 1.3 Mocks e lixo do Flutter, e o que acontece com cada um

| Item no Flutter | Destino no Kotlin |
|-----------------|-------------------|
| `AvisosView` com 4 avisos fixos | Lista real: `GET /notifications/` + WebSocket |
| `AgendaView` com "Ida 07:30 / Volta 18:00" fixos e toggle local | Rotas reais do colaborador + horários da empresa, confirmação gravada no backend |
| Lista global `paradas` mockada (Ana Souza, Carlos Lima…) | Removida; dados de `my-routes` / `driver-detail` |
| `Timer` que simula o ônibus andando | GPS real do aparelho do motorista (foreground service) |
| `_MapaMotorista` (CustomPainter não usado) e `main()` dentro do dashboard | Removidos |
| Toque no passageiro cicla status só na memória | `PATCH route-passengers/{id}/` (já existe no app Kotlin) |
| Fallback que baixa **todos** os veículos/rotas/endereços e filtra no celular | Removido (pesado e hoje dá 403) |
| `api_client.dart` duplicado e IPs fixos (`192.168.1.8`, `10.31.1.140`) | `API_BASE_URL` em `local.properties` → `BuildConfig` |
| Rota `/register` abrindo tela de "esqueci senha", `RichText` vazio de cadastro | Removidos (cadastro é feito pelo gestor no painel web) |
| "Código de demo" exibido na tela | Exibido só em build debug |
| Botões de busca/volume no mapa sem ação, "Finalizar" sem ação | Removidos / "Finalizar" passa a registrar chegada |
| Logout que só navegava para `/login` | Logout que limpa sessão e fecha o WebSocket |
| `customInput()` e `CustomInput` duplicados, cores repetidas em cada arquivo | Design system único (`core/designsystem`) |
| 14 arquivos `.md` de desenvolvimento | Substituídos por este plano + README; repositório Flutter marcado como descontinuado |

O que **vale preservar** do Flutter: identidade visual (fundo `#020B1A`, inputs `#13294B`,
botão `#2EA7FF`, marca "Rota**certa**"), fluxo de 3 passos da recuperação de senha com
medidor de força, e a UX do colaborador (mapa com ponto de embarque + card "Vou embarcar /
Não irei hoje" + "Alterar resposta").

### 1.4 Segurança (fora do escopo, mas precisa de ação)

- `.env.example` do backend tem uma `GOOGLE_MAPS_API_KEY` com cara de real versionada no git.
  **Revogue/rotacione a chave no Google Cloud** e deixe um placeholder no exemplo.

---

## 2. Arquitetura alvo

### 2.1 Visão geral

```
 App Android (Kotlin/Compose)                       Backend Django (ASGI)
 ┌─────────────────────────────┐   REST (JWT)   ┌──────────────────────────────┐
 │ Motorista                   │ ─────────────▶ │ DRF: rotas, eventos,         │
 │  • LocationTrackingService ─┼─ POST location▶│ passageiros, /me/routes,     │
 │ Colaborador                 │                │ notifications, location      │
 │                             │   WebSocket    │                              │
 │  RealtimeClient (OkHttp) ◀──┼─────────────── │ Channels: /ws/stream/        │
 └─────────────────────────────┘  eventos push  │   grupos person_<id>,        │
                                                │          route_<id>          │
                                                └──────────────┬───────────────┘
                                                        Redis (channel layer)
```

Decisões:
- **Um único WebSocket por usuário** (`/ws/stream/`), só servidor → cliente.
  O servidor decide os grupos na conexão (motorista: rotas do veículo dele;
  colaborador: rotas em que é passageiro).
- **O motorista envia a localização por REST** (`POST /routes/{id}/location/`), não pelo socket:
  funciona dentro do foreground service, reaproveita refresh de token e não perde pontos
  durante uma reconexão. O backend persiste a última posição e repassa via Channels.
- **Notificações são persistidas** (model `Notification`), assim a aba Avisos tem histórico
  e o que chegou com o app fechado aparece ao abrir.

### 2.2 Contrato do WebSocket

- URL: `ws(s)://<host>/ws/stream/?token=<access_token>`
- Fechamento `4001`: token inválido/expirado → app renova o token e reconecta.
- (Não há fechamento 4003: todo perfil conecta; gestor recebe também as rotas da empresa.)
- Keep-alive: ping do protocolo (OkHttp `pingInterval`).

Mensagens (JSON, campo `type`):

```json
{"type": "notification", "notification": {"id": 10, "kind": "route_delay", "title": "Atraso na rota",
  "message": "Trânsito intenso", "route_id": 3, "created_at": "2026-10-27T07:12:00-04:00", "read": false}}

{"type": "vehicle_location", "route_id": 3, "latitude": -3.11, "longitude": -60.02,
  "heading": 87.0, "speed": 8.3, "recorded_at": "2026-10-27T07:12:05-04:00"}

{"type": "boarding_status", "route_id": 3, "route_passenger_id": 21, "person_id": 12, "status": "confirmed"}

{"type": "route_event", "route_id": 3, "event_type": "departure", "description": "",
  "route_status": "in_route", "timestamp": "2026-10-27T07:00:00-04:00"}
```

### 2.3 Novos endpoints REST

| Método | Endpoint | Perfil | Descrição |
|--------|----------|--------|-----------|
| GET | `me/routes/` | EMPLOYEE | Vínculos do colaborador: rota, parada (com lat/lng), veículo, motorista, `route_status`, última posição do ônibus, horários da empresa |
| PATCH | `me/routes/{route_passenger_id}/` | EMPLOYEE | `{"boarding_status": "confirmed" \| "absent" \| "pending"}` |
| POST | `routes/{id}/location/` | DRIVER (rota dele) | `{latitude, longitude, heading?, speed?, accuracy?}` → 202 |
| GET | `notifications/` | todos | Notificações do usuário (paginado, `?unread=true`) |
| POST | `notifications/{id}/read/` | todos | Marca como lida |
| POST | `notifications/read-all/` | todos | Marca todas como lidas |

### 2.4 Gatilhos de notificação

| Gatilho | Quem recebe | Exemplo |
|---------|-------------|---------|
| Evento `departure` | Passageiros da rota | "Seu ônibus saiu — Rota Zona Leste" |
| Evento `delay` / `incident` | Passageiros da rota | "Atraso na rota: Trânsito intenso" |
| Evento `stop` a ≤ 200 m da parada do passageiro | Esse passageiro | "O ônibus chegou ao seu ponto" |
| Localização do ônibus a ≤ 1 km da parada (1×/dia) | Esse passageiro | "Ônibus chegando ao seu ponto" |
| Evento `arrival` | Passageiros da rota | "Rota finalizada" |
| Colaborador confirma/recusa embarque | Motorista da rota | "Ana Souza confirmou embarque" |

Broadcasts sempre via `transaction.on_commit`.

### 2.5 Estrutura do app

Pacote renomeado de `com.rotacerta.driver` para `com.rotacerta.mobile` (agora atende os dois perfis).

```
com.rotacerta.mobile/
├── core/
│   ├── designsystem/   tema escuro do Flutter, botões, inputs, cards, badges
│   ├── network/        Retrofit, OkHttp, AuthInterceptor, TokenAuthenticator, erros DRF
│   ├── session/        SessionStore (DataStore) com perfil DRIVER/EMPLOYEE
│   ├── realtime/       RealtimeClient (WebSocket), RealtimeEvent, reconexão
│   ├── location/       LocationHelper, permissões, cálculo de distância
│   └── util/           UiState, PolylineDecoder
├── data/               APIs, DTOs, repositórios (Auth, Route, Collaborator, Notification, Tracking)
├── feature/
│   ├── auth/           splash, login, esqueci senha (3 passos), troca obrigatória de senha
│   ├── driver/         minhas rotas, detalhe (timeline/passageiros/eventos), navegação,
│   │                   LocationTrackingService
│   ├── collaborator/   mapa + confirmação, agenda, perfil
│   └── notifications/  aba Avisos (compartilhada)
└── navigation/         NavHost com grafo por perfil e bottom bar
```

---

## 3. Fases de desenvolvimento

Cada fase termina com build verde e testes passando. Commits em branch própria
em cada repositório (`feature/app-unificado` no app, `feature/tempo-real` no backend).

### Fase 0 — Preparação
- [x] Branches nos dois repositórios.
- [x] Gerar Gradle wrapper (o repositório não tem `gradlew`/`gradle-wrapper.jar`).
- [x] Subir Postgres via Docker para rodar a suíte de testes do backend (linha de base).

### Fase 1 — Backend: tempo real e colaborador
- [x] Dependências: `channels`, `channels-redis`, `daphne`. ASGI com roteamento HTTP + WebSocket.
- [x] Middleware de autenticação JWT para WebSocket (token na query string, reaproveitando `AccessAccountJWTAuthentication`).
- [x] `StreamConsumer` em `/ws/stream/` com grupos `person_<id>` e `route_<id>`.
- [x] Models `Notification` e `VehicleLocation` (última posição por rota) + migrations + admin.
- [x] Serviço `notify(...)` (grava + envia) e `broadcast_route(...)`.
- [x] Endpoints `me/routes/`, `routes/{id}/location/`, `notifications/`.
- [x] Gatilhos da tabela 2.4 (eventos, boarding status, proximidade).
- [x] Testes: permissões por perfil, payloads, consumer com `WebsocketCommunicator`, gatilhos.
- [x] `docker-compose.yml`: serviço `redis`, backend servindo com `daphne`.
- [x] Atualizar Specs (`03-api-rest.md`) com os endpoints e o contrato WebSocket.

**Pronto quando:** suíte inteira passa e um cliente WebSocket de teste recebe
`vehicle_location` e `notification` ao postar localização/evento.

### Fase 2 — App: fundação
- [x] Toolchain: Gradle 9.8, AGP 9.4, Kotlin 2.4 (Kotlin embutido do AGP 9 + plugin Compose), KSP2, Hilt 2.60,
      Retrofit 3, OkHttp 5, version catalog (`gradle/libs.versions.toml`), compile/targetSdk 36.
- [x] Renomear pacote/applicationId para `com.rotacerta.mobile`.
- [x] Sessão com perfil: login aceita DRIVER e EMPLOYEE; a área é decidida pelo tipo da pessoa (gestor que também é passageiro cai na área do colaborador).
- [x] Design system com a identidade visual do Flutter; splash "Rotacerta".
- [x] `API_BASE_URL` e `WS_BASE_URL` vindos de `local.properties` (nada hardcoded).
- [x] Navegação por perfil: grafo do motorista e grafo do colaborador, cada um com bottom bar.

**Pronto quando:** `./gradlew assembleDebug testDebugUnitTest` passa; login leva cada perfil ao seu início.

### Fase 3 — App: tempo real
- [x] `RealtimeClient`: conecta quando a sessão está pronta, backoff exponencial, trata 4001 renovando o token, desconecta no logout.
- [x] `RealtimeEvent` (sealed) + parser com testes.
- [x] `NotificationRepository`: carga REST + merge do socket, contador de não lidas, marcar como lida.
- [x] Notificação do sistema Android quando o app está em segundo plano (permissão `POST_NOTIFICATIONS`).

### Fase 4 — App: GPS
- [x] Motorista: `LocationTrackingService` (foreground service tipo `location`), atualização a cada ~5 s / 10 m,
      envio para `routes/{id}/location/`, notificação fixa "Rota em andamento".
- [x] "Iniciar rota" → evento `departure` + inicia o serviço; "Finalizar" → evento `arrival` + para o serviço.
- [x] Fluxo de permissões (localização precisa, notificações) com explicação ao usuário.
- [x] Colaborador: posição do ônibus ao vivo no mapa + distância até o ponto; "minha localização" opcional.

### Fase 5 — App: telas do colaborador (port do Flutter, sem mocks)
- [x] **Mapa**: ponto de embarque, ônibus ao vivo, card "Vou embarcar / Não irei hoje", "Alterar resposta".
- [x] **Agenda**: rotas do colaborador com horários da empresa e confirmação por rota.
- [x] **Avisos**: lista real, badge de não lidas, marcar como lida.
- [x] **Perfil**: nome, empresa, rota, ponto, trocar senha, sair.

### Fase 6 — App: telas do motorista (unificação)
- [x] Reestilizar minhas rotas / detalhe / navegação com o visual escuro do dashboard Flutter.
- [x] Bottom bar: Rotas · Avisos · Perfil; Roteiro, Passageiros e Eventos viraram abas do detalhe da rota (motorista pode ter mais de uma rota).
- [x] Lista de passageiros atualiza ao vivo via `boarding_status` (fim do polling de 10 s).
- [x] Mapa de navegação com a posição GPS real do ônibus.

### Fase 7 — Qualidade e encerramento
- [x] Testes unitários: decoder de polyline, distância, parser de eventos, mapeadores, ViewModels com repositórios falsos.
- [x] README do app e do backend atualizados (como rodar, chaves, WebSocket).
- [ ] Repositório Flutter: README de descontinuado apontando para o app Kotlin (remoção só com sua confirmação).

---

## 4. Como testar (quando o Android Studio estiver instalado)

1. `docker compose up --build` na raiz de `Rota Certa 2026 (ed. final)` (Postgres, Redis, backend).
2. `python manage.py seed_manaus_demo` dentro do container do backend para ter motoristas, colaboradores e rotas.
3. No app: copie `local.properties.example` → `local.properties`, ajuste `API_BASE_URL`
   (`http://10.0.2.2:8000/api/` no emulador) e `MAPS_API_KEY`.
4. Rode dois emuladores (ou emulador + celular): um logado como motorista, outro como colaborador.
   No emulador do motorista, use *Extended controls → Location* para simular o trajeto.

## 5. Limitações conhecidas

- **Notificação com o app fechado (colaborador)**: WebSocket só funciona com o processo vivo.
  O motorista fica coberto pelo foreground service; para o colaborador receber push com o app
  fechado é preciso FCM (Firebase), que fica como próximo passo.
- Navegação do motorista depende de `GOOGLE_MAPS_API_KEY` no backend (Routes API) e de
  `MAPS_API_KEY` no app (Maps SDK).
- Os manifests de `deploy/k3s` precisarão de Redis e do comando `daphne` (a fazer depois da validação local).

---

## 6. Status em 27/10/2026

Fases 0 a 7 concluídas, exceto a descontinuação do repositório Flutter (aguarda confirmação).

Verificado:
- Backend: 114 testes (81 existentes + 33 novos) e teste de fumaça com daphne + Redis reais
  (GPS → `vehicle_location`, evento → `route_event` + notificação).
- App: build sem avisos, 20 testes unitários e teste manual no emulador contra o backend em Docker:
  login de motorista, lista e detalhe da rota com dados do seed, "Iniciar rota" (permissões, evento
  `departure`, GPS do emulador chegando ao backend), avisos gerados para o colaborador, logout encerrando
  WebSocket e serviço de GPS.

Ajustes feitos durante a execução:
- `redis-py` 8 usa `socket_timeout=5`, igual à espera bloqueante do `channels-redis`: o WebSocket caía a
  cada ~7 s. Corrigido com `socket_timeout=30` no channel layer e `redis` fixado no requirements.
- O log HTTP de debug mostrava o JWT da URL do WebSocket; agora `token` e `Authorization` são ocultados.

Falta validar manualmente: área do colaborador (mapa, confirmação, agenda) e recuperação de senha no
aparelho, mapas com `MAPS_API_KEY` real, e manifests do `deploy/k3s` (Redis + daphne).
