package android.content;

import android.Manifest.permission;
import android.app.AppOpsManager;
import android.app.backup.FullBackup;
import android.common.HwFrameworkFactory;
import android.content.pm.PathPermission;
import android.content.pm.ProviderInfo;
import android.content.pm.UserInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public abstract class ContentProvider implements ComponentCallbacks2 {
    private static final String TAG = "ContentProvider";
    private String[] mAuthorities;
    private String mAuthority;
    private final ThreadLocal<String> mCallingPackage = new ThreadLocal();
    private Context mContext = null;
    private boolean mExported;
    private int mMyUid;
    private boolean mNoPerms;
    private PathPermission[] mPathPermissions;
    private String mReadPermission;
    private boolean mSingleUser;
    private Transport mTransport = new Transport();
    private String mWritePermission;

    public interface PipeDataWriter<T> {
        void writeDataToPipe(ParcelFileDescriptor parcelFileDescriptor, Uri uri, String str, Bundle bundle, T t);
    }

    class Transport extends ContentProviderNative {
        AppOpsManager mAppOpsManager = null;
        int mReadOp = -1;
        int mWriteOp = -1;

        Transport() {
        }

        ContentProvider getContentProvider() {
            return ContentProvider.this;
        }

        public String getProviderName() {
            return getContentProvider().getClass().getName();
        }

        public Cursor query(String callingPkg, Uri uri, String[] projection, Bundle queryArgs, ICancellationSignal cancellationSignal) {
            IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
            if (manager != null) {
                manager.sendBehavior(BehaviorId.CONTENTPROVIDER_QUERY, new Object[]{uri});
            }
            uri = ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
            if (enforceReadPermission(callingPkg, uri, null) == 0) {
                String original = ContentProvider.this.setCallingPackage(callingPkg);
                try {
                    Cursor query = ContentProvider.this.query(uri, projection, queryArgs, CancellationSignal.fromTransport(cancellationSignal));
                    return query;
                } finally {
                    ContentProvider.this.setCallingPackage(original);
                }
            } else if (projection != null) {
                return new MatrixCursor(projection, 0);
            } else {
                Cursor cursor = ContentProvider.this.query(uri, projection, queryArgs, CancellationSignal.fromTransport(cancellationSignal));
                if (cursor == null) {
                    return null;
                }
                return new MatrixCursor(cursor.getColumnNames(), 0);
            }
        }

        public String getType(Uri uri) {
            return ContentProvider.this.getType(ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri)));
        }

        public Uri insert(String callingPkg, Uri uri, ContentValues initialValues) {
            IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
            if (manager != null) {
                manager.sendBehavior(BehaviorId.CONTENTPROVIDER_INSERT, new Object[]{uri});
            }
            uri = ContentProvider.this.validateIncomingUri(uri);
            int userId = ContentProvider.getUserIdFromUri(uri);
            uri = ContentProvider.this.maybeGetUriWithoutUserId(uri);
            if (enforceWritePermission(callingPkg, uri, null) != 0) {
                return ContentProvider.this.rejectInsert(uri, initialValues);
            }
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                Uri maybeAddUserId = ContentProvider.maybeAddUserId(ContentProvider.this.insert(uri, initialValues), userId);
                return maybeAddUserId;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public int bulkInsert(String callingPkg, Uri uri, ContentValues[] initialValues) {
            uri = ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
            if (enforceWritePermission(callingPkg, uri, null) != 0) {
                return 0;
            }
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                int bulkInsert = ContentProvider.this.bulkInsert(uri, initialValues);
                return bulkInsert;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public ContentProviderResult[] applyBatch(String callingPkg, ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
            int numOperations = operations.size();
            int[] userIds = new int[numOperations];
            int i = 0;
            int i2 = 0;
            while (i2 < numOperations) {
                ContentProviderOperation operation = (ContentProviderOperation) operations.get(i2);
                Uri uri = operation.getUri();
                userIds[i2] = ContentProvider.getUserIdFromUri(uri);
                uri = ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
                if (!Objects.equals(operation.getUri(), uri)) {
                    operation = new ContentProviderOperation(operation, uri);
                    operations.set(i2, operation);
                }
                if (operation.isReadOperation() && enforceReadPermission(callingPkg, uri, null) != 0) {
                    throw new OperationApplicationException("App op not allowed", 0);
                } else if (!operation.isWriteOperation() || enforceWritePermission(callingPkg, uri, null) == 0) {
                    i2++;
                } else {
                    throw new OperationApplicationException("App op not allowed", 0);
                }
            }
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                ContentProviderResult[] results = ContentProvider.this.applyBatch(operations);
                if (results != null) {
                    while (i < results.length) {
                        if (userIds[i] != -2) {
                            results[i] = new ContentProviderResult(results[i], userIds[i]);
                        }
                        i++;
                    }
                }
                ContentProvider.this.setCallingPackage(original);
                return results;
            } catch (Throwable th) {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public int delete(String callingPkg, Uri uri, String selection, String[] selectionArgs) {
            IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
            if (manager != null) {
                manager.sendBehavior(BehaviorId.CONTENTPROVIDER_DELETE, new Object[]{uri});
            }
            uri = ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
            if (enforceWritePermission(callingPkg, uri, null) != 0) {
                return 0;
            }
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                int delete = ContentProvider.this.delete(uri, selection, selectionArgs);
                return delete;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public int update(String callingPkg, Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
            if (manager != null) {
                manager.sendBehavior(BehaviorId.CONTENTPROVIDER_UPDATE, new Object[]{uri});
            }
            uri = ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
            if (enforceWritePermission(callingPkg, uri, null) != 0) {
                return 0;
            }
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                int update = ContentProvider.this.update(uri, values, selection, selectionArgs);
                return update;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public ParcelFileDescriptor openFile(String callingPkg, Uri uri, String mode, ICancellationSignal cancellationSignal, IBinder callerToken) throws FileNotFoundException {
            uri = ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
            enforceFilePermission(callingPkg, uri, mode, callerToken);
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                ParcelFileDescriptor openFile = ContentProvider.this.openFile(uri, mode, CancellationSignal.fromTransport(cancellationSignal));
                return openFile;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public AssetFileDescriptor openAssetFile(String callingPkg, Uri uri, String mode, ICancellationSignal cancellationSignal) throws FileNotFoundException {
            uri = ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
            enforceFilePermission(callingPkg, uri, mode, null);
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                AssetFileDescriptor openAssetFile = ContentProvider.this.openAssetFile(uri, mode, CancellationSignal.fromTransport(cancellationSignal));
                return openAssetFile;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public Bundle call(String callingPkg, String method, String arg, Bundle extras) {
            Bundle.setDefusable(extras, true);
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                Bundle call = ContentProvider.this.call(method, arg, extras);
                return call;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
            return ContentProvider.this.getStreamTypes(ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri)), mimeTypeFilter);
        }

        public AssetFileDescriptor openTypedAssetFile(String callingPkg, Uri uri, String mimeType, Bundle opts, ICancellationSignal cancellationSignal) throws FileNotFoundException {
            Bundle.setDefusable(opts, true);
            uri = ContentProvider.this.maybeGetUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
            enforceFilePermission(callingPkg, uri, FullBackup.ROOT_TREE_TOKEN, null);
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                AssetFileDescriptor openTypedAssetFile = ContentProvider.this.openTypedAssetFile(uri, mimeType, opts, CancellationSignal.fromTransport(cancellationSignal));
                return openTypedAssetFile;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public ICancellationSignal createCancellationSignal() {
            return CancellationSignal.createTransport();
        }

        public Uri canonicalize(String callingPkg, Uri uri) {
            uri = ContentProvider.this.validateIncomingUri(uri);
            int userId = ContentProvider.getUserIdFromUri(uri);
            uri = ContentProvider.getUriWithoutUserId(uri);
            if (enforceReadPermission(callingPkg, uri, null) != 0) {
                return null;
            }
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                Uri maybeAddUserId = ContentProvider.maybeAddUserId(ContentProvider.this.canonicalize(uri), userId);
                return maybeAddUserId;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public Uri uncanonicalize(String callingPkg, Uri uri) {
            uri = ContentProvider.this.validateIncomingUri(uri);
            int userId = ContentProvider.getUserIdFromUri(uri);
            uri = ContentProvider.getUriWithoutUserId(uri);
            if (enforceReadPermission(callingPkg, uri, null) != 0) {
                return null;
            }
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                Uri maybeAddUserId = ContentProvider.maybeAddUserId(ContentProvider.this.uncanonicalize(uri), userId);
                return maybeAddUserId;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        public boolean refresh(String callingPkg, Uri uri, Bundle args, ICancellationSignal cancellationSignal) throws RemoteException {
            uri = ContentProvider.getUriWithoutUserId(ContentProvider.this.validateIncomingUri(uri));
            if (enforceReadPermission(callingPkg, uri, null) != 0) {
                return false;
            }
            String original = ContentProvider.this.setCallingPackage(callingPkg);
            try {
                boolean refresh = ContentProvider.this.refresh(uri, args, CancellationSignal.fromTransport(cancellationSignal));
                return refresh;
            } finally {
                ContentProvider.this.setCallingPackage(original);
            }
        }

        private void enforceFilePermission(String callingPkg, Uri uri, String mode, IBinder callerToken) throws FileNotFoundException, SecurityException {
            if (mode == null || mode.indexOf(119) == -1) {
                if (enforceReadPermission(callingPkg, uri, callerToken) != 0) {
                    throw new FileNotFoundException("App op not allowed");
                }
            } else if (enforceWritePermission(callingPkg, uri, callerToken) != 0) {
                throw new FileNotFoundException("App op not allowed");
            }
        }

        private int enforceReadPermission(String callingPkg, Uri uri, IBinder callerToken) throws SecurityException {
            int mode = ContentProvider.this.enforceReadPermissionInner(uri, callingPkg, callerToken);
            if (mode != 0) {
                return mode;
            }
            if (this.mReadOp != -1) {
                return this.mAppOpsManager.noteProxyOp(this.mReadOp, callingPkg);
            }
            return 0;
        }

        private int enforceWritePermission(String callingPkg, Uri uri, IBinder callerToken) throws SecurityException {
            int mode = ContentProvider.this.enforceWritePermissionInner(uri, callingPkg, callerToken);
            if (mode != 0) {
                return mode;
            }
            if (this.mWriteOp != -1) {
                return this.mAppOpsManager.noteProxyOp(this.mWriteOp, callingPkg);
            }
            return 0;
        }
    }

    public abstract int delete(Uri uri, String str, String[] strArr);

    public abstract String getType(Uri uri);

    public abstract Uri insert(Uri uri, ContentValues contentValues);

    public abstract boolean onCreate();

    public abstract Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2);

    public abstract int update(Uri uri, ContentValues contentValues, String str, String[] strArr);

    public ContentProvider(Context context, String readPermission, String writePermission, PathPermission[] pathPermissions) {
        this.mContext = context;
        this.mReadPermission = readPermission;
        this.mWritePermission = writePermission;
        this.mPathPermissions = pathPermissions;
    }

    public static ContentProvider coerceToLocalContentProvider(IContentProvider abstractInterface) {
        if (abstractInterface instanceof Transport) {
            return ((Transport) abstractInterface).getContentProvider();
        }
        return null;
    }

    boolean checkUser(int pid, int uid, Context context) {
        return UserHandle.getUserId(uid) == context.getUserId() || this.mSingleUser || context.checkPermission(permission.INTERACT_ACROSS_USERS, pid, uid) == 0;
    }

    private int checkPermissionAndAppOp(String permission, String callingPkg, IBinder callerToken) {
        if (getContext().checkPermission(permission, Binder.getCallingPid(), Binder.getCallingUid(), callerToken) != 0) {
            return 2;
        }
        int permOp = AppOpsManager.permissionToOpCode(permission);
        if (permOp != -1) {
            return this.mTransport.mAppOpsManager.noteProxyOp(permOp, callingPkg);
        }
        return 0;
    }

    protected int enforceReadPermissionInner(Uri uri, String callingPkg, IBinder callerToken) throws SecurityException {
        Uri uri2 = uri;
        String str = callingPkg;
        IBinder iBinder = callerToken;
        Context context = getContext();
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        String missingPerm = null;
        int strongestMode = 0;
        if (UserHandle.isSameApp(uid, this.mMyUid)) {
            return 0;
        }
        String pathPerm;
        if (this.mExported && checkUser(pid, uid, context)) {
            boolean allowDefaultRead;
            String componentPerm = getReadPermission();
            if (componentPerm != null) {
                int mode = checkPermissionAndAppOp(componentPerm, str, iBinder);
                if (mode == 0) {
                    return 0;
                }
                missingPerm = componentPerm;
                strongestMode = Math.max(0, mode);
            }
            boolean allowDefaultRead2 = componentPerm == null;
            PathPermission[] pps = getPathPermissions();
            if (pps != null) {
                String path = uri.getPath();
                allowDefaultRead = allowDefaultRead2;
                allowDefaultRead2 = strongestMode;
                String missingPerm2 = missingPerm;
                for (PathPermission pp : pps) {
                    pathPerm = pp.getReadPermission();
                    if (pathPerm == null || !pp.match(path)) {
                        missingPerm2 = missingPerm2;
                    } else {
                        missingPerm2 = checkPermissionAndAppOp(pathPerm, str, iBinder);
                        if (missingPerm2 == null) {
                            return 0;
                        }
                        allowDefaultRead = false;
                        String missingPerm3 = pathPerm;
                        allowDefaultRead2 = Math.max(allowDefaultRead2, missingPerm2);
                        missingPerm2 = missingPerm3;
                    }
                }
                String str2 = missingPerm2;
                strongestMode = allowDefaultRead2;
                missingPerm = str2;
            } else {
                allowDefaultRead = allowDefaultRead2;
            }
            if (allowDefaultRead) {
                return 0;
            }
        }
        pathPerm = missingPerm;
        int strongestMode2 = strongestMode;
        Uri userUri = (!this.mSingleUser || UserHandle.isSameUser(this.mMyUid, uid)) ? uri2 : maybeAddUserId(uri2, UserHandle.getUserId(uid));
        if (context.checkUriPermission(userUri, pid, uid, 1, iBinder) == 0) {
            return 0;
        }
        if (strongestMode2 == 1) {
            return 1;
        }
        if (permission.MANAGE_DOCUMENTS.equals(this.mReadPermission)) {
            missingPerm = " requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs";
        } else if (this.mExported) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" requires ");
            stringBuilder.append(pathPerm);
            stringBuilder.append(", or grantUriPermission()");
            missingPerm = stringBuilder.toString();
        } else {
            missingPerm = " requires the provider be exported, or grantUriPermission()";
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Permission Denial: reading ");
        stringBuilder2.append(getClass().getName());
        stringBuilder2.append(" uri ");
        stringBuilder2.append(uri2);
        stringBuilder2.append(" from pid=");
        stringBuilder2.append(pid);
        stringBuilder2.append(", uid=");
        stringBuilder2.append(uid);
        stringBuilder2.append(missingPerm);
        throw new SecurityException(stringBuilder2.toString());
    }

    protected int enforceWritePermissionInner(Uri uri, String callingPkg, IBinder callerToken) throws SecurityException {
        String str = callingPkg;
        IBinder iBinder = callerToken;
        Context context = getContext();
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();
        String missingPerm = null;
        int strongestMode = 0;
        if (UserHandle.isSameApp(uid, this.mMyUid)) {
            return 0;
        }
        String pathPerm;
        if (this.mExported && checkUser(pid, uid, context)) {
            boolean allowDefaultWrite;
            String componentPerm = getWritePermission();
            if (componentPerm != null) {
                int mode = checkPermissionAndAppOp(componentPerm, str, iBinder);
                if (mode == 0) {
                    return 0;
                }
                missingPerm = componentPerm;
                strongestMode = Math.max(0, mode);
            }
            boolean allowDefaultWrite2 = componentPerm == null;
            PathPermission[] pps = getPathPermissions();
            if (pps != null) {
                String path = uri.getPath();
                allowDefaultWrite = allowDefaultWrite2;
                allowDefaultWrite2 = strongestMode;
                String missingPerm2 = missingPerm;
                for (PathPermission pp : pps) {
                    pathPerm = pp.getWritePermission();
                    if (pathPerm == null || !pp.match(path)) {
                        missingPerm2 = missingPerm2;
                    } else {
                        missingPerm2 = checkPermissionAndAppOp(pathPerm, str, iBinder);
                        if (missingPerm2 == null) {
                            return 0;
                        }
                        allowDefaultWrite = false;
                        String missingPerm3 = pathPerm;
                        allowDefaultWrite2 = Math.max(allowDefaultWrite2, missingPerm2);
                        missingPerm2 = missingPerm3;
                    }
                }
                String str2 = missingPerm2;
                strongestMode = allowDefaultWrite2;
                missingPerm = str2;
            } else {
                allowDefaultWrite = allowDefaultWrite2;
            }
            if (allowDefaultWrite) {
                return 0;
            }
        }
        pathPerm = missingPerm;
        int strongestMode2 = strongestMode;
        if (context.checkUriPermission(uri, pid, uid, 2, iBinder) == 0) {
            return 0;
        }
        if (strongestMode2 == 1) {
            return 1;
        }
        if (this.mExported) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" requires ");
            stringBuilder.append(pathPerm);
            stringBuilder.append(", or grantUriPermission()");
            missingPerm = stringBuilder.toString();
        } else {
            missingPerm = " requires the provider be exported, or grantUriPermission()";
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Permission Denial: writing ");
        stringBuilder2.append(getClass().getName());
        stringBuilder2.append(" uri ");
        stringBuilder2.append(uri);
        stringBuilder2.append(" from pid=");
        stringBuilder2.append(pid);
        stringBuilder2.append(", uid=");
        stringBuilder2.append(uid);
        stringBuilder2.append(missingPerm);
        throw new SecurityException(stringBuilder2.toString());
    }

    public final Context getContext() {
        return this.mContext;
    }

    private String setCallingPackage(String callingPackage) {
        String original = (String) this.mCallingPackage.get();
        this.mCallingPackage.set(callingPackage);
        return original;
    }

    public final String getCallingPackage() {
        String pkg = (String) this.mCallingPackage.get();
        if (pkg != null) {
            this.mTransport.mAppOpsManager.checkPackage(Binder.getCallingUid(), pkg);
        }
        return pkg;
    }

    protected final void setAuthorities(String authorities) {
        if (authorities == null) {
            return;
        }
        if (authorities.indexOf(59) == -1) {
            this.mAuthority = authorities;
            this.mAuthorities = null;
            return;
        }
        this.mAuthority = null;
        this.mAuthorities = authorities.split(";");
    }

    protected final boolean matchesOurAuthorities(String authority) {
        if (this.mAuthority != null) {
            return this.mAuthority.equals(authority);
        }
        if (this.mAuthorities != null) {
            for (String equals : this.mAuthorities) {
                if (equals.equals(authority)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected final void setReadPermission(String permission) {
        this.mReadPermission = permission;
    }

    public final String getReadPermission() {
        return this.mReadPermission;
    }

    protected final void setWritePermission(String permission) {
        this.mWritePermission = permission;
    }

    public final String getWritePermission() {
        return this.mWritePermission;
    }

    protected final void setPathPermissions(PathPermission[] permissions) {
        this.mPathPermissions = permissions;
    }

    public final PathPermission[] getPathPermissions() {
        return this.mPathPermissions;
    }

    public final void setAppOps(int readOp, int writeOp) {
        if (!this.mNoPerms) {
            this.mTransport.mReadOp = readOp;
            this.mTransport.mWriteOp = writeOp;
        }
    }

    public AppOpsManager getAppOpsManager() {
        return this.mTransport.mAppOpsManager;
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void onLowMemory() {
    }

    public void onTrimMemory(int level) {
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        return query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public Cursor query(Uri uri, String[] projection, Bundle queryArgs, CancellationSignal cancellationSignal) {
        queryArgs = queryArgs != null ? queryArgs : Bundle.EMPTY;
        String sortClause = queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER);
        if (sortClause == null && queryArgs.containsKey(ContentResolver.QUERY_ARG_SORT_COLUMNS)) {
            sortClause = ContentResolver.createSqlSortClause(queryArgs);
        }
        return query(uri, projection, queryArgs.getString(ContentResolver.QUERY_ARG_SQL_SELECTION), queryArgs.getStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS), sortClause, cancellationSignal);
    }

    public Uri canonicalize(Uri url) {
        return null;
    }

    public Uri uncanonicalize(Uri url) {
        return url;
    }

    public boolean refresh(Uri uri, Bundle args, CancellationSignal cancellationSignal) {
        return false;
    }

    public Uri rejectInsert(Uri uri, ContentValues values) {
        return uri.buildUpon().appendPath("0").build();
    }

    public int bulkInsert(Uri uri, ContentValues[] values) {
        for (ContentValues insert : values) {
            insert(uri, insert);
        }
        return numValues;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No files supported by provider at ");
        stringBuilder.append(uri);
        throw new FileNotFoundException(stringBuilder.toString());
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        return openFile(uri, mode);
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        ParcelFileDescriptor fd = openFile(uri, mode);
        return fd != null ? new AssetFileDescriptor(fd, 0, -1) : null;
    }

    public AssetFileDescriptor openAssetFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        return openAssetFile(uri, mode);
    }

    protected final ParcelFileDescriptor openFileHelper(Uri uri, String mode) throws FileNotFoundException {
        Cursor c = query(uri, new String[]{"_data"}, null, null, null);
        int count = c != null ? c.getCount() : 0;
        if (count != 1) {
            if (c != null) {
                c.close();
            }
            StringBuilder stringBuilder;
            if (count == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("No entry for ");
                stringBuilder.append(uri);
                throw new FileNotFoundException(stringBuilder.toString());
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Multiple items at ");
            stringBuilder.append(uri);
            throw new FileNotFoundException(stringBuilder.toString());
        }
        c.moveToFirst();
        int i = c.getColumnIndex("_data");
        String path = i >= 0 ? c.getString(i) : null;
        c.close();
        if (path != null) {
            return ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.parseMode(mode));
        }
        throw new FileNotFoundException("Column _data not found.");
    }

    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        return null;
    }

    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts) throws FileNotFoundException {
        if ("*/*".equals(mimeTypeFilter)) {
            return openAssetFile(uri, FullBackup.ROOT_TREE_TOKEN);
        }
        String baseType = getType(uri);
        if (baseType != null && ClipDescription.compareMimeTypes(baseType, mimeTypeFilter)) {
            return openAssetFile(uri, FullBackup.ROOT_TREE_TOKEN);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't open ");
        stringBuilder.append(uri);
        stringBuilder.append(" as type ");
        stringBuilder.append(mimeTypeFilter);
        throw new FileNotFoundException(stringBuilder.toString());
    }

    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts, CancellationSignal signal) throws FileNotFoundException {
        return openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    public <T> ParcelFileDescriptor openPipeHelper(Uri uri, String mimeType, Bundle opts, T args, PipeDataWriter<T> func) throws FileNotFoundException {
        try {
            ParcelFileDescriptor[] fds = ParcelFileDescriptor.createPipe();
            final PipeDataWriter<T> pipeDataWriter = func;
            final ParcelFileDescriptor[] parcelFileDescriptorArr = fds;
            final Uri uri2 = uri;
            final String str = mimeType;
            final Bundle bundle = opts;
            final T t = args;
            new AsyncTask<Object, Object, Object>() {
                protected Object doInBackground(Object... params) {
                    pipeDataWriter.writeDataToPipe(parcelFileDescriptorArr[1], uri2, str, bundle, t);
                    try {
                        parcelFileDescriptorArr[1].close();
                    } catch (IOException e) {
                        Log.w(ContentProvider.TAG, "Failure closing pipe", e);
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[]) null);
            return fds[0];
        } catch (IOException e) {
            throw new FileNotFoundException("failure making pipe");
        }
    }

    protected boolean isTemporary() {
        return false;
    }

    public IContentProvider getIContentProvider() {
        return this.mTransport;
    }

    public void attachInfoForTesting(Context context, ProviderInfo info) {
        attachInfo(context, info, true);
    }

    public void attachInfo(Context context, ProviderInfo info) {
        attachInfo(context, info, false);
    }

    private void attachInfo(Context context, ProviderInfo info, boolean testing) {
        this.mNoPerms = testing;
        if (this.mContext == null) {
            this.mContext = context;
            if (!(context == null || this.mTransport == null)) {
                this.mTransport.mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            }
            this.mMyUid = Process.myUid();
            if (info != null) {
                setReadPermission(info.readPermission);
                setWritePermission(info.writePermission);
                setPathPermissions(info.pathPermissions);
                this.mExported = info.exported;
                this.mSingleUser = (info.flags & 1073741824) != 0;
                setAuthorities(info.authority);
            }
            if (Log.HWINFO) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getClass().getSimpleName());
                stringBuilder.append(".onCreate");
                Trace.traceBegin(64, stringBuilder.toString());
            }
            onCreate();
            if (Log.HWINFO) {
                Trace.traceEnd(64);
            }
        }
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        int numOperations = operations.size();
        ContentProviderResult[] results = new ContentProviderResult[numOperations];
        for (int i = 0; i < numOperations; i++) {
            results[i] = ((ContentProviderOperation) operations.get(i)).apply(this, results, i);
        }
        return results;
    }

    public Bundle call(String method, String arg, Bundle extras) {
        return null;
    }

    public void shutdown() {
        Log.w(TAG, "implement ContentProvider shutdown() to make sure all database connections are gracefully shutdown");
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("nothing to dump");
    }

    public Uri validateIncomingUri(Uri uri) throws SecurityException {
        String auth = uri.getAuthority();
        if (!this.mSingleUser) {
            int userId = getUserIdFromAuthority(auth, -2);
            if (!(userId == -2 || userId == this.mContext.getUserId())) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("trying to query a ContentProvider in user ");
                stringBuilder.append(this.mContext.getUserId());
                stringBuilder.append(" with a uri belonging to user ");
                stringBuilder.append(userId);
                throw new SecurityException(stringBuilder.toString());
            }
        }
        String encodedPath;
        if (matchesOurAuthorities(getAuthorityWithoutUserId(auth))) {
            encodedPath = uri.getEncodedPath();
            if (encodedPath == null || encodedPath.indexOf("//") == -1) {
                return uri;
            }
            Uri normalized = uri.buildUpon().encodedPath(encodedPath.replaceAll("//+", "/")).build();
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Normalized ");
            stringBuilder2.append(uri);
            stringBuilder2.append(" to ");
            stringBuilder2.append(normalized);
            stringBuilder2.append(" to avoid possible security issues");
            Log.w(str, stringBuilder2.toString());
            return normalized;
        }
        encodedPath = new StringBuilder();
        encodedPath.append("The authority of the uri ");
        encodedPath.append(uri);
        encodedPath.append(" does not match the one of the contentProvider: ");
        encodedPath = encodedPath.toString();
        StringBuilder stringBuilder3;
        if (this.mAuthority != null) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(encodedPath);
            stringBuilder3.append(this.mAuthority);
            encodedPath = stringBuilder3.toString();
        } else {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(encodedPath);
            stringBuilder3.append(Arrays.toString(this.mAuthorities));
            encodedPath = stringBuilder3.toString();
        }
        throw new SecurityException(encodedPath);
    }

    private Uri maybeGetUriWithoutUserId(Uri uri) {
        if (this.mSingleUser) {
            return uri;
        }
        return getUriWithoutUserId(uri);
    }

    public static int getUserIdFromAuthority(String auth, int defaultUserId) {
        if (auth == null) {
            return defaultUserId;
        }
        int end = auth.lastIndexOf(64);
        if (end == -1) {
            return defaultUserId;
        }
        try {
            return Integer.parseInt(auth.substring(null, end));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Error parsing userId.", e);
            return UserInfo.NO_PROFILE_GROUP_ID;
        }
    }

    public static int getUserIdFromAuthority(String auth) {
        return getUserIdFromAuthority(auth, -2);
    }

    public static int getUserIdFromUri(Uri uri, int defaultUserId) {
        if (uri == null) {
            return defaultUserId;
        }
        return getUserIdFromAuthority(uri.getAuthority(), defaultUserId);
    }

    public static int getUserIdFromUri(Uri uri) {
        return getUserIdFromUri(uri, -2);
    }

    public static String getAuthorityWithoutUserId(String auth) {
        if (auth == null) {
            return null;
        }
        return auth.substring(auth.lastIndexOf(64) + 1);
    }

    public static Uri getUriWithoutUserId(Uri uri) {
        if (uri == null) {
            return null;
        }
        Builder builder = uri.buildUpon();
        builder.authority(getAuthorityWithoutUserId(uri.getAuthority()));
        return builder.build();
    }

    public static boolean uriHasUserId(Uri uri) {
        if (uri == null) {
            return false;
        }
        return TextUtils.isEmpty(uri.getUserInfo()) ^ 1;
    }

    public static Uri maybeAddUserId(Uri uri, int userId) {
        if (uri == null) {
            return null;
        }
        if (userId == -2 || !"content".equals(uri.getScheme()) || uriHasUserId(uri)) {
            return uri;
        }
        Builder builder = uri.buildUpon();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(userId);
        stringBuilder.append("@");
        stringBuilder.append(uri.getEncodedAuthority());
        builder.encodedAuthority(stringBuilder.toString());
        return builder.build();
    }
}
