package com.android.server.forcerotation;

import android.content.Context;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.HwPCUtils;
import android.util.Slog;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.server.am.ActivityRecord;
import com.huawei.forcerotation.IHwForceRotationManager.Stub;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HwForceRotationManagerService extends Stub {
    private static final int MSG_SHOW_TOAST = 1;
    private static final String TAG = "HwForceRotationService";
    private Context mContext;
    private HwForceRotationLayout mFixedLandscapeLayout;
    private List<ForceRotationAppInfo> mForceRotationAppInfos;
    private HwForceRotationConfig mForceRotationConfig;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                HwForceRotationManagerService.this.showToast();
            }
        }
    };
    private boolean mIsAppInForceRotationWhiteList = false;
    private String mPrvCompontentName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private String mPrvPackageName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private String mTmpAppName;
    private Map<String, AppToastInfo> mToastedAppInfos;

    protected void showToast() {
        Toast.makeText(this.mContext, 33685953, 0).show();
    }

    public HwForceRotationManagerService(Context context, Handler uiHandler) {
        this.mContext = context;
        this.mForceRotationAppInfos = new ArrayList();
        this.mFixedLandscapeLayout = new HwForceRotationLayout(this.mContext, uiHandler, this);
        this.mForceRotationConfig = new HwForceRotationConfigLoader().load();
        this.mToastedAppInfos = new HashMap();
    }

    public boolean isForceRotationSwitchOpen() {
        if (this.mContext == null || this.mContext.getContentResolver() == null || System.getInt(this.mContext.getContentResolver(), "force_rotation_mode", 0) != 1 || (HwPCUtils.enabledInPad() && HwPCUtils.isPcCastModeInServer())) {
            return false;
        }
        return true;
    }

    public synchronized boolean isAppInForceRotationWhiteList(String packageName) {
        return this.mForceRotationConfig.isAppSupportForceRotation(packageName);
    }

    public synchronized boolean isAppForceLandRotatable(String packageName, IBinder aToken) {
        if (!this.mForceRotationConfig.isAppSupportForceRotation(packageName)) {
            return false;
        }
        return isAppForceLandRotatable(aToken);
    }

    /* JADX WARNING: Missing block: B:40:0x00a8, code skipped:
            r0 = r3;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected synchronized boolean isAppForceLandRotatable(IBinder aToken) {
        boolean z;
        ForceRotationAppInfo tmpFRAI;
        ForceRotationAppInfo portaitFRAI = null;
        ForceRotationAppInfo landscapeFRAI = null;
        Iterator<ForceRotationAppInfo> iter = this.mForceRotationAppInfos.iterator();
        while (true) {
            z = true;
            if (!iter.hasNext()) {
                break;
            }
            tmpFRAI = (ForceRotationAppInfo) iter.next();
            IBinder tmpToken = (IBinder) tmpFRAI.getmAppToken().get();
            int tmpOrientation = tmpFRAI.getmOrientation();
            String str;
            StringBuilder stringBuilder;
            if (ActivityRecord.forToken(tmpToken) != null) {
                if (aToken == tmpToken) {
                    if (tmpOrientation != 1 && tmpOrientation != 7 && tmpOrientation != 9) {
                        if (tmpOrientation != 12) {
                            if (tmpOrientation == 0 || tmpOrientation == 6 || tmpOrientation == 8 || tmpOrientation == 11 || tmpOrientation == -1 || tmpOrientation == 4 || tmpOrientation == 5 || tmpOrientation == 2 || tmpOrientation == 13) {
                                break;
                            } else if (tmpOrientation == 10) {
                                break;
                            } else {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("utk:pn=");
                                stringBuilder.append(tmpFRAI.getmPackageName());
                                stringBuilder.append(", o=");
                                stringBuilder.append(tmpOrientation);
                                Slog.d(str, stringBuilder.toString());
                            }
                        } else {
                            break;
                        }
                    }
                    break;
                }
                continue;
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ftk:pn=");
                stringBuilder.append(tmpFRAI.getmPackageName());
                stringBuilder.append(", o=");
                stringBuilder.append(tmpOrientation);
                Slog.d(str, stringBuilder.toString());
                iter.remove();
            }
        }
        landscapeFRAI = tmpFRAI;
        if (portaitFRAI == null && landscapeFRAI != null) {
            z = false;
        }
        return z;
    }

    protected synchronized ForceRotationAppInfo queryForceRotationAppInfo(IBinder aToken) {
        ForceRotationAppInfo frai;
        frai = null;
        Iterator<ForceRotationAppInfo> iter = this.mForceRotationAppInfos.iterator();
        while (iter.hasNext()) {
            ForceRotationAppInfo tmpFRAI = (ForceRotationAppInfo) iter.next();
            IBinder tmpToken = (IBinder) tmpFRAI.getmAppToken().get();
            if (ActivityRecord.forToken(tmpToken) == null) {
                iter.remove();
            } else if (aToken == tmpToken) {
                frai = tmpFRAI;
                break;
            }
        }
        return frai;
    }

    /* JADX WARNING: Missing block: B:9:0x002d, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:18:0x0059, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean saveOrUpdateForceRotationAppInfo(String packageName, String componentName, IBinder aToken, int reqOrientation) {
        String str;
        StringBuilder stringBuilder;
        if (this.mForceRotationConfig.isAppSupportForceRotation(packageName)) {
            if (this.mForceRotationConfig.isActivitySupportForceRotation(componentName)) {
                saveOrUpdateForceRotationAppInfo(packageName, aToken, reqOrientation);
                return true;
            } else if (!(componentName == null || this.mPrvCompontentName.equals(componentName))) {
                this.mPrvCompontentName = componentName;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("isActivitySupportForceRotation-t,cn = ");
                stringBuilder.append(componentName);
                Slog.i(str, stringBuilder.toString());
            }
        } else if (!(packageName == null || this.mPrvPackageName.equals(packageName))) {
            this.mPrvPackageName = packageName;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("isAppSupportForceRotation-f,pn = ");
            stringBuilder.append(packageName);
            Slog.i(str, stringBuilder.toString());
        }
    }

    protected synchronized void saveOrUpdateForceRotationAppInfo(String packageName, IBinder aToken, int reqOrientation) {
        ForceRotationAppInfo frai = queryForceRotationAppInfo(aToken);
        if (frai == null) {
            this.mForceRotationAppInfos.add(new ForceRotationAppInfo(packageName, aToken, reqOrientation));
        } else if (reqOrientation != frai.getmOrientation()) {
            frai.setmOrientation(reqOrientation);
        }
    }

    /* JADX WARNING: Missing block: B:23:0x004c, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:32:0x0086, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void showToastIfNeeded(String packageName, int pid, String processName, IBinder aToken) {
        Display display = ((WindowManager) this.mContext.getSystemService("window")).getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getRealMetrics(dm);
        if (dm.widthPixels >= dm.heightPixels) {
            if (!isAppForceLandRotatable(packageName, aToken)) {
                return;
            }
            if (!TextUtils.isEmpty(packageName)) {
                if (pid > 0) {
                    AppToastInfo tmp = (AppToastInfo) this.mToastedAppInfos.get(packageName);
                    if (tmp == null || (pid != tmp.getmPid() && processName.equals(tmp.getmProcessName()))) {
                        if (tmp == null) {
                            tmp = new AppToastInfo(packageName, processName, pid);
                        } else {
                            tmp.setmPid(pid);
                        }
                        this.mToastedAppInfos.put(packageName, tmp);
                        Message msg = this.mHandler.obtainMessage();
                        msg.what = 1;
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("show Toast message in package:");
                        stringBuilder.append(packageName);
                        Slog.v(str, stringBuilder.toString());
                        this.mHandler.sendMessage(msg);
                    }
                }
            }
        }
    }

    public void applyForceRotationLayout(IBinder aToken, Rect vf) {
        Rect dv = null;
        if (this.mFixedLandscapeLayout != null) {
            dv = this.mFixedLandscapeLayout.getForceRotationLayout();
        }
        if (dv != null) {
            vf.set(dv);
        }
    }

    public int recalculateWidthForForceRotation(int width, int height, int logicalHeight, String packageName) {
        int resultWidth = width;
        if (width <= height || UserHandle.isIsolated(Binder.getCallingUid()) || !isForceRotationSwitchOpen()) {
            return resultWidth;
        }
        if (!(packageName == null || packageName.equals(this.mTmpAppName))) {
            this.mIsAppInForceRotationWhiteList = isAppInForceRotationWhiteList(packageName);
            this.mTmpAppName = packageName;
        }
        if (this.mIsAppInForceRotationWhiteList) {
            return logicalHeight;
        }
        return resultWidth;
    }
}
