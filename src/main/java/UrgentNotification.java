import java.util.ArrayList;
import java.util.List;

/**
 * Refined Abstraction: marks the message as [URGENT], checks that the provider
 * is available and, if not, walks through the fallback providers in order.
 * Everything goes through the MessageSender interface - no instanceof anywhere.
 */
public class UrgentNotification extends Notification {
    private static final String PREFIX = "[URGENT] ";
    private static final String TITLE = "Urgent notification";

    private final List<MessageSender> fallbackSenders = new ArrayList<>();

    public UrgentNotification(MessageSender primarySender, MessageSender... fallbacks) {
        super(primarySender);
        for (MessageSender fallback : fallbacks) {
            addFallback(fallback);
        }
    }

    public void addFallback(MessageSender fallback) {
        if (fallback == null) {
            throw new IllegalArgumentException("Fallback sender must not be null.");
        }
        fallbackSenders.add(fallback);
    }

    @Override
    public void send(String recipient, String message) {
        String body = PREFIX + requireMessage(message);

        if (trySend(sender, recipient, body)) {
            return;
        }
        for (MessageSender fallback : fallbackSenders) {
            if (trySend(fallback, recipient, body)) {
                return;
            }
        }
        recordResult(false, "none");
    }

    private boolean trySend(MessageSender candidate, String recipient, String body) {
        if (!candidate.isAvailable()) {
            return false;
        }
        boolean delivered = candidate.sendMessage(recipient, TITLE, body);
        if (delivered) {
            recordResult(true, candidate.getChannelName());
        }
        return delivered;
    }
}
