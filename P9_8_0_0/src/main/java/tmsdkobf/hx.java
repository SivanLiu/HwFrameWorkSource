package tmsdkobf;

import android.content.Intent;
import android.telephony.gsm.SmsMessage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.utils.f;
import tmsdk.common.utils.n;

final class hx implements hn {
    private static Intent qS;
    private Intent mIntent;
    private hn qx;

    static final class a implements hn {
        private SmsMessage qT;

        a() {
        }

        public SmsEntity bt() {
            return null;
        }

        public void g(byte[] bArr) {
            try {
                this.qT = SmsMessage.createFromPdu(bArr);
            } catch (Throwable th) {
                this.qT = null;
            }
        }

        public String getAddress() {
            return this.qT == null ? null : this.qT.getOriginatingAddress();
        }

        public String getBody() {
            return this.qT == null ? null : this.qT.getMessageBody();
        }

        public String getServiceCenter() {
            return this.qT == null ? null : this.qT.getServiceCenterAddress();
        }
    }

    static final class b implements hn {
        private static Method qV;
        private static Constructor<android.telephony.SmsMessage> qW;
        private android.telephony.SmsMessage qU;

        b() {
        }

        public SmsEntity bt() {
            return null;
        }

        public void g(byte[] bArr) {
            Object -l_3_R;
            Constructor constructor;
            Object[] objArr;
            try {
                Object -l_2_R = im.rE;
                -l_3_R = null;
                String -l_4_R = null;
                if (-l_2_R != null) {
                    -l_4_R = -l_2_R.f(hx.qS);
                    -l_3_R = -l_2_R.iv();
                }
                if (-l_3_R != null && -l_4_R != null && -l_2_R.iw() && -l_4_R.equals("0")) {
                    if (qV != null) {
                        if (qW != null) {
                            constructor = qW;
                            objArr = new Object[1];
                            objArr[0] = qV.invoke(null, new Object[]{bArr});
                            this.qU = (android.telephony.SmsMessage) constructor.newInstance(objArr);
                            this.qU.getMessageBody();
                            return;
                        }
                    }
                    qV = Class.forName("com.android.internal.telephony.cdma.SmsMessage").getMethod("createFromPdu", new Class[]{byte[].class});
                    qV.setAccessible(true);
                    Object -l_5_R = Class.forName("com.android.internal.telephony.SmsMessageBase");
                    qW = android.telephony.SmsMessage.class.getDeclaredConstructor(new Class[]{-l_5_R});
                    qW.setAccessible(true);
                    constructor = qW;
                    objArr = new Object[1];
                    objArr[0] = qV.invoke(null, new Object[]{bArr});
                    this.qU = (android.telephony.SmsMessage) constructor.newInstance(objArr);
                    this.qU.getMessageBody();
                    return;
                }
                if (!(-l_3_R == null || -l_4_R == null)) {
                    if (-l_2_R.ix()) {
                        int -l_5_I = -l_2_R.cu(-l_4_R);
                        this.qU = android.telephony.SmsMessage.createFromPdu(bArr);
                        if (this.qU != null) {
                            this.qU.getMessageBody();
                        }
                        if (this.qU == null || this.qU.getMessageBody() == null) {
                            try {
                                if (qV != null) {
                                    if (qW != null) {
                                    }
                                }
                                qV = Class.forName("android.telephony.gemini.GeminiSmsMessage").getMethod("createFromPdu", new Class[]{byte[].class, Integer.TYPE});
                                qV.setAccessible(true);
                                this.qU = (android.telephony.SmsMessage) qV.invoke(null, new Object[]{bArr, Integer.valueOf(-l_5_I)});
                                this.qU.getMessageBody();
                            } catch (Exception e) {
                                this.qU = null;
                            }
                        }
                        if (this.qU != null && this.qU.getMessageBody() != null) {
                            return;
                        }
                        if (-l_5_I == 1) {
                            try {
                                if (qV != null) {
                                    if (qW != null) {
                                        return;
                                    }
                                }
                                qV = Class.forName("com.android.internal.telephony.gsm.SmsMessage").getMethod("createFromPdu", new Class[]{byte[].class});
                                qV.setAccessible(true);
                                this.qU = (android.telephony.SmsMessage) qV.invoke(null, new Object[]{bArr});
                                this.qU.getMessageBody();
                                return;
                            } catch (Exception e2) {
                                this.qU = null;
                                return;
                            }
                        }
                        return;
                    }
                }
                this.qU = android.telephony.SmsMessage.createFromPdu(bArr);
                this.qU.getMessageBody();
            } catch (Exception e3) {
                this.qU = null;
            } catch (Throwable th) {
                try {
                    if (qV != null) {
                        if (qW != null) {
                            constructor = qW;
                            objArr = new Object[1];
                            objArr[0] = qV.invoke(null, new Object[]{bArr});
                            this.qU = (android.telephony.SmsMessage) constructor.newInstance(objArr);
                            this.qU.getMessageBody();
                        }
                    }
                    qV = Class.forName("com.android.internal.telephony.gsm.SmsMessage").getMethod("createFromPdu", new Class[]{byte[].class});
                    qV.setAccessible(true);
                    -l_3_R = Class.forName("com.android.internal.telephony.SmsMessageBase");
                    qW = android.telephony.SmsMessage.class.getDeclaredConstructor(new Class[]{-l_3_R});
                    qW.setAccessible(true);
                    constructor = qW;
                    objArr = new Object[1];
                    objArr[0] = qV.invoke(null, new Object[]{bArr});
                    this.qU = (android.telephony.SmsMessage) constructor.newInstance(objArr);
                    this.qU.getMessageBody();
                } catch (Exception e4) {
                    this.qU = null;
                }
            }
        }

