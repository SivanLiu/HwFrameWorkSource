package com.huawei.server.security.securitydiagnose;

import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import vendor.huawei.hardware.hwstp.V1_1.IHwStp;
import vendor.huawei.hardware.hwstp.V1_1.IHwStpKernelDetectionCallback;

/* access modifiers changed from: package-private */
public class KernelDetection {
    private static final int CMD_START = 1;
    private static final int CMD_STOP = 0;
    private static final int ERR_ARG = -1008;
    private static final int ERR_CNT = -1004;
    private static final int ERR_NULL = -1001;
    private static final int ERR_REG = -1002;
    private static final int ERR_START = -1005;
    private static final int ERR_STOP = -1006;
    private static final int ERR_UNREG = -1003;
    private static final int ERR_UPDATE = -1007;
    private static final int RET_SUCC = 0;
    private static final String STP_HIDL_SERVICE_NAME = "hwstp";
    private static final String TAG = "Module Kernel Detection";
    private volatile IHwStp mHwStp;
    private final Object mRegisterMutex;
    private final Object mStpMutex;
    private int mUidCount;

    private KernelDetection() {
        this.mRegisterMutex = new Object();
        this.mStpMutex = new Object();
        this.mHwStp = null;
        this.mUidCount = 0;
    }

    private static class SingleInstanceHolder {
        /* access modifiers changed from: private */
        public static final KernelDetection INSTANCE = new KernelDetection();

        private SingleInstanceHolder() {
        }
    }

    static KernelDetection getInstance() {
        return SingleInstanceHolder.INSTANCE;
    }

    /* access modifiers changed from: package-private */
    public int startInspection(int uid) {
        if (uid < 0) {
            return ERR_ARG;
        }
        initHwStp();
        if (this.mHwStp == null) {
            Log.e(TAG, "Start: mHwStp is null");
            return -1001;
        }
        int ret = ERR_START;
        synchronized (this.mRegisterMutex) {
            if (this.mUidCount >= 1 || (ret = registerKernelDetectionCallback()) == 0) {
                try {
                    int ret2 = this.mHwStp.stpTriggerKernelDetection(uid, 1);
                    if (ret2 != 0) {
                        Log.e(TAG, "Invoke hidl start failed ret = " + ret2);
                        return ERR_START;
                    }
                    this.mUidCount++;
                    return 0;
                } catch (RemoteException e) {
                    Log.e(TAG, "Start: RemoteException");
                    return ret;
                } catch (Exception e2) {
                    Log.e(TAG, "Start: Exception");
                    return ret;
                }
            } else {
                Log.e(TAG, "Register callback failed ret = " + ret);
                return -1002;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public int stopInspection(int uid) {
        if (uid < 0) {
            return ERR_ARG;
        }
        initHwStp();
        if (this.mHwStp == null) {
            return -1001;
        }
        int ret = ERR_STOP;
        try {
            ret = this.mHwStp.stpTriggerKernelDetection(uid, 0);
        } catch (RemoteException e) {
            Log.e(TAG, "Stop: RemoteException ret = " + ERR_STOP);
            return ERR_STOP;
        } catch (Exception e2) {
            Log.e(TAG, "Stop: Exception");
        }
        if (ret != 0) {
            return ERR_STOP;
        }
        synchronized (this.mRegisterMutex) {
            if (this.mUidCount > 1) {
                this.mUidCount--;
                return 0;
            } else if (this.mUidCount != 1) {
                return ERR_CNT;
            } else {
                int ret2 = unregisterKernelDetectionCallback();
                if (ret2 == 0) {
                    this.mUidCount--;
                    return 0;
                }
                Log.e(TAG, "Unregister callback failed ret = " + ret2);
                return ERR_UNREG;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public int updateKernelDetectionConfig(int[] conf) {
        if (conf == null || conf.length == 0) {
            return ERR_ARG;
        }
        initHwStp();
        if (this.mHwStp == null) {
            return -1001;
        }
        try {
            ArrayList<Integer> confList = new ArrayList<>(conf.length);
            for (int value : conf) {
                confList.add(Integer.valueOf(value));
            }
            int ret = this.mHwStp.stpUpdateKernelDetectionConfig(confList);
            if (ret != 0) {
                Log.e(TAG, "Update: ret = " + ret);
                return ERR_UPDATE;
            }
            Log.i(TAG, "Update configuration for kernel detection module succeed");
            return 0;
        } catch (RemoteException e) {
            Log.e(TAG, "Invoke hidl update failed ret = " + ERR_UPDATE);
            return ERR_UPDATE;
        } catch (Exception e2) {
            Log.e(TAG, "Update kernel detection config: Exception");
            return ERR_UPDATE;
        }
    }

    private void initHwStp() {
        if (this.mHwStp == null) {
            synchronized (this.mStpMutex) {
                if (this.mHwStp == null) {
                    try {
                        this.mHwStp = IHwStp.getService(STP_HIDL_SERVICE_NAME);
                    } catch (RemoteException e) {
                        Log.e(TAG, "RemoteException when initial mHwStp");
                    } catch (Exception e2) {
                        Log.e(TAG, "Exception when initial mHwStp");
                    }
                }
            }
        }
    }

    private int registerKernelDetectionCallback() {
        IHwStpKernelDetectionCallback callback = new KernelDetectionCallback();
        initHwStp();
        int ret = -1002;
        if (this.mHwStp != null) {
            try {
                ret = this.mHwStp.stpRegisterKernelDetectionCallback(callback);
            } catch (RemoteException e) {
                Log.e(TAG, "Invoke hidl register failed ret = " + -1002);
                return -1002;
            } catch (Exception e2) {
                Log.e(TAG, "Register: Exception");
                return -1002;
            }
        }
        if (ret == 0) {
            return 0;
        }
        Log.e(TAG, "Register: ret = " + ret);
        return -1002;
    }

    private int unregisterKernelDetectionCallback() {
        initHwStp();
        int ret = ERR_UNREG;
        if (this.mHwStp != null) {
            try {
                ret = this.mHwStp.stpUnregisterKernelDetectionCallback();
            } catch (RemoteException e) {
                Log.e(TAG, "Invoke hidl unregister failed ret = " + ERR_UNREG);
                return ERR_UNREG;
            } catch (Exception e2) {
                Log.e(TAG, "Unregister: Exception");
                return ERR_UNREG;
            }
        }
        if (ret == 0) {
            return 0;
        }
        Log.e(TAG, "Unregister: ret = " + ret);
        return ERR_UNREG;
    }
}
