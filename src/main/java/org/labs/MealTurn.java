package org.labs;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

final class MealTurn {
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition turnChanged = lock.newCondition();
    private final int numberOfProgrammers;
    private int nextProgrammer;

    MealTurn(int numberOfProgrammers) {
        this.numberOfProgrammers = numberOfProgrammers;
    }

    void awaitTurn(int programmerId) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (nextProgrammer != programmerId) {
                turnChanged.await();
            }
        } finally {
            lock.unlock();
        }
    }

    void finishTurn() {
        lock.lock();
        try {
            nextProgrammer = (nextProgrammer + 1) % numberOfProgrammers;
            turnChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }
}