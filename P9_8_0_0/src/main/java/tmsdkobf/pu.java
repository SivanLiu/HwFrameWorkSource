package tmsdkobf;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Xml;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.xmlpull.v1.XmlPullParser;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.roach.nest.ActionI;
import tmsdk.common.roach.nest.PowerNest;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.tcc.TccDiff;
import tmsdk.common.utils.i;
import tmsdk.common.utils.k;
import tmsdk.common.utils.q;
import tmsdkobf.oo.a;

public class pu {
    static final int[] KD = new int[]{SmsCheckResult.ESCT_PAY, 103, 104, PowerNest.sNestVersion};
    static final String[] KE = new String[]{"00B1208638DE0FCD3E920886D658DAF6", "7CC749CFC0FB5677E6ABA342EDBDBA5A"};
    static pu KL = null;
    HashMap<px, Class<?>> KF = new HashMap();
    HashMap<px, ActionI> KG = new HashMap();
    HandlerThread KH = null;
    Handler KI = null;
    a KJ = null;
    private pw KK = null;

    private pu() {
        ps.g("RoachManager-RoachManager-NEST_IDS:[" + Arrays.toString(KD) + "]");
    }

    private void a(pv pvVar) {
        if (pvVar != null && pvVar.KN != null) {
            ia();
            Message.obtain(this.KI, 259, pvVar).sendToTarget();
        }
    }

