package com.huawei.opcollect.collector.pullcollection;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.Audio.Media;
import com.huawei.nb.model.collectencrypt.RawMediaAppStastic;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;
import java.util.Date;
import org.json.JSONException;
import org.json.JSONObject;

public class MediaAppAction extends Action {
    private static final Uri AUDIO_URI = Media.EXTERNAL_CONTENT_URI;
    private static final int COLUMN_FIRST = 0;
    private static final String EIGHTY_SONGS = "eighty_songs";
    private static final Uri FRONT_PHOTO_NUM_URI = Uri.parse("content://com.huawei.gallery.provider/open_api/user_profile/get_front_photo_num");
    private static final String NEW_CENTURY_SONGS = "new_century_songs";
    private static final String NINETY_SONGS = "ninety_songs";
    private static final Uri PHOTO_TAG_INFO_URI = Uri.parse("content://com.huawei.gallery.provider/open_api/user_profile/get_photo_tag_info");
    private static final String SEVENTY_SONGS = "seventy_songs";
    private static final String TAG = "MediaAppAction";
    private static final Uri TOP_CAMERA_MODE_URI = Uri.parse("content://com.huawei.gallery.provider/open_api/user_profile/get_top_camera_mode");
    private static final Uri TOURISM_PHOTO_NUM_URI = Uri.parse("content://com.huawei.gallery.provider/open_api/user_profile/get_tourism_photo_num");
    private static MediaAppAction sInstance = null;

    private MediaAppAction(Context context, String name) {
        super(context, name);
        setDailyRecordNum(queryDailyRecordNum(RawMediaAppStastic.class));
    }

    public static synchronized MediaAppAction getInstance(Context context) {
        MediaAppAction mediaAppAction;
        synchronized (MediaAppAction.class) {
            if (sInstance == null) {
                sInstance = new MediaAppAction(context, OPCollectConstant.MEDIA_ACTION_NAME);
            }
            mediaAppAction = sInstance;
        }
        return mediaAppAction;
    }

    private static boolean isNullOrEmptyCursor(Cursor cursor) {
        if (cursor == null) {
            OPCollectLog.e(TAG, "cursor is null");
            return true;
        } else if (cursor.getCount() > 0) {
            return false;
        } else {
            OPCollectLog.e(TAG, "cursor size <= 0");
            cursor.close();
            return true;
        }
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (MediaAppAction.class) {
            sInstance = null;
        }
    }

    protected boolean execute() {
        return collectRawMediaAppStatics();
    }

