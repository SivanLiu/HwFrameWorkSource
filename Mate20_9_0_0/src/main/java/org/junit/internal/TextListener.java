package org.junit.internal;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.List;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TextListener extends RunListener {
    private final PrintStream writer;

    public TextListener(JUnitSystem system) {
        this(system.out());
    }

    public TextListener(PrintStream writer) {
        this.writer = writer;
    }

    public void testRunFinished(Result result) {
        printHeader(result.getRunTime());
        printFailures(result);
        printFooter(result);
    }

    public void testStarted(Description description) {
        this.writer.append('.');
    }

    public void testFailure(Failure failure) {
        this.writer.append('E');
    }

    public void testIgnored(Description description) {
        this.writer.append('I');
    }

    private PrintStream getWriter() {
        return this.writer;
    }

    protected void printHeader(long runTime) {
        getWriter().println();
        PrintStream writer = getWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Time: ");
        stringBuilder.append(elapsedTimeAsString(runTime));
        writer.println(stringBuilder.toString());
    }

    protected void printFailures(Result result) {
        List<Failure> failures = result.getFailures();
        if (failures.size() != 0) {
            PrintStream writer;
            StringBuilder stringBuilder;
            if (failures.size() == 1) {
                writer = getWriter();
                stringBuilder = new StringBuilder();
                stringBuilder.append("There was ");
                stringBuilder.append(failures.size());
                stringBuilder.append(" failure:");
                writer.println(stringBuilder.toString());
            } else {
                writer = getWriter();
                stringBuilder = new StringBuilder();
                stringBuilder.append("There were ");
                stringBuilder.append(failures.size());
                stringBuilder.append(" failures:");
                writer.println(stringBuilder.toString());
            }
            int i = 1;
            for (Failure each : failures) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("");
                int i2 = i + 1;
                stringBuilder2.append(i);
                printFailure(each, stringBuilder2.toString());
                i = i2;
            }
        }
    }

    protected void printFailure(Failure each, String prefix) {
        PrintStream writer = getWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(") ");
        stringBuilder.append(each.getTestHeader());
        writer.println(stringBuilder.toString());
        getWriter().print(each.getTrace());
    }

    protected void printFooter(Result result) {
        PrintStream writer;
        StringBuilder stringBuilder;
        if (result.wasSuccessful()) {
            getWriter().println();
            getWriter().print("OK");
            writer = getWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append(" (");
            stringBuilder.append(result.getRunCount());
            stringBuilder.append(" test");
            stringBuilder.append(result.getRunCount() == 1 ? "" : "s");
            stringBuilder.append(")");
            writer.println(stringBuilder.toString());
        } else {
            getWriter().println();
            getWriter().println("FAILURES!!!");
            writer = getWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Tests run: ");
            stringBuilder.append(result.getRunCount());
            stringBuilder.append(",  Failures: ");
            stringBuilder.append(result.getFailureCount());
            writer.println(stringBuilder.toString());
        }
        getWriter().println();
    }

    protected String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format(((double) runTime) / 1000.0d);
    }
}
