package com.android.server.hidata.wavemapping.entity;

import java.util.ArrayList;
import java.util.List;

public class StdRecord {
    private int batch = 0;
    private List<Integer> scanRssis;
    private int serveRssi = 0;
    private int serverLinkSpeed = 0;
    private String timeStamp;

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getBatch() {
        return this.batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public int getServerLinkSpeed() {
        return this.serverLinkSpeed;
    }

    public void setServerLinkSpeed(int serverLinkSpeed) {
        this.serverLinkSpeed = serverLinkSpeed;
    }

    public StdRecord(int batch) {
        this.batch = batch;
        this.scanRssis = new ArrayList();
    }

    public int getServeRssi() {
        return this.serveRssi;
    }

    public void setServeRssi(int serveRssi) {
        this.serveRssi = serveRssi;
    }

    public List<Integer> getScanRssis() {
        return this.scanRssis;
    }

    public void setScanRssis(List<Integer> scanRssis) {
        this.scanRssis = scanRssis;
    }
}
