package com.huawei.motiondetection;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import com.huawei.motiondetection.motionrelay.RelayListener;
import com.huawei.motiondetection.motionrelay.RelayManager;
import java.util.ArrayList;
import java.util.List;

public class MotionDetectionManager {
    private static final String MOTION_SERVICE_APK_ACTION = "com.huawei.action.MOTION_SETTINGS";
    private static final String TAG = "MotionDetectionManager";
    private Context mContext = null;
    private boolean mDestroyed = false;
    private ArrayList<MotionDetectionListener> mMDListenerList = null;
    private ArrayList<Integer> mMotionAppsRegList = null;
    private RelayListener mRelayListener = new RelayListener() {
        public void notifyResult(int relayType, Object mrecoRes) {
            MotionDetectionManager.this.processMotionRecoResult(relayType, mrecoRes);
        }
    };
    private RelayManager mRelayManager = null;

    public static boolean isMotionRecoApkExist(Context context) {
        List<ResolveInfo> packages = context.getApplicationContext().getPackageManager().queryIntentActivities(new Intent(MOTION_SERVICE_APK_ACTION), 0);
        if (packages != null && packages.size() != 0) {
            return true;
        }
        MRLog.e("MotionRecoApkCheck", "Motion service not installed, it can not do motion recognize.");
        return false;
    }

    public MotionDetectionManager(Context context) {
        this.mContext = context;
        this.mRelayManager = new RelayManager(this.mContext);
        this.mRelayManager.setRelayListener(this.mRelayListener);
        this.mMDListenerList = new ArrayList();
        this.mMotionAppsRegList = new ArrayList();
    }

    public MotionDetectionManager(Context context, boolean isEx) {
        this.mContext = context;
        this.mRelayManager = new RelayManager(this.mContext, isEx);
        this.mRelayManager.setRelayListener(this.mRelayListener);
        this.mMDListenerList = new ArrayList();
        this.mMotionAppsRegList = new ArrayList();
    }

    public void startMotionService() {
        if (this.mDestroyed) {
            MRLog.w(TAG, "startMotionService destroy called already ");
            return;
        }
        if (!MRUtils.isServiceRunning(this.mContext, MotionConfig.MOTION_SERVICE_PROCESS)) {
            this.mRelayManager.startMotionService();
        }
    }

