package tmsdk.common.module.update;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import tmsdk.bg.tcc.TelNumberLocator;
import tmsdk.common.NumMarker;
import tmsdk.common.creator.BaseManagerC;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.numbermarker.NumMarkerManager;
import tmsdk.common.module.qscanner.impl.AmScannerV2;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;
import tmsdk.common.utils.r;
import tmsdk.common.utils.s;
import tmsdk.common.utils.u;
import tmsdkobf.aa;
import tmsdkobf.ad;
import tmsdkobf.ai;
import tmsdkobf.aj;
import tmsdkobf.du;
import tmsdkobf.eo;
import tmsdkobf.fd;
import tmsdkobf.gd;
import tmsdkobf.im;
import tmsdkobf.ir;
import tmsdkobf.jy;
import tmsdkobf.kl;
import tmsdkobf.lq;
import tmsdkobf.lu;
import tmsdkobf.lx;

final class a extends BaseManagerC {
    private String JQ = null;
    private AtomicBoolean JR = new AtomicBoolean(false);
    private HashMap<Long, SoftReference<IUpdateObserver>> JS = new HashMap();
    private b JT;
    private Context mContext;

    a() {
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean B(String str, String str2) {
        Object -l_4_R = ((NumMarkerManager) ManagerCreatorC.getManager(NumMarkerManager.class)).getDataMd5(str);
        return (str2 == null || -l_4_R == null || !str2.toLowerCase().equals(-l_4_R.toLowerCase())) ? false : true;
    }

    private List<ad> D(long j) {
        Object -l_4_R = new ArrayList();
        if (j == UpdateConfig.UPDATA_FLAG_NUM_MARK) {
            -l_4_R.add(bJ(40458));
            -l_4_R.add(bJ(UpdateConfig.getLargeMarkFileId()));
        } else if (j == 2) {
            -l_4_R.add(r.m(50001, r.b(this.mContext, 50001, ".sdb")));
        } else if (j == UpdateConfig.UPDATE_FLAG_YELLOW_PAGEV2_Large) {
            Object -l_3_R = r.n(40461, r.b(this.mContext, 40461, ".sdb"));
            if (-l_3_R != null) {
                f.f("gjj", "fileId:" + -l_3_R.aE + " timestamp:" + -l_3_R.timestamp + " pfutimestamp:" + -l_3_R.aG + " version:" + -l_3_R.version);
                -l_4_R.add(-l_3_R);
            }
        } else {
            -l_4_R.add(r.b(this.mContext, j));
        }
        return -l_4_R;
    }

    @Deprecated
    private ad E(long j) {
        Object -l_3_R = null;
        if (j != 4) {
            if (j == UpdateConfig.UPDATE_FLAG_VIRUS_BASE || j == UpdateConfig.UPDATE_FLAG_VIRUS_BASE_ENG) {
                -l_3_R = r.i(this.mContext, r.a(this.mContext, j));
            } else if (j != 2) {
                return r.b(this.mContext, j);
            } else {
                -l_3_R = r.m(50001, r.a(this.mContext, j));
            }
        }
        return -l_3_R;
    }

    private void a(int i, ICheckListener iCheckListener) {
        if (iCheckListener != null) {
            if (i != 0) {
                f.d("UpdateMgr", "[callback]onCheckEvent:[" + i + "]");
                iCheckListener.onCheckEvent(i);
            }
            f.d("UpdateMgr", "[callback]onCheckFinished--null");
            iCheckListener.onCheckFinished(null);
        }
    }

    private void a(UpdateInfo updateInfo) {
        Object -l_2_R = TelNumberLocator.getDefault(this.mContext);
        aj -l_3_R = (aj) updateInfo.data1;
        if (-l_3_R.bf) {
            -l_2_R.patchLocation(this.JQ + File.separator + updateInfo.fileName, lq.bytesToHexString(-l_3_R.aF));
        }
        -l_2_R.reload();
    }

    private void b(UpdateInfo updateInfo) {
        Object -l_8_R;
        Object -l_2_R = this.JQ + File.separator + updateInfo.fileName;
        aj -l_3_R = (aj) updateInfo.data1;
        if (-l_3_R != null) {
            Object -l_7_R;
            if (-l_3_R.bl == 2) {
                -l_7_R = this.JQ + File.separator + "zipTemp" + File.separator;
                try {
                    -l_8_R = new File(-l_7_R);
                    if (-l_8_R.exists()) {
                        kl.b(-l_8_R);
                    }
                    gd.b(-l_2_R, -l_7_R);
                    Object -l_9_R = -l_8_R.listFiles();
                    if (-l_9_R != null) {
                        if (-l_9_R.length != 0 && -l_9_R.length == 1) {
                            Object -l_10_R = new File(-l_2_R);
                            if (-l_10_R.exists()) {
                                -l_10_R.delete();
                            }
                            kl.copyFile(-l_9_R[0], -l_10_R);
                            kl.b(-l_8_R);
                        } else {
                            return;
                        }
                    }
                    return;
                } catch (Object -l_8_R2) {
                    f.b("UpdateMgr", "unzip num mark file failed", -l_8_R2);
                    return;
                }
            } else if (-l_3_R.bl != 1) {
                f.f("UpdateMgr", "normal num mark file");
            } else {
                f.e("UpdateMgr", "num mark file should not zip encrypt");
                return;
            }
            -l_7_R = "";
            -l_7_R = -l_3_R.bf ? lq.bytesToString(-l_3_R.bg) : lq.bytesToString(-l_3_R.aF);
            NumMarkerManager -l_8_R3 = (NumMarkerManager) ManagerCreatorC.getManager(NumMarkerManager.class);
            if (-l_8_R3 != null && -l_8_R3.updateMarkFile(-l_2_R, -l_7_R) == 0) {
                -l_8_R3.refreshTagMap();
            }
        }
    }

    private ad bJ(int i) {
        Object -l_2_R = r.o(i, r.b(this.mContext, i, ".sdb"));
        if (-l_2_R == null) {
            -l_2_R = new ad();
            -l_2_R.aE = i;
            -l_2_R.aF = new byte[0];
            -l_2_R.timestamp = 0;
        }
        if (-l_2_R.aF == null) {
            -l_2_R.aF = lq.at("");
        }
        f.f(NumMarker.Tag, "fileId:" + -l_2_R.aE + " timestamp:" + -l_2_R.timestamp + " pfutimestamp:" + -l_2_R.aG + " version:" + -l_2_R.version);
        return -l_2_R;
    }

    private int bK(int i) {
        final Object -l_2_R = new AtomicReference(Integer.valueOf(0));
        fd -l_5_R = r.j(this.mContext, r.a(this.mContext, UpdateConfig.UPDATE_FLAG_VIRUS_BASE));
        if (-l_5_R == null) {
            f.g("UpdateMgr", "getVirusClientInfo return null!");
            -l_2_R.set(Integer.valueOf(-2));
        } else {
            -l_5_R.lF = 3;
            -l_5_R.ay = i;
            Object -l_6_R = new du();
            -l_6_R.hZ = -l_5_R;
            final long -l_7_J = System.currentTimeMillis();
            f.d("UpdateMgr", "[Shark]Cmd_CSUpdateVirusInfos, sendShark");
            final Object -l_9_R = new Object();
            im.bK().a(2006, -l_6_R, new eo(), 0, new jy(this) {
                final /* synthetic */ a JW;

                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    f.d("UpdateMgr", "[Shark]onFinish-Cmd_CSUpdateVirusInfos, elapsed time:[" + (System.currentTimeMillis() - -l_7_J) + "]cmdId:[" + i2 + "]retCode:[" + i3 + "]dataRetCode: " + i4);
                    if (i3 != 0 || i4 != 0) {
                        f.g("UpdateMgr", "retCode != 0 || dataRetCode != 0");
                        if (i3 % 20 != -4) {
                            -l_2_R.set(Integer.valueOf(-999));
                        } else {
                            -l_2_R.set(Integer.valueOf(-206));
                        }
                        if (i4 != 0) {
                            -l_2_R.set(Integer.valueOf(-205));
                        }
                    } else if (jceStruct == null) {
                        f.g("UpdateMgr", "SCUpdateVirusInfos is null!");
                        -l_2_R.set(Integer.valueOf(-6));
                    } else {
                        eo -l_6_R = (eo) jceStruct;
                        Object -l_7_R = -l_6_R.kv;
                        Object -l_8_R = -l_6_R.kw;
                        if (-l_7_R == null) {
                            f.g("UpdateMgr", "SCUpdateVirusInfos.serverinfo is null!");
                            -l_2_R.set(Integer.valueOf(-6));
                        } else if (-l_7_R.f()) {
                            f.g("UpdateMgr", "need update engine, donnot update virus base!");
                        } else if (-l_8_R != null && -l_8_R.size() > 0) {
                            Object -l_9_R = r.a(this.JW.mContext, UpdateConfig.UPDATE_FLAG_VIRUS_BASE);
                            int -l_10_I = AmScannerV2.a(this.JW.mContext, -l_9_R, -l_7_R, -l_8_R);
                            -l_2_R.set(Integer.valueOf(-l_10_I));
                            if (-l_10_I != 0) {
                                f.g("UpdateMgr", "amf file error, delete:[" + -l_9_R + "]");
                                lu.bK(-l_9_R);
                            }
                            f.d("UpdateMgr", "native updateBase, size: " + -l_8_R.size() + " ret: " + -l_10_I);
                        } else {
                            f.g("UpdateMgr", "no update info, virusInfoList: " + -l_8_R);
                        }
                    }
                    synchronized (-l_9_R) {
                        -l_9_R.notify();
                    }
                }
            }, 60000);
            Object -l_11_R = -l_9_R;
            synchronized (-l_9_R) {
                try {
                    -l_9_R.wait();
                } catch (Throwable -l_12_R) {
                    f.c("UpdateMgr", "SCAN_LOCK.wait(): " + -l_12_R, -l_12_R);
                }
            }
        }
        return ((Integer) -l_2_R.get()).intValue();
    }

