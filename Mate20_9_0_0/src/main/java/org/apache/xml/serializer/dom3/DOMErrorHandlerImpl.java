package org.apache.xml.serializer.dom3;

import java.io.PrintStream;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;

final class DOMErrorHandlerImpl implements DOMErrorHandler {
    DOMErrorHandlerImpl() {
    }

    public boolean handleError(DOMError error) {
        boolean fail = true;
        String severity = null;
        if (error.getSeverity() == (short) 1) {
            fail = false;
            severity = "[Warning]";
        } else if (error.getSeverity() == (short) 2) {
            severity = "[Error]";
        } else if (error.getSeverity() == (short) 3) {
            severity = "[Fatal Error]";
        }
        PrintStream printStream = System.err;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(severity);
        stringBuilder.append(": ");
        stringBuilder.append(error.getMessage());
        stringBuilder.append("\t");
        printStream.println(stringBuilder.toString());
        printStream = System.err;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Type : ");
        stringBuilder.append(error.getType());
        stringBuilder.append("\tRelated Data: ");
        stringBuilder.append(error.getRelatedData());
        stringBuilder.append("\tRelated Exception: ");
        stringBuilder.append(error.getRelatedException());
        printStream.println(stringBuilder.toString());
        return fail;
    }
}
