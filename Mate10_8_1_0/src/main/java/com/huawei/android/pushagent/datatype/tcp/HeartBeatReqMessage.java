package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import java.io.InputStream;

public class HeartBeatReqMessage extends PushMessage {
    private static final long serialVersionUID = 135034166096684602L;
    private byte nextHeartBeatInterval = (byte) 10;

    public HeartBeatReqMessage() {
        super(zp());
    }

    private static byte zp() {
        return (byte) -38;
    }

    public void zo(byte b) {
        this.nextHeartBeatInterval = b;
    }

    public byte[] yp() {
        return new byte[]{yq(), this.nextHeartBeatInterval};
    }

    public PushMessage yw(InputStream inputStream) {
        return null;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(yx()).append(" NextHeartBeatInterval:").append(this.nextHeartBeatInterval).toString();
    }
}
