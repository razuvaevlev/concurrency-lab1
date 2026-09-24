package org.labs;

import java.util.concurrent.locks.ReentrantLock;

public final class Spoon {
    private final int id;
    private final ReentrantLock lock = new ReentrantLock(true);

    public Spoon(int id){
        if (id < 0) {
            throw new IllegalArgumentException("Spoon id must be non-negative");
        }
        this.id = id;
    }

    public int getId(){
        return id;
    }

    public void pickUp() throws InterruptedException {
        lock.lockInterruptibly();
    }

    public void putDown() {
        lock.unlock();
    }
}
