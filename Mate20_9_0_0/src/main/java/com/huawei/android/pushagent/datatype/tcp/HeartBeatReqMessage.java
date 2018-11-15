package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import java.io.InputStream;

public class HeartBeatReqMessage extends PushMessage {
    private static final long serialVersionUID = 135034166096684602L;
    private byte nextHeartBeatInterval = (byte) 10;

    public HeartBeatReqMessage() {
        super(jk());
    }

    private static byte jk() {
        return (byte) -38;
    }

    public void jj(byte b) {
        this.nextHeartBeatInterval = b;
    }

    public byte[] is() {
        return new byte[]{it(), this.nextHeartBeatInterval};
    }

    public PushMessage jc(InputStream inputStream) {
        return null;
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(" cmdId:").append(iw()).append(" NextHeartBeatInterval:").append(this.nextHeartBeatInterval).toString();
    }
}
