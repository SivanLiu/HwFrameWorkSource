package tmsdkobf;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import tmsdk.common.TMSDKContext;
import tmsdk.common.utils.b;

public final class ir {
    private static final byte[] rR = new byte[]{(byte) 99, (byte) 111, (byte) 109, (byte) 46, (byte) 116, (byte) 101, (byte) 110, (byte) 99, (byte) 101, (byte) 110, (byte) 116, (byte) 46, (byte) 113, (byte) 113, (byte) 112, (byte) 105, (byte) 109, (byte) 115, (byte) 101, (byte) 99, (byte) 117, (byte) 114, (byte) 101};
    private static volatile ir rS = null;
    public static final String rV = new String(rR);
    private io rT;
    private Calendar rU = Calendar.getInstance();
    private iq rW;

    private ir() {
        load();
    }

    private static long a(String str, RSAPublicKey rSAPublicKey) throws Exception {
        Object -l_2_R = b.decode(str, 0);
        int -l_3_I = ByteBuffer.wrap(-l_2_R).getInt();
        Object -l_4_R = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        -l_4_R.init(2, rSAPublicKey);
        Object -l_5_R = -l_4_R.doFinal(-l_2_R, 4, -l_3_I);
        Object -l_6_R = Cipher.getInstance("DES/ECB/PKCS5Padding");
        -l_6_R.init(2, SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(-l_5_R)));
        return Long.parseLong(new String(-l_6_R.doFinal(-l_2_R, -l_3_I + 4, (-l_2_R.length - 4) - -l_3_I)), 16);
    }

    private byte[] af(int i) {
        InputStream inputStream = null;
        Object -l_3_R;
        try {
            -l_3_R = TMSDKContext.getCurrentContext();
            if (-l_3_R == null) {
                -l_3_R = TMSDKContext.getApplicaionContext();
            }
            Object -l_2_R = -l_3_R.getAssets().open(i != 0 ? "licence" + i + ".conf" : "licence.conf");
            Object -l_6_R = new byte[-l_2_R.available()];
            -l_2_R.read(-l_6_R);
            Object -l_7_R = -l_6_R;
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_8_R) {
                    -l_8_R.printStackTrace();
                }
            }
            return -l_6_R;
        } catch (Object -l_3_R2) {
            throw new RuntimeException(-l_3_R2);
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Object -l_10_R) {
                    -l_10_R.printStackTrace();
                }
            }
        }
    }

    public static synchronized ir bU() {
        ir irVar;
        synchronized (ir.class) {
            if (rS == null) {
                rS = new ir();
            }
            irVar = rS;
        }
        return irVar;
    }

    private final void bW() {
        Object -l_2_R = new md("licence").getString("expiry.enc_seconds", null);
        long -l_3_J = -1;
        if (-l_2_R != null) {
            try {
                -l_3_J = a(-l_2_R, bX());
            } catch (Object -l_6_R) {
                -l_6_R.printStackTrace();
            }
        }
        if (-l_3_J == -1) {
            -l_3_J = this.rW.bT();
        }
        int -l_5_I = (((System.currentTimeMillis() / 1000) > -l_3_J ? 1 : ((System.currentTimeMillis() / 1000) == -l_3_J ? 0 : -1)) < 0 ? 1 : 0) == 0 ? 1 : 0;
        Object -l_6_R2 = Calendar.getInstance();
        -l_6_R2.setTimeInMillis(-l_3_J * 1000);
        mb.d("LicMan", "expirySeconds=" + -l_3_J + "(" + -l_6_R2.get(1) + "-" + -l_6_R2.get(2) + "-" + -l_6_R2.get(5) + ") expired=" + -l_5_I);
        this.rT = new io(-l_5_I);
    }

    private static RSAPublicKey bX() {
        return ip.h(b.decode("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUM5ekNDQWQ4Q0NRRGlsbUFjTWxiczVEQU5C\nZ2txaGtpRzl3MEJBUVVGQURCK01Rc3dDUVlEVlFRR0V3SkQKVGpFTE1Ba0dBMVVFQ0JNQ1IwUXhD\nekFKQmdOVkJBY1RBa2RhTVJJd0VBWURWUVFLRkFsMFpXTUlibU5sYm5ReApDekFKQmdOVkJBc1RB\nak5ITVE0d0RBWURWUVFERXdWdlltRnRZVEVrTUNJR0NTcUdTSWIzRFFFSkFSWVZiMkpoCmJXRjZa\nVzVuUUhSbGJtTmxiblF1WTI5dE1CNFhEVEV4TVRFeE5qRXhNVGN4TjFvWERURXlNREl5TkRFeE1U\nY3gKTjFvd2dZQXhDekFKQmdOVkJBWVRBa05PTVFzd0NRWURWUVFJRXdKSFJERUxNQWtHQTFVRUJ4\nTUNSMW94RURBTwpCZ05WQkFvVEIzUmxibU5sYm5ReEN6QUpCZ05WQkFzVEFqTkhNUkl3RUFZRFZR\nUURFd2x2WW1GdFlYcGxibWN4CkpEQWlCZ2txaGtpRzl3MEJDUUVXRlc5aVlXMWhlbVZ1WjBCMFpX\nNWpaVzUwTG1OdmJUQ0JuekFOQmdrcWhraUcKOXcwQkFRRUZBQU9CalFBd2dZa0NnWUVBd1kvV3FI\nV2NlRERkSm16anI3TlpSeS9qTllwS1NzVzExZngxaTIrQwpxTUE3NTJXb1d1bDZuSTB1MGZkWitk\nUzVUandRNkU0Qm13dXduVTVnQmJYK1VzQ2VHRHZaQVhQc045UEVWYnZTCkcvR25YclQrcTI2VUpP\nNHcrd3VNdmk5YWxkZHhhbkNKeXJ2ZWQ2NUdvMXhXUEErWGNHaVQxMndubjZtUHhyMnUKcVEwQ0F3\nRUFBVEFOQmdrcWhraUc5dzBCQVFVRkFBT0NBUUVBblpzV3FpSmV5SC9sT0prSWN6L2ZidDN3MXFL\nRApGTXJ5cFVHVFN6Z3NONWNaMW9yOGlvVG5ENGRLaDdJN2ttbDRpcGNvMDF0enc2MGhLYUtwNG9G\nMnYrMEs2NGZDCnBEMG9EUlkrOGoyK2RsMmNxeHBsT0FYdDc1RWFKNW40MG1DZDdTN0VBS0d2Z2Na\naVhyV0Z1eUtCL2QvNTh3Qm4KOEFGUVJhTnBySXNOSHpxMkMwL0JXR1pTSnJicmhOWExFY0ZtL0Ru\nTG14ZEVNYWxPSXhnSkhGcEFOS2tadXBzdgo0L0lDVFhSL0RJaURjbXJjbDFkNkc2VmgyaUcwaS9v\nRDBHQnBMZlFPcEF0Vmx6Y2lxZnBsTkphcnpRUTZUVXRyCm5GRmVNVDNDc2t5VGJwYnp1R2dDdUxj\nQVR3cnRQd1BOOWZzQXYrSjRJZm0rZUNVVDVnZlorMSsyNHc9PQotLS0tLUVORCBDRVJUSUZJQ0FU\nRS0tLS0tCg==\n".getBytes(), 0));
    }

    private static byte[] c(byte[] bArr, byte[] bArr2) {
        Object -l_2_R = null;
        try {
            Object -l_5_R = SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(bArr2));
            Object -l_6_R = Cipher.getInstance("DES/ECB/NoPadding");
            -l_6_R.init(2, -l_5_R);
            -l_2_R = -l_6_R.doFinal(bArr);
        } catch (Object -l_3_R) {
            -l_3_R.printStackTrace();
        }
        return -l_2_R;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void load() {
        Object -l_9_R;
        int -l_1_I = 0;
        while (true) {
            try {
                Object -l_2_R = af(-l_1_I);
                if (-l_2_R != null) {
                    Object -l_3_R = bX();
                    Object -l_4_R = new byte[128];
                    System.arraycopy(-l_2_R, 0, -l_4_R, 0, -l_4_R.length);
                    Object -l_5_R = ip.a(-l_4_R, -l_3_R);
                    if (-l_5_R != null) {
                        Object -l_6_R = new byte[(-l_2_R.length - 128)];
                        System.arraycopy(-l_2_R, 128, -l_6_R, 0, -l_6_R.length);
                        Object -l_7_R = c(-l_6_R, -l_5_R);
                        if (-l_7_R != null) {
                            Object -l_8_R = new ByteArrayInputStream(-l_7_R);
                            -l_9_R = new Properties();
                            try {
                                -l_9_R.load(-l_8_R);
                                try {
                                    -l_8_R.close();
                                } catch (Object -l_10_R) {
                                    -l_10_R.printStackTrace();
                                }
                            } catch (Object -l_10_R2) {
                                -l_10_R2.printStackTrace();
                            } catch (Throwable th) {
                                try {
                                    -l_8_R.close();
                                } catch (Object -l_12_R) {
                                    -l_12_R.printStackTrace();
                                }
                            }
                            this.rW = new iq(-l_9_R, TMSDKContext.getApplicaionContext());
                            if (this.rW.bS()) {
                                -l_1_I++;
                            } else {
                                bW();
                                this.rU.setTimeInMillis(System.currentTimeMillis());
                                return;
                            }
                        }
                        return;
                    }
                    throw new RuntimeException("RSA decrypt error.");
                }
                throw new RuntimeException("Certification file is missing! Please contact TMS(Tencent Mobile Secure) group.");
            } catch (RuntimeException e) {
                throw new RuntimeException("loadLicence Invaild signature! Please contact TMS(Tencent Mobile Secure) group.");
            }
        }
        this.rW = new iq(-l_9_R, TMSDKContext.getApplicaionContext());
        if (this.rW.bS()) {
            bW();
            this.rU.setTimeInMillis(System.currentTimeMillis());
            return;
        }
        -l_1_I++;
    }

    public boolean bE() {
        return false;
    }

    public String bQ() {
        return this.rW.bQ();
    }

    public final boolean bS() {
        return this.rW.bS();
    }

    public boolean bV() {
        return true;
    }
}
