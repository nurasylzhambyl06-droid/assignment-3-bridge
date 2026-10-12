/** Result of a batch send: how many recipients, how many succeeded, how many failed. */
public record BatchSummary(int total, int successful) {

    public BatchSummary {
        if (total < 0 || successful < 0 || successful > total) {
            throw new IllegalArgumentException("Invalid batch counters.");
        }
    }

    public int failed() {
        return total - successful;
    }

    @Override
    public String toString() {
        return "Total: " + total + "\n"
                + "Successful: " + successful + "\n"
                + "Failed: " + failed();
    }
}