    public void startMotionServiceAsUser(int userId) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "startMotionServiceAsUser destroy called already ");
            return;
        }
        if (!MRUtils.isServiceRunningAsUser(this.mContext, MotionConfig.MOTION_SERVICE_PROCESS, userId)) {
            this.mRelayManager.startMotionService();
        }
    }

    public void stopMotionService() {
        if (this.mDestroyed) {
            MRLog.w(TAG, "stopMotionService destroy called already ");
            return;
        }
        if (MRUtils.isServiceRunning(this.mContext, MotionConfig.MOTION_SERVICE_PROCESS)) {
            this.mRelayManager.stopMotionService();
        }
    }

    public void stopMotionServiceAsUser(int userId) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "stopMotionServiceAsUser destroy called already ");
            return;
        }
        if (MRUtils.isServiceRunningAsUser(this.mContext, MotionConfig.MOTION_SERVICE_PROCESS, userId)) {
            this.mRelayManager.stopMotionService();
        }
    }

    public void startMotionRecoTutorial(int motionApps) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "startMotionRecoTutorial destroy called already ");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startMotionRecoTutorial motionApps: ");
        stringBuilder.append(motionApps);
        MRLog.d(str, stringBuilder.toString());
        if (this.mMotionAppsRegList.contains(Integer.valueOf(motionApps))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startMotionRecoTutorial repeat motionApps ");
            stringBuilder.append(motionApps);
            MRLog.d(str, stringBuilder.toString());
            return;
        }
        int motionTypeReco = MotionTypeApps.getMotionTypeByMotionApps(motionApps);
        if (motionTypeReco == motionApps) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startMotionRecoTutorial motionApps ");
            stringBuilder2.append(motionApps);
            stringBuilder2.append(" is not supported.");
            MRLog.e(str2, stringBuilder2.toString());
            return;
        }
        this.mRelayManager.startMotionRecognition(motionTypeReco);
        synchronized (this.mMotionAppsRegList) {
            this.mMotionAppsRegList.add(Integer.valueOf(motionApps));
        }
    }

    public void stopMotionRecoTutorial(int motionApps) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "stopMotionRecoTutorial destroy called already ");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopMotionRecoTutorial motionApps: ");
        stringBuilder.append(motionApps);
        MRLog.d(str, stringBuilder.toString());
        int motionTypeReco = MotionTypeApps.getMotionTypeByMotionApps(motionApps);
        if (motionTypeReco == motionApps || this.mMotionAppsRegList.contains(Integer.valueOf(motionApps))) {
            this.mRelayManager.stopMotionRecognition(motionTypeReco);
            synchronized (this.mMotionAppsRegList) {
                this.mMotionAppsRegList.remove(Integer.valueOf(motionApps));
            }
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("stopMotionRecoTutorial not recognition motionApps ");
        stringBuilder2.append(motionApps);
        MRLog.d(str2, stringBuilder2.toString());
    }

    public boolean startMotionAppsReco(int motionApps) {
        return startMotionAppsReco(motionApps, false);
    }

    public boolean startMotionAppsReco(int motionApps, boolean isEx) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "startMotionAppsReco destroy called already ");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startMotionAppsReco motionApps: ");
        stringBuilder.append(motionApps);
        MRLog.d(str, stringBuilder.toString());
        if (this.mMotionAppsRegList.contains(Integer.valueOf(motionApps))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startMotionAppsReco repeat motionApps ");
            stringBuilder.append(motionApps);
            MRLog.w(str, stringBuilder.toString());
            return false;
        }
        int motionTypeReco = MotionTypeApps.getMotionTypeByMotionApps(motionApps);
        String str2;
        if (motionTypeReco == motionApps) {
            str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startMotionAppsReco motionApps ");
            stringBuilder2.append(motionApps);
            stringBuilder2.append(" is not supported.");
            MRLog.e(str2, stringBuilder2.toString());
            return false;
        }
        str2 = MotionTypeApps.getMotionKeyByMotionApps(motionTypeReco);
        String str3;
        if (resetMotionState(MRUtils.getMotionEnableState(this.mContext, str2, isEx), str2, isEx) == 1) {
            StringBuilder stringBuilder3;
            if (MRUtils.getMotionEnableState(this.mContext, MotionTypeApps.getMotionKeyByMotionApps(motionApps), isEx) == 1) {
                String str4 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("startMotionAppsReco motionTypeReco: ");
                stringBuilder3.append(motionTypeReco);
                MRLog.d(str4, stringBuilder3.toString());
                this.mRelayManager.startMotionRecognition(motionTypeReco, isEx);
                synchronized (this.mMotionAppsRegList) {
                    this.mMotionAppsRegList.add(Integer.valueOf(motionApps));
                }
                return true;
            }
            str3 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("startMotionAppsReco motionApps: ");
            stringBuilder3.append(motionApps);
            stringBuilder3.append(" disabled");
            MRLog.w(str3, stringBuilder3.toString());
        } else {
            str3 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("startMotionAppsReco motionTypeReco: ");
            stringBuilder4.append(motionTypeReco);
            stringBuilder4.append(" disabled");
            MRLog.w(str3, stringBuilder4.toString());
        }
        return false;
    }

    public boolean startMotionAppsRecoAsUser(int motionApps, int userId) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "startMotionAppsRecoAsUser destroy called already ");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startMotionAppsRecoAsUser motionApps: ");
        stringBuilder.append(motionApps);
        MRLog.d(str, stringBuilder.toString());
        if (this.mMotionAppsRegList.contains(Integer.valueOf(motionApps))) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startMotionAppsRecoAsUser repeat motionApps ");
            stringBuilder.append(motionApps);
            MRLog.w(str, stringBuilder.toString());
            return false;
        }
        int motionTypeReco = MotionTypeApps.getMotionTypeByMotionApps(motionApps);
        String str2;
        if (motionTypeReco == motionApps) {
            str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startMotionAppsRecoAsUser motionApps ");
            stringBuilder2.append(motionApps);
            stringBuilder2.append(" is not supported.");
            MRLog.e(str2, stringBuilder2.toString());
            return false;
        }
        str2 = MotionTypeApps.getMotionKeyByMotionApps(motionTypeReco);
        String str3;
        if (resetMotionState(MRUtils.getMotionEnableStateAsUser(this.mContext, str2, userId), str2, false) == 1) {
            StringBuilder stringBuilder3;
            if (MRUtils.getMotionEnableStateAsUser(this.mContext, MotionTypeApps.getMotionKeyByMotionApps(motionApps), userId) == 1) {
                String str4 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("startMotionAppsRecoAsUser motionTypeReco: ");
                stringBuilder3.append(motionTypeReco);
                MRLog.d(str4, stringBuilder3.toString());
                this.mRelayManager.startMotionRecognitionAsUser(motionTypeReco, userId);
                synchronized (this.mMotionAppsRegList) {
                    this.mMotionAppsRegList.add(Integer.valueOf(motionApps));
                }
                return true;
            }
            str3 = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("startMotionAppsRecoAsUser motionApps: ");
            stringBuilder3.append(motionApps);
            stringBuilder3.append(" disabled");
            MRLog.w(str3, stringBuilder3.toString());
        } else {
            str3 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("startMotionAppsRecoAsUser motionTypeReco: ");
            stringBuilder4.append(motionTypeReco);
            stringBuilder4.append(" disabled");
            MRLog.w(str3, stringBuilder4.toString());
        }
        return false;
    }

    public boolean stopMotionAppsReco(int motionApps) {
        return stopMotionAppsReco(motionApps, false);
    }

    public boolean stopMotionAppsReco(int motionApps, boolean isEx) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "stopMotionAppsReco destroy called already ");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopMotionAppsReco motionApps: ");
        stringBuilder.append(motionApps);
        MRLog.d(str, stringBuilder.toString());
        int motionTypeReco = MotionTypeApps.getMotionTypeByMotionApps(motionApps);
        String str2;
        StringBuilder stringBuilder2;
        if (motionTypeReco == motionApps || this.mMotionAppsRegList.contains(Integer.valueOf(motionApps))) {
            if (isMotionStopValid(motionApps, motionTypeReco)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stopMotionAppsReco motionTypeReco: ");
                stringBuilder2.append(motionTypeReco);
                MRLog.d(str2, stringBuilder2.toString());
                this.mRelayManager.stopMotionRecognition(motionTypeReco);
            } else {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stopMotionAppsReco can not stop motionReco: ");
                stringBuilder2.append(motionTypeReco);
                MRLog.w(str2, stringBuilder2.toString());
            }
            synchronized (this.mMotionAppsRegList) {
                this.mMotionAppsRegList.remove(Integer.valueOf(motionApps));
            }
            return true;
        }
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("stopMotionAppsReco not recognition motionApps ");
        stringBuilder2.append(motionApps);
        MRLog.d(str2, stringBuilder2.toString());
        return false;
    }

    public boolean stopMotionAppsRecoAsUser(int motionApps, int userId) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "stopMotionAppsRecoAsUser destroy called already ");
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopMotionAppsRecoAsUser motionApps: ");
        stringBuilder.append(motionApps);
        MRLog.d(str, stringBuilder.toString());
        int motionTypeReco = MotionTypeApps.getMotionTypeByMotionApps(motionApps);
        String str2;
        StringBuilder stringBuilder2;
        if (motionTypeReco == motionApps || this.mMotionAppsRegList.contains(Integer.valueOf(motionApps))) {
            if (isMotionStopValid(motionApps, motionTypeReco)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stopMotionAppsRecoAsUser motionTypeReco: ");
                stringBuilder2.append(motionTypeReco);
                MRLog.d(str2, stringBuilder2.toString());
                this.mRelayManager.stopMotionRecognitionAsUser(motionTypeReco, userId);
            } else {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stopMotionAppsRecoAsUser can not stop motionReco: ");
                stringBuilder2.append(motionTypeReco);
                MRLog.w(str2, stringBuilder2.toString());
            }
            synchronized (this.mMotionAppsRegList) {
                this.mMotionAppsRegList.remove(Integer.valueOf(motionApps));
            }
            return true;
        }
        str2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("stopMotionAppsRecoAsUser not recognition motionApps ");
        stringBuilder2.append(motionApps);
        MRLog.d(str2, stringBuilder2.toString());
        return false;
    }

    public void destroy() {
        if (this.mDestroyed) {
            MRLog.w(TAG, "destroy() called already ");
            return;
        }
        stopAllMotionReco();
        this.mDestroyed = true;
        synchronized (this.mMotionAppsRegList) {
            this.mMotionAppsRegList.clear();
            this.mMotionAppsRegList = null;
        }
        this.mMDListenerList.clear();
        this.mMDListenerList = null;
        this.mRelayManager.destroy();
        this.mRelayManager = null;
        this.mContext = null;
    }

    public void addMotionListener(MotionDetectionListener mdListener) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "addMotionListener destroy called already ");
            return;
        }
        if (!(this.mMDListenerList == null || this.mMDListenerList.contains(mdListener))) {
            this.mMDListenerList.add(mdListener);
        }
    }

    public void removeMotionListener(MotionDetectionListener mdListener) {
        if (this.mDestroyed) {
            MRLog.w(TAG, "removeMotionListener destroy called already ");
            return;
        }
        if (this.mMDListenerList != null && this.mMDListenerList.contains(mdListener)) {
            this.mMDListenerList.remove(mdListener);
        }
    }

    private boolean isMotionStopValid(int motionApps, int motionTypeReco) {
        int motionAppsSize = this.mMotionAppsRegList.size();
        int tmpMApps = 0;
        int i = 0;
        while (i < motionAppsSize) {
            tmpMApps = ((Integer) this.mMotionAppsRegList.get(i)).intValue();
            if (MotionTypeApps.getMotionTypeByMotionApps(tmpMApps) != motionTypeReco || tmpMApps == motionApps) {
                i++;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isMotionStopValid same motionReco running by other motionApps: ");
                stringBuilder.append(tmpMApps);
                MRLog.w(str, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    private int resetMotionState(int motionState, String motionKey, boolean isEx) {
        if (motionState != -1) {
            return motionState;
        }
        MRUtils.setMotionEnableState(this.mContext, motionKey, 1, isEx);
        return 1;
    }

    private void processMotionRecoResult(int relayType, Object mrecoRes) {
        MRLog.d(TAG, "processReceiverMsg ... ");
        if (relayType == 1) {
            notifyMotionRecoResult((Intent) mrecoRes);
        }
    }

    private void stopAllMotionReco() {
        if (this.mMotionAppsRegList != null && this.mMotionAppsRegList.size() > 0) {
            int appsRegListSize = this.mMotionAppsRegList.size();
            int i = 0;
            while (i < appsRegListSize) {
                if (stopMotionAppsReco(((Integer) this.mMotionAppsRegList.get(i)).intValue())) {
                    i--;
                    appsRegListSize--;
                }
                i++;
            }
        }
    }

    private void notifyMotionRecoResult(Intent recoIntent) {
        int motionTypeReco = getRecoMotionType(recoIntent);
        int rRes = getRecoMotionResult(recoIntent);
        int rDirect = getRecoMotionDirect(recoIntent);
        Bundle rExtras = getRecoMotionExtras(recoIntent);
        try {
            ArrayList<Integer> maTypeList = getMotionsAppsByMotionReco(motionTypeReco);
            if (this.mMDListenerList.size() > 0 && this.mMotionAppsRegList.size() > 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notifyMotionRecoResult motionTypeReco: ");
                stringBuilder.append(motionTypeReco);
                stringBuilder.append("  recoRes: ");
                stringBuilder.append(rRes);
                stringBuilder.append(" rDirect: ");
                stringBuilder.append(rDirect);
                stringBuilder.append("mMDListenerList size: ");
                stringBuilder.append(this.mMDListenerList.size());
                stringBuilder.append("mMotionAppsRegList size: ");
                stringBuilder.append(this.mMotionAppsRegList.size());
                MRLog.d(str, stringBuilder.toString());
            }
            int maTypeListSize = maTypeList.size();
            int listenerSize = this.mMDListenerList.size();
            MotionRecoResult tmpMRes = null;
            int j = 0;
            while (j < maTypeListSize) {
                MotionRecoResult tmpMRes2 = tmpMRes;
                for (int i = 0; i < listenerSize; i++) {
                    tmpMRes2 = getRecoResult(((Integer) maTypeList.get(j)).intValue(), rRes, rDirect, rExtras);
                    ((MotionDetectionListener) this.mMDListenerList.get(i)).notifyMotionRecoResult(tmpMRes2);
                }
                j++;
                tmpMRes = tmpMRes2;
            }
        } catch (Exception ex) {
            MRLog.w(TAG, ex.getMessage());
        }
    }

    private MotionRecoResult getRecoResult(int pMApps, int pRes, int pDirect, Bundle pExtras) {
        return new MotionRecoResult(pMApps, pRes, pDirect, pExtras);
    }

    private ArrayList<Integer> getMotionsAppsByMotionReco(int mType) {
        ArrayList<Integer> maList = new ArrayList();
        int appsRegListSize = this.mMotionAppsRegList.size();
        if (appsRegListSize > 0) {
            for (int i = 0; i < appsRegListSize; i++) {
                if (MotionTypeApps.getMotionTypeByMotionApps(((Integer) this.mMotionAppsRegList.get(i)).intValue()) == mType) {
                    maList.add((Integer) this.mMotionAppsRegList.get(i));
                }
            }
        }
        return maList;
    }

    private int getRecoMotionType(Intent intent) {
        return intent.getIntExtra(MotionConfig.MOTION_TYPE_RECOGNITION, 0);
    }

    private int getRecoMotionResult(Intent intent) {
        return intent.getIntExtra(MotionConfig.MOTION_RECOGNITION_RESULT, 0);
    }

    private int getRecoMotionDirect(Intent recoRes) {
        return recoRes.getIntExtra(MotionConfig.MOTION_RECOGNITION_DIRECTION, 0);
    }

    private Bundle getRecoMotionExtras(Intent recoIntent) {
        return recoIntent.getBundleExtra(MotionConfig.MOTION_RECOGNITION_EXTRAS);
    }
}
