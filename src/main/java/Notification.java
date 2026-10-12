/**
 * Abstraction (the "what kind of notification" side of the Bridge).
 * The field `sender` IS the bridge: it connects this hierarchy to the
 * MessageSender hierarchy through an interface, so each side can grow alone.
 */
public abstract class Notification {
    protected MessageSender sender;

    private boolean lastSendSuccessful;
    private String lastChannelUsed = "none";

    protected Notification(MessageSender sender) {
        this.sender = requireSender(sender);
    }

    /** Runtime switching: the notification keeps working, only the provider changes. */
    public void setSender(MessageSender sender) {
        this.sender = requireSender(sender);
    }

    public MessageSender getSender() {
        return sender;
    }

    public abstract void send(String recipient, String message);

    public boolean wasLastSendSuccessful() {
        return lastSendSuccessful;
    }

    /** Name of the channel that actually delivered the last message, or "none". */
    public String getLastChannelUsed() {
        return lastChannelUsed;
    }

    /** Standard delivery through the current sender; remembers the result. */
    protected boolean deliver(String recipient, String title, String body) {
        boolean delivered = sender.isAvailable() && sender.sendMessage(recipient, title, body);
        recordResult(delivered, delivered ? sender.getChannelName() : "none");
        return delivered;
    }

    protected void recordResult(boolean successful, String channel) {
        this.lastSendSuccessful = successful;
        this.lastChannelUsed = channel;
    }

    protected static String requireMessage(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Message must not be null.");
        }
        return message;
    }

    private static MessageSender requireSender(MessageSender sender) {
        if (sender == null) {
            throw new IllegalArgumentException("Sender must not be null.");
        }
        return sender;
    }
}
