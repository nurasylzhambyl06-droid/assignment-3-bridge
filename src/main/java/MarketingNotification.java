import java.util.HashSet;
import java.util.Set;

/**
 * NEW notification type added after the initial implementation.
 * Not a single existing MessageSender was modified to make this work.
 * Adds an [AD] mark and an unsubscribe footer, and never writes to people who opted out.
 */
public class MarketingNotification extends Notification {
    private static final String TITLE = "Special offer";
    private static final String PREFIX = "[AD] ";
    private static final String FOOTER = "\nReply STOP to unsubscribe";

    private final Set<String> optedOutRecipients = new HashSet<>();

    public MarketingNotification(MessageSender sender) {
        super(sender);
    }

    public void optOut(String recipient) {
        optedOutRecipients.add(recipient);
    }

    public boolean isOptedOut(String recipient) {
        return optedOutRecipients.contains(recipient);
    }

    @Override
    public void send(String recipient, String message) {
        String body = PREFIX + requireMessage(message) + FOOTER;

        if (isOptedOut(recipient)) {
            recordResult(false, "none");
            return;
        }
        deliver(recipient, TITLE, body);
    }
}
