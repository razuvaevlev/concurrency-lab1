package org.labs;

import java.util.Objects;

public record ProgrammerActions(Runnable discussingAction, Runnable eatingAction) {
	public ProgrammerActions {
		Objects.requireNonNull(discussingAction, "discussingAction");
		Objects.requireNonNull(eatingAction, "eatingAction");
	}
}
