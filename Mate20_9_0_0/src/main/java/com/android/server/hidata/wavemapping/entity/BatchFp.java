package com.android.server.hidata.wavemapping.entity;

import java.util.ArrayList;
import java.util.List;

public class BatchFp {
    private List<Float> avgs = new ArrayList();
    private int batch;
    private float serverRssiAvg;

    public BatchFp(int batch) {
        this.batch = batch;
    }

    public float getServerRssiAvg() {
        return this.serverRssiAvg;
    }

    public void setServerRssiAvg(float serverRssiAvg) {
        this.serverRssiAvg = serverRssiAvg;
    }

    public int getBatch() {
        return this.batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public List<Float> getAvgs() {
        return this.avgs;
    }

    public void setAvgs(List<Float> avgs) {
        this.avgs = avgs;
    }
}
