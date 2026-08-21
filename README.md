# Inngage Android SDK — Java line (4.2.x)

Integração da **Inngage Android SDK** (push notifications, in-app messages e event
tracking) na linha **Java 4.2.x**.

> **Qual linha usar?**
> Este repositório mantém duas linhas independentes:
> - **`4.2.x` (esta branch/tag)** — linha **Java**, API `br.com.inngage.sdk.InngageService`.
>   Sem Kotlin/coroutines. É a versão para apps existentes que já integram a SDK Java.
> - **`5.0.0` (branch `main`)** — linha **Kotlin**, API `br.com.inngage.sdk.InngageClient`.
>
> As duas se distribuem pelo mesmo artefato JitPack (`com.github.inngage:inngage-lib`),
> escolhido pela versão. Este guia cobre **somente a linha Java 4.2.x**.

---

## 1. Instalação (JitPack)

No `settings.gradle` (ou `build.gradle` raiz) adicione o repositório JitPack:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

E a dependência no `build.gradle` do módulo do app:

```gradle
dependencies {
    implementation 'com.github.inngage:inngage-lib:v4.2.1'
}
```

A SDK usa **Firebase Cloud Messaging**. Configure o Firebase no app e inclua o
`google-services.json`, aplicando o plugin `com.google.gms.google-services`.

## 2. AndroidManifest

A SDK já declara as permissões necessárias (INTERNET, localização, etc.) via
merge do manifesto. O app consumidor precisa **registrar o serviço de push** que
recebe as mensagens do FCM:

```xml
<application>
    <service
        android:name="br.com.inngage.sdk.PushMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
</application>
```

## 3. Configuração do canal de notificação

Configure o canal (Android 8+), ícone e a Activity de destino uma vez, por
exemplo no `Application.onCreate()`. `InngagePushConfig` é um singleton fluente:

```java
InngagePushConfig.getInstance()
        .setChannelId("default_channel")
        .setChannelName("Geral")
        .setChannelDescription("Notificações gerais")
        .setSmallIcon(R.drawable.ic_notification)
        .setTargetActivity("com.seuapp.HomeActivity");
```

## 4. Subscribe (registro / identificação do usuário)

`InngageService.subscribe(...)` registra o dispositivo. Roda em background
(WorkManager) e é idempotente. Há sobrecargas do mais simples ao mais completo:

```java
// Anônimo (sem identifier): a SDK gera um id anônimo estável automaticamente.
InngageService.subscribe(context, APP_TOKEN, "prod", "FCM");

// Identificado, com custom fields, e-mail, telefone e geolocalização:
JSONObject customFields = new JSONObject();
customFields.put("plano", "premium");

InngageService.subscribe(
        context,
        APP_TOKEN,        // app token da Inngage
        "prod",           // ambiente: "prod" ou "dev"
        "FCM",            // provider de push
        "user@email.com", // identifier (ou null/"" para anônimo)
        customFields,     // JSONObject ou null
        "user@email.com", // email ou null
        "11999999999",    // telefone ou null
        true              // requestGeoLocator (captura lat/long se permitido)
);
```

Notas:
- **`identifier`**: quando vazio ou `null`, a SDK usa um id anônimo **estável e
  persistido** (`ANDROID_ID`, com fallback para um `UUID` aleatório) — nunca envia
  identifier vazio, e o mesmo dispositivo mapeia para um único assinante.
- **Geolocalização**: com `requestGeoLocator = true`, a captura é limitada por um
  timeout de 5s e roda fora da main thread; se não houver permissão ou fix, o
  subscribe segue normalmente **sem** coordenadas.

## 5. Eventos

`InngageService.sendEvent(...)` — sobrecargas para evento simples, com valores e
com conversão:

```java
// Evento simples
InngageService.sendEvent(APP_TOKEN, "user@email.com", "login");

// Com event_values
JSONObject values = new JSONObject();
values.put("origem", "home");
InngageService.sendEvent(APP_TOKEN, "user@email.com", "banner_click", values);

// Evento de conversão (ex.: compra)
JSONObject compra = new JSONObject();
compra.put("total", 99.90);
InngageService.sendEvent(
        APP_TOKEN, "user@email.com", "purchase", compra,
        true,     // conversionEvent
        99.90f,   // conversionValue
        null      // conversionId (opcional)
);
```

O endpoint de eventos segue o ambiente do último `subscribe` (`prod`/`dev`).

## 6. Tratando o clique na notificação

Na Activity de destino (a mesma de `setTargetActivity`), encaminhe o `Intent`
para a SDK registrar o callback de abertura e tratar deep links:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ...
    InngageUtils.handleNotification(this, getIntent(), APP_TOKEN, "prod");
}
```

## 7. In-App Messages

```java
new InAppUtils().startInApp(context);
```

---

## Compatibilidade

- **minSdk**: 21 · **compileSdk/targetSdk**: 36
- **Java-only** (sem Kotlin/coroutines nesta linha)
- Requer Firebase Cloud Messaging (`google-services.json`)

## Changelog

Veja [`CHANGELOG.md`](CHANGELOG.md). Releases seguem [SemVer](https://semver.org/).
