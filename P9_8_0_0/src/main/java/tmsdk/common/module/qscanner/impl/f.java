package tmsdk.common.module.qscanner.impl;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.lang.MultiLangManager;
import tmsdk.common.module.qscanner.QScanConfig;
import tmsdk.common.module.qscanner.QScanListener;
import tmsdk.common.module.qscanner.QScanResultEntity;
import tmsdk.common.module.qscanner.QScanResultPluginEntity;
import tmsdk.common.module.qscanner.QScannerManagerV2;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.tcc.QFile;
import tmsdk.common.tcc.QSdcardScanner;
import tmsdk.common.tcc.QSdcardScanner.ProgressListener;
import tmsdk.common.tcc.SdcardScannerFactory;
import tmsdk.common.utils.q;
import tmsdk.common.utils.r;
import tmsdk.common.utils.s;
import tmsdk.fg.creator.BaseManagerF;
import tmsdkobf.df;
import tmsdkobf.dg;
import tmsdkobf.dh;
import tmsdkobf.dj;
import tmsdkobf.dk;
import tmsdkobf.dl;
import tmsdkobf.dm;
import tmsdkobf.dn;
import tmsdkobf.do;
import tmsdkobf.dp;
import tmsdkobf.dz;
import tmsdkobf.ea;
import tmsdkobf.fn;
import tmsdkobf.ic;
import tmsdkobf.im;
import tmsdkobf.jy;
import tmsdkobf.kr;
import tmsdkobf.kt;
import tmsdkobf.lu;
import tmsdkobf.mc;
import tmsdkobf.md;
import tmsdkobf.mk;
import tmsdkobf.oa;
import tmsdkobf.ob;
import tmsdkobf.ov;
import tmsdkobf.ox;
import tmsdkobf.oy;
import tmsdkobf.py.a;
import tmsdkobf.pz;
import tmsdkobf.qa;

public final class f extends BaseManagerF {
    private static final String[] Cz = new String[]{"image", "icon", "photo", "music", "dcim", "weibo"};
    private String CA = "";
    private md Cp;
    private ox Cq;
    private AmScannerV2 Cr;
    private int Cs = 0;
    private byte[] Ct = new byte[0];
    private boolean Cu;
    private Object Cv = new Object();
    private boolean Cw = false;
    private boolean Cx = false;
    private Object Cy = new Object();
    private Context mContext;
    private Object mLock = new Object();
    private boolean mPaused = false;