        public String getAddress() {
            return this.qU == null ? null : this.qU.getOriginatingAddress();
        }

        public String getBody() {
            return this.qU == null ? null : this.qU.getMessageBody();
        }

        public String getServiceCenter() {
            return this.qU == null ? null : this.qU.getServiceCenterAddress();
        }
    }

    public hx(Intent intent) {
        this.mIntent = new Intent(intent);
        qS = new Intent(intent);
    }

    public SmsEntity bt() {
        Object -l_1_R = this.mIntent.getExtras();
        Object -l_2_R = null;
        if (-l_1_R != null) {
            Object[] -l_2_R2 = (Object[]) ((Object[]) -l_1_R.get("pdus"));
        }
        if (-l_2_R == null || -l_2_R.length == 0) {
            return null;
        }
        Object -l_3_R = new StringBuffer();
        String -l_4_R = null;
        for (int -l_5_I = 0; -l_5_I < -l_2_R.length; -l_5_I++) {
            if (-l_2_R[-l_5_I] != null) {
                g((byte[]) -l_2_R[-l_5_I]);
                if (getBody() == null) {
                    break;
                }
                -l_3_R.append(getBody());
                if (-l_4_R == null) {
                    -l_4_R = getAddress();
                }
            }
        }
        if (-l_4_R == null) {
            return null;
        }
        Object -l_5_R = new SmsEntity();
        -l_5_R.phonenum = -l_4_R;
        -l_5_R.body = -l_3_R.toString();
        -l_5_R.serviceCenter = getServiceCenter();
        -l_5_R.type = 1;
        -l_5_R.protocolType = 0;
        -l_5_R.raw = this.mIntent;
        Object -l_6_R = im.rE;
        if (-l_6_R != null) {
            -l_5_R.fromCard = -l_6_R.f(this.mIntent);
            f.f("DualSim", "SMSParser number:" + -l_5_R.phonenum + " fromcard:" + -l_5_R.fromCard);
        }
        return -l_5_R;
    }

    public void g(byte[] bArr) {
        if (this.qx == null) {
            this.qx = n.iX() <= 3 ? new a() : new b();
        }
        this.qx.g(bArr);
    }

    public String getAddress() {
        return this.qx.getAddress();
    }

    public String getBody() {
        return this.qx.getBody();
    }

    public String getServiceCenter() {
        return this.qx.getServiceCenter();
    }
}
