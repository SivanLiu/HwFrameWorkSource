package com.huawei.android.pushagent.datatype.tcp;

import com.huawei.android.pushagent.datatype.b.a;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.c.g;
import com.huawei.android.pushagent.utils.f;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.json.JSONObject;

public class DecoupledPushMessage extends PushMessage {
    private static final long serialVersionUID = -1186347017264161627L;
    private JSONObject payload = new JSONObject();

    public DecoupledPushMessage(byte b) {
        super(b);
    }

    public byte[] yp() {
        if (this.payload == null) {
            b.y("PushLog2976", "encode error, payload is null");
            return new byte[0];
        }
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byteArrayOutputStream.write(yq());
            byte[] bytes = this.payload.toString().getBytes("UTF-8");
            byteArrayOutputStream.write(f.fr(bytes.length + 6));
            byteArrayOutputStream.write(2);
            byteArrayOutputStream.write(f.fr(bytes.length));
            byteArrayOutputStream.write(bytes);
            return byteArrayOutputStream.toByteArray();
        } catch (UnsupportedEncodingException e) {
            b.y("PushLog2976", "unsupported encoding type");
            return new byte[0];
        } catch (IOException e2) {
            b.y("PushLog2976", "io exception");
            return new byte[0];
        }
    }

    public PushMessage yw(InputStream inputStream) {
        a ys = ys(inputStream);
        byte b = ys.xx(1)[0];
        if (!yt(b)) {
            return null;
        }
        this.payload = yu(b, ys);
        zb(this.payload);
        return this;
    }

    private void zb(JSONObject jSONObject) {
        b.z("PushLog2976", "parse decoupled msg payload.");
        if (jSONObject != null && jSONObject.has("cmdid")) {
            try {
                this.mSubCmdId = (byte) jSONObject.getInt("cmdid");
            } catch (Exception e) {
                b.y("PushLog2976", "parse decoupled msg payload exception");
            }
        }
    }

    public String toString() {
        return "payload is" + g.cb(this.payload);
    }

    public JSONObject za() {
        return this.payload;
    }

    public void zc(JSONObject jSONObject) {
        this.payload = jSONObject;
    }
}