    private int a(g gVar, AtomicReference<h> atomicReference) {
        if (!AmScannerV2.isSupported()) {
            return QScanConfig.ERR_NATIVE_LOAD;
        }
        int -l_3_I = -999;
        Object -l_4_R;
        try {
            -l_4_R = new fn();
            -l_4_R.B("UTF-8");
            -l_4_R.m();
            -l_4_R.put("reqfc", gVar);
            Object -l_5_R = new AtomicReference();
            -l_3_I = AmScannerV2.getOpcode(-l_4_R.l(), -l_5_R);
            if (-l_3_I == 0) {
                byte[] -l_6_R = (byte[]) -l_5_R.get();
                -l_4_R.k();
                -l_4_R.b(-l_6_R);
                h -l_7_R = (h) -l_4_R.a("rspfc", new h());
                if (-l_7_R == null) {
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "AmScannerV2.getOpcode rspfc == null");
                } else {
                    atomicReference.set(-l_7_R);
                    -l_3_I = 0;
                }
                return -l_3_I;
            }
            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "AmScannerV2.getOpcode ret: " + -l_3_I);
            return -l_3_I;
        } catch (Object -l_4_R2) {
            tmsdk.common.utils.f.b(QScannerManagerV2.LOG_TAG, "AmScannerV2.getOpcode exception: " + -l_4_R2, -l_4_R2);
        }
    }

    private ArrayList<e> a(int i, List<ov> list, QScanListener qScanListener, long j, int i2) {
        int -l_7_I = 0;
        int -l_8_I = 0;
        if ((i & 2) != 0) {
            -l_7_I = 1;
        }
        if ((i & 4) != 0) {
            -l_8_I = 1;
        }
        int -l_9_I = 2;
        if (-l_7_I == 0 && -l_8_I != 0) {
            -l_9_I = 4;
        }
        List -l_10_R = new ArrayList();
        int -l_12_I = 0;
        int -l_13_I = list.size();
        for (ov -l_15_R : list) {
            a(-l_9_I, qScanListener);
            if (b(-l_9_I, qScanListener)) {
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "isCanceled");
                break;
            }
            e -l_11_R = -l_7_I == 0 ? d(-l_15_R) : c(-l_15_R);
            if (-l_11_R != null) {
                -l_10_R.add(-l_11_R);
                if (-l_7_I != 0) {
                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanProgress,scanType:[2]progress:[" + (((-l_12_I + 1) * 100) / -l_13_I) + "][" + -l_11_R.packageName + "][" + -l_11_R.softName + "]");
                    qScanListener.onScanProgress(2, -l_12_I + 1, -l_13_I, a(-l_11_R));
                }
            }
            -l_12_I++;
        }
        if (!(-l_7_I == 0 || -l_8_I == 0 || b(-l_9_I, qScanListener))) {
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanStarted, scanType:[4]");
            qScanListener.onScanStarted(4);
        }
        if (-l_8_I != 0 && -l_10_R.size() > 0) {
            a(-l_10_R, qScanListener, i2, j, null);
        }
        return -l_10_R;
    }

    private QScanResultEntity a(e eVar) {
        Object -l_2_R = new QScanResultEntity();
        -l_2_R.packageName = eVar.packageName;
        -l_2_R.softName = eVar.softName;
        -l_2_R.version = eVar.version;
        -l_2_R.versionCode = eVar.versionCode;
        -l_2_R.path = eVar.path;
        -l_2_R.plugins = new ArrayList();
        -l_2_R.virusName = eVar.name;
        -l_2_R.virusDiscription = eVar.BT;
        -l_2_R.virusUrl = eVar.fA;
        int -l_3_I = 0;
        if (eVar.gS == 0) {
            -l_3_I = 257;
        } else if (eVar.gS != 4) {
            if ((eVar.category & 512) == 0) {
                if (eVar.gS != 0) {
                    if (!eVar.Cg) {
                    }
                }
                if (eVar.gS == 1 && eVar.BU != 0) {
                    -l_3_I = 260;
                } else if (eVar.gS != 0) {
                    -l_3_I = !eVar.Ch ? eVar.official != 2 ? 262 : 258 : 261;
                }
            }
            -l_3_I = 259;
        } else {
            -l_3_I = 263;
        }
        if (eVar.plugins != null) {
            Object -l_5_R = eVar.plugins.iterator();
            while (-l_5_R.hasNext()) {
                b -l_6_R = (b) -l_5_R.next();
                Object -l_4_R = new QScanResultPluginEntity();
                -l_4_R.type = -l_6_R.type;
                -l_4_R.banUrls = -l_6_R.banUrls;
                -l_4_R.banIps = -l_6_R.banIps;
                -l_4_R.name = -l_6_R.name;
                -l_2_R.plugins.add(-l_4_R);
            }
        }
        -l_2_R.scanResult = -l_3_I;
        return -l_2_R;
    }

    private void a(int i, List<e> list, Map<Integer, dh> map, List<dk> list2) {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "collectFeatureCheckInfo");
        long -l_5_J = System.currentTimeMillis();
        int -l_7_I = list.size();
        for (Entry -l_9_R : map.entrySet()) {
            int -l_10_I = ((Integer) -l_9_R.getKey()).intValue();
            dh -l_11_R = (dh) -l_9_R.getValue();
            if (-l_11_R == null || -l_11_R.gE == null || -l_11_R.gE.size() == 0) {
                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "invalid: featureParam is null or empty: " + -l_11_R);
            } else if (-l_10_I < -l_7_I) {
                e -l_12_R = (e) list.get(-l_10_I);
                g -l_13_R = new g();
                -l_13_R.path = -l_12_R.path;
                -l_13_R.gE = -l_11_R.gE;
                -l_13_R.gF = -l_11_R.gF;
                -l_13_R.gG = -l_11_R.gG;
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "path: " + -l_13_R.path + " mapParam: " + -l_13_R.gE + " fileSimhashMinCnt: " + -l_11_R.gF + " fileSimhashMaxCnt: " + -l_11_R.gG);
                AtomicReference -l_14_R = new AtomicReference();
                int -l_15_I = a(-l_13_R, -l_14_R);
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "nativeGetOpCode:[" + -l_15_I + "]");
                if (-l_15_I == 0) {
                    h -l_16_R = (h) -l_14_R.get();
                    if (-l_16_R != null) {
                        Object -l_17_R = new dk();
                        Object -l_18_R = c.a(-l_12_R, -l_10_I);
                        -l_17_R.gI = -l_18_R.gI;
                        -l_17_R.gJ = -l_18_R.gJ;
                        -l_17_R.gK = -l_18_R.gK;
                        -l_17_R.gL = -l_18_R.gL;
                        -l_17_R.gM = -l_18_R.gM;
                        -l_17_R.hs = -l_16_R.hs;
                        -l_17_R.ht = -l_16_R.ht;
                        -l_17_R.eB = -l_16_R.eB;
                        list2.add(-l_17_R);
                    }
                }
            } else {
                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "invalid: seq >= nativeCount: " + -l_10_I + " " + -l_7_I);
            }
        }
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[end]collectFeatureCheckInfo, time(millis) elapsed:[" + (System.currentTimeMillis() - -l_5_J) + "]");
    }

    private void a(int i, QScanListener qScanListener) {
        synchronized (this.Cv) {
            try {
                if (this.mPaused) {
                    if (qScanListener != null) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanPaused, scanType:[" + i + "]");
                        qScanListener.onScanPaused(i);
                    }
                    this.Cv.wait();
                    if (qScanListener != null) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanContinue, scanType:[" + i + "]");
                        qScanListener.onScanContinue(i);
                    }
                    this.mPaused = false;
                }
            } catch (Object -l_4_R) {
                tmsdk.common.utils.f.b(QScannerManagerV2.LOG_TAG, "isPaused(): " + -l_4_R.getMessage(), -l_4_R);
            }
        }
    }

    private void a(List<e> list, QScanListener qScanListener, int i, long j, byte[] bArr) {
        if (list != null && list.size() > 0) {
            kt.saveActionData(29953);
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "doCloudScanSync, requestType:[" + i + "]timeoutMillis:[" + j + "]size:[" + list.size() + "]");
            if ((j <= 0 ? 1 : null) != null) {
                j = 120000;
            }
            final long -l_7_J = j / 2;
            a(2, qScanListener);
            if (!b(2, qScanListener)) {
                int -l_9_I = p(list);
                final JceStruct -l_10_R = new dg();
                -l_10_R.gy = new dj();
                -l_10_R.gy.hg = 7;
                -l_10_R.gy.language = 1;
                if (((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG()) {
                    -l_10_R.gy.language = 2;
                }
                -l_10_R.gy.hh = i;
                -l_10_R.gy.hi = 3;
                -l_10_R.gy.hj = fp();
                -l_10_R.gy.hk = bArr;
                -l_10_R.gy.hl = 2;
                -l_10_R.gy.hm = (int) (System.currentTimeMillis() / 1000);
                -l_10_R.gy.hn = 0;
                -l_10_R.gy.hp = -l_9_I;
                a((dg) -l_10_R);
                -l_10_R.gC = new ArrayList();
                for (int -l_11_I = 0; -l_11_I < list.size(); -l_11_I++) {
                    Object -l_12_R = c.a((e) list.get(-l_11_I), -l_11_I);
                    if (((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG()) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "ELanguage.ELANG_ENG modify flag");
                        -l_12_R.gn = 1;
                        -l_12_R.gS = 0;
                        -l_12_R.gT = 0;
                    }
                    -l_10_R.gC.add(-l_12_R);
                }
                kr.dz();
                final Object -l_11_R = new Object();
                final ob -l_12_R2 = im.bK();
                final long -l_13_J = System.currentTimeMillis();
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[Shark]Cmd_CSVirusCheck, sendShark, guid:[" + -l_12_R2.b() + "]");
                final QScanListener qScanListener2 = qScanListener;
                final List<e> list2 = list;
                -l_12_R2.a(2016, -l_10_R, new do(), 1, (jy) new jy(this) {
                    final /* synthetic */ f CH;

                    public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[Shark]onFinish-Cmd_CSVirusCheck, elapsed time:[" + (System.currentTimeMillis() - -l_13_J) + "]cmdId:[" + i2 + "]retCode:[" + i3 + "]dataRetCode: " + i4);
                        this.CH.a(2, qScanListener2);
                        if (!this.CH.b(2, qScanListener2)) {
                            if (i3 != 0 || i4 != 0) {
                                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "Cmd_CSVirusCheck-onFinish, fail-retCode:[" + i3 + "]dataRetCode:[" + i4 + "]");
                                if (i3 == 0) {
                                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanError, QScanConstants.SCAN_CLOUD-dataRetCode:[" + i4 + "]");
                                    qScanListener2.onScanError(-999);
                                } else {
                                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanError, QScanConstants.SCAN_CLOUD-retCode:[" + i3 + "]");
                                    if (i3 % 20 != -4) {
                                        qScanListener2.onScanError(-999);
                                    } else {
                                        qScanListener2.onScanError(-206);
                                    }
                                }
                            } else if (jceStruct == null) {
                                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "Cmd_CSVirusCheck-onFinish, scVirusCheck is null!");
                                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanError, QScanConstants.SCAN_CLOUD-:[-205]");
                                qScanListener2.onScanError(-205);
                            } else {
                                do -l_6_R = (do) jceStruct;
                                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "server handle time(micro seconds): " + -l_6_R.hE);
                                this.CH.bf(-l_6_R.hI);
                                if (-l_6_R.gC == null) {
                                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "scVirusCheck.vecApkInfo is null, maybe because same as local result!");
                                } else {
                                    this.CH.a(list2, -l_6_R);
                                }
                                if (-l_6_R.hH == null || -l_6_R.hH.size() == 0) {
                                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "not need FeatureCheck, scVirusCheck.mapFeatureParam: " + (-l_6_R.hH != null ? "empty" : "null"));
                                } else {
                                    this.CH.a(list2, qScanListener2, -l_10_R.gy, -l_7_J, -l_12_R2, -l_6_R.hH);
                                }
                            }
                        }
                        synchronized (-l_11_R) {
                            -l_11_R.notify();
                        }
                    }
                }, -l_7_J);
                Object -l_15_R = -l_11_R;
                synchronized (-l_11_R) {
                    try {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "doCloudScanSync(), block thread " + Thread.currentThread() + ", waiting for shark callback -->|");
                        -l_11_R.wait();
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "doCloudScanSync(), continue thread: " + Thread.currentThread() + " |-->");
                    } catch (Throwable -l_16_R) {
                        tmsdk.common.utils.f.c(QScannerManagerV2.LOG_TAG, "doCloudScanSync(), SCAN_LOCK.wait(): " + -l_16_R, -l_16_R);
                    }
                }
                return;
            }
            return;
        }
        tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "doCloudScanSync, nativeResults is null or size==0");
    }

    private void a(List<e> list, QScanListener qScanListener, dj djVar, long j, oa oaVar, Map<Integer, dh> map) {
        if (map != null) {
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "doFeatureCheckSync, apk count: " + map.size());
            List -l_8_R = new ArrayList();
            a(djVar.hh, (List) list, (Map) map, -l_8_R);
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "needFeatureCheckList size:[" + -l_8_R.size() + "]");
            if (-l_8_R.size() > 0) {
                final Object -l_9_R = new Object();
                Object -l_10_R = new df();
                -l_10_R.gy = djVar;
                -l_10_R.gz = -l_8_R;
                final long -l_11_J = System.currentTimeMillis();
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[Shark]Cmd_CSFeatureCheck, sendShark, guid:[" + oaVar.b() + "]");
                final QScanListener qScanListener2 = qScanListener;
                final List<e> list2 = list;
                oaVar.a(2019, -l_10_R, new dn(), 1, new jy(this) {
                    final /* synthetic */ f CH;

                    public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[Shark]onFinish-Cmd_CSFeatureCheck, elapsed time:[" + (System.currentTimeMillis() - -l_11_J) + "]cmdId:[" + i2 + "]retCode:[" + i3 + "]dataRetCode: " + i4);
                        this.CH.a(2, qScanListener2);
                        if (!this.CH.b(2, qScanListener2)) {
                            if (i3 != 0 || i4 != 0) {
                                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "onFinish-Cmd_CSFeatureCheck fail" + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4);
                            } else if (jceStruct == null) {
                                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "scFeatureCheck is null!");
                            } else {
                                dn -l_6_R = (dn) jceStruct;
                                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "server handle time(micro seconds): " + -l_6_R.hE);
                                if (-l_6_R.gz == null) {
                                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "scFeatureCheck.vecFeatureInfo is null!");
                                } else {
                                    this.CH.a(list2, -l_6_R);
                                }
                            }
                        }
                        synchronized (-l_9_R) {
                            -l_9_R.notify();
                        }
                    }
                }, j);
                Object -l_13_R = -l_9_R;
                synchronized (-l_9_R) {
                    try {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "doFeatureCheckSync(), block thread " + Thread.currentThread() + ", waiting for shark callback -->|");
                        -l_9_R.wait();
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "doFeatureCheckSync(), continue thread: " + Thread.currentThread() + " |-->");
                    } catch (Throwable -l_14_R) {
                        tmsdk.common.utils.f.c(QScannerManagerV2.LOG_TAG, "doFeatureCheckSync(), SCAN_LOCK.wait(): " + -l_14_R, -l_14_R);
                    }
                }
            }
        }
    }

    private void a(List<e> list, dn dnVar) {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "correctNativeResultsByFeatureCheck");
        if (list == null || dnVar == null) {
            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "nativeResults == null || resp == null");
            return;
        }
        q(list);
        Object<dm> -l_3_R = dnVar.gz;
        Object -l_4_R = dnVar.hC;
        Map -l_5_R = dnVar.hD;
        if (-l_3_R == null || -l_3_R.size() == 0) {
            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "rspFeatureInfoList: " + (-l_3_R != null ? "empty" : "null"));
            return;
        }
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "rspFeatureInfoList size:[" + -l_3_R.size() + "]");
        int -l_6_I = list.size();
        for (dm -l_8_R : -l_3_R) {
            if (-l_8_R != null) {
                if (-l_8_R.hv < -l_6_I) {
                    e -l_9_R = (e) list.get(-l_8_R.hv);
                    -l_9_R.gS = -l_8_R.gS;
                    -l_9_R.BU = -l_8_R.gT;
                    -l_9_R.category = -l_8_R.gU;
                    -l_9_R.plugins = c.a(-l_8_R.gV, -l_5_R);
                    -l_9_R.dp = -l_8_R.gY;
                    if (-l_8_R.gT > 0) {
                        if (-l_4_R == null) {
                            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "mapVirusInfo is null for virusId: " + -l_8_R.gT);
                        } else {
                            dp -l_10_R = (dp) -l_4_R.get(Integer.valueOf(-l_8_R.gT));
                            if (-l_10_R == null) {
                                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "cannot find VirusInfo for virusId: " + -l_8_R.gT);
                            } else {
                                -l_9_R.label = -l_10_R.gv;
                                -l_9_R.name = -l_10_R.gv;
                                -l_9_R.BT = -l_10_R.hK;
                                -l_9_R.fA = -l_10_R.hL;
                            }
                        }
                    }
                    -l_9_R.lL = -l_8_R.hy;
                    -l_9_R.url = -l_8_R.hz;
                    -l_9_R.type = -l_8_R.gn;
                } else {
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "invalid: rspFeatureInfo.nRefSeqNo >= nativeCount: " + -l_8_R.hv + " " + -l_6_I);
                }
            }
        }
        q(list);
    }

    private void a(List<e> list, do doVar) {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "correctNativeResults");
        if (list == null || doVar == null) {
            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "nativeResults == null || resp == null");
            return;
        }
        Object -l_3_R = doVar.gC;
        Object -l_4_R = doVar.hC;
        Map -l_5_R = doVar.hD;
        if (-l_3_R == null || -l_3_R.size() == 0) {
            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "rspApkInfoList: " + (-l_3_R != null ? "empty" : "null"));
            return;
        }
        q(list);
        int -l_6_I = list.size();
        Object -l_7_R = -l_3_R.iterator();
        while (-l_7_R.hasNext()) {
            dl -l_8_R = (dl) -l_7_R.next();
            if (-l_8_R != null) {
                if (-l_8_R.hv < -l_6_I) {
                    e -l_9_R = (e) list.get(-l_8_R.hv);
                    -l_9_R.gS = -l_8_R.gS;
                    -l_9_R.BU = -l_8_R.gT;
                    -l_9_R.category = -l_8_R.gU;
                    -l_9_R.plugins = c.a(-l_8_R.gV, -l_5_R);
                    if ((-l_8_R.gW & 1) == 0) {
                        -l_9_R.Cg = false;
                    } else {
                        -l_9_R.Cg = true;
                    }
                    if ((-l_8_R.gW & 2) == 0) {
                        -l_9_R.Ch = false;
                    } else {
                        -l_9_R.Ch = true;
                    }
                    -l_9_R.Cm = -l_8_R.gX;
                    -l_9_R.dp = -l_8_R.gY;
                    -l_9_R.official = -l_8_R.official;
                    if (-l_8_R.gT <= 0) {
                        -l_9_R.label = -l_8_R.hw;
                        -l_9_R.name = -l_8_R.hw;
                        -l_9_R.BT = -l_8_R.hx;
                    } else if (-l_4_R == null) {
                        tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "mapVirusInfo is null for virusId: " + -l_8_R.gT);
                    } else {
                        dp -l_10_R = (dp) -l_4_R.get(Integer.valueOf(-l_8_R.gT));
                        if (-l_10_R == null) {
                            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "cannot find VirusInfo for virusId: " + -l_8_R.gT);
                        } else {
                            -l_9_R.label = -l_10_R.gv;
                            -l_9_R.name = -l_10_R.gv;
                            -l_9_R.BT = -l_10_R.hK;
                            -l_9_R.fA = -l_10_R.hL;
                        }
                    }
                    -l_9_R.lL = -l_8_R.hy;
                    -l_9_R.url = -l_8_R.hz;
                    -l_9_R.Ck = -l_8_R.hA;
                    -l_9_R.Cl = -l_8_R.hB;
                    -l_9_R.type = -l_8_R.gn;
                } else {
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "invalid: rspApkInfo.nRefSeqNo >= nativeCount: " + -l_8_R.hv + " " + -l_6_I);
                }
            }
        }
        q(list);
    }

    private void a(dg dgVar) {
        Object -l_2_R = fq();
        if (!-l_2_R.exists()) {
            lu.b(this.mContext, UpdateConfig.WHITELIST_CLOUDSCAN_NAME, null);
        }
        dgVar.gy.ho = f(-l_2_R);
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "whitelist file Md5:[" + dgVar.gy.ho + "]");
    }

    private void a(ov ovVar, d dVar, e eVar) {
        Object -l_4_R;
        try {
            -l_4_R = new ArrayList();
            if (!(dVar == null || dVar.BS == null || TextUtils.isEmpty(dVar.BS.bZ))) {
                -l_4_R = c.ca(dVar.BS.bZ);
                eVar.Cb = dVar.BS.bZ;
            }
            ArrayList -l_5_R;
            if (-l_4_R.size() > 0) {
                if (-l_4_R.size() > 1) {
                    if (ovVar.hC()) {
                        tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "more than one cert file, ignore for uninstalled apk");
                    } else {
                        -l_5_R = (ArrayList) oy.h(ovVar.getPackageName(), 10);
                        if (-l_5_R.size() > 0) {
                            eVar.bZ = (String) -l_5_R.get(0);
                        }
                        tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "more than one cert file, get by java api, certs: " + -l_5_R);
                        tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "more than one cert file, get by java api, main cert: " + eVar.bZ);
                    }
                }
            } else if (ovVar.hC()) {
                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "native cannot get certMd5, ignore for uninstalled apk");
            } else {
                -l_5_R = (ArrayList) oy.h(ovVar.getPackageName(), 10);
                if (-l_5_R.size() > 0) {
                    -l_4_R.add(-l_5_R);
                }
                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "native cannot get certMd5, get by java api, certs: " + -l_5_R);
            }
            if (-l_4_R.size() > 0) {
                if (eVar.bZ == null) {
                    eVar.bZ = (String) ((ArrayList) -l_4_R.get(0)).get(0);
                }
                if (TextUtils.isEmpty(eVar.Cb)) {
                    eVar.Cb = c.q(-l_4_R);
                }
            }
        } catch (Object -l_4_R2) {
            tmsdk.common.utils.f.c(QScannerManagerV2.LOG_TAG, "handleCert, exception: " + -l_4_R2, -l_4_R2);
        }
    }

    private static a b(ov ovVar, int i) {
        return ovVar != null ? new a(q.cI(ovVar.getPackageName()), q.cI(ovVar.getAppName()), q.cI(ovVar.hz()), q.cI(ovVar.getVersion()), ovVar.getVersionCode(), (int) ovVar.getSize(), q.cI(ovVar.hB()), i) : null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean b(int i, QScanListener qScanListener) {
        synchronized (this.Cy) {
            if (!this.Cw) {
                return false;
            } else if (!(qScanListener == null || this.Cx)) {
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanCanceled, scanType:[" + i + "]");
                qScanListener.onScanCanceled(i);
                this.Cx = true;
            }
        }
    }

    private boolean bd(int i) {
        return ((i & 2) == 0 && (i & 4) == 0) ? false : true;
    }

    private boolean be(int i) {
        return i == 3 || i == 4 || i == 12;
    }

    private void bf(int i) {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "processWhitelistStatus, whitelistStatus:[" + i + "]");
        switch (i) {
            case 1:
                this.Cp.a("ew", true, true);
                return;
            case 2:
                this.Cp.a("ew", false, true);
                return;
            case 3:
                fr();
                return;
            default:
                return;
        }
    }

    private e c(ov ovVar) {
        if (ovVar != null) {
            ov -l_3_R = e(ovVar);
            if (-l_3_R == null) {
                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "nativeScan, loadAppSimpleInfo == null::" + ovVar.getPackageName() + " " + ovVar.hB());
                return null;
            }
            int -l_4_I = 0;
            if (-l_3_R.hC()) {
                -l_4_I = 2;
            } else if (-l_3_R.hx()) {
                -l_4_I = 1;
            }
            if (TextUtils.isEmpty(-l_3_R.hB())) {
                tmsdk.common.utils.f.e(QScannerManagerV2.LOG_TAG, "nativeScan, appEntity.getApkPath() == null, unable to scan: " + -l_3_R.getPackageName() + " " + -l_3_R.hB());
                return null;
            }
            e -l_2_R = new e();
            -l_2_R.Co = false;
            -l_2_R.lastModified = -l_3_R.hy();
            -l_2_R.Cn = -l_3_R.hD();
            try {
                d -l_6_R = this.Cr.a(b(-l_3_R, -l_4_I));
                if (-l_6_R != null) {
                    c.a(-l_6_R, -l_2_R);
                    a(-l_3_R, -l_6_R, -l_2_R);
                    if (TextUtils.isEmpty(-l_2_R.BT)) {
                        -l_2_R.BT = this.CA;
                    }
                }
            } catch (Object -l_5_R) {
                tmsdk.common.utils.f.e(QScannerManagerV2.LOG_TAG, "nativeScan error:[" + -l_5_R + "]");
            }
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "nativeScan[" + -l_2_R.packageName + "][" + -l_2_R.softName + "][" + -l_2_R.Cb + "][" + -l_2_R.path + "]");
            this.Cs++;
            if (this.Cs > 800) {
                System.gc();
                this.Cs = 0;
            }
            return -l_2_R;
        }
        tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "nativeScan, appEntity == null");
        return null;
    }

    private e d(ov ovVar) {
        Object -l_3_R;
        e eVar = null;
        if (ovVar != null) {
            try {
                ov -l_3_R2 = e(ovVar);
                if (-l_3_R2 == null) {
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "loadAppSimpleInfo == null::" + ovVar.getPackageName() + " " + ovVar.hB());
                    return null;
                }
                int -l_4_I = 0;
                if (-l_3_R2.hC()) {
                    -l_4_I = 2;
                } else if (-l_3_R2.hx()) {
                    -l_4_I = 1;
                }
                if (TextUtils.isEmpty(-l_3_R2.hB())) {
                    tmsdk.common.utils.f.e(QScannerManagerV2.LOG_TAG, "genCloudScanEntity, appEntity.getApkPath() == null, unable to scan: " + -l_3_R2.getPackageName() + " " + -l_3_R2.hB());
                    return null;
                }
                e -l_2_R = new e();
                try {
                    -l_2_R.Co = true;
                    -l_2_R.lastModified = -l_3_R2.hy();
                    -l_2_R.Cn = -l_3_R2.hD();
                    Object -l_5_R = b(-l_3_R2, -l_4_I);
                    -l_2_R.packageName = -l_5_R.nf;
                    -l_2_R.softName = -l_5_R.softName;
                    -l_2_R.version = -l_5_R.version;
                    -l_2_R.versionCode = -l_5_R.versionCode;
                    -l_2_R.path = -l_5_R.path;
                    -l_2_R.BQ = -l_5_R.BQ;
                    -l_2_R.size = -l_5_R.size;
                    -l_2_R.type = 1;
                    -l_2_R.lL = 0;
                    -l_2_R.BU = 0;
                    -l_2_R.name = null;
                    -l_2_R.label = null;
                    -l_2_R.BT = null;
                    -l_2_R.url = null;
                    -l_2_R.gS = 0;
                    -l_2_R.dp = 0;
                    -l_2_R.plugins = null;
                    -l_2_R.name = null;
                    -l_2_R.category = 0;
                    Object -l_6_R = null;
                    d -l_7_R = AmScannerV2.b(-l_5_R);
                    if (-l_7_R != null) {
                        -l_6_R = -l_7_R.BW;
                        try {
                            a(-l_3_R2, -l_7_R, -l_2_R);
                        } catch (Throwable th) {
                            -l_3_R = th;
                            eVar = -l_2_R;
                            tmsdk.common.utils.f.b(QScannerManagerV2.LOG_TAG, "genCloudScanEntity, exception: " + -l_3_R, -l_3_R);
                            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "genCloudScanEntity[" + eVar.packageName + "][" + eVar.softName + "][" + eVar.Cb + "][" + eVar.path + "]");
                            return eVar;
                        }
                    }
                    -l_2_R.cc = -l_6_R;
                    if (TextUtils.isEmpty(-l_2_R.BT)) {
                        -l_2_R.BT = this.CA;
                    }
                    eVar = -l_2_R;
                } catch (Throwable th2) {
                    -l_3_R = th2;
                    eVar = -l_2_R;
                    tmsdk.common.utils.f.b(QScannerManagerV2.LOG_TAG, "genCloudScanEntity, exception: " + -l_3_R, -l_3_R);
                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "genCloudScanEntity[" + eVar.packageName + "][" + eVar.softName + "][" + eVar.Cb + "][" + eVar.path + "]");
                    return eVar;
                }
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "genCloudScanEntity[" + eVar.packageName + "][" + eVar.softName + "][" + eVar.Cb + "][" + eVar.path + "]");
                return eVar;
            } catch (Throwable th3) {
                -l_3_R = th3;
                tmsdk.common.utils.f.b(QScannerManagerV2.LOG_TAG, "genCloudScanEntity, exception: " + -l_3_R, -l_3_R);
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "genCloudScanEntity[" + eVar.packageName + "][" + eVar.softName + "][" + eVar.Cb + "][" + eVar.path + "]");
                return eVar;
            }
        }
        tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "genCloudScanEntity, appEntity == null");
        return null;
    }

    private ov e(ov ovVar) {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "loadAppSimpleInfo");
        return !ovVar.hC() ? TMServiceFactory.getSystemInfoService().a(ovVar, 8265) : this.Cq.c(ovVar, 9);
    }

    public static byte[] f(File file) {
        Object -l_7_R;
        FileInputStream fileInputStream = null;
        try {
            Object -l_2_R = MessageDigest.getInstance("MD5");
            FileInputStream -l_1_R = new FileInputStream(file);
            try {
                Object -l_3_R = new byte[8192];
                while (true) {
                    int -l_4_I = -l_1_R.read(-l_3_R);
                    if (-l_4_I == -1) {
                        break;
                    }
                    -l_2_R.update(-l_3_R, 0, -l_4_I);
                }
                Object -l_5_R = -l_2_R.digest();
                if (-l_1_R != null) {
                    try {
                        -l_1_R.close();
                    } catch (Object -l_6_R) {
                        -l_6_R.printStackTrace();
                    }
                }
                return -l_5_R;
            } catch (IOException e) {
                fileInputStream = -l_1_R;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Object -l_4_R) {
                        -l_4_R.printStackTrace();
                    }
                }
                return null;
            } catch (NoSuchAlgorithmException e2) {
                fileInputStream = -l_1_R;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Object -l_4_R2) {
                        -l_4_R2.printStackTrace();
                    }
                }
                return null;
            } catch (Throwable th) {
                -l_7_R = th;
                fileInputStream = -l_1_R;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (Object -l_8_R) {
                        -l_8_R.printStackTrace();
                    }
                }
                throw -l_7_R;
            }
        } catch (IOException e3) {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        } catch (NoSuchAlgorithmException e4) {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            return null;
        } catch (Throwable th2) {
            -l_7_R = th2;
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            throw -l_7_R;
        }
    }

    private void fn() {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[beg] resetScanStatus this:[" + this + "]tid:[" + Thread.currentThread().getId() + "]");
        synchronized (this.Cy) {
            this.Cw = false;
            this.Cx = false;
            this.Cs = 0;
        }
        synchronized (this.Cv) {
            this.mPaused = false;
        }
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[end] resetScanStatus this:[" + this + "]tid:[" + Thread.currentThread().getId() + "]");
    }

    private ArrayList<ov> fo() {
        long -l_1_J = System.currentTimeMillis();
        Object -l_3_R = new ArrayList();
        try {
            Object<ApplicationInfo> -l_5_R = this.mContext.getPackageManager().getInstalledApplications(0);
            if (-l_5_R != null) {
                if (-l_5_R.size() > 0) {
                    Object -l_6_R = this.mContext.getPackageName();
                    for (ApplicationInfo -l_8_R : -l_5_R) {
                        if (!(-l_8_R == null || -l_8_R.packageName == null || -l_8_R.packageName.equals(-l_6_R))) {
                            Object -l_9_R = new ov();
                            -l_9_R.cm(-l_8_R.packageName);
                            -l_3_R.add(-l_9_R);
                        }
                    }
                }
            }
        } catch (Object -l_4_R) {
            tmsdk.common.utils.f.c(QScannerManagerV2.LOG_TAG, "loadInstalledAppList, exception: ", -l_4_R);
        }
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "loadInstalledAppList,size:[" + -l_3_R.size() + "]time(millis) elpased:[" + (System.currentTimeMillis() - -l_1_J) + "]");
        return -l_3_R;
    }

    private int fp() {
        int -l_1_I = 0;
        Object -l_3_R = r.j(this.mContext, lu.b(this.mContext, UpdateConfig.VIRUS_BASE_NAME, null));
        if (-l_3_R != null) {
            -l_1_I = -l_3_R.e();
        }
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "getVirusBaseIntVersion:[" + -l_1_I + "]");
        return -l_1_I;
    }

    private File fq() {
        return new File(this.mContext.getFilesDir().toString() + File.separator + UpdateConfig.WHITELIST_CLOUDSCAN_NAME);
    }

    private boolean fr() {
        Object -l_1_R = fq();
        int -l_2_I = -l_1_R.exists();
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "white list file exist:[" + -l_2_I + "]");
        if (-l_2_I == 0 || !ft()) {
            return false;
        }
        try {
            -l_1_R.delete();
        } catch (Object -l_3_R) {
            tmsdk.common.utils.f.e(QScannerManagerV2.LOG_TAG, "e:[" + -l_3_R + "]");
        }
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "deleteWhiteList");
        fs();
        return true;
    }

    private void fs() {
        int -l_1_I = new Date().getDay();
        if (-l_1_I == this.Cp.getInt("ldd", 0)) {
            int -l_3_I = this.Cp.getInt("dtt", 0);
            this.Cp.a("ldd", -l_1_I, true);
            this.Cp.a("dtt", -l_3_I + 1, true);
            return;
        }
        this.Cp.a("ldd", -l_1_I, true);
        this.Cp.a("dtt", 1, true);
    }

    private boolean ft() {
        if (new Date().getDay() != this.Cp.getInt("ldd", 0)) {
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "today first delete operation");
        } else if (this.Cp.getInt("dtt", 0) >= 3) {
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "today delete limit");
            return false;
        }
        return true;
    }

    private int p(List<e> list) {
        int -l_2_I = 0;
        if (this.Cp.getBoolean("ew", true) != 0) {
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "filterWhiteList");
            ea -l_4_R = (ea) mk.a(this.mContext, UpdateConfig.WHITELIST_CLOUDSCAN_NAME, UpdateConfig.intToString(40427), new ea(), "UTF-8");
            if (!(-l_4_R == null || -l_4_R.iC == null || list == null)) {
                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "whitelist size:[" + -l_4_R.iC.size() + "]");
                Object -l_5_R = list.iterator();
                while (-l_5_R.hasNext()) {
                    e -l_6_R = (e) -l_5_R.next();
                    if (-l_6_R != null) {
                        if (-l_6_R.Cb == null || -l_6_R.Cb.length() <= 32) {
                            if (-l_6_R.cc != null && !-l_6_R.cc.contains(",")) {
                                Object -l_7_R = -l_4_R.iC.iterator();
                                while (-l_7_R.hasNext()) {
                                    dz -l_8_R = (dz) -l_7_R.next();
                                    if (-l_6_R.bZ != null && -l_6_R.bZ.equals(-l_8_R.iu)) {
                                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "is in whitelite:[" + -l_6_R.packageName + "][" + -l_6_R.softName + "]");
                                        -l_2_I++;
                                        -l_5_R.remove();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "hit whitelist size:[" + -l_2_I + "]");
        }
        return -l_2_I;
    }

    private void q(List<e> list) {
        if (list != null && list.size() > 0) {
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "size:[" + list.size() + "]");
            int -l_2_I = 0;
            int -l_3_I = 0;
            for (e -l_5_R : list) {
                if (-l_5_R != null) {
                    if (-l_5_R.Cg) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "isInPayList:[" + -l_5_R.packageName + "][" + -l_5_R.softName + "]");
                    }
                    if (-l_5_R.Ch) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "isInStealAccountList:[" + -l_5_R.packageName + "][" + -l_5_R.softName + "]");
                    }
                    if (-l_5_R.gS == 4) {
                        -l_3_I++;
                    } else if (-l_5_R.gS != 0) {
                        -l_2_I++;
                    }
                }
            }
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "riskCount:[" + -l_2_I + "]unknowCount:[" + -l_3_I + "]");
        }
    }

    private List<QScanResultEntity> r(List<e> list) {
        if (list == null) {
            return new ArrayList();
        }
        Object -l_2_R = new ArrayList(list.size());
        for (e -l_4_R : list) {
            -l_2_R.add(a(-l_4_R));
        }
        return -l_2_R;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int a(int i, List<String> list, QScanListener qScanListener, int i2, long j) {
        synchronized (this.mLock) {
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[beg]scanInstalledPackagesImpl, scanType:[" + i + "]packageNames size:[" + (list != null ? list.size() : -1) + "]scanListener:[" + qScanListener + "]requestType:[" + i2 + "]timeoutMillis:[" + j + "]");
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "scanInstalledPackagesImpl this:[" + this + "]tid:[" + Thread.currentThread().getId() + "]");
            s.bW(8);
            if (qScanListener != null && bd(i) && be(i2)) {
                if (i2 == 3) {
                    if (list != null && list.size() == 1) {
                    }
                }
                if ((j <= 0 ? 1 : null) == null) {
                    if ((j >= 2000 ? 1 : null) == null) {
                        return QScanConfig.ERR_ILLEGAL_ARG;
                    }
                }
                if (ic.bE()) {
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "isExpired");
                    return QScanConfig.ERR_EXPIRED;
                } else if (this.Cr != null) {
                    List -l_13_R;
                    Object -l_14_R;
                    fn();
                    synchronized (this.Ct) {
                        this.Cu = true;
                    }
                    int -l_8_I = 0;
                    int -l_9_I = 0;
                    if ((i & 2) != 0) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "QScanConfig.SCAN_LOCAL");
                        -l_8_I = 1;
                    }
                    if ((i & 4) != 0) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "QScanConfig.SCAN_CLOUD");
                        -l_9_I = 1;
                    }
                    int -l_10_I = 2;
                    if (-l_8_I == 0 && -l_9_I != 0) {
                        -l_10_I = 4;
                    }
                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanStarted, scanType:[" + -l_10_I + "]");
                    qScanListener.onScanStarted(-l_10_I);
                    long -l_11_J = System.currentTimeMillis();
                    if (list != null && list.size() > 0) {
                        -l_13_R = new ArrayList(list.size());
                        for (String -l_15_R : list) {
                            Object -l_16_R = new ov();
                            -l_16_R.cm(-l_15_R);
                            -l_13_R.add(-l_16_R);
                        }
                    } else {
                        -l_13_R = fo();
                    }
                    if (-l_13_R != null && -l_13_R.size() > 0) {
                        -l_14_R = a(i, -l_13_R, qScanListener, j, i2);
                        fn();
                        synchronized (this.Ct) {
                            this.Cu = false;
                        }
                        -l_10_I = 4;
                        if (-l_8_I != 0 && -l_9_I == 0) {
                            -l_10_I = 2;
                        }
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanFinished, scanType:[" + -l_10_I + "]size:[" + -l_14_R.size() + "]");
                        qScanListener.onScanFinished(-l_10_I, r(-l_14_R));
                        q(-l_14_R);
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "scanInstalledPackagesImpl this:[" + this + "]tid:[" + Thread.currentThread().getId() + "]");
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[end]scanInstalledPackagesImpl, time(millis) elapsed:[" + (System.currentTimeMillis() - -l_11_J) + "]");
                        return 0;
                    }
                    fn();
                    synchronized (this.Ct) {
                        this.Cu = false;
                    }
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "cannot featch installed packages");
                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanFinished, scanType:[" + -l_10_I + "]size:[0]");
                    qScanListener.onScanFinished(-l_10_I, new ArrayList());
                    return QScanConfig.W_CANNOT_FEATCH_PKGINFO;
                } else {
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "not invoke initScanner");
                    return QScanConfig.W_NOT_INIT;
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int a(int i, List<String> list, QScanListener qScanListener, long j) {
        synchronized (this.mLock) {
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[beg]scanUninstalledApksImpl, scanType:[" + i + "]apkPaths size:[" + (list != null ? list.size() : -1) + "]scanListener:[" + qScanListener + "]timeoutMillis:[" + j + "]");
            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "scanUninstalledApksImpl this:[" + this + "]tid:[" + Thread.currentThread().getId() + "]");
            s.bW(8);
            if (qScanListener != null && bd(i)) {
                if ((j <= 0 ? 1 : null) == null) {
                    if ((j >= 2000 ? 1 : null) == null) {
                        return QScanConfig.ERR_ILLEGAL_ARG;
                    }
                }
                if (ic.bE()) {
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "isExpired");
                    return QScanConfig.ERR_EXPIRED;
                } else if (this.Cr != null) {
                    fn();
                    synchronized (this.Ct) {
                        this.Cu = true;
                    }
                    int -l_7_I = 0;
                    int -l_8_I = 0;
                    if ((i & 2) != 0) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "QScanConfig.SCAN_LOCAL");
                        -l_7_I = 1;
                    }
                    if ((i & 4) != 0) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "QScanConfig.SCAN_CLOUD");
                        -l_8_I = 1;
                    }
                    final Object -l_9_R = new int[]{2};
                    if (-l_7_I == 0 && -l_8_I != 0) {
                        -l_9_R[0] = 4;
                    }
                    long -l_10_J = System.currentTimeMillis();
                    final List -l_12_R = new ArrayList();
                    if (list != null && list.size() > 0) {
                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanStarted, scanType:[" + -l_9_R[0] + "]");
                        qScanListener.onScanStarted(-l_9_R[0]);
                        List -l_13_R = new ArrayList(list.size());
                        for (String -l_15_R : list) {
                            Object -l_16_R = new ov();
                            -l_16_R.P(true);
                            -l_16_R.cn(-l_15_R);
                            -l_13_R.add(-l_16_R);
                        }
                        -l_12_R.addAll(a(i, -l_13_R, qScanListener, j, 12));
                    } else {
                        qa -l_13_R2 = new qa();
                        Object -l_15_R2 = new String[]{"/storage/emulated/legacy", "/storage_int", "/HWUserData"};
                        -l_13_R2.Lf.add(new pz(0, null, new String[]{"apk"}));
                        -l_13_R2.Lg = -l_15_R2;
                        final List -l_16_R2 = new ArrayList();
                        final Object -l_17_R = new ArrayList();
                        final QScanListener qScanListener2 = qScanListener;
                        final int i2 = i;
                        QSdcardScanner -l_18_R = SdcardScannerFactory.getQSdcardScanner(2, new a(this) {
                            final /* synthetic */ f CH;

                            public void onFound(int i, QFile qFile) {
                                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "onFound,ruleId:[" + i + "]path:[" + qFile.filePath + "]");
                                this.CH.a(-l_9_R[0], qScanListener2);
                                if (this.CH.b(-l_9_R[0], qScanListener2)) {
                                    if (-l_17_R.size() > 0 && -l_17_R.get(0) != null) {
                                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "QSdcardScanner cancel");
                                        ((QSdcardScanner) -l_17_R.get(0)).cancleScan();
                                    }
                                    return;
                                }
                                ov -l_3_R = new ov();
                                -l_3_R.P(true);
                                -l_3_R.cn(qFile.filePath);
                                if ((i2 & 2) == 0) {
                                    -l_16_R2.add(-l_3_R);
                                } else {
                                    e -l_4_R = this.CH.c(-l_3_R);
                                    if (-l_4_R != null) {
                                        -l_12_R.add(-l_4_R);
                                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanProgress,scanType:[2][" + -l_4_R.packageName + "][" + -l_4_R.softName + "][" + -l_4_R.path + "]");
                                        qScanListener2.onScanProgress(2, -1, -1, this.CH.a(-l_4_R));
                                    }
                                }
                            }
                        }, -l_13_R2);
                        if (-l_18_R != null) {
                            -l_17_R.add(-l_18_R);
                            final Object -l_19_R = new ArrayList();
                            qScanListener2 = qScanListener;
                            -l_18_R.registerProgressListener(9999, new ProgressListener(this) {
                                final /* synthetic */ f CH;

                                public boolean onScanPathChange(String str) {
                                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "onScanPathChange,path:[" + str + "]");
                                    this.CH.a(-l_9_R[0], qScanListener2);
                                    if (this.CH.b(-l_9_R[0], qScanListener2)) {
                                        if (-l_17_R.size() > 0 && -l_17_R.get(0) != null) {
                                            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "QSdcardScanner cancel");
                                            ((QSdcardScanner) -l_17_R.get(0)).cancleScan();
                                        }
                                        return false;
                                    }
                                    Object -l_2_R = mc.bU(str);
                                    if (-l_19_R.contains(-l_2_R)) {
                                        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "has scanned path:[" + str + "]");
                                        return false;
                                    }
                                    -l_19_R.add(-l_2_R);
                                    if (f.Cz != null) {
                                        for (Object -l_6_R : f.Cz) {
                                            if (str.endsWith("/" + -l_6_R)) {
                                                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "mIgnoreDirs path:[" + str + "]");
                                                return false;
                                            }
                                        }
                                    }
                                    return true;
                                }
                            });
                            tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanStarted, scanType:[" + -l_9_R[0] + "]");
                            qScanListener.onScanStarted(-l_9_R[0]);
                            for (String -l_22_R : lu.s(TMSDKContext.getApplicaionContext())) {
                                tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "startScan path:[" + -l_22_R + "]");
                                -l_18_R.startScan(-l_22_R);
                            }
                            -l_18_R.release();
                            -l_19_R.clear();
                            if (-l_8_I != 0) {
                                if (-l_12_R.size() > 0) {
                                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanStarted, scanType:[4]");
                                    qScanListener.onScanStarted(4);
                                    a(-l_12_R, qScanListener, 12, j, null);
                                } else if (-l_16_R2.size() > 0) {
                                    -l_12_R.addAll(a(4, -l_16_R2, qScanListener, j, 12));
                                }
                            }
                        } else {
                            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "SdcardScannerFactory.getQSdcardScanner failed!");
                            return QScanConfig.W_GET_SDCARD_QSCANNER;
                        }
                    }
                    fn();
                    synchronized (this.Ct) {
                        this.Cu = false;
                    }
                    -l_9_R[0] = 4;
                    if (-l_7_I != 0 && -l_8_I == 0) {
                        -l_9_R[0] = 2;
                    }
                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[callback]onScanFinished, scanType:[" + -l_9_R[0] + "]size:[" + -l_12_R.size() + "]");
                    qScanListener.onScanFinished(-l_9_R[0], r(-l_12_R));
                    q(-l_12_R);
                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "scanUninstalledApksImpl this:[" + this + "]tid:[" + Thread.currentThread().getId() + "]");
                    tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[end]scanUninstalledApksImpl, time(millis) elapsed:[" + (System.currentTimeMillis() - -l_10_J) + "]");
                    return 0;
                } else {
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "not invoke initScanner");
                    return QScanConfig.W_NOT_INIT;
                }
            }
        }
    }

    public void cancelScan() {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "cancelScan");
        synchronized (this.Cy) {
            this.Cw = true;
        }
        synchronized (this.Cv) {
            this.Cv.notifyAll();
        }
    }

    public void continueScan() {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "continueScan");
        synchronized (this.Cv) {
            this.Cv.notifyAll();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int fm() {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[beg]freeScanner this:[" + this + "]tid:[" + Thread.currentThread().getId() + "]");
        long -l_1_J = System.currentTimeMillis();
        synchronized (this.Ct) {
            if (this.Cu) {
                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "is scanning");
                return QScanConfig.W_IS_SCANNING;
            }
        }
    }

    public int getSingletonType() {
        return 2;
    }

    public String getVirusBaseVersion() {
        Object -l_3_R = new Date(((long) fp()) * 1000);
        Object -l_5_R = new SimpleDateFormat("yyyyMMdd").format(-l_3_R) + (-l_3_R.getHours() <= 12 ? "A" : "B");
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "getVirusBaseVersion:[" + -l_5_R + "]");
        return -l_5_R;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int initScanner() {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "[beg]initScanner, this:[" + this + "]tid:[" + Thread.currentThread().getId() + "]");
        if (ic.bE()) {
            tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "licence expired, initScanner");
            return QScanConfig.ERR_EXPIRED;
        }
        long -l_1_J = System.currentTimeMillis();
        synchronized (this.mLock) {
            if (!AmScannerV2.isSupported()) {
                tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "AmScannerV2 no supported, initScanner");
                return QScanConfig.ERR_NATIVE_LOAD;
            } else if (this.Cr == null) {
                this.Cr = new AmScannerV2(this.mContext, lu.b(this.mContext, UpdateConfig.VIRUS_BASE_NAME, null));
                if (!this.Cr.fl()) {
                    this.Cr = null;
                    tmsdk.common.utils.f.g(QScannerManagerV2.LOG_TAG, "initScanner failed!!");
                    return QScanConfig.ERR_INIT;
                }
            }
        }
    }

    public void onCreate(Context context) {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "onCreate, this:[" + this + "]");
        this.mContext = context;
        this.Cq = (ox) ManagerCreatorC.getManager(ox.class);
        this.Cp = new md("133_cs_wl");
    }

    public void pauseScan() {
        tmsdk.common.utils.f.f(QScannerManagerV2.LOG_TAG, "pauseScan");
        synchronized (this.Cv) {
            this.mPaused = true;
        }
    }
}
