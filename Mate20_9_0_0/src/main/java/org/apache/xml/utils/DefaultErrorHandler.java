package org.apache.xml.utils;

import java.io.PrintStream;
import java.io.PrintWriter;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import org.apache.xml.res.XMLErrorResources;
import org.apache.xml.res.XMLMessages;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DefaultErrorHandler implements ErrorHandler, ErrorListener {
    PrintWriter m_pw;
    boolean m_throwExceptionOnError;

    public DefaultErrorHandler(PrintWriter pw) {
        this.m_throwExceptionOnError = true;
        this.m_pw = pw;
    }

    public DefaultErrorHandler(PrintStream pw) {
        this.m_throwExceptionOnError = true;
        this.m_pw = new PrintWriter(pw, true);
    }

    public DefaultErrorHandler() {
        this(true);
    }

    public DefaultErrorHandler(boolean throwExceptionOnError) {
        this.m_throwExceptionOnError = true;
        this.m_throwExceptionOnError = throwExceptionOnError;
    }

    public PrintWriter getErrorWriter() {
        if (this.m_pw == null) {
            this.m_pw = new PrintWriter(System.err, true);
        }
        return this.m_pw;
    }

    public void warning(SAXParseException exception) throws SAXException {
        PrintWriter pw = getErrorWriter();
        printLocation(pw, (Throwable) exception);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Parser warning: ");
        stringBuilder.append(exception.getMessage());
        pw.println(stringBuilder.toString());
    }

    public void error(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void warning(TransformerException exception) throws TransformerException {
        PrintWriter pw = getErrorWriter();
        printLocation(pw, (Throwable) exception);
        pw.println(exception.getMessage());
    }

    public void error(TransformerException exception) throws TransformerException {
        if (this.m_throwExceptionOnError) {
            throw exception;
        }
        PrintWriter pw = getErrorWriter();
        printLocation(pw, (Throwable) exception);
        pw.println(exception.getMessage());
    }

    public void fatalError(TransformerException exception) throws TransformerException {
        if (this.m_throwExceptionOnError) {
            throw exception;
        }
        PrintWriter pw = getErrorWriter();
        printLocation(pw, (Throwable) exception);
        pw.println(exception.getMessage());
    }

    public static void ensureLocationSet(TransformerException exception) {
        SourceLocator locator = null;
        Throwable cause = exception;
        do {
            if (cause instanceof SAXParseException) {
                locator = new SAXSourceLocator((SAXParseException) cause);
            } else if (cause instanceof TransformerException) {
                SourceLocator causeLocator = ((TransformerException) cause).getLocator();
                if (causeLocator != null) {
                    locator = causeLocator;
                }
            }
            if (cause instanceof TransformerException) {
                cause = ((TransformerException) cause).getCause();
                continue;
            } else if (cause instanceof SAXException) {
                cause = ((SAXException) cause).getException();
                continue;
            } else {
                cause = null;
                continue;
            }
        } while (cause != null);
        exception.setLocator(locator);
    }

    public static void printLocation(PrintStream pw, TransformerException exception) {
        printLocation(new PrintWriter(pw), (Throwable) exception);
    }

    public static void printLocation(PrintStream pw, SAXParseException exception) {
        printLocation(new PrintWriter(pw), (Throwable) exception);
    }

    public static void printLocation(PrintWriter pw, Throwable exception) {
        SourceLocator locator = null;
        Throwable cause = exception;
        do {
            if (cause instanceof SAXParseException) {
                locator = new SAXSourceLocator((SAXParseException) cause);
            } else if (cause instanceof TransformerException) {
                SourceLocator causeLocator = ((TransformerException) cause).getLocator();
                if (causeLocator != null) {
                    locator = causeLocator;
                }
            }
            if (cause instanceof TransformerException) {
                cause = ((TransformerException) cause).getCause();
                continue;
            } else if (cause instanceof WrappedRuntimeException) {
                cause = ((WrappedRuntimeException) cause).getException();
                continue;
            } else if (cause instanceof SAXException) {
                cause = ((SAXException) cause).getException();
                continue;
            } else {
                cause = null;
                continue;
            }
        } while (cause != null);
        if (locator != null) {
            String id = locator.getPublicId() != null ? locator.getPublicId() : locator.getSystemId() != null ? locator.getSystemId() : XMLMessages.createXMLMessage(XMLErrorResources.ER_SYSTEMID_UNKNOWN, null);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(id);
            stringBuilder.append("; ");
            stringBuilder.append(XMLMessages.createXMLMessage("line", null));
            stringBuilder.append(locator.getLineNumber());
            stringBuilder.append("; ");
            stringBuilder.append(XMLMessages.createXMLMessage("column", null));
            stringBuilder.append(locator.getColumnNumber());
            stringBuilder.append("; ");
            pw.print(stringBuilder.toString());
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("(");
        stringBuilder2.append(XMLMessages.createXMLMessage(XMLErrorResources.ER_LOCATION_UNKNOWN, null));
        stringBuilder2.append(")");
        pw.print(stringBuilder2.toString());
    }
}