    private void c(UpdateInfo updateInfo) {
        Object -l_2_R = this.JQ + File.separator + updateInfo.fileName;
        aj -l_3_R = (aj) updateInfo.data1;
        if (-l_3_R != null) {
            Object -l_7_R;
            if (-l_3_R.bl == 2) {
                -l_7_R = this.JQ + File.separator + "zipTemp" + File.separator;
                Object -l_8_R;
                try {
                    -l_8_R = new File(-l_7_R);
                    if (-l_8_R.exists()) {
                        kl.b(-l_8_R);
                    }
                    gd.b(-l_2_R, -l_7_R);
                    Object -l_9_R = -l_8_R.listFiles();
                    if (-l_9_R != null) {
                        if (-l_9_R.length != 0 && -l_9_R.length == 1) {
                            Object -l_10_R = new File(-l_2_R);
                            if (-l_10_R.exists()) {
                                -l_10_R.delete();
                            }
                            kl.copyFile(-l_9_R[0], -l_10_R);
                            kl.b(-l_8_R);
                        } else {
                            return;
                        }
                    }
                    return;
                } catch (Object -l_8_R2) {
                    f.b("UpdateMgr", "unzip num mark big file failed", -l_8_R2);
                    return;
                }
            } else if (-l_3_R.bl != 1) {
                f.f("UpdateMgr", "normal num mark big file");
            } else {
                f.e("UpdateMgr", "num mark big file should not zip encrypt");
                return;
            }
            -l_7_R = "";
            -l_7_R = -l_3_R.bf ? lq.bytesToString(-l_3_R.bg) : lq.bytesToString(-l_3_R.aF);
            NumMarkerManager -l_8_R3 = (NumMarkerManager) ManagerCreatorC.getManager(NumMarkerManager.class);
            if (-l_8_R3 != null && -l_8_R3.updateMarkBigFile(-l_2_R, -l_7_R) == 0) {
                -l_8_R3.refreshTagMap();
            }
        }
    }

