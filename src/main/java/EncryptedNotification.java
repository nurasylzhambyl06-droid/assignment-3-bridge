import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Refined Abstraction: scrambles the message before it reaches ANY provider.
 * Simple XOR with a key, then Base64 so the result is printable text.
 * (A toy cipher - real cryptography is not required by the assignment.)
 */
public class EncryptedNotification extends Notification {
    private static final String TITLE = "Encrypted message";
    private static final String DEFAULT_KEY = "bridge";

    private final byte[] key;

    public EncryptedNotification(MessageSender sender) {
        this(sender, DEFAULT_KEY);
    }

    public EncryptedNotification(MessageSender sender, String key) {
        super(sender);
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Encryption key must not be empty.");
        }
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void send(String recipient, String message) {
        deliver(recipient, TITLE, encrypt(requireMessage(message)));
    }

    public String encrypt(String plainText) {
        byte[] scrambled = xor(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(scrambled);
    }

    public String decrypt(String cipherText) {
        byte[] scrambled = Base64.getDecoder().decode(cipherText);
        return new String(xor(scrambled), StandardCharsets.UTF_8);
    }

    private byte[] xor(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return result;
    }
}
