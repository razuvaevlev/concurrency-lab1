package org.labs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.labs.Programmer.DEFAULT_ACTIONS;

public final class Dinner {
    private final Programmer[] programmers;
    private final Waiter[] waiters;
    private final BlockingQueue<FoodOrder> orders;

    private Dinner(int numberOfProgrammers, int numberOfFood, int numberOfWaiters,
                   ProgrammerActions actions) {
        if (numberOfProgrammers < 2) {
            throw new IllegalArgumentException("At least two programmers are required");
        }
        if (numberOfFood < 0) {
            throw new IllegalArgumentException("The amount of food cannot be negative");
        }
        if (numberOfWaiters <= 0) {
            throw new IllegalArgumentException("At least one waiter is required");
        }
        Objects.requireNonNull(actions, "actions");
        // NOTE: ArrayBlockingQueue(numberOfProgrammers, true), LinkedBlockingQueue
        orders = new PriorityBlockingQueue<>();
        AtomicInteger foodSupply = new AtomicInteger(numberOfFood);
        MealTurn mealTurn = new MealTurn(numberOfProgrammers);
        programmers = new Programmer[numberOfProgrammers];
        Spoon[] spoons = new Spoon[numberOfProgrammers];
        waiters = new Waiter[numberOfWaiters];

        for (int i = 0; i < numberOfProgrammers; i++) {
            spoons[i] = new Spoon(i);
        }
        for (int i = 0; i < numberOfProgrammers; i++) {
            programmers[i] = new Programmer(i, spoons[i], spoons[(i + 1) % numberOfProgrammers],
                    orders, actions, mealTurn);
        }
        for (int i = 0; i < numberOfWaiters; i++) {
            waiters[i] = new Waiter(orders, foodSupply);
        }
    }

    public static Dinner getDinner() {
        return new Dinner(7, 1_000_000, 2, DEFAULT_ACTIONS);
    }

    public static Dinner getDinner(int numberOfProgrammers, int numberOfFood, int numberOfWaiters,
                                   ProgrammerActions actions) {
        return new Dinner(numberOfProgrammers, numberOfFood, numberOfWaiters, actions);
    }

    public Result run() {
        long startTime = System.nanoTime();
        ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        List<Future<Programmer.Result>> programmerFutures = new ArrayList<>(programmers.length);
        List<Future<?>> waiterFutures = new ArrayList<>(waiters.length);
        boolean completed = false;

        try {
            for (Programmer programmer : programmers) {
                programmerFutures.add(executor.submit(programmer));
            }
            for (Waiter waiter : waiters) {
                waiterFutures.add(executor.submit(waiter));
            }

            List<Programmer.Result> results = new ArrayList<>(programmers.length);
            for (Future<Programmer.Result> future : programmerFutures) {
                results.add(future.get());
            }

            for (int i = 0; i < waiters.length; i++) {
                orders.add(FoodOrder.stop());
            }
            for (Future<?> future : waiterFutures) {
                future.get();
            }

            completed = true;
            return new Result(results, elapsedMillis(startTime));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dinner was interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("A dinner worker failed", e.getCause());
        } finally {
            if (!completed) {
                programmerFutures.forEach(future -> future.cancel(true));
                waiterFutures.forEach(future -> future.cancel(true));
                executor.shutdownNow();
            } else {
                executor.shutdown();
            }
        }
    }

    public void start() {
        Result result = run();
        result.programmerResults().forEach(programmer ->
                System.out.println("Programmer result " + programmer.id() + " "
                        + programmer.numberOfMeals()));
        System.out.println("Fairness error " + result.fairnessErrorPercent() + "%");
        System.out.println("Dinner took " + result.elapsedMillis() + " ms");
    }

    private static long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    public record Result(List<Programmer.Result> programmerResults, long elapsedMillis) {
        public Result {
            programmerResults = List.copyOf(programmerResults);
        }

        public long totalMeals() {
            return programmerResults.stream().mapToLong(Programmer.Result::numberOfMeals).sum();
        }

        public long minimumMeals() {
            return programmerResults.stream().mapToLong(Programmer.Result::numberOfMeals).min().orElse(0);
        }

        public long maximumMeals() {
            return programmerResults.stream().mapToLong(Programmer.Result::numberOfMeals).max().orElse(0);
        }

        public double fairnessErrorPercent() {
            double average = programmerResults.stream()
                    .mapToLong(Programmer.Result::numberOfMeals)
                    .average()
                    .orElse(0);
            return average == 0 ? 0 : (maximumMeals() - minimumMeals()) * 100 / average;
        }
    }
}
