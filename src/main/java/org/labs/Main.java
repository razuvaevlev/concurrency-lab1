package org.labs;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            Dinner.getDinner().start();
            return;
        }
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: Main <programmers> <food> <waiters>");
        }

        int programmers = Integer.parseInt(args[0]);
        int food = Integer.parseInt(args[1]);
        int waiters = Integer.parseInt(args[2]);
        var dinner = Dinner.getDinner(programmers, food, waiters, Programmer.DEFAULT_ACTIONS);
        dinner.start();
    }
}