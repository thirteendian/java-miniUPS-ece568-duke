package edu.duke.ece568.server;

public class SequenceNumCounter {
    private static SequenceNumCounter counter_obj = null;
    public static long next_counter;
    private static long current_id;

    public SequenceNumCounter() {
        next_counter = 1;
    }

    public static SequenceNumCounter getInstance() {
        if (counter_obj == null) {
            synchronized (SequenceNumCounter.class) {
                if (counter_obj == null) {
                    counter_obj = new SequenceNumCounter();
                }
            }
        }
        current_id = next_counter;
        next_counter++;
        return counter_obj;
    }

    public long getCurrent_id() {
        return current_id;
    }
}
