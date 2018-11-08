package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.a.a;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
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

    private static byte zu() {
        return (byte) 69;
    }

    public PushDataRspMessage() {
        super(zu());
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
        zt(bArr2);
        if (bArr3 == null) {
            bArr3 = new byte[0];
        }
        this.tokenBytes = bArr3;
    }

    private void zt(byte[] bArr) {
        if (bArr == null) {
            bArr = new byte[0];
        }
        if ((this.control & 32) == 32) {
            this.cookie = new byte[bArr.length];
            System.arraycopy(bArr, 0, this.cookie, 0, bArr.length);
        }
    }

    public PushMessage yw(InputStream inputStream) {
        return null;
    }

    public void zq(byte b) {
        this.result = b;
    }

    public byte zs() {
        return this.result;
    }

    public byte[] yp() {
        if (this.msgId == null) {
            b.y("PushLog2976", "encode error, mMsgId is null ");
            return new byte[0];
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(yq());
            int length = ((((this.msgId.length + 4) + 2) + this.tokenBytes.length) + 1) + 1;
            if ((this.control & 32) == 32) {
                length += this.cookie.length + 2;
            }
            byteArrayOutputStream.write(f.fr(length));
            byteArrayOutputStream.write(this.msgId.length);
            byteArrayOutputStream.write(this.msgId);
            byteArrayOutputStream.write(f.fr(this.tokenBytes.length));
            byteArrayOutputStream.write(this.tokenBytes);
            byteArrayOutputStream.write(this.result);
            byteArrayOutputStream.write(this.control);
            if ((this.control & 32) == 32) {
                byteArrayOutputStream.write(f.fr(this.cookie.length));
                byteArrayOutputStream.write(this.cookie);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            b.y("PushLog2976", "encode error " + e.toString());
            return new byte[0];
        }
    }

    public String toString() {
        return new StringBuffer(getClass().getSimpleName()).append(",cmdId:").append(yx()).append(",msgId:").append(a.u(this.msgId)).append(",flag:").append(this.result).toString();
    }

    public byte[] zr() {
        return this.msgId;
    }
}
