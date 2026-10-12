# Assignment 3 — Multi-Channel Notification Gateway (Bridge Pattern)

**Course:** Software Design Patterns
**Pattern:** Bridge
**Language:** Java 17+
**Domain:** notifications (what is sent) × delivery providers (how it is sent). All providers are simulated.

## Project structure

```text
assignment-3-bridge/
├── src/main/java/
│   ├── MessageSender.java                  Implementor (interface)
│   ├── SimulatedSender.java                shared skeleton for simulated providers
│   ├── TelegramMessageSender.java          Concrete Implementors
│   ├── EmailMessageSender.java
│   ├── SmsMessageSender.java
│   ├── KafkaEventSender.java
│   ├── PushNotificationSender.java         (added later - extension)
│   ├── Notification.java                   Abstraction
│   ├── UrgentNotification.java             Refined Abstractions
│   ├── EncryptedNotification.java
│   ├── ScheduledBatchNotification.java
│   ├── MarketingNotification.java          (added later - extension)
│   ├── BatchSummary.java                   result of a batch send
│   └── Main.java                           demo (Client)
├── src/test/java/                          37 JUnit 5 tests in 3 files
├── docs/bridge-uml.png, bridge-uml.dot     UML class diagram
├── pom.xml
└── README.md
```

## Run

```bash
mvn clean test
```

Run `Main.java` from the IDE to see the demo.

## What each part does

| Part | Role in Bridge | Behaviour |
|---|---|---|
| `Notification` | Abstraction | Holds `sender` (the bridge), `setSender()` for runtime switching |
| `UrgentNotification` | Refined Abstraction | Adds `[URGENT]`, checks availability, tries fallback senders in order |
| `EncryptedNotification` | Refined Abstraction | XOR + Base64 before sending (toy cipher) |
| `ScheduledBatchNotification` | Refined Abstraction | Many recipients, returns `Total / Successful / Failed` |
| `MarketingNotification` | Refined Abstraction (extension) | `[AD]` mark, unsubscribe footer, respects opt-out |
| `MessageSender` | Implementor | `sendMessage`, `isAvailable`, `getChannelName` |
| Telegram | Concrete Implementor | formatted message (bold title) |
| Email | Concrete Implementor | subject line + signature |
| SMS | Concrete Implementor | text limited to 160 characters (configurable) |
| Kafka | Concrete Implementor | publishes an event with a growing offset |
| Push | Concrete Implementor (extension) | short push card, body limited to 120 characters |

## Mandatory demo scenarios (`Main.java`)

1. `UrgentNotification` + Telegram
2. `EncryptedNotification` + Email
3. `ScheduledBatchNotification` + SMS (5 recipients, 1 rejected by the provider → `Total: 5 / Successful: 4 / Failed: 1`)
4. Runtime switching: the same `Notification` object, `setSender(email)` after Telegram
5. Telegram unavailable → `UrgentNotification` falls back to SMS
6. `MarketingNotification` + `PushNotificationSender`

There is one bonus line (`EncryptedNotification` + Kafka) so that all providers are shown. There is no class per combination: every scenario reuses the same few classes.

## Tests (37)

| Required test | Where |
|---|---|
| successful delivery | `urgentNotificationAddsPrefixAndDeliversViaTelegram` |
| unavailable provider | `notificationWithoutFallbackFailsWhenProviderIsUnavailable`, `unavailableSenderRefusesToSend` |
| fallback | `urgentFallsBackToSmsWhenTelegramIsUnavailable`, `urgentUsesNextFallbackWhenFirstFallbackIsAlsoDown`, `urgentFallsBackWhenPrimaryIsUpButRejectsTheMessage` |
| SMS length restriction | `smsRejectsMessageLongerThanLimit`, `smsAcceptsMessageExactlyAtLimit`, `smsLimitCanBeChangedTo120` |
| encrypted message | `encryptedNotificationSendsScrambledTextThatDecryptsBack` |
| batch notification | `batchReportsTotalSuccessfulAndFailed`, `batchSummaryTextMatchesRequiredFormat` |
| runtime sender switching | `sameNotificationObjectSwitchesProviderAtRuntime` |
| new `PushNotificationSender` | `pushSenderWorksWithExistingUrgentNotification` and 3 more in `ExtensionTest` |
| new `MarketingNotification` | `marketingNotificationWorksWithEveryProvider` and 3 more in `ExtensionTest` |
| failure scenarios | `urgentFailsWhenEveryProviderIsUnavailable`, `notificationRejectsNullSender`, `senderRejectsBlankRecipient`, ... |
| works only through the interface | `notificationWorksWithAnySenderThatImplementsTheInterface` |

## Independent extension

`PushNotificationSender` and `MarketingNotification` were added in a separate commit. To see that no existing notification or sender was edited:

```bash
git log --stat --grep="PushNotificationSender"
```

The commit only **adds** two new classes and one test file (plus a few demo lines in `Main`). Nothing is deleted or changed in the old Notification / MessageSender classes.

## Answers

**What are the two independent dimensions?**
(1) The kind of notification: urgent, encrypted, scheduled batch, marketing. (2) The delivery provider: Telegram, Email, SMS, Kafka, Push. A new kind of notification does not depend on how messages are delivered, and a provider does not care what kind of notification uses it.

**Where exactly is the Bridge?**
In the field `protected MessageSender sender` of `Notification` (and its setter `setSender`). It is a has-a link from the Abstraction hierarchy to the Implementor hierarchy through the interface `MessageSender`. Notifications only call `sendMessage`, `isAvailable` and `getChannelName`; they never name a concrete provider and never use `instanceof`.

**Why is inheritance problematic here?**
With inheritance every combination needs its own class: `UrgentTelegramNotification`, `UrgentEmailNotification`, `EncryptedSmsNotification`, ... That is N × M classes (4 types × 5 providers = 20), and each new provider or type multiplies the work. The provider could not be changed at runtime either, because it is baked into the class. With Bridge there are only N + M classes (4 + 5 = 9) and the provider is a replaceable field.

**What happens when a new provider is added?**
One new class that implements `MessageSender` (here `PushNotificationSender`). No Notification class changes, and all existing notification types (urgent, encrypted, batch, marketing) can use it immediately.

**What happens when a new Notification type is added?**
One new class that extends `Notification` (here `MarketingNotification`). No sender changes, and it works with Telegram, Email, SMS, Kafka and Push at once.

**Why is this Bridge rather than Adapter?**
Adapter is a repair: it makes an already existing, incompatible class fit an interface that the client expects, after the two were designed separately. Bridge is a design decision made up front: we deliberately split one problem into two hierarchies that vary independently and connect them with a has-a link. Here we own both sides, there is nothing incompatible to repair, and the goal is to avoid the N × M explosion and to allow switching the provider at runtime — that is Bridge.
