package org.labs;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DinnerTest {
    private static final ProgrammerActions NO_OP_ACTIONS = new ProgrammerActions(() -> { }, () -> { });

    @Test
    void servesExactlyTheAvailableFood() {
        Dinner.Result result = Dinner.getDinner(5, 100, 3, NO_OP_ACTIONS).run();

        assertEquals(100, result.totalMeals());
        assertTrue(result.maximumMeals() - result.minimumMeals() <= 1);
    }

    @Test
    void finishesWithoutFood() {
        Dinner.Result result = Dinner.getDinner(4, 0, 2, NO_OP_ACTIONS).run();

        assertEquals(0, result.totalMeals());
        assertEquals(0, result.fairnessErrorPercent());
    }

    @Test
    void completesWithoutDeadlock() {
        Dinner dinner = Dinner.getDinner(6, 200, 3, NO_OP_ACTIONS);

        Dinner.Result result = assertTimeoutPreemptively(Duration.ofSeconds(10), dinner::run);

        assertEquals(200, result.totalMeals());
        assertTrue(result.maximumMeals() >= 0);
    }

    @Test
    void completesQuickly() {
        long start = System.nanoTime();
        Dinner.Result result = Dinner.getDinner(4, 120, 2, NO_OP_ACTIONS).run();
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertEquals(120, result.totalMeals());
        assertTrue(elapsedMillis < 5_000, "Dinner took too long: " + elapsedMillis + " ms");
    }

    @Test
    void keepsFairnessWithinReasonableBounds() {
        Dinner.Result result = Dinner.getDinner(5, 150, 2, NO_OP_ACTIONS).run();

        assertEquals(150, result.totalMeals());
        assertTrue(result.maximumMeals() - result.minimumMeals() <= 2,
                "Meals per programmer must be almost equal");
        assertTrue(result.fairnessErrorPercent() <= 10.0,
                "Fairness error is too high: " + result.fairnessErrorPercent());
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> Dinner.getDinner(1, 10, 1, NO_OP_ACTIONS));
        assertThrows(IllegalArgumentException.class,
                () -> Dinner.getDinner(2, -1, 1, NO_OP_ACTIONS));
        assertThrows(IllegalArgumentException.class,
                () -> Dinner.getDinner(2, 10, 0, NO_OP_ACTIONS));
    }
}
