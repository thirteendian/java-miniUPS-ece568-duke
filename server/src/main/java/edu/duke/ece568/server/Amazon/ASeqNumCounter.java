package edu.duke.ece568.server.Amazon;

public class ASeqNumCounter {
    private static ASeqNumCounter counter_obj = null;
    public static long next_counter;
    private static long current_id;

    public ASeqNumCounter() {
        next_counter = 1;
    }

    public static ASeqNumCounter getInstance() {
        if (counter_obj == null) {
            synchronized (ASeqNumCounter.class) {
                if (counter_obj == null) {
                    counter_obj = new ASeqNumCounter();
                }
            }
        }
        current_id = next_counter;
        next_counter++;
        return counter_obj;
    }

    public long getCurrSeqNum() {
        return current_id;
    }
}
