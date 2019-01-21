package android.mtp;

import android.app.DownloadManager;
import android.app.slice.Slice;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.HeifUtils;
import android.media.MediaFormat;
import android.media.MediaScanner;
import android.media.midi.MidiDeviceInfo;
import android.mtp.MtpStorageManager.MtpNotifier;
import android.mtp.MtpStorageManager.MtpObject;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.storage.ExternalStorageFileImpl;
import android.os.storage.StorageVolume;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Files;
import android.provider.Settings.Global;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import com.google.android.collect.Sets;
import dalvik.system.CloseGuard;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MtpDatabase implements AutoCloseable {
    private static final int[] AUDIO_PROPERTIES = new int[]{MtpConstants.PROPERTY_ARTIST, MtpConstants.PROPERTY_ALBUM_NAME, MtpConstants.PROPERTY_ALBUM_ARTIST, MtpConstants.PROPERTY_TRACK, MtpConstants.PROPERTY_ORIGINAL_RELEASE_DATE, MtpConstants.PROPERTY_DURATION, MtpConstants.PROPERTY_GENRE, MtpConstants.PROPERTY_COMPOSER, MtpConstants.PROPERTY_AUDIO_WAVE_CODEC, MtpConstants.PROPERTY_BITRATE_TYPE, MtpConstants.PROPERTY_AUDIO_BITRATE, MtpConstants.PROPERTY_NUMBER_OF_CHANNELS, MtpConstants.PROPERTY_SAMPLE_RATE};
    private static int DATABASE_COLUMN_INDEX_FIR = 1;
    private static int DATABASE_COLUMN_INDEX_FOR = 4;
    private static int DATABASE_COLUMN_INDEX_SEC = 2;
    private static int DATABASE_COLUMN_INDEX_THI = 3;
    private static final int[] DEVICE_PROPERTIES = new int[]{MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER, MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME, MtpConstants.DEVICE_PROPERTY_IMAGE_SIZE, MtpConstants.DEVICE_PROPERTY_BATTERY_LEVEL, MtpConstants.DEVICE_PROPERTY_PERCEIVED_DEVICE_TYPE};
    private static final int[] FILE_PROPERTIES = new int[]{MtpConstants.PROPERTY_STORAGE_ID, MtpConstants.PROPERTY_OBJECT_FORMAT, MtpConstants.PROPERTY_PROTECTION_STATUS, MtpConstants.PROPERTY_OBJECT_SIZE, MtpConstants.PROPERTY_OBJECT_FILE_NAME, MtpConstants.PROPERTY_DATE_MODIFIED, MtpConstants.PROPERTY_PERSISTENT_UID, MtpConstants.PROPERTY_PARENT_OBJECT, MtpConstants.PROPERTY_NAME, MtpConstants.PROPERTY_DISPLAY_NAME, MtpConstants.PROPERTY_DATE_ADDED};
    private static final int GALLERY_HEIF_SETTING_AUTOMATIC = 0;
    private static int HANDLE_OFFSET_FOR_HISUITE = 10000000;
    private static final String[] ID_PROJECTION = new String[]{DownloadManager.COLUMN_ID};
    private static final String ID_WHERE = "_id=?";
    private static final int[] IMAGE_PROPERTIES = new int[]{MtpConstants.PROPERTY_DESCRIPTION};
    private static final String MTP_TRANSFER_TEMP_FOLDER = "/data/data/com.android.providers.media/mtp_trans_cache";
    private static final String NO_MEDIA = ".nomedia";
    private static final String[] OBJECT_ALL_COLUMNS_PROJECTION = new String[]{DownloadManager.COLUMN_ID, "storage_id", "format", "parent", "_data", "_size", "date_added", "date_modified", "mime_type", "title", "_display_name", DownloadManager.COLUMN_MEDIA_TYPE, "is_drm", MediaFormat.KEY_WIDTH, MediaFormat.KEY_HEIGHT, "description", "artist", "album", "album_artist", "duration", "track", "composer", "year", MidiDeviceInfo.PROPERTY_NAME};
    private static final String[] OBJECT_INFO_PROJECTION = new String[]{DownloadManager.COLUMN_ID, "storage_id", "format", "parent", "_data", "_size", "date_added", "date_modified"};
    private static final String[] PATH_FORMAT_PROJECTION = new String[]{DownloadManager.COLUMN_ID, "_data", "_size", "format"};
    private static final String[] PATH_PROJECTION = new String[]{"_data"};
    private static final String[] PATH_PROJECTION_FOR_HISUITE = new String[]{DownloadManager.COLUMN_ID, "_data", "storage_id"};
    private static final String PATH_WHERE = "_data=?";
    private static final int[] PLAYBACK_FORMATS = new int[]{MtpConstants.FORMAT_UNDEFINED, MtpConstants.FORMAT_ASSOCIATION, MtpConstants.FORMAT_TEXT, MtpConstants.FORMAT_HTML, MtpConstants.FORMAT_WAV, MtpConstants.FORMAT_MP3, MtpConstants.FORMAT_MPEG, MtpConstants.FORMAT_EXIF_JPEG, MtpConstants.FORMAT_TIFF_EP, MtpConstants.FORMAT_BMP, MtpConstants.FORMAT_GIF, MtpConstants.FORMAT_JFIF, MtpConstants.FORMAT_PNG, MtpConstants.FORMAT_TIFF, MtpConstants.FORMAT_WMA, MtpConstants.FORMAT_OGG, MtpConstants.FORMAT_AAC, MtpConstants.FORMAT_MP4_CONTAINER, MtpConstants.FORMAT_MP2, MtpConstants.FORMAT_3GP_CONTAINER, MtpConstants.FORMAT_ABSTRACT_AV_PLAYLIST, MtpConstants.FORMAT_WPL_PLAYLIST, MtpConstants.FORMAT_M3U_PLAYLIST, MtpConstants.FORMAT_PLS_PLAYLIST, MtpConstants.FORMAT_XML_DOCUMENT, MtpConstants.FORMAT_FLAC, MtpConstants.FORMAT_DNG, MtpConstants.FORMAT_HEIF};
    private static final Uri QUERY_GALLERY_HEIF_SETTING_URI = Uri.parse("content://com.android.gallery3d.provider.GallerySettingsProvider.provider");
    private static final String SUFFIX_JPG_FILE = ".jpg";
    private static final String TAG = MtpDatabase.class.getSimpleName();
    private static final int[] VIDEO_PROPERTIES = new int[]{MtpConstants.PROPERTY_ARTIST, MtpConstants.PROPERTY_ALBUM_NAME, MtpConstants.PROPERTY_DURATION, MtpConstants.PROPERTY_DESCRIPTION};
    private static int sHandleOffsetForHisuite = HANDLE_OFFSET_FOR_HISUITE;
    private static boolean sIsHeifSettingAutomaticMode;
    private int mBatteryLevel;
    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                MtpDatabase.this.mBatteryScale = intent.getIntExtra("scale", 0);
                int newLevel = intent.getIntExtra(MediaFormat.KEY_LEVEL, 0);
                if (newLevel != MtpDatabase.this.mBatteryLevel) {
                    MtpDatabase.this.mBatteryLevel = newLevel;
                    try {
                        MtpDatabase.this.mServer.sendDevicePropertyChanged(MtpConstants.DEVICE_PROPERTY_BATTERY_LEVEL);
                    } catch (NullPointerException e) {
                        Log.e(MtpDatabase.TAG, "mServer already set to null");
                    }
                }
            }
        }
    };
    private int mBatteryScale;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final Context mContext;
    private SharedPreferences mDeviceProperties;
    private int mDeviceType;
    private MtpStorageManager mManager;
    private final ContentProviderClient mMediaProvider;
    private final MediaScanner mMediaScanner;
    private long mNativeContext;
    private final Uri mObjectsUri;
    private final HashMap<Integer, MtpPropertyGroup> mPropertyGroupsByFormat = new HashMap();
    private final HashMap<Integer, MtpPropertyGroup> mPropertyGroupsByProperty = new HashMap();
    private int mSendObjectFormat;
    private String mSendobjectPath;
    private MtpServer mServer;
    private final HashMap<String, MtpStorage> mStorageMap = new HashMap();
    private final String mVolumeName;

    private final native void native_finalize();

    private final native void native_setup();

    static {
        System.loadLibrary("media_jni");
    }

    private int[] getSupportedObjectProperties(int format) {
        switch (format) {
            case MtpConstants.FORMAT_WAV /*12296*/:
            case MtpConstants.FORMAT_MP3 /*12297*/:
            case MtpConstants.FORMAT_WMA /*47361*/:
            case MtpConstants.FORMAT_OGG /*47362*/:
            case MtpConstants.FORMAT_AAC /*47363*/:
                return IntStream.concat(Arrays.stream(FILE_PROPERTIES), Arrays.stream(AUDIO_PROPERTIES)).toArray();
            case MtpConstants.FORMAT_MPEG /*12299*/:
            case MtpConstants.FORMAT_WMV /*47489*/:
            case MtpConstants.FORMAT_3GP_CONTAINER /*47492*/:
                return IntStream.concat(Arrays.stream(FILE_PROPERTIES), Arrays.stream(VIDEO_PROPERTIES)).toArray();
            case MtpConstants.FORMAT_EXIF_JPEG /*14337*/:
            case MtpConstants.FORMAT_BMP /*14340*/:
            case MtpConstants.FORMAT_GIF /*14343*/:
            case MtpConstants.FORMAT_PNG /*14347*/:
            case MtpConstants.FORMAT_DNG /*14353*/:
            case MtpConstants.FORMAT_HEIF /*14354*/:
                return IntStream.concat(Arrays.stream(FILE_PROPERTIES), Arrays.stream(IMAGE_PROPERTIES)).toArray();
            default:
                return FILE_PROPERTIES;
        }
    }

    private int[] getSupportedDeviceProperties() {
        return DEVICE_PROPERTIES;
    }

    private int[] getSupportedPlaybackFormats() {
        return PLAYBACK_FORMATS;
    }

    private int[] getSupportedCaptureFormats() {
        return null;
    }

    public MtpDatabase(Context context, String volumeName, String[] subDirectories) {
        native_setup();
        this.mContext = context;
        this.mMediaProvider = context.getContentResolver().acquireContentProviderClient("media");
        this.mVolumeName = volumeName;
        this.mObjectsUri = Files.getMtpObjectsUri(volumeName).buildUpon().appendQueryParameter("nonotify", "1").build();
        this.mMediaScanner = new MediaScanner(context, this.mVolumeName);
        this.mManager = new MtpStorageManager(new MtpNotifier() {
            public void sendObjectAdded(int id) {
                if (MtpDatabase.this.mServer != null) {
                    MtpDatabase.this.mServer.sendObjectAdded(id);
                }
            }

            public void sendObjectRemoved(int id) {
                if (MtpDatabase.this.mServer != null) {
                    MtpDatabase.this.mServer.sendObjectRemoved(id);
                }
            }
        }, subDirectories == null ? null : Sets.newHashSet(subDirectories));
        initDeviceProperties(context);
        this.mDeviceType = SystemProperties.getInt("sys.usb.mtp.device_type", 0);
        fetchGalleryHeifSetting();
        this.mCloseGuard.open("close");
    }

    public void setServer(MtpServer server) {
        this.mServer = server;
        try {
            this.mContext.unregisterReceiver(this.mBatteryReceiver);
        } catch (IllegalArgumentException e) {
        }
        if (server != null) {
            this.mContext.registerReceiver(this.mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    public void close() {
        this.mManager.close();
        HwFrameworkFactory.getHwMtpDatabaseManager().hwClearSavedObject();
        this.mCloseGuard.close();
        if (this.mClosed.compareAndSet(false, true)) {
            this.mMediaScanner.close();
            if (this.mMediaProvider != null) {
                this.mMediaProvider.close();
            }
            native_finalize();
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    /* JADX WARNING: Missing block: B:22:0x00ba, code skipped:
            if (r0 != null) goto L_0x00bc;
     */
    /* JADX WARNING: Missing block: B:23:0x00bc, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:31:0x00d0, code skipped:
            if (r0 == null) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:32:0x00d3, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addStorage(StorageVolume storage) {
        Cursor c = null;
        try {
            ContentValues values;
            c = this.mMediaProvider.query(this.mObjectsUri, PATH_PROJECTION_FOR_HISUITE, PATH_WHERE, new String[]{storage.getPath()}, null, null);
            if (c == null || !c.moveToNext()) {
                values = new ContentValues();
                values.put("_data", storage.getPath());
                values.put("format", Integer.valueOf(MtpConstants.FORMAT_ASSOCIATION));
                values.put("date_modified", Long.valueOf(new File(storage.getPath()).lastModified()));
                try {
                    this.mMediaProvider.insert(this.mObjectsUri, values);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in addStorage", e);
                }
                if (c != null) {
                    c.close();
                }
                c = this.mMediaProvider.query(this.mObjectsUri, PATH_PROJECTION_FOR_HISUITE, PATH_WHERE, new String[]{storage.getPath()}, null, null);
                if (c != null) {
                    if (!c.moveToNext()) {
                    }
                }
                if (c != null) {
                    c.close();
                }
                return;
            }
            values = c.getInt(DATABASE_COLUMN_INDEX_SEC);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addStorageId : ");
            stringBuilder.append(values);
            Log.e(str, stringBuilder.toString());
            MtpStorage mtpStorage = this.mManager.addMtpStorage(storage, values);
            this.mStorageMap.put(storage.getPath(), mtpStorage);
            if (this.mServer != null) {
                this.mServer.addStorage(mtpStorage);
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in addStorage", e2);
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    public void removeStorage(StorageVolume storage) {
        MtpStorage mtpStorage = (MtpStorage) this.mStorageMap.get(storage.getPath());
        if (mtpStorage != null) {
            if (this.mServer != null) {
                this.mServer.removeStorage(mtpStorage);
            }
            this.mManager.removeMtpStorage(mtpStorage);
            this.mStorageMap.remove(storage.getPath());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:34:0x008e  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0093  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x007d  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0082  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void initDeviceProperties(Context context) {
        Exception e;
        Throwable th;
        Context context2 = context;
        String devicePropertiesName = "device-properties";
        this.mDeviceProperties = context2.getSharedPreferences("device-properties", 0);
        if (context2.getDatabasePath("device-properties").exists()) {
            SQLiteDatabase db = null;
            Cursor c = null;
            SQLiteDatabase db2;
            try {
                db2 = context2.openOrCreateDatabase("device-properties", 0, null);
                if (db2 != null) {
                    try {
                        SQLiteDatabase sQLiteDatabase = db2;
                        c = sQLiteDatabase.query("properties", new String[]{DownloadManager.COLUMN_ID, "code", Slice.SUBTYPE_VALUE}, null, null, null, null, null);
                        if (c != null) {
                            Editor e2 = this.mDeviceProperties.edit();
                            while (c.moveToNext()) {
                                e2.putString(c.getString(1), c.getString(2));
                            }
                            e2.commit();
                        }
                    } catch (Exception e3) {
                        e = e3;
                        db = db2;
                        try {
                            Log.e(TAG, "failed to migrate device properties", e);
                            if (c != null) {
                                c.close();
                            }
                            if (db != null) {
                                db.close();
                            }
                            db2 = db;
                            context2.deleteDatabase("device-properties");
                        } catch (Throwable th2) {
                            th = th2;
                            db2 = db;
                            if (c != null) {
                                c.close();
                            }
                            if (db2 != null) {
                                db2.close();
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        if (c != null) {
                        }
                        if (db2 != null) {
                        }
                        throw th;
                    }
                }
                if (c != null) {
                    c.close();
                }
                if (db2 != null) {
                    db2.close();
                }
            } catch (Exception e4) {
                e = e4;
                Log.e(TAG, "failed to migrate device properties", e);
                if (c != null) {
                }
                if (db != null) {
                }
                db2 = db;
                context2.deleteDatabase("device-properties");
            }
            context2.deleteDatabase("device-properties");
        }
    }

    private int beginSendObject(String path, int format, int parent, int storageId) {
        if (parent > sHandleOffsetForHisuite) {
            return beginSendObjectHisuite(path, format, parent, storageId) + sHandleOffsetForHisuite;
        }
        MtpObject parentObj = parent == 0 ? this.mManager.getStorageRoot(storageId) : this.mManager.getObject(parent);
        if (parentObj == null) {
            return -1;
        }
        return this.mManager.beginSendObject(parentObj, Paths.get(path, new String[0]).getFileName().toString(), format);
    }

    private void endSendObject(int handle, boolean succeeded) {
        if (!endSendObjectHisuite(handle, succeeded)) {
            MtpObject obj = this.mManager.getObject(handle);
            if (obj == null || !this.mManager.endSendObject(obj, succeeded)) {
                Log.e(TAG, "Failed to successfully end send object");
                return;
            }
            if (succeeded) {
                String path = obj.getPath().toString();
                int format = obj.getFormat();
                ContentValues values = new ContentValues();
                values.put("_data", path);
                values.put("format", Integer.valueOf(format));
                values.put("_size", Long.valueOf(obj.getSize()));
                values.put("date_modified", Long.valueOf(obj.getModifiedTime()));
                try {
                    int parentId;
                    if (obj.getParent().isRoot()) {
                        values.put("parent", Integer.valueOf(0));
                    } else {
                        parentId = findInMedia(obj.getParent().getPath());
                        if (parentId != -1) {
                            values.put("parent", Integer.valueOf(parentId));
                        } else {
                            return;
                        }
                    }
                    parentId = this.mMediaProvider.insert(this.mObjectsUri, values);
                    if (parentId != 0) {
                        rescanFile(path, Integer.parseInt((String) parentId.getPathSegments().get(2)), format);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in beginSendObject", e);
                }
            }
        }
    }

    private void rescanFile(String path, int handle, int format) {
        if (format == MtpConstants.FORMAT_ABSTRACT_AV_PLAYLIST) {
            String name = path;
            int lastSlash = name.lastIndexOf(47);
            if (lastSlash >= 0) {
                name = name.substring(lastSlash + 1);
            }
            if (name.endsWith(".pla")) {
                name = name.substring(0, name.length() - 4);
            }
            ContentValues values = new ContentValues(1);
            values.put("_data", path);
            values.put(MidiDeviceInfo.PROPERTY_NAME, name);
            values.put("format", Integer.valueOf(format));
            values.put("date_modified", Long.valueOf(System.currentTimeMillis() / 1000));
            values.put("media_scanner_new_object_id", Integer.valueOf(handle));
            try {
                this.mMediaProvider.insert(Playlists.EXTERNAL_CONTENT_URI, values);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in endSendObject", e);
                return;
            }
        }
        this.mMediaScanner.scanMtpFile(path, handle, format);
    }

    private int[] getObjectList(int storageID, int format, int parent) {
        Stream<MtpObject> objectStream = this.mManager.getObjects(parent, format, storageID);
        if (objectStream == null) {
            return null;
        }
        return objectStream.mapToInt(-$$Lambda$iwOv5HKUnGm7PVU3weoI9-JmsXc.INSTANCE).toArray();
    }

    private int getNumObjects(int storageID, int format, int parent) {
        Stream<MtpObject> objectStream = this.mManager.getObjects(parent, format, storageID);
        if (objectStream == null) {
            return -1;
        }
        return (int) objectStream.count();
    }

    /* JADX WARNING: Missing block: B:16:0x0060, code skipped:
            if (r0 != null) goto L_0x0062;
     */
    /* JADX WARNING: Missing block: B:17:0x0062, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:22:0x0070, code skipped:
            if (r0 == null) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:23:0x0073, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:36:0x00ad, code skipped:
            if (r0 != null) goto L_0x00af;
     */
    /* JADX WARNING: Missing block: B:37:0x00af, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:42:0x00bd, code skipped:
            if (r0 == null) goto L_0x00c0;
     */
    /* JADX WARNING: Missing block: B:43:0x00c0, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void cacheObjectAllInfos(int handle) {
        if (handle > 0) {
            Cursor c = null;
            if (HwFrameworkFactory.getHwMtpDatabaseManager().hwGetSavedObjectHandle() != handle) {
                boolean z = false;
                MtpObject newObject;
                if (handle > sHandleOffsetForHisuite) {
                    try {
                        c = this.mMediaProvider.query(this.mObjectsUri, OBJECT_ALL_COLUMNS_PROJECTION, ID_WHERE, new String[]{Integer.toString(handle - sHandleOffsetForHisuite)}, null, null);
                        if (c != null && c.moveToNext()) {
                            String string = c.getString(DATABASE_COLUMN_INDEX_FOR);
                            if (c.getInt(DATABASE_COLUMN_INDEX_SEC) == MtpConstants.FORMAT_ASSOCIATION) {
                                z = true;
                            }
                            newObject = new MtpObject(string, handle, null, z);
                            newObject.setStorageId(c.getInt(DATABASE_COLUMN_INDEX_FIR));
                            HwFrameworkFactory.getHwMtpDatabaseManager().hwSaveCurrentObject(newObject, c);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "RemoteException in cacheObjectAllInfos", e);
                    } catch (Throwable th) {
                        if (c != null) {
                            c.close();
                        }
                    }
                } else {
                    newObject = this.mManager.getObject(handle);
                    if (newObject != null) {
                        try {
                            c = this.mMediaProvider.query(this.mObjectsUri, OBJECT_ALL_COLUMNS_PROJECTION, PATH_WHERE, new String[]{newObject.getPath().toString()}, null, null);
                            if (c != null && c.moveToNext()) {
                                HwFrameworkFactory.getHwMtpDatabaseManager().hwSaveCurrentObject(newObject, c);
                            }
                        } catch (RemoteException e2) {
                            Log.e(TAG, "RemoteException in cacheObjectAllInfos", e2);
                        } catch (Throwable th2) {
                            if (c != null) {
                                c.close();
                            }
                        }
                    }
                }
            }
        }
    }

    private MtpPropertyList getObjectPropertyList(int handle, int format, int property, int groupCode, int depth) {
        int i = handle;
        int i2 = format;
        int i3 = property;
        int depth2 = depth;
        if (depth2 == 0) {
            cacheObjectAllInfos(handle);
            MtpPropertyList result = HwFrameworkFactory.getHwMtpDatabaseManager().getObjectPropertyList(this, i, i2, i3, groupCode);
            if (result != null) {
                return result;
            }
        }
        if (i3 != 0) {
            int handle2;
            int i4 = -1;
            if (depth2 == -1 && (i == 0 || i == -1)) {
                handle2 = -1;
                depth2 = 0;
            } else {
                handle2 = i;
            }
            int i5 = 1;
            if (depth2 != 0 && depth2 != 1) {
                return new MtpPropertyList(MtpConstants.RESPONSE_SPECIFICATION_BY_DEPTH_UNSUPPORTED);
            }
            Stream<MtpObject> objectStream = Stream.of(new MtpObject[0]);
            if (handle2 == -1) {
                objectStream = this.mManager.getObjects(0, i2, -1);
                if (objectStream == null) {
                    return new MtpPropertyList(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                }
            } else if (handle2 != 0) {
                MtpObject obj = this.mManager.getObject(handle2);
                if (obj == null) {
                    return new MtpPropertyList(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                }
                if (obj.getFormat() == i2 || i2 == 0) {
                    objectStream = Stream.of(obj);
                }
            }
            if (handle2 == 0 || depth2 == 1) {
                if (handle2 == 0) {
                    handle2 = -1;
                }
                Stream<MtpObject> childStream = this.mManager.getObjects(handle2, i2, -1);
                if (childStream == null) {
                    return new MtpPropertyList(MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE);
                }
                objectStream = Stream.concat(objectStream, childStream);
            }
            MtpPropertyList ret = new MtpPropertyList(MtpConstants.RESPONSE_OK);
            for (MtpObject obj2 : objectStream) {
                MtpPropertyGroup propertyGroup;
                if (i3 == i4) {
                    propertyGroup = (MtpPropertyGroup) this.mPropertyGroupsByFormat.get(Integer.valueOf(obj2.getFormat()));
                    if (propertyGroup == null) {
                        propertyGroup = new MtpPropertyGroup(this.mMediaProvider, this.mVolumeName, getSupportedObjectProperties(i2));
                        this.mPropertyGroupsByFormat.put(Integer.valueOf(format), propertyGroup);
                    }
                } else {
                    int[] propertyList = new int[i5];
                    propertyList[0] = i3;
                    propertyGroup = (MtpPropertyGroup) this.mPropertyGroupsByProperty.get(Integer.valueOf(property));
                    if (propertyGroup == null) {
                        propertyGroup = new MtpPropertyGroup(this.mMediaProvider, this.mVolumeName, propertyList);
                        this.mPropertyGroupsByProperty.put(Integer.valueOf(property), propertyGroup);
                    }
                }
                i4 = propertyGroup.getPropertyList(obj2, ret);
                if (i4 != MtpConstants.RESPONSE_OK) {
                    return new MtpPropertyList(i4);
                }
                int i6 = 8193;
                i4 = -1;
                i5 = 1;
            }
            return ret;
        } else if (groupCode == 0) {
            return new MtpPropertyList(MtpConstants.RESPONSE_PARAMETER_NOT_SUPPORTED);
        } else {
            return new MtpPropertyList(MtpConstants.RESPONSE_SPECIFICATION_BY_GROUP_UNSUPPORTED);
        }
    }

    private int renameFile(int handle, String newName) {
        String str;
        StringBuilder stringBuilder;
        MtpObject obj = this.mManager.getObject(handle);
        if (obj == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        Path oldPath = obj.getPath();
        if (!this.mManager.beginRenameObject(obj, newName)) {
            return 8194;
        }
        Path newPath = obj.getPath();
        boolean success = new ExternalStorageFileImpl(oldPath.toString()).renameTo(new ExternalStorageFileImpl(newPath.toString()));
        try {
            Os.access(oldPath.toString(), OsConstants.F_OK);
            Os.access(newPath.toString(), OsConstants.F_OK);
        } catch (ErrnoException e) {
        }
        if (!this.mManager.endRenameObject(obj, oldPath.getFileName().toString(), success)) {
            Log.e(TAG, "Failed to end rename object");
        }
        if (!success) {
            return 8194;
        }
        ContentValues values = new ContentValues();
        values.put("_data", newPath.toString());
        int oldHandle = -1;
        try {
            oldHandle = this.mMediaProvider.update(this.mObjectsUri, values, PATH_WHERE, new String[]{oldPath.toString()});
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in mMediaProvider.update", e2);
        }
        if (oldHandle <= 0) {
            if (obj.isDir()) {
                values = new ContentValues();
                values.put("_size", Long.valueOf(obj.getSize()));
                values.put("date_modified", Long.valueOf(obj.getModifiedTime()));
                values.put("format", Integer.valueOf(MtpConstants.FORMAT_ASSOCIATION));
                values.put("_data", newPath.toString());
                try {
                    this.mMediaProvider.insert(this.mObjectsUri, values);
                } catch (RemoteException e22) {
                    Log.e(TAG, "RemoteException in scanMtpFile", e22);
                }
            } else {
                this.mMediaScanner.scanSingleFile(newPath.toString(), null);
            }
            return MtpConstants.RESPONSE_OK;
        }
        if (obj.isDir()) {
            if (oldPath.getFileName().startsWith(".") && !newPath.startsWith(".")) {
                try {
                    this.mMediaProvider.call("unhide", newPath.toString(), null);
                } catch (RemoteException e3) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("failed to unhide/rescan for ");
                    stringBuilder.append(newPath);
                    Log.e(str, stringBuilder.toString());
                }
            }
        } else if (oldPath.getFileName().toString().toLowerCase(Locale.US).equals(NO_MEDIA) && !newPath.getFileName().toString().toLowerCase(Locale.US).equals(NO_MEDIA)) {
            try {
                this.mMediaProvider.call("unhide", oldPath.getParent().toString(), null);
            } catch (RemoteException e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to unhide/rescan for ");
                stringBuilder.append(newPath);
                Log.e(str, stringBuilder.toString());
            }
        }
        return MtpConstants.RESPONSE_OK;
    }

    private int beginMoveObject(int handle, int newParent, int newStorage) {
        MtpObject obj = this.mManager.getObject(handle);
        MtpObject parent = newParent == 0 ? this.mManager.getStorageRoot(newStorage) : this.mManager.getObject(newParent);
        if (obj == null || parent == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        return this.mManager.beginMoveObject(obj, parent) ? MtpConstants.RESPONSE_OK : 8194;
    }

    private void endMoveObject(int oldParent, int newParent, int oldStorage, int newStorage, int objId, boolean success) {
        MtpObject storageRoot;
        Cursor cursor;
        int i = oldParent;
        int i2 = newParent;
        int i3 = objId;
        boolean z = success;
        if (i == 0) {
            storageRoot = this.mManager.getStorageRoot(oldStorage);
        } else {
            int i4 = oldStorage;
            storageRoot = this.mManager.getObject(i);
        }
        MtpObject oldParentObj = storageRoot;
        if (i2 == 0) {
            storageRoot = this.mManager.getStorageRoot(newStorage);
        } else {
            int i5 = newStorage;
            storageRoot = this.mManager.getObject(i2);
        }
        MtpObject newParentObj = storageRoot;
        String name = this.mManager.getObject(i3).getName();
        if (newParentObj == null || oldParentObj == null || !this.mManager.endMoveObject(oldParentObj, newParentObj, name, z)) {
            Log.e(TAG, "Failed to end move object");
            return;
        }
        MtpObject obj = this.mManager.getObject(i3);
        if (z && obj != null) {
            int parentId;
            ContentValues values = new ContentValues();
            Path path = newParentObj.getPath().resolve(name);
            Path oldPath = oldParentObj.getPath().resolve(name);
            values.put("_data", path.toString());
            if (obj.getParent().isRoot()) {
                values.put("parent", Integer.valueOf(0));
            } else {
                parentId = findInMedia(path.getParent());
                if (parentId != -1) {
                    values.put("parent", Integer.valueOf(parentId));
                } else {
                    deleteFromMedia(oldPath, obj.isDir());
                    return;
                }
            }
            String[] whereArgs = new String[]{oldPath.toString()};
            parentId = -1;
            try {
                int i6;
                if (oldParentObj.isRoot()) {
                    i6 = -1;
                } else {
                    i6 = -1;
                    try {
                        parentId = findInMedia(oldPath.getParent());
                    } catch (RemoteException e) {
                        storageRoot = e;
                        cursor = null;
                    }
                }
                int i7;
                if (oldParentObj.isRoot()) {
                    i7 = parentId;
                    cursor = null;
                } else {
                    cursor = null;
                    if (parentId != -1) {
                        i7 = parentId;
                    } else {
                        try {
                            values.put("format", Integer.valueOf(obj.getFormat()));
                            values.put("_size", Long.valueOf(obj.getSize()));
                            values.put("date_modified", Long.valueOf(obj.getModifiedTime()));
                            parentId = this.mMediaProvider.insert(this.mObjectsUri, values);
                            if (parentId != 0) {
                                Uri uri = parentId;
                                rescanFile(path.toString(), Integer.parseInt((String) parentId.getPathSegments().get(2)), obj.getFormat());
                            }
                        } catch (RemoteException e2) {
                            storageRoot = e2;
                            Log.e(TAG, "RemoteException in mMediaProvider.update", storageRoot);
                        }
                    }
                }
                this.mMediaProvider.update(this.mObjectsUri, values, PATH_WHERE, whereArgs);
            } catch (RemoteException e3) {
                storageRoot = e3;
                cursor = null;
                Log.e(TAG, "RemoteException in mMediaProvider.update", storageRoot);
            }
        }
    }

    private int beginCopyObject(int handle, int newParent, int newStorage) {
        MtpObject obj = this.mManager.getObject(handle);
        MtpObject parent = newParent == 0 ? this.mManager.getStorageRoot(newStorage) : this.mManager.getObject(newParent);
        if (obj == null || parent == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        return this.mManager.beginCopyObject(obj, parent);
    }

    private void endCopyObject(int handle, boolean success) {
        MtpObject obj = this.mManager.getObject(handle);
        if (obj == null || !this.mManager.endCopyObject(obj, success)) {
            Log.e(TAG, "Failed to end copy object");
        } else if (success) {
            String path = obj.getPath().toString();
            int format = obj.getFormat();
            ContentValues values = new ContentValues();
            values.put("_data", path);
            values.put("format", Integer.valueOf(format));
            values.put("_size", Long.valueOf(obj.getSize()));
            values.put("date_modified", Long.valueOf(obj.getModifiedTime()));
            try {
                int parentId;
                if (obj.getParent().isRoot()) {
                    values.put("parent", Integer.valueOf(0));
                } else {
                    parentId = findInMedia(obj.getParent().getPath());
                    if (parentId != -1) {
                        values.put("parent", Integer.valueOf(parentId));
                    } else {
                        return;
                    }
                }
                if (obj.isDir()) {
                    this.mMediaScanner.scanDirectories(new String[]{path});
                } else {
                    parentId = this.mMediaProvider.insert(this.mObjectsUri, values);
                    if (parentId != 0) {
                        rescanFile(path, Integer.parseInt((String) parentId.getPathSegments().get(2)), format);
                    }
                }
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in beginSendObject", e);
            }
        }
    }

    private int setObjectProperty(int handle, int property, long intValue, String stringValue) {
        if (HwFrameworkFactory.getHwMtpDatabaseManager().hwGetSavedObjectHandle() == handle) {
            HwFrameworkFactory.getHwMtpDatabaseManager().hwClearSavedObject();
        }
        if (property != MtpConstants.PROPERTY_OBJECT_FILE_NAME) {
            return MtpConstants.RESPONSE_OBJECT_PROP_NOT_SUPPORTED;
        }
        return renameFile(handle, stringValue);
    }

    private int getDeviceProperty(int property, long[] outIntValue, char[] outStringValue) {
        boolean isDeviceNameUpdate = true;
        int width;
        if (property == MtpConstants.DEVICE_PROPERTY_BATTERY_LEVEL) {
            outIntValue[0] = (long) this.mBatteryLevel;
            outIntValue[1] = (long) this.mBatteryScale;
            return MtpConstants.RESPONSE_OK;
        } else if (property == MtpConstants.DEVICE_PROPERTY_IMAGE_SIZE) {
            Display display = ((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            width = display.getMaximumSizeDimension();
            int height = display.getMaximumSizeDimension();
            String imageSize = new StringBuilder();
            imageSize.append(Integer.toString(width));
            imageSize.append("x");
            imageSize.append(Integer.toString(height));
            imageSize = imageSize.toString();
            imageSize.getChars(0, imageSize.length(), outStringValue, 0);
            outStringValue[imageSize.length()] = 0;
            return MtpConstants.RESPONSE_OK;
        } else if (property != MtpConstants.DEVICE_PROPERTY_PERCEIVED_DEVICE_TYPE) {
            switch (property) {
                case MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER /*54273*/:
                case MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME /*54274*/:
                    String value = this.mDeviceProperties.getString(Integer.toString(property), "");
                    if (property == MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME) {
                        if (Global.getInt(this.mContext.getContentResolver(), "unified_device_name_updated", 0) != 1) {
                            isDeviceNameUpdate = false;
                        }
                        if (isDeviceNameUpdate) {
                            value = Global.getString(this.mContext.getContentResolver(), "unified_device_name");
                            Global.putInt(this.mContext.getContentResolver(), "unified_device_name_updated", 0);
                            Editor e = this.mDeviceProperties.edit();
                            e.putString(Integer.toString(property), value);
                            e.commit();
                        }
                        if (value == null || value.equals("")) {
                            value = SystemProperties.get("ro.config.marketing_name");
                        }
                        if (value == null || value.equals("")) {
                            value = SystemProperties.get("ro.product.model");
                        }
                    }
                    width = value.length();
                    if (width > 255) {
                        width = 255;
                    }
                    value.getChars(0, width, outStringValue, 0);
                    outStringValue[width] = 0;
                    return MtpConstants.RESPONSE_OK;
                default:
                    return MtpConstants.RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
            }
        } else {
            outIntValue[0] = (long) this.mDeviceType;
            return MtpConstants.RESPONSE_OK;
        }
    }

    private int setDeviceProperty(int property, long intValue, String stringValue) {
        switch (property) {
            case MtpConstants.DEVICE_PROPERTY_SYNCHRONIZATION_PARTNER /*54273*/:
            case MtpConstants.DEVICE_PROPERTY_DEVICE_FRIENDLY_NAME /*54274*/:
                int i;
                Editor e = this.mDeviceProperties.edit();
                e.putString(Integer.toString(property), stringValue);
                if (e.commit()) {
                    i = MtpConstants.RESPONSE_OK;
                } else {
                    i = 8194;
                }
                return i;
            default:
                return MtpConstants.RESPONSE_DEVICE_PROP_NOT_SUPPORTED;
        }
    }

    private boolean getObjectInfo(int handle, int[] outStorageFormatParent, char[] outName, long[] outCreatedModified) {
        MtpObject obj = this.mManager.getObject(handle);
        if (obj == null) {
            return false;
        }
        outStorageFormatParent[0] = obj.getStorageId();
        outStorageFormatParent[1] = obj.getFormat();
        outStorageFormatParent[2] = obj.getParent().isRoot() ? 0 : obj.getParent().getId();
        int nameLen = Integer.min(obj.getName().length(), 255);
        obj.getName().getChars(0, nameLen, outName, 0);
        outName[nameLen] = 0;
        outCreatedModified[0] = obj.getModifiedTime();
        outCreatedModified[1] = obj.getModifiedTime();
        return true;
    }

    private boolean isExternalStoragePath(String path) {
        return path.startsWith("/storage/") && !path.startsWith("/storage/emulated/");
    }

    private int getObjectFilePath(int handle, char[] outFilePath, long[] outFileLengthFormat) {
        String path;
        String pathEx;
        if (handle > sHandleOffsetForHisuite) {
            Cursor c = null;
            try {
                c = this.mMediaProvider.query(this.mObjectsUri, PATH_FORMAT_PROJECTION, ID_WHERE, new String[]{Integer.toString(handle - sHandleOffsetForHisuite)}, null, null);
                if (c == null || !c.moveToNext()) {
                    if (c != null) {
                        c.close();
                    }
                    return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
                }
                path = c.getString(DATABASE_COLUMN_INDEX_FIR);
                if (isExternalStoragePath(path)) {
                    pathEx = path.replaceFirst("/storage/", "/mnt/media_rw/");
                } else {
                    pathEx = path;
                }
                pathEx.getChars(0, pathEx.length(), outFilePath, 0);
                outFilePath[pathEx.length()] = 0;
                outFileLengthFormat[0] = new File(pathEx).length();
                outFileLengthFormat[1] = c.getLong(DATABASE_COLUMN_INDEX_THI);
                if (c != null) {
                    c.close();
                }
                return MtpConstants.RESPONSE_OK;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in getObjectFilePath", e);
                if (c != null) {
                    c.close();
                }
                return 8194;
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
                throw th;
            }
        }
        MtpObject obj = this.mManager.getObject(handle);
        if (obj == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        path = obj.getPath().toString();
        if (isExternalStoragePath(path)) {
            pathEx = path.replaceFirst("/storage/", "/mnt/media_rw/");
        } else {
            pathEx = path;
        }
        int pathLen = Integer.min(pathEx.length(), 4096);
        pathEx.getChars(0, pathLen, outFilePath, 0);
        outFilePath[pathLen] = 0;
        outFileLengthFormat[0] = obj.getSize();
        outFileLengthFormat[1] = (long) obj.getFormat();
        return MtpConstants.RESPONSE_OK;
    }

    private int getObjectFormat(int handle) {
        MtpObject obj = this.mManager.getObject(handle);
        if (obj == null) {
            return -1;
        }
        return obj.getFormat();
    }

    private int beginDeleteObject(int handle) {
        MtpObject obj = this.mManager.getObject(handle);
        if (obj == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        if (this.mManager.beginRemoveObject(obj)) {
            return MtpConstants.RESPONSE_OK;
        }
        return 8194;
    }

    private void endDeleteObject(int handle, boolean success) {
        MtpObject obj = this.mManager.getObject(handle);
        if (obj != null) {
            if (!this.mManager.endRemoveObject(obj, success)) {
                Log.e(TAG, "Failed to end remove object");
            }
            if (success) {
                deleteFromMedia(obj.getPath(), obj.isDir());
            }
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0028, code skipped:
            if (r1 != null) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:9:0x002a, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:14:0x004c, code skipped:
            if (r1 == null) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:15:0x004f, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int findInMedia(Path path) {
        int ret = -1;
        Cursor c = null;
        try {
            c = this.mMediaProvider.query(this.mObjectsUri, ID_PROJECTION, PATH_WHERE, new String[]{path.toString()}, null, null);
            if (c != null && c.moveToNext()) {
                ret = c.getInt(0);
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error finding ");
            stringBuilder.append(path);
            stringBuilder.append(" in MediaProvider");
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    private void deleteFromMedia(Path path, boolean isDir) {
        String str;
        StringBuilder stringBuilder;
        if (isDir) {
            try {
                String[] strArr = new String[3];
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(path);
                stringBuilder2.append("/%");
                strArr[0] = stringBuilder2.toString();
                strArr[1] = Integer.toString(path.toString().length() + 1);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(path.toString());
                stringBuilder3.append("/");
                strArr[2] = stringBuilder3.toString();
                this.mMediaProvider.delete(this.mObjectsUri, "_data LIKE ?1 AND lower(substr(_data,1,?2))=lower(?3)", strArr);
            } catch (Exception e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to delete ");
                stringBuilder.append(path);
                stringBuilder.append(" from MediaProvider");
                Log.d(str, stringBuilder.toString());
                return;
            }
        }
        if (this.mMediaProvider.delete(this.mObjectsUri, PATH_WHERE, new String[]{path.toString()}) <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Mediaprovider didn't delete ");
            stringBuilder.append(path);
            Log.i(str, stringBuilder.toString());
        } else if (!isDir && path.toString().toLowerCase(Locale.US).endsWith(NO_MEDIA)) {
            try {
                this.mMediaProvider.call("unhide", path.getParent().toString(), null);
            } catch (RemoteException e2) {
                String str2 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("failed to unhide/rescan for ");
                stringBuilder4.append(path);
                Log.e(str2, stringBuilder4.toString());
            }
        }
    }

    private int[] getObjectReferences(int handle) {
        if (HwFrameworkFactory.getHwMtpDatabaseManager().hwGetObjectReferences(handle)) {
            return null;
        }
        MtpObject obj = this.mManager.getObject(handle);
        if (obj == null) {
            return null;
        }
        handle = findInMedia(obj.getPath());
        if (handle == -1) {
            return null;
        }
        Cursor c = null;
        try {
            c = this.mMediaProvider.query(Files.getMtpReferencesUri(this.mVolumeName, (long) handle), PATH_PROJECTION, null, null, null, null);
            if (c == null) {
                if (c != null) {
                    c.close();
                }
                return null;
            }
            ArrayList<Integer> result = new ArrayList();
            while (c.moveToNext()) {
                MtpObject refObj = this.mManager.getByPath(c.getString(null));
                if (refObj != null) {
                    result.add(Integer.valueOf(refObj.getId()));
                }
            }
            int[] toArray = result.stream().mapToInt(-$$Lambda$MtpDatabase$UV1wDVoVlbcxpr8zevj_aMFtUGw.INSTANCE).toArray();
            if (c != null) {
                c.close();
            }
            return toArray;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getObjectList", e);
            if (c != null) {
                c.close();
            }
            return null;
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private int setObjectReferences(int handle, int[] references) {
        int[] iArr = references;
        MtpObject obj = this.mManager.getObject(handle);
        if (obj == null) {
            return MtpConstants.RESPONSE_INVALID_OBJECT_HANDLE;
        }
        int handle2 = findInMedia(obj.getPath());
        int i = -1;
        if (handle2 == -1) {
            return 8194;
        }
        Uri uri = Files.getMtpReferencesUri(this.mVolumeName, (long) handle2);
        ArrayList<ContentValues> valuesList = new ArrayList();
        int length = iArr.length;
        int i2 = 0;
        while (i2 < length) {
            MtpObject refObj = this.mManager.getObject(iArr[i2]);
            if (refObj != null) {
                int refHandle = findInMedia(refObj.getPath());
                if (refHandle != i) {
                    ContentValues values = new ContentValues();
                    values.put(DownloadManager.COLUMN_ID, Integer.valueOf(refHandle));
                    valuesList.add(values);
                }
            }
            i2++;
            i = -1;
        }
        try {
            if (this.mMediaProvider.bulkInsert(uri, (ContentValues[]) valuesList.toArray(new ContentValues[0])) > 0) {
                return MtpConstants.RESPONSE_OK;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setObjectReferences", e);
        }
        return 8194;
    }

    private void convertHeifToJgp(String heifPath) {
        String jpgPath = new StringBuilder();
        jpgPath.append(MTP_TRANSFER_TEMP_FOLDER);
        jpgPath.append(heifPath.substring(heifPath.lastIndexOf(47), heifPath.lastIndexOf(46)));
        jpgPath.append(SUFFIX_JPG_FILE);
        try {
            HeifUtils.convertHeifToJpg(heifPath, jpgPath.toString());
        } catch (IOException e) {
            Log.e(TAG, "failed to convert heif to jpg");
        }
    }

    private boolean fetchGalleryHeifSetting() {
        sIsHeifSettingAutomaticMode = false;
        return sIsHeifSettingAutomaticMode;
    }

    public static boolean issIsHeifSettingAutomaticMode() {
        return sIsHeifSettingAutomaticMode;
    }

    public static void setHandleOffsetForHisuite(int offset) {
        sHandleOffsetForHisuite = offset;
    }

    public static int getHandleOffsetForHisuite() {
        return sHandleOffsetForHisuite;
    }

    private boolean inStorageRoot(String path) {
        try {
            String canonical = new File(path).getCanonicalPath();
            for (String root : this.mStorageMap.keySet()) {
                if (canonical.startsWith(root)) {
                    return true;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "inStorageRoot ioException!");
        }
        return false;
    }

    private boolean endSendObjectHisuite(int handle, boolean succeeded) {
        if (handle < sHandleOffsetForHisuite) {
            return false;
        }
        this.mMediaScanner.scanMtpFile(this.mSendobjectPath, handle - sHandleOffsetForHisuite, this.mSendObjectFormat);
        return true;
    }

    /* JADX WARNING: Missing block: B:17:0x0064, code skipped:
            if (r1 != null) goto L_0x0073;
     */
    /* JADX WARNING: Missing block: B:22:0x0071, code skipped:
            if (r1 == null) goto L_0x007d;
     */
    /* JADX WARNING: Missing block: B:23:0x0073, code skipped:
            r1.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int beginSendObjectHisuite(String path, int format, int parent, int storageId) {
        if (path == null || inStorageRoot(path)) {
            if (path != null) {
                Cursor c = null;
                try {
                    c = this.mMediaProvider.query(this.mObjectsUri, ID_PROJECTION, PATH_WHERE, new String[]{path}, null, null);
                    if (c != null && c.getCount() > 0) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("file already exists in beginSendObject: ");
                        stringBuilder.append(path);
                        Log.w(str, stringBuilder.toString());
                        int hwBeginSendObject = HwFrameworkFactory.getHwMtpDatabaseManager().hwBeginSendObject(path, c);
                        if (c != null) {
                            c.close();
                        }
                        return hwBeginSendObject;
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in beginSendObject", e);
                } catch (Throwable th) {
                    if (c != null) {
                        c.close();
                    }
                    throw th;
                }
            }
            this.mSendObjectFormat = format;
            this.mSendobjectPath = path;
            ContentValues values = new ContentValues();
            values.put("_data", path);
            values.put("format", Integer.valueOf(format));
            values.put("parent", Integer.valueOf(parent - sHandleOffsetForHisuite));
            values.put("storage_id", Integer.valueOf(storageId));
            try {
                Uri uri = this.mMediaProvider.insert(this.mObjectsUri, values);
                if (uri != null) {
                    return Integer.parseInt((String) uri.getPathSegments().get(DATABASE_COLUMN_INDEX_SEC));
                }
                return -1;
            } catch (RemoteException e2) {
                Log.e(TAG, "RemoteException in beginSendObject", e2);
                return -1;
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("attempt to put file outside of storage area: ");
        stringBuilder2.append(path);
        Log.e(str2, stringBuilder2.toString());
        return -1;
    }
}
