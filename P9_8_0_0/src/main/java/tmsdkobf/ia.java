package tmsdkobf;

import android.content.Intent;
import android.text.TextUtils;
import java.io.ByteArrayInputStream;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.utils.n;

final class ia implements hn {
    private final String SUBJECT = "01";
    private Intent mIntent;
    private final String rs = "0c";
    private final String rt = "03";
    private final String ru = "00";
    private a rv;

    static final class a {
        String subject;
        String url;

        a() {
        }

        public String getAddress() {
            return this.url;
        }

        public String getBody() {
            return this.subject + this.url;
        }

        public String getServiceCenter() {
            return null;
        }
    }

    public ia(Intent intent) {
        this.mIntent = new Intent(intent);
    }

    private boolean a(ByteArrayInputStream byteArrayInputStream) {
        byteArrayInputStream.reset();
        while (byteArrayInputStream.available() > 0) {
            Object -l_2_R = f(byteArrayInputStream);
            if ("0c".equals(-l_2_R)) {
                this.rv.url = "http://" + new String(aF(e(byteArrayInputStream)));
            } else if ("01".equals(-l_2_R)) {
                this.rv.subject = new String(aF(e(byteArrayInputStream)));
            }
        }
        return TextUtils.isEmpty(this.rv.subject);
    }

    private byte[] aF(String str) {
        if (str == null) {
            return null;
        }
        int -l_3_I = str.length();
        Object -l_2_R = new byte[(-l_3_I / 2)];
        for (int -l_4_I = 0; -l_4_I < -l_3_I; -l_4_I += 2) {
            -l_2_R[-l_4_I / 2] = (byte) ((byte) ((hexCharToInt(str.charAt(-l_4_I)) << 4) | hexCharToInt(str.charAt(-l_4_I + 1))));
        }
        return -l_2_R;
    }

    private boolean b(ByteArrayInputStream byteArrayInputStream) {
        byteArrayInputStream.reset();
        while (byteArrayInputStream.available() > 0) {
            this.rv.subject = new String(aF(e(byteArrayInputStream)));
        }
        return TextUtils.isEmpty(this.rv.subject);
    }

    private boolean c(ByteArrayInputStream byteArrayInputStream) {
        Object -l_2_R = new StringBuilder();
        Object -l_3_R = "";
        byteArrayInputStream.reset();
        while (byteArrayInputStream.available() > 0) {
            Object -l_4_R = f(byteArrayInputStream);
            if (-l_4_R.equals("03")) {
                Object -l_5_R = new String(aF(d(byteArrayInputStream)));
                -l_2_R.append(-l_5_R);
                if (-l_3_R.equals("0c")) {
                    this.rv.url = "http://" + -l_5_R;
                } else if (-l_3_R.equals("01")) {
                    this.rv.subject = -l_5_R;
                }
            } else {
                -l_3_R = -l_4_R;
            }
        }
        if (TextUtils.isEmpty(this.rv.subject)) {
            this.rv.subject = -l_2_R.toString();
        }
        return TextUtils.isEmpty(this.rv.subject);
    }

    private String d(ByteArrayInputStream byteArrayInputStream) {
        if (byteArrayInputStream == null) {
            return null;
        }
        Object -l_2_R = new StringBuilder();
        while (byteArrayInputStream.available() > 0) {
            Object -l_3_R = f(byteArrayInputStream);
            if (-l_3_R.equals("00")) {
                break;
            }
            -l_2_R.append(-l_3_R);
        }
        return -l_2_R.toString();
    }

    private String e(ByteArrayInputStream byteArrayInputStream) {
        if (byteArrayInputStream == null) {
            return null;
        }
        Object -l_2_R = new StringBuilder();
        while (!f(byteArrayInputStream).equals("03")) {
            if (byteArrayInputStream.available() <= 0) {
                break;
            }
        }
        while (true) {
            Object -l_3_R = f(byteArrayInputStream);
            if (!-l_3_R.equals("00") && byteArrayInputStream.available() > 0) {
                -l_2_R.append(-l_3_R);
            }
        }
        return -l_2_R.toString();
    }

    private String f(ByteArrayInputStream byteArrayInputStream) {
        if (byteArrayInputStream == null) {
            return null;
        }
        int -l_2_I = byteArrayInputStream.read();
        Object -l_4_R = new StringBuilder(2);
        -l_4_R.append("0123456789abcdef".charAt((-l_2_I >> 4) & 15));
        -l_4_R.append("0123456789abcdef".charAt(-l_2_I & 15));
        return -l_4_R.toString().toLowerCase();
    }

    private int hexCharToInt(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 65) + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 97) + 10;
        }
        throw new RuntimeException("invalid hex char '" + c + "'");
    }

    public SmsEntity bt() {
        Object -l_1_R = this.mIntent.getByteArrayExtra("data");
        Object -l_2_R = null;
        if (-l_1_R != null && n.iX() > 3) {
            g(-l_1_R);
            -l_2_R = new SmsEntity();
            -l_2_R.phonenum = getAddress();
            -l_2_R.body = getBody();
            -l_2_R.serviceCenter = getServiceCenter();
            -l_2_R.type = 1;
            -l_2_R.protocolType = 2;
            -l_2_R.raw = this.mIntent;
            Object -l_3_R = im.rE;
            if (-l_3_R != null) {
                -l_2_R.fromCard = -l_3_R.f(this.mIntent);
            }
        }
        return -l_2_R;
    }

    public void g(byte[] bArr) {
        Object -l_2_R = new ByteArrayInputStream(bArr);
        this.rv = new a();
        if (!a(-l_2_R) && !b(-l_2_R) && !c(-l_2_R)) {
            this.rv = null;
        }
    }

    public String getAddress() {
        return this.rv == null ? null : this.rv.getAddress();
    }

    public String getBody() {
        return this.rv == null ? null : this.rv.getBody();
    }

    public String getServiceCenter() {
        return this.rv == null ? null : this.rv.getServiceCenter();
    }
}
