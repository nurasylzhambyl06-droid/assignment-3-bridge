import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Refined Abstractions working through the MessageSender interface. */
class NotificationTest {

    // ---------- successful delivery ----------

    @Test
    void urgentNotificationAddsPrefixAndDeliversViaTelegram() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        Notification notification = new UrgentNotification(telegram);

        notification.send("@nurasyl", "Server is down");

        assertTrue(notification.wasLastSendSuccessful());
        assertEquals("Telegram", notification.getLastChannelUsed());
        assertTrue(telegram.getLastDelivered().contains("[URGENT] Server is down"));
    }

    // ---------- unavailable provider + fallback ----------

    @Test
    void notificationWithoutFallbackFailsWhenProviderIsUnavailable() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        telegram.setAvailable(false);
        Notification notification = new UrgentNotification(telegram);

        notification.send("@nurasyl", "Server is down");

        assertFalse(notification.wasLastSendSuccessful());
        assertEquals("none", notification.getLastChannelUsed());
    }

    @Test
    void urgentFallsBackToSmsWhenTelegramIsUnavailable() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        telegram.setAvailable(false);
        Notification notification = new UrgentNotification(telegram, sms);

        notification.send("+77000000001", "Database is unreachable");

        assertTrue(notification.wasLastSendSuccessful());
        assertEquals("SMS", notification.getLastChannelUsed());
        assertTrue(telegram.getDeliveredMessages().isEmpty());
        assertTrue(sms.getLastDelivered().contains("[URGENT] Database is unreachable"));
    }

    @Test
    void urgentUsesNextFallbackWhenFirstFallbackIsAlsoDown() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        EmailMessageSender email = new EmailMessageSender();
        telegram.setAvailable(false);
        sms.setAvailable(false);
        Notification notification = new UrgentNotification(telegram, sms, email);

        notification.send("nurasyl", "Disk is full");

        assertEquals("Email", notification.getLastChannelUsed());
    }

    @Test
    void urgentFallsBackWhenPrimaryIsUpButRejectsTheMessage() {
        SmsMessageSender tinySms = new SmsMessageSender(10); // "[URGENT] ..." does not fit
        EmailMessageSender email = new EmailMessageSender();
        Notification notification = new UrgentNotification(tinySms, email);

        notification.send("nurasyl", "Server is down");

        assertTrue(notification.wasLastSendSuccessful());
        assertEquals("Email", notification.getLastChannelUsed());
    }

    @Test
    void urgentFailsWhenEveryProviderIsUnavailable() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        telegram.setAvailable(false);
        sms.setAvailable(false);
        Notification notification = new UrgentNotification(telegram, sms);

        notification.send("nurasyl", "Anyone there?");

        assertFalse(notification.wasLastSendSuccessful());
        assertEquals("none", notification.getLastChannelUsed());
    }

    @Test
    void fallbackCanBeAddedAfterCreation() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        SmsMessageSender sms = new SmsMessageSender();
        telegram.setAvailable(false);
        UrgentNotification notification = new UrgentNotification(telegram);

        notification.addFallback(sms);
        notification.send("+77000000001", "Server is down");

        assertEquals("SMS", notification.getLastChannelUsed());
    }

    @Test
    void notificationWorksWithAnySenderThatImplementsTheInterface() {
        // A sender invented inside the test: no instanceof, no known class - only the interface.
        boolean[] called = {false};
        MessageSender custom = new MessageSender() {
            @Override
            public boolean sendMessage(String recipient, String title, String body) {
                called[0] = true;
                return true;
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String getChannelName() {
                return "Custom";
            }
        };
        Notification notification = new UrgentNotification(custom);

        notification.send("anyone", "hello");

        assertTrue(called[0]);
        assertEquals("Custom", notification.getLastChannelUsed());
    }

    // ---------- encrypted message ----------

    @Test
    void encryptedNotificationSendsScrambledTextThatDecryptsBack() {
        EmailMessageSender email = new EmailMessageSender();
        EncryptedNotification notification = new EncryptedNotification(email, "secret");

        notification.send("a@b.com", "Meet at 10");

        String delivered = email.getLastDelivered();
        String cipher = notification.encrypt("Meet at 10");
        assertFalse(delivered.contains("Meet at 10"));
        assertTrue(delivered.contains(cipher));
        assertEquals("Meet at 10", notification.decrypt(cipher));
    }

    @Test
    void encryptionRoundTripKeepsUnicodeText() {
        EncryptedNotification notification = new EncryptedNotification(new TelegramMessageSender());

        String cipher = notification.encrypt("Привет, мир!");

        assertEquals("Привет, мир!", notification.decrypt(cipher));
    }

    @Test
    void encryptedNotificationRejectsEmptyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new EncryptedNotification(new TelegramMessageSender(), ""));
    }

    // ---------- SMS length restriction through a notification ----------

    @Test
    void longMessageFailsOnSmsButSucceedsOnTelegram() {
        String longText = "x".repeat(200);
        SmsMessageSender sms = new SmsMessageSender();
        TelegramMessageSender telegram = new TelegramMessageSender();
        Notification notification = new ScheduledBatchNotification(sms, List.of("+77000000001"));

        notification.send("+77000000001", longText);
        assertFalse(notification.wasLastSendSuccessful());

        notification.setSender(telegram);
        notification.send("@nurasyl", longText);
        assertTrue(notification.wasLastSendSuccessful());
    }

    // ---------- batch ----------

    @Test
    void batchReportsTotalSuccessfulAndFailed() {
        SmsMessageSender sms = new SmsMessageSender();
        sms.rejectRecipient("+3");
        ScheduledBatchNotification notification =
                new ScheduledBatchNotification(sms, List.of("+1", "+2", "+3", "+4"));

        BatchSummary summary = notification.sendToAll("Meeting at 15:00");

        assertEquals(4, summary.total());
        assertEquals(3, summary.successful());
        assertEquals(1, summary.failed());
        assertEquals(3, sms.getDeliveredMessages().size());
    }

    @Test
    void batchSummaryTextMatchesRequiredFormat() {
        BatchSummary summary = new BatchSummary(10, 8);

        assertEquals("Total: 10\nSuccessful: 8\nFailed: 2", summary.toString());
    }

    @Test
    void batchCountsEveryRecipientAsFailedWhenProviderIsDown() {
        SmsMessageSender sms = new SmsMessageSender();
        sms.setAvailable(false);
        ScheduledBatchNotification notification =
                new ScheduledBatchNotification(sms, List.of("+1", "+2", "+3"));

        BatchSummary summary = notification.sendToAll("Hello");

        assertEquals(3, summary.failed());
        assertEquals(0, summary.successful());
    }

    @Test
    void batchNeedsAtLeastOneRecipient() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScheduledBatchNotification(new SmsMessageSender(), List.of()));
    }

    // ---------- runtime switching ----------

    @Test
    void sameNotificationObjectSwitchesProviderAtRuntime() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        EmailMessageSender email = new EmailMessageSender();
        Notification notification = new UrgentNotification(telegram);

        notification.send("nurasyl", "Deploy finished");
        assertEquals("Telegram", notification.getLastChannelUsed());

        notification.setSender(email);
        notification.send("nurasyl", "Deploy finished");

        assertEquals("Email", notification.getLastChannelUsed());
        assertEquals(1, telegram.getDeliveredMessages().size());
        assertEquals(1, email.getDeliveredMessages().size());
    }

    // ---------- failure scenarios ----------

    @Test
    void notificationRejectsNullSender() {
        assertThrows(IllegalArgumentException.class, () -> new UrgentNotification(null));

        Notification notification = new UrgentNotification(new TelegramMessageSender());
        assertThrows(IllegalArgumentException.class, () -> notification.setSender(null));
    }

    @Test
    void notificationRejectsNullMessage() {
        Notification notification = new UrgentNotification(new TelegramMessageSender());

        assertThrows(IllegalArgumentException.class, () -> notification.send("@nurasyl", null));
    }
}
