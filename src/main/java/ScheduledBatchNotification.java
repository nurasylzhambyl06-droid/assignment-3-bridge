import java.util.ArrayList;
import java.util.List;

/**
 * Refined Abstraction: holds many recipients and sends the same message to
 * all of them through the current provider, then reports a summary.
 * send(recipient, message) still works for ONE recipient, like any Notification.
 */
public class ScheduledBatchNotification extends Notification {
    private static final String TITLE = "Scheduled notification";

    private final List<String> recipients;

    public ScheduledBatchNotification(MessageSender sender, List<String> recipients) {
        super(sender);
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("Batch needs at least one recipient.");
        }
        this.recipients = new ArrayList<>(recipients);
    }

    @Override
    public void send(String recipient, String message) {
        deliver(recipient, TITLE, requireMessage(message));
    }

    public BatchSummary sendToAll(String message) {
        String text = requireMessage(message);
        int successful = 0;
        for (String recipient : recipients) {
            if (deliver(recipient, TITLE, text)) {
                successful++;
            }
        }
        return new BatchSummary(recipients.size(), successful);
    }

    public List<String> getRecipients() {
        return List.copyOf(recipients);
    }
}
