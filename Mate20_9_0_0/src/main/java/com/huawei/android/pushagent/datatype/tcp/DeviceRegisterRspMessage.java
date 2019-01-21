package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.a.d;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import java.io.InputStream;
import org.json.JSONObject;

public class DeviceRegisterRspMessage extends PushMessage {
    private static final long serialVersionUID = -6902385088066252595L;
    private JSONObject payload;
    private byte result = (byte) 1;

    private static byte ah() {
        return (byte) 65;
    }

    public DeviceRegisterRspMessage() {
        super(ah());
    }

    public byte getResult() {
        return this.result;
    }

    public byte[] b() {
        return new byte[0];
    }

    public PushMessage a(InputStream inputStream) {
        d e = e(inputStream);
        this.result = e.ax(1)[0];
        byte b = e.ax(1)[0];
        if (!h(b)) {
            return null;
        }
        this.payload = f(b, e);
        return this;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(d()).append(" result:").append(this.result).toString();
    }
}
