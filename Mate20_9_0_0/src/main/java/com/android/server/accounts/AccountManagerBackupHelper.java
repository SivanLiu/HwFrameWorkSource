package com.android.server.accounts;

import android.accounts.Account;
import android.accounts.AccountManagerInternal;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

        /* JADX WARNING: Missing block: B:26:0x0091, code skipped:
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
                        if (pendingAppPermission.packageName.equals(packageName)) {
                            if (pendingAppPermission.apply(AccountManagerBackupHelper.this.mAccountManagerService.mContext.getPackageManager())) {
                                AccountManagerBackupHelper.this.mRestorePendingAppPermissions.remove(i);
                            }
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

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:48:0x011a, B:54:0x0123] */
    /* JADX WARNING: Missing block: B:67:0x0133, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:73:0x013a, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public byte[] backupAccountAccessPermissions(int userId) {
        Throwable th;
        String str;
        IOException e;
        int i = userId;
        UserAccounts accounts = this.mAccountManagerService.getUserAccounts(i);
        synchronized (accounts.dbLock) {
            UserAccounts userAccounts;
            try {
                synchronized (accounts.cacheLock) {
                    try {
                        List<Pair<String, Integer>> allAccountGrants = accounts.accountsDb.findAllAccountGrants();
                        if (allAccountGrants.isEmpty()) {
                            try {
                                try {
                                    return null;
                                } catch (Throwable th2) {
                                    th = th2;
                                    userAccounts = accounts;
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                userAccounts = accounts;
                                throw th;
                            }
                        }
                        List<Pair<String, Integer>> allAccountGrants2;
                        try {
                            ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
                            FastXmlSerializer serializer = new FastXmlSerializer();
                            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
                            serializer.startDocument(null, Boolean.valueOf(true));
                            serializer.startTag(null, TAG_PERMISSIONS);
                            PackageManager packageManager = this.mAccountManagerService.mContext.getPackageManager();
                            for (Pair<String, Integer> grant : allAccountGrants) {
                                String accountName = (String) grant.first;
                                String[] packageNames = packageManager.getPackagesForUid(((Integer) grant.second).intValue());
                                if (packageNames != null) {
                                    int length = packageNames.length;
                                    int i2 = 0;
                                    while (i2 < length) {
                                        PackageInfo packageInfo;
                                        int i3;
                                        userAccounts = accounts;
                                        String accounts2 = packageNames[i2];
                                        try {
                                            packageInfo = packageManager.getPackageInfoAsUser(accounts2, 64, i);
                                        } catch (NameNotFoundException e2) {
                                            allAccountGrants2 = allAccountGrants;
                                            i3 = length;
                                            NameNotFoundException nameNotFoundException = e2;
                                            str = TAG;
                                            StringBuilder stringBuilder = new StringBuilder();
                                            stringBuilder.append("Skipping backup of account access grant for non-existing package: ");
                                            stringBuilder.append(accounts2);
                                            Slog.i(str, stringBuilder.toString());
                                        }
                                        try {
                                            str = PackageUtils.computeSignaturesSha256Digest(packageInfo.signatures);
                                            if (str != null) {
                                                allAccountGrants2 = allAccountGrants;
                                                try {
                                                    serializer.startTag(null, TAG_PERMISSION);
                                                    i3 = length;
                                                    serializer.attribute(null, ATTR_ACCOUNT_SHA_256, PackageUtils.computeSha256Digest(accountName.getBytes()));
                                                    serializer.attribute(null, "package", accounts2);
                                                    serializer.attribute(null, ATTR_DIGEST, str);
                                                    serializer.endTag(null, TAG_PERMISSION);
                                                } catch (IOException e3) {
                                                    e = e3;
                                                    Log.e(TAG, "Error backing up account access grants", e);
                                                    return null;
                                                }
                                            }
                                            allAccountGrants2 = allAccountGrants;
                                            i3 = length;
                                            i2++;
                                            accounts = userAccounts;
                                            allAccountGrants = allAccountGrants2;
                                            length = i3;
                                            i = userId;
                                        } catch (IOException e4) {
                                            e = e4;
                                            allAccountGrants2 = allAccountGrants;
                                            Log.e(TAG, "Error backing up account access grants", e);
                                            return null;
                                        }
                                    }
                                    allAccountGrants2 = allAccountGrants;
                                    i = userId;
                                }
                            }
                            allAccountGrants2 = allAccountGrants;
                            serializer.endTag(null, TAG_PERMISSIONS);
                            serializer.endDocument();
                            serializer.flush();
                            byte[] toByteArray = dataStream.toByteArray();
                            return toByteArray;
                        } catch (IOException e5) {
                            e = e5;
                            userAccounts = accounts;
                            allAccountGrants2 = allAccountGrants;
                            Log.e(TAG, "Error backing up account access grants", e);
                            return null;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        userAccounts = accounts;
                        throw th;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
                userAccounts = accounts;
                throw th;
            }
        }
    }

    public void restoreAccountAccessPermissions(byte[] data, int userId) {
        Exception e;
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
                                    if (!pendingAppPermission.apply(packageManager)) {
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
            } catch (IOException | XmlPullParserException e2) {
                e = e2;
                Log.e(TAG, "Error restoring app permissions", e);
            }
        } catch (IOException | XmlPullParserException e3) {
            e = e3;
            byte[] bArr = data;
            Log.e(TAG, "Error restoring app permissions", e);
        }
    }
}
