/** SMS: no title, and the text must fit into the length limit (160 characters by default). */
public class SmsMessageSender extends SimulatedSender {
    public static final int DEFAULT_MAX_LENGTH = 160;

    private final int maxLength;

    public SmsMessageSender() {
        this(DEFAULT_MAX_LENGTH);
    }

    public SmsMessageSender(int maxLength) {
        super("SMS");
        if (maxLength <= 0) {
            throw new IllegalArgumentException("SMS length limit must be greater than 0.");
        }
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    protected boolean canDeliver(String recipient, String title, String body) {
        return super.canDeliver(recipient, title, body) && body.length() <= maxLength;
    }

    @Override
    protected String format(String recipient, String title, String body) {
        return "SMS -> " + recipient + ": " + body;
    }
}
