package edu.duke.ece568.server.Amazon;

public class AShippingNumCounter {
    private static AShippingNumCounter counter_obj = null;
    public static long next_counter;
    private static long current_id;

    public AShippingNumCounter() {
        next_counter = 1;
    }

    public static AShippingNumCounter getInstance() {
        if (counter_obj == null) {
            synchronized (AShippingNumCounter.class) {
                if (counter_obj == null) {
                    counter_obj = new AShippingNumCounter();
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
