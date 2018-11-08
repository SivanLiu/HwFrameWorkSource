package com.huawei.android.pushagent.datatype.tcp;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.b.a;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.c.g;
import com.huawei.android.pushagent.utils.f;
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

    public static final byte zf() {
        return (byte) 68;
    }

    public PushDataReqMessage() {
        super(zf());
    }

    public byte[] ze() {
        if (this.mCookie == null || this.mCookie.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.mCookie.length];
        System.arraycopy(this.mCookie, 0, bArr, 0, this.mCookie.length);
        return bArr;
    }

    public byte zd() {
        return this.control;
    }

    public byte[] zh() {
        if (this.msgId == null || this.msgId.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.msgId.length];
        System.arraycopy(this.msgId, 0, bArr, 0, this.msgId.length);
        return bArr;
    }

    public byte[] yp() {
        return new byte[0];
    }

    public boolean isValid() {
        boolean z;
        CharSequence pkgName = getPkgName();
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
        if (z && !TextUtils.isEmpty(pkgName) && (r3 ^ 1) == 0 && zg().length != 0) {
            return true;
        }
        b.y("PushLog2976", "token pkgName msgId msgBody exist null");
        return false;
    }

    public PushMessage yw(InputStream inputStream) {
        a ys = ys(inputStream);
        this.msgId = ys.xx(f.fs(ys.xx(1)[0]));
        this.tokenBytes = ys.xx(f.ft(ys.xx(2)));
        this.control = ys.xx(1)[0];
        if (!yt(this.control)) {
            return null;
        }
        b.y("PushLog2976", "control is: " + this.control);
        if ((this.control & 32) == 32) {
            int ft = f.ft(ys.xx(2));
            b.y("PushLog2976", "cookieLen is: " + ft);
            this.mCookie = ys.xx(ft);
        }
        this.payload = yu(this.control, ys);
        return this;
    }

    public int getUserId() {
        if (this.payload == null) {
            b.y("PushLog2976", "payload is null");
            return 0;
        } else if (this.payload.has("userId")) {
            try {
                return this.payload.getInt("userId");
            } catch (JSONException e) {
                b.y("PushLog2976", "fail to get userid from payload");
            }
        } else {
            b.y("PushLog2976", "payload not has userid");
            return 0;
        }
    }

    public String getPkgName() {
        if (this.payload == null) {
            b.y("PushLog2976", "payload is null");
            return null;
        } else if (this.payload.has("pkgName")) {
            try {
                return this.payload.getString("pkgName");
            } catch (JSONException e) {
                b.y("PushLog2976", "fail to get pkgname from payload");
            }
        } else {
            b.y("PushLog2976", "payload not has pkgname");
            return null;
        }
    }

    public byte[] zj() {
        if (this.tokenBytes == null || this.tokenBytes.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[this.tokenBytes.length];
        System.arraycopy(this.tokenBytes, 0, bArr, 0, this.tokenBytes.length);
        return bArr;
    }

    public int zi() {
        if (this.payload == null) {
            b.y("PushLog2976", "payload is null");
            return -1;
        } else if (this.payload.has("msgType")) {
            try {
                return this.payload.getInt("msgType");
            } catch (JSONException e) {
                b.y("PushLog2976", "fail to get msgType from payload");
            }
        } else {
            b.y("PushLog2976", "payload not has msgType");
            return -1;
        }
    }

    public byte[] zg() {
        if (this.payload == null) {
            b.y("PushLog2976", "payload is null");
            return new byte[0];
        }
        if (this.payload.has("msg")) {
            try {
                String string = this.payload.getString("msg");
                if (string != null) {
                    return string.getBytes("UTF-8");
                }
            } catch (JSONException e) {
                b.y("PushLog2976", "fail to get msg from payload");
            } catch (UnsupportedEncodingException e2) {
                b.y("PushLog2976", "fail to get msg from payload, unsupport encoding type");
            }
        } else {
            b.y("PushLog2976", "payload not has msg");
        }
        return new byte[0];
    }

    public String toString() {
        return "payload is" + g.cb(this.payload);
    }
}
