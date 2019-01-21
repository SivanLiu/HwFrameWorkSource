package android.installerMgr;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.rms.iaware.AwareLog;

public class InstallerMgr {
    public static final int EVENT_FORK_NOTIFY = 2;
    public static final int EVENT_INSTALLED = 1;
    public static final int EVENT_INSTALLING = 0;
    public static final String KEY_BUNDLE_DEXOPT_PID = "dexopt_pid";
    public static final String KEY_BUNDLE_EVENT_ID = "eventId";
    public static final String KEY_BUNDLE_INSTALLER_NAME = "installer_name";
    public static final String KEY_BUNDLE_INSTALLER_UID = "installer_uid";
    public static final String KEY_BUNDLE_PACKAGE_NAME = "package_name";
    private static final String TAG = "InstallerMgr";
    private static InstallerMgr sInstallerMgr;
    private static Object sObject = new Object();
    private IBinder mAwareService;
    private InstallerInfo mInstallerInfo;
    private Object mLock = new Object();

    public static class InstallerInfo {
        public String installerPkgName;
        public int installerUid;
        public String pkgName;

        public InstallerInfo(String installerPkgName, String pkgName, int installerUid) {
            this.installerPkgName = installerPkgName;
            this.pkgName = pkgName;
            this.installerUid = installerUid;
        }
    }

    private InstallerMgr() {
    }

    public static InstallerMgr getInstance() {
        InstallerMgr installerMgr;
        synchronized (sObject) {
            if (sInstallerMgr == null) {
                sInstallerMgr = new InstallerMgr();
            }
            installerMgr = sInstallerMgr;
        }
        return installerMgr;
    }

    /* JADX WARNING: Missing block: B:9:0x003c, code skipped:
            if (r0 == null) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:10:0x003e, code skipped:
            if (r2 != null) goto L_0x0041;
     */
    /* JADX WARNING: Missing block: B:11:0x0041, code skipped:
            r4 = new android.os.Bundle();
            r4.putInt(KEY_BUNDLE_EVENT_ID, r9);
            r4.putString(KEY_BUNDLE_INSTALLER_NAME, r0);
            r4.putString("package_name", r2);
            r4.putInt(KEY_BUNDLE_INSTALLER_UID, r3);
            notifiyEvent(r4);
     */
    /* JADX WARNING: Missing block: B:12:0x005e, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:13:0x005f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void notifiyInstallEvent(int eventId) {
        synchronized (this.mLock) {
            if (this.mInstallerInfo == null) {
                return;
            }
            String installerPkgName = this.mInstallerInfo.installerPkgName;
            String pkgName = this.mInstallerInfo.pkgName;
            int installerUid = this.mInstallerInfo.installerUid;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifiyInstallEvent eventId = ");
            stringBuilder.append(eventId);
            stringBuilder.append(" pkgName = ");
            stringBuilder.append(pkgName);
            AwareLog.d(str, stringBuilder.toString());
        }
    }

    private IBinder getAwareService() {
        return ServiceManager.getService("hwsysresmanager");
    }

    private void notifiyEvent(Bundle bundle) {
        if (this.mAwareService == null) {
            this.mAwareService = getAwareService();
            if (this.mAwareService == null) {
                return;
            }
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken("android.rms.IHwSysResManager");
            data.writeBundle(bundle);
            this.mAwareService.transact(15016, data, reply, 0);
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mAwareService ontransact ");
            stringBuilder.append(e.getMessage());
            AwareLog.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            data.recycle();
            reply.recycle();
        }
        data.recycle();
        reply.recycle();
    }

    public void installPackage(int eventId, String installerPkgName, String pkgName) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("installPackage eventId = ");
        stringBuilder.append(eventId);
        stringBuilder.append(" installerPkgName = ");
        stringBuilder.append(installerPkgName);
        stringBuilder.append(" pkgName = ");
        stringBuilder.append(pkgName);
        AwareLog.d(str, stringBuilder.toString());
        synchronized (this.mLock) {
            if (eventId == 0) {
                try {
                    this.mInstallerInfo = new InstallerInfo(installerPkgName, pkgName, 0);
                    notifiyInstallEvent(eventId);
                } catch (Throwable th) {
                }
            } else if (eventId == 1) {
                notifiyInstallEvent(eventId);
                this.mInstallerInfo = null;
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("installPackage unknown eventId = ");
                stringBuilder2.append(eventId);
                AwareLog.w(str2, stringBuilder2.toString());
            }
        }
    }
}
