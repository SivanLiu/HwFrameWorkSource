package com.android.server.hidata.wavemapping.entity;

import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

public class FingerInfo {
    int ar;
    int batch;
    HashMap<String, Integer> bissiddatas;
    String jsonResult;
    int labelId;
    int linkSpeed;
    int screen;
    String serveMac;
    String timestamp;

    public String getServeMac() {
        return this.serveMac;
    }

    public void setServeMac(String serveMac) {
        this.serveMac = serveMac;
    }

    public int getLabelId() {
        return this.labelId;
    }

    public void setLabelId(int labelId) {
        this.labelId = labelId;
    }

    public int getBatch() {
        return this.batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public int getAr() {
        return this.ar;
    }

    public void setAr(int ar) {
        this.ar = ar;
    }

    public int getScreen() {
        return this.screen;
    }

    public void setScreen(int screen) {
        this.screen = screen;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getLinkSpeed() {
        return this.linkSpeed;
    }

    public void setLinkSpeed(int linkSpeed) {
        this.linkSpeed = linkSpeed;
    }

    public HashMap<String, Integer> getBissiddatas() {
        return this.bissiddatas;
    }

    public void setBissiddatas(HashMap<String, Integer> bissiddatas) {
        this.bissiddatas = bissiddatas;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("FingerInfo{batch=");
        stringBuilder.append(this.batch);
        stringBuilder.append(", ar=");
        stringBuilder.append(this.ar);
        stringBuilder.append(", screen=");
        stringBuilder.append(this.screen);
        stringBuilder.append(", timestamp='");
        stringBuilder.append(this.timestamp);
        stringBuilder.append('\'');
        stringBuilder.append(", linkSpeed=");
        stringBuilder.append(this.linkSpeed);
        stringBuilder.append(", labelId=");
        stringBuilder.append(this.labelId);
        stringBuilder.append(", bissiddatas=");
        stringBuilder.append(this.bissiddatas);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public String toReString() {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("batch", this.batch);
            jsonObj.put("ar", this.ar);
            jsonObj.put("screen", this.screen);
            jsonObj.put("timestamp", this.timestamp);
            jsonObj.put("linkSpeed", this.linkSpeed);
            jsonObj.put("labelId", this.labelId);
            jsonObj.put("bissiddatas", this.bissiddatas);
        } catch (JSONException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LocatingState,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        this.jsonResult = jsonObj.toString();
        return this.jsonResult.replace(",", CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
    }
}
