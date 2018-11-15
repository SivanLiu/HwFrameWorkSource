package com.android.server.hidata.wavemapping.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ModelInfo {
    private String[] bssidLst;
    private int dataLen;
    private int[][] datas;
    private ArrayList<HashMap<String, Integer>> hpDatas;
    private String modelName;
    private String place;
    private HashSet<String> setBssids;
    private String storePath;
    private String updateTime;

    public String getModelName() {
        return this.modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public ArrayList<HashMap<String, Integer>> getHpDatas() {
        return this.hpDatas;
    }

    public void setHpDatas(ArrayList<HashMap<String, Integer>> hpDatas) {
        this.hpDatas = hpDatas;
    }

    public int getDataLen() {
        return this.dataLen;
    }

    public void setDataLen(int dataLen) {
        this.dataLen = dataLen;
    }

    public HashSet<String> getSetBssids() {
        return this.setBssids;
    }

    public void setSetBssids(HashSet<String> setBssids) {
        this.setBssids = setBssids;
    }

    public String[] getBssidLst() {
        if (this.bssidLst == null) {
            return new String[0];
        }
        return (String[]) this.bssidLst.clone();
    }

    public void setBssidLst(String[] bssidLst) {
        if (bssidLst == null) {
            this.bssidLst = null;
        } else {
            this.bssidLst = (String[]) bssidLst.clone();
        }
    }

    public int[][] getDatas() {
        if (this.datas == null) {
            return new int[0][];
        }
        return (int[][]) this.datas.clone();
    }

    public void setDatas(int[][] datas) {
        if (datas == null) {
            this.datas = null;
        } else {
            this.datas = (int[][]) datas.clone();
        }
    }

    public ModelInfo(String place, String storePath, String updateTime) {
        this.place = place;
        this.storePath = storePath;
        this.updateTime = updateTime;
    }

    public ModelInfo(String place, String modelName) {
        this.place = place;
        this.modelName = modelName;
    }

    public String getPlace() {
        return this.place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getStorePath() {
        return this.storePath;
    }

    public void setStorePath(String storePath) {
        this.storePath = storePath;
    }

    public String getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
