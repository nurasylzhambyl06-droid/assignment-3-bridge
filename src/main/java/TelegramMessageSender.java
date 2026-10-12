/** Telegram: supports formatted messages (the title is shown in bold). */
public class TelegramMessageSender extends SimulatedSender {

    public TelegramMessageSender() {
        super("Telegram");
    }

    @Override
    protected String format(String recipient, String title, String body) {
        return "Telegram -> " + recipient + "\n*" + title + "*\n" + body;
    }
}
