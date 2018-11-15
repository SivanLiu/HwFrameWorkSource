package com.android.server.pm;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.SigningInfo;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.LocalServices;
import com.android.server.backup.BackupUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import libcore.util.HexEncoding;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutPackageInfo {
    private static final String ATTR_BACKUP_ALLOWED = "allow-backup";
    private static final String ATTR_BACKUP_ALLOWED_INITIALIZED = "allow-backup-initialized";
    private static final String ATTR_BACKUP_SOURCE_BACKUP_ALLOWED = "bk_src_backup-allowed";
    private static final String ATTR_BACKUP_SOURCE_VERSION = "bk_src_version";
    private static final String ATTR_LAST_UPDATE_TIME = "last_udpate_time";
    private static final String ATTR_SHADOW = "shadow";
    private static final String ATTR_SIGNATURE_HASH = "hash";
    private static final String ATTR_VERSION = "version";
    private static final String TAG = "ShortcutService";
    static final String TAG_ROOT = "package-info";
    private static final String TAG_SIGNATURE = "signature";
    private boolean mBackupAllowed;
    private boolean mBackupAllowedInitialized;
    private boolean mBackupSourceBackupAllowed;
    private long mBackupSourceVersionCode = -1;
    private boolean mIsShadow;
    private long mLastUpdateTime;
    private ArrayList<byte[]> mSigHashes;
    private long mVersionCode = -1;

    private ShortcutPackageInfo(long versionCode, long lastUpdateTime, ArrayList<byte[]> sigHashes, boolean isShadow) {
        this.mVersionCode = versionCode;
        this.mLastUpdateTime = lastUpdateTime;
        this.mIsShadow = isShadow;
        this.mSigHashes = sigHashes;
        this.mBackupAllowed = false;
        this.mBackupSourceBackupAllowed = false;
    }

    public static ShortcutPackageInfo newEmpty() {
        return new ShortcutPackageInfo(-1, 0, new ArrayList(0), false);
    }

    public boolean isShadow() {
        return this.mIsShadow;
    }

    public void setShadow(boolean shadow) {
        this.mIsShadow = shadow;
    }

    public long getVersionCode() {
        return this.mVersionCode;
    }

    public long getBackupSourceVersionCode() {
        return this.mBackupSourceVersionCode;
    }

    @VisibleForTesting
    public boolean isBackupSourceBackupAllowed() {
        return this.mBackupSourceBackupAllowed;
    }

    public long getLastUpdateTime() {
        return this.mLastUpdateTime;
    }

    public boolean isBackupAllowed() {
        return this.mBackupAllowed;
    }

    public void updateFromPackageInfo(PackageInfo pi) {
        if (pi != null) {
            this.mVersionCode = pi.getLongVersionCode();
            this.mLastUpdateTime = pi.lastUpdateTime;
            this.mBackupAllowed = ShortcutService.shouldBackupApp(pi);
            this.mBackupAllowedInitialized = true;
        }
    }

    public boolean hasSignatures() {
        return this.mSigHashes.size() > 0;
    }

    public int canRestoreTo(ShortcutService s, PackageInfo currentPackage, boolean anyVersionOkay) {
        if (!BackupUtils.signaturesMatch(this.mSigHashes, currentPackage, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class))) {
            Slog.w(TAG, "Can't restore: Package signature mismatch");
            return 102;
        } else if (!ShortcutService.shouldBackupApp(currentPackage) || !this.mBackupSourceBackupAllowed) {
            Slog.w(TAG, "Can't restore: package didn't or doesn't allow backup");
            return 101;
        } else if (anyVersionOkay || currentPackage.getLongVersionCode() >= this.mBackupSourceVersionCode) {
            return 0;
        } else {
            Slog.w(TAG, String.format("Can't restore: package current version %d < backed up version %d", new Object[]{Long.valueOf(currentPackage.getLongVersionCode()), Long.valueOf(this.mBackupSourceVersionCode)}));
            return 100;
        }
    }

    @VisibleForTesting
    public static ShortcutPackageInfo generateForInstalledPackageForTest(ShortcutService s, String packageName, int packageUserId) {
        PackageInfo pi = s.getPackageInfoWithSignatures(packageName, packageUserId);
        SigningInfo signingInfo = pi.signingInfo;
        if (signingInfo == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't get signatures: package=");
            stringBuilder.append(packageName);
            Slog.e(str, stringBuilder.toString());
            return null;
        }
        ShortcutPackageInfo ret = new ShortcutPackageInfo(pi.getLongVersionCode(), pi.lastUpdateTime, BackupUtils.hashSignatureArray(signingInfo.getApkContentsSigners()), false);
        ret.mBackupSourceBackupAllowed = ShortcutService.shouldBackupApp(pi);
        ret.mBackupSourceVersionCode = pi.getLongVersionCode();
        return ret;
    }

    public void refreshSignature(ShortcutService s, ShortcutPackageItem pkg) {
        if (this.mIsShadow) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempted to refresh package info for shadow package ");
            stringBuilder.append(pkg.getPackageName());
            stringBuilder.append(", user=");
            stringBuilder.append(pkg.getOwnerUserId());
            s.wtf(stringBuilder.toString());
            return;
        }
        PackageInfo pi = s.getPackageInfoWithSignatures(pkg.getPackageName(), pkg.getPackageUserId());
        if (pi == null) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Package not found: ");
            stringBuilder2.append(pkg.getPackageName());
            Slog.w(str, stringBuilder2.toString());
            return;
        }
        SigningInfo signingInfo = pi.signingInfo;
        if (signingInfo == null) {
            String str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Not refreshing signature for ");
            stringBuilder3.append(pkg.getPackageName());
            stringBuilder3.append(" since it appears to have no signing info.");
            Slog.w(str2, stringBuilder3.toString());
            return;
        }
        this.mSigHashes = BackupUtils.hashSignatureArray(signingInfo.getApkContentsSigners());
    }

    public void saveToXml(ShortcutService s, XmlSerializer out, boolean forBackup) throws IOException {
        if (forBackup && !this.mBackupAllowedInitialized) {
            s.wtf("Backup happened before mBackupAllowed is initialized.");
        }
        out.startTag(null, TAG_ROOT);
        ShortcutService.writeAttr(out, ATTR_VERSION, this.mVersionCode);
        ShortcutService.writeAttr(out, ATTR_LAST_UPDATE_TIME, this.mLastUpdateTime);
        ShortcutService.writeAttr(out, ATTR_SHADOW, this.mIsShadow);
        ShortcutService.writeAttr(out, ATTR_BACKUP_ALLOWED, this.mBackupAllowed);
        ShortcutService.writeAttr(out, ATTR_BACKUP_ALLOWED_INITIALIZED, this.mBackupAllowedInitialized);
        ShortcutService.writeAttr(out, ATTR_BACKUP_SOURCE_VERSION, this.mBackupSourceVersionCode);
        ShortcutService.writeAttr(out, ATTR_BACKUP_SOURCE_BACKUP_ALLOWED, this.mBackupSourceBackupAllowed);
        for (int i = 0; i < this.mSigHashes.size(); i++) {
            out.startTag(null, TAG_SIGNATURE);
            ShortcutService.writeAttr(out, ATTR_SIGNATURE_HASH, Base64.getEncoder().encodeToString((byte[]) this.mSigHashes.get(i)));
            out.endTag(null, TAG_SIGNATURE);
        }
        out.endTag(null, TAG_ROOT);
    }

    public void loadFromXml(XmlPullParser parser, boolean fromBackup) throws IOException, XmlPullParserException {
        boolean shadow;
        XmlPullParser xmlPullParser = parser;
        long versionCode = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_VERSION, -1);
        long lastUpdateTime = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_LAST_UPDATE_TIME);
        boolean outerDepth = true;
        boolean shadow2 = fromBackup || ShortcutService.parseBooleanAttribute(xmlPullParser, ATTR_SHADOW);
        long backupSourceVersion = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_BACKUP_SOURCE_VERSION, -1);
        boolean backupAllowed = ShortcutService.parseBooleanAttribute(xmlPullParser, ATTR_BACKUP_ALLOWED, true);
        boolean backupSourceBackupAllowed = ShortcutService.parseBooleanAttribute(xmlPullParser, ATTR_BACKUP_SOURCE_BACKUP_ALLOWED, true);
        ArrayList<byte[]> hashes = new ArrayList();
        int outerDepth2 = parser.getDepth();
        while (true) {
            int outerDepth3 = outerDepth2;
            boolean next = parser.next();
            boolean type = next;
            boolean z;
            int i;
            if (next == outerDepth) {
                z = type;
                shadow = shadow2;
                i = outerDepth3;
                break;
            }
            int outerDepth4;
            if (type) {
                outerDepth4 = outerDepth3;
                if (parser.getDepth() <= outerDepth4) {
                    z = type;
                    i = outerDepth4;
                    shadow = shadow2;
                    break;
                }
            }
            outerDepth4 = outerDepth3;
            if (!type) {
                i = outerDepth4;
                shadow = shadow2;
            } else {
                next = parser.getDepth();
                int type2 = parser.getName();
                shadow = shadow2;
                if (next == outerDepth4 + 1) {
                    i = outerDepth4;
                    Object obj = (type2.hashCode() == 1073584312 && type2.equals(TAG_SIGNATURE) != 0) ? null : -1;
                    if (obj == null) {
                        hashes.add(Base64.getDecoder().decode(ShortcutService.parseStringAttribute(xmlPullParser, ATTR_SIGNATURE_HASH)));
                    }
                } else {
                    i = outerDepth4;
                }
                ShortcutService.warnForInvalidTag(next, type2);
            }
            shadow2 = shadow;
            outerDepth2 = i;
            outerDepth = true;
        }
        if (fromBackup) {
            this.mVersionCode = -1;
            this.mBackupSourceVersionCode = versionCode;
            this.mBackupSourceBackupAllowed = backupAllowed;
        } else {
            this.mVersionCode = versionCode;
            this.mBackupSourceVersionCode = backupSourceVersion;
            this.mBackupSourceBackupAllowed = backupSourceBackupAllowed;
        }
        this.mLastUpdateTime = lastUpdateTime;
        this.mIsShadow = shadow;
        this.mSigHashes = hashes;
        this.mBackupAllowed = false;
        this.mBackupAllowedInitialized = false;
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println();
        pw.print(prefix);
        pw.println("PackageInfo:");
        pw.print(prefix);
        pw.print("  IsShadow: ");
        pw.print(this.mIsShadow);
        pw.print(this.mIsShadow ? " (not installed)" : " (installed)");
        pw.println();
        pw.print(prefix);
        pw.print("  Version: ");
        pw.print(this.mVersionCode);
        pw.println();
        if (this.mBackupAllowedInitialized) {
            pw.print(prefix);
            pw.print("  Backup Allowed: ");
            pw.print(this.mBackupAllowed);
            pw.println();
        }
        if (this.mBackupSourceVersionCode != -1) {
            pw.print(prefix);
            pw.print("  Backup source version: ");
            pw.print(this.mBackupSourceVersionCode);
            pw.println();
            pw.print(prefix);
            pw.print("  Backup source backup allowed: ");
            pw.print(this.mBackupSourceBackupAllowed);
            pw.println();
        }
        pw.print(prefix);
        pw.print("  Last package update time: ");
        pw.print(this.mLastUpdateTime);
        pw.println();
        for (int i = 0; i < this.mSigHashes.size(); i++) {
            pw.print(prefix);
            pw.print("    ");
            pw.print("SigHash: ");
            pw.println(HexEncoding.encode((byte[]) this.mSigHashes.get(i)));
        }
    }
}
