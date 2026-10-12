import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared skeleton for the simulated providers (no real network calls).
 * The flow of sendMessage() is the same for every provider; only the
 * delivery rules (canDeliver) and the output format (format) differ.
 */
public abstract class SimulatedSender implements MessageSender {
    private final String channelName;
    private final List<String> deliveredMessages = new ArrayList<>();
    private final Set<String> rejectedRecipients = new HashSet<>();
    private boolean available = true;

    protected SimulatedSender(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public final boolean sendMessage(String recipient, String title, String body) {
        if (!available) {
            return false;
        }
        if (!canDeliver(recipient, title, body)) {
            return false;
        }
        deliveredMessages.add(format(recipient, title, body));
        return true;
    }

    /** Common rules for every provider. Subclasses add their own on top. */
    protected boolean canDeliver(String recipient, String title, String body) {
        return recipient != null && !recipient.isBlank()
                && title != null
                && body != null
                && !rejectedRecipients.contains(recipient);
    }

    /** How this provider presents the message to the recipient. */
    protected abstract String format(String recipient, String title, String body);

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String getChannelName() {
        return channelName;
    }

    /** Simulates a provider refusing one particular recipient (e.g. a blocked number). */
    public void rejectRecipient(String recipient) {
        rejectedRecipients.add(recipient);
    }

    public List<String> getDeliveredMessages() {
        return Collections.unmodifiableList(deliveredMessages);
    }

    public String getLastDelivered() {
        return deliveredMessages.isEmpty() ? null : deliveredMessages.get(deliveredMessages.size() - 1);
    }
}
