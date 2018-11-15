package com.huawei.android.pushagent.datatype.tcp;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONObject;

public class DeviceRegisterReqMessage extends PushMessage {
    private static final long serialVersionUID = 5945012025726493592L;
    private int agentVersion;
    private byte chanMode;
    private byte control;
    private String deviceId;
    private byte networkType;
    private JSONObject payload;

    private static byte jq() {
        return (byte) 64;
    }

    public DeviceRegisterReqMessage() {
        super(jq());
        this.deviceId = null;
        this.networkType = (byte) -1;
        this.chanMode = (byte) 1;
        this.control = (byte) 2;
        this.payload = new JSONObject();
    }

    public DeviceRegisterReqMessage(String str, byte b, byte b2, int i) {
        this();
        this.deviceId = str;
        this.chanMode = b;
        this.networkType = b2;
        this.agentVersion = i;
    }

    public void jp(JSONObject jSONObject) {
        this.payload = jSONObject;
    }

    private byte[] jr(String str) {
        if (TextUtils.isEmpty(str)) {
            return new byte[0];
        }
        byte[] bytes;
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            c.eq("PushLog3413", "fail to get parm bytes");
            bytes = new byte[0];
        }
        return bytes;
    }

    public byte[] is() {
        if (TextUtils.isEmpty(this.deviceId)) {
            c.eq("PushLog3413", "encode error, reason mDeviceId = " + this.deviceId);
            return new byte[0];
        } else if (this.payload == null) {
            c.eq("PushLog3413", "encode error, payload is null");
            return new byte[0];
        } else {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(it());
                byte[] jr = jr(this.deviceId);
                byte[] jr2 = jr(this.payload.toString());
                byteArrayOutputStream.write(g.gd(((((((jr.length + 4) + 1) + 4) + 1) + 1) + 2) + jr2.length));
                byteArrayOutputStream.write(jr.length);
                byteArrayOutputStream.write(jr);
                byteArrayOutputStream.write(this.networkType);
                byteArrayOutputStream.write(g.gw(this.agentVersion));
                byteArrayOutputStream.write(this.chanMode);
                byteArrayOutputStream.write(this.control);
                byteArrayOutputStream.write(g.gd(jr2.length));
                byteArrayOutputStream.write(jr2);
                return byteArrayOutputStream.toByteArray();
            } catch (Exception e) {
                c.eq("PushLog3413", "encode error " + e.toString());
                return new byte[0];
            }
        }
    }

    public PushMessage jc(InputStream inputStream) {
        return null;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(iw()).append(" mDeviceId:").append(this.deviceId).append(" mNetworkType:").append(this.networkType).append(" mAgentVersion:").append(this.agentVersion).toString();
    }
}
