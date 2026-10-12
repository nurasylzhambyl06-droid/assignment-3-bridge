import java.util.List;

/** Demo: every scenario uses the SAME few notification classes with different providers. */
public class Main {

    public static void main(String[] args) {
        urgentWithTelegram();
        encryptedWithEmail();
        batchWithSms();
        runtimeSwitchingTelegramToEmail();
        telegramUnavailableFallbackToSms();
        marketingWithPush();
        bonusEncryptedWithKafka();
    }

    private static void urgentWithTelegram() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        Notification notification = new UrgentNotification(telegram);

        notification.send("@nurasyl", "Server is down");

        show("1. UrgentNotification + Telegram", notification, telegram);
    }

    private static void encryptedWithEmail() {
        EmailMessageSender email = new EmailMessageSender();
        EncryptedNotification notification = new EncryptedNotification(email, "secret");

        notification.send("nurasyl@example.com", "Password is 12345");

        show("2. EncryptedNotification + Email", notification, email);
        System.out.println("Decrypted back: " + notification.decrypt(notification.encrypt("Password is 12345")));
    }

    private static void batchWithSms() {
        SmsMessageSender sms = new SmsMessageSender();
        sms.rejectRecipient("+77000000004"); // the provider refuses this number
        ScheduledBatchNotification notification = new ScheduledBatchNotification(sms,
                List.of("+77000000001", "+77000000002", "+77000000003", "+77000000004", "+77000000005"));

        BatchSummary summary = notification.sendToAll("Meeting moved to 15:00");

        System.out.println("=== 3. ScheduledBatchNotification + SMS ===");
        System.out.println(summary);
        System.out.println();
    }

    private static void runtimeSwitchingTelegramToEmail() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        EmailMessageSender email = new EmailMessageSender();
        Notification notification = new UrgentNotification(telegram);

        notification.send("nurasyl", "Deploy finished");
        show("4a. Runtime switching: Telegram", notification, telegram);

        notification.setSender(email); // same object, different provider
        notification.send("nurasyl", "Deploy finished");
        show("4b. Runtime switching: Email", notification, email);
    }

    private static void telegramUnavailableFallbackToSms() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        telegram.setAvailable(false);
        Notification notification = new UrgentNotification(telegram, sms);

        notification.send("+77000000001", "Database is unreachable");

        show("5. Telegram unavailable -> fallback to SMS", notification, telegram, sms);
    }

    private static void marketingWithPush() {
        PushNotificationSender push = new PushNotificationSender();
        Notification notification = new MarketingNotification(push);

        notification.send("nurasyl", "Autumn sale: -20% on everything");

        show("6. MarketingNotification + PushNotificationSender", notification, push);
    }

    private static void bonusEncryptedWithKafka() {
        KafkaEventSender kafka = new KafkaEventSender();
        Notification notification = new EncryptedNotification(kafka);

        notification.send("user-42", "Audit event");

        show("Bonus. EncryptedNotification + Kafka", notification, kafka);
    }

    private static void show(String heading, Notification notification, SimulatedSender... senders) {
        System.out.println("=== " + heading + " ===");
        System.out.println("Delivered: " + notification.wasLastSendSuccessful()
                + " (channel: " + notification.getLastChannelUsed() + ")");
        for (SimulatedSender sender : senders) {
            if (sender.getChannelName().equals(notification.getLastChannelUsed())) {
                System.out.println(sender.getLastDelivered());
            }
        }
        System.out.println();
    }
}
