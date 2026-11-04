# Rota Certa — App Android (motorista + colaborador)

App nativo em **Kotlin + Jetpack Compose** do sistema Rota Certa. Substitui o antigo app Flutter
(`rota_em_flutter`) e reúne em um único app os dois perfis de campo:

| Perfil | Quem é | O que faz no app |
|--------|--------|------------------|
| **Motorista** | pessoa do tipo `DRIVER` | Vê as rotas do seu veículo, inicia e finaliza a rota, navega até as paradas, marca o embarque dos passageiros e registra eventos. Durante a rota, envia o GPS do ônibus. |
| **Colaborador** | pessoa do tipo `EMPLOYEE` | Acompanha no mapa o seu ponto de embarque e o ônibus ao vivo, confirma ou recusa a presença, consulta a agenda e recebe avisos. |

O painel Angular continua sendo o back-office (cadastros). O plano completo da migração está em
[`docs/PLANO_DE_DESENVOLVIMENTO.md`](docs/PLANO_DE_DESENVOLVIMENTO.md).

## Telas

O login é único. Depois de entrar, o app mostra a área de acordo com o tipo da pessoa no backend.
As telas abaixo são as do código atual (`app/src/main/java/com/rotacerta/mobile/feature/`).

### Acesso (ambos os perfis)

| Tela | Arquivo | Para que serve |
|------|---------|----------------|
| Splash | `auth/SplashScreen.kt` | Abre enquanto a sessão salva é verificada. |
| Login | `auth/LoginScreen.kt` | E-mail corporativo e senha, com link "Esqueci minha senha". |
| Recuperação de senha | `auth/ForgotPasswordScreen.kt` | Em três passos: código recebido por e-mail, depois a nova senha (com medidor de força). |
| Troca de senha | `auth/ChangePasswordScreen.kt` | Obrigatória no primeiro acesso; depois, acessível pelo Perfil. |

### Motorista

Barra inferior: **Rotas · Avisos · Perfil**.

| Tela | Arquivo | O que o motorista vê e faz |
|------|---------|----------------------------|
| Minhas rotas | `driver/routes/MyRoutesScreen.kt` | Lista das rotas do veículo vinculado, com horário de chegada, dias da semana, paradas e quantos passageiros confirmaram. Destaca a rota em andamento e as rotas extras do dia. Mostra o aviso "sem conexão" quando o tempo real cai. |
| Detalhe da rota | `driver/detail/RouteDetailScreen.kt` | Três abas: **Roteiro** (paradas em ordem, com contagem de passageiros), **Passageiros** (pendentes, confirmados e ausentes, com a ação de marcar embarque) e **Eventos** (parada, atraso ou incidente com descrição e localização atual). Traz os botões **Iniciar rota** / **Finalizar rota** (com confirmação) e **Navegação**. |
| Navegação | `driver/navigation/NavigationMapScreen.kt` | Mapa em tela cheia com a próxima instrução (curva, rotatória, saída etc.), distância até a próxima parada, chegada estimada e o restante do trajeto. Permite seguir o ônibus, ver a rota inteira, recalcular a partir do ponto atual e registrar a parada. Avisa quando o motorista sai do trajeto e quando o GPS está sem sinal. |
| Avisos | `notifications/NotificationsScreen.kt` | Saída, atrasos e chegada, em tempo real. Marcar todas como lidas e carregar mais. |
| Perfil | `profile/ProfileScreen.kt` | Dados do usuário, endereço, troca de senha e saída da conta. Ao sair com rota em andamento, o app avisa que o envio do GPS será interrompido. |

Durante a rota, um serviço em primeiro plano (`driver/tracking/LocationTrackingService.kt`) mantém o
GPS ativo mesmo com a tela desligada. Enquanto ele está ligado, a notificação do sistema permanece visível.

### Colaborador

Barra inferior: **Mapa · Agenda · Avisos · Perfil**.

