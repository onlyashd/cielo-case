# Decisões arquiteturais

## Visão geral

```
        ui (Compose + ViewModels)
                 │  observa Flows / dispara casos de uso
        domain (puro Kotlin)
         ├── model: Event, Purchase, Ticket, TicketQrPayload
         ├── payment: PaymentGateway, PaymentOutcome, PurchaseSettlement
         └── usecase: StartCheckout, StartPayment, RegisterPaymentResult
                 │  interfaces (EventRepository, PurchaseRepository, PaymentGateway)
        data
         ├── local: Room (events, purchases, tickets)
         ├── catalog: eventos hardcoded
         └── payment/cielo: request factory, callback parser, gateway, callback handler
```

O `domain` não conhece Android, Room nem Gson. Isso permite testar as regras críticas na JVM, sem
emulador — inclusive todo o formato de fio da integração.

## ADR 1 - Integração por deep link, não por SDK

**Decisão:** usar `lio://payment` + callback `order://response`, conforme
[Deep Link: Exemplo de código](https://docs.cielo.com.br/cielo-smart/docs/deep-link-exemplo-de-codigo).

**Porque:** foi a forma pedida no case e é a recomendada pela própria Cielo para integrações
locais (sem dependência de SDK, menos conflito de bibliotecas). O contrato é pequeno e estável:
JSON → Base64 → parâmetro `request`, com retorno também em Base64 no parâmetro `response`.

**Consequência:** o resultado é assíncrono e *out-of-process*. O app precisa (a) de um
`intent-filter` de resposta, (b) `launchMode="singleTask"` para receber o retorno em
`onNewIntent`, e (c) de estado persistente, pois o processo pode morrer enquanto a Cielo está em
foreground.

## ADR 2 - `PaymentGateway` como interface de domínio

O domínio depende da abstração `PaymentGateway` (`start(request): PaymentLaunchResult`). A
implementação real (`CieloDeeplinkGateway`) é a única classe que toca `Intent`/`PackageManager`.

Benefícios: os casos de uso são testados com `FakePaymentGateway`; adicionar outro meio de captura
(SDK, integração remota) não muda o domínio; erros de *lançamento* (credenciais ausentes, app da
Cielo indisponível) são modelados explicitamente e diferenciados de recusa de pagamento.

## ADR 3 - Fonte da verdade é o banco, não a tela

O callback é tratado por `CieloCallbackHandler`, que roda em um `CoroutineScope` de aplicação e
grava o resultado imediatamente. As telas apenas observam a compra (`Flow<Purchase?>`) e os
ingressos.

Consequência: um pagamento aprovado nunca é perdido porque o usuário saiu da tela, e a tela de
resultado é reconstruída corretamente após *process death*.

## ADR 4 - Idempotência baseada em `reference`

O `id` da compra (UUID) é criado **antes** do pagamento e enviado como `reference`. Ele é a chave
de idempotência de ponta a ponta:

- reenvio do checkout reaproveita a compra pendente;
- `StartPaymentUseCase` recusa pagar compra liquidada ou com pagamento em voo (janela de 3
  minutos; depois permite retry com o **mesmo** `reference`);
- `settle()` roda em `withTransaction` e trata callback de compra já liquidada como no-op;
- ingressos têm índice único `(purchaseId, sequence)`.

Os payloads de **erro** da Cielo (`{"code":1,"reason":"..."}`) não trazem `reference`; por isso o
`reference` em voo é persistido em `SharedPreferences` (`InFlightPaymentStore`) e usado como
fallback — sobrevive à morte do processo.

**Retry de compra recusada cria um novo pedido**, nunca reaproveita uma compra liquidada.

## ADR 5 - Decisão de liquidação como função pura

`PurchaseSettlement.decide(purchase, outcome, now)` retorna `Settle(purchase, issueTickets)` ou
`AlreadySettled(purchase)`. O repositório Room só aplica a decisão dentro de uma transação.

Assim a regra mais sensível do sistema (o que cobra, o que emite ingresso, o que é duplicado) é
testada sem banco, e a implementação Room é coberta por testes instrumentados.

## ADR 6 - Estoque reservado na criação da compra

`EventDao.reserveTickets` usa `UPDATE ... WHERE availableTickets >= :quantity` e o número de linhas
afetadas indica sucesso — reserva atômica, sem race entre checkouts concorrentes. Resultados não
aprovados devolvem o estoque (`releaseTickets`).

## ADR 7 - Simulador que usa o caminho de produção

`SimulatedCallbackFactory` gera **o mesmo deep link** que a Cielo enviaria, e o `PaymentSimulator`
o injeta no `CieloCallbackHandler`. Nada é "curto-circuitado": parser, liquidação, emissão de
ingressos e telas são os de produção. Fica disponível apenas em debug
(`BuildConfig.PAYMENT_SIMULATOR_ENABLED`) e é reaproveitado nos testes
(`SimulatedCallbackFactoryTest`).

## ADR 8 - Valores sempre em centavos (`Long`)

Preços, totais e valores de pagamento são `Long` em centavos, igual ao contrato da Cielo
(`value: "10000"`). Formatação para exibição só na camada de UI (`formatAsCurrency`).

## ADR 9 - QR Code assinado e vinculado ao pagamento

`CC1|<purchaseId>|<ticketId>|<eventId>|<sequence>|<assinatura>`, assinatura =
`SHA-256(corpo|authCode|segredo)` truncada em 16 hex. Inclui o `authCode` retornado pela Cielo, o
que garante o vínculo com a compra concluída e permite validação offline no portão.

Em produção o segredo ficaria no backend de bilheteria; aqui é uma constante demonstrativa.

## ADR 10 - Credenciais fora do versionamento

`local.properties`/variáveis de ambiente → `BuildConfig.CIELO_CLIENT_ID` /
`CIELO_ACCESS_TOKEN` / `CIELO_MERCHANT_CODE`. Ausência de credenciais é um erro de domínio
explícito (`MISSING_CREDENTIALS`), não um crash nem uma tentativa de pagamento.

## ADR 11 - Observabilidade opt-in e com dados sensíveis removidos

O Sentry veio do scaffold, mas a configuração estava inerte (DSN literal vazio) enquanto o plugin
empacotava todas as integrações. Decisões tomadas na auditoria:

- **Opt-in real:** `SENTRY_DSN` vem de `local.properties`/ambiente; `App.onCreate()` só chama
  `SentryConfig` se o DSN não estiver vazio, e o auto-init do manifesto continua desligado.
- **Superfície mínima:** apenas `io.sentry:sentry-android-core`; `autoInstallation` e
  `tracingInstrumentation` desligados no bloco `sentry { }` (antes vinham session replay, NDK,
  Compose, Fragment, Navigation e SQLite — 9 artefatos e bibliotecas nativas no APK; agora são 2).
- **Privacidade por padrão:** `isSendDefaultPii = false`, sem screenshot/view hierarchy, sem
  tracing e sem NDK — coerente com um app que manipula pagamento.
- **Scrubbing obrigatório:** o Base64 do deep link contém `accessToken`/`clientID`, e mensagens de
  exceção do Android ecoam a Intent inteira. `SensitiveData.redact()` é aplicado em
  `Throwable.log()`, no `reason` persistido da compra e no `beforeSend` do Sentry.
- **Permissões declaradas explicitamente:** `INTERNET`/`ACCESS_NETWORK_STATE` existem só para o
  envio de eventos; sem elas o SDK inicializava mas não conseguia registrar o callback de rede nem
  entregar nada (comportamento observado em logcat antes da correção).

Nada de build-time: sem upload de mapping/source context (não há auth token no build) e sem
telemetria do plugin.

## Achados de campo (validados no emulador Cielo Smart)

1. **Pacote do app de pagamento** - a documentação manda declarar
   `<package android:name="com.ads.lio.uriappclient" />` em `<queries>`, mas o emulador atual
   publica os deep links em `br.com.cielosmart.orderservice`. O manifesto declara os dois pacotes
   **e** um `<intent>` com `android:scheme="lio"`, para não depender do nome do pacote.
2. **Base64 com quebras de linha** - o retorno usa `Base64.DEFAULT` (Android), que insere `\n` a
   cada 76 caracteres. `java.net.URI`/parsers estritos rejeitam a URI de callback. O
   `CieloCallbackParser` faz o parsing manual do query string e ignora espaços em branco antes de
   decodificar. Caso coberto pelo teste
   `parses payloads whose base64 contains line breaks`.
3. **`urlCallback` é obrigatório na URI** - sem ele o SDK da Cielo registra
   `calling integrator url callback: null?response=...` e o resultado nunca volta.
