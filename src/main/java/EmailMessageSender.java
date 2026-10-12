/** Email: has a subject line (the title) and a signature at the bottom. */
public class EmailMessageSender extends SimulatedSender {
    private final String signature;

    public EmailMessageSender() {
        this("Notification Gateway Team");
    }

    public EmailMessageSender(String signature) {
        super("Email");
        this.signature = signature;
    }

    @Override
    protected String format(String recipient, String title, String body) {
        return "Email -> " + recipient + "\n"
                + "Subject: " + title + "\n\n"
                + body + "\n\n"
                + "-- \n" + signature;
    }
}
