package gov.nist.core;

import java.io.PrintStream;

public class InternalErrorHandler {
    public static void handleException(Exception ex) throws RuntimeException {
        PrintStream printStream = System.err;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected internal error FIXME!! ");
        stringBuilder.append(ex.getMessage());
        printStream.println(stringBuilder.toString());
        ex.printStackTrace();
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected internal error FIXME!! ");
        stringBuilder.append(ex.getMessage());
        throw new RuntimeException(stringBuilder.toString(), ex);
    }

    public static void handleException(Exception ex, StackLogger stackLogger) {
        PrintStream printStream = System.err;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected internal error FIXME!! ");
        stringBuilder.append(ex.getMessage());
        printStream.println(stringBuilder.toString());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("UNEXPECTED INTERNAL ERROR FIXME ");
        stringBuilder2.append(ex.getMessage());
        stackLogger.logError(stringBuilder2.toString());
        ex.printStackTrace();
        stackLogger.logException(ex);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unexpected internal error FIXME!! ");
        stringBuilder.append(ex.getMessage());
        throw new RuntimeException(stringBuilder.toString(), ex);
    }

    public static void handleException(String emsg) {
        new Exception().printStackTrace();
        System.err.println("Unexepcted INTERNAL ERROR FIXME!!");
        System.err.println(emsg);
        throw new RuntimeException(emsg);
    }

    public static void handleException(String emsg, StackLogger stackLogger) {
        stackLogger.logStackTrace();
        stackLogger.logError("Unexepcted INTERNAL ERROR FIXME!!");
        stackLogger.logFatalError(emsg);
        throw new RuntimeException(emsg);
    }
}
