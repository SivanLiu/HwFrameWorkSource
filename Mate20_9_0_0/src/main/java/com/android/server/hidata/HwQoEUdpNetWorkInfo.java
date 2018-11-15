package com.android.server.hidata;

public class HwQoEUdpNetWorkInfo {
    public int mNetworkID;
    public long mRxTcpBytes = 0;
    public long mRxTcpPackets = 0;
    private long mRxUdpBytes;
    private long mRxUdpPackets;
    private long mSumRxPackets;
    private int mSumUdpSockets;
    private int mSumUdpUids;
    private long mTimestamp;
    public long mTxTcpBytes = 0;
    public long mTxTcpPackets = 0;
    private long mTxUdpBytes;
    private long mTxUdpPackets;
    private int mUid;
    private int mUidUdpSockets;

    public void setUdpNetWorkInfo(HwQoEUdpNetWorkInfo newInfo) {
        if (newInfo != null) {
            this.mRxUdpBytes = newInfo.getRxUdpBytes();
            this.mRxUdpPackets = newInfo.getRxUdpPackets();
            this.mTxUdpBytes = newInfo.getTxUdpBytes();
            this.mTxUdpPackets = newInfo.getTxUdpPackets();
            this.mUid = newInfo.getUid();
            this.mTimestamp = newInfo.getTimestamp();
            this.mUidUdpSockets = newInfo.getUidUdpSockets();
            this.mSumUdpUids = newInfo.getSumUdpUids();
            this.mSumUdpSockets = newInfo.getSumUdpSockets();
            this.mSumRxPackets = newInfo.getSumRxPackets();
            this.mRxTcpBytes = newInfo.mRxTcpBytes;
            this.mRxTcpPackets = newInfo.mRxTcpPackets;
            this.mTxTcpBytes = newInfo.mTxTcpBytes;
            this.mTxTcpPackets = newInfo.mTxTcpPackets;
            this.mNetworkID = newInfo.mNetworkID;
        }
    }

    public void setRxUdpBytes(long mRxUdpBytes) {
        this.mRxUdpBytes = mRxUdpBytes;
    }

    public long getRxUdpBytes() {
        return this.mRxUdpBytes;
    }

    public void setRxUdpPackets(long mRxUdpPackets) {
        this.mRxUdpPackets = mRxUdpPackets;
    }

    public long getRxUdpPackets() {
        return this.mRxUdpPackets;
    }

    public void setTxUdpBytes(long mTxUdpBytes) {
        this.mTxUdpBytes = mTxUdpBytes;
    }

    public long getTxUdpBytes() {
        return this.mTxUdpBytes;
    }

    public void setTxUdpPackets(long mTxUdpPackets) {
        this.mTxUdpPackets = mTxUdpPackets;
    }

    public long getTxUdpPackets() {
        return this.mTxUdpPackets;
    }

    public void setUid(int mUid) {
        this.mUid = mUid;
    }

    public int getUid() {
        return this.mUid;
    }

    public void setUidUdpSockets(int mUidUdpSockets) {
        this.mUidUdpSockets = mUidUdpSockets;
    }

    public int getUidUdpSockets() {
        return this.mUidUdpSockets;
    }

    public void setSumUdpUids(int mSumUdpUids) {
        this.mSumUdpUids = mSumUdpUids;
    }

    public int getSumUdpUids() {
        return this.mSumUdpUids;
    }

    public void setSumUdpSockets(int mSumUdpSockets) {
        this.mSumUdpSockets = mSumUdpSockets;
    }

    public int getSumUdpSockets() {
        return this.mSumUdpSockets;
    }

    public long getTimestamp() {
        return this.mTimestamp;
    }

    public void setTimestamp(long mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

    public long getSumRxPackets() {
        return this.mSumRxPackets;
    }

    public void setSumRxPackets(long mSumRxPackets) {
        this.mSumRxPackets = mSumRxPackets;
    }

    public void setNetworkID(int mNetworkID) {
        this.mNetworkID = mNetworkID;
    }

    public int getNetwork() {
        return this.mNetworkID;
    }

    public String dump() {
        StringBuffer buffer = new StringBuffer("UdpNetWorkInfo : ");
        buffer.append("Timestamp: ");
        buffer.append(this.mTimestamp);
        buffer.append(" uid: ");
        buffer.append(this.mUid);
        buffer.append(" mUidUdpSockets: ");
        buffer.append(this.mUidUdpSockets);
        buffer.append(" mRxUdpBytes: ");
        buffer.append(this.mRxUdpBytes);
        buffer.append(" mRxUdpPackets: ");
        buffer.append(this.mRxUdpPackets);
        buffer.append(" mSumRxPackets: ");
        buffer.append(this.mSumRxPackets);
        buffer.append(" mTxUdpBytes: ");
        buffer.append(this.mTxUdpBytes);
        buffer.append(" mTxUdpPackets: ");
        buffer.append(this.mTxUdpPackets);
        buffer.append(" mSumUdpUids: ");
        buffer.append(this.mSumUdpUids);
        buffer.append(" mSumUdpSockets: ");
        buffer.append(this.mSumUdpSockets);
        buffer.append(" mNetworkID: ");
        buffer.append(this.mNetworkID);
        return buffer.toString();
    }
}
