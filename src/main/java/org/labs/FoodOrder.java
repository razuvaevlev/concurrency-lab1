package org.labs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

final class FoodOrder implements Comparable<FoodOrder> {
    private final CompletableFuture<Boolean> result;
    private final boolean stop;
    private final Long sequence;
    private final long ownerNumberOfPlates;

    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong();

    private FoodOrder(CompletableFuture<Boolean> result, long ownerNumberOfPlates, boolean stop) {
        this.result = result;
        this.stop = stop;
        this.sequence = SEQUENCE_GENERATOR.incrementAndGet();
        this.ownerNumberOfPlates = ownerNumberOfPlates;
    }

    static FoodOrder request(CompletableFuture<Boolean> result, long ownerNumberOfPlates) {
        return new FoodOrder(result,ownerNumberOfPlates, false);
    }

    static FoodOrder stop() {
        return new FoodOrder(null, 0, true);
    }

    boolean isStop() {
        return stop;
    }

    void complete(boolean served) {
        result.complete(served);
    }

    @Override
    public int compareTo(FoodOrder other) {
        int result = Long.compare(this.ownerNumberOfPlates, other.ownerNumberOfPlates);

        if (result != 0) {
            return result;
        }

        return Long.compare(this.sequence, other.sequence);
    }
}