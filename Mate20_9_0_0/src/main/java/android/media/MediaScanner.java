package android.media;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.common.HwFrameworkFactory;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.drm.DrmManagerClient;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.hwtheme.HwThemeManager;
import android.media.MediaCodec.MetricsConstants;
import android.media.MediaFile.MediaFileType;
import android.media.midi.MidiDeviceInfo;
import android.mtp.MtpConstants;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.ExternalStorageFileImpl;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import dalvik.system.CloseGuard;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class MediaScanner implements AutoCloseable {
    private static final String ALARMS_DIR = "/alarms/";
    private static final int DATE_MODIFIED_PLAYLISTS_COLUMN_INDEX = 2;
    private static final String DEFAULT_RINGTONE_PROPERTY_PREFIX = "ro.config.";
    private static final boolean ENABLE_BULK_INSERTS = true;
    private static final int FILES_BLACKLIST_ID_COLUMN_INDEX = 0;
    private static final int FILES_BLACKLIST_MEDIA_TYPE_COLUMN_INDEX = 2;
    private static final int FILES_BLACKLIST_PATH_COLUMN_INDEX = 1;
    private static final String[] FILES_BLACKLIST_PROJECTION_MEDIA = new String[]{DownloadManager.COLUMN_ID, "_data", DownloadManager.COLUMN_MEDIA_TYPE};
    private static final int FILES_PRESCAN_DATE_MODIFIED_COLUMN_INDEX = 3;
    private static final int FILES_PRESCAN_FORMAT_COLUMN_INDEX = 2;
    private static final int FILES_PRESCAN_ID_COLUMN_INDEX = 0;
    private static final int FILES_PRESCAN_MEDIA_TYPE_COLUMN_INDEX = 4;
    private static final int FILES_PRESCAN_PATH_COLUMN_INDEX = 1;
    private static final String[] FILES_PRESCAN_PROJECTION = new String[]{DownloadManager.COLUMN_ID, "_data", "format", "date_modified", DownloadManager.COLUMN_MEDIA_TYPE};
    private static final String[] FILES_PRESCAN_PROJECTION_MEDIA = new String[]{DownloadManager.COLUMN_ID, "_data"};
    private static final String HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN = "special_file_type";
    private static final String[] ID3_GENRES = new String[]{"Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave", "Psychadelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival", "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music", "Sonata", "Symphony", "Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo", "A capella", "Euro-House", "Dance Hall", "Goa", "Drum & Bass", "Club-House", "Hardcore", "Terror", "Indie", "Britpop", null, "Polsk Punk", "Beat", "Christian Gangsta", "Heavy Metal", "Black Metal", "Crossover", "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal", "Anime", "JPop", "Synthpop"};
    private static final int ID_PLAYLISTS_COLUMN_INDEX = 0;
    private static final String[] ID_PROJECTION = new String[]{DownloadManager.COLUMN_ID};
    private static final String IMAGE_TYPE_BEAUTY_FRONT = "fbt";
    private static final String IMAGE_TYPE_BEAUTY_REAR = "rbt";
    private static final String IMAGE_TYPE_HDR = "hdr";
    private static final String IMAGE_TYPE_JHDR = "jhdr";
    private static final String IMAGE_TYPE_PORTRAIT_FRONT = "fpt";
    private static final String IMAGE_TYPE_PORTRAIT_REAR = "rpt";
    private static final int IMAGE_TYPE_VALUE_BEAUTY_FRONT = 40;
    private static final int IMAGE_TYPE_VALUE_BEAUTY_REAR = 41;
    private static final int IMAGE_TYPE_VALUE_DEFAULT = 0;
    private static final int IMAGE_TYPE_VALUE_HDR = 1;
    private static final int IMAGE_TYPE_VALUE_JHDR = 2;
    private static final int IMAGE_TYPE_VALUE_PORTRAIT_FRONT = 30;
    private static final int IMAGE_TYPE_VALUE_PORTRAIT_REAR = 31;
    private static final String INTERNAL_VOLUME = "internal";
    public static final String LAST_INTERNAL_SCAN_FINGERPRINT = "lastScanFingerprint";
    private static final int MAX_ENTRY_SIZE = 40000;
    private static long MAX_NOMEDIA_SIZE = 1024;
    private static final String MUSIC_DIR = "/music/";
    private static final String NOTIFICATIONS_DIR = "/notifications/";
    private static final int PATH_PLAYLISTS_COLUMN_INDEX = 1;
    private static final String[] PLAYLIST_MEMBERS_PROJECTION = new String[]{"playlist_id"};
    private static final String PODCAST_DIR = "/podcasts/";
    private static final String PRODUCT_SOUNDS_DIR = "/product/media/audio";
    private static final String RINGTONES_DIR = "/ringtones/";
    public static final String SCANNED_BUILD_PREFS_NAME = "MediaScanBuild";
    private static final int SQL_MEDIA_TYPE_BLACKLIST = 10;
    private static final int SQL_MEDIA_TYPE_IMGAGE = 1;
    private static final int SQL_QUERY_COUNT = 100;
    private static final String SQL_QUERY_LIMIT = "1000";
    private static final String SQL_VALUE_EXIF_FLAG = "1";
    private static final String SYSTEM_SOUNDS_DIR = "/system/media/audio";
    private static final String TAG = "MediaScanner";
    private static HashMap<String, String> mMediaPaths = new HashMap();
    private static HashMap<String, String> mNoMediaPaths = new HashMap();
    public static final Set sBlackList = new HashSet();
    public static String sCurScanDIR = "";
    private static String sLastInternalScanFingerprint;
    private static final String[] sNomediaFilepath = new String[]{"/.nomedia", "/DCIM/.nomedia", "/DCIM/Camera/.nomedia", "/Pictures/.nomedia", "/Pictures/Screenshots/.nomedia", "/tencent/.nomedia", "/tencent/MicroMsg/.nomedia", "/tencent/MicroMsg/Weixin/.nomedia", "/tencent/QQ_Images/.nomedia"};
    private final Uri mAudioUri;
    private final Options mBitmapOptions = new Options();
    private boolean mBlackListFlag = false;
    private final MyMediaScannerClient mClient = new MyMediaScannerClient();
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final Context mContext;
    private String mDefaultAlarmAlertFilename;
    private boolean mDefaultAlarmSet;
    private String mDefaultNotificationFilename;
    private boolean mDefaultNotificationSet;
    private String mDefaultRingtoneFilename;
    private boolean mDefaultRingtoneSet;
    private DrmManagerClient mDrmManagerClient = null;
    private String mExtStroagePath;
    private HashMap<String, FileEntry> mFileCache;
    private final Uri mFilesUri;
    private final Uri mFilesUriNoNotify;
    private final Uri mImagesUri;
    private boolean mIsImageType = false;
    private boolean mIsPrescanFiles = true;
    private MediaInserter mMediaInserter;
    private ContentProviderClient mMediaProvider;
    private boolean mMediaTypeConflict;
    private int mMtpObjectHandle;
    private long mNativeContext;
    private boolean mNeedFilter;
    private int mOriginalCount;
    private final String mPackageName;
    private final ArrayList<FileEntry> mPlayLists = new ArrayList();
    private final ArrayList<PlaylistEntry> mPlaylistEntries = new ArrayList();
    private final Uri mPlaylistsUri;
    private final boolean mProcessGenres;
    private final boolean mProcessPlaylists;
    private long mScanDirectoryFilesNum = 0;
    private boolean mSkipExternelQuery = false;
    private final Uri mVideoUri;
    private final String mVolumeName;

    private static class FileEntry {
        int mFormat;
        long mLastModified;
        boolean mLastModifiedChanged = false;
        int mMediaType;
        String mPath;
        long mRowId;

        FileEntry(long rowId, String path, long lastModified, int format, int mediaType) {
            this.mRowId = rowId;
            this.mPath = path;
            this.mLastModified = lastModified;
            this.mFormat = format;
            this.mMediaType = mediaType;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mPath);
            stringBuilder.append(" mRowId: ");
            stringBuilder.append(this.mRowId);
            return stringBuilder.toString();
        }
    }

    static class MediaBulkDeleter {
        final Uri mBaseUri;
        final ContentProviderClient mProvider;
        ArrayList<String> whereArgs = new ArrayList(100);
        StringBuilder whereClause = new StringBuilder();

        public MediaBulkDeleter(ContentProviderClient provider, Uri baseUri) {
            this.mProvider = provider;
            this.mBaseUri = baseUri;
        }

        public void delete(long id) throws RemoteException {
            if (this.whereClause.length() != 0) {
                this.whereClause.append(",");
            }
            this.whereClause.append("?");
            ArrayList arrayList = this.whereArgs;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(id);
            arrayList.add(stringBuilder.toString());
            if (this.whereArgs.size() > 100) {
                flush();
            }
        }

        public void flush() throws RemoteException {
            int size = this.whereArgs.size();
            if (size > 0) {
                String[] foo = (String[]) this.whereArgs.toArray(new String[size]);
                int numrows = this.mProvider;
                Uri uri = this.mBaseUri;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("_id IN (");
                stringBuilder.append(this.whereClause.toString());
                stringBuilder.append(")");
                numrows = numrows.delete(uri, stringBuilder.toString(), foo);
                this.whereClause.setLength(0);
                this.whereArgs.clear();
            }
        }
    }

    private static class PlaylistEntry {
        long bestmatchid;
        int bestmatchlevel;
        String path;

        private PlaylistEntry() {
        }
    }

    public class MyMediaScannerClient implements MediaScannerClient {
        private static final int ALBUM = 1;
        private static final int ARTIST = 2;
        private static final int TITLE = 3;
        private static final long Time_1970 = 2082844800;
        private String mAlbum;
        private String mAlbumArtist;
        private String mArtist;
        private int mCompilation;
        private String mComposer;
        private long mDate;
        private final SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        private int mDuration;
        private long mFileSize;
        private int mFileType;
        private String mGenre;
        private int mHeight;
        private boolean mIsAlbumMessy;
        private boolean mIsArtistMessy;
        private boolean mIsDrm;
        private boolean mIsTitleMessy;
        private long mLastModified;
        private String mMimeType;
        private boolean mNoMedia;
        private String mPath;
        private boolean mScanSuccess;
        private String mTitle;
        private int mTrack;
        private int mWidth;
        private String mWriter;
        private int mYear;

        public MyMediaScannerClient() {
            this.mDateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public FileEntry beginFile(String path, String mimeType, long lastModified, long fileSize, boolean isDirectory, boolean noMedia) {
            String str = path;
            String str2 = mimeType;
            long j = lastModified;
            this.mMimeType = str2;
            this.mFileType = 0;
            this.mFileSize = fileSize;
            this.mIsDrm = false;
            if (MediaScanner.this.mNeedFilter && HwFrameworkFactory.getHwMediaScannerManager().isAudioFilterFile(str)) {
                return null;
            }
            boolean noMedia2;
            if (str != null && (str.endsWith(".isma") || str.endsWith(".ismv"))) {
                this.mIsDrm = true;
            }
            this.mScanSuccess = true;
            if (isDirectory) {
            } else {
                if (noMedia || !MediaScanner.isNoMediaFile(path)) {
                    noMedia2 = noMedia;
                } else {
                    noMedia2 = true;
                }
                this.mNoMedia = noMedia2;
                if (str2 != null) {
                    this.mFileType = MediaFile.getFileTypeForMimeType(mimeType);
                }
                if (this.mFileType == 0) {
                    MediaFileType mediaFileType = MediaFile.getFileType(path);
                    if (mediaFileType != null) {
                        this.mFileType = mediaFileType.fileType;
                        if (this.mMimeType == null) {
                            this.mMimeType = mediaFileType.mimeType;
                        }
                    }
                }
                if (MediaScanner.this.isDrmEnabled() && MediaFile.isDrmFileType(this.mFileType)) {
                    this.mFileType = getFileTypeFromDrm(path);
                }
                boolean z = noMedia2;
            }
            String key = str;
            FileEntry entry = (FileEntry) MediaScanner.this.mFileCache.remove(key);
            if (entry == null && !(MediaScanner.this.mSkipExternelQuery && str.startsWith(MediaScanner.this.mExtStroagePath))) {
                entry = MediaScanner.this.makeEntryFor(str);
            }
            FileEntry entry2 = entry;
            long delta = entry2 != null ? j - entry2.mLastModified : 0;
            noMedia2 = delta > 1 || delta < -1;
            boolean wasModified = noMedia2;
            String str3;
            if (entry2 == null || wasModified) {
                boolean z2;
                if (wasModified) {
                    entry2.mLastModified = j;
                    z2 = true;
                    str3 = key;
                } else {
                    z2 = true;
                    entry2 = new FileEntry(0, str, j, isDirectory ? 12289 : 0, this.mFileType);
                }
                entry2.mLastModifiedChanged = z2;
            } else {
                str3 = key;
            }
            if (MediaScanner.this.mProcessPlaylists && MediaFile.isPlayListFileType(this.mFileType)) {
                MediaScanner.this.mPlayLists.add(entry2);
                return null;
            }
            this.mArtist = null;
            this.mAlbumArtist = null;
            this.mAlbum = null;
            this.mTitle = null;
            this.mComposer = null;
            this.mGenre = null;
            this.mTrack = 0;
            this.mYear = 0;
            this.mDuration = 0;
            this.mPath = str;
            this.mDate = 0;
            this.mLastModified = j;
            this.mWriter = null;
            this.mCompilation = 0;
            this.mWidth = 0;
            this.mHeight = 0;
            this.mIsAlbumMessy = false;
            this.mIsArtistMessy = false;
            this.mIsTitleMessy = false;
            return entry2;
        }

        public void setBlackListFlag(boolean flag) {
            MediaScanner.this.mBlackListFlag = true;
        }

        public void scanFile(String path, long lastModified, long fileSize, boolean isDirectory, boolean noMedia) {
            MediaScanner.access$904(MediaScanner.this);
            doScanFile(path, null, lastModified, fileSize, isDirectory, false, noMedia);
        }

        /* JADX WARNING: Removed duplicated region for block: B:88:0x0193 A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:99:0x01c7 A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:102:0x01da A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:105:0x01ed A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:83:0x0181 A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:88:0x0193 A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:99:0x01c7 A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:102:0x01da A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:105:0x01ed A:{Catch:{ RemoteException -> 0x0216, NullPointerException -> 0x0214 }} */
        /* JADX WARNING: Removed duplicated region for block: B:44:0x00e0 A:{SYNTHETIC, Splitter:B:44:0x00e0} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Uri doScanFile(String path, String mimeType, long lastModified, long fileSize, boolean isDirectory, boolean scanAlways, boolean noMedia) {
            RemoteException e;
            NullPointerException e2;
            String path2 = path;
            if (path2 != null && (path.toUpperCase().endsWith(".HEIC") || path.toUpperCase().endsWith(".HEIF"))) {
                HwMediaMonitorManager.writeBigData(HwMediaMonitorUtils.BD_MEDIA_HEIF, 3);
            }
            Uri result = null;
            boolean scanAlways2;
            try {
                FileEntry entry = beginFile(path2, mimeType, lastModified, fileSize, isDirectory, noMedia);
                if (entry == null) {
                    return null;
                }
                if (MediaScanner.this.mMtpObjectHandle != 0) {
                    entry.mRowId = 0;
                }
                if (entry.mPath != null) {
                    boolean scanAlways3;
                    if ((!MediaScanner.this.mDefaultNotificationSet && doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultNotificationFilename)) || ((!MediaScanner.this.mDefaultRingtoneSet && doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultRingtoneFilename)) || (!MediaScanner.this.mDefaultAlarmSet && doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultAlarmAlertFilename)))) {
                        Log.w(MediaScanner.TAG, "forcing rescan , since ringtone setting didn't finish");
                        scanAlways3 = true;
                    } else if (MediaScanner.isSystemSoundWithMetadata(entry.mPath) && !Build.FINGERPRINT.equals(MediaScanner.sLastInternalScanFingerprint)) {
                        String str = MediaScanner.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("forcing rescan of ");
                        stringBuilder.append(entry.mPath);
                        stringBuilder.append(" since build fingerprint changed");
                        Log.i(str, stringBuilder.toString());
                        scanAlways3 = true;
                    } else if (this.mFileType == 40 && entry.mMediaType != 1) {
                        MediaScanner.this.mMediaTypeConflict = true;
                        scanAlways3 = true;
                    }
                    scanAlways2 = scanAlways3;
                    if (entry != null) {
                        try {
                            if (entry.mLastModifiedChanged || scanAlways2 || this.mIsDrm) {
                                if (noMedia) {
                                    result = endFile(entry, false, false, false, false, false);
                                } else {
                                    boolean music;
                                    boolean isaudio;
                                    boolean isvideo;
                                    boolean isimage;
                                    boolean ringtones;
                                    boolean notifications;
                                    boolean alarms;
                                    boolean podcasts;
                                    boolean music2;
                                    String lowpath = path2.toLowerCase(Locale.ROOT);
                                    scanAlways3 = lowpath.indexOf(MediaScanner.RINGTONES_DIR) > 0;
                                    boolean notifications2 = lowpath.indexOf(MediaScanner.NOTIFICATIONS_DIR) > 0;
                                    boolean alarms2 = lowpath.indexOf(MediaScanner.ALARMS_DIR) > 0;
                                    boolean podcasts2 = lowpath.indexOf(MediaScanner.PODCAST_DIR) > 0;
                                    if (lowpath.indexOf(MediaScanner.MUSIC_DIR) <= 0) {
                                        if (scanAlways3 || notifications2 || alarms2 || podcasts2) {
                                            music = false;
                                            scanAlways3 |= HwThemeManager.isTRingtones(lowpath);
                                            notifications2 |= HwThemeManager.isTNotifications(lowpath);
                                            alarms2 |= HwThemeManager.isTAlarms(lowpath);
                                            isaudio = MediaFile.isAudioFileType(this.mFileType);
                                            isvideo = MediaFile.isVideoFileType(this.mFileType);
                                            isimage = MediaFile.isImageFileType(this.mFileType);
                                            MediaScanner.this.mIsImageType = isimage;
                                            if (isaudio || isvideo || isimage) {
                                                path2 = Environment.maybeTranslateEmulatedPathToInternal(new File(path2)).getAbsolutePath();
                                            }
                                            if (!isaudio) {
                                                if (!isvideo) {
                                                    String str2 = mimeType;
                                                    if (isimage) {
                                                        this.mScanSuccess = processImageFile(path2);
                                                    }
                                                    ringtones = this.mScanSuccess & scanAlways3;
                                                    notifications = notifications2 & this.mScanSuccess;
                                                    alarms = alarms2 & this.mScanSuccess;
                                                    podcasts = podcasts2 & this.mScanSuccess;
                                                    music2 = music & this.mScanSuccess;
                                                    if (isaudio && (this.mIsAlbumMessy || this.mIsArtistMessy || this.mIsTitleMessy)) {
                                                        HwFrameworkFactory.getHwMediaScannerManager().initializeSniffer(this.mPath);
                                                        if (this.mIsAlbumMessy) {
                                                            this.mAlbum = HwFrameworkFactory.getHwMediaScannerManager().postHandleStringTag(this.mAlbum, this.mPath, 1);
                                                        }
                                                        if (this.mIsArtistMessy) {
                                                            this.mArtist = HwFrameworkFactory.getHwMediaScannerManager().postHandleStringTag(this.mArtist, this.mPath, 2);
                                                        }
                                                        if (this.mIsTitleMessy) {
                                                            this.mTitle = HwFrameworkFactory.getHwMediaScannerManager().postHandleStringTag(this.mTitle, this.mPath, 3);
                                                        }
                                                        HwFrameworkFactory.getHwMediaScannerManager().resetSniffer();
                                                    }
                                                    result = endFile(entry, ringtones, notifications, alarms, music2, podcasts);
                                                }
                                            }
                                            this.mScanSuccess = MediaScanner.this.processFile(path2, mimeType, this);
                                            if (isimage) {
                                            }
                                            ringtones = this.mScanSuccess & scanAlways3;
                                            notifications = notifications2 & this.mScanSuccess;
                                            alarms = alarms2 & this.mScanSuccess;
                                            podcasts = podcasts2 & this.mScanSuccess;
                                            music2 = music & this.mScanSuccess;
                                            HwFrameworkFactory.getHwMediaScannerManager().initializeSniffer(this.mPath);
                                            if (this.mIsAlbumMessy) {
                                            }
                                            if (this.mIsArtistMessy) {
                                            }
                                            if (this.mIsTitleMessy) {
                                            }
                                            HwFrameworkFactory.getHwMediaScannerManager().resetSniffer();
                                            result = endFile(entry, ringtones, notifications, alarms, music2, podcasts);
                                        }
                                    }
                                    music = true;
                                    scanAlways3 |= HwThemeManager.isTRingtones(lowpath);
                                    notifications2 |= HwThemeManager.isTNotifications(lowpath);
                                    alarms2 |= HwThemeManager.isTAlarms(lowpath);
                                    isaudio = MediaFile.isAudioFileType(this.mFileType);
                                    isvideo = MediaFile.isVideoFileType(this.mFileType);
                                    isimage = MediaFile.isImageFileType(this.mFileType);
                                    MediaScanner.this.mIsImageType = isimage;
                                    path2 = Environment.maybeTranslateEmulatedPathToInternal(new File(path2)).getAbsolutePath();
                                    if (isaudio) {
                                    }
                                    this.mScanSuccess = MediaScanner.this.processFile(path2, mimeType, this);
                                    if (isimage) {
                                    }
                                    ringtones = this.mScanSuccess & scanAlways3;
                                    notifications = notifications2 & this.mScanSuccess;
                                    alarms = alarms2 & this.mScanSuccess;
                                    podcasts = podcasts2 & this.mScanSuccess;
                                    music2 = music & this.mScanSuccess;
                                    HwFrameworkFactory.getHwMediaScannerManager().initializeSniffer(this.mPath);
                                    if (this.mIsAlbumMessy) {
                                    }
                                    if (this.mIsArtistMessy) {
                                    }
                                    if (this.mIsTitleMessy) {
                                    }
                                    HwFrameworkFactory.getHwMediaScannerManager().resetSniffer();
                                    result = endFile(entry, ringtones, notifications, alarms, music2, podcasts);
                                }
                            }
                        } catch (RemoteException e3) {
                            e = e3;
                            Log.e(MediaScanner.TAG, "RemoteException in MediaScanner.scanFile()", e);
                            MediaScanner.this.mBlackListFlag = false;
                            MediaScanner.this.mIsImageType = false;
                            return result;
                        } catch (NullPointerException e4) {
                            e2 = e4;
                            Log.e(MediaScanner.TAG, "NullPointerException in MediaScanner", e2);
                            MediaScanner.this.mBlackListFlag = false;
                            MediaScanner.this.mIsImageType = false;
                            return result;
                        }
                    }
                    MediaScanner.this.mBlackListFlag = false;
                    MediaScanner.this.mIsImageType = false;
                    return result;
                }
                scanAlways2 = scanAlways;
                if (entry != null) {
                }
                MediaScanner.this.mBlackListFlag = false;
                MediaScanner.this.mIsImageType = false;
                return result;
            } catch (RemoteException e5) {
                e = e5;
                scanAlways2 = scanAlways;
                Log.e(MediaScanner.TAG, "RemoteException in MediaScanner.scanFile()", e);
                MediaScanner.this.mBlackListFlag = false;
                MediaScanner.this.mIsImageType = false;
                return result;
            } catch (NullPointerException e6) {
                e2 = e6;
                scanAlways2 = scanAlways;
                Log.e(MediaScanner.TAG, "NullPointerException in MediaScanner", e2);
                MediaScanner.this.mBlackListFlag = false;
                MediaScanner.this.mIsImageType = false;
                return result;
            }
        }

        private long parseDate(String date) {
            try {
                return this.mDateFormatter.parse(date).getTime();
            } catch (ParseException e) {
                return 0;
            }
        }

        private int parseSubstring(String s, int ch, int defaultValue) {
            int length = s.length();
            if (ch == length) {
                return defaultValue;
            }
            int start = ch + 1;
            char ch2 = s.charAt(ch);
            if (ch2 < '0' || ch2 > '9') {
                return defaultValue;
            }
            int result = ch2 - 48;
            while (start < length) {
                int start2 = start + 1;
                ch2 = s.charAt(start);
                if (ch2 < '0' || ch2 > '9') {
                    return result;
                }
                result = (result * 10) + (ch2 - 48);
                start = start2;
            }
            return result;
        }

        public void handleStringTag(String name, String value) {
            boolean z = true;
            boolean isAlbum = name.equalsIgnoreCase("album") || name.startsWith("album;");
            boolean isArtist = name.equalsIgnoreCase("artist") || name.startsWith("artist;");
            boolean isTitle = name.equalsIgnoreCase("title") || name.startsWith("title;");
            if (isAlbum) {
                this.mIsAlbumMessy = HwFrameworkFactory.getHwMediaScannerManager().preHandleStringTag(value, this.mMimeType);
            }
            if (isArtist) {
                this.mIsArtistMessy = HwFrameworkFactory.getHwMediaScannerManager().preHandleStringTag(value, this.mMimeType);
            }
            if (isTitle) {
                this.mIsTitleMessy = HwFrameworkFactory.getHwMediaScannerManager().preHandleStringTag(value, this.mMimeType);
            }
            if (name.equalsIgnoreCase("title") || name.startsWith("title;")) {
                this.mTitle = value;
            } else if (name.equalsIgnoreCase("artist") || name.startsWith("artist;")) {
                this.mArtist = value.trim();
            } else if (name.equalsIgnoreCase("albumartist") || name.startsWith("albumartist;") || name.equalsIgnoreCase("band") || name.startsWith("band;")) {
                this.mAlbumArtist = value.trim();
            } else if (name.equalsIgnoreCase("album") || name.startsWith("album;")) {
                this.mAlbum = value.trim();
            } else if (name.equalsIgnoreCase("composer") || name.startsWith("composer;")) {
                this.mComposer = value.trim();
            } else if (MediaScanner.this.mProcessGenres && (name.equalsIgnoreCase("genre") || name.startsWith("genre;"))) {
                this.mGenre = getGenreName(value);
            } else if (name.equalsIgnoreCase("year") || name.startsWith("year;")) {
                this.mYear = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("tracknumber") || name.startsWith("tracknumber;")) {
                this.mTrack = ((this.mTrack / 1000) * 1000) + parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("discnumber") || name.equals("set") || name.startsWith("set;")) {
                this.mTrack = (parseSubstring(value, 0, 0) * 1000) + (this.mTrack % 1000);
            } else if (name.equalsIgnoreCase("duration")) {
                this.mDuration = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("writer") || name.startsWith("writer;")) {
                this.mWriter = value.trim();
            } else if (name.equalsIgnoreCase("compilation")) {
                this.mCompilation = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("isdrm")) {
                if (parseSubstring(value, 0, 0) != 1) {
                    z = false;
                }
                this.mIsDrm = z;
            } else if (name.equalsIgnoreCase("date")) {
                this.mDate = parseDate(value);
            } else if (name.equalsIgnoreCase(MediaFormat.KEY_WIDTH)) {
                this.mWidth = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase(MediaFormat.KEY_HEIGHT)) {
                this.mHeight = parseSubstring(value, 0, 0);
            }
        }

        private boolean convertGenreCode(String input, String expected) {
            String output = getGenreName(input);
            if (output.equals(expected)) {
                return true;
            }
            String str = MediaScanner.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("'");
            stringBuilder.append(input);
            stringBuilder.append("' -> '");
            stringBuilder.append(output);
            stringBuilder.append("', expected '");
            stringBuilder.append(expected);
            stringBuilder.append("'");
            Log.d(str, stringBuilder.toString());
            return false;
        }

        private void testGenreNameConverter() {
            convertGenreCode("2", "Country");
            convertGenreCode("(2)", "Country");
            convertGenreCode("(2", "(2");
            convertGenreCode("2 Foo", "Country");
            convertGenreCode("(2) Foo", "Country");
            convertGenreCode("(2 Foo", "(2 Foo");
            convertGenreCode("2Foo", "2Foo");
            convertGenreCode("(2)Foo", "Country");
            convertGenreCode("200 Foo", "Foo");
            convertGenreCode("(200) Foo", "Foo");
            convertGenreCode("200Foo", "200Foo");
            convertGenreCode("(200)Foo", "Foo");
            convertGenreCode("200)Foo", "200)Foo");
            convertGenreCode("200) Foo", "200) Foo");
        }

        public String getGenreName(String genreTagValue) {
            if (genreTagValue == null) {
                return null;
            }
            int length = genreTagValue.length();
            if (length > 0) {
                char c;
                boolean parenthesized = false;
                StringBuffer number = new StringBuffer();
                int i = 0;
                while (i < length) {
                    c = genreTagValue.charAt(i);
                    if (i != 0 || c != '(') {
                        if (!Character.isDigit(c)) {
                            break;
                        }
                        number.append(c);
                    } else {
                        parenthesized = true;
                    }
                    i++;
                }
                c = i < length ? genreTagValue.charAt(i) : ' ';
                if ((parenthesized && c == ')') || (!parenthesized && Character.isWhitespace(c))) {
                    try {
                        short genreIndex = Short.parseShort(number.toString());
                        if (genreIndex >= (short) 0) {
                            if (genreIndex < MediaScanner.ID3_GENRES.length && MediaScanner.ID3_GENRES[genreIndex] != null) {
                                return MediaScanner.ID3_GENRES[genreIndex];
                            }
                            if (genreIndex == (short) 255) {
                                return null;
                            }
                            if (genreIndex >= (short) 255 || i + 1 >= length) {
                                return number.toString();
                            }
                            if (parenthesized && c == ')') {
                                i++;
                            }
                            String ret = genreTagValue.substring(i).trim();
                            if (ret.length() != 0) {
                                return ret;
                            }
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
            return genreTagValue;
        }

        private boolean processImageFile(String path) {
            boolean z = false;
            try {
                MediaScanner.this.mBitmapOptions.outWidth = 0;
                MediaScanner.this.mBitmapOptions.outHeight = 0;
                if (HwFrameworkFactory.getHwMediaScannerManager().isBitmapSizeTooLarge(path)) {
                    this.mWidth = -1;
                    this.mHeight = -1;
                } else {
                    BitmapFactory.decodeFile(path, MediaScanner.this.mBitmapOptions);
                    this.mWidth = MediaScanner.this.mBitmapOptions.outWidth;
                    this.mHeight = MediaScanner.this.mBitmapOptions.outHeight;
                }
                if (this.mWidth > 0 && this.mHeight > 0) {
                    z = true;
                }
                return z;
            } catch (Throwable th) {
                return false;
            }
        }

        public void setMimeType(String mimeType) {
            if (!"audio/mp4".equals(this.mMimeType) || !mimeType.startsWith(MetricsConstants.MODE_VIDEO)) {
                this.mMimeType = mimeType;
                this.mFileType = MediaFile.getFileTypeForMimeType(mimeType);
            }
        }

        private ContentValues toValues() {
            ContentValues map = new ContentValues();
            map.put("_data", this.mPath);
            map.put("title", this.mTitle);
            map.put("date_modified", Long.valueOf(this.mLastModified));
            map.put("_size", Long.valueOf(this.mFileSize));
            map.put("mime_type", this.mMimeType);
            map.put("is_drm", Boolean.valueOf(this.mIsDrm));
            String resolution = null;
            if (this.mWidth > 0 && this.mHeight > 0) {
                map.put(MediaFormat.KEY_WIDTH, Integer.valueOf(this.mWidth));
                map.put(MediaFormat.KEY_HEIGHT, Integer.valueOf(this.mHeight));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.mWidth);
                stringBuilder.append("x");
                stringBuilder.append(this.mHeight);
                resolution = stringBuilder.toString();
            }
            if (!this.mNoMedia) {
                String str;
                String str2;
                if (MediaFile.isVideoFileType(this.mFileType)) {
                    str = "artist";
                    str2 = (this.mArtist == null || this.mArtist.length() <= 0) ? "<unknown>" : this.mArtist;
                    map.put(str, str2);
                    str = "album";
                    str2 = (this.mAlbum == null || this.mAlbum.length() <= 0) ? "<unknown>" : this.mAlbum;
                    map.put(str, str2);
                    map.put("duration", Integer.valueOf(this.mDuration));
                    if (resolution != null) {
                        map.put("resolution", resolution);
                    }
                    if (this.mDate > Time_1970) {
                        map.put("datetaken", Long.valueOf(this.mDate));
                    }
                } else if (!MediaFile.isImageFileType(this.mFileType) && this.mScanSuccess && MediaFile.isAudioFileType(this.mFileType)) {
                    str = "artist";
                    str2 = (this.mArtist == null || this.mArtist.length() <= 0) ? "<unknown>" : this.mArtist;
                    map.put(str, str2);
                    str = "album_artist";
                    str2 = (this.mAlbumArtist == null || this.mAlbumArtist.length() <= 0) ? null : this.mAlbumArtist;
                    map.put(str, str2);
                    str = "album";
                    str2 = (this.mAlbum == null || this.mAlbum.length() <= 0) ? "<unknown>" : this.mAlbum;
                    map.put(str, str2);
                    map.put("composer", this.mComposer);
                    map.put("genre", this.mGenre);
                    if (this.mYear != 0) {
                        map.put("year", Integer.valueOf(this.mYear));
                    }
                    map.put("track", Integer.valueOf(this.mTrack));
                    map.put("duration", Integer.valueOf(this.mDuration));
                    map.put("compilation", Integer.valueOf(this.mCompilation));
                }
                if (!this.mScanSuccess) {
                    map.put(DownloadManager.COLUMN_MEDIA_TYPE, Integer.valueOf(0));
                }
            }
            return map;
        }

        /* JADX WARNING: Removed duplicated region for block: B:21:0x006c  */
        /* JADX WARNING: Removed duplicated region for block: B:172:0x036b  */
        /* JADX WARNING: Removed duplicated region for block: B:147:0x02fd  */
        /* JADX WARNING: Removed duplicated region for block: B:120:0x027c  */
        /* JADX WARNING: Removed duplicated region for block: B:172:0x036b  */
        /* JADX WARNING: Missing block: B:96:0x020e, code skipped:
            if (doesPathHaveFilename(r2.mPath, android.media.MediaScanner.access$1200(r1.this$0)) != false) goto L_0x0213;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private Uri endFile(FileEntry entry, boolean ringtones, boolean notifications, boolean alarms, boolean music, boolean podcasts) throws RemoteException {
            boolean needToSetSettings2;
            boolean needToSetSettings;
            Uri result;
            Uri result2;
            long rowId;
            FileEntry fileEntry = entry;
            if (this.mArtist == null || this.mArtist.length() == 0) {
                this.mArtist = this.mAlbumArtist;
            }
            ContentValues values = toValues();
            String title = values.getAsString("title");
            if (title == null || TextUtils.isEmpty(title.trim())) {
                title = MediaFile.getFileTitle(values.getAsString("_data"));
                values.put("title", title);
            }
            title = values.getAsString("album");
            if ("<unknown>".equals(title)) {
                title = values.getAsString("_data");
                int lastSlash = title.lastIndexOf(47);
                if (lastSlash >= 0) {
                    int previousSlash = 0;
                    while (true) {
                        int idx = title.indexOf(47, previousSlash + 1);
                        if (idx >= 0 && idx < lastSlash) {
                            previousSlash = idx;
                        } else if (previousSlash != 0) {
                            title = title.substring(previousSlash + 1, lastSlash);
                            values.put("album", title);
                        }
                    }
                    if (previousSlash != 0) {
                    }
                }
            }
            long j = fileEntry.mRowId;
            if (MediaFile.isAudioFileType(this.mFileType) && (j == 0 || MediaScanner.this.mMtpObjectHandle != 0)) {
                values.put("is_ringtone", Boolean.valueOf(ringtones));
                values.put("is_notification", Boolean.valueOf(notifications));
                values.put("is_alarm", Boolean.valueOf(alarms));
                values.put("is_music", Boolean.valueOf(music));
                values.put("is_podcast", Boolean.valueOf(podcasts));
            } else if ((this.mFileType == 34 || this.mFileType == 40 || MediaFile.isRawImageFileType(this.mFileType)) && !this.mNoMedia) {
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(fileEntry.mPath);
                } catch (IOException e) {
                }
                if (exif != null) {
                    float[] latlng = new float[2];
                    boolean mHasLatLong = exif.getLatLong(latlng);
                    if (mHasLatLong) {
                        values.put("latitude", Float.valueOf(latlng[0]));
                        values.put("longitude", Float.valueOf(latlng[1]));
                    }
                    long time = exif.getGpsDateTime();
                    if (time == -1 || !mHasLatLong) {
                        time = exif.getDateTime();
                        if (time != -1) {
                            if (Math.abs((this.mLastModified * 1000) - time) >= AlarmManager.INTERVAL_DAY) {
                                values.put("datetaken", Long.valueOf(time));
                            }
                        }
                    } else {
                        values.put("datetaken", Long.valueOf(time));
                        boolean z = mHasLatLong;
                    }
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                    if (orientation != -1) {
                        int degree;
                        if (orientation == 3) {
                            degree = 180;
                        } else if (orientation == 6) {
                            degree = 90;
                        } else if (orientation != 8) {
                            degree = 0;
                        } else {
                            degree = 270;
                        }
                        values.put("orientation", Integer.valueOf(degree));
                    }
                    scannerSpecialImageType(values, exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION));
                }
                HwFrameworkFactory.getHwMediaScannerManager().initializeHwVoiceAndFocus(fileEntry.mPath, values);
            }
            MediaScanner.this.updateValues(fileEntry.mPath, values);
            Uri tableUri = MediaScanner.this.mFilesUri;
            MediaInserter inserter = MediaScanner.this.mMediaInserter;
            if (!(!this.mScanSuccess || this.mNoMedia || (MediaScanner.this.mBlackListFlag && MediaScanner.this.mIsImageType))) {
                if (MediaFile.isVideoFileType(this.mFileType)) {
                    tableUri = MediaScanner.this.mVideoUri;
                } else if (MediaFile.isImageFileType(this.mFileType)) {
                    tableUri = MediaScanner.this.mImagesUri;
                } else if (MediaFile.isAudioFileType(this.mFileType)) {
                    tableUri = MediaScanner.this.mAudioUri;
                }
            }
            boolean needToSetSettings3 = false;
            Uri result3;
            if (!notifications || MediaScanner.this.mDefaultNotificationSet) {
                result3 = null;
                if (ringtones) {
                    if ((!MediaScanner.this.mDefaultRingtoneSet && TextUtils.isEmpty(MediaScanner.this.mDefaultRingtoneFilename)) || doesPathHaveFilename(fileEntry.mPath, MediaScanner.this.mDefaultRingtoneFilename)) {
                        needToSetSettings3 = true;
                    }
                    needToSetSettings2 = HwFrameworkFactory.getHwMediaScannerManager().hwNeedSetSettings(fileEntry.mPath);
                    needToSetSettings = needToSetSettings3;
                    if (j != 0) {
                        if (MediaScanner.this.mMtpObjectHandle != 0) {
                            values.put("media_scanner_new_object_id", Integer.valueOf(MediaScanner.this.mMtpObjectHandle));
                        }
                        if (tableUri == MediaScanner.this.mFilesUri) {
                            int format = fileEntry.mFormat;
                            if (format == 0) {
                                format = MediaFile.getFormatCode(fileEntry.mPath, this.mMimeType);
                            }
                            values.put("format", Integer.valueOf(format));
                        }
                        if (MediaScanner.this.mBlackListFlag && MediaScanner.this.mIsImageType) {
                            values.put(DownloadManager.COLUMN_MEDIA_TYPE, Integer.valueOf(10));
                        }
                        if (inserter == null || needToSetSettings || needToSetSettings2) {
                            if (inserter != null) {
                                inserter.flushAll();
                            }
                            result = MediaScanner.this.mMediaProvider.insert(tableUri, values);
                        } else {
                            if (fileEntry.mFormat == MtpConstants.FORMAT_ASSOCIATION) {
                                inserter.insertwithPriority(tableUri, values);
                            } else {
                                inserter.insert(tableUri, values);
                            }
                            result = result3;
                        }
                        if (result != null) {
                            long rowId2 = ContentUris.parseId(result);
                            fileEntry.mRowId = rowId2;
                            result2 = result;
                            rowId = rowId2;
                            if (needToSetSettings) {
                                if (notifications) {
                                    setRingtoneIfNotSet("notification_sound", tableUri, rowId);
                                    MediaScanner.this.mDefaultNotificationSet = true;
                                } else if (ringtones) {
                                    setRingtoneIfNotSet("ringtone", tableUri, rowId);
                                    MediaScanner.this.mDefaultRingtoneSet = true;
                                } else if (alarms) {
                                    setRingtoneIfNotSet("alarm_alert", tableUri, rowId);
                                    MediaScanner.this.mDefaultAlarmSet = true;
                                }
                            }
                            HwFrameworkFactory.getHwMediaScannerManager().hwSetRingtone2Settings(needToSetSettings2, ringtones, tableUri, rowId, MediaScanner.this.mContext);
                            return result2;
                        }
                    }
                    result = ContentUris.withAppendedId(tableUri, j);
                    values.remove("_data");
                    rowId = null;
                    if (MediaScanner.this.mBlackListFlag && MediaScanner.this.mIsImageType) {
                        values.put(DownloadManager.COLUMN_MEDIA_TYPE, Integer.valueOf(10));
                    } else if (this.mScanSuccess && !MediaScanner.isNoMediaPath(fileEntry.mPath)) {
                        int fileType = MediaFile.getFileTypeForMimeType(this.mMimeType);
                        if (MediaFile.isAudioFileType(fileType)) {
                            rowId = 2;
                        } else if (MediaFile.isVideoFileType(fileType)) {
                            rowId = 3;
                        } else if (MediaFile.isImageFileType(fileType)) {
                            rowId = 1;
                        } else if (MediaFile.isPlayListFileType(fileType)) {
                            rowId = 4;
                        }
                        values.put(DownloadManager.COLUMN_MEDIA_TYPE, Integer.valueOf(rowId));
                    }
                    MediaScanner.this.mMediaProvider.update(result, values, null, null);
                    result2 = result;
                    rowId = j;
                    if (needToSetSettings) {
                    }
                    HwFrameworkFactory.getHwMediaScannerManager().hwSetRingtone2Settings(needToSetSettings2, ringtones, tableUri, rowId, MediaScanner.this.mContext);
                    return result2;
                } else if (alarms && !MediaScanner.this.mDefaultAlarmSet && (TextUtils.isEmpty(MediaScanner.this.mDefaultAlarmAlertFilename) || doesPathHaveFilename(fileEntry.mPath, MediaScanner.this.mDefaultAlarmAlertFilename))) {
                    needToSetSettings3 = true;
                }
            } else {
                if (TextUtils.isEmpty(MediaScanner.this.mDefaultNotificationFilename)) {
                    result3 = null;
                } else {
                    result3 = null;
                }
                needToSetSettings3 = true;
            }
            needToSetSettings = needToSetSettings3;
            needToSetSettings2 = false;
            if (j != 0) {
            }
            result2 = result;
            rowId = j;
            if (needToSetSettings) {
            }
            HwFrameworkFactory.getHwMediaScannerManager().hwSetRingtone2Settings(needToSetSettings2, ringtones, tableUri, rowId, MediaScanner.this.mContext);
            return result2;
        }

        private void scannerSpecialImageType(ContentValues values, String exifDescription) {
            int hdrType = 0;
            if ("hdr".equals(exifDescription)) {
                hdrType = 1;
            } else if (MediaScanner.IMAGE_TYPE_JHDR.equals(exifDescription)) {
                hdrType = 2;
            }
            values.put("is_hdr", Integer.valueOf(hdrType));
            if (exifDescription != null && exifDescription.length() >= 3) {
                String subString = exifDescription.substring(null, 3);
                if (MediaScanner.IMAGE_TYPE_PORTRAIT_FRONT.equals(subString)) {
                    values.put(MediaScanner.HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, Integer.valueOf(30));
                } else if (MediaScanner.IMAGE_TYPE_PORTRAIT_REAR.equals(subString)) {
                    values.put(MediaScanner.HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, Integer.valueOf(31));
                } else if (MediaScanner.IMAGE_TYPE_BEAUTY_FRONT.equals(subString)) {
                    values.put(MediaScanner.HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, Integer.valueOf(40));
                } else if (MediaScanner.IMAGE_TYPE_BEAUTY_REAR.equals(subString)) {
                    values.put(MediaScanner.HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, Integer.valueOf(41));
                }
            }
        }

        private boolean doesPathHaveFilename(String path, String filename) {
            int pathFilenameStart = path.lastIndexOf(File.separatorChar) + 1;
            int filenameLength = filename.length();
            if (path.regionMatches(pathFilenameStart, filename, 0, filenameLength) && pathFilenameStart + filenameLength == path.length()) {
                return true;
            }
            return false;
        }

        private void setRingtoneIfNotSet(String settingName, Uri uri, long rowId) {
            String str = MediaScanner.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setRingtoneIfNotSet.name:");
            stringBuilder.append(settingName);
            stringBuilder.append(" value:");
            stringBuilder.append(uri);
            stringBuilder.append(rowId);
            Log.v(str, stringBuilder.toString());
            if (!MediaScanner.this.wasRingtoneAlreadySet(settingName)) {
                ContentResolver cr = MediaScanner.this.mContext.getContentResolver();
                if (TextUtils.isEmpty(System.getString(cr, settingName))) {
                    Log.v(MediaScanner.TAG, "setSetting when NotSet");
                    Uri settingUri = System.getUriFor(settingName);
                    RingtoneManager.setActualDefaultRingtoneUri(MediaScanner.this.mContext, RingtoneManager.getDefaultType(settingUri), ContentUris.withAppendedId(uri, rowId));
                }
                System.putInt(cr, MediaScanner.this.settingSetIndicatorName(settingName), 1);
            }
        }

        private int getFileTypeFromDrm(String path) {
            if (!MediaScanner.this.isDrmEnabled()) {
                return 0;
            }
            int resultFileType = 0;
            if (MediaScanner.this.mDrmManagerClient == null) {
                MediaScanner.this.mDrmManagerClient = new DrmManagerClient(MediaScanner.this.mContext);
            }
            if (MediaScanner.this.mDrmManagerClient.canHandle(path, null)) {
                this.mIsDrm = true;
                String drmMimetype = MediaScanner.this.mDrmManagerClient.getOriginalMimeType(path);
                if (drmMimetype != null) {
                    this.mMimeType = drmMimetype;
                    resultFileType = MediaFile.getFileTypeForMimeType(drmMimetype);
                }
            }
            return resultFileType;
        }
    }

    class WplHandler implements ElementListener {
        final ContentHandler handler;
        String playListDirectory;

        public WplHandler(String playListDirectory, Uri uri, Cursor fileList) {
            this.playListDirectory = playListDirectory;
            RootElement root = new RootElement("smil");
            root.getChild(TtmlUtils.TAG_BODY).getChild("seq").getChild("media").setElementListener(this);
            this.handler = root.getContentHandler();
        }

        public void start(Attributes attributes) {
            String path = attributes.getValue("", "src");
            if (path != null) {
                MediaScanner.this.cachePlaylistEntry(path, this.playListDirectory);
            }
        }

        public void end() {
        }

        ContentHandler getContentHandler() {
            return this.handler;
        }
    }

    private final native void native_finalize();

    private static final native void native_init();

    private final native void native_setup();

    private native void processDirectory(String str, MediaScannerClient mediaScannerClient);

    private native boolean processFile(String str, String str2, MediaScannerClient mediaScannerClient);

    private static native void releaseBlackList();

    private static native void setBlackList(String str);

    private native void setLocale(String str);

    public static native void setStorageEjectFlag(boolean z);

    public native void addSkipCustomDirectory(String str, int i);

    public native void clearSkipCustomDirectory();

    public native byte[] extractAlbumArt(FileDescriptor fileDescriptor);

    public native void setExteLen(int i);

    static /* synthetic */ long access$904(MediaScanner x0) {
        long j = x0.mScanDirectoryFilesNum + 1;
        x0.mScanDirectoryFilesNum = j;
        return j;
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }

    public MediaScanner(Context c, String volumeName) {
        native_setup();
        this.mContext = c;
        this.mPackageName = c.getPackageName();
        this.mVolumeName = volumeName;
        this.mBitmapOptions.inSampleSize = 1;
        this.mBitmapOptions.inJustDecodeBounds = true;
        setDefaultRingtoneFileNames();
        this.mMediaProvider = this.mContext.getContentResolver().acquireContentProviderClient("media");
        if (sLastInternalScanFingerprint == null) {
            sLastInternalScanFingerprint = this.mContext.getSharedPreferences(SCANNED_BUILD_PREFS_NAME, 0).getString(LAST_INTERNAL_SCAN_FINGERPRINT, new String());
        }
        this.mAudioUri = Media.getContentUri(volumeName);
        this.mVideoUri = Video.Media.getContentUri(volumeName);
        this.mImagesUri = Images.Media.getContentUri(volumeName);
        this.mFilesUri = Files.getContentUri(volumeName);
        this.mFilesUriNoNotify = this.mFilesUri.buildUpon().appendQueryParameter("nonotify", SQL_VALUE_EXIF_FLAG).build();
        if (volumeName.equals(INTERNAL_VOLUME)) {
            this.mProcessPlaylists = false;
            this.mProcessGenres = false;
            this.mPlaylistsUri = null;
            this.mNeedFilter = HwFrameworkFactory.getHwMediaScannerManager().loadAudioFilterConfig(this.mContext);
        } else {
            this.mProcessPlaylists = true;
            this.mProcessGenres = true;
            this.mPlaylistsUri = Playlists.getContentUri(volumeName);
            this.mExtStroagePath = HwFrameworkFactory.getHwMediaScannerManager().getExtSdcardVolumePath(this.mContext);
            this.mSkipExternelQuery = HwFrameworkFactory.getHwMediaScannerManager().isSkipExtSdcard(this.mMediaProvider, this.mExtStroagePath, this.mPackageName, this.mFilesUriNoNotify);
            this.mNeedFilter = false;
        }
        Locale locale = this.mContext.getResources().getConfiguration().locale;
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language != null) {
                if (country != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(language);
                    stringBuilder.append("_");
                    stringBuilder.append(country);
                    setLocale(stringBuilder.toString());
                } else {
                    setLocale(language);
                }
            }
        }
        setStorageEjectFlag(false);
        this.mCloseGuard.open("close");
    }

    private void setDefaultRingtoneFileNames() {
        this.mDefaultRingtoneFilename = SystemProperties.get("ro.config.ringtone");
        HwFrameworkFactory.getHwMediaScannerManager().setHwDefaultRingtoneFileNames();
        this.mDefaultNotificationFilename = SystemProperties.get("ro.config.notification_sound");
        this.mDefaultAlarmAlertFilename = SystemProperties.get("ro.config.alarm_alert");
    }

    private boolean isDrmEnabled() {
        String prop = SystemProperties.get("drm.service.enabled");
        return prop != null && prop.equals("true");
    }

    public void setIsPrescanFiles(boolean prescanFiles) {
        this.mIsPrescanFiles = prescanFiles;
    }

    public boolean getIsPrescanFiles() {
        return this.mIsPrescanFiles;
    }

    public static void updateBlackList(Set blackLists) {
        sBlackList.clear();
        releaseBlackList();
        for (String black : blackLists) {
            if (!(black == null || black.length() == 0)) {
                setBlackList(black);
                sBlackList.add(black);
            }
        }
    }

    public boolean startWithIgnoreCase(String src, String sub) {
        if (sub.length() > src.length()) {
            return false;
        }
        return src.substring(0, sub.length()).equalsIgnoreCase(sub);
    }

    /* JADX WARNING: Missing block: B:17:0x0035, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isBlackListPath(String path, int exterlen) {
        if (path == null || sBlackList.size() == 0 || exterlen == 0 || path.length() <= exterlen) {
            return false;
        }
        String subPath = path.substring(exterlen);
        for (String itp : sBlackList) {
            if (startWithIgnoreCase(subPath, itp)) {
                return true;
            }
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:121:0x0224 A:{Catch:{ SQLException -> 0x0225, OperationApplicationException -> 0x0219, RemoteException -> 0x0209, all -> 0x0204, all -> 0x0254 }} */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x0224 A:{Catch:{ SQLException -> 0x0225, OperationApplicationException -> 0x0219, RemoteException -> 0x0209, all -> 0x0204, all -> 0x0254 }} */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x0224 A:{Catch:{ SQLException -> 0x0225, OperationApplicationException -> 0x0219, RemoteException -> 0x0209, all -> 0x0204, all -> 0x0254 }} */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x0224 A:{Catch:{ SQLException -> 0x0225, OperationApplicationException -> 0x0219, RemoteException -> 0x0209, all -> 0x0204, all -> 0x0254 }} */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x0224 A:{Catch:{ SQLException -> 0x0225, OperationApplicationException -> 0x0219, RemoteException -> 0x0209, all -> 0x0204, all -> 0x0254 }} */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x0224 A:{Catch:{ SQLException -> 0x0225, OperationApplicationException -> 0x0219, RemoteException -> 0x0209, all -> 0x0204, all -> 0x0254 }} */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x0224 A:{Catch:{ SQLException -> 0x0225, OperationApplicationException -> 0x0219, RemoteException -> 0x0209, all -> 0x0204, all -> 0x0254 }} */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x0259  */
    /* JADX WARNING: Missing block: B:114:0x0213, code skipped:
            if (r4 != null) goto L_0x0215;
     */
    /* JADX WARNING: Missing block: B:125:0x022f, code skipped:
            if (r4 == null) goto L_0x0232;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateBlackListFile() {
        StringBuilder stringBuilder;
        int count;
        String[] strArr;
        ArrayList<ContentProviderOperation> where;
        long end;
        String str;
        StringBuilder stringBuilder2;
        Throwable th;
        Cursor cursor;
        int fileCount;
        String where2 = "_id>? and (media_type=10 or media_type=1)";
        Cursor cursor2 = null;
        String[] selectionArgs = new String[]{"0"};
        int count2 = 0;
        Uri limitUri = this.mFilesUri.buildUpon().appendQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT, SQL_QUERY_LIMIT).build();
        long start = System.currentTimeMillis();
        ArrayList<ContentProviderOperation> ops = new ArrayList();
        Uri updateRowId = null;
        long lastId = Long.MIN_VALUE;
        int fileCount2 = 0;
        while (count2 < 100) {
            String str2;
            String str3;
            int i;
            String[] strArr2;
            try {
                stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(lastId);
                selectionArgs[0] = stringBuilder.toString();
                count = count2 + 1;
                if (cursor2 != null) {
                    try {
                        cursor2.close();
                        cursor2 = null;
                    } catch (SQLException e) {
                        str2 = where2;
                        strArr = selectionArgs;
                        count2 = count;
                        where = ops;
                        Log.e(TAG, "updateBlackListFile SQLException ! ");
                    } catch (OperationApplicationException e2) {
                        str2 = where2;
                        strArr = selectionArgs;
                        count2 = count;
                        where = ops;
                        Log.e(TAG, "MediaProvider upate all file Exception when use the applyBatch ! ");
                        if (cursor2 != null) {
                        }
                        end = System.currentTimeMillis();
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateBlackListFile total time = ");
                        stringBuilder2.append(end - start);
                        Log.d(str, stringBuilder2.toString());
                    } catch (RemoteException e3) {
                        str2 = where2;
                        strArr = selectionArgs;
                        count2 = count;
                        where = ops;
                        Log.e(TAG, "updateBlackListFile RemoteException ! ");
                    } catch (Throwable th2) {
                        th = th2;
                        str2 = where2;
                        strArr = selectionArgs;
                        where = ops;
                        if (cursor2 != null) {
                        }
                        throw th;
                    }
                }
                cursor = cursor2;
                try {
                    str3 = where2;
                    i = 0;
                    strArr2 = selectionArgs;
                    str2 = where2;
                    where = ops;
                    strArr = selectionArgs;
                    fileCount = fileCount2;
                } catch (SQLException e4) {
                    str2 = where2;
                    strArr = selectionArgs;
                    where = ops;
                    selectionArgs = fileCount2;
                    count2 = count;
                    cursor2 = cursor;
                    Log.e(TAG, "updateBlackListFile SQLException ! ");
                } catch (OperationApplicationException e5) {
                    str2 = where2;
                    strArr = selectionArgs;
                    where = ops;
                    selectionArgs = fileCount2;
                    count2 = count;
                    cursor2 = cursor;
                    Log.e(TAG, "MediaProvider upate all file Exception when use the applyBatch ! ");
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                    end = System.currentTimeMillis();
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("updateBlackListFile total time = ");
                    stringBuilder2.append(end - start);
                    Log.d(str, stringBuilder2.toString());
                } catch (RemoteException e6) {
                    str2 = where2;
                    strArr = selectionArgs;
                    where = ops;
                    selectionArgs = fileCount2;
                    count2 = count;
                    cursor2 = cursor;
                    Log.e(TAG, "updateBlackListFile RemoteException ! ");
                } catch (Throwable th3) {
                    th = th3;
                    str2 = where2;
                    strArr = selectionArgs;
                    where = ops;
                    selectionArgs = fileCount2;
                    cursor2 = cursor;
                    if (cursor2 != null) {
                        cursor2.close();
                    }
                    throw th;
                }
            } catch (SQLException e7) {
                str2 = where2;
                strArr = selectionArgs;
                where = ops;
                selectionArgs = fileCount2;
                Log.e(TAG, "updateBlackListFile SQLException ! ");
            } catch (OperationApplicationException e8) {
                str2 = where2;
                strArr = selectionArgs;
                where = ops;
                selectionArgs = fileCount2;
                Log.e(TAG, "MediaProvider upate all file Exception when use the applyBatch ! ");
                if (cursor2 != null) {
                }
                end = System.currentTimeMillis();
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateBlackListFile total time = ");
                stringBuilder2.append(end - start);
                Log.d(str, stringBuilder2.toString());
            } catch (RemoteException e9) {
                str2 = where2;
                strArr = selectionArgs;
                where = ops;
                selectionArgs = fileCount2;
                Log.e(TAG, "updateBlackListFile RemoteException ! ");
            } catch (Throwable th4) {
                th = th4;
                str2 = where2;
                strArr = selectionArgs;
                where = ops;
                selectionArgs = fileCount2;
                count = count2;
                if (cursor2 != null) {
                }
                throw th;
            }
            try {
                cursor2 = this.mMediaProvider.query(limitUri, FILES_BLACKLIST_PROJECTION_MEDIA, str3, strArr2, DownloadManager.COLUMN_ID, null);
                if (cursor2 != null) {
                    try {
                        int num = cursor2.getCount();
                        if (num != 0) {
                            fileCount2 = fileCount;
                            while (cursor2.moveToNext()) {
                                try {
                                    long rowId = cursor2.getLong(i);
                                    String path = cursor2.getString(1);
                                    selectionArgs = cursor2.getInt(2);
                                    lastId = rowId;
                                    fileCount2++;
                                    ContentValues values = new ContentValues();
                                    int len = getRootDirLength(path);
                                    cursor = isBlackListPath(path, len);
                                    int num2 = num;
                                    Uri updateRowId2;
                                    if (cursor == null || selectionArgs != 1) {
                                        int i2 = len;
                                        if (cursor == null && selectionArgs == 10) {
                                            values.put(DownloadManager.COLUMN_MEDIA_TYPE, Integer.valueOf(1));
                                            updateRowId2 = ContentUris.withAppendedId(this.mImagesUri, rowId);
                                            where.add(ContentProviderOperation.newUpdate(updateRowId2).withValues(values).build());
                                        }
                                        num = num2;
                                        i = 0;
                                    } else {
                                        values.put(DownloadManager.COLUMN_MEDIA_TYPE, Integer.valueOf(10));
                                        updateRowId2 = ContentUris.withAppendedId(this.mImagesUri, rowId);
                                        try {
                                            where.add(ContentProviderOperation.newUpdate(updateRowId2).withValues(values).build());
                                        } catch (SQLException e10) {
                                            updateRowId = updateRowId2;
                                        } catch (OperationApplicationException e11) {
                                            updateRowId = updateRowId2;
                                            count2 = count;
                                            Log.e(TAG, "MediaProvider upate all file Exception when use the applyBatch ! ");
                                            if (cursor2 != null) {
                                            }
                                            end = System.currentTimeMillis();
                                            str = TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("updateBlackListFile total time = ");
                                            stringBuilder2.append(end - start);
                                            Log.d(str, stringBuilder2.toString());
                                        } catch (RemoteException e12) {
                                            updateRowId = updateRowId2;
                                            count2 = count;
                                            Log.e(TAG, "updateBlackListFile RemoteException ! ");
                                        } catch (Throwable th5) {
                                            th = th5;
                                            updateRowId = updateRowId2;
                                            if (cursor2 != null) {
                                            }
                                            throw th;
                                        }
                                    }
                                    num = num2;
                                    i = 0;
                                } catch (SQLException e13) {
                                    count2 = count;
                                    Log.e(TAG, "updateBlackListFile SQLException ! ");
                                } catch (OperationApplicationException e14) {
                                    count2 = count;
                                    Log.e(TAG, "MediaProvider upate all file Exception when use the applyBatch ! ");
                                    if (cursor2 != null) {
                                    }
                                    end = System.currentTimeMillis();
                                    str = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("updateBlackListFile total time = ");
                                    stringBuilder2.append(end - start);
                                    Log.d(str, stringBuilder2.toString());
                                } catch (RemoteException e15) {
                                    count2 = count;
                                    Log.e(TAG, "updateBlackListFile RemoteException ! ");
                                } catch (Throwable th6) {
                                    th = th6;
                                    if (cursor2 != null) {
                                    }
                                    throw th;
                                }
                            }
                            ops = where;
                            count2 = count;
                            where2 = str2;
                            selectionArgs = strArr;
                        }
                    } catch (SQLException e16) {
                        fileCount2 = fileCount;
                        count2 = count;
                        Log.e(TAG, "updateBlackListFile SQLException ! ");
                    } catch (OperationApplicationException e17) {
                        fileCount2 = fileCount;
                        count2 = count;
                        Log.e(TAG, "MediaProvider upate all file Exception when use the applyBatch ! ");
                        if (cursor2 != null) {
                        }
                        end = System.currentTimeMillis();
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateBlackListFile total time = ");
                        stringBuilder2.append(end - start);
                        Log.d(str, stringBuilder2.toString());
                    } catch (RemoteException e18) {
                        fileCount2 = fileCount;
                        count2 = count;
                        Log.e(TAG, "updateBlackListFile RemoteException ! ");
                    } catch (Throwable th7) {
                        th = th7;
                        if (cursor2 != null) {
                        }
                        throw th;
                    }
                }
                count2 = count;
                break;
            } catch (SQLException e19) {
                fileCount2 = fileCount;
                count2 = count;
                cursor2 = cursor;
                Log.e(TAG, "updateBlackListFile SQLException ! ");
            } catch (OperationApplicationException e20) {
                fileCount2 = fileCount;
                count2 = count;
                cursor2 = cursor;
                Log.e(TAG, "MediaProvider upate all file Exception when use the applyBatch ! ");
                if (cursor2 != null) {
                }
                end = System.currentTimeMillis();
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateBlackListFile total time = ");
                stringBuilder2.append(end - start);
                Log.d(str, stringBuilder2.toString());
            } catch (RemoteException e21) {
                fileCount2 = fileCount;
                count2 = count;
                cursor2 = cursor;
                Log.e(TAG, "updateBlackListFile RemoteException ! ");
            } catch (Throwable th8) {
                th = th8;
                fileCount2 = fileCount;
                cursor2 = cursor;
                if (cursor2 != null) {
                }
                throw th;
            }
        }
        strArr = selectionArgs;
        where = ops;
        fileCount = fileCount2;
        try {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateBlackListFile filecount = ");
            stringBuilder.append(fileCount);
            Log.d(str, stringBuilder.toString());
            if (count2 == 100) {
                Log.d(TAG, "SQL query exceed the limit 100 !");
            }
            this.mMediaProvider.applyBatch(where);
            if (cursor2 != null) {
                cursor2.close();
            }
            fileCount2 = fileCount;
        } catch (SQLException e22) {
            fileCount2 = fileCount;
            Log.e(TAG, "updateBlackListFile SQLException ! ");
        } catch (OperationApplicationException e23) {
            fileCount2 = fileCount;
            Log.e(TAG, "MediaProvider upate all file Exception when use the applyBatch ! ");
            if (cursor2 != null) {
            }
            end = System.currentTimeMillis();
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateBlackListFile total time = ");
            stringBuilder2.append(end - start);
            Log.d(str, stringBuilder2.toString());
        } catch (RemoteException e24) {
            fileCount2 = fileCount;
            Log.e(TAG, "updateBlackListFile RemoteException ! ");
        } catch (Throwable th9) {
            th = th9;
            count = count2;
            if (cursor2 != null) {
            }
            throw th;
        }
        end = System.currentTimeMillis();
        str = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("updateBlackListFile total time = ");
        stringBuilder2.append(end - start);
        Log.d(str, stringBuilder2.toString());
    }

    private static boolean isSystemSoundWithMetadata(String path) {
        if (path.startsWith("/system/media/audio/alarms/") || path.startsWith("/system/media/audio/ringtones/") || path.startsWith("/system/media/audio/notifications/") || path.startsWith("/product/media/audio/alarms/") || path.startsWith("/product/media/audio/ringtones/") || path.startsWith("/product/media/audio/notifications/")) {
            return true;
        }
        return false;
    }

    private String settingSetIndicatorName(String base) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(base);
        stringBuilder.append("_set");
        return stringBuilder.toString();
    }

    private boolean wasRingtoneAlreadySet(String name) {
        boolean z = false;
        try {
            if (System.getInt(this.mContext.getContentResolver(), settingSetIndicatorName(name)) != 0) {
                z = true;
            }
            return z;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    /* JADX WARNING: Failed to extract finally block: empty outs */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void deleteFilesIfPossible() {
        String where = "_data is null and media_type != 4";
        Cursor c = null;
        try {
            c = this.mMediaProvider.query(this.mFilesUri, FILES_PRESCAN_PROJECTION, where, null, null, null);
            if (c != null && c.getCount() > 0) {
                this.mMediaProvider.delete(this.mFilesUri, where, null);
            }
            if (c == null) {
                return;
            }
        } catch (RemoteException e) {
            Log.d(TAG, "deleteFilesIfPossible catch RemoteException ");
            if (c == null) {
                return;
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
        c.close();
    }

    private void prescanOnlyMedia(Uri uriTable, boolean isOnlyMedia) throws RemoteException {
        hwPrescan(null, true, uriTable, isOnlyMedia);
    }

    /* JADX WARNING: Removed duplicated region for block: B:52:0x00e6  */
    /* JADX WARNING: Removed duplicated region for block: B:62:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x00e0  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00e6  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateExifFile() throws RemoteException {
        int count;
        Cursor c;
        Throwable th;
        Cursor c2;
        String where = "_id>? and cam_exif_flag is null";
        String[] selectionArgs = new String[]{"0"};
        Uri limitUri = this.mImagesUri.buildUpon().appendQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT, SQL_QUERY_LIMIT).build();
        int i = 0;
        Uri updateRowId = null;
        long lastId = Long.MIN_VALUE;
        int c3 = null;
        int count2 = 0;
        while (count2 < 100) {
            count = count2 + 1;
            try {
                Cursor c4;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(lastId);
                selectionArgs[i] = stringBuilder.toString();
                if (c3 != null) {
                    c3.close();
                    c4 = null;
                }
                c = c4;
            } catch (SQLException e) {
                try {
                    Log.e(TAG, "updateExifFile SQLException ! ");
                    if (c3 == 0) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (c3 != 0) {
                    }
                    throw th;
                }
            }
            try {
                c3 = this.mMediaProvider.query(limitUri, FILES_PRESCAN_PROJECTION_MEDIA, where, selectionArgs, DownloadManager.COLUMN_ID, null);
                if (c3 != null) {
                    if (c3.getCount() != 0) {
                        while (c3.moveToNext()) {
                            long rowId = c3.getLong(i);
                            lastId = rowId;
                            ContentValues values = new ContentValues();
                            values.put("cam_exif_flag", SQL_VALUE_EXIF_FLAG);
                            updateRowId = ContentUris.withAppendedId(this.mImagesUri, rowId);
                            ExifInterface exif = null;
                            try {
                                exif = new ExifInterface(c3.getString(1));
                            } catch (IOException e2) {
                                Log.e(TAG, "new ExifInterface Exception !");
                            }
                            HwFrameworkFactory.getHwMediaScannerManager().scanHwMakerNote(values, exif);
                            this.mMediaProvider.update(updateRowId, values, null, null);
                            i = 0;
                        }
                        count2 = count;
                        i = 0;
                    }
                }
                c2 = c3;
                c3 = count;
                break;
            } catch (SQLException e3) {
                c3 = c;
                Log.e(TAG, "updateExifFile SQLException ! ");
                if (c3 == 0) {
                    c3.close();
                    return;
                }
                return;
            } catch (Throwable th3) {
                th = th3;
                c3 = c;
                if (c3 != 0) {
                    c3.close();
                }
                throw th;
            }
        }
        c2 = c3;
        c3 = count2;
        if (c3 == 100) {
            try {
                Log.d(TAG, "SQL query exceed the limit 10 !");
            } catch (SQLException e4) {
                count = c3;
                c3 = c2;
            } catch (Throwable th4) {
                th = th4;
                count = c3;
                c3 = c2;
                if (c3 != 0) {
                }
                throw th;
            }
        }
        if (c2 != null) {
            c2.close();
        }
        count = c3;
    }

    /* JADX WARNING: Removed duplicated region for block: B:106:0x0260  */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x0260  */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x0260  */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x0260  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void hwPrescan(String filePath, boolean prescanFiles, Uri uriTable, boolean isOnlyMedia) throws RemoteException {
        String where;
        String where2;
        Throwable th;
        MediaBulkDeleter deleter;
        MediaBulkDeleter deleter2;
        Cursor c = null;
        this.mPlayLists.clear();
        if (this.mFileCache == null) {
            this.mFileCache = new HashMap();
        } else {
            this.mFileCache.clear();
        }
        int i = 2;
        int i2 = 1;
        int fileType = 0;
        if (filePath != null) {
            where = "_id>? AND _data=?";
            where2 = new String[]{"", filePath};
        } else {
            where = "_id>?";
            where2 = new String[]{""};
        }
        String[] selectionArgs = where2;
        where2 = where;
        Builder builder = uriTable.buildUpon();
        builder.appendQueryParameter("deletedata", "false");
        MediaBulkDeleter deleter3 = new MediaBulkDeleter(this.mMediaProvider, builder.build());
        Builder builder2;
        if (prescanFiles) {
            String str;
            try {
                Uri limitUri = uriTable.buildUpon().appendQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT, SQL_QUERY_LIMIT).build();
                deleteFilesIfPossible();
                int count = 0;
                long lastId = Long.MIN_VALUE;
                while (true) {
                    String lastFilePath = "";
                    long start = System.currentTimeMillis();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(lastId);
                    selectionArgs[fileType] = stringBuilder.toString();
                    if (c != null) {
                        try {
                            c.close();
                            c = null;
                        } catch (Throwable th2) {
                            th = th2;
                            str = where2;
                            deleter = deleter3;
                            builder2 = builder;
                        }
                    }
                    long lastId2 = lastId;
                    deleter2 = deleter3;
                    builder2 = builder;
                    try {
                        c = this.mMediaProvider.query(limitUri, isOnlyMedia ? FILES_PRESCAN_PROJECTION_MEDIA : FILES_PRESCAN_PROJECTION, where2, selectionArgs, DownloadManager.COLUMN_ID, null);
                    } catch (Throwable th3) {
                        th = th3;
                        str = where2;
                        deleter = deleter2;
                        if (c != null) {
                            c.close();
                        }
                        deleter.flush();
                        throw th;
                    }
                    Cursor c2;
                    try {
                        long query = System.currentTimeMillis();
                        if (c == null) {
                            break;
                        }
                        int num = c.getCount();
                        if (num == 0) {
                            break;
                        }
                        String path;
                        int i3;
                        Uri uri;
                        long lastModified = 0;
                        int count2 = count;
                        count = fileType;
                        int format = 0;
                        String lastFilePath2 = lastFilePath;
                        while (c.moveToNext()) {
                            long rowId = c.getLong(fileType);
                            String path2 = c.getString(i2);
                            if (isOnlyMedia) {
                                i = count;
                            } else {
                                try {
                                    format = c.getInt(i);
                                    lastModified = c.getLong(3);
                                    i = c.getInt(4);
                                } catch (Throwable th4) {
                                    th = th4;
                                    str = where2;
                                    deleter = deleter2;
                                    if (c != null) {
                                    }
                                    deleter.flush();
                                    throw th;
                                }
                            }
                            lastId2 = rowId;
                            path = path2;
                            if (path != null) {
                                if (path.startsWith("/")) {
                                    lastFilePath2 = path;
                                    boolean exists = fileType;
                                    try {
                                        exists = Os.access(path, OsConstants.F_OK);
                                    } catch (ErrnoException e) {
                                    }
                                    if (!exists) {
                                        if (!MtpConstants.isAbstractObject(format)) {
                                            MediaFileType mediaFileType = MediaFile.getFileType(path);
                                            if (mediaFileType != null) {
                                                fileType = mediaFileType.fileType;
                                            }
                                            if (!MediaFile.isPlayListFileType(fileType)) {
                                                c2 = c;
                                                str = where2;
                                                deleter = deleter2;
                                                try {
                                                    deleter.delete(rowId);
                                                    if (path.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                                                        deleter.flush();
                                                        i3 = num;
                                                        uri = limitUri;
                                                        this.mMediaProvider.call("unhide", new File(path).getParent(), null);
                                                    } else {
                                                        i3 = num;
                                                        uri = limitUri;
                                                    }
                                                    deleter2 = deleter;
                                                    count = i;
                                                    where2 = str;
                                                    c = c2;
                                                    num = i3;
                                                    limitUri = uri;
                                                    i = 2;
                                                    i2 = 1;
                                                    fileType = 0;
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                    c = c2;
                                                    if (c != null) {
                                                    }
                                                    deleter.flush();
                                                    throw th;
                                                }
                                            }
                                        }
                                    }
                                    c2 = c;
                                    str = where2;
                                    i3 = num;
                                    uri = limitUri;
                                    deleter = deleter2;
                                    c = rowId;
                                    if (this.mNeedFilter && HwFrameworkFactory.getHwMediaScannerManager().isAudioFilterFile(path)) {
                                        deleter.delete(c);
                                        deleter2 = deleter;
                                        count = i;
                                        where2 = str;
                                        c = c2;
                                        num = i3;
                                        limitUri = uri;
                                        i = 2;
                                        i2 = 1;
                                        fileType = 0;
                                    } else {
                                        if (!isOnlyMedia && count2 < MAX_ENTRY_SIZE) {
                                            this.mFileCache.put(path, new FileEntry(c, path, lastModified, format, i));
                                        }
                                        count2++;
                                        deleter2 = deleter;
                                        count = i;
                                        where2 = str;
                                        c = c2;
                                        num = i3;
                                        limitUri = uri;
                                        i = 2;
                                        i2 = 1;
                                        fileType = 0;
                                    }
                                }
                            }
                            c2 = c;
                            str = where2;
                            i3 = num;
                            uri = limitUri;
                            deleter = deleter2;
                            deleter2 = deleter;
                            count = i;
                            where2 = str;
                            c = c2;
                            num = i3;
                            limitUri = uri;
                            i = 2;
                            i2 = 1;
                            fileType = 0;
                        }
                        c2 = c;
                        str = where2;
                        i3 = num;
                        uri = limitUri;
                        deleter = deleter2;
                        long end = System.currentTimeMillis();
                        if (end - start > 20000) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("hwPrescan time: ");
                            stringBuilder2.append(end - start);
                            stringBuilder2.append(", query time: ");
                            stringBuilder2.append(query - start);
                            stringBuilder2.append(", process time: ");
                            stringBuilder2.append(end - query);
                            stringBuilder2.append(", count: ");
                            stringBuilder2.append(count2);
                            Log.d(str2, stringBuilder2.toString());
                            i = lastFilePath2.lastIndexOf(47);
                            if (i > 0) {
                                path = lastFilePath2.substring(0, i);
                                String str3 = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("hwPrescan filePath: ");
                                stringBuilder.append(path);
                                Log.d(str3, stringBuilder.toString());
                            }
                        }
                        count = count2;
                        builder = builder2;
                        lastId = lastId2;
                        where2 = str;
                        c = c2;
                        limitUri = uri;
                        i = 2;
                        i2 = 1;
                        fileType = 0;
                        deleter3 = deleter;
                    } catch (Throwable th6) {
                        th = th6;
                        c2 = c;
                        str = where2;
                        deleter = deleter2;
                        if (c != null) {
                        }
                        deleter.flush();
                        throw th;
                    }
                }
                str = where2;
                deleter = deleter2;
            } catch (Throwable th7) {
                th = th7;
                str = where2;
                deleter = deleter3;
                builder2 = builder;
                if (c != null) {
                }
                deleter.flush();
                throw th;
            }
        }
        deleter = deleter3;
        builder2 = builder;
        if (c != null) {
            c.close();
        }
        deleter.flush();
        if (isOnlyMedia) {
            this.mOriginalCount = -1;
            this.mDefaultRingtoneSet = true;
            this.mDefaultNotificationSet = true;
            this.mDefaultAlarmSet = true;
            return;
        }
        this.mDefaultRingtoneSet = wasRingtoneAlreadySet("ringtone");
        this.mDefaultNotificationSet = wasRingtoneAlreadySet("notification_sound");
        this.mDefaultAlarmSet = wasRingtoneAlreadySet("alarm_alert");
        this.mOriginalCount = 0;
        c = this.mMediaProvider.query(this.mImagesUri, new String[]{"COUNT(*)"}, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                this.mOriginalCount = c.getInt(0);
            }
            c.close();
        }
    }

    private void prescan(String filePath, boolean prescanFiles) throws RemoteException {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("prescan begin prescanFiles: ");
        stringBuilder.append(prescanFiles);
        Log.d(str, stringBuilder.toString());
        long start = System.currentTimeMillis();
        hwPrescan(filePath, prescanFiles, this.mFilesUri, false);
        long end = System.currentTimeMillis();
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("prescan end, time:  ");
        stringBuilder2.append(end - start);
        stringBuilder2.append("ms");
        Log.d(str2, stringBuilder2.toString());
    }

    public void postscan(String[] directories) throws RemoteException {
        if (this.mProcessPlaylists) {
            processPlayLists();
        }
        HwFrameworkFactory.getHwMediaScannerManager().pruneDeadThumbnailsFolder();
        this.mPlayLists.clear();
    }

    private void releaseResources() {
        if (this.mDrmManagerClient != null) {
            this.mDrmManagerClient.close();
            this.mDrmManagerClient = null;
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:44:0x0104=Splitter:B:44:0x0104, B:49:0x0114=Splitter:B:49:0x0114, B:54:0x0124=Splitter:B:54:0x0124} */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0144  */
    /* JADX WARNING: Missing block: B:33:0x00ee, code skipped:
            if (r1.mFileCache != null) goto L_0x0132;
     */
    /* JADX WARNING: Missing block: B:47:0x0110, code skipped:
            if (r1.mFileCache != null) goto L_0x0132;
     */
    /* JADX WARNING: Missing block: B:52:0x0120, code skipped:
            if (r1.mFileCache != null) goto L_0x0132;
     */
    /* JADX WARNING: Missing block: B:57:0x0130, code skipped:
            if (r1.mFileCache != null) goto L_0x0132;
     */
    /* JADX WARNING: Missing block: B:58:0x0132, code skipped:
            r1.mFileCache.clear();
            r1.mFileCache = null;
     */
    /* JADX WARNING: Missing block: B:59:0x0139, code skipped:
            r1.mSkipExternelQuery = false;
     */
    /* JADX WARNING: Missing block: B:60:0x013c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void scanDirectories(String[] directories) {
        SQLException e;
        UnsupportedOperationException e2;
        RemoteException e3;
        Throwable th;
        MediaScanner mediaScanner = this;
        String[] strArr = directories;
        try {
            int flag;
            boolean flag2 = false;
            for (StorageVolume storageVolume : StorageManager.getVolumeList(UserHandle.myUserId(), 256)) {
                String rootPath = storageVolume.getPath();
                for (String equalsIgnoreCase : strArr) {
                    if (rootPath.equalsIgnoreCase(equalsIgnoreCase)) {
                        flag2 = true;
                        Log.i(TAG, "MediaScanner scanDirectories flag = true means root dirs!");
                        break;
                    }
                }
                if (flag2) {
                    break;
                }
            }
            if (INTERNAL_VOLUME.equalsIgnoreCase(mediaScanner.mVolumeName)) {
                flag2 = true;
                Log.i(TAG, "MediaScanner scanDirectories flag = true means internal!");
            }
            long start = System.currentTimeMillis();
            if (flag2) {
                mediaScanner.prescan(null, mediaScanner.mIsPrescanFiles);
            } else {
                mediaScanner.prescanOnlyMedia(mediaScanner.mAudioUri, true);
                mediaScanner.prescanOnlyMedia(mediaScanner.mVideoUri, true);
                mediaScanner.prescanOnlyMedia(mediaScanner.mImagesUri, true);
            }
            long prescan = System.currentTimeMillis();
            mediaScanner.mMediaInserter = new MediaInserter(mediaScanner.mMediaProvider, RunningAppProcessInfo.IMPORTANCE_EMPTY);
            Log.d(TAG, "delete nomedia File when scanDirectories");
            mediaScanner.deleteNomediaFile(StorageManager.getVolumeList(UserHandle.myUserId(), 256));
            for (flag = 0; flag < strArr.length; flag++) {
                mediaScanner.setExteLen(mediaScanner.getRootDirLength(strArr[flag]));
                sCurScanDIR = strArr[flag];
                mediaScanner.processDirectory(strArr[flag], mediaScanner.mClient);
            }
            if (mediaScanner.mMediaTypeConflict) {
                Log.i(TAG, "find some files's media type did not match with database");
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("scanDirectories total files number is ");
            stringBuilder.append(mediaScanner.mScanDirectoryFilesNum);
            Log.d(str, stringBuilder.toString());
            mediaScanner.mMediaInserter.flushAll();
            mediaScanner.mMediaInserter = null;
            long scan = System.currentTimeMillis();
            postscan(directories);
            try {
                HwMediaMonitorManager.writeBigData(HwMediaMonitorUtils.BD_MEDIA_SCANNING, 0, (int) ((System.currentTimeMillis() - start) / 1000), 0);
                mediaScanner = this;
                releaseResources();
            } catch (SQLException e4) {
                e = e4;
                mediaScanner = this;
                Log.e(TAG, "SQLException in MediaScanner.scan()", e);
                releaseResources();
            } catch (UnsupportedOperationException e5) {
                e2 = e5;
                mediaScanner = this;
                Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e2);
                releaseResources();
            } catch (RemoteException e6) {
                e3 = e6;
                mediaScanner = this;
                try {
                    Log.e(TAG, "RemoteException in MediaScanner.scan()", e3);
                    releaseResources();
                } catch (Throwable th2) {
                    th = th2;
                    releaseResources();
                    if (mediaScanner.mFileCache != null) {
                        mediaScanner.mFileCache.clear();
                        mediaScanner.mFileCache = null;
                    }
                    mediaScanner.mSkipExternelQuery = false;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                mediaScanner = this;
                releaseResources();
                if (mediaScanner.mFileCache != null) {
                }
                mediaScanner.mSkipExternelQuery = false;
                throw th;
            }
        } catch (SQLException e7) {
            e = e7;
            Log.e(TAG, "SQLException in MediaScanner.scan()", e);
            releaseResources();
        } catch (UnsupportedOperationException e8) {
            e2 = e8;
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e2);
            releaseResources();
        } catch (RemoteException e9) {
            e3 = e9;
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e3);
            releaseResources();
        }
    }

    public void scanCustomDirectories(String[] directories, String volumeName, String[] whiteList, String[] blackList) {
        long start = System.currentTimeMillis();
        this.mMediaProvider = this.mContext.getContentResolver().acquireContentProviderClient("media");
        this.mMediaInserter = new MediaInserter(this.mMediaProvider, RunningAppProcessInfo.IMPORTANCE_EMPTY);
        HwFrameworkFactory.getHwMediaScannerManager().setMediaInserter(this.mMediaInserter);
        Log.d(TAG, "delete nomedia File when scanCustomDirectories");
        deleteNomediaFile(StorageManager.getVolumeList(UserHandle.myUserId(), 256));
        HwFrameworkFactory.getHwMediaScannerManager().scanCustomDirectories(this, this.mClient, directories, volumeName, whiteList, blackList);
        clearSkipCustomDirectory();
        if (this.mMediaTypeConflict) {
            Log.i(TAG, "find some files's media type did not match with database");
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("scanCustomDirectories total files number is ");
        stringBuilder.append(this.mScanDirectoryFilesNum);
        Log.d(str, stringBuilder.toString());
        if (this.mFileCache != null) {
            this.mFileCache.clear();
            this.mFileCache = null;
        }
        HwFrameworkFactory.getHwMediaScannerManager().setMediaInserter(null);
        this.mMediaInserter = null;
        this.mSkipExternelQuery = false;
        HwMediaMonitorManager.writeBigData(HwMediaMonitorUtils.BD_MEDIA_SCANNING, 0, (int) ((System.currentTimeMillis() - start) / 1000), 0);
    }

    public int getRootDirLength(String path) {
        if (path == null) {
            return 0;
        }
        for (StorageVolume storageVolume : StorageManager.getVolumeList(UserHandle.myUserId(), 256)) {
            String rootPath = storageVolume.getPath();
            if (path.startsWith(rootPath)) {
                return rootPath.length();
            }
        }
        return 0;
    }

    public Uri scanSingleFile(String path, String mimeType) {
        String str = path;
        try {
            prescan(str, true);
            this.mBlackListFlag = false;
            if (isBlackListPath(str, getRootDirLength(path))) {
                this.mBlackListFlag = true;
            }
            File file = new File(str);
            if (file.exists()) {
                if (file.canRead()) {
                    Log.d(TAG, "delete nomedia File when scanSingleFile");
                    deleteNomediaFile(StorageManager.getVolumeList(UserHandle.myUserId(), 256));
                    String str2 = str;
                    String str3 = mimeType;
                    Uri doScanFile = this.mClient.doScanFile(str2, str3, file.lastModified() / 1000, file.length(), false, true, isNoMediaPath(path));
                    releaseResources();
                    return doScanFile;
                }
            }
            releaseResources();
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
            releaseResources();
            return null;
        } catch (Throwable th) {
            releaseResources();
            throw th;
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0072, code skipped:
            if (r12.regionMatches(true, r1 + 1, "AlbumArtSmall", 0, 13) == false) goto L_0x0074;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean isNoMediaFile(String path) {
        if (new File(path).isDirectory()) {
            return false;
        }
        int lastSlash = path.lastIndexOf(47);
        if (lastSlash >= 0 && lastSlash + 2 < path.length()) {
            if (path.regionMatches(lastSlash + 1, "._", 0, 2)) {
                return true;
            }
            if (path.regionMatches(true, path.length() - 4, ".jpg", 0, 4)) {
                if (!path.regionMatches(true, lastSlash + 1, "AlbumArt_{", 0, 10)) {
                    if (!path.regionMatches(true, lastSlash + 1, "AlbumArt.", 0, 9)) {
                        int length = (path.length() - lastSlash) - 1;
                        if (length == 17) {
                        }
                        if (length == 10) {
                            if (path.regionMatches(true, lastSlash + 1, "Folder", 0, 6)) {
                                return true;
                            }
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static void clearMediaPathCache(boolean clearMediaPaths, boolean clearNoMediaPaths) {
        synchronized (MediaScanner.class) {
            if (clearMediaPaths) {
                try {
                    mMediaPaths.clear();
                } catch (Throwable th) {
                }
            }
            if (clearNoMediaPaths) {
                mNoMediaPaths.clear();
            }
        }
    }

    /* JADX WARNING: Missing block: B:31:0x0072, code skipped:
            return isNoMediaFile(r11);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isNoMediaPath(String path) {
        if (path == null) {
            return false;
        }
        if (path.indexOf("/.") >= 0) {
            return true;
        }
        int firstSlash = path.lastIndexOf(47);
        if (firstSlash <= 0) {
            return false;
        }
        String parent = path.substring(0, firstSlash);
        synchronized (MediaScanner.class) {
            if (mNoMediaPaths.containsKey(parent)) {
                return true;
            } else if (!mMediaPaths.containsKey(parent)) {
                int offset = 1;
                while (offset >= 0) {
                    int slashIndex = path.indexOf(47, offset);
                    if (slashIndex > offset) {
                        slashIndex++;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(path.substring(0, slashIndex));
                        stringBuilder.append(".nomedia");
                        if (new File(stringBuilder.toString()).exists()) {
                            mNoMediaPaths.put(parent, "");
                            return true;
                        }
                    }
                    offset = slashIndex;
                }
                mMediaPaths.put(parent, "");
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:68:0x0135  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0143  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x0135  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0143  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0143  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x0143  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void scanMtpFile(String path, int objectHandle, int format) {
        RemoteException e;
        long j;
        File file;
        Throwable fileList;
        String str = path;
        MediaFileType mediaFileType = MediaFile.getFileType(path);
        int fileType = mediaFileType == null ? 0 : mediaFileType.fileType;
        File file2 = new File(str);
        long lastModifiedSeconds = file2.lastModified() / 1000;
        if (MediaFile.isAudioFileType(fileType) || MediaFile.isVideoFileType(fileType) || MediaFile.isImageFileType(fileType) || MediaFile.isPlayListFileType(fileType) || MediaFile.isDrmFileType(fileType)) {
            this.mMtpObjectHandle = objectHandle;
            Cursor fileList2 = null;
            Cursor fileList3;
            try {
                if (MediaFile.isPlayListFileType(fileType)) {
                    try {
                        prescan(null, true);
                        FileEntry entry = (FileEntry) this.mFileCache.remove(str);
                        if (entry == null) {
                            entry = makeEntryFor(path);
                        }
                        if (entry != null) {
                            fileList3 = this.mMediaProvider.query(this.mFilesUri, FILES_PRESCAN_PROJECTION, null, null, null, null);
                            try {
                                processPlayList(entry, fileList3);
                                fileList2 = fileList3;
                            } catch (RemoteException e2) {
                                e = e2;
                                j = lastModifiedSeconds;
                                file = file2;
                                try {
                                    Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
                                    this.mMtpObjectHandle = 0;
                                    if (fileList3 != null) {
                                    }
                                    releaseResources();
                                    return;
                                } catch (Throwable th) {
                                    fileList = th;
                                    this.mMtpObjectHandle = 0;
                                    if (fileList3 != null) {
                                    }
                                    releaseResources();
                                    throw fileList;
                                }
                            } catch (Throwable th2) {
                                fileList = th2;
                                j = lastModifiedSeconds;
                                file = file2;
                                this.mMtpObjectHandle = 0;
                                if (fileList3 != null) {
                                }
                                releaseResources();
                                throw fileList;
                            }
                        }
                    } catch (RemoteException e3) {
                        e = e3;
                        j = lastModifiedSeconds;
                        file = file2;
                        fileList3 = fileList2;
                        Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
                        this.mMtpObjectHandle = 0;
                        if (fileList3 != null) {
                            fileList3.close();
                        }
                        releaseResources();
                        return;
                    } catch (Throwable th3) {
                        fileList = th3;
                        j = lastModifiedSeconds;
                        file = file2;
                        fileList3 = fileList2;
                        this.mMtpObjectHandle = 0;
                        if (fileList3 != null) {
                            fileList3.close();
                        }
                        releaseResources();
                        throw fileList;
                    }
                }
                prescan(str, false);
                j = lastModifiedSeconds;
                file = file2;
                try {
                    this.mClient.doScanFile(str, mediaFileType.mimeType, lastModifiedSeconds, file2.length(), format == 12289, true, isNoMediaPath(path));
                } catch (RemoteException e4) {
                    e = e4;
                } catch (Throwable th4) {
                    fileList = th4;
                    fileList3 = fileList2;
                    this.mMtpObjectHandle = 0;
                    if (fileList3 != null) {
                    }
                    releaseResources();
                    throw fileList;
                }
                Cursor fileList4 = fileList2;
                this.mMtpObjectHandle = 0;
                if (fileList4 != null) {
                    fileList4.close();
                }
                releaseResources();
            } catch (RemoteException e5) {
                e = e5;
                j = lastModifiedSeconds;
                file = file2;
                fileList3 = fileList2;
                Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
                this.mMtpObjectHandle = 0;
                if (fileList3 != null) {
                }
                releaseResources();
                return;
            } catch (Throwable th5) {
                fileList = th5;
                j = lastModifiedSeconds;
                file = file2;
                fileList3 = fileList2;
                this.mMtpObjectHandle = 0;
                if (fileList3 != null) {
                }
                releaseResources();
                throw fileList;
            }
            return;
        }
        ContentValues values = new ContentValues();
        values.put("_size", Long.valueOf(file2.length()));
        values.put("date_modified", Long.valueOf(lastModifiedSeconds));
        try {
            this.mMediaProvider.update(Files.getMtpObjectsUri(this.mVolumeName), values, "_id=?", new String[]{Integer.toString(objectHandle)});
        } catch (RemoteException e6) {
            Log.e(TAG, "RemoteException in scanMtpFile", e6);
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0040, code skipped:
            if (r3 != null) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:9:0x0042, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:15:0x004e, code skipped:
            if (r3 == null) goto L_0x0051;
     */
    /* JADX WARNING: Missing block: B:16:0x0051, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    FileEntry makeEntryFor(String path) {
        Cursor c = null;
        try {
            String[] selectionArgs = new String[]{path};
            c = this.mMediaProvider.query(this.mFilesUriNoNotify, FILES_PRESCAN_PROJECTION, "_data=?", selectionArgs, null, null);
            if (c.moveToFirst()) {
                String str = path;
                FileEntry fileEntry = new FileEntry(c.getLong(0), str, c.getLong(3), c.getInt(2), c.getInt(4));
                if (c != null) {
                    c.close();
                }
                return fileEntry;
            }
        } catch (RemoteException e) {
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    private int matchPaths(String path1, String path2) {
        String str = path1;
        String str2 = path2;
        int end1 = path1.length();
        int end2 = path2.length();
        int result = 0;
        int end12 = end1;
        while (true) {
            int end22 = end2;
            if (end12 <= 0 || end22 <= 0) {
                break;
            }
            int start2;
            int slash1 = str.lastIndexOf(47, end12 - 1);
            int slash2 = str2.lastIndexOf(47, end22 - 1);
            int backSlash1 = str.lastIndexOf(92, end12 - 1);
            int backSlash2 = str2.lastIndexOf(92, end22 - 1);
            int start1 = slash1 > backSlash1 ? slash1 : backSlash1;
            end1 = slash2 > backSlash2 ? slash2 : backSlash2;
            int start12 = start1 < 0 ? 0 : start1 + 1;
            if (end1 < 0) {
                start2 = 0;
            } else {
                start2 = end1 + 1;
            }
            int length = end12 - start12;
            if (end22 - start2 == length) {
                if (!str.regionMatches(true, start12, str2, start2, length)) {
                    break;
                }
                result++;
                end12 = start12 - 1;
                end2 = start2 - 1;
            } else {
                break;
            }
        }
        return result;
    }

    private boolean matchEntries(long rowId, String data) {
        int len = this.mPlaylistEntries.size();
        boolean done = true;
        for (int i = 0; i < len; i++) {
            PlaylistEntry entry = (PlaylistEntry) this.mPlaylistEntries.get(i);
            if (entry.bestmatchlevel != Integer.MAX_VALUE) {
                done = false;
                if (data.equalsIgnoreCase(entry.path)) {
                    entry.bestmatchid = rowId;
                    entry.bestmatchlevel = Integer.MAX_VALUE;
                } else {
                    int matchLength = matchPaths(data, entry.path);
                    if (matchLength > entry.bestmatchlevel) {
                        entry.bestmatchid = rowId;
                        entry.bestmatchlevel = matchLength;
                    }
                }
            }
        }
        return done;
    }

    private void cachePlaylistEntry(String line, String playListDirectory) {
        PlaylistEntry entry = new PlaylistEntry();
        int entryLength = line.length();
        while (entryLength > 0 && Character.isWhitespace(line.charAt(entryLength - 1))) {
            entryLength--;
        }
        if (entryLength >= 3) {
            boolean fullPath = false;
            if (entryLength < line.length()) {
                line = line.substring(0, entryLength);
            }
            char ch1 = line.charAt(0);
            if (ch1 == '/' || (Character.isLetter(ch1) && line.charAt(1) == ':' && line.charAt(2) == '\\')) {
                fullPath = true;
            }
            if (!fullPath) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(playListDirectory);
                stringBuilder.append(line);
                line = stringBuilder.toString();
            }
            entry.path = line;
            this.mPlaylistEntries.add(entry);
        }
    }

    private void processCachedPlaylist(Cursor fileList, ContentValues values, Uri playlistUri) {
        int i;
        int len;
        int index;
        fileList.moveToPosition(-1);
        while (true) {
            i = 0;
            if (!fileList.moveToNext() || matchEntries(fileList.getLong(0), fileList.getString(1))) {
                len = this.mPlaylistEntries.size();
                index = 0;
            }
        }
        len = this.mPlaylistEntries.size();
        index = 0;
        while (i < len) {
            PlaylistEntry entry = (PlaylistEntry) this.mPlaylistEntries.get(i);
            if (entry.bestmatchlevel > 0) {
                try {
                    values.clear();
                    values.put("play_order", Integer.valueOf(index));
                    values.put("audio_id", Long.valueOf(entry.bestmatchid));
                    this.mMediaProvider.insert(playlistUri, values);
                    index++;
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in MediaScanner.processCachedPlaylist()", e);
                    return;
                }
            }
            i++;
        }
        this.mPlaylistEntries.clear();
    }

    private void processM3uPlayList(String path, String playListDirectory, Uri uri, ContentValues values, Cursor fileList) {
        BufferedReader reader = null;
        try {
            File f = new File(path);
            if (f.exists()) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)), 8192);
                String line = reader.readLine();
                this.mPlaylistEntries.clear();
                while (line != null) {
                    if (line.length() > 0 && line.charAt(0) != '#') {
                        cachePlaylistEntry(line, playListDirectory);
                    }
                    line = reader.readLine();
                }
                processCachedPlaylist(fileList, values, uri);
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e);
                }
            }
        } catch (IOException e2) {
            Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e2);
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e3);
                }
            }
        }
    }

    private void processPlsPlayList(String path, String playListDirectory, Uri uri, ContentValues values, Cursor fileList) {
        BufferedReader reader = null;
        try {
            File f = new File(path);
            if (f.exists()) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)), 8192);
                this.mPlaylistEntries.clear();
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.startsWith("File")) {
                        int equals = line.indexOf(61);
                        if (equals > 0) {
                            cachePlaylistEntry(line.substring(equals + 1), playListDirectory);
                        }
                    }
                }
                processCachedPlaylist(fileList, values, uri);
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e);
                }
            }
        } catch (IOException e2) {
            Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e2);
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e3);
                }
            }
        }
    }

    private void processWplPlayList(String path, String playListDirectory, Uri uri, ContentValues values, Cursor fileList) {
        FileInputStream fis = null;
        try {
            File f = new File(path);
            if (f.exists()) {
                fis = new FileInputStream(f);
                this.mPlaylistEntries.clear();
                Xml.parse(fis, Xml.findEncodingByName("UTF-8"), new WplHandler(playListDirectory, uri, fileList).getContentHandler());
                processCachedPlaylist(fileList, values, uri);
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e);
                }
            }
        } catch (SAXException e2) {
            e2.printStackTrace();
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e3) {
            e3.printStackTrace();
            if (fis != null) {
                fis.close();
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e4) {
                    Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e4);
                }
            }
        }
    }

    private void processPlayList(FileEntry entry, Cursor fileList) throws RemoteException {
        FileEntry fileEntry = entry;
        String path = fileEntry.mPath;
        ContentValues values = new ContentValues();
        int lastSlash = path.lastIndexOf(47);
        if (lastSlash >= 0) {
            int lastDot;
            Uri membersUri;
            long rowId = fileEntry.mRowId;
            String name = values.getAsString(MidiDeviceInfo.PROPERTY_NAME);
            if (name == null) {
                name = values.getAsString("title");
                if (name == null) {
                    String substring;
                    lastDot = path.lastIndexOf(46);
                    if (lastDot < 0) {
                        substring = path.substring(lastSlash + 1);
                    } else {
                        substring = path.substring(lastSlash + 1, lastDot);
                    }
                    name = substring;
                }
            }
            values.put(MidiDeviceInfo.PROPERTY_NAME, name);
            values.put("date_modified", Long.valueOf(fileEntry.mLastModified));
            Uri uri;
            if (rowId == 0) {
                values.put("_data", path);
                uri = this.mMediaProvider.insert(this.mPlaylistsUri, values);
                rowId = ContentUris.parseId(uri);
                membersUri = Uri.withAppendedPath(uri, "members");
            } else {
                uri = ContentUris.withAppendedId(this.mPlaylistsUri, rowId);
                this.mMediaProvider.update(uri, values, null, null);
                membersUri = Uri.withAppendedPath(uri, "members");
                this.mMediaProvider.delete(membersUri, null, null);
            }
            Uri membersUri2 = membersUri;
            int i = 0;
            String playListDirectory = path.substring(0, lastSlash + 1);
            MediaFileType mediaFileType = MediaFile.getFileType(path);
            if (mediaFileType != null) {
                i = mediaFileType.fileType;
            }
            lastDot = i;
            if (lastDot == 44) {
                processM3uPlayList(path, playListDirectory, membersUri2, values, fileList);
                return;
            }
            int fileType = lastDot;
            MediaFileType mediaFileType2 = mediaFileType;
            if (fileType == 45) {
                processPlsPlayList(path, playListDirectory, membersUri2, values, fileList);
                return;
            } else if (fileType == 46) {
                processWplPlayList(path, playListDirectory, membersUri2, values, fileList);
                return;
            } else {
                return;
            }
        }
        throw new IllegalArgumentException("bad path ");
    }

    /* JADX WARNING: Missing block: B:21:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void processPlayLists() throws RemoteException {
        Iterator<FileEntry> iterator = this.mPlayLists.iterator();
        Cursor fileList = null;
        try {
            fileList = this.mMediaProvider.query(this.mFilesUri, FILES_PRESCAN_PROJECTION, "media_type=2", null, null, null);
            while (iterator.hasNext()) {
                FileEntry entry = (FileEntry) iterator.next();
                if (entry.mLastModifiedChanged) {
                    processPlayList(entry, fileList);
                }
            }
            if (fileList == null) {
                return;
            }
        } catch (RemoteException e) {
            if (fileList == null) {
                return;
            }
        } catch (Throwable th) {
            if (fileList != null) {
                fileList.close();
            }
        }
        fileList.close();
    }

    public static void setStorageEject(String path) {
        if (path == null || path.equals("")) {
            Log.e(TAG, "setStorageEject path = null!");
            return;
        }
        if (sCurScanDIR.startsWith(path)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setStorageEject curscanDir is ejected storage ! path ");
            stringBuilder.append(path);
            stringBuilder.append("sCurScanDIR =  ");
            stringBuilder.append(sCurScanDIR);
            Log.d(str, stringBuilder.toString());
            setStorageEjectFlag(true);
        }
    }

    public void close() {
        this.mCloseGuard.close();
        if (this.mClosed.compareAndSet(false, true)) {
            this.mMediaProvider.close();
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

    protected void updateValues(String path, ContentValues contentValues) {
    }

    public void deleteNomediaFile(StorageVolume[] volumes) {
        String str;
        StringBuilder stringBuilder;
        StorageVolume[] storageVolumeArr = volumes;
        if (storageVolumeArr != null) {
            for (StorageVolume storageVolume : storageVolumeArr) {
                String rootPath = storageVolume.getPath();
                for (String nomedia : sNomediaFilepath) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(rootPath);
                    stringBuilder2.append(nomedia);
                    String nomediaPath = stringBuilder2.toString();
                    ExternalStorageFileImpl nomediaFile = new ExternalStorageFileImpl(nomediaPath);
                    try {
                        String str2;
                        if (!nomediaFile.exists()) {
                        } else if (!nomediaFile.isFile() || nomediaFile.length() <= MAX_NOMEDIA_SIZE) {
                            try {
                                StringBuilder stringBuilder3;
                                if (deleteFile(nomediaFile)) {
                                    str2 = TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("delete nomedia file success [");
                                    stringBuilder3.append(nomediaPath);
                                    stringBuilder3.append("]");
                                    Log.w(str2, stringBuilder3.toString());
                                } else {
                                    str2 = TAG;
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("delete nomedia file fail [");
                                    stringBuilder3.append(nomediaPath);
                                    stringBuilder3.append("]");
                                    Log.w(str2, stringBuilder3.toString());
                                }
                            } catch (IOException e) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("delete nomedia file exception [");
                                stringBuilder.append(nomediaPath);
                                stringBuilder.append("]");
                                Log.w(str, stringBuilder.toString());
                            }
                        } else {
                            str2 = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("skip nomedia file [");
                            stringBuilder4.append(nomediaPath);
                            stringBuilder4.append("]  size:");
                            stringBuilder4.append(nomediaFile.length());
                            Log.w(str2, stringBuilder4.toString());
                        }
                    } catch (IOException e2) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("delete nomedia file exception [");
                        stringBuilder.append(nomediaPath);
                        stringBuilder.append("]");
                        Log.w(str, stringBuilder.toString());
                    }
                }
            }
        }
    }

    private boolean deleteFile(File file) throws IOException {
        boolean result = true;
        if (!file.exists()) {
            return true;
        }
        if (file.isFile()) {
            if (!file.delete()) {
                result = false;
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File deleteFile : files) {
                    if (!deleteFile(deleteFile)) {
                        result = false;
                    }
                }
            }
            if (!file.delete()) {
                result = false;
            }
        }
        return result;
    }
}
