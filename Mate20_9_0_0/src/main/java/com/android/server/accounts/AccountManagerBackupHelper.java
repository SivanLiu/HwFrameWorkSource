package com.android.server.accounts;

import android.accounts.Account;
import android.accounts.AccountManagerInternal;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.PackageUtils;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.XmlUtils;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class AccountManagerBackupHelper {
    private static final String ATTR_ACCOUNT_SHA_256 = "account-sha-256";
    private static final String ATTR_DIGEST = "digest";
    private static final String ATTR_PACKAGE = "package";
    private static final long PENDING_RESTORE_TIMEOUT_MILLIS = 3600000;
    private static final String TAG = "AccountManagerBackupHelper";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSIONS = "permissions";
    private final AccountManagerInternal mAccountManagerInternal;
    private final AccountManagerService mAccountManagerService;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private Runnable mRestoreCancelCommand;
    @GuardedBy("mLock")
    private RestorePackageMonitor mRestorePackageMonitor;
    @GuardedBy("mLock")
    private List<PendingAppPermission> mRestorePendingAppPermissions;

    private final class CancelRestoreCommand implements Runnable {
        private CancelRestoreCommand() {
        }

        public void run() {
            synchronized (AccountManagerBackupHelper.this.mLock) {
                AccountManagerBackupHelper.this.mRestorePendingAppPermissions = null;
                if (AccountManagerBackupHelper.this.mRestorePackageMonitor != null) {
                    AccountManagerBackupHelper.this.mRestorePackageMonitor.unregister();
                    AccountManagerBackupHelper.this.mRestorePackageMonitor = null;
                }
            }
        }
    }

    private final class PendingAppPermission {
        private final String accountDigest;
        private final String certDigest;
        private final String packageName;
        private final int userId;

        public PendingAppPermission(String accountDigest, String packageName, String certDigest, int userId) {
            this.accountDigest = accountDigest;
            this.packageName = packageName;
            this.certDigest = certDigest;
            this.userId = userId;
        }

        public boolean apply(PackageManager packageManager) {
            Account account = null;
            UserAccounts accounts = AccountManagerBackupHelper.this.mAccountManagerService.getUserAccounts(this.userId);
            synchronized (accounts.dbLock) {
                synchronized (accounts.cacheLock) {
                    for (Account[] accountsPerType : accounts.accountCache.values()) {
                        for (Account accountPerType : (Account[]) r4.next()) {
                            if (this.accountDigest.equals(PackageUtils.computeSha256Digest(accountPerType.name.getBytes()))) {
                                account = accountPerType;
                                break;
                            }
                        }
                        if (account != null) {
                            break;
                        }
                    }
                }
            }
            if (account == null) {
                return false;
            }
            try {
                PackageInfo packageInfo = packageManager.getPackageInfoAsUser(this.packageName, 64, this.userId);
                String[] signaturesSha256Digests = PackageUtils.computeSignaturesSha256Digests(packageInfo.signatures);
                if (!this.certDigest.equals(PackageUtils.computeSignaturesSha256Digest(signaturesSha256Digests)) && (packageInfo.signatures.length <= 1 || !this.certDigest.equals(signaturesSha256Digests[0]))) {
                    return false;
                }
                int uid = packageInfo.applicationInfo.uid;
                if (!AccountManagerBackupHelper.this.mAccountManagerInternal.hasAccountAccess(account, uid)) {
                    AccountManagerBackupHelper.this.mAccountManagerService.grantAppPermission(account, "com.android.AccountManager.ACCOUNT_ACCESS_TOKEN_TYPE", uid);
                }
                return true;
            } catch (NameNotFoundException e) {
                return false;
            }
        }
    }

    private final class RestorePackageMonitor extends PackageMonitor {
        private RestorePackageMonitor() {
        }

        /* JADX WARNING: Missing block: B:25:0x0091, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onPackageAdded(String packageName, int uid) {
            synchronized (AccountManagerBackupHelper.this.mLock) {
                if (AccountManagerBackupHelper.this.mRestorePendingAppPermissions == null) {
                } else if (UserHandle.getUserId(uid) != 0) {
                } else {
                    for (int i = AccountManagerBackupHelper.this.mRestorePendingAppPermissions.size() - 1; i >= 0; i--) {
                        PendingAppPermission pendingAppPermission = (PendingAppPermission) AccountManagerBackupHelper.this.mRestorePendingAppPermissions.get(i);
                        if (pendingAppPermission.packageName.equals(packageName) && pendingAppPermission.apply(AccountManagerBackupHelper.this.mAccountManagerService.mContext.getPackageManager())) {
                            AccountManagerBackupHelper.this.mRestorePendingAppPermissions.remove(i);
                        }
                    }
                    if (AccountManagerBackupHelper.this.mRestorePendingAppPermissions.isEmpty() && AccountManagerBackupHelper.this.mRestoreCancelCommand != null) {
                        AccountManagerBackupHelper.this.mAccountManagerService.mHandler.removeCallbacks(AccountManagerBackupHelper.this.mRestoreCancelCommand);
                        AccountManagerBackupHelper.this.mRestoreCancelCommand.run();
                        AccountManagerBackupHelper.this.mRestoreCancelCommand = null;
                    }
                }
            }
        }
    }

    public AccountManagerBackupHelper(AccountManagerService accountManagerService, AccountManagerInternal accountManagerInternal) {
        this.mAccountManagerService = accountManagerService;
        this.mAccountManagerInternal = accountManagerInternal;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: RegionMakerVisitor
        jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.server.accounts.AccountManagerBackupHelper.backupAccountAccessPermissions(int):byte[], dom blocks: [B:48:0x011a, B:54:0x0123]
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:89)
        	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
        	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public byte[] backupAccountAccessPermissions(int r22) {
        /*
        r21 = this;
        r1 = r21;
        r2 = r22;
        r0 = r1.mAccountManagerService;
        r3 = r0.getUserAccounts(r2);
        r4 = r3.dbLock;
        monitor-enter(r4);
        r5 = r3.cacheLock;	 Catch:{ all -> 0x0135 }
        monitor-enter(r5);	 Catch:{ all -> 0x0135 }
        r0 = r3.accountsDb;	 Catch:{ all -> 0x012e }
        r0 = r0.findAllAccountGrants();	 Catch:{ all -> 0x012e }
        r6 = r0;	 Catch:{ all -> 0x012e }
        r0 = r6.isEmpty();	 Catch:{ all -> 0x012e }
        r7 = 0;
        if (r0 == 0) goto L_0x002b;
    L_0x001e:
        monitor-exit(r5);	 Catch:{ all -> 0x0026 }
        monitor-exit(r4);	 Catch:{ all -> 0x0021 }
        return r7;
    L_0x0021:
        r0 = move-exception;
        r17 = r3;
        goto L_0x0138;
    L_0x0026:
        r0 = move-exception;
        r17 = r3;
        goto L_0x0131;
    L_0x002b:
        r0 = new java.io.ByteArrayOutputStream;	 Catch:{ IOException -> 0x011e }
        r0.<init>();	 Catch:{ IOException -> 0x011e }
        r8 = r0;	 Catch:{ IOException -> 0x011e }
        r0 = new com.android.internal.util.FastXmlSerializer;	 Catch:{ IOException -> 0x011e }
        r0.<init>();	 Catch:{ IOException -> 0x011e }
        r9 = r0;	 Catch:{ IOException -> 0x011e }
        r0 = java.nio.charset.StandardCharsets.UTF_8;	 Catch:{ IOException -> 0x011e }
        r0 = r0.name();	 Catch:{ IOException -> 0x011e }
        r9.setOutput(r8, r0);	 Catch:{ IOException -> 0x011e }
        r0 = 1;	 Catch:{ IOException -> 0x011e }
        r0 = java.lang.Boolean.valueOf(r0);	 Catch:{ IOException -> 0x011e }
        r9.startDocument(r7, r0);	 Catch:{ IOException -> 0x011e }
        r0 = "permissions";	 Catch:{ IOException -> 0x011e }
        r9.startTag(r7, r0);	 Catch:{ IOException -> 0x011e }
        r0 = r1.mAccountManagerService;	 Catch:{ IOException -> 0x011e }
        r0 = r0.mContext;	 Catch:{ IOException -> 0x011e }
        r0 = r0.getPackageManager();	 Catch:{ IOException -> 0x011e }
        r10 = r0;	 Catch:{ IOException -> 0x011e }
        r11 = r6.iterator();	 Catch:{ IOException -> 0x011e }
    L_0x005b:
        r0 = r11.hasNext();	 Catch:{ IOException -> 0x011e }
        if (r0 == 0) goto L_0x0104;	 Catch:{ IOException -> 0x011e }
    L_0x0061:
        r0 = r11.next();	 Catch:{ IOException -> 0x011e }
        r0 = (android.util.Pair) r0;	 Catch:{ IOException -> 0x011e }
        r12 = r0;	 Catch:{ IOException -> 0x011e }
        r0 = r12.first;	 Catch:{ IOException -> 0x011e }
        r0 = (java.lang.String) r0;	 Catch:{ IOException -> 0x011e }
        r13 = r0;	 Catch:{ IOException -> 0x011e }
        r0 = r12.second;	 Catch:{ IOException -> 0x011e }
        r0 = (java.lang.Integer) r0;	 Catch:{ IOException -> 0x011e }
        r0 = r0.intValue();	 Catch:{ IOException -> 0x011e }
        r14 = r0;	 Catch:{ IOException -> 0x011e }
        r0 = r10.getPackagesForUid(r14);	 Catch:{ IOException -> 0x011e }
        r15 = r0;	 Catch:{ IOException -> 0x011e }
        if (r15 != 0) goto L_0x007e;	 Catch:{ IOException -> 0x011e }
    L_0x007d:
        goto L_0x005b;	 Catch:{ IOException -> 0x011e }
    L_0x007e:
        r7 = r15.length;	 Catch:{ IOException -> 0x011e }
        r0 = 0;	 Catch:{ IOException -> 0x011e }
        r1 = r0;	 Catch:{ IOException -> 0x011e }
    L_0x0081:
        if (r1 >= r7) goto L_0x00f9;	 Catch:{ IOException -> 0x011e }
    L_0x0083:
        r0 = r15[r1];	 Catch:{ IOException -> 0x011e }
        r16 = r0;
        r0 = 64;
        r17 = r3;
        r3 = r16;
        r0 = r10.getPackageInfoAsUser(r3, r0, r2);	 Catch:{ NameNotFoundException -> 0x00d1 }
        r2 = r0.signatures;	 Catch:{ IOException -> 0x00cd }
        r2 = android.util.PackageUtils.computeSignaturesSha256Digest(r2);	 Catch:{ IOException -> 0x00cd }
        if (r2 == 0) goto L_0x00c8;	 Catch:{ IOException -> 0x00cd }
    L_0x009b:
        r18 = r0;	 Catch:{ IOException -> 0x00cd }
        r0 = "permission";	 Catch:{ IOException -> 0x00cd }
        r19 = r6;
        r6 = 0;
        r9.startTag(r6, r0);	 Catch:{ IOException -> 0x011c }
        r0 = "account-sha-256";	 Catch:{ IOException -> 0x011c }
        r6 = r13.getBytes();	 Catch:{ IOException -> 0x011c }
        r6 = android.util.PackageUtils.computeSha256Digest(r6);	 Catch:{ IOException -> 0x011c }
        r20 = r7;	 Catch:{ IOException -> 0x011c }
        r7 = 0;	 Catch:{ IOException -> 0x011c }
        r9.attribute(r7, r0, r6);	 Catch:{ IOException -> 0x011c }
        r0 = "package";	 Catch:{ IOException -> 0x011c }
        r9.attribute(r7, r0, r3);	 Catch:{ IOException -> 0x011c }
        r0 = "digest";	 Catch:{ IOException -> 0x011c }
        r9.attribute(r7, r0, r2);	 Catch:{ IOException -> 0x011c }
        r0 = "permission";	 Catch:{ IOException -> 0x011c }
        r9.endTag(r7, r0);	 Catch:{ IOException -> 0x011c }
        goto L_0x00ee;	 Catch:{ IOException -> 0x011c }
    L_0x00c8:
        r19 = r6;	 Catch:{ IOException -> 0x011c }
        r20 = r7;	 Catch:{ IOException -> 0x011c }
        goto L_0x00ee;	 Catch:{ IOException -> 0x011c }
    L_0x00cd:
        r0 = move-exception;	 Catch:{ IOException -> 0x011c }
        r19 = r6;	 Catch:{ IOException -> 0x011c }
        goto L_0x0123;	 Catch:{ IOException -> 0x011c }
    L_0x00d1:
        r0 = move-exception;	 Catch:{ IOException -> 0x011c }
        r19 = r6;	 Catch:{ IOException -> 0x011c }
        r20 = r7;	 Catch:{ IOException -> 0x011c }
        r2 = r0;	 Catch:{ IOException -> 0x011c }
        r2 = "AccountManagerBackupHelper";	 Catch:{ IOException -> 0x011c }
        r6 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x011c }
        r6.<init>();	 Catch:{ IOException -> 0x011c }
        r7 = "Skipping backup of account access grant for non-existing package: ";	 Catch:{ IOException -> 0x011c }
        r6.append(r7);	 Catch:{ IOException -> 0x011c }
        r6.append(r3);	 Catch:{ IOException -> 0x011c }
        r6 = r6.toString();	 Catch:{ IOException -> 0x011c }
        android.util.Slog.i(r2, r6);	 Catch:{ IOException -> 0x011c }
    L_0x00ee:
        r1 = r1 + 1;	 Catch:{ IOException -> 0x011c }
        r3 = r17;	 Catch:{ IOException -> 0x011c }
        r6 = r19;	 Catch:{ IOException -> 0x011c }
        r7 = r20;	 Catch:{ IOException -> 0x011c }
        r2 = r22;	 Catch:{ IOException -> 0x011c }
        goto L_0x0081;	 Catch:{ IOException -> 0x011c }
    L_0x00f9:
        r17 = r3;	 Catch:{ IOException -> 0x011c }
        r19 = r6;	 Catch:{ IOException -> 0x011c }
        r1 = r21;	 Catch:{ IOException -> 0x011c }
        r2 = r22;	 Catch:{ IOException -> 0x011c }
        r7 = 0;	 Catch:{ IOException -> 0x011c }
        goto L_0x005b;	 Catch:{ IOException -> 0x011c }
    L_0x0104:
        r17 = r3;	 Catch:{ IOException -> 0x011c }
        r19 = r6;	 Catch:{ IOException -> 0x011c }
        r0 = "permissions";	 Catch:{ IOException -> 0x011c }
        r1 = 0;	 Catch:{ IOException -> 0x011c }
        r9.endTag(r1, r0);	 Catch:{ IOException -> 0x011c }
        r9.endDocument();	 Catch:{ IOException -> 0x011c }
        r9.flush();	 Catch:{ IOException -> 0x011c }
        r0 = r8.toByteArray();	 Catch:{ IOException -> 0x011c }
        monitor-exit(r5);	 Catch:{ all -> 0x0133 }
        monitor-exit(r4);	 Catch:{ all -> 0x013a }
        return r0;
    L_0x011c:
        r0 = move-exception;
        goto L_0x0123;
    L_0x011e:
        r0 = move-exception;
        r17 = r3;
        r19 = r6;
    L_0x0123:
        r1 = "AccountManagerBackupHelper";	 Catch:{ all -> 0x0133 }
        r2 = "Error backing up account access grants";	 Catch:{ all -> 0x0133 }
        android.util.Log.e(r1, r2, r0);	 Catch:{ all -> 0x0133 }
        monitor-exit(r5);	 Catch:{ all -> 0x0133 }
        monitor-exit(r4);	 Catch:{ all -> 0x013a }
        r1 = 0;
        return r1;
    L_0x012e:
        r0 = move-exception;
        r17 = r3;
    L_0x0131:
        monitor-exit(r5);	 Catch:{ all -> 0x0133 }
        throw r0;	 Catch:{ all -> 0x013a }
    L_0x0133:
        r0 = move-exception;	 Catch:{ all -> 0x013a }
        goto L_0x0131;	 Catch:{ all -> 0x013a }
    L_0x0135:
        r0 = move-exception;	 Catch:{ all -> 0x013a }
        r17 = r3;	 Catch:{ all -> 0x013a }
    L_0x0138:
        monitor-exit(r4);	 Catch:{ all -> 0x013a }
        throw r0;
    L_0x013a:
        r0 = move-exception;
        goto L_0x0138;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.accounts.AccountManagerBackupHelper.backupAccountAccessPermissions(int):byte[]");
    }

    /* JADX WARNING: Removed duplicated region for block: B:45:0x00f0 A:{Splitter: B:1:0x0002, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00ee A:{Splitter: B:4:0x0006, ExcHandler: org.xmlpull.v1.XmlPullParserException (e org.xmlpull.v1.XmlPullParserException)} */
    /* JADX WARNING: Missing block: B:44:0x00ee, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:45:0x00f0, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:46:0x00f1, code:
            r8 = r19;
     */
    /* JADX WARNING: Missing block: B:47:0x00f3, code:
            android.util.Log.e(TAG, "Error restoring app permissions", r0);
     */
    /* JADX WARNING: Missing block: B:54:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void restoreAccountAccessPermissions(byte[] data, int userId) {
        try {
            try {
                ByteArrayInputStream dataStream = new ByteArrayInputStream(data);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(dataStream, StandardCharsets.UTF_8.name());
                PackageManager packageManager = this.mAccountManagerService.mContext.getPackageManager();
                int permissionsOuterDepth = parser.getDepth();
                while (true) {
                    int permissionsOuterDepth2 = permissionsOuterDepth;
                    if (XmlUtils.nextElementWithin(parser, permissionsOuterDepth2)) {
                        if (TAG_PERMISSIONS.equals(parser.getName())) {
                            permissionsOuterDepth = parser.getDepth();
                            while (true) {
                                int permissionOuterDepth = permissionsOuterDepth;
                                if (!XmlUtils.nextElementWithin(parser, permissionOuterDepth)) {
                                    continue;
                                    break;
                                }
                                if (TAG_PERMISSION.equals(parser.getName())) {
                                    String accountDigest = parser.getAttributeValue(null, ATTR_ACCOUNT_SHA_256);
                                    if (TextUtils.isEmpty(accountDigest)) {
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                    String packageName = parser.getAttributeValue(null, "package");
                                    if (TextUtils.isEmpty(packageName)) {
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                    String attributeValue = parser.getAttributeValue(null, ATTR_DIGEST);
                                    if (TextUtils.isEmpty(attributeValue)) {
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                    String digest = attributeValue;
                                    PendingAppPermission pendingAppPermission = new PendingAppPermission(accountDigest, packageName, attributeValue, userId);
                                    if (pendingAppPermission.apply(packageManager)) {
                                        continue;
                                    } else {
                                        synchronized (this.mLock) {
                                            if (this.mRestorePackageMonitor == null) {
                                                this.mRestorePackageMonitor = new RestorePackageMonitor();
                                                this.mRestorePackageMonitor.register(this.mAccountManagerService.mContext, this.mAccountManagerService.mHandler.getLooper(), true);
                                            }
                                            if (this.mRestorePendingAppPermissions == null) {
                                                this.mRestorePendingAppPermissions = new ArrayList();
                                            }
                                            this.mRestorePendingAppPermissions.add(pendingAppPermission);
                                        }
                                    }
                                }
                                permissionsOuterDepth = permissionOuterDepth;
                            }
                        }
                        permissionsOuterDepth = permissionsOuterDepth2;
                    } else {
                        this.mRestoreCancelCommand = new CancelRestoreCommand();
                        this.mAccountManagerService.mHandler.postDelayed(this.mRestoreCancelCommand, 3600000);
                        return;
                    }
                }
            } catch (XmlPullParserException e) {
            }
        } catch (XmlPullParserException e2) {
        }
    }
}