| Tela | Arquivo | O que o colaborador vê e faz |
|------|---------|------------------------------|
| Mapa | `collaborator/CollaboratorMapScreen.kt` | Seu ponto de embarque, o ônibus com posição ao vivo, distância até o seu ponto e tempo estimado. Botões **Vou embarcar** e **Não irei hoje**, com **Alterar resposta** depois de confirmar. Se você ainda não está em uma rota ou não tem ponto definido, a tela explica o que falta. |
| Agenda | `collaborator/AgendaScreen.kt` | Suas rotas e horários (dias da semana, veículo e motorista) e a opção de **Ausentar** em um dia. Sem rota vinculada, orienta a falar com o gestor. |
| Avisos | `notifications/NotificationsScreen.kt` | Saída do ônibus, atrasos e chegada ao seu ponto, em tempo real. Marcar todas como lidas e carregar mais. |
| Perfil | `profile/ProfileScreen.kt` | Usuário, **Meu endereço** (com o status de um envio pendente ou recusado), troca de senha e saída da conta. |
| Meu endereço | `address/AddressScreen.kt` | Cadastro do endereço de casa, usado no planejamento das rotas. Três formas de preencher: CEP, digitação ou localização atual. Um alfinete no mapa deve ficar sobre a casa. Há limite de alterações por semana, e o novo endereço só vale depois de aprovado pelo gestor de transporte. |

## Funcionalidades

- **Login único**: o perfil (motorista ou colaborador) é definido pelo backend.
- **Primeiro acesso e recuperação**: troca de senha obrigatória no primeiro login e recuperação de senha em três passos.
- **Tempo real** (WebSocket em `/ws/stream/`): avisos, posição do ônibus, status da rota e confirmações de embarque chegam sem atualizar a tela.
  Com o app em segundo plano, os avisos viram notificação do Android.
- **Motorista**: "Iniciar rota" registra a partida e liga o envio de GPS a cada ~5 s. "Finalizar rota" registra a chegada e desliga o envio.
  Também é possível registrar parada, atraso e incidente com a localização atual, e marcar o embarque dos passageiros.
- **Colaborador**: confirmação de presença, mapa com o ônibus ao vivo, agenda e avisos.
- **Sessão**: renovada automaticamente (refresh token) no HTTP e no WebSocket. Quando a sessão expira, o app volta para o login.

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
│   ├── driver/         rotas, detalhe da rota, navegação, tracking (serviço de GPS)
│   ├── collaborator/   mapa, agenda e home do colaborador
│   ├── address/        cadastro do endereço de casa
│   ├── notifications/  aba Avisos (compartilhada)
│   ├── profile/        aba Perfil (compartilhada)
│   └── home/           scaffold com a barra inferior
└── navigation/         raiz por estado da sessão + rotas tipadas
```

- **Raiz por sessão**: `navigation/AppRoot.kt` escolhe entre splash, login, troca de senha obrigatória e o app.
  Cada estado tem sua própria pilha de telas, então nunca sobra uma tela da conta anterior.
- **MVVM**: `ViewModel` com `StateFlow`; repositórios devolvem `ApiResult`.
- **Fonte única de dados**: os repositórios do colaborador e de avisos alimentam as abas e aplicam os eventos do WebSocket.

## Requisitos

- Android Studio 2026.2 ou mais recente (o projeto usa AGP 9.4 e Gradle 9.8; o JDK embutido do Studio serve).
- minSdk 26 · targetSdk 36 · compileSdk 37.
- Backend Django com Channels rodando (veja a seção abaixo).

## Configuração

1. Copie `local.properties.example` para `local.properties` e ajuste:
   - `API_BASE_URL`: no emulador, `http://10.0.2.2:8000/api/`; em celular na mesma rede, `http://SEU_IP:8000/api/`.
   - `WS_URL` (opcional): por padrão vira `ws://<mesmo host>/ws/stream/`.
   - `MAPS_API_KEY`: chave do **Maps SDK for Android**. Sem ela, o mapa fica cinza, mas o restante funciona.
2. Suba o backend na raiz de `Rota Certa 2026 (ed. final)`: `docker compose up --build`
   (Postgres, Redis e o backend em daphne). Para dados de demonstração:
   `docker compose exec backend python manage.py seed_manaus_demo`.
3. Abra esta pasta no Android Studio, aguarde o Gradle sincronizar e rode no emulador.

**Testando o tempo real:** use dois aparelhos (ou emulador + celular), um logado como motorista e outro como
colaborador da mesma rota. No emulador do motorista, simule o trajeto em *Extended controls → Location → Routes*.

## Testes

```bash
./gradlew testDebugUnitTest
```

Cobrem o parser do WebSocket, o backoff de reconexão, distância/ETA, regras de senha, formatadores,
erros do DRF, decodificação de polyline e as regras de estado das telas.

## Limitações conhecidas

- Com o app **fechado**, o colaborador não recebe avisos, porque o WebSocket exige o processo ativo.
  Para push com o app fechado é preciso FCM, que é o próximo passo sugerido.
  O motorista em rota não é afetado, pois o serviço de GPS mantém o app ativo.
- O `boarding_status` não volta a "pendente" sozinho a cada dia; essa regra ainda será definida no backend.
