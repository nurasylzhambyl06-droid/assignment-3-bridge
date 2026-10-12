/**
 * NEW provider added after the initial implementation.
 * Not a single existing Notification class was modified to make this work.
 * Push shows a short card on the phone, so the body is limited to 120 characters.
 */
public class PushNotificationSender extends SimulatedSender {
    public static final int MAX_BODY_LENGTH = 120;

    public PushNotificationSender() {
        super("Push");
    }

    @Override
    protected boolean canDeliver(String recipient, String title, String body) {
        return super.canDeliver(recipient, title, body) && body.length() <= MAX_BODY_LENGTH;
    }

    @Override
    protected String format(String recipient, String title, String body) {
        return "Push -> device of " + recipient + ": [" + title + "] " + body;
    }
}
