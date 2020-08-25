package com.huawei.server.security.securitydiagnose;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import vendor.huawei.hardware.hwstp.V1_0.IHwStp;
import vendor.huawei.hardware.hwstp.V1_0.StpItem;

public class AppLayerStpProxy {
    /* access modifiers changed from: private */
    public static final boolean HW_DEBUG = (Log.HWINFO || RS_DEBUG || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final int[] ITEM_ID = {STP_ID_KCODE, STP_ID_KCODE_SYSCALL, STP_ID_SE_HOOK, STP_ID_SE_ENFROCING, STP_ID_SU, STP_ID_RW, 512, 768, STP_ID_SETIDS, 901};
    private static final int[] ITEM_MARK = {1, 2, 4, 8, 16, 32, 128, 256, 512, 1024};
    private static final int MAX_STP_HIDL_DEAMON_REGISTER_TIMES = 10;
    private static final int MSG_STP_HIDL_DEAMON_SERVIE_REGISTER = 1;
    private static final int RET_DEFAULT_ERROR_VALUE = -1001;
    private static final int RET_EXCEPTION_WHEN_STP_CALL = -1002;
    private static final int RET_STP_HIDL_DEAMON_IS_NOT_READY = -1000;
    private static final boolean RS_DEBUG = SystemProperties.get("ro.secure", "1").equals("0");
    private static final String STP_HIDL_MATCH_CREDIBLE = "credible: Y";
    private static final String STP_HIDL_MATCH_ROOTPROC = "root-procs";
    private static final String STP_HIDL_MATCH_VB = "verifyboot";
    private static final String STP_HIDL_MATCH_VB_RISK = "RISK";
    private static final String STP_HIDL_SERVICE_NAME = "hwstp";
    private static final int STP_ID_KCODE = 642;
    private static final int STP_ID_KCODE_SYSCALL = 643;
    private static final int STP_ID_PROP = 768;
    private static final int STP_ID_ROOT_PROCS = 901;
    private static final int STP_ID_RW = 773;
    private static final int STP_ID_SETIDS = 896;
    private static final int STP_ID_SE_ENFROCING = 899;
    private static final int STP_ID_SE_HOOK = 644;
    private static final int STP_ID_SU = 520;
    private static final int STP_ID_VERIFYBOOT = 512;
    private static final int STP_ITEM_ROOT = 1;
    /* access modifiers changed from: private */
    public static final String TAG = AppLayerStpProxy.class.getSimpleName();
    private static final int TRY_GET_HIDL_DEAMON_DEALY_MILLIS = 1000;
    private static AppLayerStpProxy sInstance;
    private Context mContext;
    /* access modifiers changed from: private */
    public IHwStp mHwStp;
    /* access modifiers changed from: private */
    public final HwStpHandler mHwStpHandler;
    private final HandlerThread mHwStpHandlerThread;
    private int mStpGetItemStatusRetValue = 0;
    /* access modifiers changed from: private */
    public String mStpGetStatusByCategoryOutBuff;
    /* access modifiers changed from: private */
    public int mStpGetStatusByCategoryRetValue = -1001;
    /* access modifiers changed from: private */
    public int mStpGetStatusByIDRetValue = -1001;
    /* access modifiers changed from: private */
    public int mStpGetStatusRetValue = -1001;
    /* access modifiers changed from: private */
    public IHwBinder.DeathRecipient mStpHidlDeamonDeathRecipient = new IHwBinder.DeathRecipient() {
        /* class com.huawei.server.security.securitydiagnose.AppLayerStpProxy.AnonymousClass1 */

        public void serviceDied(long cookie) {
            if (AppLayerStpProxy.this.mHwStpHandler != null) {
                Log.e(AppLayerStpProxy.TAG, "stp hidl deamon service has died, try to reconnect it later.");
                IHwStp unused = AppLayerStpProxy.this.mHwStp = null;
                AppLayerStpProxy.this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            }
        }
    };
    /* access modifiers changed from: private */
    public int mStpHidlDeamonRegisterTryTimes = 0;

    static /* synthetic */ int access$308(AppLayerStpProxy x0) {
        int i = x0.mStpHidlDeamonRegisterTryTimes;
        x0.mStpHidlDeamonRegisterTryTimes = i + 1;
        return i;
    }

    private AppLayerStpProxy(Context context) {
        this.mContext = context;
        this.mHwStpHandlerThread = new HandlerThread(TAG);
        this.mHwStpHandlerThread.start();
        this.mHwStpHandler = new HwStpHandler(this.mHwStpHandlerThread.getLooper());
    }

    public static void init(Context context) {
        synchronized (AppLayerStpProxy.class) {
            if (sInstance == null) {
                sInstance = new AppLayerStpProxy(context);
            }
        }
    }

    public static AppLayerStpProxy getInstance() {
        AppLayerStpProxy appLayerStpProxy;
        synchronized (AppLayerStpProxy.class) {
            appLayerStpProxy = sInstance;
        }
        return appLayerStpProxy;
    }

    /* access modifiers changed from: private */
    public final class HwStpHandler extends Handler {
        public HwStpHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what != 1) {
                String access$100 = AppLayerStpProxy.TAG;
                Log.e(access$100, "handler thread received unknown message : " + msg.what);
                return;
            }
            try {
                IHwStp unused = AppLayerStpProxy.this.mHwStp = IHwStp.getService(AppLayerStpProxy.STP_HIDL_SERVICE_NAME);
            } catch (RemoteException e) {
                Log.e(AppLayerStpProxy.TAG, "get stp hidl remote exception in handler message.");
            } catch (Exception e2) {
                Log.e(AppLayerStpProxy.TAG, "get stp hidl exception in handler message.");
            }
            if (AppLayerStpProxy.this.mHwStp != null) {
                int unused2 = AppLayerStpProxy.this.mStpHidlDeamonRegisterTryTimes = 0;
                try {
                    AppLayerStpProxy.this.mHwStp.linkToDeath(AppLayerStpProxy.this.mStpHidlDeamonDeathRecipient, 0);
                } catch (RemoteException e3) {
                    Log.e(AppLayerStpProxy.TAG, "remote exception occured when linkToDeath in handle message");
                } catch (Exception e4) {
                    Log.e(AppLayerStpProxy.TAG, "exception occured when linkToDeath in handle message");
                }
            } else {
                AppLayerStpProxy.access$308(AppLayerStpProxy.this);
                if (AppLayerStpProxy.this.mStpHidlDeamonRegisterTryTimes < 10) {
                    String access$1002 = AppLayerStpProxy.TAG;
                    Log.e(access$1002, "stp hidl daemon service is not ready, try times : " + AppLayerStpProxy.this.mStpHidlDeamonRegisterTryTimes);
                    AppLayerStpProxy.this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
                } else {
                    Log.e(AppLayerStpProxy.TAG, "stp hidl daemon service connection failed.");
                }
            }
            if (AppLayerStpProxy.HW_DEBUG) {
                Log.d(AppLayerStpProxy.TAG, "handler thread received request stp hidl deamon message.");
            }
        }
    }

    public int getRootStatusSync() {
        int ret;
        if (this.mHwStp == null) {
            try {
                this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
            } catch (RemoteException e) {
                Log.e(TAG, "get stp hidl remote exception when get root status sync.");
            } catch (Exception e2) {
                Log.e(TAG, "get stp hidl exception when get root status sync.");
            }
        }
        IHwStp iHwStp = this.mHwStp;
        if (iHwStp != null) {
            try {
                iHwStp.stpGetStatusByCategory(8, false, false, new IHwStp.stpGetStatusByCategoryCallback() {
                    /* class com.huawei.server.security.securitydiagnose.AppLayerStpProxy.AnonymousClass2 */

                    @Override // vendor.huawei.hardware.hwstp.V1_0.IHwStp.stpGetStatusByCategoryCallback
                    public void onValues(int stpGetStatusByCategoryRet, String outBuffer) {
                        if (AppLayerStpProxy.HW_DEBUG) {
                            String access$100 = AppLayerStpProxy.TAG;
                            Log.d(access$100, "sync get root status from hidl ret : " + stpGetStatusByCategoryRet + " outBuffer : " + outBuffer);
                        }
                        int unused = AppLayerStpProxy.this.mStpGetStatusByCategoryRetValue = stpGetStatusByCategoryRet;
                    }
                });
                ret = this.mStpGetStatusByCategoryRetValue;
            } catch (RemoteException e3) {
                ret = -1002;
                Log.e(TAG, "sync get root status from stp hidl failed.");
            }
        } else {
            ret = -1000;
            this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "stp hidl deamon is not ready when sync get root status from stp hidl");
        }
        if (HW_DEBUG) {
            String str = TAG;
            Log.d(str, "sync get root status , stp hidl ret : " + ret);
        }
        return ret;
    }

    public int getSystemStatusSync() {
        int ret;
        if (this.mHwStp == null) {
            try {
                this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
            } catch (RemoteException e) {
                Log.e(TAG, "get stp hidl remote exception when get system status sync.");
            } catch (Exception e2) {
                Log.e(TAG, "get stp hidl exception when get system status sync.");
            }
        }
        IHwStp iHwStp = this.mHwStp;
        if (iHwStp != null) {
            try {
                iHwStp.stpGetStatus(false, false, new IHwStp.stpGetStatusCallback() {
                    /* class com.huawei.server.security.securitydiagnose.AppLayerStpProxy.AnonymousClass3 */

                    @Override // vendor.huawei.hardware.hwstp.V1_0.IHwStp.stpGetStatusCallback
                    public void onValues(int stpGetStatusRet, String outBuffer) {
                        if (AppLayerStpProxy.HW_DEBUG) {
                            String access$100 = AppLayerStpProxy.TAG;
                            Log.d(access$100, "sync get system status from hidl ret : " + stpGetStatusRet + " outBuffer : " + outBuffer);
                        }
                        int unused = AppLayerStpProxy.this.mStpGetStatusRetValue = stpGetStatusRet;
                    }
                });
                ret = this.mStpGetStatusRetValue;
            } catch (RemoteException e3) {
                ret = -1002;
                Log.e(TAG, "sync get system status from stp hidl failed.");
            }
        } else {
            ret = -1000;
            this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "stp hidl deamon is not ready when sync get system status from stp hidl");
        }
        if (HW_DEBUG) {
            String str = TAG;
            Log.d(str, "sync get system status , stp hidl ret : " + ret);
        }
        return ret;
    }

    public int sendThreatenInfo(int id, byte status, byte credible, byte version, String name, String addition) {
        int ret;
        if (HW_DEBUG) {
            String str = TAG;
            Log.d(str, "receive the app's threaten info to stp hidl deamon , status: " + ((int) status) + ", credible: " + ((int) credible) + ", name:" + name + ", addition:" + addition);
        }
        StpItem item = new StpItem();
        item.id = id;
        item.status = status;
        item.credible = credible;
        item.version = version;
        item.name = name;
        if (this.mHwStp == null) {
            try {
                this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
            } catch (RemoteException e) {
                Log.e(TAG, "get stp hidl remote exception when send threaten info.");
            } catch (Exception e2) {
                Log.e(TAG, "get stp hidl exception when send threaten info.");
            }
        }
        IHwStp iHwStp = this.mHwStp;
        if (iHwStp != null) {
            try {
                ret = iHwStp.stpAddThreat(item, addition);
            } catch (RemoteException e3) {
                Log.e(TAG, "sync send threaten info to stp hidl failed.");
                ret = -1002;
            }
        } else {
            ret = -1000;
            this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "stp hidl deamon is not ready when app send threaten info to stp hidl deamon");
        }
        if (HW_DEBUG) {
            String str2 = TAG;
            Log.d(str2, "send the app's threaten info to stp hidl deamon, ret : " + ret);
        }
        return ret;
    }

    /* access modifiers changed from: private */
    public void checkAdbd(String outBuffer) {
        if (outBuffer.contains(STP_HIDL_MATCH_ROOTPROC) && outBuffer.contains(STP_HIDL_MATCH_CREDIBLE)) {
            this.mStpGetItemStatusRetValue |= 64;
            if (HW_DEBUG) {
                Log.d(TAG, "adbd abnormal, stpGetItemStatusRetValue " + this.mStpGetItemStatusRetValue);
            }
        }
    }

    /* access modifiers changed from: private */
    public void checkVerifyboot(String outBuffer) {
        if (outBuffer.contains(STP_HIDL_MATCH_VB) && outBuffer.contains(STP_HIDL_MATCH_VB_RISK)) {
            this.mStpGetItemStatusRetValue |= 128;
            if (HW_DEBUG) {
                Log.d(TAG, "verifyboot abnormal, stpGetItemStatusRetValue " + this.mStpGetItemStatusRetValue);
            }
        }
    }

    public int getEachItemRootStatus() {
        int ret;
        if (this.mHwStp == null) {
            try {
                this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
            } catch (RemoteException e) {
                Log.e(TAG, "get stp hidl remote exception when get item root status sync.");
            } catch (Exception e2) {
                Log.e(TAG, "get stp hidl exception when get item root status sync.");
            }
        }
        this.mStpGetItemStatusRetValue = 0;
        if (this.mHwStp == null) {
            this.mStpGetItemStatusRetValue = -1000;
            this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "stp hidl deamon is not ready when sync get item root status from stp");
            return this.mStpGetItemStatusRetValue;
        }
        int i = 0;
        while (true) {
            int[] iArr = ITEM_ID;
            if (i >= iArr.length) {
                return this.mStpGetItemStatusRetValue;
            }
            try {
                this.mHwStp.stpGetStatusById(iArr[i], true, false, new IHwStp.stpGetStatusByIdCallback() {
                    /* class com.huawei.server.security.securitydiagnose.AppLayerStpProxy.AnonymousClass4 */

                    @Override // vendor.huawei.hardware.hwstp.V1_0.IHwStp.stpGetStatusByIdCallback
                    public void onValues(int stpGetStatusByIdRet, String outBuffer) {
                        if (outBuffer == null) {
                            Log.e(AppLayerStpProxy.TAG, "parameter outBuffer is null");
                        } else {
                            if (AppLayerStpProxy.HW_DEBUG) {
                                String access$100 = AppLayerStpProxy.TAG;
                                Log.d(access$100, "sync get item root status from hidl ret : " + stpGetStatusByIdRet + " outBuffer : " + outBuffer);
                            }
                            AppLayerStpProxy.this.checkAdbd(outBuffer);
                            AppLayerStpProxy.this.checkVerifyboot(outBuffer);
                        }
                        if (AppLayerStpProxy.HW_DEBUG) {
                            String access$1002 = AppLayerStpProxy.TAG;
                            Log.d(access$1002, "mStpGetStatusByIDRetValue is " + AppLayerStpProxy.this.mStpGetStatusByIDRetValue);
                        }
                        int unused = AppLayerStpProxy.this.mStpGetStatusByIDRetValue = stpGetStatusByIdRet;
                    }
                });
                ret = this.mStpGetStatusByIDRetValue;
            } catch (RemoteException e3) {
                Log.e(TAG, "get each item root status from stp hidl failed. ITEM_ID :" + ITEM_ID[i]);
                ret = -1002;
            }
            if (HW_DEBUG) {
                Log.d(TAG, "get item root status , stp hidl ret : " + ret + " ITEM_ID : " + ITEM_ID[i]);
            }
            if (ret < 0) {
                if (HW_DEBUG) {
                    Log.d(TAG, "stp get status by id failed");
                }
                return ret;
            }
            if (ret == 1) {
                this.mStpGetItemStatusRetValue |= ITEM_MARK[i];
            } else {
                Log.i(TAG, "get item root status from stp hidl is normal");
            }
            if (HW_DEBUG) {
                Log.d(TAG, "mStpGetItemStatusRetValue ret : " + this.mStpGetItemStatusRetValue + " ITEM_ID : " + ITEM_ID[i]);
            }
            i++;
        }
    }

    public int getStpStatusByCategory(int category, boolean inDetail, boolean withHistory, char[] outBuff, int[] outBuffLen) {
        if (this.mHwStp == null) {
            try {
                this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
            } catch (RemoteException e) {
                Log.e(TAG, "get stp hidl remote exception when get status by category.");
            } catch (Exception e2) {
                Log.e(TAG, "get stp hidl exception when get status by category.");
            }
        }
        IHwStp iHwStp = this.mHwStp;
        if (iHwStp == null) {
            this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "stp hidl deamon is not ready when get status by category");
            return -1000;
        }
        try {
            iHwStp.stpGetStatusByCategory(category, inDetail, withHistory, new IHwStp.stpGetStatusByCategoryCallback() {
                /* class com.huawei.server.security.securitydiagnose.AppLayerStpProxy.AnonymousClass5 */

                @Override // vendor.huawei.hardware.hwstp.V1_0.IHwStp.stpGetStatusByCategoryCallback
                public void onValues(int stpGetStatusByCategoryRet, String stpOutBuff) {
                    if (AppLayerStpProxy.HW_DEBUG) {
                        String access$100 = AppLayerStpProxy.TAG;
                        Log.d(access$100, "sync get status by category from hidl ret : " + stpGetStatusByCategoryRet + "stpOutBuff : " + stpOutBuff);
                    }
                    int unused = AppLayerStpProxy.this.mStpGetStatusByCategoryRetValue = stpGetStatusByCategoryRet;
                    String unused2 = AppLayerStpProxy.this.mStpGetStatusByCategoryOutBuff = stpOutBuff;
                }
            });
            int ret = this.mStpGetStatusByCategoryRetValue;
            char[] temp = this.mStpGetStatusByCategoryOutBuff.toCharArray();
            if ((inDetail || withHistory) && outBuff != null) {
                outBuffLen[0] = outBuff.length <= temp.length ? outBuff.length : temp.length;
                System.arraycopy(temp, 0, outBuff, 0, outBuffLen[0]);
                return ret;
            }
            outBuffLen[0] = 0;
            return ret;
        } catch (RemoteException e3) {
            Log.e(TAG, "get status by category from stp hidl failed.");
            return -1002;
        }
    }
}
