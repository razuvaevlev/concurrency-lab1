package org.labs;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Waiter implements Runnable {
    private final BlockingQueue<FoodOrder> orders;
    private final AtomicInteger numberOfFood;

    public Waiter(BlockingQueue<FoodOrder> orders, AtomicInteger numberOfFood) {
        this.orders = orders;
        this.numberOfFood = numberOfFood;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                FoodOrder order = orders.take();
                if (order.isStop()) {
                    return;
                }
                boolean served = numberOfFood.getAndUpdate(food -> food > 0 ? food - 1 : food) > 0;
                order.complete(served);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
