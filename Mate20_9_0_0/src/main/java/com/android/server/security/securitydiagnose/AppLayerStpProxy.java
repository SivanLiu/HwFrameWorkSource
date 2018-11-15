package com.android.server.security.securitydiagnose;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IHwBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import vendor.huawei.hardware.hwstp.V1_0.IHwStp;
import vendor.huawei.hardware.hwstp.V1_0.IHwStp.stpGetStatusByCategoryCallback;
import vendor.huawei.hardware.hwstp.V1_0.IHwStp.stpGetStatusByIdCallback;
import vendor.huawei.hardware.hwstp.V1_0.IHwStp.stpGetStatusCallback;
import vendor.huawei.hardware.hwstp.V1_0.StpItem;

public class AppLayerStpProxy {
    private static final boolean HW_DEBUG;
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
    private static final int STP_ID_KCODE = 897;
    private static final int STP_ID_KCODE_SYSCALL = 898;
    private static final int STP_ID_PROP = 768;
    private static final int STP_ID_ROOT_PROCS = 901;
    private static final int STP_ID_RW = 773;
    private static final int STP_ID_SETIDS = 896;
    private static final int STP_ID_SE_ENFROCING = 899;
    private static final int STP_ID_SE_HOOK = 900;
    private static final int STP_ID_SU = 772;
    private static final int STP_ID_VB = 512;
    private static final String TAG = AppLayerStpProxy.class.getSimpleName();
    private static final int TRY_GET_HIDL_DEAMON_DEALY_MILLIS = 1000;
    private static final int[] itemId = new int[]{STP_ID_KCODE, STP_ID_KCODE_SYSCALL, 900, STP_ID_SE_ENFROCING, STP_ID_SU, STP_ID_RW, 512, 768, STP_ID_SETIDS, 901};
    private static final int[] itemMark = new int[]{1, 2, 4, 8, 16, 32, 128, 256, 512, 1024};
    private static AppLayerStpProxy mInstance;
    private Context mContext;
    private IHwStp mHwStp;
    private final HwStpHandler mHwStpHandler;
    private final HandlerThread mHwStpHandlerThread;
    private DeathRecipient mStpHidlDeamonDeathRecipient = new DeathRecipient() {
        public void serviceDied(long cookie) {
            if (AppLayerStpProxy.this.mHwStpHandler != null) {
                Log.e(AppLayerStpProxy.TAG, "stp hidl deamon service has died, try to reconnect it later.");
                AppLayerStpProxy.this.mHwStp = null;
                AppLayerStpProxy.this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            }
        }
    };
    private int mStpHidlDeamonRegisterTryTimes = 0;
    private int stpGetItemStatusRetValue = 0;
    private int stpGetStatusByCategoryRetValue = -1001;
    private int stpGetStatusByIDRetValue = -1001;
    private int stpGetStatusRetValue = -1001;

