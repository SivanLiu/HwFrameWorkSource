package com.android.server.wifi.LAA;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class HwLaaController {
    private static String TAG = "LAA_HwLaaController";
    private static HwLaaController mHwLaaController;
    Object hwTelephonyManagerProxy;
    Class<?> hwTelephonyManagerProxyClass;
    private Context mContext;
    private HwLaaCellStatusObserver mHwLaaCellStatusObserver;
    private HwLaaContentAware mHwLaaContentAware;
    private Handler mHwLaaControllerHandler;
    private boolean mIsMobileDataEnabled;
    private SparseArray<Integer> mLaaContorlVoteRecords;
    private int mLastRequestCmd = -1;

    public static HwLaaController createHwLaaController(Context context) {
        if (mHwLaaController == null) {
            mHwLaaController = new HwLaaController(context);
            HwLaaUtils.logD(TAG, "createHwLaaController");
        }
        return mHwLaaController;
    }

    public static HwLaaController getInstrance() {
        if (HwLaaUtils.isLaaPlusEnable()) {
            return mHwLaaController;
        }
        HwLaaUtils.logD(TAG, "HwLaaController getInstrance is null");
        return null;
    }

    private HwLaaController(Context context) {
        this.mContext = context;
        try {
            this.hwTelephonyManagerProxyClass = Class.forName("android.telephony.HwTelephonyManager");
            this.hwTelephonyManagerProxy = this.hwTelephonyManagerProxyClass.newInstance();
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception,e:");
            stringBuilder.append(e.getMessage());
            HwLaaUtils.logD(str, stringBuilder.toString());
        }
        initHwLaaContentAwareHandler();
        initialLaaContorlVoteRecords();
        this.mHwLaaContentAware = new HwLaaContentAware(this.mContext, this.mHwLaaControllerHandler);
        HwLaaWifiStatusObserver hwLaaWifiStatusObserver = new HwLaaWifiStatusObserver(this.mContext, this.mHwLaaControllerHandler);
        this.mHwLaaCellStatusObserver = new HwLaaCellStatusObserver(this.mContext, this.mHwLaaControllerHandler);
    }

    public synchronized boolean setLAAEnabled(boolean enable, int type) {
        return requestSendLaaCmd(enable, type);
    }

    /* JADX WARNING: Missing block: B:21:0x005b, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean requestSendLaaCmd(int cmd, int type) {
        if (!HwLaaUtils.isLaaPlusEnable()) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestSendLaaCmd type: ");
        stringBuilder.append(type);
        stringBuilder.append(" , cmd:");
        stringBuilder.append(cmd);
        HwLaaUtils.logD(str, stringBuilder.toString());
        if (requestIsLegal(cmd, type)) {
            this.mLastRequestCmd = cmd;
            this.mLaaContorlVoteRecords.put(type, Integer.valueOf(cmd));
            if (cmd == 0) {
                this.mLastRequestCmd = 0;
                sendLaaCmdToRil(this.mLastRequestCmd);
            } else if (isVoteRequestEnableLaa()) {
                this.mLastRequestCmd = 1;
                sendLaaCmdToRil(this.mLastRequestCmd);
            } else {
                HwLaaUtils.logD(TAG, "Can Not sendLaaCmdToRil ");
                return false;
            }
        }
        HwLaaUtils.logD(TAG, "request is illegal !!");
        return false;
    }

    public synchronized int getLaaDetailedState() {
        if (!HwLaaUtils.isLaaPlusEnable()) {
            return -1;
        }
        return this.mHwLaaCellStatusObserver.getLaaDetailedState();
    }

    private void initialLaaContorlVoteRecords() {
        this.mLaaContorlVoteRecords = new SparseArray();
        this.mLaaContorlVoteRecords.append(1, Integer.valueOf(1));
        this.mLaaContorlVoteRecords.append(2, Integer.valueOf(1));
        this.mLaaContorlVoteRecords.append(3, Integer.valueOf(1));
        this.mLaaContorlVoteRecords.append(4, Integer.valueOf(1));
        this.mLaaContorlVoteRecords.append(5, Integer.valueOf(1));
    }

    private boolean requestIsLegal(int cmd, int type) {
        if (type > 5 || type < 1) {
            return false;
        }
        return cmd == 1 || cmd == 0;
    }

    private boolean isVoteRequestEnableLaa() {
        if (this.mLaaContorlVoteRecords == null || this.mLaaContorlVoteRecords.size() == 0) {
            HwLaaUtils.logD(TAG, " mLaaContorlVoteRecords is null,Can not EnableLaa");
            return false;
        }
        int size = this.mLaaContorlVoteRecords.size();
        int key = 0;
        for (int i = 0; i < size; i++) {
            key = this.mLaaContorlVoteRecords.keyAt(i);
            if (((Integer) this.mLaaContorlVoteRecords.get(key)).intValue() == 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(key);
                stringBuilder.append(" opposes enabling LAA!");
                HwLaaUtils.logD(str, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    private void initHwLaaContentAwareHandler() {
        this.mHwLaaControllerHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        HwLaaController.this.requestSendLaaCmd(msg.arg1, msg.arg2);
                        return;
                    case 2:
                        if (HwLaaController.this.mLastRequestCmd != -1) {
                            HwLaaController.this.sendLaaCmdToRil(HwLaaController.this.mLastRequestCmd);
                            return;
                        }
                        return;
                    case 3:
                        if (msg.arg2 == 0) {
                            HwLaaContentAware access$200 = HwLaaController.this.mHwLaaContentAware;
                            boolean z = true;
                            if (msg.arg1 != 1) {
                                z = false;
                            }
                            access$200.setLaaContentAwareEnabled(z);
                            return;
                        }
                        return;
                    case 4:
                        HwLaaController.this.mIsMobileDataEnabled = HwLaaController.this.mHwLaaCellStatusObserver.getMobileDataEnabled();
                        if (!HwLaaController.this.mIsMobileDataEnabled) {
                            HwLaaController.this.mHwLaaContentAware.setLaaContentAwareEnabled(HwLaaController.this.mIsMobileDataEnabled);
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    private void sendLaaCmdToRil(int cmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("trysendLaaCmdToRil,cmd = ");
        stringBuilder.append(cmd);
        HwLaaUtils.logD(str, stringBuilder.toString());
        if (this.mHwLaaCellStatusObserver.isPermitSendLaaCmd()) {
            if ((cmd == 1 || cmd == 0) && !sendLaaCmdToTelephony(cmd)) {
                HwLaaUtils.logW(TAG, "sendLaaCmdToRil is fail");
            }
            return;
        }
        HwLaaUtils.logW(TAG, "ServiceState is not permit send laa cmd");
    }

    private boolean sendLaaCmdToTelephony(int cmd) {
        String str;
        StringBuilder stringBuilder;
        if (this.hwTelephonyManagerProxyClass == null || this.hwTelephonyManagerProxy == null) {
            HwLaaUtils.logD(TAG, "[hwTelephonyManagerProxyClass == null]");
            return false;
        }
        Method method = null;
        Object result = null;
        try {
            method = this.hwTelephonyManagerProxyClass.getMethod("sendLaaCmd", new Class[]{Integer.TYPE, String.class, Message.class});
        } catch (NoSuchMethodException e1) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("NoSuchMethodException:");
            stringBuilder2.append(e1.getMessage());
            HwLaaUtils.logD(str2, stringBuilder2.toString());
        }
        if (method != null) {
            try {
                result = method.invoke(this.hwTelephonyManagerProxy, new Object[]{Integer.valueOf(cmd), null, null});
            } catch (IllegalAccessException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IllegalAccessException:");
                stringBuilder.append(e.getMessage());
                HwLaaUtils.logD(str, stringBuilder.toString());
            } catch (IllegalArgumentException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IllegalArgumentException:");
                stringBuilder.append(e2.getMessage());
                HwLaaUtils.logD(str, stringBuilder.toString());
            } catch (InvocationTargetException e22) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("InvocationTargetException:");
                stringBuilder.append(e22.getMessage());
                HwLaaUtils.logD(str, stringBuilder.toString());
            }
            if (result != null) {
                return ((Boolean) result).booleanValue();
            }
            HwLaaUtils.logD(TAG, "[HwTelephonyManager+]sendLaaCmd,result==null!");
            return false;
        }
        HwLaaUtils.logD(TAG, "[HwTelephonyManager+]sendLaaCmd,method==null!");
        return false;
    }
}
