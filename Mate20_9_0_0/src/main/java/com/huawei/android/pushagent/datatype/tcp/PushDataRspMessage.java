package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PushDataRspMessage extends PushMessage {
    private static final long serialVersionUID = -8613876352739508266L;
    private byte control;
    private byte[] cookie;
    private byte[] msgId;
    private byte result;
    private byte[] tokenBytes;

    private static byte jg() {
        return (byte) 69;
    }

    public PushDataRspMessage() {
        super(jg());
    }

    public PushDataRspMessage(byte[] bArr, byte b, byte b2, byte[] bArr2, byte[] bArr3) {
        this();
        if (bArr == null) {
            this.msgId = new byte[0];
        } else {
            this.msgId = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.msgId, 0, bArr.length);
        }
        this.result = b;
        this.control = (byte) (b2 & 48);
        je(bArr2);
        if (bArr3 == null) {
            bArr3 = new byte[0];
        }
        this.tokenBytes = bArr3;
    }

    private void je(byte[] bArr) {
        if (bArr == null) {
            bArr = new byte[0];
        }
        if ((this.control & 32) == 32) {
            this.cookie = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.cookie, 0, bArr.length);
        }
    }

    public PushMessage jc(InputStream inputStream) {
        return null;
    }

    public void ji(byte b) {
        this.result = b;
    }

    public byte jf() {
        return this.result;
    }

    public byte[] is() {
        if (this.msgId == null) {
            c.eq("PushLog3413", "encode error, mMsgId is null ");
            return new byte[0];
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(it());
            int length = ((((this.msgId.length + 4) + 2) + this.tokenBytes.length) + 1) + 1;
            if ((this.control & 32) == 32) {
                length += this.cookie.length + 2;
            }
            byteArrayOutputStream.write(g.gd(length));
            byteArrayOutputStream.write(this.msgId.length);
            byteArrayOutputStream.write(this.msgId);
            byteArrayOutputStream.write(g.gd(this.tokenBytes.length));
            byteArrayOutputStream.write(this.tokenBytes);
            byteArrayOutputStream.write(this.result);
            byteArrayOutputStream.write(this.control);
            if ((this.control & 32) == 32) {
                byteArrayOutputStream.write(g.gd(this.cookie.length));
                byteArrayOutputStream.write(this.cookie);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            c.eq("PushLog3413", "encode error " + e.toString());
            return new byte[0];
        }
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(",cmdId:").append(iw()).append(",msgId:").append(b.el(this.msgId)).append(",flag:").append(this.result).toString();
    }

    public byte[] jh() {
        return this.msgId;
    }
}
