package gov.nist.javax.sip.stack;

import gov.nist.core.Separators;
import gov.nist.javax.sip.LogRecord;

class MessageLog implements LogRecord {
    private String callId;
    private String destination;
    private String firstLine;
    private boolean isSender;
    private String message;
    private String source;
    private String tid;
    private long timeStamp;
    private long timeStampHeaderValue;

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof MessageLog)) {
            return false;
        }
        MessageLog otherLog = (MessageLog) other;
        if (otherLog.message.equals(this.message) && otherLog.timeStamp == this.timeStamp) {
            z = true;
        }
        return z;
    }

    public MessageLog(String message, String source, String destination, String timeStamp, boolean isSender, String firstLine, String tid, String callId, long timeStampHeaderValue) {
        if (message == null || message.equals("")) {
            throw new IllegalArgumentException("null msg");
        }
        this.message = message;
        this.source = source;
        this.destination = destination;
        try {
            long ts = Long.parseLong(timeStamp);
            if (ts >= 0) {
                this.timeStamp = ts;
                this.isSender = isSender;
                this.firstLine = firstLine;
                this.tid = tid;
                this.callId = callId;
                this.timeStampHeaderValue = timeStampHeaderValue;
                return;
            }
            throw new IllegalArgumentException("Bad time stamp ");
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad number format ");
            stringBuilder.append(timeStamp);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public MessageLog(String message, String source, String destination, long timeStamp, boolean isSender, String firstLine, String tid, String callId, long timestampVal) {
        if (message == null || message.equals("")) {
            throw new IllegalArgumentException("null msg");
        }
        this.message = message;
        this.source = source;
        this.destination = destination;
        if (timeStamp >= 0) {
            this.timeStamp = timeStamp;
            this.isSender = isSender;
            this.firstLine = firstLine;
            this.tid = tid;
            this.callId = callId;
            this.timeStampHeaderValue = timestampVal;
            return;
        }
        throw new IllegalArgumentException("negative ts");
    }

    public String toString() {
        StringBuilder stringBuilder;
        String stringBuilder2;
        String log = new StringBuilder();
        log.append("<message\nfrom=\"");
        log.append(this.source);
        log.append("\" \nto=\"");
        log.append(this.destination);
        log.append("\" \ntime=\"");
        log.append(this.timeStamp);
        log.append(Separators.DOUBLE_QUOTE);
        if (this.timeStampHeaderValue != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("\ntimeStamp = \"");
            stringBuilder.append(this.timeStampHeaderValue);
            stringBuilder.append(Separators.DOUBLE_QUOTE);
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "";
        }
        log.append(stringBuilder2);
        log.append("\nisSender=\"");
        log.append(this.isSender);
        log.append("\" \ntransactionId=\"");
        log.append(this.tid);
        log.append("\" \ncallId=\"");
        log.append(this.callId);
        log.append("\" \nfirstLine=\"");
        log.append(this.firstLine.trim());
        log.append("\" \n>\n");
        log = log.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(log);
        stringBuilder.append("<![CDATA[");
        log = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(log);
        stringBuilder.append(this.message);
        log = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(log);
        stringBuilder.append("]]>\n");
        log = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(log);
        stringBuilder.append("</message>\n");
        return stringBuilder.toString();
    }
}
