package com.huawei.nb.coordinator.helper;

public class RefreshResult {
    private long deltaSize;
    private long downloadedSize;
    private int errorCode;
    private boolean finished;
    private long index;

    public int getErrorCode() {
        return this.errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public long getIndex() {
        return this.index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public long getDeltaSize() {
        return this.deltaSize;
    }

    public void setDeltaSize(long deltaSize) {
        this.deltaSize = deltaSize;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void increaseIndex() {
        this.index++;
    }

    public long getDownloadedSize() {
        return this.downloadedSize;
    }

    public void setDownloadedSize(long size) {
        this.downloadedSize = size;
    }
}
