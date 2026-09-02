# Rota Certa — App do Motorista (Kotlin / Android)

App nativo Android, em **Kotlin + Jetpack Compose**, para o **motorista** do sistema
Rota Certa. Consome diretamente a API já existente em `Rota-certa-Backend-Django`,
reaproveitando os endpoints que o próprio backend já expõe para app mobile de
motorista (`/routes/my-routes/`, `/routes/{id}/driver-detail/`,
`/routes/{id}/navigation/`, `/routes/{id}/events/`, `/route-events/`,
`/route-passengers/{id}/`).

> O painel Angular já existente continua sendo o back-office (admin cadastra
> empresas, rotas, funcionários, veículos). Este app é o complemento do lado do
> motorista, em campo.

## Funcionalidades

- **Login** com e-mail/senha (JWT), bloqueando acesso de quem não é `DRIVER`.
- **Troca de senha obrigatória** no primeiro acesso (`must_change_password`).
- **Esqueci minha senha** (solicitação de código + redefinição).
- **Minhas rotas**: lista as rotas do veículo/motorista logado.
- **Detalhe da rota**: timeline de paradas, lista de passageiros com
  atualização de status de embarque (pendente/embarcado/ausente), registro de
  eventos (partida, chegada, parada, atraso, incidente) com geolocalização
  automática, e histórico de eventos.
- **Navegação**: mapa com o traçado da rota (polyline do Google Routes API),
  marcadores de origem/paradas/destino, instrução e ETA/distância.
- **Renovação automática de sessão**: ao expirar o access token (1h), o app
  renova sozinho usando o refresh token (7 dias), sem derrubar o motorista.

## Arquitetura

```
app/src/main/java/com/rotacerta/driver/
├── data/
│   ├── remote/          # Retrofit APIs, DTOs, interceptor de auth, refresh automático
│   ├── local/            # DataStore (tokens e sessão)
│   └── repository/       # AuthRepository, RouteRepository
├── di/                    # Módulos Hilt (rede, contexto)
├── ui/
│   ├── login/ changepassword/ forgotpassword/
│   ├── myroutes/ routedetail/ navigationmap/
│   ├── session/           # observa a sessão e decide a tela inicial
│   ├── navigation/        # NavHost + rotas (Navigation Compose)
│   └── theme/
└── util/                  # UiState, parser de erros DRF, decodificador de polyline, GPS
```

MVVM simples: `ViewModel` com `StateFlow<UiState<T>>`, `Repository` traduzindo
chamadas Retrofit em `DataResult`/`AuthResult`, sem camada de domínio extra
(o projeto é pequeno o suficiente para não precisar de use cases separados).

## Configuração antes de rodar

### 1. Backend

Suba o Django normalmente (`python manage.py runserver 0.0.0.0:8000`).
Para o app conseguir calcular a tela de **Navegação**, configure
`GOOGLE_MAPS_API_KEY` no `.env` do backend (Google Routes API habilitada).

### 2. `local.properties`

Copie o exemplo e ajuste:

```bash
cp local.properties.example local.properties
```

- `API_BASE_URL`:
  - Emulador Android: `http://10.0.2.2:8000/api/` (padrão já configurado)
  - Dispositivo físico na mesma rede: `http://SEU_IP_LOCAL:8000/api/`
  - Servidor remoto: a URL pública, de preferência com HTTPS
- `MAPS_API_KEY`: chave do **Maps SDK for Android** (Google Cloud Console).
  Pode ser diferente da chave usada pelo backend (Routes API), mas ambas
  precisam existir no mesmo projeto do Google Cloud, com faturamento ativo.

O Android Studio adiciona automaticamente a linha `sdk.dir=...` neste mesmo
arquivo ao abrir o projeto — não é preciso mexer nela.

### 3. Abrir e rodar

Abra a pasta `RotaCertaDriver` no Android Studio (Koala ou mais recente) e
deixe o Gradle sincronizar. O projeto usa **minSdk 26 / targetSdk 34**.

### 4. Criar um usuário motorista para testar

O login só é aceito para pessoas do tipo `DRIVER` (regra aplicada no app,
não no backend — o backend aceita qualquer `Person` com conta de acesso).
No Django admin ou via shell, garanta que existe:

- Uma `Person` com `type="DRIVER"` e `license_number` preenchido;
- Uma `AccessAccount` vinculada a essa pessoa, com senha definida;
- Idealmente, um `Vehicle` com `driver` apontando para essa pessoa e uma
  `Route` com `vehicle` apontando para esse veículo — senão "Minhas rotas"
  aparecerá vazio (é esperado: a query de `my-routes` filtra por
  `vehicle__driver=person`).

## Decisões técnicas relevantes

- **Retrofit + kotlinx.serialization**, com `isLenient = true`: o Django REST
  Framework serializa `DecimalField` (latitude/longitude) como **string**
  por padrão (`COERCE_DECIMAL_TO_STRING`). O modo lenient do
  kotlinx.serialization permite decodificar esses números "entre aspas"
  direto em campos `Double`, sem precisar de serializers customizados.
- **OkHttp `Authenticator`** (não apenas interceptor) para renovar o token:
  intercepta qualquer 401, chama `POST /auth/refresh/` uma única vez e repete
  a requisição original — se falhar, limpa a sessão e a UI volta ao login
  sozinha (a tela observa `SessionStore.sessionFlow`).
- **DataStore** em vez de `SharedPreferences` para os tokens (API assíncrona,
  recomendada atualmente pelo Android).
- App **não implementa cadastro de empresas/rotas/veículos/funcionários** —
  isso é responsabilidade do painel Angular já existente. Manter essa
  separação evita duplicar regras de negócio (CNPJ, e-mail automático por
  domínio da empresa, TPH de `Person`, etc.) em dois lugares.

## Possíveis próximos passos

- Testes instrumentados de `ViewModel` (os repositórios já retornam tipos
  seláveis fáceis de mockar).
- Notificações push quando o admin reatribuir a rota do motorista.
- Modo offline básico (cache do último `driver-detail` carregado).