    private boolean collectRawMediaAppStatics() {
        new Thread(new Runnable() {
            public void run() {
                OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, MediaAppAction.this.getRawMediaAppStatics()).sendToTarget();
            }
        }).start();
        return true;
    }

    private RawMediaAppStastic getRawMediaAppStatics() {
        RawMediaAppStastic rawMediaAppStastic = new RawMediaAppStastic();
        rawMediaAppStastic.setMTourismPhotoNum(Integer.valueOf(getTourismPhotoNum()));
        rawMediaAppStastic.setMFrontPhotoNum(Integer.valueOf(getFrontPhotoNum()));
        rawMediaAppStastic.setMPhotoTagInfo(getPhotoTagInfo());
        rawMediaAppStastic.setMTopCameraMode(getTopCameraMode());
        rawMediaAppStastic.setMTimeStamp(new Date());
        rawMediaAppStastic.setMAppInstalled(OPCollectUtils.getThirdPartyAppList(this.mContext));
        rawMediaAppStastic.setMAppUsageTime(OPCollectUtils.getAppUsageState(this.mContext));
        rawMediaAppStastic.setMMusicYear(analyseAudioYearList());
        rawMediaAppStastic.setMReservedText(OPCollectUtils.formatCurrentTime());
        return rawMediaAppStastic;
    }

    private int getTourismPhotoNum() {
        int num = 0;
        Cursor cursor = queryDataFromGallery2(TOURISM_PHOTO_NUM_URI);
        if (isNullOrEmptyCursor(cursor)) {
            return num;
        }
        cursor.moveToFirst();
        try {
            num = Integer.parseInt(cursor.getString(0));
        } catch (NumberFormatException e) {
            OPCollectLog.e(TAG, e.getMessage());
        } finally {
            cursor.close();
        }
        OPCollectLog.i(TAG, "getTourismPhotoNum: " + num);
        return num;
    }

    private int getFrontPhotoNum() {
        int num = 0;
        Cursor cursor = queryDataFromGallery2(FRONT_PHOTO_NUM_URI);
        if (isNullOrEmptyCursor(cursor)) {
            return 0;
        }
        cursor.moveToFirst();
        try {
            num = Integer.parseInt(cursor.getString(0));
            OPCollectLog.i(TAG, "getFrontPhotoNum: " + num);
        } catch (NumberFormatException e) {
            OPCollectLog.e(TAG, e.getMessage());
        } finally {
            cursor.close();
        }
        return num;
    }

    private String getPhotoTagInfo() {
        String str = "";
        Cursor cursor = queryDataFromGallery2(PHOTO_TAG_INFO_URI);
        if (isNullOrEmptyCursor(cursor)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        cursor.moveToFirst();
        do {
            sb.append(cursor.getString(0));
            sb.append(";");
        } while (cursor.moveToNext());
        cursor.close();
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private String getTopCameraMode() {
        String str = "";
        Cursor cursor = queryDataFromGallery2(TOP_CAMERA_MODE_URI);
        if (isNullOrEmptyCursor(cursor)) {
            return str;
        }
        cursor.moveToFirst();
        str = cursor.getString(0);
        OPCollectLog.i(TAG, "getTopCameraMode: " + str);
        cursor.close();
        return str;
    }

    private Cursor queryDataFromGallery2(Uri uri) {
        if (uri == null || this.mContext == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            return this.mContext.getContentResolver().query(uri, null, null, null, null);
        } catch (Exception e) {
            OPCollectLog.e(TAG, "queryDataFromGallery2: " + e.getMessage());
            return cursor;
        }
    }

    private String analyseAudioYearList() {
        if (this.mContext == null) {
            OPCollectLog.e(TAG, "context is null");
            return "";
        }
        JSONObject object = new JSONObject();
        String[] audioProjection = new String[]{"year"};
        String where_eighty = "year <= 1989";
        String where_ninety = "year <= 1999";
        String where_new_century = "year > 1999";
        int count_seventy = 0;
        int count_eighty = 0;
        Cursor audioCursor_seventy = null;
        Cursor audioCursor_eighty = null;
        Cursor audioCursor_ninety = null;
        Cursor audioCursor_new_century = null;
        try {
            audioCursor_seventy = this.mContext.getContentResolver().query(AUDIO_URI, audioProjection, "year <= 1979", null, null);
            if (audioCursor_seventy != null) {
                count_seventy = audioCursor_seventy.getCount();
                object.put(SEVENTY_SONGS, count_seventy);
            }
            audioCursor_eighty = this.mContext.getContentResolver().query(AUDIO_URI, audioProjection, where_eighty, null, null);
            if (audioCursor_eighty != null) {
                count_eighty = audioCursor_eighty.getCount() - count_seventy;
                object.put(EIGHTY_SONGS, count_eighty);
            }
            audioCursor_ninety = this.mContext.getContentResolver().query(AUDIO_URI, audioProjection, where_ninety, null, null);
            if (audioCursor_ninety != null) {
                JSONObject jSONObject = object;
                jSONObject.put(NINETY_SONGS, (audioCursor_ninety.getCount() - count_eighty) - count_seventy);
            }
            audioCursor_new_century = this.mContext.getContentResolver().query(AUDIO_URI, audioProjection, where_new_century, null, null);
            if (audioCursor_new_century != null) {
                object.put(NEW_CENTURY_SONGS, audioCursor_new_century.getCount());
            }
            if (audioCursor_seventy != null) {
                audioCursor_seventy.close();
            }
            if (audioCursor_eighty != null) {
                audioCursor_eighty.close();
            }
            if (audioCursor_ninety != null) {
                audioCursor_ninety.close();
            }
            if (audioCursor_new_century != null) {
                audioCursor_new_century.close();
            }
        } catch (JSONException e) {
            OPCollectLog.e(TAG, "json exception: " + e.getMessage());
            if (audioCursor_seventy != null) {
                audioCursor_seventy.close();
            }
            if (audioCursor_eighty != null) {
                audioCursor_eighty.close();
            }
            if (audioCursor_ninety != null) {
                audioCursor_ninety.close();
            }
            if (audioCursor_new_century != null) {
                audioCursor_new_century.close();
            }
        } catch (RuntimeException e2) {
            OPCollectLog.e(TAG, "runtime exception: " + e2.getMessage());
            if (audioCursor_seventy != null) {
                audioCursor_seventy.close();
            }
            if (audioCursor_eighty != null) {
                audioCursor_eighty.close();
            }
            if (audioCursor_ninety != null) {
                audioCursor_ninety.close();
            }
            if (audioCursor_new_century != null) {
                audioCursor_new_century.close();
            }
        } catch (Throwable th) {
            if (audioCursor_seventy != null) {
                audioCursor_seventy.close();
            }
            if (audioCursor_eighty != null) {
                audioCursor_eighty.close();
            }
            if (audioCursor_ninety != null) {
                audioCursor_ninety.close();
            }
            if (audioCursor_new_century != null) {
                audioCursor_new_century.close();
            }
        }
        OPCollectLog.d(TAG, "Years of music:" + object.toString());
        return object.toString();
    }
}
