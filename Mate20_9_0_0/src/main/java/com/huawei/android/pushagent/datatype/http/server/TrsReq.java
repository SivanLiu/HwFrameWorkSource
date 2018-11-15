package com.huawei.android.pushagent.datatype.http.server;

import android.content.Context;
import android.os.Build;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.utils.d.c;
import com.huawei.android.pushagent.utils.g;

public class TrsReq {
    private static final String PROTOCOL_VERSION_DEFAULT = "2.0";
    private static final int SUPPORT_LOW_FLOW = 2;
    private long agentV;
    private String belongid;
    private int chanMode;
    private String channel;
    private String connId;
    private int deviceIdType = -1;
    private String emV;
    private int info = 0;
    private int intelligent = SUPPORT_LOW_FLOW;
    private String mccmnc;
    private String mode;
    private String protocolversion = PROTOCOL_VERSION_DEFAULT;
    private String pushDeviceId;
    private String rV;
    private String sn;
    private String version;

    public TrsReq(Context context, String str, String str2) {
        this.mccmnc = g.fy(context);
        this.pushDeviceId = g.gi(context);
        this.deviceIdType = g.gp(context);
        this.chanMode = b.oq(context).or();
        this.connId = str2;
        this.belongid = str;
        this.version = g.gk(context);
        this.channel = context.getPackageName();
        this.mode = Build.MODEL;
        this.emV = g.gq();
        this.rV = Build.DISPLAY;
        this.agentV = getAgentV(context);
        if (1 == this.chanMode) {
            this.sn = new c().getDeviceId();
        }
    }

    private long getAgentV(Context context) {
        long co = com.huawei.android.pushagent.utils.tools.b.co(context, "com.huawei.android.pushagent");
        if (co < 0) {
            return -1;
        }
        return co;
    }

    public String getMccmnc() {
        return this.mccmnc;
    }

    public void setMccmnc(String str) {
        this.mccmnc = str;
    }

    public String getPushDeviceId() {
        return this.pushDeviceId;
    }

    public void setPushDeviceId(String str) {
        this.pushDeviceId = str;
    }

    public int getDeviceIdType() {
        return this.deviceIdType;
    }

    public void setDeviceIdType(int i) {
        this.deviceIdType = i;
    }

    public int getChanMode() {
        return this.chanMode;
    }

    public void setChanMode(int i) {
        this.chanMode = i;
    }

    public String getConnId() {
        return this.connId;
    }

    public void setConnId(String str) {
        this.connId = str;
    }

    public String getBelongId() {
        return this.belongid;
    }

    public void setBelongid(String str) {
        this.belongid = str;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String str) {
        this.version = str;
    }

    public String getProtocolversion() {
        return this.protocolversion;
    }

    public void setProtocolversion(String str) {
        this.protocolversion = str;
    }

    public int getInfo() {
        return this.info;
    }

    public void setInfo(int i) {
        this.info = i;
    }

    public String getChannel() {
        return this.channel;
    }

    public void setChannel(String str) {
        this.channel = str;
    }

    public String getMode() {
        return this.mode;
    }

    public void setMode(String str) {
        this.mode = str;
    }

    public int getIntelligent() {
        return this.intelligent;
    }

    public void setIntelligent(int i) {
        this.intelligent = i;
    }

    public String getEmV() {
        return this.emV;
    }

    public void setEmV(String str) {
        this.emV = str;
    }

    public String getrV() {
        return this.rV;
    }

    public void setrV(String str) {
        this.rV = str;
    }

    public long getAgentV() {
        return this.agentV;
    }

    public void setAgentV(long j) {
        this.agentV = j;
    }

    public String getSn() {
        return this.sn;
    }

    public void setSn(String str) {
        this.sn = str;
    }
}
