package com.huawei.android.pushagent.datatype.tcp;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;
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

    private static byte ae() {
        return (byte) 64;
    }

    public DeviceRegisterReqMessage() {
        super(ae());
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

    public void ad(JSONObject jSONObject) {
        this.payload = jSONObject;
    }

    private byte[] af(String str) {
        if (TextUtils.isEmpty(str)) {
            return new byte[0];
        }
        byte[] bytes;
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            a.su("PushLog3414", "fail to get parm bytes");
            bytes = new byte[0];
        }
        return bytes;
    }

    public byte[] b() {
        if (TextUtils.isEmpty(this.deviceId)) {
            a.su("PushLog3414", "encode error, reason mDeviceId = " + this.deviceId);
            return new byte[0];
        } else if (this.payload == null) {
            a.su("PushLog3414", "encode error, payload is null");
            return new byte[0];
        } else {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(c());
                byte[] af = af(this.deviceId);
                byte[] af2 = af(this.payload.toString());
                byteArrayOutputStream.write(d.zr(((((((af.length + 4) + 1) + 4) + 1) + 1) + 2) + af2.length));
                byteArrayOutputStream.write(af.length);
                byteArrayOutputStream.write(af);
                byteArrayOutputStream.write(this.networkType);
                byteArrayOutputStream.write(d.zi(this.agentVersion));
                byteArrayOutputStream.write(this.chanMode);
                byteArrayOutputStream.write(this.control);
                byteArrayOutputStream.write(d.zr(af2.length));
                byteArrayOutputStream.write(af2);
                return byteArrayOutputStream.toByteArray();
            } catch (Exception e) {
                a.su("PushLog3414", "encode error " + e.toString());
                return new byte[0];
            }
        }
    }

    public PushMessage a(InputStream inputStream) {
        return null;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(d()).append(" mDeviceId:").append(this.deviceId).append(" mNetworkType:").append(this.networkType).append(" mAgentVersion:").append(this.agentVersion).toString();
    }
}
