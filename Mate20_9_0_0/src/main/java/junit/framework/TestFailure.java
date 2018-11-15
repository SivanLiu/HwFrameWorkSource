package junit.framework;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TestFailure {
    protected Test fFailedTest;
    protected Throwable fThrownException;

    public TestFailure(Test failedTest, Throwable thrownException) {
        this.fFailedTest = failedTest;
        this.fThrownException = thrownException;
    }

    public Test failedTest() {
        return this.fFailedTest;
    }

    public Throwable thrownException() {
        return this.fThrownException;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.fFailedTest);
        stringBuilder.append(": ");
        stringBuilder.append(this.fThrownException.getMessage());
        return stringBuilder.toString();
    }

    public String trace() {
        StringWriter stringWriter = new StringWriter();
        thrownException().printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public String exceptionMessage() {
        return thrownException().getMessage();
    }

    public boolean isFailure() {
        return thrownException() instanceof AssertionFailedError;
    }
}
