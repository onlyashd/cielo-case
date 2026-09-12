# Uso de IA na construção da solução

Este documento descreve **como** a IA foi usada (não apenas que foi usada): o harness, os prompts,
as restrições impostas e, principalmente, os **resultados que orientaram decisões** de
implementação.

## 1. Harness

| Item               | Valor                                                                                                  |
|--------------------|--------------------------------------------------------------------------------------------------------|
| Agente             | Devin CLI (agente de linha de comando com acesso a shell, edição de arquivos e busca)                  |
| Modelo             | Claude Opus (raciocínio médio)                                                                         |
| Ferramentas usadas | leitura/edição de arquivos, `grep`/glob, shell (`gradlew`, `curl`, `adb`, `python3`), lista de tarefas |
| Ambiente           | macOS, JDK 21, Android SDK (API 37), AVD Android 12 com o **emulador Cielo Smart** instalado           |
| Ponto de partida   | repositório com scaffold Compose/Hilt/Room/detekt e telas vazias (`// todo`), commit `wip`             |

Ciclo de trabalho adotado: **pesquisar o contrato real → implementar em camadas → verificar
(compilação, testes, detekt) → validar no device com o emulador Cielo → corrigir → documentar**.

## 2. Prompts que direcionaram o trabalho

1. **Prompt inicial (humano):** o enunciado completo do case (funcionalidades, requisitos
   funcionais e não-funcionais, restrições técnicas, links da Cielo Smart).
2. **Decisão de integração (humano):** *"implemente via deeplink"* — respondendo à dúvida do
   agente sobre qual modelo de integração usar (SDK Order Manager x deep link). Essa foi a
   principal escolha de arquitetura delegada ao humano.
3. **Contexto de ambiente (humano):** *"i've installed the cielo emulator on the running adb
   device"* — habilitou a validação ponta a ponta real, não apenas simulada.

## 3. Restrições impostas ao agente

- **Não inventar API de terceiros.** O contrato do deep link foi extraído da documentação oficial
  antes de escrever código. O agente identificou que as páginas da Cielo são SPA e obteve o
  markdown puro usando o padrão `<url>.md` do Readme.io:
  ```bash
  curl -sL https://docs.cielo.com.br/cielo-smart/docs/pagamento.md
  curl -sL https://docs.cielo.com.br/cielo-smart/docs/recuperando-dados.md
  curl -sL https://docs.cielo.com.br/cielo-smart/docs/codigos-de-erro.md
  curl -sL https://docs.cielo.com.br/cielo-smart/docs/configurando-o-android-manifest.md
  ```
  O formato exato da URI (`lio://payment?request=...&urlCallback=...`) foi confirmado cruzando a
  documentação de cancelamento/impressão com o
  [sample oficial em React Native](https://github.com/matheus-caldeira/cielo_sample)
  (`android/.../payment/Payment.java`).
- **Nenhum segredo no repositório.** Credenciais vêm de `local.properties`/ambiente; o
  `local.properties` está no `.gitignore`.
- **Dependências conservadoras.** Apenas Gson, ZXing e `kotlinx-coroutines-test`/`room-testing`,
  em versões estáveis e publicadas há tempo (verificadas no Maven Central antes de adicionar).
- **Respeitar as convenções existentes** do scaffold: Hilt, Room, Compose Material 3, detekt,
  Sentry opcional, `libs.versions.toml`.
- **Testes como critério de pronto**, com os cenários críticos definidos antes da implementação
  (contrato do deep link, duplicidade de cobrança, idempotência de callback, ingresso apenas para
  compra aprovada).

## 4. Resultados que orientaram a implementação

Três descobertas empíricas mudaram o código — nenhuma delas estava na documentação:

### 4.1 O pacote do app de pagamento não é o documentado

```bash
$ adb shell pm list packages | grep -i cielo
package:br.com.cielosmart.orderservice

$ adb shell dumpsys package br.com.cielosmart.orderservice | grep -A6 Schemes
  Schemes:
      lio:
        br.com.cielosmart.orderservice/com.cielo.lio.uriappclient.activities.CheckoutActivity
          Scheme: "lio"  Authority: "payment"
```

A documentação instrui declarar `com.ads.lio.uriappclient` em `<queries>`. Se o app dependesse
apenas disso, `resolveActivity` retornaria `null` no Android 11+ e o pagamento nunca abriria.
**Resultado:** o manifesto declara os dois pacotes e também um `<intent>` com `scheme="lio"`.

### 4.2 O Base64 do callback vem com quebras de linha

No primeiro teste ponta a ponta pelo app, o pagamento foi aprovado no emulador mas a tela ficou em
"Aguardando o retorno da Cielo…". O log mostrou o callback chegando com o payload quebrado em
linhas (`Base64.DEFAULT` do Android insere `\n` a cada 76 caracteres), e o parser — que usava
`java.net.URI` — descartava a URI como inválida.

**Resultado:** `CieloCallbackParser` passou a fazer parsing manual do query string e a ignorar
espaços em branco antes de decodificar; foi criado o teste
`parses payloads whose base64 contains line breaks`. Depois da correção o fluxo real terminou em
*Pagamento aprovado* com comprovante e QR Code.

### 4.3 `adb shell am start` engole o `&` da URI

Durante a investigação, disparar o deep link pelo shell produziu
`calling integrator url callback: null?response=...` — o `&urlCallback=` havia sido cortado pelo
shell, e não pelo app. Serviu para confirmar que **sem `urlCallback` a Cielo não devolve o
resultado** e para evitar uma "correção" em cima de um diagnóstico errado.

### 4.4 Verificações automatizadas que apontaram ajustes

- `testDebugUnitTest`: 46 testes; uma falha real de teste (assert de `statusCode` que não casava
  com o JSON formatado) foi corrigida antes de seguir.
- `connectedDebugAndroidTest`: 4 testes de transação Room passando no device.
- `detekt`: 15 apontamentos resolvidos — constantes movidas para objetos, arquivo em pacote
  divergente (`util/observability/Throwable.kt` → `util/extensions/`), número mágico nomeado — e
  duas regras ajustadas com justificativa no `detekt.yml` (`LongMethod` ignora `@Composable`;
  `ReturnCount` permite *guard clauses*, padrão de validação usado no domínio).

## 5. O que foi decidido por julgamento (humano/agente) e não por documentação

- Modelar `PaymentGateway` no domínio para poder testar os casos de uso sem Android (ADR 2).
- Gravar o resultado do pagamento fora do ciclo de vida das telas (ADR 3) — motivado pelo aviso da
  própria Cielo de que o app de integração pode ser morto durante o pagamento.
- Estratégia de idempotência em quatro camadas e a regra "retry de compra recusada cria novo
  pedido" (ADR 4), porque reaproveitar um `reference` já liquidado é exatamente o caminho para a
  cobrança duplicada.
- Assinar o QR Code com o `authCode` da Cielo (ADR 9), garantindo o vínculo exigido pelo case
  ("o ingresso deve estar vinculado à compra concluída").

## 6. Limites do uso de IA neste projeto

- A IA não teve acesso a credenciais reais do Portal de Desenvolvedores; a validação ponta a ponta
  usou credenciais *placeholder*, aceitas pelo emulador. Antes de rodar em terminal físico é
  necessário preencher `CIELO_CLIENT_ID`/`CIELO_ACCESS_TOKEN` reais.
- Fluxos não exigidos pelo case (estorno, impressão, consulta de pedidos) foram deliberadamente
  deixados de fora, com o contrato mapeado em `CieloDeeplink` para extensão futura.
