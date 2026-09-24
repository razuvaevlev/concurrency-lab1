package org.labs;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.Objects;

public final class Programmer implements Callable<Programmer.Result> {

    private final Spoon firstSpoon;
    private final Spoon secondSpoon;
    private final int id;
    private long numberOfMeals;
    private final BlockingQueue<FoodOrder> orders;
    private final ProgrammerActions actions;
    private final MealTurn mealTurn;
    public record Result(int id, long numberOfMeals) {}

    public static final ProgrammerActions DEFAULT_ACTIONS = new ProgrammerActions(Thread::yield, Thread::yield);

    public Programmer(int id, Spoon leftSpoon, Spoon rightSpoon,
                      BlockingQueue<FoodOrder> orders, ProgrammerActions actions, MealTurn mealTurn){
        Objects.requireNonNull(leftSpoon, "leftSpoon");
        Objects.requireNonNull(rightSpoon, "rightSpoon");
        if (leftSpoon == rightSpoon) {
            throw new IllegalArgumentException("A programmer must have two different spoons");
        }
        this.firstSpoon = leftSpoon.getId() < rightSpoon.getId() ? leftSpoon : rightSpoon;
        this.secondSpoon = leftSpoon.getId() < rightSpoon.getId() ? rightSpoon : leftSpoon;
        this.id = id;
        this.orders = Objects.requireNonNull(orders, "orders");
        this.actions = Objects.requireNonNull(actions, "actions");
        this.mealTurn = Objects.requireNonNull(mealTurn, "mealTurn");
    }

    public int getId(){
        return this.id;
    }

    private void discuss() {
        actions.discussingAction().run();
    }

    private void eat() {
        actions.eatingAction().run();
        numberOfMeals++;
    }

    private boolean askWaiter() throws InterruptedException {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        orders.put(FoodOrder.request(result, numberOfMeals));
        try {
            return result.get();
        } catch (ExecutionException e) {
            throw new IllegalStateException("Waiter failed", e.getCause());
        }
    }

    @Override
    public Result call() {
        try {
            while (true) {
                mealTurn.awaitTurn(id);
                boolean served;
                try {
                    served = askWaiter();
                    if (!served) {
                        return new Result(id, numberOfMeals);
                    }
                } finally {
                    mealTurn.finishTurn();
                }

                discuss();
                boolean firstPickedUp = false;
                boolean secondPickedUp = false;
                try {
                    firstSpoon.pickUp();
                    firstPickedUp = true;
                    secondSpoon.pickUp();
                    secondPickedUp = true;
                    eat();
                } finally {
                    if (secondPickedUp) secondSpoon.putDown();
                    if (firstPickedUp) firstSpoon.putDown();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new Result(id, numberOfMeals);
    }
}
