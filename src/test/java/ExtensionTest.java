import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The independent-extension tests: the new provider works with the OLD notifications,
 * and the new notification works with the OLD providers - nothing old was edited.
 */
class ExtensionTest {

    // ---------- new provider: PushNotificationSender ----------

    @Test
    void pushSenderWorksWithExistingUrgentNotification() {
        PushNotificationSender push = new PushNotificationSender();
        Notification notification = new UrgentNotification(push);

        notification.send("nurasyl", "Server is down");

        assertTrue(notification.wasLastSendSuccessful());
        assertEquals("Push", notification.getLastChannelUsed());
        assertTrue(push.getLastDelivered().contains("[URGENT] Server is down"));
    }

    @Test
    void pushSenderWorksWithExistingEncryptedNotification() {
        PushNotificationSender push = new PushNotificationSender();
        EncryptedNotification notification = new EncryptedNotification(push);

        notification.send("nurasyl", "Secret");

        assertTrue(notification.wasLastSendSuccessful());
        assertFalse(push.getLastDelivered().contains("Secret"));
    }

    @Test
    void pushSenderCanServeAsFallback() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        PushNotificationSender push = new PushNotificationSender();
        telegram.setAvailable(false);
        Notification notification = new UrgentNotification(telegram, push);

        notification.send("nurasyl", "Server is down");

        assertEquals("Push", notification.getLastChannelUsed());
    }

    @Test
    void pushSenderRejectsBodyLongerThanLimit() {
        PushNotificationSender push = new PushNotificationSender();

        assertFalse(push.sendMessage("nurasyl", "t", "x".repeat(PushNotificationSender.MAX_BODY_LENGTH + 1)));
        assertTrue(push.sendMessage("nurasyl", "t", "x".repeat(PushNotificationSender.MAX_BODY_LENGTH)));
    }

    // ---------- new notification type: MarketingNotification ----------

    @Test
    void marketingNotificationAddsAdMarkAndUnsubscribeFooter() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        Notification notification = new MarketingNotification(telegram);

        notification.send("@nurasyl", "Sale -20%");

        assertTrue(telegram.getLastDelivered().contains("[AD] Sale -20%"));
        assertTrue(telegram.getLastDelivered().contains("Reply STOP to unsubscribe"));
    }

    @Test
    void marketingNotificationWorksWithEveryProvider() {
        SimulatedSender[] allSenders = {
                new TelegramMessageSender(),
                new EmailMessageSender(),
                new SmsMessageSender(),
                new KafkaEventSender(),
                new PushNotificationSender()
        };

        for (SimulatedSender sender : allSenders) {
            Notification notification = new MarketingNotification(sender);

            notification.send("user", "Sale");

            assertTrue(notification.wasLastSendSuccessful());
            assertEquals(sender.getChannelName(), notification.getLastChannelUsed());
        }
    }

    @Test
    void marketingNotificationSkipsOptedOutRecipient() {
        SmsMessageSender sms = new SmsMessageSender();
        MarketingNotification notification = new MarketingNotification(sms);
        notification.optOut("+77000000002");

        notification.send("+77000000002", "Sale");

        assertFalse(notification.wasLastSendSuccessful());
        assertTrue(sms.getDeliveredMessages().isEmpty());
    }

    @Test
    void marketingNotificationFailsWhenProviderIsUnavailable() {
        PushNotificationSender push = new PushNotificationSender();
        push.setAvailable(false);
        Notification notification = new MarketingNotification(push);

        notification.send("nurasyl", "Sale");

        assertFalse(notification.wasLastSendSuccessful());
    }
}
