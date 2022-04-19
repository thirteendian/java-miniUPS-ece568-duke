package edu.duke.ece568.server.World;

public class WSeqNumCounter {
    private static WSeqNumCounter counter_obj = null;
    public static long next_counter;
    private static long current_id;

    public WSeqNumCounter() {
        next_counter = 1;
    }

    public static WSeqNumCounter getInstance() {
        if (counter_obj == null) {
            synchronized (WSeqNumCounter.class) {
                if (counter_obj == null) {
                    counter_obj = new WSeqNumCounter();
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
