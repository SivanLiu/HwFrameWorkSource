package gov.nist.javax.sip.stack;

import gov.nist.core.Separators;
import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.LogRecord;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.message.SIPMessage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Properties;
import javax.sip.SipStack;
import javax.sip.header.TimeStampHeader;
import javax.sip.message.Request;

public class ServerLog implements ServerLogger {
    private String auxInfo;
    private Properties configurationProperties;
    private String description;
    private boolean logContent;
    private String logFileName;
    private PrintWriter printWriter;
    private SIPTransactionStack sipStack;
    private String stackIpAddress;
    protected StackLogger stackLogger;
    protected int traceLevel = 16;

    private void setProperties(Properties configurationProperties) {
        this.configurationProperties = configurationProperties;
        this.description = configurationProperties.getProperty("javax.sip.STACK_NAME");
        this.stackIpAddress = configurationProperties.getProperty("javax.sip.IP_ADDRESS");
        this.logFileName = configurationProperties.getProperty("gov.nist.javax.sip.SERVER_LOG");
        String logLevel = configurationProperties.getProperty("gov.nist.javax.sip.TRACE_LEVEL");
        String logContent = configurationProperties.getProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT");
        boolean z = logContent != null && logContent.equals("true");
        this.logContent = z;
        if (!(logLevel == null || logLevel.equals("LOG4J"))) {
            try {
                int ll;
                if (logLevel.equals("DEBUG")) {
                    ll = 32;
                } else if (logLevel.equals(Request.INFO)) {
                    ll = 16;
                } else if (logLevel.equals("ERROR")) {
                    ll = 4;
                } else {
                    if (!logLevel.equals("NONE")) {
                        if (!logLevel.equals("OFF")) {
                            ll = Integer.parseInt(logLevel);
                            setTraceLevel(ll);
                        }
                    }
                    ll = 0;
                }
                setTraceLevel(ll);
            } catch (NumberFormatException e) {
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ServerLog: WARNING Bad integer ");
                stringBuilder.append(logLevel);
                printStream.println(stringBuilder.toString());
                System.out.println("logging dislabled ");
                setTraceLevel(0);
            }
        }
        checkLogFile();
    }

    public void setStackIpAddress(String ipAddress) {
        this.stackIpAddress = ipAddress;
    }

    public synchronized void closeLogFile() {
        if (this.printWriter != null) {
            this.printWriter.close();
            this.printWriter = null;
        }
    }

