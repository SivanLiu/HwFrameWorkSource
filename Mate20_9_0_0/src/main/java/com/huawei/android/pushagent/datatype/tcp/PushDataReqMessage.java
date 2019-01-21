package com.huawei.android.pushagent.datatype.tcp;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.a.d;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.e.c;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONException;
import org.json.JSONObject;

public class PushDataReqMessage extends PushMessage {
    private static final long serialVersionUID = -6661677033729532404L;
    private byte control;
    private byte[] mCookie;
    private byte[] msgId;
    private JSONObject payload;
    private byte[] tokenBytes;

    public static final byte ac() {
        return (byte) 68;
    }

    public PushDataReqMessage() {
        super(ac());
    }

    public byte[] aa() {
        if (this.mCookie == null || this.mCookie.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.mCookie.length];
        System.arraycopy(this.mCookie, 0, bArr, 0, this.mCookie.length);
        return bArr;
    }

    public byte z() {
        return this.control;
    }

    public byte[] y() {
        if (this.msgId == null || this.msgId.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.msgId.length];
        System.arraycopy(this.msgId, 0, bArr, 0, this.msgId.length);
        return bArr;
    }

    public byte[] b() {
        return new byte[0];
    }

    public boolean isValid() {
        boolean z;
        String pkgName = getPkgName();
        if (this.tokenBytes == null || this.tokenBytes.length <= 0) {
            z = false;
        } else {
            z = true;
        }
        int i;
        if (this.msgId == null || this.msgId.length <= 0) {
            i = 0;
        } else {
            i = 1;
        }
        if (z && !TextUtils.isEmpty(pkgName) && (i ^ 1) == 0 && x().length != 0) {
            return true;
        }
        a.su("PushLog3414", "token pkgName msgId msgBody exist null");
        return false;
    }

    public PushMessage a(InputStream inputStream) {
        d e = e(inputStream);
        this.msgId = e.ax(com.huawei.android.pushagent.utils.d.zs(e.ax(1)[0]));
        this.tokenBytes = e.ax(com.huawei.android.pushagent.utils.d.yg(e.ax(2)));
        this.control = e.ax(1)[0];
        if (!h(this.control)) {
            return null;
        }
        a.sv("PushLog3414", "control is: " + this.control);
        if ((this.control & 32) == 32) {
            int yg = com.huawei.android.pushagent.utils.d.yg(e.ax(2));
            a.sv("PushLog3414", "cookieLen is: " + yg);
            this.mCookie = e.ax(yg);
        }
        this.payload = f(this.control, e);
        return this;
    }

    public int getUserId() {
        if (this.payload == null) {
            a.su("PushLog3414", "payload is null");
            return 0;
        } else if (this.payload.has("userId")) {
            try {
                return this.payload.getInt("userId");
            } catch (JSONException e) {
                a.su("PushLog3414", "fail to get userid from payload");
            }
        } else {
            a.su("PushLog3414", "payload not has userid");
            return 0;
        }
    }

    public String getPkgName() {
        if (this.payload == null) {
            a.su("PushLog3414", "payload is null");
            return null;
        } else if (this.payload.has("pkgName")) {
            try {
                return this.payload.getString("pkgName");
            } catch (JSONException e) {
                a.su("PushLog3414", "fail to get pkgname from payload");
            }
        } else {
            a.su("PushLog3414", "payload not has pkgname");
            return null;
        }
    }

    public byte[] w() {
        if (this.tokenBytes == null || this.tokenBytes.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.tokenBytes.length];
        System.arraycopy(this.tokenBytes, 0, bArr, 0, this.tokenBytes.length);
        return bArr;
    }

    public int ab() {
        if (this.payload == null) {
            a.su("PushLog3414", "payload is null");
            return -1;
        } else if (this.payload.has("msgType")) {
            try {
                return this.payload.getInt("msgType");
            } catch (JSONException e) {
                a.su("PushLog3414", "fail to get msgType from payload");
            }
        } else {
            a.su("PushLog3414", "payload not has msgType");
            return -1;
        }
    }

    public byte[] x() {
        if (this.payload == null) {
            a.su("PushLog3414", "payload is null");
            return new byte[0];
        }
        if (this.payload.has("msg")) {
            try {
                String string = this.payload.getString("msg");
                if (string != null) {
                    return string.getBytes("UTF-8");
                }
            } catch (JSONException e) {
                a.su("PushLog3414", "fail to get msg from payload");
            } catch (UnsupportedEncodingException e2) {
                a.su("PushLog3414", "fail to get msg from payload, unsupport encoding type");
            }
        } else {
            a.su("PushLog3414", "payload not has msg");
        }
        return new byte[0];
    }

    public String toString() {
        return "payload is" + c.wi(this.payload);
    }
}
