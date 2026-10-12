/** Kafka: does not "message a person", it publishes an event to a topic with a growing offset. */
public class KafkaEventSender extends SimulatedSender {
    private final String topic;

    public KafkaEventSender() {
        this("notifications");
    }

    public KafkaEventSender(String topic) {
        super("Kafka");
        this.topic = topic;
    }

    @Override
    protected String format(String recipient, String title, String body) {
        int offset = getDeliveredMessages().size();
        return "Kafka event: topic=" + topic + " partition=0 offset=" + offset
                + " key=" + recipient + " payload={title=" + title + ", body=" + body + "}";
    }
}
