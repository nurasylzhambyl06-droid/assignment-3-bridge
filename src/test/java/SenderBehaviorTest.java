import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Each provider (Implementor) behaves differently - tested on its own, without any Notification. */
class SenderBehaviorTest {

    @Test
    void telegramShowsTitleInBold() {
        TelegramMessageSender telegram = new TelegramMessageSender();

        assertTrue(telegram.sendMessage("@nurasyl", "Alert", "Server is down"));

        assertTrue(telegram.getLastDelivered().contains("*Alert*"));
        assertTrue(telegram.getLastDelivered().contains("Server is down"));
    }

    @Test
    void emailHasSubjectAndSignature() {
        EmailMessageSender email = new EmailMessageSender("Best regards, Support");

        assertTrue(email.sendMessage("a@b.com", "Invoice", "Your invoice is ready"));

        assertTrue(email.getLastDelivered().contains("Subject: Invoice"));
        assertTrue(email.getLastDelivered().contains("Best regards, Support"));
    }

    @Test
    void smsRejectsMessageLongerThanLimit() {
        SmsMessageSender sms = new SmsMessageSender();

        boolean delivered = sms.sendMessage("+77000000001", "ignored", "x".repeat(161));

        assertFalse(delivered);
        assertTrue(sms.getDeliveredMessages().isEmpty());
    }

    @Test
    void smsAcceptsMessageExactlyAtLimit() {
        SmsMessageSender sms = new SmsMessageSender();

        assertTrue(sms.sendMessage("+77000000001", "ignored", "x".repeat(160)));
    }

    @Test
    void smsLimitCanBeChangedTo120() {
        SmsMessageSender sms = new SmsMessageSender(120);

        assertFalse(sms.sendMessage("+77000000001", "t", "x".repeat(121)));
        assertTrue(sms.sendMessage("+77000000001", "t", "x".repeat(120)));
    }

    @Test
    void smsRejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> new SmsMessageSender(0));
    }

    @Test
    void kafkaPublishesEventsWithGrowingOffset() {
        KafkaEventSender kafka = new KafkaEventSender();

        kafka.sendMessage("user-1", "t", "first");
        kafka.sendMessage("user-1", "t", "second");

        assertTrue(kafka.getDeliveredMessages().get(0).contains("offset=0"));
        assertTrue(kafka.getDeliveredMessages().get(1).contains("offset=1"));
    }

    @Test
    void unavailableSenderRefusesToSend() {
        TelegramMessageSender telegram = new TelegramMessageSender();
        telegram.setAvailable(false);

        assertFalse(telegram.isAvailable());
        assertFalse(telegram.sendMessage("@nurasyl", "t", "b"));
        assertTrue(telegram.getDeliveredMessages().isEmpty());
    }

    @Test
    void senderRejectsBlankRecipient() {
        EmailMessageSender email = new EmailMessageSender();

        assertFalse(email.sendMessage("   ", "t", "b"));
        assertFalse(email.sendMessage(null, "t", "b"));
    }

    @Test
    void senderRejectsRecipientBlockedByProvider() {
        SmsMessageSender sms = new SmsMessageSender();
        sms.rejectRecipient("+77000000009");

        assertFalse(sms.sendMessage("+77000000009", "t", "hello"));
        assertTrue(sms.sendMessage("+77000000001", "t", "hello"));
    }
}
