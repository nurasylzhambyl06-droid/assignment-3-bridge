/**
 * Implementor (the "how do we deliver it" side of the Bridge).
 * Notifications know ONLY this interface, never a concrete provider.
 */
public interface MessageSender {

    /** Tries to deliver a message. Returns true if the provider accepted it. */
    boolean sendMessage(String recipient, String title, String body);

    /** Whether the provider can accept messages right now. */
    boolean isAvailable();

    String getChannelName();
}
