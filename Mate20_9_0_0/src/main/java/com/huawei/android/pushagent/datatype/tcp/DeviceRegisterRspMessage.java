package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.a.d;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import java.io.InputStream;
import org.json.JSONObject;

public class DeviceRegisterRspMessage extends PushMessage {
    private static final long serialVersionUID = -6902385088066252595L;
    private JSONObject payload;
    private byte result = (byte) 1;

    private static byte jz() {
        return (byte) 65;
    }

    public DeviceRegisterRspMessage() {
        super(jz());
    }

    public byte getResult() {
        return this.result;
    }

    public byte[] is() {
        return new byte[0];
    }

    public PushMessage jc(InputStream inputStream) {
        d iy = iy(inputStream);
        this.result = iy.kp(1)[0];
        byte b = iy.kp(1)[0];
        if (!iz(b)) {
            return null;
        }
        this.payload = ja(b, iy);
        return this;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(iw()).append(" result:").append(this.result).toString();
    }
}