    private void a(pv pvVar, int i) {
        Object -l_5_R;
        Object -l_20_R;
        ps.g("loadItem:[" + pvVar + "]type:[" + i + "]");
        px -l_3_R = pvVar.KN;
        for (px -l_5_R2 : this.KG.keySet()) {
            if (-l_5_R2.KV == -l_3_R.KV) {
                ps.g("[" + -l_5_R2.KV + "]is running:[" + -l_5_R2.KW + "]new:[" + -l_3_R.KW + "]");
                ActionI -l_6_R = (ActionI) this.KG.get(-l_5_R2);
                ps.g("rEntityRunning:[" + -l_6_R + "]");
                try {
                    if (-l_3_R.KW > -l_5_R2.KW) {
                        ps.g("entity--onStop()/clean()");
                        -l_6_R.onStop();
                        -l_6_R.clean();
                        this.KG.remove(-l_5_R2);
                        this.KF.remove(-l_5_R2);
                        if (i == 1) {
                            break;
                        } else if (i == 2) {
                            return;
                        }
                    } else {
                        if (-l_3_R.KW == -l_5_R2.KW) {
                            if (i == 1) {
                                ps.g("save version, do nothing");
                                return;
                            } else if (i == 2) {
                                ps.g("save version, entity--onStop()/clean()");
                                -l_6_R.onStop();
                                -l_6_R.clean();
                                this.KG.remove(-l_5_R2);
                                this.KF.remove(-l_5_R2);
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                } catch (Object -l_7_R) {
                    if (i == 1) {
                        kt.e(1320042, "0;1017;" + pvVar.ie());
                    } else if (i == 2) {
                        kt.e(1320042, "0;1018;" + pvVar.ie());
                    }
                    ps.h("e:[" + -l_7_R + "]");
                    return;
                }
            }
        }
        for (px -l_5_R22 : this.KF.keySet()) {
            if (-l_5_R22.KV == -l_3_R.KV) {
                ps.g("[" + -l_3_R.KV + "]is loaded:[" + -l_5_R22.KW + "]new:[" + -l_3_R.KW + "]");
                try {
                    Class -l_6_R2;
                    ActionI -l_8_R;
                    if (-l_3_R.KW > -l_5_R22.KW) {
                        -l_6_R2 = (Class) this.KF.get(-l_5_R22);
                        -l_8_R = (ActionI) -l_6_R2.getConstructor(new Class[0]).newInstance(new Object[0]);
                        ps.g("rEntityClass:[" + -l_6_R2 + "]rEntity:[" + -l_8_R + "]");
                        ps.g("entity--clean()");
                        -l_8_R.clean();
                        if (i == 1) {
                            break;
                        } else if (i == 2) {
                            return;
                        }
                    } else {
                        if (-l_5_R22.KW == -l_3_R.KW) {
                            -l_6_R2 = (Class) this.KF.get(-l_5_R22);
                            -l_8_R = (ActionI) -l_6_R2.getConstructor(new Class[0]).newInstance(new Object[0]);
                            ps.g("rEntityClass:[" + -l_6_R2 + "]rEntity:[" + -l_8_R + "]");
                            if (i == 1) {
                                ps.g("entity--onStart()");
                                Bundle -l_9_R = new Bundle();
                                -l_9_R.putString(ActionI.privDirKey, pvVar.if());
                                -l_8_R.onStart(-l_9_R);
                                this.KG.put(-l_5_R22, -l_8_R);
                            } else if (i == 2) {
                                ps.g("entity--clean()");
                                -l_8_R.clean();
                                this.KF.remove(-l_5_R22);
                            }
                            return;
                        }
                        return;
                    }
                } catch (Object -l_6_R3) {
                    if (i == 1) {
                        kt.e(1320042, "0;1017;" + pvVar.ie());
                    } else if (i == 2) {
                        kt.e(1320042, "0;1018;" + pvVar.ie());
                    }
                    ps.h("e:[" + -l_6_R3 + "]");
                    return;
                }
            }
        }
        ZipFile zipFile = null;
        try {
            ps.g("private path:[" + pvVar.if() + "]");
            String -l_5_R3 = pvVar.if() + File.separator + pvVar.ie();
            File file = new File(-l_5_R3);
            if (!file.exists()) {
                ps.g("srcFile:[" + file + "]not exist");
                if (null != null) {
                    try {
                        zipFile.close();
                    } catch (Object -l_7_R2) {
                        ps.h("e :[" + -l_7_R2 + "]");
                    }
                }
            } else if (b(-l_5_R3, -l_5_R3, false)) {
                String -l_7_R3 = "armeabi";
                if (TMSDKContext.is_arm64v8a()) {
                    -l_7_R3 = "arm64-v8a";
                }
                ps.g("lib:[" + -l_7_R3 + "]");
                ZipFile zipFile2 = new ZipFile(file);
                try {
                    Object -l_11_R;
                    Object -l_14_R;
                    Object -l_15_R;
                    Object -l_8_R2 = zipFile2.entries();
                    while (-l_8_R2.hasMoreElements()) {
                        ZipEntry -l_9_R2 = (ZipEntry) -l_8_R2.nextElement();
                        if (!(-l_9_R2 == null || -l_9_R2.isDirectory())) {
                            Object -l_10_R = -l_9_R2.getName();
                            if (!q.cK(-l_10_R) && -l_10_R.contains(-l_7_R3)) {
                                -l_11_R = -l_10_R.substring(-l_10_R.lastIndexOf("/") + 1);
                                int -l_12_I = -l_11_R.lastIndexOf(".");
                                if (-l_12_I > 0) {
                                    -l_11_R = -l_11_R.substring(0, -l_12_I) + ".dat";
                                }
                                Object -l_13_R = pvVar.if() + File.separator + -l_11_R;
                                ps.g("destPath:[" + -l_13_R + "]");
                                -l_14_R = new File(-l_13_R);
                                if (-l_14_R.exists()) {
                                    if (-l_14_R.length() == -l_9_R2.getSize()) {
                                        ps.g("dest file exists and same length, not copy");
                                    } else {
                                        -l_14_R.delete();
                                    }
                                }
                                -l_15_R = new BufferedInputStream(zipFile2.getInputStream(-l_9_R2));
                                Object -l_16_R = new FileOutputStream(-l_14_R);
                                Object -l_17_R = new byte[8192];
                                while (true) {
                                    int -l_18_I = -l_15_R.read(-l_17_R);
                                    if (-l_18_I <= 0) {
                                        break;
                                    }
                                    -l_16_R.write(-l_17_R, 0, -l_18_I);
                                }
                                -l_16_R.flush();
                                try {
                                    -l_15_R.close();
                                } catch (Object -l_19_R) {
                                    ps.h("e :[" + -l_19_R + "]");
                                }
                                try {
                                    -l_16_R.close();
                                } catch (Object -l_19_R2) {
                                    ps.h("e :[" + -l_19_R2 + "]");
                                }
                                ps.g("copy done, destPath:[" + -l_13_R + "]");
                            }
                        }
                    }
                    String -l_9_R3 = file.getAbsolutePath();
                    int -l_10_I = file.getAbsolutePath().lastIndexOf(".");
                    if (-l_10_I > 0) {
                        -l_9_R3 = file.getAbsolutePath().substring(0, -l_10_I);
                    }
                    -l_11_R = new File(-l_9_R3 + new String(new byte[]{(byte) 46, (byte) 97, (byte) 112, (byte) 107}));
                    file.renameTo(-l_11_R);
                    Object -l_12_R = Class.forName(new String(new byte[]{(byte) 100, (byte) 97, (byte) 108, (byte) 118, (byte) 105, (byte) 107, (byte) 46, (byte) 115, (byte) 121, (byte) 115, (byte) 116, (byte) 101, (byte) 109, (byte) 46, (byte) 68, (byte) 101, (byte) 120, (byte) 67, (byte) 108, (byte) 97, (byte) 115, (byte) 115, (byte) 76, (byte) 111, (byte) 97, (byte) 100, (byte) 101, (byte) 114}));
                    -l_14_R = -l_12_R.getConstructor(new Class[]{String.class, String.class, String.class, ClassLoader.class}).newInstance(new Object[]{-l_11_R.getAbsolutePath(), pvVar.ig(), null, TMSDKContext.class.getClassLoader()});
                    -l_15_R = -l_12_R.getMethod(new String(new byte[]{(byte) 108, (byte) 111, (byte) 97, (byte) 100, (byte) 67, (byte) 108, (byte) 97, (byte) 115, (byte) 115}), new Class[]{String.class});
                    lu.bL(pvVar.ig());
                    -l_11_R.renameTo(file);
                    if (!b(-l_5_R3, -l_5_R3, true)) {
                        kt.e(1320038, "0;1015;" + pvVar.ie());
                        a(pvVar, false);
                    }
                    if (-l_15_R != null) {
                        -l_15_R.setAccessible(true);
                        Class -l_16_R2 = (Class) -l_15_R.invoke(-l_14_R, new Object[]{"com.roach.REntity"});
                        ActionI -l_18_R = (ActionI) -l_16_R2.getConstructor(new Class[0]).newInstance(new Object[0]);
                        ps.g("rEntityClass:[" + -l_16_R2 + "]rEntity:[" + -l_18_R + "]");
                        if (i == 1) {
                            ps.g("entity--onStart()");
                            Bundle bundle = new Bundle(pu.class.getClassLoader());
                            bundle.putString(ActionI.privDirKey, pvVar.if());
                            -l_18_R.onStart(bundle);
                            this.KF.put(-l_3_R, -l_16_R2);
                            this.KG.put(-l_3_R, -l_18_R);
                            if (-l_3_R.KY == 2) {
                                a(pvVar, true);
                            }
                        } else if (i == 2) {
                            ps.g("entity--clean()");
                            -l_18_R.clean();
                        }
                    }
                    if (zipFile2 != null) {
                        try {
                            zipFile2.close();
                        } catch (Object -l_5_R4) {
                            ps.h("e :[" + -l_5_R4 + "]");
                        }
                    }
                    if (i == 1) {
                        kt.e(1320042, "1;;" + pvVar.ie());
                    }
                } catch (Throwable th) {
                    -l_20_R = th;
                    zipFile = zipFile2;
                }
            } else {
                kt.e(1320038, "0;1016;" + pvVar.ie());
                a(pvVar, false);
                if (null != null) {
                    try {
                        zipFile.close();
                    } catch (Object -l_7_R22) {
                        ps.h("e :[" + -l_7_R22 + "]");
                    }
                }
            }
        } catch (Throwable th2) {
            -l_5_R4 = th2;
            try {
                ps.h("e :[" + -l_5_R4 + "]");
                if (i == 1) {
                    kt.e(1320042, "0;1017;" + pvVar.ie());
                } else if (i == 2) {
                    kt.e(1320042, "0;1018;" + pvVar.ie());
                }
                a(pvVar, false);
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (Object -l_6_R32) {
                        ps.h("e :[" + -l_6_R32 + "]");
                    }
                }
            } catch (Throwable th3) {
                -l_20_R = th3;
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (Object -l_21_R) {
                        ps.h("e :[" + -l_21_R + "]");
                    }
                }
                throw -l_20_R;
            }
        }
    }

    private void a(pv pvVar, boolean z) {
        if (pvVar != null && pvVar.KN != null) {
            try {
                ps.g("cleanItem-itemClean:[" + pvVar + "]");
                if (z) {
                    a(pvVar, 2);
                }
                lu.bL(pvVar.if());
                lu.bL(pvVar.ig());
                this.KK.bR(pvVar.KN.KV);
                kt.e(1320043, "1;;" + pvVar.ie());
            } catch (Object -l_3_R) {
                kt.e(1320043, "0;1019;" + pvVar.ie());
                ps.h("cleanItem-error:[" + -l_3_R + "]");
            }
        }
    }

    private boolean b(String str, String str2, boolean z) {
        Object -l_8_R;
        Object -l_13_R;
        ps.g("encryptFile-srcPath:[" + str + "]destPath:[" + str2 + "]encrypt:[" + z + "]");
        int -l_4_I = 0;
        BufferedInputStream bufferedInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            long -l_8_J = new File(str).length();
            if ((-l_8_J > 0 ? 1 : null) == null) {
                if (bufferedInputStream != null) {
                    try {
                        bufferedInputStream.close();
                    } catch (Object -l_11_R) {
                        ps.h("e:[" + -l_11_R + "]");
                    }
                }
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (Object -l_11_R2) {
                        ps.h("e:[" + -l_11_R2 + "]");
                    }
                }
                return false;
            }
            byte[] -l_5_R = new byte[((int) -l_8_J)];
            int -l_10_I = 0;
            BufferedInputStream -l_6_R = new BufferedInputStream(new FileInputStream(str));
            try {
                Object -l_12_R = new byte[8192];
                while (true) {
                    int -l_11_I = -l_6_R.read(-l_12_R);
                    if (-l_11_I == -1) {
                        break;
                    }
                    System.arraycopy(-l_12_R, 0, -l_5_R, -l_10_I, -l_11_I);
                    -l_10_I += -l_11_I;
                }
                FileOutputStream -l_7_R = new FileOutputStream(str2);
                if (z) {
                    -l_7_R.write(TccCryptor.encrypt(-l_5_R, null));
                } else {
                    try {
                        -l_7_R.write(TccCryptor.decrypt(-l_5_R, null));
                    } catch (Throwable th) {
                        -l_13_R = th;
                        fileOutputStream = -l_7_R;
                        bufferedInputStream = -l_6_R;
                        if (bufferedInputStream != null) {
                            bufferedInputStream.close();
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                        throw -l_13_R;
                    }
                }
                -l_7_R.flush();
                -l_4_I = 1;
                if (-l_6_R == null) {
                    bufferedInputStream = -l_6_R;
                } else {
                    try {
                        -l_6_R.close();
                    } catch (Object -l_8_R2) {
                        ps.h("e:[" + -l_8_R2 + "]");
                    }
                }
                if (-l_7_R == null) {
                    fileOutputStream = -l_7_R;
                    return -l_4_I;
                }
                try {
                    -l_7_R.close();
                } catch (Object -l_8_R22) {
                    ps.h("e:[" + -l_8_R22 + "]");
                }
                return -l_4_I;
            } catch (Throwable th2) {
                -l_13_R = th2;
                bufferedInputStream = -l_6_R;
            }
        } catch (Throwable th3) {
            -l_8_R22 = th3;
            ps.h("e:[" + -l_8_R22 + "]");
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            return -l_4_I;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean b(pv pvVar) {
        ps.g("checkValidity:[" + pvVar + "]");
        if (pvVar == null || pvVar.KN == null) {
            return false;
        }
        Object -l_2_R = pvVar.if() + File.separator + pvVar.ie();
        Object -l_3_R = new File(-l_2_R);
        if (!-l_3_R.exists()) {
            ps.h(-l_2_R + "[" + -l_2_R + "]not exists");
            kt.e(1320038, "0;1005;" + pvVar.ie());
            return false;
        } else if (-l_3_R.length() != ((long) pvVar.KN.La)) {
            ps.h("file size:[" + -l_3_R.length() + "]config item size:[" + pvVar.KN.La + "]");
            kt.e(1320038, "0;1006;" + pvVar.ie());
            return false;
        } else {
            Object -l_4_R = TccDiff.fileMd5(-l_2_R);
            if (-l_4_R.compareToIgnoreCase(pvVar.KN.Lb) == 0) {
                Object -l_5_R = cr(-l_2_R);
                if (q.cK(-l_5_R)) {
                    kt.e(1320038, "0;1008;" + pvVar.ie());
                    return false;
                }
                int -l_6_I = 0;
                while (-l_6_I < KE.length && -l_5_R.compareToIgnoreCase(KE[-l_6_I]) != 0) {
                    -l_6_I++;
                }
                if (-l_6_I < KE.length) {
                    InputStream inputStream = null;
                    ps.g("parse [info.xml]");
                    AssetManager -l_7_R = (AssetManager) AssetManager.class.newInstance();
                    Method -l_8_R = AssetManager.class.getDeclaredMethod("addAssetPath", new Class[]{String.class});
                    -l_8_R.setAccessible(true);
                    -l_8_R.invoke(-l_7_R, new Object[]{-l_2_R});
                    inputStream = -l_7_R.open("info.xml", 1);
                    XmlPullParser -l_9_R = Xml.newPullParser();
                    -l_9_R.setInput(inputStream, "UTF-8");
                    int -l_10_I = -l_9_R.getEventType();
                    while (-l_10_I != 1) {
                        Object -l_11_R;
                        switch (-l_10_I) {
                            case 2:
                                -l_11_R = -l_9_R.getName();
                                int -l_12_I;
                                if (-l_11_R.compareTo("id") == 0) {
                                    -l_12_I = Integer.valueOf(-l_9_R.nextText()).intValue();
                                    if (-l_12_I != pvVar.KN.KV) {
                                        ps.h("id:[" + -l_12_I + "]config id:[" + pvVar.KN.KV + "]");
                                        kt.e(1320038, "0;1010;" + pvVar.ie());
                                        if (inputStream != null) {
                                            try {
                                                inputStream.close();
                                            } catch (Object -l_14_R) {
                                                ps.h("e:[" + -l_14_R + "]");
                                            }
                                        }
                                        return false;
                                    }
                                } else if (-l_11_R.compareTo("version_plugin") == 0) {
                                    -l_12_I = Integer.valueOf(-l_9_R.nextText()).intValue();
                                    if (-l_12_I != pvVar.KN.KW) {
                                        ps.h("version_r:[" + -l_12_I + "]config version:[" + pvVar.KN.KW + "]");
                                        kt.e(1320038, "0;1011;" + pvVar.ie());
                                        if (inputStream != null) {
                                            try {
                                                inputStream.close();
                                            } catch (Object -l_14_R2) {
                                                ps.h("e:[" + -l_14_R2 + "]");
                                            }
                                        }
                                        return false;
                                    }
                                } else if (-l_11_R.compareTo("version_host") == 0) {
                                    -l_12_I = Integer.valueOf(-l_9_R.nextText()).intValue();
                                    if (-l_12_I != pvVar.KN.KX) {
                                        ps.h("version_nest:[" + -l_12_I + "]config version_nest:[" + pvVar.KN.KX + "]");
                                        kt.e(1320038, "0;1012;" + pvVar.ie());
                                        if (inputStream != null) {
                                            try {
                                                inputStream.close();
                                            } catch (Object -l_14_R22) {
                                                ps.h("e:[" + -l_14_R22 + "]");
                                            }
                                        }
                                        return false;
                                    }
                                } else if (-l_11_R.compareTo("run_type") == 0) {
                                    -l_12_I = Integer.valueOf(-l_9_R.nextText()).intValue();
                                    if (-l_12_I != pvVar.KN.KY) {
                                        ps.h("runtype:[" + -l_12_I + "]config runtype:[" + pvVar.KN.KY + "]");
                                        kt.e(1320038, "0;1013;" + pvVar.ie());
                                        if (inputStream != null) {
                                            try {
                                                inputStream.close();
                                            } catch (Object -l_14_R222) {
                                                ps.h("e:[" + -l_14_R222 + "]");
                                            }
                                        }
                                        return false;
                                    }
                                }
                            case 3:
                                -l_11_R = -l_9_R.getName();
                            default:
                                try {
                                    -l_10_I = -l_9_R.next();
                                } catch (Object -l_7_R2) {
                                    ps.h("e:[" + -l_7_R2 + "]");
                                    kt.e(1320038, "0;1014;" + pvVar.ie());
                                    if (inputStream != null) {
                                        try {
                                            inputStream.close();
                                        } catch (Object -l_9_R2) {
                                            ps.h("e:[" + -l_9_R2 + "]");
                                        }
                                    }
                                    return false;
                                } catch (Throwable th) {
                                    if (inputStream != null) {
                                        try {
                                            inputStream.close();
                                        } catch (Object -l_16_R) {
                                            ps.h("e:[" + -l_16_R + "]");
                                        }
                                    }
                                }
                        }
                        ps.h("e:[" + -l_7_R2 + "]");
                        kt.e(1320038, "0;1014;" + pvVar.ie());
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        return false;
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Object -l_7_R22) {
                            ps.h("e:[" + -l_7_R22 + "]");
                        }
                    }
                    kt.e(1320038, "1;;" + pvVar.ie());
                    return true;
                }
                ps.h("Signature error");
                kt.e(1320038, "0;1009;" + pvVar.ie());
                return false;
            }
            ps.h("file md5:[" + -l_4_R + "]config item md5:[" + pvVar.KN.Lb + "]");
            kt.e(1320038, "0;1007;" + pvVar.ie());
            return false;
        }
    }

    private void c(pv pvVar) {
        Object -l_2_R;
        try {
            ps.g("download:[" + pvVar + "]");
            if (this.KK.bQ(pvVar.KN.KV) == null) {
                ps.g("DB no item");
                a(pvVar, false);
            } else if (pvVar.KN.La > 51200 && !i.K(TMSDKContext.getApplicaionContext())) {
                ps.g("no wifi connected and size:[" + pvVar.KN.La + "]");
            } else {
                -l_2_R = new File(pvVar.if());
                if (!-l_2_R.exists()) {
                    -l_2_R.mkdir();
                }
                pvVar.KP = 2;
                this.KK.d(pvVar);
                Object -l_3_R;
                if (pvVar.KN.La != -199) {
                    int -l_4_I;
                    ps.g("start download:[" + pvVar + "]");
                    -l_3_R = new lx(TMSDKContext.getApplicaionContext());
                    -l_3_R.bP(pvVar.if());
                    -l_3_R.bQ(pvVar.ie());
                    do {
                        -l_4_I = -l_3_R.a(null, pvVar.KN.Lc, false, null);
                    } while (-l_4_I == -7);
                    ps.g("end download:[" + pvVar + "]");
                    if (this.KK.bQ(pvVar.KN.KV) == null) {
                        a(pvVar, false);
                        return;
                    } else if (-l_4_I != 0) {
                        ps.g("download failed:[" + -l_4_I + "]");
                        kt.e(1320036, "0;1004;" + pvVar.ie());
                        pvVar.KP = 1;
                        this.KK.d(pvVar);
                        ic();
                    } else {
                        ps.g("download success");
                        kt.e(1320036, "1;;" + pvVar.ie());
                        if (b(pvVar)) {
                            Object -l_5_R = pvVar.if() + File.separator + pvVar.ie();
                            if (b(-l_5_R, -l_5_R, true)) {
                                pvVar.KP = 3;
                                this.KK.d(pvVar);
                                a(pvVar, 1);
                            } else {
                                ps.h("encOrdecFile false, need clean item:" + pvVar + "]");
                                kt.e(1320038, "0;1015;" + pvVar.ie());
                                a(pvVar, false);
                                return;
                            }
                        }
                        ps.h("checkValidity false, need clean item:" + pvVar + "]");
                        a(pvVar, false);
                        return;
                    }
                }
                lu.q(pvVar.KN.Lc, pvVar.if() + File.separator + pvVar.ie());
                -l_3_R = pvVar.if() + File.separator + pvVar.ie();
                if (b(-l_3_R, -l_3_R, true)) {
                    pvVar.KP = 3;
                    this.KK.d(pvVar);
                    a(pvVar, 1);
                } else {
                    ps.h("encOrdecFile false, need clean item:" + pvVar + "]");
                    kt.e(1320038, "0;1015;" + pvVar.ie());
                    a(pvVar, false);
                }
            }
        } catch (Object -l_2_R2) {
            ps.h("e:[" + -l_2_R2 + "]");
            kt.e(1320036, "0;1004;" + pvVar.ie());
            pvVar.KP = 1;
            this.KK.e(pvVar);
            ic();
        }
    }

    private String cr(String str) {
        Object -l_3_R;
        ps.g("getSignatureMd5-apkPath:[" + str + "]");
        Object -l_2_R = "";
        try {
            -l_3_R = k.bW(str);
            PackageParser -l_4_R = (PackageParser) -l_3_R;
            Object -l_5_R = new DisplayMetrics();
            -l_5_R.setToDefaults();
            Package -l_7_R = (Package) k.a(-l_3_R, new File(str), str, -l_5_R, 0);
            PackageParser.class.getMethod("collectCertificates", new Class[]{-l_7_R.getClass(), Integer.TYPE}).invoke(-l_4_R, new Object[]{-l_7_R, Integer.valueOf(0)});
            if (-l_7_R.mSignatures != null) {
                Object -l_11_R = -l_7_R.mSignatures[0];
                if (-l_11_R != null) {
                    -l_2_R = TccDiff.getByteMd5(((X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(-l_11_R.toByteArray()))).getEncoded());
                    -l_2_R = -l_2_R == null ? null : -l_2_R.toUpperCase();
                }
            }
        } catch (Object -l_3_R2) {
            ps.h("e:[" + -l_3_R2 + "]");
        }
        ps.g("SignatureMd5:[" + -l_2_R + "]");
        return -l_2_R;
    }

    public static synchronized pu hW() {
        pu puVar;
        synchronized (pu.class) {
            Object -l_0_R = pu.class;
            synchronized (pu.class) {
                if (KL == null) {
                    Object -l_1_R = pu.class;
                    synchronized (pu.class) {
                        if (KL == null) {
                            ps.g("RoachManager-getInstance");
                            KL = new pu();
                        }
                    }
                }
                puVar = KL;
            }
        }
        return puVar;
    }

    private void ia() {
        if (this.KK == null) {
            this.KK = pw.ih();
        }
        if (this.KH == null) {
            ps.g("startMainJobScheduler");
            this.KH = im.bJ().newFreeHandlerThread("m_d");
            this.KH.start();
            this.KI = new Handler(this, this.KH.getLooper()) {
                final /* synthetic */ pu KM;

                public void handleMessage(Message message) {
                    if (message.what == 259) {
                        pv -l_2_R = (pv) message.obj;
                        ps.g("MSG_CONFIG_ITEM_ARRIVE aConfigItem:[" + -l_2_R + "]");
                        if (-l_2_R == null || -l_2_R.KN == null) {
                            ps.h("MSG_CONFIG_ITEM_ARRIVE item no base info");
                            return;
                        }
                        pv -l_3_R = this.KM.KK.bQ(-l_2_R.KN.KV);
                        if (-l_3_R == null) {
                            ps.g("item not exist");
                            if (-l_2_R.KO == 1) {
                                ps.g("op_add config item");
                                this.KM.KK.e(-l_2_R);
                            } else if (-l_2_R.KO == 2) {
                                ps.g("op_del config item");
                                this.KM.a(-l_2_R, true);
                            }
                        } else {
                            ps.g("item exist, config version:[" + -l_2_R.KN.KW + "]local version:[" + -l_3_R.KN.KW + "]state:[" + -l_3_R.KP + "]");
                            if (-l_2_R.KN.KW >= -l_3_R.KN.KW) {
                                if (-l_2_R.KO == 1) {
                                    ps.g("op_add config item");
                                    if (-l_3_R.KP != 3) {
                                        if (-l_2_R.KN.KW > -l_3_R.KN.KW) {
                                            ps.g("has new version and not local state, update item");
                                            this.KM.KK.d(-l_2_R);
                                        }
                                    } else if (-l_2_R.KN.KW != -l_3_R.KN.KW) {
                                        ps.g("has new version, clean old item and download new version");
                                        this.KM.a(-l_3_R, true);
                                        this.KM.KK.e(-l_2_R);
                                    } else {
                                        ps.g("same version, try load");
                                        this.KM.a(-l_3_R, 1);
                                    }
                                } else if (-l_2_R.KO == 2) {
                                    ps.g("op_del config item");
                                    this.KM.a(-l_3_R, true);
                                }
                            }
                        }
                        this.KM.KI.removeMessages(260);
                        this.KM.KI.sendEmptyMessage(260);
                        this.KM.KI.removeMessages(258);
                        this.KM.KI.sendEmptyMessage(258);
                        this.KM.KI.removeMessages(262);
                        this.KM.KI.sendEmptyMessage(262);
                    } else if (message.what == 257) {
                        ps.g("MSG_SCAN_AUTO_RUN_ITEMS");
                        -l_2_R = this.KM.KK.il();
                        if (-l_2_R != null && -l_2_R.size() > 0) {
                            ps.g("auto run item size:[" + -l_2_R.size() + "]");
                            for (pv -l_4_R : -l_2_R) {
                                if (-l_4_R.KP == 3 && -l_4_R.KO == 1 && -l_4_R.KN != null) {
                                    if ((-l_4_R.KN.KZ >= System.currentTimeMillis()) && new File(-l_4_R.if() + File.separator + -l_4_R.ie()).exists()) {
                                        this.KM.a(-l_4_R, 1);
                                    }
                                }
                            }
                        }
                    } else if (message.what == 258) {
                        ps.g("MSG_DOWNLOAD_ITEMS");
                        -l_2_R = this.KM.KK.ik();
                        if (-l_2_R != null) {
                            for (pv -l_4_R2 : -l_2_R) {
                                this.KM.c(-l_4_R2);
                            }
                            if (this.KM.KK.ik() == null) {
                                this.KM.id();
                            } else {
                                this.KM.ic();
                            }
                        }
                    } else if (message.what == 260) {
                        ps.g("MSG_SCAN_CLEAN_ITEMS");
                        -l_2_R = this.KM.KK.ij();
                        if (-l_2_R != null) {
                            for (pv -l_4_R22 : -l_2_R) {
                                this.KM.a(-l_4_R22, true);
                            }
                        }
                    } else if (message.what == 261) {
                        ps.g("MSG_RELEASE_ITEM");
                        ActionI -l_2_R2 = (ActionI) message.obj;
                        if (-l_2_R2 != null) {
                            try {
                                for (px -l_4_R3 : this.KM.KG.keySet()) {
                                    if (((ActionI) this.KM.KG.get(-l_4_R3)) == -l_2_R2) {
                                        ps.g("releaseItem[" + -l_2_R2 + "]");
                                        this.KM.KG.remove(-l_4_R3);
                                        break;
                                    }
                                }
                            } catch (Object -l_3_R2) {
                                ps.h("releaseItem-error:[" + -l_3_R2 + "]");
                            }
                        }
                    } else if (message.what == 262) {
                        this.KM.ib();
                    }
                }
            };
        }
    }

    private void ib() {
        if (this.KH != null) {
            if (this.KI.hasMessages(257) || this.KI.hasMessages(258) || this.KI.hasMessages(259) || this.KI.hasMessages(260) || this.KI.hasMessages(261)) {
                ps.g("stopMainJobScheduler, but has messages, not stop");
                this.KI.removeMessages(262);
                this.KI.sendEmptyMessage(262);
                return;
            }
            if (this.KK.getCount() <= 0) {
                gf.S().l(Boolean.valueOf(false));
            }
            ps.g("stopMainJobScheduler");
            this.KH.quit();
            this.KH = null;
            this.KI = null;
        }
        if (this.KK != null) {
            this.KK.ii();
            this.KK = null;
        }
    }

    private void ic() {
        if (this.KJ == null) {
            ps.g("addNetworkChange");
            this.KJ = new a(this) {
                final /* synthetic */ pu KM;

                {
                    this.KM = r1;
                }

                public void dC() {
                    this.KM.hZ();
                }

                public void dD() {
                }
            };
            oo.A(TMSDKContext.getApplicaionContext()).a(this.KJ);
        }
    }

    private void id() {
        if (this.KJ != null) {
            ps.g("removeNetworkChange");
            oo.A(TMSDKContext.getApplicaionContext()).b(this.KJ);
            this.KJ = null;
        }
    }

    public synchronized void a(ActionI actionI) {
        ia();
        Message.obtain(this.KI, 261, actionI).sendToTarget();
        this.KI.sendEmptyMessage(262);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void b(ju.a aVar) {
        ps.g("onRecvPush-[" + aVar + "]");
        pv -l_2_R = new pv();
        -l_2_R.KN = new px();
        try {
            if (hX() == aVar.tA.Y) {
                t -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                long -l_4_J = ((long) aVar.tA.ag.R) * 1000;
                if ((-l_4_J >= System.currentTimeMillis() ? 1 : null) == null) {
                    ps.h("config item expired");
                    return;
                }
                if (!TMSDKContext.is_armeabi()) {
                    if (!TMSDKContext.is_arm64v8a()) {
                        ps.h("[not armeabi\\arm64-v8a]");
                        kt.e(1320035, "0;1002;" + -l_2_R.ie());
                        return;
                    }
                }
                Object -l_8_R = Calendar.getInstance();
                -l_8_R.setTimeInMillis(-l_4_J);
                ps.g("validEndTime[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(-l_8_R.getTime()) + "]");
                -l_2_R.KO = com.tencent.tcuser.util.a.av((String) -l_3_R.ar.get(0));
                -l_2_R.KN.KV = com.tencent.tcuser.util.a.av((String) -l_3_R.ar.get(1));
                -l_2_R.KN.KW = com.tencent.tcuser.util.a.av((String) -l_3_R.ar.get(2));
                -l_2_R.KN.KX = com.tencent.tcuser.util.a.av((String) -l_3_R.ar.get(3));
                -l_2_R.KN.KY = com.tencent.tcuser.util.a.av((String) -l_3_R.ar.get(4));
                -l_2_R.KN.La = com.tencent.tcuser.util.a.av((String) -l_3_R.ar.get(5));
                -l_2_R.KN.Lb = (String) -l_3_R.ar.get(6);
                -l_2_R.KN.Lc = (String) -l_3_R.ar.get(7);
                -l_2_R.KN.KZ = -l_4_J;
                -l_2_R.KP = 1;
                if (-l_2_R.KO != 1) {
                    if (-l_2_R.KO != 2) {
                        ps.h("config item op error:[" + -l_2_R.KO + "]");
                        kt.e(1320035, "0;1000;" + -l_2_R.ie());
                        return;
                    }
                }
                ps.g("push config item:[" + -l_2_R + "]");
                int -l_10_I = 0;
                while (-l_10_I < KD.length) {
                    if (-l_2_R.KN.KX == KD[-l_10_I]) {
                        break;
                    }
                    -l_10_I++;
                }
                if (-l_10_I < KD.length) {
                    kt.e(1320035, "1;;" + -l_2_R.ie());
                    gf.S().l(Boolean.valueOf(true));
                    a(-l_2_R);
                } else {
                    ps.h("current nest not support, roach nest id:[" + -l_2_R.KN.KX + "]");
                    kt.e(1320035, "0;1001;" + -l_2_R.ie());
                }
            }
        } catch (Object -l_3_R2) {
            kt.e(1320035, "0;1003;" + -l_2_R.ie());
            ps.h("e:[" + -l_3_R2 + "]");
        }
    }

    public int hX() {
        return 519;
    }

    public synchronized void hY() {
        ia();
        this.KI.removeMessages(260);
        this.KI.sendEmptyMessage(260);
        this.KI.removeMessages(257);
        this.KI.sendEmptyMessage(257);
        this.KI.removeMessages(258);
        this.KI.sendEmptyMessage(258);
        this.KI.removeMessages(262);
        this.KI.sendEmptyMessage(262);
    }

    public synchronized void hZ() {
        ps.g("onNetworkConnected");
        if (this.KK == null) {
            this.KK = pw.ih();
        }
        if (this.KK.ik() != null) {
            ia();
            this.KI.removeMessages(258);
            this.KI.sendEmptyMessage(258);
            this.KI.removeMessages(262);
            this.KI.sendEmptyMessage(262);
            return;
        }
        id();
    }
}
