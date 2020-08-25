package com.android.server.rms.iaware.cpu;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.rms.iaware.AwareLog;
import com.android.server.mtm.taskstatus.ProcessInfo;
import com.android.server.mtm.taskstatus.ProcessInfoCollector;
import com.android.server.mtm.utils.InnerUtils;
import com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup;
import com.android.server.rms.iaware.appmng.AwareDefaultConfigList;
import com.android.server.rms.iaware.qos.AwareQosFeatureManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CPUKeyBackground {
    private static final String GRP_BACKGROUND = "/background";
    public static final String GRP_KEY_BACKGROUND = "/key-background";
    private static final int KBG_MSG_LEN = 12;
    private static final String KEY_BUNDLE_GRP = "KEY_GRP";
    private static final String KEY_BUNDLE_PID = "KEY_PID";
    private static final String KEY_BUNDLE_UID = "KEY_UID";
    private static final Object SLOCK = new Object();
    private static final String TAG = "CPUKeyBackground";
    private static CPUKeyBackground sInstance;
    private AwareStateCallback mAwareStateCallback;
    private CPUFeature mCPUFeatureInstance;
    private KeyBackgroundHandler mKeyBackgroundHandler;

    private CPUKeyBackground() {
    }

    public static CPUKeyBackground getInstance() {
        CPUKeyBackground cPUKeyBackground;
        synchronized (SLOCK) {
            if (sInstance == null) {
                sInstance = new CPUKeyBackground();
            }
            cPUKeyBackground = sInstance;
        }
        return cPUKeyBackground;
    }

    public void start(CPUFeature feature) {
        initHandler();
        this.mCPUFeatureInstance = feature;
        registerStateCallback();
    }

    private void registerStateCallback() {
        if (this.mAwareStateCallback == null) {
            this.mAwareStateCallback = new AwareStateCallback();
            AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 0);
            AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 5);
            AwareAppKeyBackgroup.getInstance().registerStateCallback(this.mAwareStateCallback, 2);
        }
    }

    public void destroy() {
        unregisterStateCallback();
    }

    private void unregisterStateCallback() {
        if (this.mAwareStateCallback != null) {
            AwareAppKeyBackgroup.getInstance().unregisterStateCallback(this.mAwareStateCallback, 0);
            AwareAppKeyBackgroup.getInstance().unregisterStateCallback(this.mAwareStateCallback, 5);
            AwareAppKeyBackgroup.getInstance().unregisterStateCallback(this.mAwareStateCallback, 2);
            this.mAwareStateCallback = null;
        }
    }

    private void initHandler() {
        if (this.mKeyBackgroundHandler == null) {
            this.mKeyBackgroundHandler = new KeyBackgroundHandler();
        }
    }

    /* access modifiers changed from: private */
    public void checkKeyBackgroundAndSendMsg(int pid, int uid) {
        KeyBackgroundHandler keyBackgroundHandler;
        boolean isKeyBG = AwareAppKeyBackgroup.getInstance().checkIsKeyBackgroup(pid, uid);
        if (isProtectPackage(getAppPkgName(pid, uid)) && isKeyBG) {
            setProcessGroup(pid, -1);
        } else if (isKeyBG && (keyBackgroundHandler = this.mKeyBackgroundHandler) != null) {
            Message msg = keyBackgroundHandler.obtainMessage(104);
            msg.arg1 = pid;
            this.mKeyBackgroundHandler.sendMessage(msg);
        }
    }

    public void sendSwitchGroupMessage(int pid, int messgaeId) {
        KeyBackgroundHandler keyBackgroundHandler = this.mKeyBackgroundHandler;
        if (keyBackgroundHandler != null) {
            Message msg = keyBackgroundHandler.obtainMessage(messgaeId);
            msg.arg1 = pid;
            this.mKeyBackgroundHandler.sendMessage(msg);
        }
    }

    private void closeBufferedReader(BufferedReader br) {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                AwareLog.e(TAG, "closeBufferedReader exception " + e.getMessage());
            }
        }
    }

    private void closeInputStreamReader(InputStreamReader isr) {
        if (isr != null) {
            try {
                isr.close();
            } catch (IOException e) {
                AwareLog.e(TAG, "closeInputStreamReader exception " + e.getMessage());
            }
        }
    }

    private void closeFileInputStream(FileInputStream fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                AwareLog.e(TAG, "closeFileInputStream exception " + e.getMessage());
            }
        }
    }

    private void moveForkToBackground(int uid, int pid) {
        if (this.mKeyBackgroundHandler != null) {
            FileInputStream fis = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                fis = new FileInputStream("/acct/uid_" + uid + "/pid_" + pid + "/cgroup.procs");
                isr = new InputStreamReader(fis, "UTF-8");
                br = new BufferedReader(isr);
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    int heriPid = Integer.parseInt(line.trim());
                    if (heriPid != pid) {
                        if (checkIsTargetGroup(heriPid, GRP_KEY_BACKGROUND)) {
                            Message msg = this.mKeyBackgroundHandler.obtainMessage(105);
                            msg.arg1 = heriPid;
                            this.mKeyBackgroundHandler.sendMessage(msg);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                AwareLog.e(TAG, "NumberFormatException " + e.getMessage());
            } catch (FileNotFoundException e2) {
                AwareLog.e(TAG, "FileNotFoundException " + e2.getMessage());
            } catch (UnsupportedEncodingException e3) {
                AwareLog.e(TAG, "UnsupportedEncodingException " + e3.getMessage());
            } catch (IOException e4) {
                AwareLog.e(TAG, "IOException " + e4.getMessage());
            } catch (Throwable th) {
                closeBufferedReader(null);
                closeInputStreamReader(null);
                closeFileInputStream(null);
                throw th;
            }
            closeBufferedReader(br);
            closeInputStreamReader(isr);
            closeFileInputStream(fis);
        }
    }

    /* access modifiers changed from: private */
    public void sendMessagesByUid(int code, int uid, String grp) {
        List<ProcessInfo> procs = getProcessesByUid(uid);
        if (!procs.isEmpty() && this.mKeyBackgroundHandler != null) {
            int procsSize = procs.size();
            for (int i = 0; i < procsSize; i++) {
                ProcessInfo info = procs.get(i);
                if (info != null && checkIsTargetGroup(info.mPid, grp)) {
                    Message msg = this.mKeyBackgroundHandler.obtainMessage(code);
                    msg.arg1 = info.mPid;
                    this.mKeyBackgroundHandler.sendMessage(msg);
                    if (105 == code) {
                        moveForkToBackground(uid, info.mPid);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public String getAppPkgName(int pid, int uid) {
        if (pid > 0) {
            return InnerUtils.getAwarePkgName(pid);
        }
        return InnerUtils.getPackageNameByUid(uid);
    }

    /* access modifiers changed from: private */
    public boolean checkIsNativeKeyBackgroupApp(String pkgName) {
        if (pkgName != null && AwareDefaultConfigList.getInstance().getKeyHabitAppList().contains(pkgName)) {
            return true;
        }
        return false;
    }

    private class AwareStateCallback implements AwareAppKeyBackgroup.IAwareStateCallback {
        private AwareStateCallback() {
        }

        @Override // com.android.server.rms.iaware.appmng.AwareAppKeyBackgroup.IAwareStateCallback
        public void onStateChanged(int stateType, int eventType, int pid, int uid) {
            if (stateType == 5) {
                NetManager.getInstance().sendMsgToNetMng(uid, eventType, 3);
            }
            if (stateType == 2) {
                NetManager.getInstance().sendMsgToNetMng(uid, eventType, 4);
            }
            if (stateType == 0 && CPUFeature.isCpusetEnable()) {
                String pkgName = CPUKeyBackground.this.getAppPkgName(pid, uid);
                if (!CPUKeyBackground.this.checkIsNativeKeyBackgroupApp(pkgName)) {
                    if (!CPUKeyBackground.this.isProtectPackage(pkgName)) {
                        if (eventType == 1) {
                            CPUKeyBackground.this.sendMessagesByUid(104, uid, CPUKeyBackground.GRP_BACKGROUND);
                        }
                        if (eventType == 2) {
                            CPUKeyBackground.this.sendMessagesByUid(105, uid, CPUKeyBackground.GRP_KEY_BACKGROUND);
                        }
                    } else if (eventType == 1) {
                        CPUKeyBackground.this.setProcessGroupByUid(uid);
                    }
                }
            }
        }
    }

    private List<ProcessInfo> getProcessesByUid(int uid) {
        List<ProcessInfo> procs = new ArrayList<>();
        ArrayList<ProcessInfo> procList = ProcessInfoCollector.getInstance().getProcessInfoList();
        if (procList.isEmpty()) {
            AwareLog.e(TAG, "getProcessesByUid procList is null!");
            return procs;
        }
        Iterator<ProcessInfo> it = procList.iterator();
        while (it.hasNext()) {
            ProcessInfo info = it.next();
            if (info != null && uid == info.mUid) {
                procs.add(info);
            }
        }
        return procs;
    }

    public void notifyProcessGroupChange(int pid, int uid, int grp) {
        KeyBackgroundHandler keyBackgroundHandler;
        if (CPUFeature.isCpusetEnable() && (keyBackgroundHandler = this.mKeyBackgroundHandler) != null) {
            Message msg = keyBackgroundHandler.obtainMessage(106);
            Bundle bundle = new Bundle();
            bundle.putInt("KEY_PID", pid);
            bundle.putInt(KEY_BUNDLE_UID, uid);
            bundle.putInt(KEY_BUNDLE_GRP, grp);
            msg.setData(bundle);
            this.mKeyBackgroundHandler.sendMessage(msg);
        }
    }

    /* access modifiers changed from: private */
    public int doKBackground(int msg, int pid) {
        if (!CPUFeature.isCpusetEnable()) {
            return 0;
        }
        if (this.mCPUFeatureInstance == null) {
            AwareLog.e(TAG, "doKBackground mCPUFeatureInstance = null!");
            return -1;
        }
        ByteBuffer buffer = ByteBuffer.allocate(12);
        buffer.putInt(msg);
        buffer.putInt(pid);
        int isLimit = 0;
        if (msg == 104 && !AwareQosFeatureManager.getInstance().doCheckPreceptible(pid)) {
            isLimit = 1;
        }
        buffer.putInt(isLimit);
        int resCode = this.mCPUFeatureInstance.sendPacket(buffer);
        if (resCode != 1) {
            AwareLog.e(TAG, "doKBackground sendPacked failed, send error code:" + resCode);
        }
        return 0;
    }

    private class KeyBackgroundHandler extends Handler {
        private KeyBackgroundHandler() {
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 104:
                    int unused = CPUKeyBackground.this.doKBackground(104, msg.arg1);
                    return;
                case 105:
                    int unused2 = CPUKeyBackground.this.doKBackground(105, msg.arg1);
                    return;
                case 106:
                    Bundle bundle = msg.getData();
                    if (bundle == null) {
                        AwareLog.e(CPUKeyBackground.TAG, "handleMessage inavlid params null bundle!");
                        return;
                    }
                    int pid = bundle.getInt("KEY_PID");
                    int uid = bundle.getInt(CPUKeyBackground.KEY_BUNDLE_UID);
                    if (bundle.getInt(CPUKeyBackground.KEY_BUNDLE_GRP) == 0) {
                        CPUKeyBackground.this.checkKeyBackgroundAndSendMsg(pid, uid);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public boolean checkIsTargetGroup(int pid, String grp) {
        if (grp == null) {
            AwareLog.e(TAG, "checkIsTargetGroup invalid params");
            return false;
        }
        File file = new File("/proc/" + pid + "/cpuset");
        FileInputStream inputStream = null;
        StringBuilder sb = new StringBuilder();
        try {
            FileInputStream inputStream2 = new FileInputStream(file);
            while (true) {
                int temp = inputStream2.read();
                if (temp != -1) {
                    char ch = (char) temp;
                    if (!(ch == '\n' || ch == '\r')) {
                        sb.append(ch);
                    }
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e.getMessage());
                    }
                }
            }
            inputStream2.close();
            if (sb.length() == 0) {
                AwareLog.e(TAG, "checkIsTargetGroup read fail");
                return false;
            } else if (grp.equals(sb.toString())) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e2) {
            AwareLog.e(TAG, "checkIsTargetGroup read catch IOException msg = " + e2.getMessage());
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e3.getMessage());
                }
            }
            return false;
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    AwareLog.e(TAG, "checkIsTargetGroup close catch IOException msg = " + e4.getMessage());
                }
            }
            throw th;
        }
    }

    /* access modifiers changed from: private */
    public boolean isProtectPackage(String pkgName) {
        if (pkgName == null || !pkgName.contains("com.hicloud.android.clone")) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void setProcessGroupByUid(int uid) {
        List<ProcessInfo> procs = getProcessesByUid(uid);
        int procsSize = procs.size();
        for (int i = 0; i < procsSize; i++) {
            ProcessInfo info = procs.get(i);
            if (info != null && checkIsTargetGroup(info.mPid, GRP_BACKGROUND)) {
                setProcessGroup(info.mPid, -1);
            }
        }
    }

    private void setProcessGroup(int pid, int schedGroup) {
        try {
            Process.setProcessGroup(pid, schedGroup);
        } catch (IllegalArgumentException e) {
            AwareLog.e(TAG, "setProcessGroup pid " + pid + e.getMessage());
        } catch (SecurityException e2) {
            AwareLog.e(TAG, "setProcessGroup pid " + pid + e2.getMessage());
        } catch (RuntimeException e3) {
            AwareLog.e(TAG, "setProcessGroup pid " + pid + e3.getMessage());
        }
    }
}
