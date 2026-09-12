# Especificação executável

Rastreabilidade entre o enunciado do case, o comportamento implementado e o teste que o protege.

## Requisitos funcionais

### RF1 - Visualizar eventos disponíveis para compra

- Catálogo hardcoded em `data/catalog/EventCatalog.kt` (5 eventos), gravado no Room no primeiro
  acesso para permitir reserva transacional de estoque.
- Cada card mostra título, descrição, data/hora, local, preço unitário e um chip de estado:
  *N ingressos disponíveis*, *Esgotado* ou *Vendas não abertas*.
- O botão de compra é desabilitado quando `salesOpen == false` ou o estoque é zero
  (`Event.isPurchasable`).

### RF2 - Selecionar a quantidade de ingressos

- Seletor `-`/`+` com total recalculado (`quantidade × preço unitário`, sempre em centavos).
- Limites: mínimo 1, máximo `Event.MAX_TICKETS_PER_PURCHASE` (6) e nunca acima do estoque.
- Violações produzem mensagens específicas ("Máximo de 6 ingressos por compra.", "Restam apenas N
  ingressos.").
- Testes: `StartCheckoutUseCaseTest` (limite, estoque, vendas fechadas, evento inexistente).

### RF3 - Iniciar e concluir o pagamento via integração com a Cielo

- Integração por **deep link**: `lio://payment?request=<base64(json)>&urlCallback=order://response`.
- Formas de pagamento oferecidas: `CREDITO_AVISTA`, `DEBITO_AVISTA` e `PIX`.
- Payload conforme a documentação (`accessToken`, `clientID`, `reference`, `merchantCode`,
  `installments`, `items[]`, `paymentCode`, `value` em centavos como string).
- Retorno pelo `intent-filter` `order://response` declarado na `MainActivity`
  (`launchMode="singleTask"` → `onNewIntent`).
- Testes: `CieloPaymentRequestFactoryTest` (formato do request), `CieloCallbackParserTest`
  (todos os retornos), `StartPaymentUseCaseTest` (orquestração).

### RF4 - Registrar o resultado da compra (aprovada, negada ou cancelada)

| Retorno da Cielo                                                   | Estado da compra |
|--------------------------------------------------------------------|------------------|
| Ordem com `paymentFields.statusCode` = `1` (ou `0`, Pix)           | `APPROVED`       |
| `{"code":1}` cancelado pelo usuário                                | `CANCELLED`      |
| `{"code":3}` erro no pagamento / `statusCode` = `2`                | `DENIED`         |
| `{"code":2}`, `{"code":4}`, payload ilegível, falha ao abrir o app | `FAILED`         |

- O resultado é gravado no momento em que o callback chega (`CieloCallbackHandler` → escopo de
  aplicação), não quando alguma tela está visível.
- Compras não aprovadas devolvem o estoque reservado.
- Testes: `RegisterPaymentResultUseCaseTest`, `PurchaseSettlementTest`,
  `RoomPurchaseRepositoryTest` (instrumentado).

### RF5 - Exibir comprovante/resumo da compra

- Tela de comprovante com pedido, quantidade, total e dados da transação: autorização
  (`authCode`), código Cielo, produto, bandeira/máscara do cartão, terminal e data.
- Tela de resultado com estado explícito e ações: *Ver comprovante* (aprovado) ou *Tentar
  novamente* (negado/cancelado/falha).

### RF6 (opcional) - QR Code vinculado à compra concluída

- Ingressos só existem para compras `APPROVED` (`PurchaseSettlement` decide `issueTickets`).
- Um ingresso por unidade comprada, com `sequence` 1..N e índice único `(purchaseId, sequence)`.
- Conteúdo do QR: `CC1|<purchaseId>|<ticketId>|<eventId>|<sequence>|<assinatura>`, onde a
  assinatura é `SHA-256(corpo + authCode da Cielo + segredo)` truncada — ou seja, o ingresso está
  criptograficamente ligado ao pagamento aprovado.
- Testes: `TicketQrPayloadTest`, `RegisterPaymentResultUseCaseTest`.

## Requisitos não-funcionais

### RNF1 - Tratamento explícito de erros de integração e pagamento

`PaymentFailureKind` distingue: `MISSING_CREDENTIALS`, `CIELO_APP_UNAVAILABLE`, `USER_CANCELLED`,
`PAYMENT_DENIED`, `AUTHENTICATION`, `INVALID_RESPONSE`, `GENERIC`. Cada um tem mensagem própria
(`ui/payment/PaymentMessages.kt`) e distingue "nada foi cobrado" de "pagamento recusado".

### RNF2 - Evitar duplicidade de cobrança em reenvio de ação

Quatro camadas (detalhadas na seção 6 do README): idempotency key (`reference` = id da compra),
reuso do pedido pendente, guard de pagamento em voo (3 min) e liquidação única em transação.

Testes dedicados: `StartPaymentUseCaseTest` ("does not launch a second payment while one is in
flight", "never pays a purchase that is already settled"), `RegisterPaymentResultUseCaseTest`
("a duplicated callback does not issue tickets twice") e `RoomPurchaseRepositoryTest`
("settle_isAppliedOnlyOnce_forDuplicatedCallbacks").

### RNF3 - Código organizado e de fácil manutenção

Camadas `domain` (sem Android) → `data` → `ui`; regras de decisão em funções puras; contrato da
Cielo isolado em `data/payment/cielo`; detekt no build.

### RNF4 - Testes automatizados para cenários críticos

46 testes JVM + 4 instrumentados (Room). Cobrem contrato do deep link, parsing de retornos reais,
regras de quantidade/estoque, duplicidade de cobrança, emissão de ingressos, QR Code e remoção de
credenciais de mensagens de erro (`SensitiveDataTest`).

### RNF5 - Uso de IA como suporte à implementação

Documentado em [`AI-USAGE.md`](AI-USAGE.md).

## Fora de escopo

- Backend de apoio (explicitamente não avaliado).
- Cancelamento/estorno (`lio://payment-reversal`), impressão de comprovante no terminal
  (`lio://print`) e consulta de pedidos (`lio://orders`) — contratos mapeados, UI não implementada.
