package junit.textui;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Enumeration;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.runner.BaseTestRunner;

public class ResultPrinter implements TestListener {
    int fColumn = 0;
    PrintStream fWriter;

    public ResultPrinter(PrintStream writer) {
        this.fWriter = writer;
    }

    synchronized void print(TestResult result, long runTime) {
        printHeader(runTime);
        printErrors(result);
        printFailures(result);
        printFooter(result);
    }

    void printWaitPrompt() {
        getWriter().println();
        getWriter().println("<RETURN> to continue");
    }

    protected void printHeader(long runTime) {
        getWriter().println();
        PrintStream writer = getWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Time: ");
        stringBuilder.append(elapsedTimeAsString(runTime));
        writer.println(stringBuilder.toString());
    }

    protected void printErrors(TestResult result) {
        printDefects(result.errors(), result.errorCount(), "error");
    }

    protected void printFailures(TestResult result) {
        printDefects(result.failures(), result.failureCount(), "failure");
    }

    protected void printDefects(Enumeration<TestFailure> booBoos, int count, String type) {
        if (count != 0) {
            int i = 1;
            PrintStream writer;
            StringBuilder stringBuilder;
            if (count == 1) {
                writer = getWriter();
                stringBuilder = new StringBuilder();
                stringBuilder.append("There was ");
                stringBuilder.append(count);
                stringBuilder.append(" ");
                stringBuilder.append(type);
                stringBuilder.append(":");
                writer.println(stringBuilder.toString());
            } else {
                writer = getWriter();
                stringBuilder = new StringBuilder();
                stringBuilder.append("There were ");
                stringBuilder.append(count);
                stringBuilder.append(" ");
                stringBuilder.append(type);
                stringBuilder.append("s:");
                writer.println(stringBuilder.toString());
            }
            while (booBoos.hasMoreElements()) {
                printDefect((TestFailure) booBoos.nextElement(), i);
                i++;
            }
        }
    }

    public void printDefect(TestFailure booBoo, int count) {
        printDefectHeader(booBoo, count);
        printDefectTrace(booBoo);
    }

    protected void printDefectHeader(TestFailure booBoo, int count) {
        PrintStream writer = getWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(count);
        stringBuilder.append(") ");
        stringBuilder.append(booBoo.failedTest());
        writer.print(stringBuilder.toString());
    }

    protected void printDefectTrace(TestFailure booBoo) {
        getWriter().print(BaseTestRunner.getFilteredTrace(booBoo.trace()));
    }

    protected void printFooter(TestResult result) {
        PrintStream writer;
        StringBuilder stringBuilder;
        if (result.wasSuccessful()) {
            getWriter().println();
            getWriter().print("OK");
            writer = getWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append(" (");
            stringBuilder.append(result.runCount());
            stringBuilder.append(" test");
            stringBuilder.append(result.runCount() == 1 ? "" : "s");
            stringBuilder.append(")");
            writer.println(stringBuilder.toString());
        } else {
            getWriter().println();
            getWriter().println("FAILURES!!!");
            writer = getWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Tests run: ");
            stringBuilder.append(result.runCount());
            stringBuilder.append(",  Failures: ");
            stringBuilder.append(result.failureCount());
            stringBuilder.append(",  Errors: ");
            stringBuilder.append(result.errorCount());
            writer.println(stringBuilder.toString());
        }
        getWriter().println();
    }

    protected String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format(((double) runTime) / 1000.0d);
    }

    public PrintStream getWriter() {
        return this.fWriter;
    }

    public void addError(Test test, Throwable e) {
        getWriter().print("E");
    }

    public void addFailure(Test test, AssertionFailedError t) {
        getWriter().print("F");
    }

    public void endTest(Test test) {
    }

    public void startTest(Test test) {
        getWriter().print(".");
        int i = this.fColumn;
        this.fColumn = i + 1;
        if (i >= 40) {
            getWriter().println();
            this.fColumn = 0;
        }
    }
}
