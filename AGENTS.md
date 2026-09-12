# Cielo Case - notas do projeto

App Android (Kotlin/Compose) de venda de ingressos com pagamento na Cielo Smart via deep link.
Visão geral em [`README.md`](README.md); decisões em [`docs/`](docs).

## Comandos

```bash
./gradlew :app:assembleDebug              # build
./gradlew :app:testDebugUnitTest          # testes JVM (46)
./gradlew :app:connectedDebugAndroidTest  # testes instrumentados (Room) - precisa de device
./gradlew :app:detekt                     # análise estática (config em detekt/detekt.yml)
./gradlew :app:installDebug               # instala no device conectado
```

Verificação completa antes de entregar:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:detekt
```

## Convenções

- `domain/` é Kotlin puro (sem Android/Room/Gson) — regras críticas testáveis na JVM.
- Valores monetários sempre em **centavos** (`Long`); formatação só na UI.
- Textos de usuário em `res/values/strings.xml` (pt-BR).
- Dependências declaradas em `gradle/libs.versions.toml`.
- Credenciais da Cielo vêm de `local.properties`/ambiente
  (`CIELO_CLIENT_ID`, `CIELO_ACCESS_TOKEN`, `CIELO_MERCHANT_CODE`) → `BuildConfig`.
- Texto que possa conter o deep link de pagamento (mensagens de erro, logs, campo `reason`) deve
  passar por `SensitiveData.redact()`: o Base64 do `request=` embute `accessToken`/`clientID`.

## Observabilidade (Sentry)

- Opt-in: só inicializa se `SENTRY_DSN` estiver definido em `local.properties`/ambiente
  (`App.onCreate()` → `SentryConfig`); auto-init desligado no manifesto.
- Apenas `sentry-android-core`; `autoInstallation`/`tracingInstrumentation` desligados no bloco
  `sentry { }` do `app/build.gradle.kts` — não reative sem necessidade (traz replay/NDK).
- `isSendDefaultPii = false`, sem screenshot/view hierarchy, sem tracing, NDK off, e `beforeSend`
  com scrubbing de credenciais.
- Validação: `adb logcat -d | grep -i sentry` deve mostrar `Initializing SDK with DSN`,
  `NdkIntegration enabled: false` e **nenhum** `Failed to register network callback`.

## Integração Cielo Smart (deep link)

- Request: `lio://payment?request=<base64(json)>&urlCallback=order://response`
- Callback: `order://response?response=<base64(json)>&responsecode=0` (tratado em
  `MainActivity` → `CieloCallbackHandler`)
- O app de pagamento pode ser `com.ads.lio.uriappclient` (doc) **ou**
  `br.com.cielosmart.orderservice` (emulador/terminais atuais); ambos declarados em `<queries>`.
- O Base64 de resposta pode conter quebras de linha (`Base64.DEFAULT`): não usar parsers de URI
  estritos.
- Em builds debug há um simulador de callback na tela de revisão para testar sem o emulador.

## Testes manuais no device

```bash
adb shell monkey -p com.example.cielocase -c android.intent.category.LAUNCHER 1
adb shell dumpsys activity activities | grep mResumedActivity   # ver qual app está em foreground
adb logcat -d | grep "integrator url callback"                  # inspecionar o callback da Cielo
```

Cuidado: `adb shell am start -d "...&..."` corta a URI no `&`; use o app ou escape corretamente.
