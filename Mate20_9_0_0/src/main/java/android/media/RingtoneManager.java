package android.media;

import android.Manifest.permission;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.database.StaleDataException;
import android.media.IAudioService.Stub;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.database.SortCursor;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class RingtoneManager {
    public static final String ACTION_RINGTONE_PICKER = "android.intent.action.RINGTONE_PICKER";
    public static final String EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS = "android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS";
    public static final String EXTRA_RINGTONE_DEFAULT_URI = "android.intent.extra.ringtone.DEFAULT_URI";
    public static final String EXTRA_RINGTONE_EXISTING_URI = "android.intent.extra.ringtone.EXISTING_URI";
    @Deprecated
    public static final String EXTRA_RINGTONE_INCLUDE_DRM = "android.intent.extra.ringtone.INCLUDE_DRM";
    public static final String EXTRA_RINGTONE_PICKED_URI = "android.intent.extra.ringtone.PICKED_URI";
    public static final String EXTRA_RINGTONE_SHOW_DEFAULT = "android.intent.extra.ringtone.SHOW_DEFAULT";
    public static final String EXTRA_RINGTONE_SHOW_SILENT = "android.intent.extra.ringtone.SHOW_SILENT";
    public static final String EXTRA_RINGTONE_TITLE = "android.intent.extra.ringtone.TITLE";
    public static final String EXTRA_RINGTONE_TYPE = "android.intent.extra.ringtone.TYPE";
    private static final int[] HW_RINGTONE_TYPES = new int[]{1, 2, 4, 8};
    public static final int HW_TYPE_ALL = 15;
    public static final int ID_COLUMN_INDEX = 0;
    private static final String[] INTERNAL_COLUMNS;
    private static final String[] MEDIA_COLUMNS;
    private static final int[] RINGTONE_TYPES = new int[]{1, 2, 4};
    private static final String TAG = "RingtoneManager";
    public static final int TITLE_COLUMN_INDEX = 1;
    public static final int TYPE_ALARM = 4;
    public static final int TYPE_ALL = 7;
    public static final int TYPE_NOTIFICATION = 2;
    public static final int TYPE_RINGTONE = 1;
    public static final int TYPE_RINGTONE2 = 8;
    public static final int URI_COLUMN_INDEX = 2;
    private static boolean mSetUriStat = false;
    private final Activity mActivity;
    private final Context mContext;
    private Cursor mCursor;
    private final List<String> mFilterColumns;
    private boolean mIncludeParentRingtones;
    private Ringtone mPreviousRingtone;
    private boolean mStopPreviousRingtone;
    private int mType;

    private class NewRingtoneScanner implements Closeable, MediaScannerConnectionClient {
        private File mFile;
        private MediaScannerConnection mMediaScannerConnection;
        private LinkedBlockingQueue<Uri> mQueue = new LinkedBlockingQueue(1);

        public NewRingtoneScanner(File file) {
            this.mFile = file;
            this.mMediaScannerConnection = new MediaScannerConnection(RingtoneManager.this.mContext, this);
            this.mMediaScannerConnection.connect();
        }

        public void close() {
            this.mMediaScannerConnection.disconnect();
        }

        public void onMediaScannerConnected() {
            this.mMediaScannerConnection.scanFile(this.mFile.getAbsolutePath(), null);
        }

        public void onScanCompleted(String path, Uri uri) {
            if (uri == null) {
                if (!this.mFile.delete()) {
                    Log.w(RingtoneManager.TAG, "Delete copied file failed when scan completed");
                }
                return;
            }
            try {
                this.mQueue.put(uri);
            } catch (InterruptedException e) {
                Log.e(RingtoneManager.TAG, "Unable to put new ringtone Uri in queue", e);
            }
        }

        public Uri take() throws InterruptedException {
            return (Uri) this.mQueue.take();
        }
    }

    static {
        String[] strArr = new String[4];
        strArr[0] = DownloadManager.COLUMN_ID;
        strArr[1] = "title";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"");
        stringBuilder.append(Media.INTERNAL_CONTENT_URI);
        stringBuilder.append("\"");
        strArr[2] = stringBuilder.toString();
        strArr[3] = "title_key";
        INTERNAL_COLUMNS = strArr;
        String[] strArr2 = new String[4];
        strArr2[0] = DownloadManager.COLUMN_ID;
        strArr2[1] = "title";
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("\"");
        stringBuilder2.append(Media.EXTERNAL_CONTENT_URI);
        stringBuilder2.append("\"");
        strArr2[2] = stringBuilder2.toString();
        strArr2[3] = "title_key";
        MEDIA_COLUMNS = strArr2;
    }

    public RingtoneManager(Activity activity) {
        this(activity, false);
    }

    public RingtoneManager(Activity activity, boolean includeParentRingtones) {
        this.mType = 1;
        this.mFilterColumns = new ArrayList();
        this.mStopPreviousRingtone = true;
        this.mActivity = activity;
        this.mContext = activity;
        setType(this.mType);
        this.mIncludeParentRingtones = includeParentRingtones;
    }

    public RingtoneManager(Context context) {
        this(context, false);
    }

    public RingtoneManager(Context context, boolean includeParentRingtones) {
        this.mType = 1;
        this.mFilterColumns = new ArrayList();
        this.mStopPreviousRingtone = true;
        this.mActivity = null;
        this.mContext = context;
        setType(this.mType);
        this.mIncludeParentRingtones = includeParentRingtones;
    }

    public void setType(int type) {
        if (this.mCursor == null) {
            this.mType = type;
            setFilterColumnsList(type);
            return;
        }
        throw new IllegalStateException("Setting filter columns should be done before querying for ringtones.");
    }

    public int inferStreamType() {
        int i = this.mType;
        if (i != 2) {
            return i != 4 ? 2 : 4;
        } else {
            return 5;
        }
    }

    public void setStopPreviousRingtone(boolean stopPreviousRingtone) {
        this.mStopPreviousRingtone = stopPreviousRingtone;
    }

    public boolean getStopPreviousRingtone() {
        return this.mStopPreviousRingtone;
    }

    public void stopPreviousRingtone() {
        if (this.mPreviousRingtone != null) {
            this.mPreviousRingtone.stop();
        }
    }

    @Deprecated
    public boolean getIncludeDrm() {
        return false;
    }

    @Deprecated
    public void setIncludeDrm(boolean includeDrm) {
        if (includeDrm) {
            Log.w(TAG, "setIncludeDrm no longer supported");
        }
    }

    public Cursor getCursor() {
        try {
            if (!(this.mCursor == null || this.mCursor.isClosed() || !this.mCursor.requery())) {
                return this.mCursor;
            }
        } catch (StaleDataException e) {
            Log.w(TAG, "requery failded: ");
            this.mCursor = null;
        }
        ArrayList<Cursor> ringtoneCursors = new ArrayList();
        ringtoneCursors.add(getInternalRingtones());
        ringtoneCursors.add(getMediaRingtones());
        if (this.mIncludeParentRingtones) {
            Cursor parentRingtonesCursor = getParentProfileRingtones();
            if (parentRingtonesCursor != null) {
                ringtoneCursors.add(parentRingtonesCursor);
            }
        }
        SortCursor sortCursor = new SortCursor((Cursor[]) ringtoneCursors.toArray(new Cursor[ringtoneCursors.size()]), "title_key");
        this.mCursor = sortCursor;
        return sortCursor;
    }

    private Cursor getParentProfileRingtones() {
        UserInfo parentInfo = UserManager.get(this.mContext).getProfileParent(this.mContext.getUserId());
        if (!(parentInfo == null || parentInfo.id == this.mContext.getUserId())) {
            Context parentContext = createPackageContextAsUser(this.mContext, parentInfo.id);
            if (parentContext != null) {
                return new ExternalRingtonesCursorWrapper(getMediaRingtones(parentContext), parentInfo.id);
            }
        }
        return null;
    }

    public Ringtone getRingtone(int position) {
        if (this.mStopPreviousRingtone && this.mPreviousRingtone != null) {
            this.mPreviousRingtone.stop();
        }
        this.mPreviousRingtone = getRingtone(this.mContext, getRingtoneUri(position), inferStreamType());
        return this.mPreviousRingtone;
    }

    public Uri getRingtoneUri(int position) {
        try {
            if (this.mCursor == null || !this.mCursor.moveToPosition(position)) {
                return null;
            }
            return getUriFromCursor(this.mCursor);
        } catch (IllegalStateException e) {
            Log.e(TAG, "attempt to re-open an already-closed object");
            return null;
        } catch (StaleDataException staleDataException) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getRingtoneUri -- ");
            stringBuilder.append(position);
            Log.e(str, stringBuilder.toString(), staleDataException);
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:21:0x0058, code skipped:
            if (r0 != null) goto L_0x005a;
     */
    /* JADX WARNING: Missing block: B:22:0x005a, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Uri getExistingRingtoneUriFromPath(Context context, String path) {
        Cursor cursor = context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, new String[]{DownloadManager.COLUMN_ID}, "_data=? ", new String[]{path}, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                if (id == -1) {
                    if (cursor != null) {
                        $closeResource(null, cursor);
                    }
                    return null;
                }
                Uri uri = Media.EXTERNAL_CONTENT_URI;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(id);
                uri = Uri.withAppendedPath(uri, stringBuilder.toString());
                if (cursor != null) {
                    $closeResource(null, cursor);
                }
                return uri;
            }
        }
        if (cursor != null) {
            $closeResource(null, cursor);
        }
        return null;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private static Uri getUriFromCursor(Cursor cursor) {
        try {
            return ContentUris.withAppendedId(Uri.parse(cursor.getString(2)), cursor.getLong(0));
        } catch (Exception e) {
            Log.e(TAG, "Failed to get uri from cursor!!!!!!!!!!!!");
            return null;
        }
    }

    public int getRingtonePosition(Uri ringtoneUri) {
        if (ringtoneUri == null) {
            return -1;
        }
        Cursor cursor = getCursor();
        int cursorCount = cursor.getCount();
        if (!cursor.moveToFirst()) {
            return -1;
        }
        String previousUriString = null;
        Uri currentUri = null;
        for (int i = 0; i < cursorCount; i++) {
            String uriString = cursor.getString(2);
            if (currentUri == null || !uriString.equals(previousUriString)) {
                currentUri = Uri.parse(uriString);
            }
            if (ringtoneUri.equals(ContentUris.withAppendedId(currentUri, cursor.getLong(0)))) {
                return i;
            }
            cursor.move(1);
            previousUriString = uriString;
        }
        return -1;
    }

    public static Uri getValidRingtoneUri(Context context) {
        RingtoneManager rm = new RingtoneManager(context);
        Uri uri = getValidRingtoneUriFromCursorAndClose(context, rm.getInternalRingtones());
        if (uri == null) {
            return getValidRingtoneUriFromCursorAndClose(context, rm.getMediaRingtones());
        }
        return uri;
    }

    private static Uri getValidRingtoneUriFromCursorAndClose(Context context, Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        Uri uri = null;
        if (cursor.moveToFirst()) {
            uri = getUriFromCursor(cursor);
        }
        cursor.close();
        return uri;
    }

    private Cursor getInternalRingtones() {
        return query(Media.INTERNAL_CONTENT_URI, INTERNAL_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns), null, "title_key");
    }

    private Cursor getMediaRingtones() {
        return getMediaRingtones(this.mContext);
    }

    private Cursor getMediaRingtones(Context context) {
        Cursor cursor = null;
        if (context.checkPermission(permission.READ_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != 0) {
            Log.w(TAG, "No READ_EXTERNAL_STORAGE permission, ignoring ringtones on ext storage");
            return null;
        }
        String status = Environment.getExternalStorageState();
        if (status.equals("mounted") || status.equals("mounted_ro")) {
            cursor = query(Media.EXTERNAL_CONTENT_URI, MEDIA_COLUMNS, constructBooleanTrueWhereClause(this.mFilterColumns), null, "title_key", context);
        }
        return cursor;
    }

    private void setFilterColumnsList(int type) {
        List<String> columns = this.mFilterColumns;
        columns.clear();
        if ((type & 1) != 0) {
            columns.add("is_ringtone");
        }
        if ((type & 8) != 0) {
            columns.add("is_ringtone");
        }
        if ((type & 2) != 0) {
            columns.add("is_notification");
        }
        if ((type & 4) != 0) {
            columns.add("is_alarm");
        }
    }

    private static String constructBooleanTrueWhereClause(List<String> columns) {
        if (columns == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = columns.size() - 1; i >= 0; i--) {
            sb.append((String) columns.get(i));
            sb.append("=1 or ");
        }
        if (columns.size() > 0) {
            sb.setLength(sb.length() - 4);
        }
        sb.append(" and is_drm =0)");
        return sb.toString();
    }

    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return query(uri, projection, selection, selectionArgs, sortOrder, this.mContext);
    }

    private Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, Context context) {
        if (this.mActivity != null) {
            return this.mActivity.managedQuery(uri, projection, selection, selectionArgs, sortOrder);
        }
        return context.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public static Ringtone getRingtone(Context context, Uri ringtoneUri) {
        return getRingtone(context, ringtoneUri, -1);
    }

    private static Ringtone getRingtone(Context context, Uri ringtoneUri, int streamType) {
        try {
            mSetUriStat = false;
            Ringtone r = new Ringtone(context, true);
            if (streamType >= 0) {
                r.setStreamType(streamType);
            }
            if (ringtoneUri == null) {
                Log.w(TAG, "ringtoneUri is null ...");
                return null;
            }
            r.setUri(ringtoneUri);
            if (r.getPrepareStat()) {
                Log.w(TAG, "prepare failed ......");
                mSetUriStat = true;
            }
            return r;
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to open ringtone ");
            stringBuilder.append(ringtoneUri);
            stringBuilder.append(": ");
            stringBuilder.append(ex);
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0032, code skipped:
            if (r1 != null) goto L_0x0034;
     */
    /* JADX WARNING: Missing block: B:12:0x0034, code skipped:
            $closeResource(r2, r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private File getRingtonePathFromUri(Uri uri) {
        String[] projection = new String[]{"_data"};
        setFilterColumnsList(7);
        String path = null;
        Cursor cursor = query(uri, projection, constructBooleanTrueWhereClause(this.mFilterColumns), null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex("_data"));
            }
        }
        if (cursor != null) {
            $closeResource(null, cursor);
        }
        if (path != null) {
            return new File(path);
        }
        return null;
    }

    public static boolean getSetUriStat() {
        return mSetUriStat;
    }

    public static void disableSyncFromParent(Context userContext) {
        try {
            Stub.asInterface(ServiceManager.getService("audio")).disableRingtoneSync(userContext.getUserId());
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to disable ringtone sync.");
        }
    }

    public static void enableSyncFromParent(Context userContext) {
        Secure.putIntForUser(userContext.getContentResolver(), "sync_parent_sounds", 1, userContext.getUserId());
    }

    public static Uri getActualDefaultRingtoneUri(Context context, int type) {
        String setting = getSettingForType(type);
        Uri ringtoneUri = null;
        if (setting == null) {
            return null;
        }
        String uriString = System.getStringForUser(context.getContentResolver(), setting, context.getUserId());
        if (uriString != null) {
            ringtoneUri = Uri.parse(uriString);
        }
        if (ringtoneUri != null && ContentProvider.getUserIdFromUri(ringtoneUri) == context.getUserId()) {
            ringtoneUri = ContentProvider.getUriWithoutUserId(ringtoneUri);
        }
        return ringtoneUri;
    }

    public static void setActualDefaultRingtoneUri(Context context, int type, Uri ringtoneUri) {
        OutputStream out;
        Throwable th;
        Throwable th2;
        String setting = getSettingForType(type);
        if (setting != null) {
            ContentResolver resolver = context.getContentResolver();
            if (Secure.getIntForUser(resolver, "sync_parent_sounds", 0, context.getUserId()) == 1) {
                disableSyncFromParent(context);
            }
            if (!isInternalRingtoneUri(ringtoneUri)) {
                ringtoneUri = ContentProvider.maybeAddUserId(ringtoneUri, context.getUserId());
            }
            System.putStringForUser(resolver, setting, ringtoneUri != null ? ringtoneUri.toString() : null, context.getUserId());
            if (ringtoneUri != null) {
                String actualUri = MediaStore.getPath(context, ringtoneUri);
                if (actualUri == null || !actualUri.endsWith(".isma")) {
                    Uri cacheUri = getCacheForType(type, context.getUserId());
                    InputStream in;
                    try {
                        in = openRingtone(context, ringtoneUri);
                        out = resolver.openOutputStream(cacheUri);
                        try {
                            FileUtils.copy(in, out);
                            if (out != null) {
                                $closeResource(null, out);
                            }
                            if (in != null) {
                                $closeResource(null, in);
                            }
                        } catch (Throwable th22) {
                            Throwable th3 = th22;
                            th22 = th;
                            th = th3;
                        }
                    } catch (IOException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to cache ringtone: ");
                        stringBuilder.append(e);
                        Log.w(str, stringBuilder.toString());
                    } catch (Throwable th4) {
                        if (in != null) {
                            $closeResource(r2, in);
                        }
                    }
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setActualDefaultRingtoneUri actualUri = ");
                    stringBuilder2.append(actualUri);
                    Log.d(str2, stringBuilder2.toString());
                    Toast.makeText(context, R.string.ringtone_unknown, 1).show();
                    return;
                }
            }
            return;
        }
        return;
        if (out != null) {
            $closeResource(th22, out);
        }
        throw th;
    }

    private static boolean isInternalRingtoneUri(Uri uri) {
        return isRingtoneUriInStorage(uri, Media.INTERNAL_CONTENT_URI);
    }

    private static boolean isExternalRingtoneUri(Uri uri) {
        return isRingtoneUriInStorage(uri, Media.EXTERNAL_CONTENT_URI);
    }

    private static boolean isRingtoneUriInStorage(Uri ringtone, Uri storage) {
        Uri uriWithoutUserId = ContentProvider.getUriWithoutUserId(ringtone);
        if (uriWithoutUserId == null) {
            return false;
        }
        return uriWithoutUserId.toString().startsWith(storage.toString());
    }

    public boolean isCustomRingtone(Uri uri) {
        if (!isExternalRingtoneUri(uri)) {
            return false;
        }
        File parent = null;
        File ringtoneFile = uri == null ? null : getRingtonePathFromUri(uri);
        if (ringtoneFile != null) {
            parent = ringtoneFile.getParentFile();
        }
        if (parent == null) {
            return false;
        }
        for (String directory : new String[]{Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_NOTIFICATIONS, Environment.DIRECTORY_ALARMS}) {
            if (parent.equals(Environment.getExternalStoragePublicDirectory(directory))) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:48:0x0084, code skipped:
            if (r3 != null) goto L_0x0086;
     */
    /* JADX WARNING: Missing block: B:49:0x0086, code skipped:
            $closeResource(r4, r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Uri addCustomExternalRingtone(Uri fileUri, int type) throws FileNotFoundException, IllegalArgumentException, IOException {
        OutputStream output;
        Throwable th;
        Throwable th2;
        if (Environment.getExternalStorageState().equals("mounted")) {
            String mimeType = this.mContext.getContentResolver().getType(fileUri);
            if (mimeType == null || !(mimeType.startsWith("audio/") || mimeType.equals("application/ogg"))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ringtone file must have MIME type \"audio/*\". Given file has MIME type \"");
                stringBuilder.append(mimeType);
                stringBuilder.append("\"");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            File outFile = Utils.getUniqueExternalFile(this.mContext, getExternalDirectoryForType(type), Utils.getFileDisplayNameFromUri(this.mContext, fileUri), mimeType);
            InputStream input = this.mContext.getContentResolver().openInputStream(fileUri);
            output = new FileOutputStream(outFile);
            try {
                FileUtils.copy(input, output);
                $closeResource(null, output);
                if (input != null) {
                    $closeResource(null, input);
                }
                NewRingtoneScanner scanner;
                try {
                    scanner = new NewRingtoneScanner(outFile);
                    Uri take = scanner.take();
                    $closeResource(null, scanner);
                    return take;
                } catch (InterruptedException input2) {
                    throw new IOException("Audio file failed to scan as a ringtone", input2);
                } catch (Throwable th3) {
                    $closeResource(r4, scanner);
                }
            } catch (Throwable th22) {
                Throwable th4 = th22;
                th22 = th;
                th = th4;
            }
        } else {
            throw new IOException("External storage is not mounted. Unable to install ringtones.");
        }
        $closeResource(th22, output);
        throw th;
    }

    private static final String getExternalDirectoryForType(int type) {
        if (type == 4) {
            return Environment.DIRECTORY_ALARMS;
        }
        switch (type) {
            case 1:
                return Environment.DIRECTORY_RINGTONES;
            case 2:
                return Environment.DIRECTORY_NOTIFICATIONS;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported ringtone type: ");
                stringBuilder.append(type);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public boolean deleteExternalRingtone(Uri uri) {
        if (!isCustomRingtone(uri)) {
            return false;
        }
        File ringtoneFile = getRingtonePathFromUri(uri);
        if (ringtoneFile != null) {
            try {
                if (this.mContext.getContentResolver().delete(uri, null, null) > 0) {
                    return ringtoneFile.delete();
                }
            } catch (SecurityException e) {
                Log.d(TAG, "Unable to delete custom ringtone", e);
            }
        }
        return false;
    }

    private static InputStream openRingtone(Context context, Uri uri) throws IOException {
        try {
            return context.getContentResolver().openInputStream(uri);
        } catch (IOException | SecurityException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to open directly; attempting failover: ");
            stringBuilder.append(e);
            Log.w(str, stringBuilder.toString());
            try {
                return new AutoCloseInputStream(((AudioManager) context.getSystemService(AudioManager.class)).getRingtonePlayer().openRingtone(uri));
            } catch (Exception e2) {
                throw new IOException(e2);
            }
        }
    }

    private static String getSettingForType(int type) {
        if ((type & 1) != 0) {
            return "ringtone";
        }
        if ((type & 8) != 0) {
            return "ringtone2";
        }
        if ((type & 2) != 0) {
            return "notification_sound";
        }
        if ((type & 4) != 0) {
            return "alarm_alert";
        }
        return null;
    }

    public static Uri getCacheForType(int type) {
        return getCacheForType(type, UserHandle.getCallingUserId());
    }

    public static Uri getCacheForType(int type, int userId) {
        if ((type & 1) != 0) {
            return ContentProvider.maybeAddUserId(System.RINGTONE_CACHE_URI, userId);
        }
        if ((type & 8) != 0) {
            return ContentProvider.maybeAddUserId(System.RINGTONE2_CACHE_URI, userId);
        }
        if ((type & 2) != 0) {
            return ContentProvider.maybeAddUserId(System.NOTIFICATION_SOUND_CACHE_URI, userId);
        }
        if ((type & 4) != 0) {
            return ContentProvider.maybeAddUserId(System.ALARM_ALERT_CACHE_URI, userId);
        }
        return null;
    }

    public static boolean isDefault(Uri ringtoneUri) {
        return getDefaultType(ringtoneUri) != -1;
    }

    public static int getDefaultType(Uri defaultRingtoneUri) {
        defaultRingtoneUri = ContentProvider.getUriWithoutUserId(defaultRingtoneUri);
        if (defaultRingtoneUri == null) {
            return -1;
        }
        if (defaultRingtoneUri.equals(System.HUAWEI_RINGTONE2_URI)) {
            return 8;
        }
        if (defaultRingtoneUri.equals(System.DEFAULT_RINGTONE_URI)) {
            return 1;
        }
        if (defaultRingtoneUri.equals(System.DEFAULT_NOTIFICATION_URI)) {
            return 2;
        }
        if (defaultRingtoneUri.equals(System.DEFAULT_ALARM_ALERT_URI)) {
            return 4;
        }
        return -1;
    }

    public static Uri getDefaultUri(int type) {
        if ((type & 1) != 0) {
            return System.DEFAULT_RINGTONE_URI;
        }
        if ((type & 8) != 0) {
            return System.HUAWEI_RINGTONE2_URI;
        }
        if ((type & 2) != 0) {
            return System.DEFAULT_NOTIFICATION_URI;
        }
        if ((type & 4) != 0) {
            return System.DEFAULT_ALARM_ALERT_URI;
        }
        return null;
    }

    private static Context createPackageContextAsUser(Context context, int userId) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, UserHandle.of(userId));
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to create package context", e);
            return null;
        }
    }
}