    public void checkLogFile() {
        if (this.logFileName != null && this.traceLevel >= 16) {
            try {
                File logFile = new File(this.logFileName);
                if (!logFile.exists()) {
                    logFile.createNewFile();
                    this.printWriter = null;
                }
                if (this.printWriter == null) {
                    this.printWriter = new PrintWriter(new FileWriter(this.logFileName, !Boolean.valueOf(this.configurationProperties.getProperty("gov.nist.javax.sip.SERVER_LOG_OVERWRITE")).booleanValue()), true);
                    PrintWriter printWriter = this.printWriter;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("<!-- Use the  Trace Viewer in src/tools/tracesviewer to view this  trace  \nHere are the stack configuration properties \njavax.sip.IP_ADDRESS= ");
                    stringBuilder.append(this.configurationProperties.getProperty("javax.sip.IP_ADDRESS"));
                    stringBuilder.append("\njavax.sip.STACK_NAME= ");
                    stringBuilder.append(this.configurationProperties.getProperty("javax.sip.STACK_NAME"));
                    stringBuilder.append("\njavax.sip.ROUTER_PATH= ");
                    stringBuilder.append(this.configurationProperties.getProperty("javax.sip.ROUTER_PATH"));
                    stringBuilder.append("\njavax.sip.OUTBOUND_PROXY= ");
                    stringBuilder.append(this.configurationProperties.getProperty("javax.sip.OUTBOUND_PROXY"));
                    stringBuilder.append("\n-->");
                    printWriter.println(stringBuilder.toString());
                    printWriter = this.printWriter;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("<description\n logDescription=\"");
                    stringBuilder.append(this.description);
                    stringBuilder.append("\"\n name=\"");
                    stringBuilder.append(this.configurationProperties.getProperty("javax.sip.STACK_NAME"));
                    stringBuilder.append("\"\n auxInfo=\"");
                    stringBuilder.append(this.auxInfo);
                    stringBuilder.append("\"/>\n ");
                    printWriter.println(stringBuilder.toString());
                    StackLogger stackLogger;
                    if (this.auxInfo != null) {
                        if (this.sipStack.isLoggingEnabled()) {
                            stackLogger = this.stackLogger;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Here are the stack configuration properties \njavax.sip.IP_ADDRESS= ");
                            stringBuilder.append(this.configurationProperties.getProperty("javax.sip.IP_ADDRESS"));
                            stringBuilder.append("\njavax.sip.ROUTER_PATH= ");
                            stringBuilder.append(this.configurationProperties.getProperty("javax.sip.ROUTER_PATH"));
                            stringBuilder.append("\njavax.sip.OUTBOUND_PROXY= ");
                            stringBuilder.append(this.configurationProperties.getProperty("javax.sip.OUTBOUND_PROXY"));
                            stringBuilder.append("\ngov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS= ");
                            stringBuilder.append(this.configurationProperties.getProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS"));
                            stringBuilder.append("\ngov.nist.javax.sip.CACHE_SERVER_CONNECTIONS= ");
                            stringBuilder.append(this.configurationProperties.getProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS"));
                            stringBuilder.append("\ngov.nist.javax.sip.REENTRANT_LISTENER= ");
                            stringBuilder.append(this.configurationProperties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER"));
                            stringBuilder.append("gov.nist.javax.sip.THREAD_POOL_SIZE= ");
                            stringBuilder.append(this.configurationProperties.getProperty("gov.nist.javax.sip.THREAD_POOL_SIZE"));
                            stringBuilder.append(Separators.RETURN);
                            stackLogger.logDebug(stringBuilder.toString());
                            this.stackLogger.logDebug(" ]]> ");
                            this.stackLogger.logDebug("</debug>");
                            stackLogger = this.stackLogger;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("<description\n logDescription=\"");
                            stringBuilder.append(this.description);
                            stringBuilder.append("\"\n name=\"");
                            stringBuilder.append(this.stackIpAddress);
                            stringBuilder.append("\"\n auxInfo=\"");
                            stringBuilder.append(this.auxInfo);
                            stringBuilder.append("\"/>\n ");
                            stackLogger.logDebug(stringBuilder.toString());
                            this.stackLogger.logDebug("<debug>");
                            this.stackLogger.logDebug("<![CDATA[ ");
                        }
                    } else if (this.sipStack.isLoggingEnabled()) {
                        stackLogger = this.stackLogger;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Here are the stack configuration properties \n");
                        stringBuilder.append(this.configurationProperties);
                        stringBuilder.append(Separators.RETURN);
                        stackLogger.logDebug(stringBuilder.toString());
                        this.stackLogger.logDebug(" ]]>");
                        this.stackLogger.logDebug("</debug>");
                        stackLogger = this.stackLogger;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("<description\n logDescription=\"");
                        stringBuilder.append(this.description);
                        stringBuilder.append("\"\n name=\"");
                        stringBuilder.append(this.stackIpAddress);
                        stringBuilder.append("\" />\n");
                        stackLogger.logDebug(stringBuilder.toString());
                        this.stackLogger.logDebug("<debug>");
                        this.stackLogger.logDebug("<![CDATA[ ");
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    public boolean needsLogging() {
        return this.logFileName != null;
    }

    public void setLogFileName(String name) {
        this.logFileName = name;
    }

    public String getLogFileName() {
        return this.logFileName;
    }

    private void logMessage(String message) {
        checkLogFile();
        String logInfo = message;
        if (this.printWriter != null) {
            this.printWriter.println(logInfo);
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.stackLogger.logInfo(logInfo);
        }
    }

    private void logMessage(String message, String from, String to, boolean sender, String callId, String firstLine, String status, String tid, long time, long timestampVal) {
        LogRecord log = this.sipStack.logRecordFactory.createLogRecord(message, from, to, time, sender, firstLine, tid, callId, timestampVal);
        if (log != null) {
            logMessage(log.toString());
        }
    }

    public void logMessage(SIPMessage message, String from, String to, boolean sender, long time) {
        checkLogFile();
        if (message.getFirstLine() != null) {
            CallID cid = (CallID) message.getCallId();
            String callId = null;
            if (cid != null) {
                callId = cid.getCallId();
            }
            TimeStampHeader tsHdr = (TimeStampHeader) message.getHeader("Timestamp");
            logMessage(this.logContent ? message.encode() : message.encodeMessage(), from, to, sender, callId, message.getFirstLine().trim(), null, message.getTransactionId(), time, tsHdr == null ? 0 : tsHdr.getTime());
        }
    }

    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender, long time) {
        checkLogFile();
        CallID cid = (CallID) message.getCallId();
        String callId = null;
        if (cid != null) {
            callId = cid.getCallId();
        }
        TimeStampHeader tshdr = (TimeStampHeader) message.getHeader("Timestamp");
        logMessage(this.logContent ? message.encode() : message.encodeMessage(), from, to, sender, callId, message.getFirstLine().trim(), status, message.getTransactionId(), time, tshdr == null ? 0 : tshdr.getTime());
    }

    public void logMessage(SIPMessage message, String from, String to, String status, boolean sender) {
        logMessage(message, from, to, status, sender, System.currentTimeMillis());
    }

    public void logException(Exception ex) {
        if (this.traceLevel >= 4) {
            checkLogFile();
            ex.printStackTrace();
            if (this.printWriter != null) {
                ex.printStackTrace(this.printWriter);
            }
        }
    }

    public void setTraceLevel(int level) {
        this.traceLevel = level;
    }

    public int getTraceLevel() {
        return this.traceLevel;
    }

    public void setAuxInfo(String auxInfo) {
        this.auxInfo = auxInfo;
    }

    public void setSipStack(SipStack sipStack) {
        if (sipStack instanceof SIPTransactionStack) {
            this.sipStack = (SIPTransactionStack) sipStack;
            this.stackLogger = this.sipStack.getStackLogger();
            return;
        }
        throw new IllegalArgumentException("sipStack must be a SIPTransactionStack");
    }

    public void setStackProperties(Properties stackProperties) {
        setProperties(stackProperties);
    }

    public void setLevel(int jsipLoggingLevel) {
    }
}
