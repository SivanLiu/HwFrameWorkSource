package android.media;

import android.common.HwMediaScannerManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.media.MediaScanner.MyMediaScannerClient;
import android.media.hwmnote.HwMnoteInterface;
import android.media.hwmnote.HwMnoteInterfaceUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.ExternalStorageFileImpl;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.utils.reflect.EasyInvokeFactory;
import huawei.android.provider.HwSettings.System;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.Character.UnicodeBlock;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class HwMediaScannerImpl implements HwMediaScannerManager {
    private static final String ALARMS = "alarms";
    private static final String ALARMS_PATH = "/system/media/audio/alarms/";
    private static final String AUDIO_FORMAT = ".ogg";
    private static final String DEFAULT_RINGTONE_PROPERTY_PREFIX = "ro.config.";
    private static final int DEFAULT_THRESHOLD_MAX_BYTES = 524288000;
    private static final String DEL_AUDIO_LIST_FILE = "del_audio_list.xml";
    private static final boolean ENABLE_BULK_INSERTS = true;
    private static final Uri EXTERNAL_AUDIO_URI = Media.getContentUri("external");
    private static final Uri EXTERNAL_IMAGE_URI = Images.Media.getContentUri("external");
    private static final Uri EXTERNAL_VIDEO_URI = Video.Media.getContentUri("external");
    private static final int FUNCTION_BYTE = 1;
    private static final int FUNCTION_LONG = 0;
    private static final String HW_3D_MODEL_IMAGE_TAG = "H W 3 D ";
    private static final int HW_3D_MODEL_IMAGE_TYPE = 16;
    private static final String HW_ALLFOCUS_IMAGE_COLUMN = "hw_image_refocus";
    private static final int HW_ALLFOCUS_IMAGE_TYPE_DUAL_CAMERA = 2;
    private static final int HW_ALLFOCUS_IMAGE_TYPE_SINGLE_CAMERA = 1;
    private static final String HW_AUTO_BEAUTY_BACK_IMAGE_TAG = "sbcb\u0000\u0000\u0000\u0000";
    private static final String HW_AUTO_BEAUTY_FRONT_IMAGE_TAG = "sbc\u0000\u0000\u0000\u0000\u0000";
    private static final int HW_AUTO_BEAUTY_IMAGE = 51;
    private static final int HW_CUSTOM_IMAGE_TAG_LEN = 20;
    private static final int HW_DUAL_CAMERA_ALLFOCUS_IMAGE_LEN = 8;
    private static final String HW_DUAL_CAMERA_ALLFOCUS_IMAGE_TAG = "DepthEn\u0000";
    private static final String HW_FAIR_LIGHT_IMAGE_TAG = "FairEn\u0000\u0000";
    private static final int HW_FAIR_LIGHT_IMAGE_TYPE_CAMERA = 6;
    private static final int HW_LIVE_PHOTO_IMAGE_TYPE = 50;
    private static final String HW_LIVE_TAG = "LIVE_";
    private static final String HW_MAKER_NOTE = "HwMakerNote";
    private static final String HW_MNOTE_ISO = "ISO-8859-1";
    private static final int HW_PANORAMA_3D_COMBINED_IMAGE_TYPE = 20;
    private static final String HW_PANORAMA_3D_COMBINED_TAG = "#FYUSEv3";
    private static final String HW_RECTIFY_IMAGE_COLUMN = "hw_rectify_offset";
    private static final String HW_RECTIFY_IMAGE_TAG = "RECTIFY_";
    private static final int HW_SINGLE_CAMERA_ALLFOCUS_IMAGE_LEN = 7;
    private static final String HW_SINGLE_CAMERA_ALLFOCUS_IMAGE_TAG = "Refocus";
    private static final String HW_SPECIAL_FILE_OFFSET_IMAGE_COLUMN = "special_file_offset";
    private static final String HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN = "special_file_type";
    private static final String HW_VOICE_IMAGE_COLUMN = "hw_voice_offset";
    private static final String HW_VOICE_TAG = "HWVOICE_";
    private static final String INVALID_UTF8_TOKEN = "??";
    private static final String KEY_TAG_BURST_NUMBER = "101";
    private static final String KEY_TAG_CAPTURE_MODE = "100";
    private static final String KEY_TAG_FACE_CONF = "302";
    private static final String KEY_TAG_FACE_COUNT = "301";
    private static final String KEY_TAG_FACE_LEYE_CENTER = "305";
    private static final String KEY_TAG_FACE_MOUTH_CENTER = "307";
    private static final String KEY_TAG_FACE_RECT = "304";
    private static final String KEY_TAG_FACE_REYE_CENTER = "306";
    private static final String KEY_TAG_FACE_SMILE_SCORE = "303";
    private static final String KEY_TAG_FACE_VERSION = "300";
    private static final String KEY_TAG_FRONT_CAMERA = "102";
    private static final String KEY_TAG_SCENE_BEACH_CONF = "205";
    private static final String KEY_TAG_SCENE_BLUESKY_CONF = "203";
    private static final String KEY_TAG_SCENE_FLOWERS_CONF = "208";
    private static final String KEY_TAG_SCENE_FOOD_CONF = "201";
    private static final String KEY_TAG_SCENE_GREENPLANT_CONF = "204";
    private static final String KEY_TAG_SCENE_NIGHT_CONF = "209";
    private static final String KEY_TAG_SCENE_SNOW_CONF = "206";
    private static final String KEY_TAG_SCENE_STAGE_CONF = "202";
    private static final String KEY_TAG_SCENE_SUNSET_CONF = "207";
    private static final String KEY_TAG_SCENE_TEXT_CONF = "210";
    private static final String KEY_TAG_SCENE_VERSION = "200";
    private static final int MAX_HW_CUSTOM_IMAGE_TAG_LEN = 20;
    private static long MAX_NOMEDIA_SIZE = 1024;
    private static final int MEDIA_BUFFER_SIZE = 100;
    private static final String NOTIFICATIONS = "notifications";
    private static final String NOTIFICATIONS_PATH = "/system/media/audio/notifications/";
    private static final String RINGTONES = "ringtones";
    private static final String RINGTONES_PATH = "/system/media/audio/ringtones/";
    private static final String TAG = "HwMediaScannerImpl";
    private static final String UI = "ui";
    private static final String UI_PATH = "/system/media/audio/ui/";
    private static HwMediaScannerManager mHwMediaScannerManager = new HwMediaScannerImpl();
    private static final HashMap<Integer, DataFlagMap> mTagsDataFlagMap = new LinkedHashMap();
    private static final String[] sNomediaFilepath = new String[]{"/.nomedia", "/DCIM/.nomedia", "/DCIM/Camera/.nomedia", "/Pictures/.nomedia", "/Pictures/Screenshots/.nomedia", "/tencent/.nomedia", "/tencent/MicroMsg/.nomedia", "/tencent/MicroMsg/Weixin/.nomedia", "/tencent/QQ_Images/.nomedia"};
    private static Sniffer sniffer = new Sniffer();
    private static MediaScannerUtils utils = ((MediaScannerUtils) EasyInvokeFactory.getInvokeUtils(MediaScannerUtils.class));
    private CustomImageInfo[] mCustomImageInfos;
    private String mDefaultRingtoneFilename2;
    private boolean mDefaultRingtoneSet2;
    private HashSet<String> mDelRingtonesList = new HashSet();
    private final String mExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    private boolean mIsAudioFilterLoad = false;
    private MediaInserter mMediaInserter;

    private static abstract class CustomImageInfo {
        protected String customImageTag;
        protected String databaseColumn;
        protected int databaseType;
        protected String tagCharsetName;
        protected int tagLength;

        protected abstract boolean checkTag(byte[] bArr, ContentValues contentValues);

        protected CustomImageInfo(String customImageTag, int tagLength, String tagCharsetName, String databaseColumn, int databaseType) {
            this.customImageTag = customImageTag;
            this.tagLength = tagLength;
            this.tagCharsetName = tagCharsetName;
            this.databaseColumn = databaseColumn;
            this.databaseType = databaseType;
        }
    }

    private static class DataFlagMap {
        public final String tagData;
        public final int tagFlag;

        DataFlagMap(String tagData, int tagFlag) {
            this.tagData = tagData;
            this.tagFlag = tagFlag;
        }
    }

    private static class FixedEndTagCustomImageInfo extends CustomImageInfo {
        protected FixedEndTagCustomImageInfo(String customImageTag, int tagLength, String tagCharsetName, String databaseColumn, int databaseType) {
            super(customImageTag, tagLength, tagCharsetName, databaseColumn, databaseType);
        }

        /* JADX WARNING: Missing block: B:9:0x0032, code skipped:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean checkTag(byte[] fileEndBytes, ContentValues values) {
            if (fileEndBytes == null || fileEndBytes.length < this.tagLength || !Arrays.equals(Arrays.copyOfRange(fileEndBytes, fileEndBytes.length - this.tagLength, fileEndBytes.length), this.customImageTag.getBytes(Charset.forName(this.tagCharsetName)))) {
                return false;
            }
            values.put(this.databaseColumn, Integer.valueOf(this.databaseType));
            return true;
        }
    }

    private static class HwVoiceOrRectifyImageInfo extends CustomImageInfo {
        protected HwVoiceOrRectifyImageInfo(String customImageTag, int tagLength, String tagCharsetName, String databaseColumn) {
            super(customImageTag, tagLength, tagCharsetName, databaseColumn, 0);
        }

        protected boolean checkTag(byte[] fileEndBytes, ContentValues values) {
            if (fileEndBytes == null || fileEndBytes.length < this.tagLength) {
                return false;
            }
            try {
                String tag = new String(Arrays.copyOfRange(fileEndBytes, fileEndBytes.length - this.tagLength, fileEndBytes.length), this.tagCharsetName).trim();
                if (tag.startsWith(this.customImageTag)) {
                    String[] split = tag.split("_");
                    if (split.length < 2) {
                        return false;
                    }
                    values.put(this.databaseColumn, Long.valueOf(split[1]));
                    return true;
                }
            } catch (UnsupportedEncodingException e) {
                Log.w(HwMediaScannerImpl.TAG, "fail to check custom image tag, throws UnsupportedEncodingException");
            } catch (NumberFormatException e2) {
                Log.w(HwMediaScannerImpl.TAG, "fail to check custom image tag, throws NumberFormatException");
            } catch (UnsupportedCharsetException e3) {
                Log.w(HwMediaScannerImpl.TAG, "fail to check custom image tag, throws UnsupportedCharsetException");
            }
            return false;
        }
    }

    private static class SpecialOffsetImageInfo extends CustomImageInfo {
        protected SpecialOffsetImageInfo(String customImageTag, int tagLength, String tagCharsetName, String databaseColumn, int databaseType) {
            super(customImageTag, tagLength, tagCharsetName, databaseColumn, databaseType);
        }

        protected boolean checkTag(byte[] fileEndBytes, ContentValues values) {
            String str;
            StringBuilder stringBuilder;
            if (fileEndBytes == null || fileEndBytes.length < this.tagLength) {
                return false;
            }
            try {
                String tag = new String(Arrays.copyOfRange(fileEndBytes, fileEndBytes.length - this.tagLength, fileEndBytes.length), this.tagCharsetName).trim();
                if (tag.startsWith(this.customImageTag)) {
                    String[] split = tag.split("_");
                    if (split.length < 2) {
                        return false;
                    }
                    values.put(this.databaseColumn, Integer.valueOf(this.databaseType));
                    values.put(HwMediaScannerImpl.HW_SPECIAL_FILE_OFFSET_IMAGE_COLUMN, Long.valueOf(split[1]));
                    String str2 = HwMediaScannerImpl.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("find a live tag. ");
                    stringBuilder2.append(tag);
                    Log.d(str2, stringBuilder2.toString());
                    return true;
                }
            } catch (UnsupportedEncodingException e) {
                str = HwMediaScannerImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fail tddo check custom image tag, throws UnsupportedEncodingException ");
                stringBuilder.append(this.databaseType);
                Log.w(str, stringBuilder.toString());
            } catch (Exception e2) {
                str = HwMediaScannerImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fail to check custom image tag, throws UnsupportedCharsetException ");
                stringBuilder.append(this.databaseType);
                Log.w(str, stringBuilder.toString());
            }
            return false;
        }
    }

    static {
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_CAPTURE_MODE, KEY_TAG_CAPTURE_MODE, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_BURST_NUMBER, KEY_TAG_BURST_NUMBER, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FRONT_CAMERA, KEY_TAG_FRONT_CAMERA, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_VERSION, KEY_TAG_SCENE_VERSION, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_FOOD_CONF, KEY_TAG_SCENE_FOOD_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_STAGE_CONF, KEY_TAG_SCENE_STAGE_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_BLUESKY_CONF, KEY_TAG_SCENE_BLUESKY_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_GREENPLANT_CONF, KEY_TAG_SCENE_GREENPLANT_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_BEACH_CONF, KEY_TAG_SCENE_BEACH_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_SNOW_CONF, KEY_TAG_SCENE_SNOW_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_SUNSET_CONF, KEY_TAG_SCENE_SUNSET_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_FLOWERS_CONF, KEY_TAG_SCENE_FLOWERS_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_NIGHT_CONF, KEY_TAG_SCENE_NIGHT_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_SCENE_TEXT_CONF, KEY_TAG_SCENE_TEXT_CONF, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_VERSION, KEY_TAG_FACE_VERSION, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_COUNT, KEY_TAG_FACE_COUNT, 0);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_CONF, KEY_TAG_FACE_CONF, 1);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_SMILE_SCORE, KEY_TAG_FACE_SMILE_SCORE, 1);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_RECT, KEY_TAG_FACE_RECT, 1);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_LEYE_CENTER, KEY_TAG_FACE_LEYE_CENTER, 1);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_REYE_CENTER, KEY_TAG_FACE_REYE_CENTER, 1);
        addTagsType(HwMnoteInterfaceUtils.HW_MNOTE_TAG_FACE_MOUTH_CENTER, KEY_TAG_FACE_MOUTH_CENTER, 1);
    }

    private static void addTagsType(int tag, String tagData, int tagFlag) {
        mTagsDataFlagMap.put(Integer.valueOf(tag), new DataFlagMap(tagData, tagFlag));
    }

    private HwMediaScannerImpl() {
        initCustomImageInfos();
    }

    public static HwMediaScannerManager getDefault() {
        return mHwMediaScannerManager;
    }

    public boolean loadAudioFilterConfig(Context context) {
        boolean z;
        synchronized (this) {
            z = true;
            if (!this.mIsAudioFilterLoad) {
                loadAudioFilterConfigFromCust();
                loadAudioFilterConfigFromCache(context);
                this.mIsAudioFilterLoad = true;
            }
            if (this.mDelRingtonesList.size() == 0) {
                z = false;
            }
        }
        return z;
    }

    private void loadAudioFilterConfigFromCust() {
        ArrayList<File> files = HwCfgFilePolicy.getCfgFileList("xml/del_audio_list.xml", 0);
        int filesLen = files.size();
        for (int i = 0; i < filesLen; i++) {
            File file = (File) files.get(i);
            if (file != null && file.exists()) {
                FileInputStream in = null;
                XmlPullParser xpp = null;
                try {
                    in = new FileInputStream(file);
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    xpp = factory.newPullParser();
                    xpp.setInput(in, null);
                    for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                        if (eventType == 2) {
                            HashSet hashSet;
                            StringBuilder stringBuilder;
                            if (ALARMS.equals(xpp.getName())) {
                                hashSet = this.mDelRingtonesList;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(ALARMS_PATH);
                                stringBuilder.append(xpp.nextText());
                                stringBuilder.append(AUDIO_FORMAT);
                                hashSet.add(stringBuilder.toString());
                            } else if (NOTIFICATIONS.equals(xpp.getName())) {
                                hashSet = this.mDelRingtonesList;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(NOTIFICATIONS_PATH);
                                stringBuilder.append(xpp.nextText());
                                stringBuilder.append(AUDIO_FORMAT);
                                hashSet.add(stringBuilder.toString());
                            } else if (RINGTONES.equals(xpp.getName())) {
                                hashSet = this.mDelRingtonesList;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(RINGTONES_PATH);
                                stringBuilder.append(xpp.nextText());
                                stringBuilder.append(AUDIO_FORMAT);
                                hashSet.add(stringBuilder.toString());
                            } else if (UI.equals(xpp.getName())) {
                                hashSet = this.mDelRingtonesList;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(UI_PATH);
                                stringBuilder.append(xpp.nextText());
                                stringBuilder.append(AUDIO_FORMAT);
                                hashSet.add(stringBuilder.toString());
                            }
                        }
                    }
                } catch (XmlPullParserException e) {
                    Log.w(TAG, "failed to load audio filter config from cust, parser exception");
                } catch (IOException e2) {
                    Log.w(TAG, "failed to load audio filter config from cust, io exception");
                } catch (Throwable th) {
                    IoUtils.closeQuietly(null);
                }
                IoUtils.closeQuietly(in);
            }
        }
    }

    private void loadAudioFilterConfigFromCache(Context context) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(context.openFileInput(DEL_AUDIO_LIST_FILE), "UTF-8"));
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine == null) {
                    break;
                }
                this.mDelRingtonesList.add(line);
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "failed to load audio filter config from cache, file not found exception");
        } catch (IOException e2) {
            Log.w(TAG, "failed to load audio filter config from cache, io exception");
        } catch (Throwable th) {
            IoUtils.closeQuietly(reader);
        }
        IoUtils.closeQuietly(reader);
    }

    public boolean isAudioFilterFile(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        boolean contains;
        synchronized (this) {
            contains = this.mDelRingtonesList.contains(path);
        }
        return contains;
    }

    private int getSkipCustomDirectory(String[] whiteList, String[] blackList, StringBuffer sb) {
        int i = 0;
        int size = 0;
        for (String dir : whiteList) {
            if (!dir.isEmpty()) {
                sb.append(dir);
                sb.append(",");
                size++;
            }
        }
        int size2 = blackList.length;
        while (i < size2) {
            String dir2 = blackList[i];
            if (!dir2.isEmpty()) {
                sb.append(dir2);
                sb.append(",");
                size++;
            }
            i++;
        }
        return size;
    }

    public void setMediaInserter(MediaInserter mediaInserter) {
        this.mMediaInserter = mediaInserter;
    }

    public void scanCustomDirectories(MediaScanner scanner, MyMediaScannerClient mClient, String[] directories, String volumeName, String[] whiteList, String[] blackList) {
        try {
            int i;
            utils.prescan(scanner, null, scanner.getIsPrescanFiles());
            int i2 = 0;
            for (int i3 = 0; i3 < whiteList.length; i3++) {
                insertDirectory(mClient, whiteList[i3]);
                int len = scanner.getRootDirLength(whiteList[i3]);
                MediaScanner.sCurScanDIR = whiteList[i3];
                scanner.setExteLen(len);
                utils.processDirectory(scanner, whiteList[i3], mClient);
            }
            if (this.mMediaInserter != null) {
                this.mMediaInserter.flushAll();
            }
            StringBuffer sb = new StringBuffer();
            scanner.addSkipCustomDirectory(sb.toString(), getSkipCustomDirectory(whiteList, blackList, sb));
            for (i = 0; i < directories.length; i++) {
                if (!shouldSkipDirectory(directories[i], whiteList, blackList)) {
                    insertDirectory(mClient, directories[i]);
                }
                int len2 = scanner.getRootDirLength(directories[i]);
                MediaScanner.sCurScanDIR = directories[i];
                scanner.setExteLen(len2);
                utils.processDirectory(scanner, directories[i], mClient);
            }
            scanner.clearSkipCustomDirectory();
            if (this.mMediaInserter != null) {
                this.mMediaInserter.flushAll();
            }
            while (i2 < blackList.length) {
                insertDirectory(mClient, blackList[i2]);
                i = scanner.getRootDirLength(blackList[i2]);
                MediaScanner.sCurScanDIR = blackList[i2];
                scanner.setExteLen(i);
                utils.processDirectory(scanner, blackList[i2], mClient);
                i2++;
            }
            if (this.mMediaInserter != null) {
                this.mMediaInserter.flushAll();
            }
            scanner.postscan(directories);
        } catch (SQLException e) {
            Log.e(TAG, "SQLException in MediaScanner.scan()", e);
        } catch (UnsupportedOperationException e2) {
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e2);
        } catch (RemoteException e3) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e3);
        }
    }

    private static boolean shouldSkipDirectory(String path, String[] whiteList, String[] blackList) {
        for (String dir : whiteList) {
            if (dir.equals(path)) {
                return true;
            }
        }
        for (String dir2 : blackList) {
            if (dir2.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private void insertDirectory(MediaScannerClient client, String path) {
        File file = new File(path);
        if (file.exists()) {
            String filePath;
            long lastModifiedSeconds = file.lastModified() / 1000;
            if (path.length() <= 1 || path.charAt(path.length() - 1) != '/') {
                filePath = path;
            } else {
                filePath = path.substring(null, path.length() - 1);
            }
            client.scanFile(filePath, lastModifiedSeconds, 0, true, false);
        }
    }

    public int getBufferSize(Uri tableUri, int bufferSizePerUri) {
        return (EXTERNAL_IMAGE_URI.equals(tableUri) || EXTERNAL_VIDEO_URI.equals(tableUri) || EXTERNAL_AUDIO_URI.equals(tableUri)) ? 100 : bufferSizePerUri;
    }

    public void setHwDefaultRingtoneFileNames() {
        if (isMultiSimEnabled()) {
            this.mDefaultRingtoneFilename2 = SystemProperties.get("ro.config.ringtone2");
        }
    }

    public boolean hwNeedSetSettings(String path) {
        if (isMultiSimEnabled() && !this.mDefaultRingtoneSet2 && (TextUtils.isEmpty(this.mDefaultRingtoneFilename2) || doesPathHaveFilename(path, this.mDefaultRingtoneFilename2))) {
            return true;
        }
        return false;
    }

    private boolean doesPathHaveFilename(String path, String filename) {
        int pathFilenameStart = path.lastIndexOf(File.separatorChar) + 1;
        int filenameLength = filename.length();
        if (path.regionMatches(pathFilenameStart, filename, 0, filenameLength) && pathFilenameStart + filenameLength == path.length()) {
            return true;
        }
        return false;
    }

    public void hwSetRingtone2Settings(boolean needToSetSettings2, boolean ringtones, Uri tableUri, long rowId, Context context) {
        if (isMultiSimEnabled() && needToSetSettings2 && ringtones) {
            setSettingIfNotSet(System.RINGTONE2, tableUri, rowId, context);
            this.mDefaultRingtoneSet2 = true;
        }
    }

    private String settingSetIndicatorName(String base) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(base);
        stringBuilder.append("_set");
        return stringBuilder.toString();
    }

    private boolean wasRingtoneAlreadySet(ContentResolver cr, String name) {
        boolean z = false;
        try {
            if (Settings.System.getInt(cr, settingSetIndicatorName(name)) != 0) {
                z = true;
            }
            return z;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    private void setSettingIfNotSet(String settingName, Uri uri, long rowId, Context context) {
        ContentResolver cr = context.getContentResolver();
        if (!wasRingtoneAlreadySet(cr, settingName)) {
            if (TextUtils.isEmpty(Settings.System.getString(cr, settingName))) {
                Uri settingUri = Settings.System.getUriFor(settingName);
                RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.getDefaultType(settingUri), ContentUris.withAppendedId(uri, rowId));
            }
            Settings.System.putInt(cr, settingSetIndicatorName(settingName), 1);
        }
    }

    public String getExtSdcardVolumePath(Context context) {
        if (context == null) {
            return null;
        }
        StorageVolume[] storageVolumes = ((StorageManager) context.getSystemService("storage")).getVolumeList();
        if (storageVolumes == null) {
            return null;
        }
        for (StorageVolume storageVolume : storageVolumes) {
            if (storageVolume.isRemovable() && !storageVolume.getPath().contains("usb")) {
                return storageVolume.getPath();
            }
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:11:0x003c, code skipped:
            if (r10 != null) goto L_0x003e;
     */
    /* JADX WARNING: Missing block: B:12:0x003e, code skipped:
            r10.close();
     */
    /* JADX WARNING: Missing block: B:17:0x0048, code skipped:
            if (r10 == null) goto L_0x004b;
     */
    /* JADX WARNING: Missing block: B:18:0x004b, code skipped:
            if (r1 != 0) goto L_0x004e;
     */
    /* JADX WARNING: Missing block: B:19:0x004d, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:20:0x004e, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isSkipExtSdcard(ContentProviderClient mMediaProvider, String mExtStroagePath, String mPackageName, Uri mFilesUriNoNotify) {
        boolean skip = false;
        if (mExtStroagePath == null) {
            return false;
        }
        int externelNum = -1;
        String[] projectionIn = new String[]{"COUNT(*)"};
        String selection = new StringBuilder();
        selection.append("_data LIKE '");
        selection.append(mExtStroagePath);
        selection.append("%'");
        Cursor c = null;
        try {
            c = mMediaProvider.query(mFilesUriNoNotify, projectionIn, selection.toString(), null, null, null);
            if (c != null && c.moveToFirst()) {
                externelNum = c.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
        }
    }

    public boolean isBitmapSizeTooLarge(String path) {
        File imageFile = new File(path);
        long limitSize = SystemProperties.getLong("ro.config.hw_pic_limit_size", 0);
        if (limitSize <= 0 || imageFile.length() <= (limitSize * 1024) * 1024) {
            return false;
        }
        return true;
    }

    public void initializeHwVoiceAndFocus(String path, ContentValues values) {
        byte[] fileEndBytes = readFileEndBytes(path);
        int i = 0;
        while (i < this.mCustomImageInfos.length && !this.mCustomImageInfos[i].checkTag(fileEndBytes, values)) {
            i++;
        }
    }

    private byte[] readFileEndBytes(String path) {
        RandomAccessFile randomFile = null;
        byte[] buffer = new byte[null];
        if (path == null) {
            return buffer;
        }
        try {
            randomFile = new RandomAccessFile(path, "r");
            long fileLength = randomFile.length();
            if (fileLength < 20) {
                try {
                    randomFile.close();
                } catch (IOException e) {
                    Log.w(TAG, "fail to process custom image, readFileEndBytes close file fail");
                }
                return buffer;
            }
            byte[] tmp = new byte[20];
            randomFile.seek(fileLength - 20);
            if (randomFile.read(tmp) != 20) {
                try {
                    randomFile.close();
                } catch (IOException e2) {
                    Log.w(TAG, "fail to process custom image, readFileEndBytes close file fail");
                }
                return buffer;
            }
            buffer = tmp;
            try {
                randomFile.close();
            } catch (IOException e3) {
                Log.w(TAG, "fail to process custom image, readFileEndBytes close file fail");
            }
            return buffer;
        } catch (IOException e4) {
            Log.w(TAG, "fail to process custom image, readFileEndBytes throws IOException");
            if (randomFile != null) {
                randomFile.close();
            }
        } catch (SecurityException e5) {
            Log.w(TAG, "fail to process custom image, readFileEndBytes throws SecurityException");
            if (randomFile != null) {
                randomFile.close();
            }
        } catch (IllegalArgumentException e6) {
            Log.w(TAG, "fail to process custom image, readFileEndBytes throws IllegalArgumentException");
            if (randomFile != null) {
                randomFile.close();
            }
        } catch (NullPointerException e7) {
            Log.w(TAG, "fail to process custom image, readFileEndBytes throws NullPointerException");
            if (randomFile != null) {
                randomFile.close();
            }
        } catch (Throwable th) {
            if (randomFile != null) {
                try {
                    randomFile.close();
                } catch (IOException e8) {
                    Log.w(TAG, "fail to process custom image, readFileEndBytes close file fail");
                }
            }
        }
    }

    public void pruneDeadThumbnailsFolder() {
        int i = 0;
        boolean isDelete = false;
        try {
            StatFs sdcardFileStats = new StatFs(this.mExternalStoragePath);
            long freeMem = ((long) sdcardFileStats.getAvailableBlocks()) * ((long) sdcardFileStats.getBlockSize());
            long totalMem = (((long) sdcardFileStats.getBlockCount()) * ((long) sdcardFileStats.getBlockSize())) / 10;
            long thresholdMem = 524288000;
            if (totalMem <= 524288000) {
                thresholdMem = totalMem;
            }
            isDelete = freeMem <= thresholdMem;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("freeMem[");
            stringBuilder.append(freeMem);
            stringBuilder.append("] 10%totalMem[");
            stringBuilder.append(totalMem);
            stringBuilder.append("] under ");
            stringBuilder.append(this.mExternalStoragePath);
            Log.v(str, stringBuilder.toString());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException in pruneDeadThumbnailsFolder", e);
        }
        if (isDelete) {
            String directory = new StringBuilder();
            directory.append(this.mExternalStoragePath);
            directory.append("/DCIM/.thumbnails");
            File thumbFolder = new File(directory.toString());
            if (thumbFolder.exists()) {
                File[] files = thumbFolder.listFiles();
                if (files != null) {
                    Log.v(TAG, "delete .thumbnails");
                    int length = files.length;
                    while (i < length) {
                        if (!files[i].delete()) {
                            Log.e(TAG, "Failed to delete file!");
                        }
                        i++;
                    }
                } else {
                    return;
                }
            }
            Log.e(TAG, ".thumbnails folder not exists. ");
        }
    }

    private static boolean isMultiSimEnabled() {
        try {
            return TelephonyManager.getDefault().isMultiSimEnabled();
        } catch (Exception e) {
            Log.w(TAG, "isMultiSimEnabled api met Exception!");
            return false;
        }
    }

    private boolean isMessyCharacter(char input) {
        UnicodeBlock unicodeBlock = UnicodeBlock.of(input);
        return unicodeBlock == UnicodeBlock.LATIN_1_SUPPLEMENT || unicodeBlock == UnicodeBlock.SPECIALS || unicodeBlock == UnicodeBlock.HEBREW || unicodeBlock == UnicodeBlock.GREEK || unicodeBlock == UnicodeBlock.CYRILLIC_SUPPLEMENTARY || unicodeBlock == UnicodeBlock.LATIN_EXTENDED_A || unicodeBlock == UnicodeBlock.LATIN_EXTENDED_B || unicodeBlock == UnicodeBlock.COMBINING_DIACRITICAL_MARKS || unicodeBlock == UnicodeBlock.PRIVATE_USE_AREA || unicodeBlock == UnicodeBlock.ARMENIAN;
    }

    private boolean isMessyCharacterOrigin(char input) {
        UnicodeBlock unicodeBlock = UnicodeBlock.of(input);
        return unicodeBlock == UnicodeBlock.SPECIALS || unicodeBlock == UnicodeBlock.GREEK || unicodeBlock == UnicodeBlock.CYRILLIC_SUPPLEMENTARY || unicodeBlock == UnicodeBlock.LATIN_EXTENDED_A || unicodeBlock == UnicodeBlock.LATIN_EXTENDED_B || unicodeBlock == UnicodeBlock.COMBINING_DIACRITICAL_MARKS || unicodeBlock == UnicodeBlock.PRIVATE_USE_AREA || ((unicodeBlock == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS && !CharacterTables.isFrequentHan(input)) || unicodeBlock == UnicodeBlock.BOX_DRAWING || unicodeBlock == UnicodeBlock.HANGUL_SYLLABLES || unicodeBlock == UnicodeBlock.ARMENIAN);
    }

    private boolean isAcceptableCharacter(char input) {
        UnicodeBlock unicodeBlock = UnicodeBlock.of(input);
        return unicodeBlock == UnicodeBlock.BASIC_LATIN || unicodeBlock == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || unicodeBlock == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || unicodeBlock == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || unicodeBlock == UnicodeBlock.GENERAL_PUNCTUATION || unicodeBlock == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || unicodeBlock == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    private String trimIncorrectPunctuation(String input) {
        return Pattern.compile("[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]").matcher(Pattern.compile("\\s*|\t*|\r*|\n*").matcher(input).replaceAll("").replaceAll("\\p{P}", "")).replaceAll("");
    }

    private boolean isAcceptableString(String input) {
        for (char c : trimIncorrectPunctuation(input).trim().toCharArray()) {
            if (!isAcceptableCharacter(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isStringMessy(String input) {
        for (char c : trimIncorrectPunctuation(input).trim().toCharArray()) {
            if (isMessyCharacter(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStringMessyOrigin(String input) {
        for (char c : trimIncorrectPunctuation(input).trim().toCharArray()) {
            if (isMessyCharacterOrigin(c)) {
                return true;
            }
        }
        return false;
    }

    private String getCorrectEncodedString(String input) {
        if (isStringMessy(input)) {
            try {
                String utf8 = new String(input.getBytes(HW_MNOTE_ISO), "UTF-8");
                if (isAcceptableString(utf8)) {
                    return utf8;
                }
                String gbk = new String(input.getBytes(HW_MNOTE_ISO), "GBK");
                if (isAcceptableString(gbk)) {
                    return gbk;
                }
                String big5 = new String(input.getBytes(HW_MNOTE_ISO), "BIG5");
                if (isAcceptableString(big5)) {
                    return big5;
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "unsupported encoding : \n", e);
            }
        }
        return input;
    }

    private boolean isInvalidUtf8(String input) {
        return input != null && input.contains(INVALID_UTF8_TOKEN);
    }

    private boolean isInvalidString(String input) {
        return TextUtils.isEmpty(input) || isInvalidUtf8(input) || isStringMessy(input);
    }

    private String finalCheck(String value, String path, int flag) {
        if (isInvalidString(value)) {
            if (flag == 1 || flag == 2) {
                return "<unknown>";
            }
            value = getDisplayName(path);
        }
        return value;
    }

    private String getDisplayName(String path) {
        int lastdotIndex = path.lastIndexOf(".");
        int lastSlashIndex = path.lastIndexOf("/");
        if (lastdotIndex <= 0 || lastSlashIndex <= 0 || lastSlashIndex > lastdotIndex) {
            return "";
        }
        return path.substring(lastSlashIndex + 1, lastdotIndex);
    }

    public boolean useMessyOptimize() {
        String debug = SystemProperties.get("ro.product.locale.region", "");
        return debug != null && "CN".equals(debug);
    }

    public boolean isMp3(String mimetype) {
        if (mimetype == null || (!Sniffer.MEDIA_MIMETYPE_AUDIO_MPEG.equalsIgnoreCase(mimetype) && !"audio/x-mp3".equalsIgnoreCase(mimetype) && !"audio/x-mpeg".equalsIgnoreCase(mimetype) && !"audio/mp3".equalsIgnoreCase(mimetype))) {
            return false;
        }
        return true;
    }

    public boolean preHandleStringTag(String value, String mimetype) {
        if (!useMessyOptimize() || !isMp3(mimetype) || TextUtils.isEmpty(value) || !isStringMessyOrigin(value)) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("value: ");
        stringBuilder.append(value);
        Log.e(str, stringBuilder.toString());
        return true;
    }

    public void initializeSniffer(String path) {
        sniffer.setDataSource(path);
    }

    public void resetSniffer() {
        sniffer.reset();
    }

    public String postHandleStringTag(String value, String path, int flag) {
        switch (flag) {
            case 1:
                return finalCheck(getCorrectEncodedString(sniffer.getAlbum()), path, flag);
            case 2:
                return finalCheck(getCorrectEncodedString(sniffer.getArtist()), path, flag);
            case 3:
                try {
                    return finalCheck(getCorrectEncodedString(sniffer.getTitle()), path, flag);
                } catch (Exception e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("postHandleStringTag e: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    break;
                }
        }
        return value;
    }

    private void initCustomImageInfos() {
        this.mCustomImageInfos = new CustomImageInfo[]{new HwVoiceOrRectifyImageInfo(HW_VOICE_TAG, 20, HW_MNOTE_ISO, HW_VOICE_IMAGE_COLUMN), new HwVoiceOrRectifyImageInfo(HW_RECTIFY_IMAGE_TAG, 20, HW_MNOTE_ISO, HW_RECTIFY_IMAGE_COLUMN), new SpecialOffsetImageInfo(HW_LIVE_TAG, 20, "UTF-8", HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, 50), new FixedEndTagCustomImageInfo(HW_SINGLE_CAMERA_ALLFOCUS_IMAGE_TAG, 7, "UTF-8", HW_ALLFOCUS_IMAGE_COLUMN, 1), new FixedEndTagCustomImageInfo(HW_DUAL_CAMERA_ALLFOCUS_IMAGE_TAG, 8, "UTF-8", HW_ALLFOCUS_IMAGE_COLUMN, 2), new FixedEndTagCustomImageInfo(HW_FAIR_LIGHT_IMAGE_TAG, 8, "UTF-8", HW_ALLFOCUS_IMAGE_COLUMN, 6), new FixedEndTagCustomImageInfo(HW_PANORAMA_3D_COMBINED_TAG, HW_PANORAMA_3D_COMBINED_TAG.length(), "UTF-8", HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, 20), new FixedEndTagCustomImageInfo(HW_3D_MODEL_IMAGE_TAG, HW_3D_MODEL_IMAGE_TAG.length(), "UTF-8", HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, 16), new FixedEndTagCustomImageInfo(HW_AUTO_BEAUTY_FRONT_IMAGE_TAG, HW_AUTO_BEAUTY_FRONT_IMAGE_TAG.length(), "UTF-8", HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, HW_AUTO_BEAUTY_IMAGE), new FixedEndTagCustomImageInfo(HW_AUTO_BEAUTY_BACK_IMAGE_TAG, HW_AUTO_BEAUTY_FRONT_IMAGE_TAG.length(), "UTF-8", HW_SPECIAL_FILE_TYPE_IMAGE_COLUMN, HW_AUTO_BEAUTY_IMAGE)};
    }

    public void deleteNomediaFile() {
        String str;
        StringBuilder stringBuilder;
        for (StorageVolume storageVolume : StorageManager.getVolumeList(UserHandle.myUserId(), 256)) {
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

    public void scanHwMakerNote(ContentValues values, ExifInterface exif) {
        if (exif == null || values == null) {
            Log.e(TAG, "HwMediaScannerImpl scanhwMnote arguments error !");
            return;
        }
        String hwMakerNoteStr = exif.getAttribute(HW_MAKER_NOTE);
        if (hwMakerNoteStr != null) {
            byte[] hwMakerNote = hwMakerNoteStr.getBytes(Charset.forName(HW_MNOTE_ISO));
            HwMnoteInterface hwMnoteInterface = new HwMnoteInterface();
            try {
                hwMnoteInterface.readHwMnote(hwMakerNote);
                JSONObject jsonObject = getJsonDatas(hwMnoteInterface);
                if (jsonObject == null) {
                    Log.e(TAG, "HwMediaScannerImpl scanhwMnote jsonObject == null !");
                } else {
                    values.put("cam_perception", jsonObject.toString());
                }
            } catch (IOException e) {
                Log.e(TAG, "HwMediaScannerImpl scanHwMnote readHwMnote() failed !!!");
            }
        }
    }

    private JSONObject getJsonDatas(HwMnoteInterface hwMnoteInterface) {
        if (hwMnoteInterface == null) {
            Log.w(TAG, "HwMediaScannerImpl getJsonDatas parameter hwMnoteInterface == null !");
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        for (Entry<Integer, DataFlagMap> entry : mTagsDataFlagMap.entrySet()) {
            int key = ((Integer) entry.getKey()).intValue();
            DataFlagMap dataFlagMap = (DataFlagMap) entry.getValue();
            switch (dataFlagMap.tagFlag) {
                case 0:
                    try {
                        jsonObject.put(dataFlagMap.tagData, hwMnoteInterface.getTagLongValue(key));
                        break;
                    } catch (JSONException e) {
                        Log.w(TAG, "HwMediaScannerImpl getJsonDatas FUNCTION_LONG jsonObject.put has JSONException !");
                        break;
                    }
                case 1:
                    try {
                        byte[] bytes = hwMnoteInterface.getTagByteValues(key);
                        if (bytes != null) {
                            String tmpStr = new String(bytes, Charset.forName(HW_MNOTE_ISO));
                            if (tmpStr.length() <= 0) {
                                break;
                            }
                            jsonObject.put(dataFlagMap.tagData, tmpStr);
                            break;
                        }
                        break;
                    } catch (JSONException e2) {
                        Log.w(TAG, "HwMediaScannerImpl getJsonDatas FUNCTION_BYTE jsonObject.put has JSONException");
                        break;
                    }
                default:
                    Log.i(TAG, "Other Function Type !");
                    break;
            }
        }
        return jsonObject;
    }
}
