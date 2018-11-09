package com.huawei.android.pushagent.datatype.tcp;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
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

    private static byte zm() {
        return (byte) 64;
    }

    public DeviceRegisterReqMessage() {
        super(zm());
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

    public void zl(JSONObject jSONObject) {
        this.payload = jSONObject;
    }

    private byte[] zn(String str) {
        if (TextUtils.isEmpty(str)) {
            return new byte[0];
        }
        byte[] bytes;
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            b.y("PushLog2976", "fail to get parm bytes");
            bytes = new byte[0];
        }
        return bytes;
    }

    public byte[] yp() {
        if (TextUtils.isEmpty(this.deviceId)) {
            b.y("PushLog2976", "encode error, reason mDeviceId = " + this.deviceId);
            return new byte[0];
        } else if (this.payload == null) {
            b.y("PushLog2976", "encode error, payload is null");
            return new byte[0];
        } else {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(yq());
                byte[] zn = zn(this.deviceId);
                byte[] zn2 = zn(this.payload.toString());
                byteArrayOutputStream.write(f.fr(((((((zn.length + 4) + 1) + 4) + 1) + 1) + 2) + zn2.length));
                byteArrayOutputStream.write(zn.length);
                byteArrayOutputStream.write(zn);
                byteArrayOutputStream.write(this.networkType);
                byteArrayOutputStream.write(f.gv(this.agentVersion));
                byteArrayOutputStream.write(this.chanMode);
                byteArrayOutputStream.write(this.control);
                byteArrayOutputStream.write(f.fr(zn2.length));
                byteArrayOutputStream.write(zn2);
                return byteArrayOutputStream.toByteArray();
            } catch (Exception e) {
                b.y("PushLog2976", "encode error " + e.toString());
                return new byte[0];
            }
        }
    }

    public PushMessage yw(InputStream inputStream) {
        return null;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(yx()).append(" mDeviceId:").append(this.deviceId).append(" mNetworkType:").append(this.networkType).append(" mAgentVersion:").append(this.agentVersion).toString();
    }
}
