package android.app;

import android.annotation.SystemApi;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.backup.FullBackup;
import android.common.HwFrameworkFactory;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.Downloads.Impl;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.text.TextUtils;
import android.util.Pair;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class DownloadManager {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public static final String ACTION_DOWNLOAD_COMPLETE = "android.intent.action.DOWNLOAD_COMPLETE";
    @SystemApi
    public static final String ACTION_DOWNLOAD_COMPLETED = "android.intent.action.DOWNLOAD_COMPLETED";
    public static final String ACTION_NOTIFICATION_CLICKED = "android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED";
    public static final String ACTION_VIEW_DOWNLOADS = "android.intent.action.VIEW_DOWNLOADS";
    public static final String COLUMN_ALLOW_WRITE = "allow_write";
    public static final String COLUMN_BYTES_DOWNLOADED_SO_FAR = "bytes_so_far";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
    @Deprecated
    public static final String COLUMN_LOCAL_FILENAME = "local_filename";
    public static final String COLUMN_LOCAL_URI = "local_uri";
    public static final String COLUMN_MEDIAPROVIDER_URI = "mediaprovider_uri";
    public static final String COLUMN_MEDIA_TYPE = "media_type";
    public static final String COLUMN_REASON = "reason";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_TOTAL_SIZE_BYTES = "total_size";
    public static final String COLUMN_URI = "uri";
    public static final int ERROR_BLOCKED = 1010;
    public static final int ERROR_CANNOT_RESUME = 1008;
    public static final int ERROR_DEVICE_NOT_FOUND = 1007;
    public static final int ERROR_FILE_ALREADY_EXISTS = 1009;
    public static final int ERROR_FILE_ERROR = 1001;
    public static final int ERROR_HTTP_DATA_ERROR = 1004;
    public static final int ERROR_INSUFFICIENT_SPACE = 1006;
    public static final int ERROR_TOO_MANY_REDIRECTS = 1005;
    public static final int ERROR_UNHANDLED_HTTP_CODE = 1002;
    public static final int ERROR_UNKNOWN = 1000;
    public static final String EXTRA_DOWNLOAD_ID = "extra_download_id";
    public static final String EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS = "extra_click_download_ids";
    public static final String INTENT_EXTRAS_SORT_BY_SIZE = "android.app.DownloadManager.extra_sortBySize";
    private static final String NON_DOWNLOADMANAGER_DOWNLOAD = "non-dwnldmngr-download-dont-retry2download";
    public static final int PAUSED_QUEUED_FOR_WIFI = 3;
    public static final int PAUSED_UNKNOWN = 4;
    public static final int PAUSED_WAITING_FOR_NETWORK = 2;
    public static final int PAUSED_WAITING_TO_RETRY = 1;
    public static final int STATUS_FAILED = 16;
    public static final int STATUS_PAUSED = 4;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_SUCCESSFUL = 8;
    public static final String[] UNDERLYING_COLUMNS = new String[]{COLUMN_ID, "_data AS local_filename", COLUMN_MEDIAPROVIDER_URI, "destination", "title", "description", COLUMN_URI, "status", "hint", "mimetype AS media_type", "total_bytes AS total_size", "lastmod AS last_modified_timestamp", "current_bytes AS bytes_so_far", COLUMN_ALLOW_WRITE, "'placeholder' AS local_uri", "'placeholder' AS reason"};
    private boolean mAccessFilename;
    private Uri mBaseUri = Impl.CONTENT_URI;
    private final String mPackageName;
    private final ContentResolver mResolver;

    public static class Query {
        public static final int ORDER_ASCENDING = 1;
        public static final int ORDER_DESCENDING = 2;
        private String mFilterString = null;
        private long[] mIds = null;
        private boolean mOnlyIncludeVisibleInDownloadsUi = false;
        private String mOrderByColumn = "lastmod";
        private int mOrderDirection = 2;
        private Integer mStatusFlags = null;

        public Query setFilterById(long... ids) {
            this.mIds = ids;
            return this;
        }

        public Query setFilterByString(String filter) {
            this.mFilterString = filter;
            return this;
        }

        public Query setFilterByStatus(int flags) {
            this.mStatusFlags = Integer.valueOf(flags);
            return this;
        }

        public Query setOnlyIncludeVisibleInDownloadsUi(boolean value) {
            this.mOnlyIncludeVisibleInDownloadsUi = value;
            return this;
        }

        public Query orderBy(String column, int direction) {
            StringBuilder stringBuilder;
            if (direction == 1 || direction == 2) {
                if (column.equals(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)) {
                    this.mOrderByColumn = "lastmod";
                } else if (column.equals(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)) {
                    this.mOrderByColumn = "total_bytes";
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot order by ");
                    stringBuilder.append(column);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                this.mOrderDirection = direction;
                return this;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid direction: ");
            stringBuilder.append(direction);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        Cursor runQuery(ContentResolver resolver, String[] projection, Uri baseUri) {
            StringBuilder stringBuilder;
            Uri uri = baseUri;
            ArrayList selectionParts = new ArrayList();
            int whereArgsCount = this.mIds == null ? 0 : this.mIds.length;
            int whereArgsCount2 = this.mFilterString == null ? whereArgsCount : whereArgsCount + 1;
            String[] selectionArgs = new String[whereArgsCount2];
            if (whereArgsCount2 > 0) {
                if (this.mIds != null) {
                    selectionParts.add(DownloadManager.getWhereClauseForIds(this.mIds));
                    DownloadManager.getWhereArgsForIds(this.mIds, selectionArgs);
                }
                if (this.mFilterString != null) {
                    selectionParts.add("title LIKE ?");
                    whereArgsCount = selectionArgs.length - 1;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("%");
                    stringBuilder.append(this.mFilterString);
                    stringBuilder.append("%");
                    selectionArgs[whereArgsCount] = stringBuilder.toString();
                }
            }
            if (this.mStatusFlags != null) {
                List<String> parts = new ArrayList();
                if ((this.mStatusFlags.intValue() & 1) != 0) {
                    parts.add(statusClause("=", 190));
                }
                if ((this.mStatusFlags.intValue() & 2) != 0) {
                    parts.add(statusClause("=", 192));
                }
                if ((this.mStatusFlags.intValue() & 4) != 0) {
                    parts.add(statusClause("=", 193));
                    parts.add(statusClause("=", 194));
                    parts.add(statusClause("=", 195));
                    parts.add(statusClause("=", 196));
                }
                if ((this.mStatusFlags.intValue() & 8) != 0) {
                    parts.add(statusClause("=", 200));
                }
                if ((this.mStatusFlags.intValue() & 16) != 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("(");
                    stringBuilder.append(statusClause(">=", 400));
                    stringBuilder.append(" AND ");
                    stringBuilder.append(statusClause("<", 600));
                    stringBuilder.append(")");
                    parts.add(stringBuilder.toString());
                }
                selectionParts.add(joinStrings(" OR ", parts));
            }
            if (this.mOnlyIncludeVisibleInDownloadsUi) {
                selectionParts.add("is_visible_in_downloads_ui != '0'");
            }
            selectionParts.add("deleted != '1'");
            String selection = joinStrings(" AND ", selectionParts);
            String orderDirection = this.mOrderDirection == 1 ? "ASC" : "DESC";
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mOrderByColumn);
            stringBuilder2.append(" ");
            stringBuilder2.append(orderDirection);
            return resolver.query(uri, projection, selection, selectionArgs, stringBuilder2.toString());
        }

        private String joinStrings(String joiner, Iterable<String> parts) {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (String part : parts) {
                if (!first) {
                    builder.append(joiner);
                }
                builder.append(part);
                first = false;
            }
            return builder.toString();
        }

        private String statusClause(String operator, int value) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("status");
            stringBuilder.append(operator);
            stringBuilder.append("'");
            stringBuilder.append(value);
            stringBuilder.append("'");
            return stringBuilder.toString();
        }
    }

    public static class Request {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        @Deprecated
        public static final int NETWORK_BLUETOOTH = 4;
        public static final int NETWORK_MOBILE = 1;
        public static final int NETWORK_WIFI = 2;
        private static final int SCANNABLE_VALUE_NO = 2;
        private static final int SCANNABLE_VALUE_YES = 0;
        public static final int VISIBILITY_HIDDEN = 2;
        public static final int VISIBILITY_VISIBLE = 0;
        public static final int VISIBILITY_VISIBLE_NOTIFY_COMPLETED = 1;
        public static final int VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION = 3;
        private int mAllowedNetworkTypes = -1;
        private CharSequence mDescription;
        private Uri mDestinationUri;
        private int mFlags = 0;
        private boolean mIsVisibleInDownloadsUi = true;
        private boolean mMeteredAllowed = true;
        private String mMimeType;
        private int mNotificationVisibility = 0;
        private List<Pair<String, String>> mRequestHeaders = new ArrayList();
        private boolean mRoamingAllowed = true;
        private boolean mScannable = false;
        private CharSequence mTitle;
        private Uri mUri;

        static {
            Class cls = DownloadManager.class;
        }

        public Request(Uri uri) {
            if (uri != null) {
                String scheme = uri.getScheme();
                if (scheme == null || !(scheme.equals(IntentFilter.SCHEME_HTTP) || scheme.equals(IntentFilter.SCHEME_HTTPS))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can only download HTTP/HTTPS URIs: ");
                    stringBuilder.append(uri);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                this.mUri = uri;
                return;
            }
            throw new NullPointerException();
        }

        Request(String uriString) {
            this.mUri = Uri.parse(uriString);
        }

        public Request setDestinationUri(Uri uri) {
            this.mDestinationUri = uri;
            return this;
        }

        public Request setDestinationInExternalFilesDir(Context context, String dirType, String subPath) {
            File file = context.getExternalFilesDir(dirType);
            if (file != null) {
                StringBuilder stringBuilder;
                if (file.exists()) {
                    if (!file.isDirectory()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(file.getAbsolutePath());
                        stringBuilder.append(" already exists and is not a directory");
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                } else if (!file.mkdirs()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to create directory: ");
                    stringBuilder.append(file.getAbsolutePath());
                    throw new IllegalStateException(stringBuilder.toString());
                }
                setDestinationFromBase(file, subPath);
                return this;
            }
            throw new IllegalStateException("Failed to get external storage files directory");
        }

        public Request setDestinationInExternalPublicDir(String dirType, String subPath) {
            File file = Environment.getExternalStoragePublicDirectory(dirType);
            if (file != null) {
                StringBuilder stringBuilder;
                if (file.exists()) {
                    if (!file.isDirectory()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(file.getAbsolutePath());
                        stringBuilder.append(" already exists and is not a directory");
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                } else if (!file.mkdirs()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to create directory: ");
                    stringBuilder.append(file.getAbsolutePath());
                    throw new IllegalStateException(stringBuilder.toString());
                }
                setDestinationFromBase(file, subPath);
                return this;
            }
            throw new IllegalStateException("Failed to get external storage public directory");
        }

        private void setDestinationFromBase(File base, String subPath) {
            if (subPath != null) {
                this.mDestinationUri = Uri.withAppendedPath(Uri.fromFile(base), subPath);
                return;
            }
            throw new NullPointerException("subPath cannot be null");
        }

        public void allowScanningByMediaScanner() {
            this.mScannable = true;
        }

        public Request addRequestHeader(String header, String value) {
            if (header == null) {
                throw new NullPointerException("header cannot be null");
            } else if (header.contains(":")) {
                throw new IllegalArgumentException("header may not contain ':'");
            } else {
                Object value2;
                if (value2 == null) {
                    value2 = "";
                }
                this.mRequestHeaders.add(Pair.create(header, value2));
                return this;
            }
        }

        public Request setTitle(CharSequence title) {
            this.mTitle = title;
            return this;
        }

        public Request setDescription(CharSequence description) {
            this.mDescription = description;
            return this;
        }

        public Request setMimeType(String mimeType) {
            this.mMimeType = mimeType;
            return this;
        }

        @Deprecated
        public Request setShowRunningNotification(boolean show) {
            if (show) {
                return setNotificationVisibility(0);
            }
            return setNotificationVisibility(2);
        }

        public Request setNotificationVisibility(int visibility) {
            this.mNotificationVisibility = visibility;
            return this;
        }

        public Request setAllowedNetworkTypes(int flags) {
            this.mAllowedNetworkTypes = flags;
            return this;
        }

        public Request setAllowedOverRoaming(boolean allowed) {
            this.mRoamingAllowed = allowed;
            return this;
        }

        public Request setAllowedOverMetered(boolean allow) {
            this.mMeteredAllowed = allow;
            return this;
        }

        public Request setRequiresCharging(boolean requiresCharging) {
            if (requiresCharging) {
                this.mFlags |= 1;
            } else {
                this.mFlags &= -2;
            }
            return this;
        }

        public Request setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            if (requiresDeviceIdle) {
                this.mFlags |= 2;
            } else {
                this.mFlags &= -3;
            }
            return this;
        }

        public Request setVisibleInDownloadsUi(boolean isVisible) {
            this.mIsVisibleInDownloadsUi = isVisible;
            return this;
        }

        ContentValues toContentValues(String packageName) {
            ContentValues values = new ContentValues();
            values.put(DownloadManager.COLUMN_URI, this.mUri.toString());
            values.put("is_public_api", Boolean.valueOf(true));
            values.put("notificationpackage", packageName);
            int i = 2;
            if (this.mDestinationUri != null) {
                values.put("destination", Integer.valueOf(4));
                values.put("hint", this.mDestinationUri.toString());
            } else {
                values.put("destination", Integer.valueOf(2));
            }
            String str = "scanned";
            if (this.mScannable) {
                i = 0;
            }
            values.put(str, Integer.valueOf(i));
            if (!this.mRequestHeaders.isEmpty()) {
                encodeHttpHeaders(values);
            }
            putIfNonNull(values, "title", this.mTitle);
            putIfNonNull(values, "description", this.mDescription);
            putIfNonNull(values, "mimetype", this.mMimeType);
            values.put("visibility", Integer.valueOf(this.mNotificationVisibility));
            values.put("allowed_network_types", Integer.valueOf(this.mAllowedNetworkTypes));
            values.put("allow_roaming", Boolean.valueOf(this.mRoamingAllowed));
            values.put(ContentResolver.SYNC_EXTRAS_DISALLOW_METERED, Boolean.valueOf(this.mMeteredAllowed));
            values.put("flags", Integer.valueOf(this.mFlags));
            values.put("is_visible_in_downloads_ui", Boolean.valueOf(this.mIsVisibleInDownloadsUi));
            return values;
        }

        private void encodeHttpHeaders(ContentValues values) {
            int index = 0;
            for (Pair<String, String> header : this.mRequestHeaders) {
                String headerString = new StringBuilder();
                headerString.append((String) header.first);
                headerString.append(": ");
                headerString.append((String) header.second);
                headerString = headerString.toString();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("http_header_");
                stringBuilder.append(index);
                values.put(stringBuilder.toString(), headerString);
                index++;
            }
        }

        private void putIfNonNull(ContentValues contentValues, String key, Object value) {
            if (value != null) {
                contentValues.put(key, value.toString());
            }
        }
    }

    private static class CursorTranslator extends CursorWrapper {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private final boolean mAccessFilename;
        private final Uri mBaseUri;

        static {
            Class cls = DownloadManager.class;
        }

        public CursorTranslator(Cursor cursor, Uri baseUri, boolean accessFilename) {
            super(cursor);
            this.mBaseUri = baseUri;
            this.mAccessFilename = accessFilename;
        }

        public int getInt(int columnIndex) {
            return (int) getLong(columnIndex);
        }

        public long getLong(int columnIndex) {
            if (getColumnName(columnIndex).equals("reason")) {
                return getReason(super.getInt(getColumnIndex("status")));
            }
            if (getColumnName(columnIndex).equals("status")) {
                return (long) translateStatus(super.getInt(getColumnIndex("status")));
            }
            return super.getLong(columnIndex);
        }

        /* JADX WARNING: Removed duplicated region for block: B:16:0x003b  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002e  */
        /* JADX WARNING: Removed duplicated region for block: B:16:0x003b  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002e  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public String getString(int columnIndex) {
            Object obj;
            String columnName = getColumnName(columnIndex);
            int hashCode = columnName.hashCode();
            if (hashCode != -1204869480) {
                if (hashCode == 22072411 && columnName.equals(DownloadManager.COLUMN_LOCAL_FILENAME)) {
                    obj = 1;
                    switch (obj) {
                        case null:
                            return getLocalUri();
                        case 1:
                            if (!this.mAccessFilename) {
                                throw new SecurityException("COLUMN_LOCAL_FILENAME is deprecated; use ContentResolver.openFileDescriptor() instead");
                            }
                            break;
                    }
                    return super.getString(columnIndex);
                }
            } else if (columnName.equals(DownloadManager.COLUMN_LOCAL_URI)) {
                obj = null;
                switch (obj) {
                    case null:
                        break;
                    case 1:
                        break;
                }
                return super.getString(columnIndex);
            }
            obj = -1;
            switch (obj) {
                case null:
                    break;
                case 1:
                    break;
            }
            return super.getString(columnIndex);
        }

        private String getLocalUri() {
            long destinationType = getLong(getColumnIndex("destination"));
            if (destinationType == 4 || destinationType == 0 || destinationType == 6) {
                String localPath = super.getString(getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                if (localPath == null) {
                    return null;
                }
                return Uri.fromFile(new File(localPath)).toString();
            }
            return ContentUris.withAppendedId(Impl.ALL_DOWNLOADS_CONTENT_URI, getLong(getColumnIndex(DownloadManager.COLUMN_ID))).toString();
        }

        private long getReason(int status) {
            int translateStatus = translateStatus(status);
            if (translateStatus == 4) {
                return getPausedReason(status);
            }
            if (translateStatus != 16) {
                return 0;
            }
            return getErrorCode(status);
        }

        private long getPausedReason(int status) {
            switch (status) {
                case 194:
                    return 1;
                case 195:
                    return 2;
                case 196:
                    return 3;
                default:
                    return 4;
            }
        }

        private long getErrorCode(int status) {
            if ((400 <= status && status < 488) || (RunningAppProcessInfo.IMPORTANCE_EMPTY <= status && status < 600)) {
                return (long) status;
            }
            switch (status) {
                case 198:
                    return 1006;
                case 199:
                    return 1007;
                case 488:
                    return 1009;
                case 489:
                    return 1008;
                case 492:
                    return 1001;
                case 493:
                case 494:
                    return 1002;
                case 495:
                    return 1004;
                case 497:
                    return 1005;
                default:
                    return 1000;
            }
        }

        private int translateStatus(int status) {
            if (status == 190) {
                return 1;
            }
            if (status == 200) {
                return 8;
            }
            switch (status) {
                case 192:
                    return 2;
                case 193:
                case 194:
                case 195:
                case 196:
                    return 4;
                default:
                    return 16;
            }
        }
    }

    public DownloadManager(Context context) {
        this.mResolver = context.getContentResolver();
        this.mPackageName = context.getPackageName();
        this.mAccessFilename = context.getApplicationInfo().targetSdkVersion < 24;
    }

    public void setAccessAllDownloads(boolean accessAllDownloads) {
        if (accessAllDownloads) {
            this.mBaseUri = Impl.ALL_DOWNLOADS_CONTENT_URI;
        } else {
            this.mBaseUri = Impl.CONTENT_URI;
        }
    }

    public void setAccessFilename(boolean accessFilename) {
        this.mAccessFilename = accessFilename;
    }

    public long enqueue(Request request) {
        return Long.parseLong(this.mResolver.insert(Impl.CONTENT_URI, request.toContentValues(this.mPackageName)).getLastPathSegment());
    }

    public int markRowDeleted(long... ids) {
        if (ids != null && ids.length != 0) {
            return this.mResolver.delete(this.mBaseUri, getWhereClauseForIds(ids), getWhereArgsForIds(ids));
        }
        throw new IllegalArgumentException("input param 'ids' can't be null");
    }

    public int remove(long... ids) {
        return markRowDeleted(ids);
    }

    public Cursor query(Query query) {
        Cursor underlyingCursor = query.runQuery(this.mResolver, UNDERLYING_COLUMNS, this.mBaseUri);
        if (underlyingCursor == null) {
            return null;
        }
        return new CursorTranslator(underlyingCursor, this.mBaseUri, this.mAccessFilename);
    }

    public ParcelFileDescriptor openDownloadedFile(long id) throws FileNotFoundException {
        return this.mResolver.openFileDescriptor(getDownloadUri(id), FullBackup.ROOT_TREE_TOKEN);
    }

    public Uri getUriForDownloadedFile(long id) {
        Cursor cursor = null;
        try {
            cursor = query(new Query().setFilterById(id));
            if (cursor == null) {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            } else if (cursor.moveToFirst() && 8 == cursor.getInt(cursor.getColumnIndexOrThrow("status"))) {
                Uri withAppendedId = ContentUris.withAppendedId(Impl.ALL_DOWNLOADS_CONTENT_URI, id);
                return withAppendedId;
            } else {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getMimeTypeForDownloadedFile(long id) {
        Cursor cursor = null;
        try {
            cursor = query(new Query().setFilterById(id));
            if (cursor == null) {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            } else if (cursor.moveToFirst()) {
                String string = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEDIA_TYPE));
                return string;
            } else {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void restartDownload(long... ids) {
        Cursor cursor = query(new Query().setFilterById(ids));
        String uri = null;
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                int status = cursor.getInt(cursor.getColumnIndex("status"));
                if (status != 8) {
                    if (status != 16) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot restart incomplete download: ");
                        stringBuilder.append(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                uri = cursor.getString(cursor.getColumnIndex(COLUMN_URI));
                cursor.moveToNext();
            }
            ContentValues values = new ContentValues();
            values.put("current_bytes", Integer.valueOf(0));
            values.put("total_bytes", Integer.valueOf(-1));
            values.putNull("_data");
            values.put("status", Integer.valueOf(190));
            values.put("numfailed", Integer.valueOf(0));
            HwFrameworkFactory.getHwDrmManager().updateOmaMimeType(uri, values);
            this.mResolver.update(this.mBaseUri, values, getWhereClauseForIds(ids), getWhereArgsForIds(ids));
        } finally {
            cursor.close();
        }
    }

    public void forceDownload(long... ids) {
        ContentValues values = new ContentValues();
        values.put("status", Integer.valueOf(190));
        values.put("control", Integer.valueOf(0));
        values.put("bypass_recommended_size_limit", Integer.valueOf(1));
        this.mResolver.update(this.mBaseUri, values, getWhereClauseForIds(ids), getWhereArgsForIds(ids));
    }

    public static Long getMaxBytesOverMobile(Context context) {
        try {
            return Long.valueOf(Global.getLong(context.getContentResolver(), "download_manager_max_bytes_over_mobile"));
        } catch (SettingNotFoundException e) {
            return null;
        }
    }

    /*  JADX ERROR: NullPointerException in pass: ProcessVariables
        java.lang.NullPointerException
        	at jadx.core.dex.visitors.regions.ProcessVariables.addToUsageMap(ProcessVariables.java:271)
        	at jadx.core.dex.visitors.regions.ProcessVariables.access$000(ProcessVariables.java:31)
        	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processInsn(ProcessVariables.java:163)
        	at jadx.core.dex.visitors.regions.ProcessVariables$CollectUsageRegionVisitor.processBlockTraced(ProcessVariables.java:129)
        	at jadx.core.dex.visitors.regions.TracedRegionVisitor.processBlock(TracedRegionVisitor.java:23)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:53)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1080)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.lambda$traverseInternal$0(DepthRegionTraversal.java:57)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:57)
        	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:18)
        	at jadx.core.dex.visitors.regions.ProcessVariables.visit(ProcessVariables.java:183)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0063 A:{REMOVE} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0068 A:{SKIP} */
    public boolean rename(android.content.Context r21, long r22, java.lang.String r24) {
        /*
        r20 = this;
        r1 = r20;
        r2 = r22;
        r4 = r24;
        r0 = android.os.FileUtils.isValidFatFilename(r24);
        if (r0 == 0) goto L_0x014f;
    L_0x000c:
        r0 = new android.app.DownloadManager$Query;
        r0.<init>();
        r5 = 1;
        r6 = new long[r5];
        r7 = 0;
        r6[r7] = r2;
        r6 = r0.setFilterById(r6);
        r8 = 0;
        r9 = 0;
        r0 = 0;
        r10 = r0;
        r0 = r1.query(r6);	 Catch:{ all -> 0x0146 }
        r8 = r0;
        if (r8 != 0) goto L_0x002d;
        if (r8 == 0) goto L_0x002c;
    L_0x0029:
        r8.close();
    L_0x002c:
        return r7;
    L_0x002d:
        r0 = r8.moveToFirst();	 Catch:{ all -> 0x0146 }
        if (r0 == 0) goto L_0x0061;	 Catch:{ all -> 0x0146 }
    L_0x0033:
        r0 = "status";	 Catch:{ all -> 0x0146 }
        r0 = r8.getColumnIndexOrThrow(r0);	 Catch:{ all -> 0x0146 }
        r0 = r8.getInt(r0);	 Catch:{ all -> 0x0146 }
        r11 = 8;
        if (r11 == r0) goto L_0x0049;
        if (r8 == 0) goto L_0x0048;
    L_0x0045:
        r8.close();
    L_0x0048:
        return r7;
    L_0x0049:
        r11 = "title";	 Catch:{ all -> 0x0146 }
        r11 = r8.getColumnIndexOrThrow(r11);	 Catch:{ all -> 0x0146 }
        r11 = r8.getString(r11);	 Catch:{ all -> 0x0146 }
        r9 = r11;	 Catch:{ all -> 0x0146 }
        r11 = "media_type";	 Catch:{ all -> 0x0146 }
        r11 = r8.getColumnIndexOrThrow(r11);	 Catch:{ all -> 0x0146 }
        r11 = r8.getString(r11);	 Catch:{ all -> 0x0146 }
        r10 = r11;
    L_0x0061:
        if (r8 == 0) goto L_0x0066;
    L_0x0063:
        r8.close();
    L_0x0066:
        if (r9 == 0) goto L_0x0128;
    L_0x0068:
        if (r10 == 0) goto L_0x0128;
    L_0x006a:
        r0 = android.os.Environment.DIRECTORY_DOWNLOADS;
        r0 = android.os.Environment.getExternalStoragePublicDirectory(r0);
        r11 = new java.io.File;
        r11.<init>(r0, r9);
        r12 = new java.io.File;
        r12.<init>(r0, r4);
        r13 = r12.exists();
        if (r13 != 0) goto L_0x010d;
    L_0x0080:
        r13 = r11.renameTo(r12);
        if (r13 == 0) goto L_0x00f2;
    L_0x0086:
        r13 = "image/";
        r13 = r10.startsWith(r13);
        if (r13 == 0) goto L_0x00b7;
    L_0x008e:
        r13 = r21.getContentResolver();
        r14 = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        r15 = "_data=?";
        r7 = new java.lang.String[r5];
        r17 = r11.getAbsolutePath();
        r16 = 0;
        r7[r16] = r17;
        r13.delete(r14, r15, r7);
        r7 = new android.content.Intent;
        r13 = "android.intent.action.MEDIA_SCANNER_SCAN_FILE";
        r7.<init>(r13);
        r13 = android.net.Uri.fromFile(r12);
        r7.setData(r13);
        r13 = r21;
        r13.sendBroadcast(r7);
        goto L_0x00b9;
    L_0x00b7:
        r13 = r21;
    L_0x00b9:
        r7 = new android.content.ContentValues;
        r7.<init>();
        r14 = "title";
        r7.put(r14, r4);
        r14 = "_data";
        r15 = r12.toString();
        r7.put(r14, r15);
        r14 = "mediaprovider_uri";
        r7.putNull(r14);
        r14 = new long[r5];
        r16 = 0;
        r14[r16] = r2;
        r15 = r1.mResolver;
        r5 = r1.mBaseUri;
        r18 = r0;
        r0 = getWhereClauseForIds(r14);
        r1 = getWhereArgsForIds(r14);
        r0 = r15.update(r5, r7, r0, r1);
        r1 = 1;
        if (r0 != r1) goto L_0x00f1;
    L_0x00ee:
        r16 = r1;
    L_0x00f1:
        return r16;
    L_0x00f2:
        r13 = r21;
        r18 = r0;
        r0 = new java.lang.IllegalStateException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r5 = "Failed to rename to ";
        r1.append(r5);
        r1.append(r12);
        r1 = r1.toString();
        r0.<init>(r1);
        throw r0;
    L_0x010d:
        r13 = r21;
        r18 = r0;
        r0 = new java.lang.IllegalStateException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r5 = "Already exists ";
        r1.append(r5);
        r1.append(r12);
        r1 = r1.toString();
        r0.<init>(r1);
        throw r0;
    L_0x0128:
        r13 = r21;
        r0 = new java.lang.IllegalStateException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r5 = "Document with id ";
        r1.append(r5);
        r1.append(r2);
        r5 = " does not exist";
        r1.append(r5);
        r1 = r1.toString();
        r0.<init>(r1);
        throw r0;
    L_0x0146:
        r0 = move-exception;
        r13 = r21;
        if (r8 == 0) goto L_0x014e;
    L_0x014b:
        r8.close();
    L_0x014e:
        throw r0;
    L_0x014f:
        r13 = r21;
        r0 = new java.lang.SecurityException;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r1.append(r4);
        r5 = " is not a valid filename";
        r1.append(r5);
        r1 = r1.toString();
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.app.DownloadManager.rename(android.content.Context, long, java.lang.String):boolean");
    }

    public static Long getRecommendedMaxBytesOverMobile(Context context) {
        try {
            return Long.valueOf(Global.getLong(context.getContentResolver(), "download_manager_recommended_max_bytes_over_mobile"));
        } catch (SettingNotFoundException e) {
            return null;
        }
    }

    public static boolean isActiveNetworkExpensive(Context context) {
        return false;
    }

    public static long getActiveNetworkWarningBytes(Context context) {
        return -1;
    }

    public long addCompletedDownload(String title, String description, boolean isMediaScannerScannable, String mimeType, String path, long length, boolean showNotification) {
        return addCompletedDownload(title, description, isMediaScannerScannable, mimeType, path, length, showNotification, false, null, null);
    }

    public long addCompletedDownload(String title, String description, boolean isMediaScannerScannable, String mimeType, String path, long length, boolean showNotification, Uri uri, Uri referer) {
        return addCompletedDownload(title, description, isMediaScannerScannable, mimeType, path, length, showNotification, false, uri, referer);
    }

    public long addCompletedDownload(String title, String description, boolean isMediaScannerScannable, String mimeType, String path, long length, boolean showNotification, boolean allowWrite) {
        return addCompletedDownload(title, description, isMediaScannerScannable, mimeType, path, length, showNotification, allowWrite, null, null);
    }

    public long addCompletedDownload(String title, String description, boolean isMediaScannerScannable, String mimeType, String path, long length, boolean showNotification, boolean allowWrite, Uri uri, Uri referer) {
        String str = title;
        String str2 = description;
        String str3 = mimeType;
        String str4 = path;
        Uri uri2 = uri;
        validateArgumentIsNonEmpty("title", str);
        validateArgumentIsNonEmpty("description", str2);
        validateArgumentIsNonEmpty("path", str4);
        validateArgumentIsNonEmpty("mimeType", str3);
        if (length >= 0) {
            Request request;
            if (uri2 != null) {
                request = new Request(uri2);
            } else {
                request = new Request(NON_DOWNLOADMANAGER_DOWNLOAD);
            }
            request.setTitle(str).setDescription(str2).setMimeType(str3);
            if (referer != null) {
                request.addRequestHeader("Referer", referer.toString());
            }
            ContentValues values = request.toContentValues(null);
            values.put("destination", Integer.valueOf(6));
            values.put("_data", str4);
            values.put("status", Integer.valueOf(200));
            values.put("total_bytes", Long.valueOf(length));
            int i = 2;
            values.put("scanned", Integer.valueOf(isMediaScannerScannable ? 0 : 2));
            String str5 = "visibility";
            if (showNotification) {
                i = 3;
            }
            values.put(str5, Integer.valueOf(i));
            values.put(COLUMN_ALLOW_WRITE, Integer.valueOf(allowWrite));
            Uri downloadUri = this.mResolver.insert(Impl.CONTENT_URI, values);
            if (downloadUri == null) {
                return -1;
            }
            return Long.parseLong(downloadUri.getLastPathSegment());
        }
        throw new IllegalArgumentException(" invalid value for param: totalBytes");
    }

    private static void validateArgumentIsNonEmpty(String paramName, String val) {
        if (TextUtils.isEmpty(val)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(paramName);
            stringBuilder.append(" can't be null");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public Uri getDownloadUri(long id) {
        return ContentUris.withAppendedId(Impl.ALL_DOWNLOADS_CONTENT_URI, id);
    }

    static String getWhereClauseForIds(long[] ids) {
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("(");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                whereClause.append("OR ");
            }
            whereClause.append(COLUMN_ID);
            whereClause.append(" = ? ");
        }
        whereClause.append(")");
        return whereClause.toString();
    }

    static String[] getWhereArgsForIds(long[] ids) {
        return getWhereArgsForIds(ids, new String[ids.length]);
    }

    static String[] getWhereArgsForIds(long[] ids, String[] args) {
        for (int i = 0; i < ids.length; i++) {
            args[i] = Long.toString(ids[i]);
        }
        return args;
    }
}
