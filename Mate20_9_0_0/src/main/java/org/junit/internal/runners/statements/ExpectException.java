package org.junit.internal.runners.statements;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.model.Statement;

public class ExpectException extends Statement {
    private final Class<? extends Throwable> expected;
    private final Statement next;

    public ExpectException(Statement next, Class<? extends Throwable> expected) {
        this.next = next;
        this.expected = expected;
    }

    public void evaluate() throws Exception {
        boolean complete = false;
        try {
            this.next.evaluate();
            complete = true;
        } catch (AssumptionViolatedException e) {
            throw e;
        } catch (Throwable e2) {
            if (!this.expected.isAssignableFrom(e2.getClass())) {
                String message = new StringBuilder();
                message.append("Unexpected exception, expected<");
                message.append(this.expected.getName());
                message.append("> but was<");
                message.append(e2.getClass().getName());
                message.append(">");
                Exception exception = new Exception(message.toString(), e2);
            }
        }
        if (complete) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected exception: ");
            stringBuilder.append(this.expected.getName());
            throw new AssertionError(stringBuilder.toString());
        }
    }
}
