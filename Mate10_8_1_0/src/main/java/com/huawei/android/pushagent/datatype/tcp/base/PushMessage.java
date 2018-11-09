package com.huawei.android.pushagent.datatype.tcp.base;

import com.huawei.android.pushagent.utils.a.a;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class PushMessage implements IPushMessage {
    protected byte mCmdId = (byte) -1;
    protected byte mSubCmdId = (byte) -1;

    public abstract String toString();

    public abstract byte[] yp();

    public abstract PushMessage yw(InputStream inputStream);

    public String yx() {
        return a.u(new byte[]{this.mCmdId});
    }

    public PushMessage(byte b) {
        yv(b);
    }

    public byte yq() {
        return this.mCmdId;
    }

    public void yv(byte b) {
        this.mCmdId = b;
    }

    public byte yr() {
        return this.mSubCmdId;
    }

    public void yy(byte b) {
        this.mSubCmdId = b;
    }

    public com.huawei.android.pushagent.datatype.b.a ys(InputStream inputStream) {
        byte[] bArr = new byte[2];
        yz(inputStream, bArr);
        int ft = f.ft(bArr);
        b.x("PushLog2976", "msg total lenth is: " + ft);
        bArr = new byte[((ft - 1) - 2)];
        yz(inputStream, bArr);
        return new com.huawei.android.pushagent.datatype.b.a(bArr);
    }

    public JSONObject yu(byte b, com.huawei.android.pushagent.datatype.b.a aVar) {
        if ((b & 2) == 2) {
            try {
                byte[] xx = aVar.xx(f.ft(aVar.xx(2)));
                if (xx == null || xx.length == 0) {
                    return null;
                }
                if ((b & 1) == 1) {
                    xx = com.huawei.android.pushagent.utils.b.fg(xx);
                }
                return new JSONObject(new String(xx, "UTF-8"));
            } catch (JSONException e) {
                b.y("PushLog2976", "fail to parser payload");
            } catch (UnsupportedEncodingException e2) {
                b.y("PushLog2976", "unsupported encoding type");
            } catch (ArrayIndexOutOfBoundsException e3) {
                b.y("PushLog2976", "fail to parser payload, array out of bounds");
            }
        }
        return null;
    }

    public boolean yt(byte b) {
        boolean z = false;
        if ((b & 12) == 0) {
            z = true;
        }
        if (!z) {
            b.y("PushLog2976", "tlv is not support in current version");
        }
        return z;
    }

    public static void yz(InputStream inputStream, byte[] bArr) {
        int i = 0;
        while (i < bArr.length) {
            int read = inputStream.read(bArr, i, bArr.length - i);
            if (-1 == read) {
                throw new IOException("read -1 reached");
            }
            i += read;
        }
    }
}
