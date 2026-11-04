# Rota Certa — App Android (motorista + colaborador)

App nativo em **Kotlin + Jetpack Compose** do sistema Rota Certa. Substitui o antigo
app Flutter (`rota_em_flutter`) e unifica os dois perfis de campo:

| Perfil | Quem | O que faz no app |
|--------|------|------------------|
| **Motorista** | pessoa `DRIVER` | Rotas do seu veículo, paradas, passageiros, eventos, navegação; envia o GPS do ônibus durante a rota |
| **Colaborador** | pessoa `EMPLOYEE` | Mapa com seu ponto e o ônibus ao vivo, confirmação de presença, agenda, avisos |

O painel Angular continua sendo o back-office (cadastros). Plano completo da migração em
[`docs/PLANO_DE_DESENVOLVIMENTO.md`](docs/PLANO_DE_DESENVOLVIMENTO.md).

## Funcionalidades

- **Login único**: a área (motorista ou colaborador) é escolhida pelo tipo da pessoa no backend.
- **Troca de senha obrigatória** no primeiro acesso e **recuperação de senha** em 3 passos (código + nova senha com medidor de força).
- **Tempo real (WebSocket `/ws/stream/`)**: avisos, posição do ônibus, status da rota e confirmações de embarque chegam sem polling.
  Com o app em segundo plano, avisos viram notificação do Android.
- **Motorista**: "Iniciar rota" registra a partida e liga um *foreground service* que envia o GPS a cada ~5 s;
  "Finalizar rota" registra a chegada e desliga. Também registra parada, atraso e incidente com a localização atual,
  e marca o embarque dos passageiros (a lista atualiza ao vivo quando o colaborador responde).
- **Colaborador**: mapa com ponto de embarque e ônibus ao vivo, distância e chegada estimada, "Vou embarcar / Não irei hoje",
  agenda com horários da empresa, avisos e perfil.
- Sessão renovada automaticamente (refresh token) no HTTP e no WebSocket; sessão expirada volta ao login.

## Arquitetura

```
com.rotacerta.mobile/
├── core/
│   ├── designsystem/   tema escuro da marca, componentes, marcadores de mapa
│   ├── network/        ApiResult, interceptor, renovação de token (HTTP + WebSocket)
│   ├── realtime/       RealtimeClient (OkHttp WebSocket, backoff) e parser de eventos
│   ├── session/        SessionStore (DataStore) com o perfil do usuário
│   ├── location/       GPS (Fused Location) e cálculo de distância/ETA
│   ├── notification/   notificações do sistema
│   └── util/           UiState, formatadores, regras de senha, polyline
├── data/               APIs Retrofit, DTOs e repositórios (Auth, Driver, Collaborator, Notification)
├── di/                 módulos Hilt
├── feature/
│   ├── auth/           splash, login, recuperação e troca de senha
│   ├── driver/         rotas, detalhe (roteiro/passageiros/eventos), navegação, tracking (serviço GPS)
│   ├── collaborator/   mapa, agenda e home do colaborador
│   ├── notifications/  aba Avisos (compartilhada)
│   ├── profile/        aba Perfil (compartilhada)
│   └── home/           scaffold com barra inferior
└── navigation/         raiz por estado da sessão + rotas tipadas
```

MVVM: `ViewModel` com `StateFlow`, repositórios devolvendo `ApiResult`. Os repositórios do colaborador
e de avisos são a fonte única de dados das abas e aplicam os eventos do WebSocket.

## Requisitos

- Android Studio 2026.2 ou mais recente (o projeto usa AGP 9.4 e Gradle 9.8; o JDK embutido do Studio serve).
- minSdk 26 · targetSdk 36 · compileSdk 37.
- Backend Django rodando com Channels (veja abaixo).

## Configuração

1. Copie `local.properties.example` para `local.properties` e ajuste:
   - `API_BASE_URL` — emulador: `http://10.0.2.2:8000/api/`; celular na mesma rede: `http://SEU_IP:8000/api/`.
   - `WS_URL` — opcional; por padrão vira `ws://<mesmo host>/ws/stream/`.
   - `MAPS_API_KEY` — chave do **Maps SDK for Android** (sem ela o mapa fica cinza, o resto funciona).
2. Suba o backend: na raiz de `Rota Certa 2026 (ed. final)`, `docker compose up --build`
   (Postgres, Redis e o backend em daphne). Para dados de teste:
   `docker compose exec backend python manage.py seed_manaus_demo`.
3. Abra esta pasta no Android Studio, deixe o Gradle sincronizar e rode no emulador.

Para testar o tempo real, use dois aparelhos (ou emulador + celular): um logado como motorista e outro como
colaborador da mesma rota. No emulador do motorista, simule o trajeto em *Extended controls → Location → Routes*.

## Testes

```bash
./gradlew testDebugUnitTest
```

Cobrem o parser do WebSocket, backoff de reconexão, distância/ETA, regras de senha, formatadores,
erros do DRF, decodificação de polyline e regras de estado das telas.

## Limitações conhecidas

- Com o app **fechado**, o colaborador não recebe avisos (WebSocket exige o processo vivo). Push com app fechado
  exige FCM — próximo passo sugerido. O motorista em rota não é afetado (o serviço de GPS mantém o app vivo).
- O `boarding_status` não volta a "pendente" sozinho a cada dia; isso é regra do backend a definir.
