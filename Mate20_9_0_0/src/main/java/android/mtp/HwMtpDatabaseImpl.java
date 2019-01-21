package android.mtp;

import android.database.Cursor;
import android.mtp.MtpStorageManager.MtpObject;
import android.os.storage.ExternalStorageFileImpl;
import android.util.Log;
import java.io.File;

public class HwMtpDatabaseImpl implements HwMtpDatabaseManager {
    private static final int FILE_SIZE_MAGNIFICATION = 3;
    private static final String[] OBJECT_ALL_COLUMNS_PROJECTION = new String[]{"_id", "storage_id", "format", "parent", "_data", "_size", "date_added", "date_modified", "mime_type", "title", "_display_name", "media_type", "is_drm", "width", "height", "description", "artist", "album", "album_artist", "duration", "track", "composer", "year", "name"};
    private static final int ROOT_DIR_PARENT_ID = 0;
    private static final String SUFFIX_JPG_FILE = ".jpg";
    private static final String TAG = "HwMtpDatabaseImpl";
    private static HwMtpDatabaseManager mHwMtpDatabaseManager = new HwMtpDatabaseImpl();
    int[] mGroup4Props = new int[]{56322, 56324};
    int[] mGroup8Props = new int[]{56323, 56327, 56388, 56329, 56398, 56473, 56385, 56459, 56390, 56474, 56460, 56985, 56986, 56979, 56978, 56980, 56321, 56331, 56457, 56544, 56475, 56470, 56392};
    private Object mLockObject = new Object();
    private MtpObjectColumnInfo mObjectColumnInfo = new MtpObjectColumnInfo();