    private final class HwStpHandler extends Handler {
        public HwStpHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String access$100;
            if (msg.what != 1) {
                access$100 = AppLayerStpProxy.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handler thread received unknown message : ");
                stringBuilder.append(msg.what);
                Log.e(access$100, stringBuilder.toString());
                return;
            }
            try {
                AppLayerStpProxy.this.mHwStp = IHwStp.getService(AppLayerStpProxy.STP_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(AppLayerStpProxy.TAG, "Try get stp hidl deamon servcie failed in handler message.");
            }
            if (AppLayerStpProxy.this.mHwStp != null) {
                AppLayerStpProxy.this.mStpHidlDeamonRegisterTryTimes = 0;
                try {
                    AppLayerStpProxy.this.mHwStp.linkToDeath(AppLayerStpProxy.this.mStpHidlDeamonDeathRecipient, 0);
                } catch (Exception e2) {
                    Log.e(AppLayerStpProxy.TAG, "Exception occured when linkToDeath in handle message");
                }
            } else {
                AppLayerStpProxy.this.mStpHidlDeamonRegisterTryTimes = AppLayerStpProxy.this.mStpHidlDeamonRegisterTryTimes + 1;
                if (AppLayerStpProxy.this.mStpHidlDeamonRegisterTryTimes < 10) {
                    access$100 = AppLayerStpProxy.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("stp hidl daemon service is not ready, try times : ");
                    stringBuilder2.append(AppLayerStpProxy.this.mStpHidlDeamonRegisterTryTimes);
                    Log.e(access$100, stringBuilder2.toString());
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

    static /* synthetic */ int access$876(AppLayerStpProxy x0, int x1) {
        int i = x0.stpGetItemStatusRetValue | x1;
        x0.stpGetItemStatusRetValue = i;
        return i;
    }

    static {
        boolean z = Log.HWINFO || RS_DEBUG || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HW_DEBUG = z;
    }

    private AppLayerStpProxy(Context context) {
        this.mContext = context;
        this.mHwStpHandlerThread = new HandlerThread(TAG);
        this.mHwStpHandlerThread.start();
        this.mHwStpHandler = new HwStpHandler(this.mHwStpHandlerThread.getLooper());
    }

    public static void init(Context context) {
        synchronized (AppLayerStpProxy.class) {
            if (mInstance == null) {
                mInstance = new AppLayerStpProxy(context);
            }
        }
    }

    public static AppLayerStpProxy getInstance() {
        AppLayerStpProxy appLayerStpProxy;
        synchronized (AppLayerStpProxy.class) {
            appLayerStpProxy = mInstance;
        }
        return appLayerStpProxy;
    }

    public int getRootStatusSync() {
        int ret;
        boolean hasExecption = false;
        if (this.mHwStp == null) {
            try {
                this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get stp hidl deamon servcie failed when get root status sync.");
            }
        }
        if (this.mHwStp != null) {
            try {
                this.mHwStp.stpGetStatusByCategory(8, false, false, new stpGetStatusByCategoryCallback() {
                    public void onValues(int stpGetStatusByCategoryRet, String out_buff) {
                        if (AppLayerStpProxy.HW_DEBUG) {
                            String access$100 = AppLayerStpProxy.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("sync get root status from hidl ret : ");
                            stringBuilder.append(stpGetStatusByCategoryRet);
                            stringBuilder.append(" out_buf : ");
                            stringBuilder.append(out_buff);
                            Log.d(access$100, stringBuilder.toString());
                        }
                        AppLayerStpProxy.this.stpGetStatusByCategoryRetValue = stpGetStatusByCategoryRet;
                    }
                });
            } catch (RemoteException e2) {
                hasExecption = true;
                Log.e(TAG, "sync get root status from stp hidl failed.");
            }
            ret = hasExecption ? -1002 : this.stpGetStatusByCategoryRetValue;
        } else {
            ret = -1000;
            this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "stp hidl deamon is not ready when sync get root status from stp hidl");
        }
        if (HW_DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sync get root status , stp hidl ret : ");
            stringBuilder.append(ret);
            Log.d(str, stringBuilder.toString());
        }
        return ret;
    }

    public int getSystemStatusSync() {
        int ret;
        boolean hasExecption = false;
        if (this.mHwStp == null) {
            try {
                this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get stp hidl deamon servcie failed when get system status sync.");
            }
        }
        if (this.mHwStp != null) {
            try {
                this.mHwStp.stpGetStatus(false, false, new stpGetStatusCallback() {
                    public void onValues(int stpGetStatusRet, String out_buff) {
                        if (AppLayerStpProxy.HW_DEBUG) {
                            String access$100 = AppLayerStpProxy.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("sync get system status from hidl ret : ");
                            stringBuilder.append(stpGetStatusRet);
                            stringBuilder.append(" out_buf : ");
                            stringBuilder.append(out_buff);
                            Log.d(access$100, stringBuilder.toString());
                        }
                        AppLayerStpProxy.this.stpGetStatusRetValue = stpGetStatusRet;
                    }
                });
            } catch (RemoteException e2) {
                hasExecption = true;
                Log.e(TAG, "sync get system status from stp hidl failed.");
            }
            ret = hasExecption ? -1002 : this.stpGetStatusRetValue;
        } else {
            ret = -1000;
            this.mHwStpHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "stp hidl deamon is not ready when sync get system status from stp hidl");
        }
        if (HW_DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sync get system status , stp hidl ret : ");
            stringBuilder.append(ret);
            Log.d(str, stringBuilder.toString());
        }
        return ret;
    }

    public int sendThreatenInfo(int id, byte status, byte credible, byte version, String name, String additian_info) {
        int ret;
        if (HW_DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("receive the app's threaten info to stp hidl deamon, id: ");
            stringBuilder.append(id);
            stringBuilder.append(", status: ");
            stringBuilder.append(status);
            stringBuilder.append(", credible: ");
            stringBuilder.append(credible);
            stringBuilder.append(", name:");
            stringBuilder.append(name);
            stringBuilder.append(",additian_info:");
            stringBuilder.append(additian_info);
            Log.d(str, stringBuilder.toString());
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
            } catch (Exception e) {
                Log.e(TAG, "Try get stp hidl deamon servcie failed when send threaten info.");
            }
        }
        if (this.mHwStp != null) {
            try {
                ret = this.mHwStp.stpAddThreat(item, additian_info);
            } catch (RemoteException e2) {
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
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("send the app's threaten info to stp hidl deamon, ret : ");
            stringBuilder2.append(ret);
            Log.d(str2, stringBuilder2.toString());
        }
        return ret;
    }

    public int getEachItemRootStatus() {
        this.stpGetItemStatusRetValue = 0;
        if (this.mHwStp == null) {
            try {
                this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get stp hidl deamon servcie failed when get item root status sync.");
            }
        }
        for (int i = 0; i < itemId.length; i++) {
            int ret;
            String str;
            StringBuilder stringBuilder;
            boolean hasExecption = false;
            if (this.mHwStp != null) {
                try {
                    this.mHwStp.stpGetStatusById(itemId[i], true, false, new stpGetStatusByIdCallback() {
                        public void onValues(int stpGetStatusByIdRet, String out_buff) {
                            String access$100;
                            StringBuilder stringBuilder;
                            if (AppLayerStpProxy.HW_DEBUG) {
                                access$100 = AppLayerStpProxy.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("sync get item root status from hidl ret : ");
                                stringBuilder.append(stpGetStatusByIdRet);
                                stringBuilder.append(" out_buf : ");
                                stringBuilder.append(out_buff);
                                Log.d(access$100, stringBuilder.toString());
                            }
                            if (out_buff.contains(AppLayerStpProxy.STP_HIDL_MATCH_ROOTPROC) && out_buff.contains(AppLayerStpProxy.STP_HIDL_MATCH_CREDIBLE)) {
                                AppLayerStpProxy.access$876(AppLayerStpProxy.this, 64);
                                if (AppLayerStpProxy.HW_DEBUG) {
                                    access$100 = AppLayerStpProxy.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("adbd abnormal, stpGetItemStatusRetValue ");
                                    stringBuilder.append(AppLayerStpProxy.this.stpGetItemStatusRetValue);
                                    Log.d(access$100, stringBuilder.toString());
                                }
                            }
                            if (out_buff.contains(AppLayerStpProxy.STP_HIDL_MATCH_VB) && out_buff.contains(AppLayerStpProxy.STP_HIDL_MATCH_VB_RISK)) {
                                AppLayerStpProxy.access$876(AppLayerStpProxy.this, 128);
                                if (AppLayerStpProxy.HW_DEBUG) {
                                    access$100 = AppLayerStpProxy.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("verifyboot abnormal, stpGetItemStatusRetValue ");
                                    stringBuilder.append(AppLayerStpProxy.this.stpGetItemStatusRetValue);
                                    Log.d(access$100, stringBuilder.toString());
                                }
                            }
                            AppLayerStpProxy.this.stpGetStatusByIDRetValue = stpGetStatusByIdRet;
                            if (AppLayerStpProxy.HW_DEBUG) {
                                access$100 = AppLayerStpProxy.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("stpGetStatusByIDRetValue is ");
                                stringBuilder.append(AppLayerStpProxy.this.stpGetStatusByIDRetValue);
                                Log.d(access$100, stringBuilder.toString());
                            }
                        }
                    });
                } catch (RemoteException e2) {
                    hasExecption = true;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("get each item root status from stp hidl failed. id:");
                    stringBuilder2.append(itemId[i]);
                    Log.e(str2, stringBuilder2.toString());
                }
                ret = hasExecption ? -1002 : this.stpGetStatusByIDRetValue;
            } else {
                ret = -1000;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("stp hidl deamon is not ready when sync get item root status from stp hidl.id:");
                stringBuilder.append(itemId[i]);
                Log.e(str, stringBuilder.toString());
            }
            if (HW_DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("get item root status , stp hidl ret : ");
                stringBuilder.append(ret);
                stringBuilder.append(" itemId : ");
                stringBuilder.append(itemId[i]);
                Log.d(str, stringBuilder.toString());
            }
            if (ret < 0) {
                if (HW_DEBUG) {
                    Log.d(TAG, "stp get status by id failed");
                }
                return ret;
            }
            if (ret == 1) {
                this.stpGetItemStatusRetValue |= itemMark[i];
            }
            if (HW_DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("stpGetItemStatusRetValue ret : ");
                stringBuilder.append(this.stpGetItemStatusRetValue);
                stringBuilder.append(" itemId : ");
                stringBuilder.append(itemId[i]);
                Log.d(str, stringBuilder.toString());
            }
        }
        return this.stpGetItemStatusRetValue;
    }
}
