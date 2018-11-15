package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.b.a;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import java.io.InputStream;
import org.json.JSONObject;

public class DeviceRegisterRspMessage extends PushMessage {
    private static final long serialVersionUID = -6902385088066252595L;
    private JSONObject payload;
    private byte result = (byte) 1;

    private static byte zk() {
        return (byte) 65;
    }

    public DeviceRegisterRspMessage() {
        super(zk());
    }

    public byte getResult() {
        return this.result;
    }

    public byte[] yp() {
        return new byte[0];
    }

    public PushMessage yw(InputStream inputStream) {
        a ys = ys(inputStream);
        this.result = ys.xx(1)[0];
        byte b = ys.xx(1)[0];
        if (!yt(b)) {
            return null;
        }
        this.payload = yu(b, ys);
        return this;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(yx()).append(" result:").append(this.result).toString();
    }
}