    private static class MtpObjectColumnInfo {
        public String album;
        public String albumArtist;
        public String artist;
        public String composer;
        public String data;
        public long dateAdded;
        public long dateModified;
        public String description;
        public String displayName;
        public int duration;
        public int format;
        public int handle = -1;
        public int height;
        public int isDrm;
        public int mediaType;
        public int mimeType;
        public String name;
        public int parent;
        public long size;
        public int storageId;
        public String title;
        public int track;
        public int[] types = new int[24];
        public int width;
        public int year;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MtpObjectColumnInfo[mimeType : ");
            stringBuilder.append(this.mimeType);
            stringBuilder.append(", isDrm : ");
            stringBuilder.append(this.isDrm);
            stringBuilder.append(", width : ");
            stringBuilder.append(this.width);
            stringBuilder.append(", height : ");
            stringBuilder.append(this.height);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    private HwMtpDatabaseImpl() {
    }

    public static HwMtpDatabaseManager getDefault() {
        return mHwMtpDatabaseManager;
    }

    public int hwBeginSendObject(String path, Cursor c) {
        ExternalStorageFileImpl tempFile = new ExternalStorageFileImpl(path);
        if (tempFile.exists()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(path);
            stringBuilder.append(" exist, delete it");
            Log.w(str, stringBuilder.toString());
            if (!tempFile.delete()) {
                Log.w(TAG, "delete fail.");
            }
        }
        c.moveToNext();
        int handleExist = c.getInt(c.getColumnIndex("_id"));
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("file already exists in beginSendObject: ");
        stringBuilder2.append(path);
        stringBuilder2.append(", ID = ");
        stringBuilder2.append(handleExist);
        Log.w(str2, stringBuilder2.toString());
        return handleExist;
    }

    public void hwSaveCurrentObject(MtpObject mtpObject, Cursor c) {
        c.moveToFirst();
        synchronized (this.mLockObject) {
            int i = 0;
            int i2 = 0;
            while (i2 < 24) {
                try {
                    this.mObjectColumnInfo.types[i2] = c.getType(i2);
                    i2++;
                } catch (Throwable th) {
                }
            }
            this.mObjectColumnInfo.handle = mtpObject.getId();
            this.mObjectColumnInfo.storageId = mtpObject.getStorageId();
            this.mObjectColumnInfo.format = c.getInt(2);
            if (mtpObject.getParent() != null) {
                MtpObjectColumnInfo mtpObjectColumnInfo = this.mObjectColumnInfo;
                if (!mtpObject.getParent().isRoot()) {
                    i = mtpObject.getParent().getId();
                }
                mtpObjectColumnInfo.parent = i;
            }
            this.mObjectColumnInfo.data = c.getString(4);
            this.mObjectColumnInfo.size = mtpObject.getSize();
            this.mObjectColumnInfo.dateAdded = c.getLong(6);
            this.mObjectColumnInfo.dateModified = mtpObject.getModifiedTime();
            this.mObjectColumnInfo.mimeType = c.getInt(8);
            this.mObjectColumnInfo.title = c.getString(9);
            this.mObjectColumnInfo.displayName = c.getString(10);
            this.mObjectColumnInfo.mediaType = c.getInt(11);
            this.mObjectColumnInfo.isDrm = c.getInt(12);
            this.mObjectColumnInfo.width = c.getInt(13);
            this.mObjectColumnInfo.height = c.getInt(14);
            this.mObjectColumnInfo.description = c.getString(15);
            this.mObjectColumnInfo.artist = c.getString(16);
            this.mObjectColumnInfo.album = c.getString(17);
            this.mObjectColumnInfo.albumArtist = c.getString(18);
            this.mObjectColumnInfo.duration = c.getInt(19);
            this.mObjectColumnInfo.track = c.getInt(20);
            this.mObjectColumnInfo.year = c.getInt(21);
            this.mObjectColumnInfo.composer = c.getString(22);
            this.mObjectColumnInfo.name = mtpObject.getName();
        }
    }

    public void hwClearSavedObject() {
        synchronized (this.mLockObject) {
            this.mObjectColumnInfo.handle = -1;
        }
    }

    public int hwGetSavedObjectHandle() {
        int i;
        synchronized (this.mLockObject) {
            i = this.mObjectColumnInfo.handle;
        }
        return i;
    }

    public MtpPropertyListEx getObjectPropertyList(MtpDatabase database, int handle, int format, int property, int groupCode) {
        if (groupCode == 0) {
            return null;
        }
        if (groupCode == 4 || groupCode == 8) {
            return getCurrentPropertyList(handle, getGroupObjectProperties(groupCode));
        }
        return new MtpPropertyListEx(43013);
    }

    public MtpPropertyListEx getObjectPropertyList(int property, int handle) {
        synchronized (this.mLockObject) {
            if (this.mObjectColumnInfo.handle > 0) {
                if (handle == this.mObjectColumnInfo.handle) {
                    MtpPropertyListEx currentPropertyList = getCurrentPropertyList(handle, new int[]{property});
                    return currentPropertyList;
                }
            }
            return null;
        }
    }

    public MtpPropertyListEx getObjectPropertyList(int handle, int format, int[] proplist) {
        synchronized (this.mLockObject) {
            if (this.mObjectColumnInfo.handle > 0) {
                if (handle == this.mObjectColumnInfo.handle) {
                    MtpPropertyListEx currentPropertyList;
                    if (format == 0 || this.mObjectColumnInfo.format == format) {
                        currentPropertyList = getCurrentPropertyList(handle, proplist);
                        return currentPropertyList;
                    }
                    currentPropertyList = new MtpPropertyListEx(8201);
                    return currentPropertyList;
                }
            }
            return null;
        }
    }

    public int hwGetObjectFilePath(int handle, char[] outFilePath, long[] outFileLengthFormat) {
        synchronized (this.mLockObject) {
            if (this.mObjectColumnInfo.handle > 0) {
                if (handle == this.mObjectColumnInfo.handle) {
                    String path = this.mObjectColumnInfo.data;
                    path.getChars(0, path.length(), outFilePath, 0);
                    outFilePath[path.length()] = 0;
                    outFileLengthFormat[0] = new File(path).length();
                    outFileLengthFormat[1] = (long) this.mObjectColumnInfo.format;
                    return 0;
                }
            }
            return -1;
        }
    }

    public int hwGetObjectFormat(int handle) {
        synchronized (this.mLockObject) {
            if (this.mObjectColumnInfo.handle > 0) {
                if (handle == this.mObjectColumnInfo.handle) {
                    int i = this.mObjectColumnInfo.format;
                    return i;
                }
            }
            return -1;
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0026, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:18:0x0028, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean hwGetObjectReferences(int handle) {
        synchronized (this.mLockObject) {
            if (this.mObjectColumnInfo.handle > 0) {
                if (handle == this.mObjectColumnInfo.handle) {
                    if (this.mObjectColumnInfo.types[11] == 0 || 4 == this.mObjectColumnInfo.mediaType) {
                    } else {
                        return true;
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x003c, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean hwGetObjectInfo(int handle, int[] outStorageFormatParent, char[] outName, long[] outModified) {
        Throwable th;
        synchronized (this.mLockObject) {
            int i;
            try {
                if (this.mObjectColumnInfo.handle <= 0) {
                    i = handle;
                } else if (this.mObjectColumnInfo.handle == handle) {
                    composeObjectInfoParemeters(outStorageFormatParent, outName, outModified, this.mObjectColumnInfo.storageId, this.mObjectColumnInfo.format, this.mObjectColumnInfo.parent, this.mObjectColumnInfo.data, this.mObjectColumnInfo.size, this.mObjectColumnInfo.dateModified);
                    return true;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void composeObjectInfoParemeters(int[] outStorageFormatParent, char[] outName, long[] outSizeModified, int storageId, int format, int parent, String data, long size, long dateModified) {
        char[] cArr = outName;
        outStorageFormatParent[0] = storageId;
        outStorageFormatParent[1] = format;
        outStorageFormatParent[2] = parent;
        String path = data;
        int lastSlash = path.lastIndexOf(47);
        int start = lastSlash >= 0 ? lastSlash + 1 : 0;
        int end = path.length();
        if (end - start > 255) {
            end = start + 255;
        }
        path.getChars(start, end, cArr, 0);
        cArr[end - start] = 0;
        outSizeModified[0] = size;
        outSizeModified[1] = dateModified;
    }

    private int[] getGroupObjectProperties(int groupCode) {
        if (groupCode == 4) {
            return this.mGroup4Props;
        }
        if (groupCode == 8) {
            return this.mGroup8Props;
        }
        return new int[0];
    }

    private MtpPropertyListEx getCurrentPropertyList(int handle, int[] proplist) {
        if (this.mObjectColumnInfo.handle <= 0 || handle != this.mObjectColumnInfo.handle) {
            return null;
        }
        MtpPropertyListEx result = new MtpPropertyListEx(8193);
        int index = 0;
        while (true) {
            int index2 = index;
            if (index2 >= proplist.length) {
                return result;
            }
            int propertyCode = proplist[index2];
            String filePath;
            int suffixIndex;
            StringBuilder stringBuilder;
            switch (propertyCode) {
                case 56321:
                    result.append(handle, propertyCode, 6, (long) this.mObjectColumnInfo.storageId);
                    break;
                case 56322:
                    result.append(handle, propertyCode, 4, (long) this.mObjectColumnInfo.format);
                    break;
                case 56323:
                case 56978:
                case 56980:
                    result.append(handle, propertyCode, 4, 0);
                    break;
                case 56324:
                    if (needConvertHeifToJPG()) {
                        this.mObjectColumnInfo.size *= 3;
                    }
                    result.append(handle, propertyCode, 8, this.mObjectColumnInfo.size);
                    break;
                case 56327:
                    if (this.mObjectColumnInfo.types[4] != 0) {
                        if (this.mObjectColumnInfo.data == null) {
                            break;
                        }
                        filePath = this.mObjectColumnInfo.data;
                        if (!needConvertHeifToJPG()) {
                            result.append(handle, propertyCode, nameFromPath(this.mObjectColumnInfo.data));
                            break;
                        }
                        suffixIndex = filePath.lastIndexOf(46);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(filePath.substring(0, suffixIndex));
                        stringBuilder.append(SUFFIX_JPG_FILE);
                        result.append(handle, propertyCode, nameFromPath(stringBuilder.toString()));
                        break;
                    }
                    result.setResult(8201);
                    break;
                case 56329:
                    result.append(handle, propertyCode, format_date_time(this.mObjectColumnInfo.dateModified));
                    break;
                case 56331:
                    result.append(handle, propertyCode, 6, (long) this.mObjectColumnInfo.parent);
                    break;
                case 56385:
                    result.append(handle, propertyCode, 10, (((long) this.mObjectColumnInfo.storageId) << 32) + ((long) handle));
                    break;
                case 56388:
                    if (this.mObjectColumnInfo.types[9] == 0) {
                        if (this.mObjectColumnInfo.types[23] == 0) {
                            if (this.mObjectColumnInfo.types[4] == 0) {
                                result.setResult(8201);
                                break;
                            }
                            result.append(handle, propertyCode, nameFromPath(this.mObjectColumnInfo.data));
                            break;
                        }
                        result.append(handle, propertyCode, this.mObjectColumnInfo.name);
                        break;
                    }
                    result.append(handle, propertyCode, this.mObjectColumnInfo.title);
                    break;
                case 56390:
                    filePath = "";
                    if (this.mObjectColumnInfo.mediaType == 2) {
                        filePath = this.mObjectColumnInfo.artist;
                    }
                    result.append(handle, propertyCode, filePath);
                    break;
                case 56392:
                    result.append(handle, propertyCode, this.mObjectColumnInfo.description);
                    break;
                case 56398:
                    result.append(handle, propertyCode, format_date_time(this.mObjectColumnInfo.dateAdded));
                    break;
                case 56457:
                    result.append(handle, propertyCode, 6, (long) this.mObjectColumnInfo.duration);
                    break;
                case 56459:
                    result.append(handle, propertyCode, 4, (long) (this.mObjectColumnInfo.track % 1000));
                    break;
                case 56460:
                    if (this.mObjectColumnInfo.mediaType != 2) {
                        result.append(handle, propertyCode, "");
                        break;
                    }
                    result.append(handle, propertyCode, this.mObjectColumnInfo.name);
                    break;
                case 56470:
                    result.append(handle, propertyCode, this.mObjectColumnInfo.composer);
                    break;
                case 56473:
                    filePath = new StringBuilder();
                    filePath.append(Integer.toString(this.mObjectColumnInfo.year));
                    filePath.append("0101T000000");
                    result.append(handle, propertyCode, filePath.toString());
                    break;
                case 56474:
                    filePath = "";
                    if (this.mObjectColumnInfo.mediaType == 2) {
                        filePath = this.mObjectColumnInfo.album;
                    }
                    result.append(handle, propertyCode, filePath);
                    break;
                case 56475:
                    result.append(handle, propertyCode, this.mObjectColumnInfo.albumArtist);
                    break;
                case 56544:
                    if (this.mObjectColumnInfo.displayName != null) {
                        filePath = this.mObjectColumnInfo.displayName;
                        if (needConvertHeifToJPG()) {
                            suffixIndex = filePath.lastIndexOf(46);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(filePath.substring(0, suffixIndex));
                            stringBuilder.append(SUFFIX_JPG_FILE);
                            result.append(handle, propertyCode, stringBuilder.toString());
                            break;
                        }
                    }
                    result.append(handle, propertyCode, this.mObjectColumnInfo.displayName);
                    break;
                case 56979:
                case 56985:
                case 56986:
                    result.append(handle, propertyCode, 6, 0);
                    break;
                default:
                    result.append(handle, propertyCode, 0, 0);
                    break;
            }
            index = index2 + 1;
        }
    }

    private static String nameFromPath(String path) {
        int start = 0;
        int lastSlash = path.lastIndexOf(47);
        if (lastSlash >= 0) {
            start = lastSlash + 1;
        }
        int end = path.length();
        if (end - start > 255) {
            end = start + 255;
        }
        return path.substring(start, end);
    }

    private static String format_date_time(long seconds) {
        return MtpPropertyGroup.format_date_time(seconds);
    }

    private boolean needConvertHeifToJPG() {
        boolean isAutomaticMode = MtpDatabase.issIsHeifSettingAutomaticMode();
        boolean isHeifFile = this.mObjectColumnInfo.format == 14354;
        if (isAutomaticMode && isHeifFile) {
            return true;
        }
        return false;
    }
}
