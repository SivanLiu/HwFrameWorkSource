package com.huawei.systemmanager.mms;

public class HwMmsInfo {
    private long expiry;
    private String from;
    private long messageSize;
    private int messageType;
    private String subject;

    public HwMmsInfo(String from, String subject, long messageSize, long expiry, int messageType) {
        this.from = from;
        this.subject = subject;
        this.messageSize = messageSize;
        this.expiry = expiry;
        this.messageType = messageType;
    }

    public String getFrom() {
        return this.from;
    }

    public String getSubject() {
        return this.subject;
    }

    public long getMessageSize() {
        return this.messageSize;
    }

    public long getExpiry() {
        return this.expiry;
    }

    public boolean isNotificationMsg() {
        return this.messageType == 130;
    }
}
