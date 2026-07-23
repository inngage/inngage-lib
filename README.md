# Guia de Integração — Inngage Android SDK

Documentação para desenvolvedores integrarem a **Inngage Android SDK** (push notifications, in-app messages e event tracking) em um app Android.

Este guia descreve a integração pela **API pública oficial** — o objeto `br.com.inngage.sdk.InngageClient` — módulo por módulo, com exemplos prontos para copiar e colar. Todos os exemplos refletem exatamente como a SDK é consumida no app de exemplo (`app/`).

> A SDK é escrita em Kotlin e 100% interoperável com Java. Os exemplos estão em Kotlin (recomendado); há uma seção específica de interop Java ao final de cada método.

---

## Sumário

1. [Visão geral da arquitetura de integração](#1-visão-geral)
2. [Requisitos](#2-requisitos)
3. [Passo a passo de instalação](#3-instalação)
    - 3.1 [Firebase (google-services.json)](#31-firebase)
    - 3.2 [Dependência da SDK (JitPack)](#32-dependência-da-sdk)
    - 3.3 [AndroidManifest.xml](#33-androidmanifestxml)
    - 3.4 [Ícone de notificação](#34-ícone-de-notificação)
    - 3.5 [Constantes de integração](#35-constantes-de-integração)
4. [Ponto de entrada: `InngageClient`](#4-ponto-de-entrada)
5. [Módulos / Serviços](#5-módulos--serviços)
    - 5.1 [Subscription — registro e identificação](#51-subscription)
    - 5.2 [Events — rastreamento de eventos e conversão](#52-events)
    - 5.3 [Push — configuração e tratamento de toques](#53-push)
    - 5.4 [In-App Messages](#54-in-app-messages)
6. [Fluxo de integração recomendado](#6-fluxo-recomendado)
7. [Deep Links por push](#7-deep-links)
8. [Referência rápida de métodos](#8-referência-rápida)
9. [Checklist final](#9-checklist)

---

## 1. Visão geral

A integração é feita por **um único ponto de entrada**: o objeto `InngageClient`. Você nunca precisa tocar em pacotes `internal.*` (repositórios, DTOs, use cases) — toda a superfície pública passa por `InngageClient`.

A SDK está dividida em quatro serviços, todos acessíveis pelo `InngageClient`:

| Serviço | Para que serve | Métodos do `InngageClient` |
|---|---|---|
| **Subscription** | Registrar o dispositivo e enriquecer o perfil do usuário | `subscribe`, `addUserData` |
| **Events** | Enviar eventos de analytics e conversões | `sendEvent` |
| **Push** | Configurar aparência e tratar toques em notificações | `configurePush`, `handleNotification` |
| **In-App** | Buscar e exibir mensagens In-App | `showInAppMessage` |

Os componentes internos que precisam ser registrados no Android (o serviço de recebimento FCM, o listener de refresh de token e as Activities de In-App) **já estão declarados no manifesto da própria SDK** e são incorporados ao seu app automaticamente por *manifest merging*. Você **não** precisa declará-los — diferente de versões antigas da SDK.

---

## 2. Requisitos

| Item | Valor |
|---|---|
| `minSdk` mínimo suportado pela SDK | **21** |
| `compileSdk` / `targetSdk` recomendado | **36** |
| Java / Kotlin target | **11** |
| Provedor de push | **Firebase Cloud Messaging (FCM)** |
| Conta Firebase | Obrigatória (arquivo `google-services.json`) |
| App Token Inngage | Obtido no painel da Inngage |

A SDK já traz suas próprias dependências transitivas (Firebase Messaging, WorkManager, Coroutines, Play Services Location, Glide, Chrome Custom Tabs, etc.). No seu app você só precisa aplicar o plugin do Google Services e incluir o Firebase BoM/Analytics.

---

## 3. Instalação

### 3.1 Firebase

A SDK usa FCM para push. Você precisa de um projeto Firebase e do arquivo `google-services.json`.

1. Crie/abra um projeto no [Firebase Console](https://console.firebase.google.com/).
2. Registre seu app Android usando o mesmo `applicationId` do seu `build.gradle`.
3. Baixe o `google-services.json` e coloque-o na **raiz do módulo `app/`**:
   ```
   app/google-services.json
   ```
4. Cadastre a chave do servidor FCM (ou credenciais do Firebase) no painel da Inngage, para que a plataforma consiga enviar push ao seu app.

### 3.2 Dependência da SDK

A SDK é distribuída via **JitPack** com a coordenada `com.github.inngage:inngage-lib`.

**`settings.gradle`** — adicione o repositório JitPack:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }   // <- adicione esta linha
    }
}
```

> Em projetos mais antigos que ainda usam `allprojects { repositories { … } }` no `build.gradle` raiz, adicione `maven { url 'https://jitpack.io' }` lá.

**`build.gradle` raiz (Project)** — declare os plugins do Android e do Google Services:

```groovy
plugins {
    id 'com.android.application' version '8.13.2' apply false
    id 'org.jetbrains.kotlin.android' version '2.2.0' apply false
    id 'com.google.gms.google-services' version '4.4.4' apply false
}
```

**`build.gradle` do módulo `app/`** — aplique os plugins e adicione as dependências:

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.gms.google-services'   // <- necessário para o FCM
}

android {
    compileSdk 36
    defaultConfig {
        minSdk 24        // mínimo do seu app (a SDK suporta a partir de 21)
        targetSdk 36
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = '11' }
}

dependencies {
    // === Inngage SDK ===
    // Use a tag da última release publicada no JitPack.
    implementation 'com.github.inngage:inngage-lib:5.0.0-beta01'

    // === Firebase (obrigatório para push) ===
    implementation platform('com.google.firebase:firebase-bom:34.11.0')
    implementation 'com.google.firebase:firebase-analytics'

    // === WorkManager ===
    // A SDK registra o dispositivo via WorkManager (SubscriptionWorker).
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
}
```

> **Versão:** substitua `5.0.0-beta01` pela última tag disponível em `https://jitpack.io/#inngage/inngage-lib`. A SDK segue SemVer; mudanças de API pública só ocorrem em versões MAJOR.

### 3.3 AndroidManifest.xml

Você **não precisa** declarar o serviço FCM, o listener de refresh de token nem as Activities de In-App — eles já vêm no manifesto da SDK e são mesclados no seu app.

O que você precisa declarar no **seu** `AndroidManifest.xml` são apenas as **permissões** que o seu app quer usar:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Obrigatória no Android 13+ (API 33) para exibir notificações.
         Deve ser solicitada em runtime antes do subscribe. -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Localização: NÃO precisa declarar — a SDK já declara ACCESS_COARSE_LOCATION
         e ACCESS_FINE_LOCATION e elas são mescladas no seu app automaticamente.
         Só são efetivamente usadas quando você chama subscribe(requestGeoLocation = true).
         Se o seu app NÃO usa geolocalização e você quer removê-las do manifesto final,
         veja a nota de "opt-out" logo abaixo. -->

    <application
        android:name=".SampleApplication"
        ... >
        <!-- Suas Activities aqui. -->
    </application>
</manifest>
```

> **Permissões declaradas pela própria SDK** (mescladas automaticamente no seu app, sem você precisar declará-las):
> `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `WAKE_LOCK`, `VIBRATE`, `READ_PHONE_STATE`, `ACCESS_COARSE_LOCATION` e `ACCESS_FINE_LOCATION`.
>
> Duas delas aparecem na ficha da Play Store e podem exigir atenção: `READ_PHONE_STATE` (usada pela camada legada de identificação de dispositivo) e as duas de **localização** (usadas apenas quando `requestGeoLocation = true`).
>
> **Opt-out:** se o seu app não usa geolocalização (ou não quer `READ_PHONE_STATE`), remova a permissão do manifesto final com `tools:node="remove"`:
> ```xml
> <manifest xmlns:android="http://schemas.android.com/apk/res/android"
>           xmlns:tools="http://schemas.android.com/tools">
>     <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"   tools:node="remove" />
>     <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" tools:node="remove" />
>     <uses-permission android:name="android.permission.READ_PHONE_STATE"       tools:node="remove" />
> </manifest>
> ```

**Permissões em runtime:** `POST_NOTIFICATIONS` (Android 13+) e as de localização precisam ser solicitadas em runtime. Veja o exemplo completo em [5.1 Subscription](#51-subscription).

### 3.4 Ícone de notificação

Como os pushes da Inngage chegam como *data messages* do FCM, **a SDK constrói cada notificação por conta própria** — o Firebase não aplica ícone/cor automaticamente. Você configura o ícone e a cor via `PushConfig` (ver [5.3 Push](#53-push)).

Coloque um ícone de status bar monocromático em `res/drawable/` (ex.: `ic_notification.xml`) e referencie-o no `PushConfig.smallIcon(R.drawable.ic_notification)`.

### 3.5 Constantes de integração

Recomendamos centralizar o App Token (e outras constantes) em um único arquivo, para desacoplar da lógica:

```kotlin
interface InngageConstants {
    companion object {
        /** Token do aplicativo na plataforma Inngage. */
        const val appToken: String = "SEU_APP_TOKEN"   // <- do painel da Inngage
    }
}
```

Uso: `InngageConstants.appToken`.

---

## 4. Ponto de entrada

Todo o consumo passa por `br.com.inngage.sdk.InngageClient`. É um `object` Kotlin com métodos `@JvmStatic` (chamáveis diretamente do Java como métodos estáticos) e `@JvmOverloads` (parâmetros opcionais também são visíveis do Java).

```kotlin
import br.com.inngage.sdk.InngageClient
```

Quick-start mínimo:

```kotlin
// Em Application.onCreate() ou na tela de login:
InngageClient.subscribe(context = this, appToken = "SEU_APP_TOKEN")

// Em Activity.onResume() (tela alvo do push):
InngageClient.handleNotification(context = this, intent = intent, appToken = "SEU_APP_TOKEN")
```

---

## 5. Módulos / Serviços

### 5.1 Subscription

**Registra o dispositivo na plataforma Inngage** e permite enriquecer o perfil do usuário depois.

#### `subscribe(...)` — registrar o dispositivo

Deve ser chamado no início do ciclo de vida do app (em `Application.onCreate()` ou na primeira Activity). O registro roda em background (via `WorkManager`, idempotente) e persiste o token FCM para uso pelos demais serviços.

**Identificação:**
- Se você **não passar** `identifier`, a SDK gera um **UUID de instalação** aleatório (persistido e reutilizado a cada abertura) e o envia como `identifier` e `uuid` — é um **subscriber anônimo**.
- Se você **passar** `identifier` (ex.: e-mail, CPF, ID do usuário), ele é enviado como está, e o `uuid` continua carregando o UUID de instalação.

**Assinatura:**

```kotlin
InngageClient.subscribe(
    context: Context,                    // Application context
    appToken: String,                    // token Inngage (mín. 8 caracteres)
    identifier: String? = null,          // opcional: identificador do usuário
    customFields: JSONObject? = null,    // opcional: atributos extras p/ segmentação
    email: String? = null,               // opcional
    phoneNumber: String? = null,         // opcional
    requestGeoLocation: Boolean = false  // opcional: inclui última localização conhecida
)
```

**Exemplo — subscribe anônimo com permissões em runtime** (na tela de login, como no app de exemplo):

```kotlin
class LoginActivity : AppCompatActivity() {

    // O subscribe roda independentemente de conceder/negar: a SDK lida com a
    // ausência de permissão graciosamente (sem coordenadas / sem notificação).
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { subscribeAnonymously() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        requestPermissionsThenSubscribe()
    }

    private fun requestPermissionsThenSubscribe() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) subscribeAnonymously()
        else permissionLauncher.launch(missing.toTypedArray())
    }

    private fun subscribeAnonymously() {
        InngageClient.subscribe(
            context            = this,
            appToken           = InngageConstants.appToken,
            requestGeoLocation = true
        )
    }
}
```

**Exemplo — subscribe já identificado com campos customizados:**

```kotlin
InngageClient.subscribe(
    context      = this,
    appToken     = InngageConstants.appToken,
    identifier   = "usuario@email.com",
    customFields = JSONObject().apply {
        put("nome", "Maria Silva")
        put("plano", "premium")
    },
    email        = "usuario@email.com",
    phoneNumber  = "11999998888",
    requestGeoLocation = true
)
```

> **Campos customizados** devem estar previamente cadastrados no painel da Inngage antes de serem enviados.

#### `addUserData(...)` — enriquecer o perfil depois do login

Fluxo típico: chame `subscribe` **anônimo** na tela de login (o registro roda em paralelo enquanto o usuário digita) e, depois do login, chame `addUserData` na tela principal para vincular os dados reais ao subscriber criado.

**Identificação:** se `identifier` for omitido, a SDK usa o identifier persistido pelo último `subscribe` (o UUID de instalação anônimo), de modo que os dados são vinculados ao mesmo subscriber. Ao menos um entre `identifier`, `customFields`, `email` ou `phoneNumber` deve ser fornecido — caso contrário a requisição é descartada (com log).

**Assinatura:**

```kotlin
InngageClient.addUserData(
    context: Context,
    appToken: String,
    identifier: String? = null,
    customFields: JSONObject? = null,
    email: String? = null,
    phoneNumber: String? = null
)
```

**Exemplo** (na Home, uma vez por sessão, como no app de exemplo):

```kotlin
private fun sendUserDataOnce() {
    val email = UserSession.email ?: return
    if (UserSession.userDataSent) return
    UserSession.userDataSent = true

    InngageClient.addUserData(
        context      = this,
        appToken     = InngageConstants.appToken,
        identifier   = email,
        customFields = JSONObject().apply {
            put("nome", "Demo User")
            put("plano", "premium")
        },
        email        = email,
        phoneNumber  = "81988887777"
    )
}
```

**Interop Java:**

```java
InngageClient.subscribe(context, "SEU_APP_TOKEN");
InngageClient.addUserData(context, "SEU_APP_TOKEN", "usuario@email.com",
        customFieldsJson, "usuario@email.com", "11999998888");
```

---

### 5.2 Events

**Envia eventos nomeados** para analytics e automações na Inngage. Também suporta eventos de **conversão**.

#### `sendEvent(...)`

**Identificação:** se `identifier` for fornecido, é enviado como `identifier`; caso contrário a SDK envia o **token FCM** persistido no `subscribe` como `registration`. Se nenhum dos dois estiver disponível, o evento é descartado (com log) — por isso é preciso ter chamado `subscribe` ao menos uma vez antes.

**Conversão:** defina `conversionEvent = true` para marcar o evento como conversão; só então `conversionValue` e `conversionNotid` são enviados.

**Assinatura:**

```kotlin
InngageClient.sendEvent(
    context: Context,
    appToken: String,
    eventName: String,                  // ex.: "purchase_completed"
    identifier: String? = null,         // deve casar com o usado no subscribe
    eventValues: JSONObject? = null,    // metadados do evento
    conversionValue: Any? = null,       // valor da conversão (número ou string)
    conversionNotid: String? = null,    // id da notificação ligada à conversão
    conversionEvent: Boolean = false    // marca como conversão
)
```

**Exemplo — evento simples:**

```kotlin
InngageClient.sendEvent(
    context     = this,
    appToken    = InngageConstants.appToken,
    eventName   = "product_viewed",
    identifier  = UserSession.email,   // ou null p/ usar o token FCM como fallback
    eventValues = JSONObject().apply {
        put("product_id", 42)
        put("product_name", "Camiseta")
        put("price", 79.90)
    }
)
```

**Exemplo — evento de conversão (compra concluída):**

```kotlin
InngageClient.sendEvent(
    context         = this,
    appToken        = InngageConstants.appToken,
    eventName       = "purchase",
    identifier      = UserSession.email,
    eventValues     = JSONObject().apply {
        put("items", itemsJsonArray)
        put("total", total)
    },
    conversionValue = total,
    conversionEvent = true
)
```

**Dica de organização:** centralize os eventos em um `object` para reaproveitar token e identifier, como no app de exemplo:

```kotlin
object StoreEvents {
    fun addToCart(context: Context, product: Product, origin: String) =
        send(context, "add_to_cart", JSONObject().apply {
            put("product_id", product.id)
            put("origin", origin)
        })

    private fun send(context: Context, name: String, values: JSONObject) =
        InngageClient.sendEvent(
            context     = context,
            appToken    = InngageConstants.appToken,
            eventName   = name,
            identifier  = UserSession.email,
            eventValues = values
        )
}
```

**Interop Java:**

```java
InngageClient.sendEvent(context, "SEU_APP_TOKEN", "product_viewed", "usuario@email.com", valuesJson);
```

---

### 5.3 Push

Cobre dois pontos: **configurar a aparência** das notificações e **tratar o toque** do usuário na notificação.

#### `configurePush(config)` — aparência das notificações

Chame **cedo**, em `Application.onCreate()`, para que a SDK consiga exibir notificações mesmo antes de qualquer Activity existir (ex.: app aberto a frio a partir de um push).

`PushConfig` é construído com um `Builder`:

```kotlin
import br.com.inngage.sdk.internal.service.push.domain.PushConfig

val pushConfig = PushConfig.Builder()
    .channelId("inngage_sample_channel")             // id do canal de notificação
    .channelName("Inngage Notifications")            // nome exibido nas configs do sistema
    .channelDescription("Notificações do app")       // descrição do canal
    .smallIcon(R.drawable.ic_notification)           // ícone da status bar (obrigatório na prática)
    .notificationColor(Color.parseColor("#7043CC"))  // cor de destaque (opcional)
    .targetActivity("com.seuapp.HomeActivity")       // Activity aberta ao tocar (opcional)
    .build()

InngageClient.configurePush(pushConfig)
```

| Método do Builder | Descrição | Padrão |
|---|---|---|
| `channelId(String)` | ID do canal (Android 8+) | `"inngage_default_channel"` |
| `channelName(String)` | Nome do canal exibido nas configurações | `"Notificações"` |
| `channelDescription(String)` | Descrição do canal | `"Canal padrão de notificações"` |
| `smallIcon(@DrawableRes Int)` | Ícone da status bar / small icon | ícone genérico do sistema |
| `notificationColor(Int)` | Cor de destaque (ARGB). `PushConfig.COLOR_UNSET` mantém a cor do sistema | `COLOR_UNSET` |
| `targetActivity(String)` | Nome totalmente qualificado da Activity aberta ao tocar | `null` |

> Como os pushes são *data messages*, `smallIcon` e `notificationColor` são aplicados pela SDK a **todas** as notificações, em foreground e background.

**Exemplo completo — `Application.onCreate()`** (como no app de exemplo):

```kotlin
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val pushConfig = PushConfig.Builder()
            .channelId("inngage_sample_channel")
            .channelName("Inngage Notifications")
            .channelDescription("Push notifications from Inngage SDK sample")
            .smallIcon(R.drawable.ic_notification)
            .targetActivity("com.example.inngage_java_app.HomeActivity")
            .build()
        InngageClient.configurePush(pushConfig)
    }
}
```

> Registre a `Application` no manifesto: `<application android:name=".SampleApplication" ...>`.

#### `handleNotification(...)` — tratar o toque na notificação

Processa o `Intent` de um toque em notificação. Deve ser chamado no **`onResume()`** da sua Activity alvo (a mesma configurada em `targetActivity`), pois assim funciona em todos os estados do app: foreground, background e morto (killed).

**O que ele faz, em ordem:**
1. Dispara o callback de abertura para a Inngage (`/v1/notification/` com `id`, `notid`, `app_token`).
2. Invoca `onNotificationClick` com o `NotificationPayload` completo (se fornecido).
3. Se `payload.type == "deep"` e `blockDeepLink == false` → abre `payload.url` no **navegador externo** padrão do dispositivo.
4. Se `payload.type == "inapp"` → abre `payload.url` numa **Chrome Custom Tab** dentro do app (nunca bloqueado).

**Assinatura:**

```kotlin
InngageClient.handleNotification(
    context: Context,
    intent: Intent,
    appToken: String,
    blockDeepLink: Boolean = false,   // true = pula navegação de deep-link externo
    onNotificationClick: ((NotificationPayload) -> Unit)? = null
)
```

**O objeto `NotificationPayload`** entregue ao callback:

| Propriedade | Tipo | Descrição |
|---|---|---|
| `notId` | `String` | ID da notificação Inngage (usado no callback de abertura) |
| `id` | `String` | ID duplicado presente em alguns payloads |
| `title` | `String` | Título da notificação |
| `body` | `String` | Texto da notificação |
| `type` | `String?` | `"deep"` (navegador externo), `"inapp"` (Custom Tab), ou `null` |
| `url` | `String?` | URL de destino associada ao `type` |
| `additionalData` | `String?` | JSON extra bruto anexado à notificação |

**Exemplo — na Activity alvo** (como a `HomeActivity` do app de exemplo):

```kotlin
class HomeActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()

        InngageClient.handleNotification(
            context       = this,
            intent        = intent,
            appToken      = InngageConstants.appToken,
            blockDeepLink = false
        ) { payload: NotificationPayload ->
            Log.d("Inngage", "notId=${payload.notId} type=${payload.type} url=${payload.url}")
            Toast.makeText(this, "Notificação: ${payload.title}", Toast.LENGTH_SHORT).show()
        }

        // Limpe os extras após processar para que onResume subsequentes
        // (ex.: retorno do navegador) não re-disparem a mesma notificação.
        intent.replaceExtras(Bundle())
    }

    // Necessário quando a Activity está na back stack (launchMode="singleTop"):
    // um novo toque chama onNewIntent em vez de onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
```

**Configuração de manifesto para a Activity alvo:** use `launchMode="singleTop"` para garantir que `onNewIntent` seja chamado quando um toque reabre a Activity já existente:

```xml
<activity
    android:name=".HomeActivity"
    android:exported="true"
    android:launchMode="singleTop" />
```

**Interop Java:**

```java
// Sem callback:
InngageClient.handleNotification(this, getIntent(), "SEU_APP_TOKEN");

// Com callback:
InngageClient.handleNotification(this, getIntent(), "SEU_APP_TOKEN", false, payload -> {
    // trate o payload
    return kotlin.Unit.INSTANCE;
});
```

---

### 5.4 In-App Messages

**Busca e exibe** uma mensagem In-App do backend da Inngage como um overlay em tela cheia (banners, carrosséis, botões e deep-links). É **independente** do push — chame quando quiser disparar a exibição (ex.: ao carregar telas-chave, depois do login).

A SDK consulta o endpoint da Inngage, valida a mensagem e a exibe. **A SDK não busca In-App sozinha** — quem decide quando chamar é o app.

> **Pré-requisito:** o `subscribe` precisa já ter persistido o `appId` e o token FCM; caso contrário a busca é ignorada silenciosamente. Na prática, chame `showInAppMessage` em telas que só aparecem após o `subscribe` (ex.: pós-login).

#### `showInAppMessage(...)`

**Quem trata as ações dos botões:**
- **`handledBySdk = true` (padrão):** a própria SDK resolve as ações (deep-link, browser, etc.). Os callbacks **não** são chamados.
- **`handledBySdk = false`:** a SDK **renderiza** a mensagem mas **não navega**. Ela entrega as ações ao app:
    - `onActions` dispara **uma vez**, ao exibir, com a lista completa de `InAppActionData`.
    - `onActionClick` dispara **a cada toque** em botão/fundo, com aquela ação específica (incluindo a `url`) — para o app abrir, por exemplo, a tela correspondente à URL do botão. O toque ainda fecha a mensagem.

**Assinatura:**

```kotlin
InngageClient.showInAppMessage(
    context: Context,
    handledBySdk: Boolean = true,
    onActions: ((List<InAppActionData>) -> Unit)? = null,
    onActionClick: ((InAppActionData) -> Unit)? = null
)
```

**O objeto `InAppActionData`** (uma entrada por botão e uma por fundo de slide):

| Propriedade | Tipo | Descrição |
|---|---|---|
| `slideIndex` | `Int` | Índice (base 0) do slide da ação |
| `buttonIndex` | `Int` | Índice (base 0) do botão no slide; `-1` para ação de fundo (`"background"`) |
| `source` | `String` | `"button"` ou `"background"` |
| `buttonText` | `String?` | Texto do botão; `null` para fundo |
| `type` | `String` | `"deeplink"`, `"weblink"`, `"in_app_url"`, `"metadata"` ou `"dismiss"` |
| `url` | `String?` | URL/URI de destino associada ao `type` |
| `metadata` | `Map<String, String>` | Pares chave-valor para ações `"metadata"` (vazio caso contrário) |

**Exemplo — modo padrão (SDK trata tudo)**, como na Home do app de exemplo:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...
    // Consulta a API a cada carregamento da tela; se houver mensagem pendente,
    // a SDK renderiza o overlay, senão não faz nada.
    InngageClient.showInAppMessage(this, handledBySdk = true)
}
```

**Exemplo — o app trata as ações (`handledBySdk = false`)**, como na tela de detalhes do app de exemplo:

```kotlin
InngageClient.showInAppMessage(
    context = this,
    handledBySdk = false,
    onActionClick = { action ->
        Log.d("Inngage", "slide=${action.slideIndex} botão=${action.buttonIndex} " +
                "'${action.buttonText}' type=${action.type} url=${action.url}")
        // Abra a tela certa conforme a URL do botão clicado:
        when (action.url) {
            "https://a" -> startActivity(CartActivity.newIntent(this))
            else -> Toast.makeText(this, "Botão → ${action.url}", Toast.LENGTH_SHORT).show()
        }
    }
)
```

**Interop Java:**

```java
// Modo padrão:
InngageClient.showInAppMessage(context);

// Com tratamento pelo app:
InngageClient.showInAppMessage(context, false, null, action -> {
    // trate a ação (action.getUrl(), action.getButtonText(), ...)
    return kotlin.Unit.INSTANCE;
});
```

---

## 6. Fluxo recomendado

O app de exemplo demonstra o fluxo mais robusto para apps com login:

```
┌──────────────────────────────────────────────────────────────────────┐
│ Application.onCreate()                                                  │
│   • configurePush(...)          → aparência das notificações (cedo)     │
├──────────────────────────────────────────────────────────────────────┤
│ LoginActivity (tela de login)                                          │
│   • pede permissões (POST_NOTIFICATIONS + localização)                 │
│   • subscribe(...) ANÔNIMO      → registro roda em paralelo             │
│   • sendEvent("login")          → no clique do botão                    │
├──────────────────────────────────────────────────────────────────────┤
│ HomeActivity (tela alvo do push, singleTop)                            │
│   • onCreate: addUserData(...)  → vincula dados reais ao subscriber     │
│   • onCreate: showInAppMessage(...)                                     │
│   • onResume: handleNotification(...) → trata toques de push            │
│   • ações do usuário: sendEvent("add_to_cart", ...) etc.               │
└──────────────────────────────────────────────────────────────────────┘
```

**Por que `subscribe` anônimo primeiro?** Porque o registro (e a persistência do token FCM) pode rodar em paralelo enquanto o usuário ainda digita as credenciais. Depois do login, `addUserData` enriquece o mesmo subscriber com e-mail, telefone e campos customizados.

---

## 7. Deep Links

Quando um push tem `type = "deep"` e `url = "meuapp://..."`, o `handleNotification` dispara um `Intent` `ACTION_VIEW` para essa URL. Para que uma Activity sua receba esse intent, declare um `intent-filter` no manifesto:

```xml
<activity
    android:name=".DeepLinkActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="inngage"
            android:host="deeplink" />
    </intent-filter>
</activity>
```

Na Activity, leia os parâmetros da URI (`intent.data`) e roteie conforme necessário:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val productId = intent?.data?.getQueryParameter("product")?.toIntOrNull()
    if (productId != null) {
        startActivity(ProductDetailActivity.newIntent(this, productId))
        finish()
    }
}
```

**Testar um deep link via adb:**

```bash
adb shell am start -a android.intent.action.VIEW -d "inngage://deeplink?product=3"
```

> Se quiser tratar a navegação você mesmo (sem abrir o navegador externo), passe `blockDeepLink = true` no `handleNotification` e use o callback `onNotificationClick` para rotear.

---

## 8. Referência rápida

| Método | Onde chamar | Assinatura resumida |
|---|---|---|
| `subscribe` | `Application.onCreate()` ou login | `subscribe(context, appToken, identifier?, customFields?, email?, phoneNumber?, requestGeoLocation?)` |
| `addUserData` | Após login | `addUserData(context, appToken, identifier?, customFields?, email?, phoneNumber?)` |
| `sendEvent` | Nas ações do usuário | `sendEvent(context, appToken, eventName, identifier?, eventValues?, conversionValue?, conversionNotid?, conversionEvent?)` |
| `configurePush` | `Application.onCreate()` | `configurePush(config: PushConfig)` |
| `handleNotification` | `Activity.onResume()` | `handleNotification(context, intent, appToken, blockDeepLink?, onNotificationClick?)` |
| `showInAppMessage` | `onCreate()`/telas-chave | `showInAppMessage(context, handledBySdk?, onActions?, onActionClick?)` |

Imports públicos usados:

```kotlin
import br.com.inngage.sdk.InngageClient
import br.com.inngage.sdk.internal.service.push.domain.PushConfig
import br.com.inngage.sdk.internal.service.notificationstatus.domain.NotificationPayload
import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionData
```

---

## 9. Checklist

- [ ] `google-services.json` adicionado em `app/`.
- [ ] Plugin `com.google.gms.google-services` aplicado no `app/build.gradle`.
- [ ] Repositório JitPack (`https://jitpack.io`) adicionado.
- [ ] Dependência `com.github.inngage:inngage-lib:<versão>` adicionada.
- [ ] Firebase BoM + `firebase-analytics` e `work-runtime-ktx` adicionados.
- [ ] Permissão `POST_NOTIFICATIONS` no manifesto + solicitada em runtime (Android 13+).
- [ ] (Opcional) Permissões de localização, se usar `requestGeoLocation = true`.
- [ ] Ícone `ic_notification` em `res/drawable/` e referenciado no `PushConfig.smallIcon`.
- [ ] `configurePush(...)` chamado em `Application.onCreate()`.
- [ ] `subscribe(...)` chamado (anônimo no login ou identificado).
- [ ] `handleNotification(...)` chamado no `onResume()` da Activity alvo (`singleTop`).
- [ ] `Activity` alvo casando com `PushConfig.targetActivity`.
- [ ] (Opcional) `addUserData(...)` após login.
- [ ] (Opcional) `showInAppMessage(...)` nas telas desejadas.
- [ ] (Opcional) `intent-filter` de deep link declarado.
- [ ] App e token cadastrados no painel da Inngage.

---

*Referência: superfície pública `br.com.inngage.sdk.InngageClient`. Nunca dependa de pacotes `internal.*` além dos modelos de domínio expostos pelos callbacks (`PushConfig`, `NotificationPayload`, `InAppActionData`). Alterações na API pública seguem SemVer e só ocorrem em versões MAJOR.*