    public void a(long j, ICheckListener iCheckListener) {
        f.d("UpdateMgr", "check-checkFlag:[" + j + "]");
        this.JR.set(false);
        if (iCheckListener != null) {
            f.d("UpdateMgr", "[callback]onCheckStarted");
            iCheckListener.onCheckStarted();
        }
        long prepareCheckFlag = UpdateConfig.prepareCheckFlag(j);
        final Object -l_4_R = new ArrayList();
        if (!ir.bU().bV()) {
            CheckResult -l_5_R = new CheckResult();
            -l_5_R.mTitle = "Warning";
            -l_5_R.mMessage = "Expired! Please contact TMS(Tencent Mobile Secure) group.";
            -l_5_R.mUpdateInfoList = -l_4_R;
            if (iCheckListener != null) {
                f.d("UpdateMgr", "[callback]onCheckFinished--Licence Expired");
                iCheckListener.onCheckFinished(-l_5_R);
            }
        } else if (this.JR.get()) {
            if (iCheckListener != null) {
                f.d("UpdateMgr", "[callback]111onCheckCanceled");
                iCheckListener.onCheckCanceled();
            }
            a(0, iCheckListener);
        } else {
            ArrayList -l_5_R2 = new ArrayList();
            for (long -l_9_J : UpdateConfig.UPDATE_FLAGS) {
                if ((-l_9_J & prepareCheckFlag) != 0) {
                    if (-l_9_J == UpdateConfig.UPDATA_FLAG_NUM_MARK) {
                        -l_5_R2.addAll(D(UpdateConfig.UPDATA_FLAG_NUM_MARK));
                    } else if (-l_9_J == 2) {
                        -l_5_R2.addAll(D(2));
                    } else if (-l_9_J == UpdateConfig.UPDATE_FLAG_YELLOW_PAGEV2_Large) {
                        -l_5_R2.addAll(D(UpdateConfig.UPDATE_FLAG_YELLOW_PAGEV2_Large));
                    } else {
                        ad -l_11_R = E(-l_9_J);
                        if (-l_11_R == null) {
                            -l_11_R = new ad();
                            -l_11_R.aE = UpdateConfig.getFileIdByFlag(-l_9_J);
                            -l_11_R.aF = new byte[0];
                            -l_11_R.timestamp = 0;
                        }
                        if (-l_11_R.aF == null) {
                            -l_11_R.aF = lq.at("");
                        }
                        if (-l_9_J == UpdateConfig.UPDATE_FLAG_VIRUS_BASE) {
                            f.d("UpdateMgr", "req::UpdateConfig.UPDATE_FLAG_VIRUS_BASE");
                            f.d("UpdateMgr", "req::fileId:[" + -l_11_R.aE + "]");
                        } else if (-l_9_J == UpdateConfig.UPDATE_FLAG_VIRUS_BASE_ENG) {
                            f.d("UpdateMgr", "req::UpdateConfig.UPDATE_FLAG_VIRUS_BASE_ENG");
                            f.d("UpdateMgr", "req::fileId:[" + -l_11_R.aE + "]");
                        }
                        -l_5_R2.add(-l_11_R);
                    }
                }
            }
            Object -l_6_R = new aa();
            -l_6_R.ax = -l_5_R2;
            -l_6_R.ay = 1;
            Object -l_7_R = new ai();
            final long -l_8_J = System.currentTimeMillis();
            f.d("UpdateMgr", "[Shark]Cmd_CSConfInfo, sendShark");
            final ICheckListener iCheckListener2 = iCheckListener;
            im.bK().a(108, -l_6_R, -l_7_R, 0, new jy(this) {
                final /* synthetic */ a JW;

                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    f.d("UpdateMgr", "[Shark]onFinish-Cmd_CSConfInfo, elapsed time:[" + (System.currentTimeMillis() - -l_8_J) + "]cmdId:[" + i2 + "]retCode:[" + i3 + "]dataRetCode: " + i4);
                    if (jceStruct != null) {
                        ai -l_6_R = (ai) jceStruct;
                        if (-l_6_R == null || -l_6_R.ba == null) {
                            f.g("UpdateMgr", "(SCConfInfo)resp.vecConfInfo empty");
                            this.JW.a(-205, iCheckListener2);
                            return;
                        } else if (-l_6_R.ba.size() > 0) {
                            if (i3 != 0) {
                                f.d("UpdateMgr", "failed, retCode: " + i3);
                                if (i3 % 20 != -4) {
                                    this.JW.a(-999, iCheckListener2);
                                } else {
                                    this.JW.a(-206, iCheckListener2);
                                }
                            } else {
                                int -l_7_I = 0;
                                Object -l_8_R = -l_6_R.ba.iterator();
                                while (-l_8_R.hasNext()) {
                                    aj -l_9_R = (aj) -l_8_R.next();
                                    if (-l_9_R != null) {
                                        int -l_7_I2 = -l_7_I + 1;
                                        f.d("UpdateMgr", "[" + -l_7_I + "]resp::fileId:[" + -l_9_R.aE + "]isIncreUpdate:[" + -l_9_R.bf + "]");
                                        Object -l_10_R = new UpdateInfo();
                                        String fileNameByFileId = (40458 == -l_9_R.aE || UpdateConfig.getLargeMarkFileId() == -l_9_R.aE) ? !-l_9_R.bf ? UpdateConfig.intToString(-l_9_R.aE) + ".sdb" : UpdateConfig.intToString(-l_9_R.aE) + ".sdb" + UpdateConfig.PATCH_SUFIX : !-l_9_R.bf ? UpdateConfig.getFileNameByFileId(-l_9_R.aE) : UpdateConfig.getFileNameByFileId(-l_9_R.aE) + UpdateConfig.PATCH_SUFIX;
                                        -l_10_R.fileName = fileNameByFileId;
                                        -l_10_R.mFileID = -l_9_R.aE;
                                        f.d("UpdateMgr", "[" + -l_7_I2 + "]resp::fileName:[" + -l_10_R.fileName + "]url:[" + -l_9_R.url + "]");
                                        -l_10_R.flag = UpdateConfig.getFlagByFileId(-l_9_R.aE);
                                        -l_10_R.type = 0;
                                        -l_10_R.url = -l_9_R.url;
                                        -l_10_R.data1 = -l_9_R;
                                        -l_10_R.fileSize = -l_9_R.fileSize;
                                        -l_4_R.add(-l_10_R);
                                        -l_7_I = -l_7_I2;
                                    }
                                }
                                -l_8_R = new CheckResult();
                                Object -l_9_R2 = -l_6_R.aY;
                                -l_8_R.mTitle = -l_9_R2 == null ? "" : -l_9_R2.title;
                                -l_8_R.mMessage = -l_9_R2 == null ? "" : -l_9_R2.T;
                                -l_8_R.mUpdateInfoList = -l_4_R;
                                if (iCheckListener2 != null) {
                                    f.d("UpdateMgr", "[callback]onCheckFinished");
                                    iCheckListener2.onCheckFinished(-l_8_R);
                                }
                                f.d("UpdateMgr", "title:[" + -l_8_R.mTitle + "]msg:[" + -l_8_R.mMessage + "]");
                            }
                            return;
                        } else {
                            f.d("UpdateMgr", "size: 0, no available db");
                            this.JW.a(i3, iCheckListener2);
                            return;
                        }
                    }
                    f.g("UpdateMgr", "null == resp");
                    this.JW.a(-205, iCheckListener2);
                }
            });
        }
    }

    public void addObserver(long j, IUpdateObserver iUpdateObserver) {
        synchronized (this.JS) {
            if (iUpdateObserver != null) {
                this.JS.put(Long.valueOf(j), new SoftReference(iUpdateObserver));
            }
        }
    }

    public void cancel() {
        this.JR.set(true);
    }

    public void d(UpdateInfo updateInfo) {
        synchronized (this.JS) {
            for (Entry -l_4_R : this.JS.entrySet()) {
                if ((((Long) -l_4_R.getKey()).longValue() & updateInfo.flag) != 0) {
                    IUpdateObserver -l_5_R = (IUpdateObserver) ((SoftReference) -l_4_R.getValue()).get();
                    if (-l_5_R != null) {
                        -l_5_R.onChanged(updateInfo);
                    }
                }
            }
        }
    }

    public String getFileSavePath() {
        return this.JQ;
    }

    public int getSingletonType() {
        return 1;
    }

    public void onCreate(Context context) {
        this.mContext = context;
        this.JQ = context.getFilesDir().getAbsolutePath();
        this.JT = b.hL();
    }

    public void removeObserver(long j) {
        synchronized (this.JS) {
            this.JS.remove(Long.valueOf(j));
        }
    }

    public boolean update(List<UpdateInfo> list, final IUpdateListener iUpdateListener) {
        f.d("UpdateMgr", "update-updateInfoList:[" + list + "]updateListener:[" + iUpdateListener + "]");
        s.bW(4);
        this.JR.set(false);
        if (iUpdateListener != null) {
            f.d("UpdateMgr", "[callback]onUpdateStarted");
            iUpdateListener.onUpdateStarted();
        }
        if (!ir.bU().bV()) {
            if (iUpdateListener != null) {
                f.d("UpdateMgr", "[callback]00onUpdateFinished");
                iUpdateListener.onUpdateFinished();
            }
            return false;
        } else if (this.JR.get()) {
            if (iUpdateListener != null) {
                f.d("UpdateMgr", "[callback]onUpdateCanceled");
                iUpdateListener.onUpdateCanceled();
                f.d("UpdateMgr", "[callback]11onUpdateFinished");
                iUpdateListener.onUpdateFinished();
            }
            return false;
        } else {
            int -l_3_I = 1;
            final Object -l_4_R = new AtomicBoolean(false);
            f.d("UpdateMgr", "updateInfoList size: " + list.size());
            for (int -l_5_I = 0; -l_5_I < list.size(); -l_5_I++) {
                -l_4_R.set(false);
                final UpdateInfo -l_6_R = (UpdateInfo) list.get(-l_5_I);
                if (-l_6_R != null) {
                    Object -l_7_R;
                    f.d("UpdateMgr", "[" + -l_5_I + "]updateInfo fileName:[" + -l_6_R.fileName + "]url:[" + -l_6_R.url + "]");
                    if (list.size() != 1) {
                        if (iUpdateListener != null) {
                            f.d("UpdateMgr", "[callback]onProgressChanged:[" + -l_5_I + "]");
                            iUpdateListener.onProgressChanged(-l_6_R, (-l_5_I * 100) / list.size());
                        }
                    } else if (iUpdateListener != null) {
                        f.d("UpdateMgr", "[callback]onProgressChanged:[" + -l_5_I + "]");
                        iUpdateListener.onProgressChanged(-l_6_R, 50);
                    }
                    if (-l_6_R.flag == UpdateConfig.UPDATE_FLAG_VIRUS_BASE || -l_6_R.flag == UpdateConfig.UPDATE_FLAG_VIRUS_BASE_ENG) {
                        int -l_7_I = bK(1);
                        -l_6_R.downType = (byte) 2;
                        if (-l_7_I != 0) {
                            -l_4_R.set(true);
                            -l_3_I = 0;
                            -l_6_R.errorCode = -l_7_I;
                            if (iUpdateListener != null) {
                                f.d("UpdateMgr", "[callback]onUpdateEvent:[" + -l_7_I + "]");
                                iUpdateListener.onUpdateEvent(-l_6_R, -l_7_I);
                            }
                        }
                    } else if (!(-l_6_R == null || -l_6_R.url == null)) {
                        int -l_9_I;
                        -l_7_R = new lx(this.mContext);
                        -l_7_R.bP(this.JQ + "/");
                        -l_7_R.bQ(-l_6_R.fileName);
                        -l_7_R.a(new tmsdkobf.lv.a(this) {
                            final /* synthetic */ a JW;

                            public void a(Bundle bundle) {
                                int -l_2_I = bundle.getInt("key_errcode");
                                if (iUpdateListener != null) {
                                    f.d("UpdateMgr", "[callback]onUpdateEvent:[" + -l_2_I + "]");
                                    iUpdateListener.onUpdateEvent(-l_6_R, -l_2_I);
                                }
                                -l_4_R.set(true);
                                -l_6_R.errorCode = -l_2_I;
                                -l_6_R.errorMsg = bundle.getString("key_errorMsg");
                                -l_6_R.downSize = bundle.getInt("key_downSize");
                                -l_6_R.fileSize = bundle.getInt("key_total");
                                -l_6_R.sdcardStatus = bundle.getInt("key_sdcardstatus");
                                -l_6_R.downType = (byte) bundle.getByte("key_downType");
                            }

                            public void b(Bundle bundle) {
                            }
                        });
                        tmsdkobf.lx.a aVar = null;
                        if (-l_6_R.flag == UpdateConfig.UPDATA_FLAG_NUM_MARK) {
                            aVar = new tmsdkobf.lx.a(this) {
                                final /* synthetic */ a JW;

                                public boolean bS(String -l_3_R) {
                                    aj -l_2_R = (aj) -l_6_R.data1;
                                    if (-l_2_R == null) {
                                        return true;
                                    }
                                    Log.e("UpdateMgr", "isMatch confSrc.md5Bin = " + (-l_2_R.aF != null ? Arrays.toString(-l_2_R.aF) : "null"));
                                    String -l_4_R = lq.bytesToString(-l_2_R.aF);
                                    if (-l_2_R.bl == 2) {
                                        Object -l_10_R = new File(-l_3_R).getParentFile().getPath() + File.separator + "tempZip" + File.separator;
                                        Object -l_11_R;
                                        try {
                                            -l_11_R = new File(-l_10_R);
                                            if (-l_11_R.exists()) {
                                                kl.b(-l_11_R);
                                            }
                                            gd.b(-l_3_R, -l_10_R);
                                            Object -l_12_R = -l_11_R.listFiles();
                                            if (-l_12_R != null) {
                                                if (-l_12_R.length != 0) {
                                                    if (-l_12_R.length != 1) {
                                                        return false;
                                                    }
                                                    Object -l_13_R = ".zip.tmp";
                                                    -l_3_R = -l_3_R.length() <= -l_13_R.length() ? -l_3_R + ".tmp" : -l_3_R.substring(0, -l_3_R.length() - -l_13_R.length());
                                                    try {
                                                        Object -l_14_R = new File(-l_3_R);
                                                        if (-l_14_R.exists()) {
                                                            -l_14_R.delete();
                                                        }
                                                        kl.copyFile(-l_12_R[0], -l_14_R);
                                                        kl.b(-l_11_R);
                                                    } catch (Exception e) {
                                                        -l_11_R = e;
                                                        f.b("UpdateMgr", "unzip num mark file failed", -l_11_R);
                                                        return false;
                                                    }
                                                }
                                            }
                                            return false;
                                        } catch (Exception e2) {
                                            -l_11_R = e2;
                                            Object obj = null;
                                            f.b("UpdateMgr", "unzip num mark file failed", -l_11_R);
                                            return false;
                                        }
                                    } else if (-l_2_R.bl != 1) {
                                        f.f("UpdateMgr", "normal num mark file");
                                    } else {
                                        f.e("UpdateMgr", "num mark file should not zip encrypt");
                                        return false;
                                    }
                                    if (TextUtils.isEmpty(-l_3_R)) {
                                        return false;
                                    }
                                    int -l_8_I = this.JW.B(-l_3_R, -l_4_R);
                                    f.f(NumMarker.Tag, "DataMd5Cheker isMatch() isMth: " + -l_8_I);
                                    return -l_8_I;
                                }
                            };
                        }
                        f.d("UpdateMgr", "before invoke httpGetFile.doGetFile()");
                        do {
                            -l_9_I = -l_7_R.a(null, -l_6_R.url, false, aVar);
                        } while (-l_9_I == -7);
                        if (!-l_4_R.get() && -l_6_R.flag == 2 && -l_9_I == 0) {
                            a(-l_6_R);
                        }
                        if (!-l_4_R.get() && 40458 == -l_6_R.mFileID && -l_9_I == 0) {
                            b(-l_6_R);
                        }
                        if (!-l_4_R.get() && UpdateConfig.getLargeMarkFileId() == -l_6_R.mFileID && -l_9_I == 0) {
                            c(-l_6_R);
                        }
                        if (-l_9_I != 0) {
                            -l_3_I = 0;
                            -l_6_R.errorCode = -l_9_I;
                        }
                    }
                    if (-l_4_R.get()) {
                        -l_6_R.success = (byte) 0;
                        -l_6_R.downnetType = (byte) i.iG().value();
                        -l_6_R.downNetName = i.getNetworkName();
                        -l_6_R.rssi = u.aK(5);
                    } else {
                        d(-l_6_R);
                        -l_6_R.success = (byte) 1;
                        -l_7_R = UpdateConfig.getLargeMarkFileId() != -l_6_R.mFileID ? 40458 != -l_6_R.mFileID ? 40461 != -l_6_R.mFileID ? 50001 != -l_6_R.mFileID ? E(-l_6_R.flag) : r.m(50001, r.b(this.mContext, 50001, ".sdb")) : r.n(40461, r.b(this.mContext, 40461, ".sdb")) : bJ(40458) : bJ(UpdateConfig.getLargeMarkFileId());
                        if (-l_7_R != null) {
                            -l_6_R.checkSum = lq.bytesToHexString(-l_7_R.aF);
                            -l_6_R.timestamp = -l_7_R.timestamp;
                        }
                    }
                    this.JT.e(-l_6_R);
                    if (this.JR.get()) {
                        if (iUpdateListener != null) {
                            f.d("UpdateMgr", "[callback]onUpdateCanceled");
                            iUpdateListener.onUpdateCanceled();
                        }
                        if (iUpdateListener != null) {
                            f.d("UpdateMgr", "[callback]onUpdateFinished");
                            iUpdateListener.onUpdateFinished();
                            this.JT.en();
                        }
                        return -l_3_I;
                    }
                }
            }
            if (iUpdateListener != null) {
                f.d("UpdateMgr", "[callback]onUpdateFinished");
                iUpdateListener.onUpdateFinished();
                this.JT.en();
            }
            return -l_3_I;
        }
    }
}
