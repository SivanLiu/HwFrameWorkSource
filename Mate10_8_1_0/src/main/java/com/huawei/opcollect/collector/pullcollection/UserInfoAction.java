package com.huawei.opcollect.collector.pullcollection;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import com.huawei.nb.model.collectencrypt.RawUserInfoStatistic;
import com.huawei.opcollect.odmf.OdmfCollectScheduler;
import com.huawei.opcollect.strategy.Action;
import com.huawei.opcollect.utils.OPCollectConstant;
import com.huawei.opcollect.utils.OPCollectLog;
import com.huawei.opcollect.utils.OPCollectUtils;

public class UserInfoAction extends Action {
    private static final Uri AUDIO_URI = Media.EXTERNAL_CONTENT_URI;
    private static final int CALL_DIAL_TYPE = 2;
    private static final int CALL_RECEIVE_TYPE = 1;
    private static final Uri CALL_URI = Calls.CONTENT_URI;
    private static final Uri CONTACTS_URI = Contacts.CONTENT_URI;
    private static final long DAY_IN_MILLISECOND = 86400000;
    private static final Uri IMAGE_URI = Images.Media.EXTERNAL_CONTENT_URI;
    private static final String TAG = "UserInfoAction";
    private static final Uri VIDEO_URI = Video.Media.EXTERNAL_CONTENT_URI;
    private static UserInfoAction sInstance = null;
    private Collection mCollection;

    public static synchronized UserInfoAction getInstance(Context context) {
        UserInfoAction userInfoAction;
        synchronized (UserInfoAction.class) {
            if (sInstance == null) {
                sInstance = new UserInfoAction(context, OPCollectConstant.USER_INFO_ACTION_NAME);
            }
            userInfoAction = sInstance;
        }
        return userInfoAction;
    }

    private UserInfoAction(Context context, String name) {
        super(context, name);
        this.mCollection = null;
        this.mCollection = new Collection();
        setDailyRecordNum(queryDailyRecordNum(RawUserInfoStatistic.class));
    }

    public boolean destroy() {
        super.destroy();
        destroyInstance();
        return true;
    }

    private static synchronized void destroyInstance() {
        synchronized (UserInfoAction.class) {
            sInstance = null;
        }
    }

    protected boolean execute() {
        return collectRawUserData();
    }

    private boolean collectRawUserData() {
        OdmfCollectScheduler.getInstance().getDataHandler().obtainMessage(4, getRawUserInfoStatistic()).sendToTarget();
        return true;
    }

    private RawUserInfoStatistic getRawUserInfoStatistic() {
        RawUserInfoStatistic rawUserInfoStatistic = new RawUserInfoStatistic();
        rawUserInfoStatistic.setMTimeStamp(OPCollectUtils.getCurrentTime());
        rawUserInfoStatistic.setMContactNum(Integer.valueOf(getContactsNumber()));
        rawUserInfoStatistic.setMVideoNum(Integer.valueOf(getVideoNumber()));
        rawUserInfoStatistic.setMMusicNum(Integer.valueOf(getAudioNumber()));
        rawUserInfoStatistic.setMPhotoNum(Integer.valueOf(getImageNumber()));
        setCallStatisticPerDay(rawUserInfoStatistic);
        rawUserInfoStatistic.setMMobileDataSurplus(Double.valueOf((double) getMobileLeftBytes()));
        rawUserInfoStatistic.setMMobileDataTotal(Double.valueOf((double) getTodayMobileTotalBytes()));
        rawUserInfoStatistic.setMWifiDataTotal(Double.valueOf((double) getTodayWifiTotalBytes()));
        rawUserInfoStatistic.setMReservedInt(Integer.valueOf(0));
        rawUserInfoStatistic.setMReservedText(OPCollectUtils.formatCurrentTime());
        return rawUserInfoStatistic;
    }

    private int getContactsNumber() {
        int count = 0;
        if (this.mContext == null) {
            return 0;
        }
        Cursor contactsCursor = null;
        try {
            contactsCursor = this.mContext.getContentResolver().query(CONTACTS_URI, null, null, null, null);
        } catch (RuntimeException e) {
            OPCollectLog.e(TAG, "query contacts uri failed: " + e.getMessage());
        }
        if (contactsCursor != null) {
            count = contactsCursor.getCount();
        }
        if (contactsCursor != null) {
            contactsCursor.close();
        }
        return count;
    }

    private int getVideoNumber() {
        int count = 0;
        if (this.mContext == null) {
            return 0;
        }
        Cursor videoCursor = null;
        try {
            videoCursor = this.mContext.getContentResolver().query(VIDEO_URI, new String[]{"_id"}, null, null, null);
        } catch (RuntimeException e) {
            OPCollectLog.e(TAG, "query video uri failed: " + e.getMessage());
        }
        if (videoCursor != null) {
            count = videoCursor.getCount();
        }
        if (videoCursor != null) {
            videoCursor.close();
        }
        return count;
    }

    private int getImageNumber() {
        int count = 0;
        if (this.mContext == null) {
            return 0;
        }
        Cursor imageCursor = null;
        try {
            imageCursor = this.mContext.getContentResolver().query(IMAGE_URI, new String[]{"_id"}, "_data LIKE '%DCIM/Camera%'", null, null);
        } catch (RuntimeException e) {
            OPCollectLog.e(TAG, "query image uri failed: " + e.getMessage());
        }
        if (imageCursor != null) {
            count = imageCursor.getCount();
        }
        if (imageCursor != null) {
            imageCursor.close();
        }
        return count;
    }

    private int getAudioNumber() {
        int count = 0;
        if (this.mContext == null) {
            return 0;
        }
        Cursor audioCursor = null;
        try {
            audioCursor = this.mContext.getContentResolver().query(AUDIO_URI, new String[]{"_id"}, null, null, null);
        } catch (RuntimeException e) {
            OPCollectLog.e(TAG, "query audio uri failed: " + e.getMessage());
        }
        if (audioCursor != null) {
            count = audioCursor.getCount();
        }
        if (audioCursor != null) {
            audioCursor.close();
        }
        return count;
    }

    private long getTodayMobileTotalBytes() {
        return NetAssistantManager.getInstance(this.mContext).getTodayMobileTotalBytes(this.mCollection.getDefaultDataSlotIMSI(this.mContext));
    }

    private long getMobileLeftBytes() {
        try {
            return NetAssistantManager.getInstance(this.mContext).getMobileLeftBytes(this.mCollection.getDefaultDataSlotIMSI(this.mContext));
        } catch (RemoteException e) {
            OPCollectLog.e(TAG, "RemoteException:" + e.getMessage());
            return 0;
        }
    }

    private long getTodayWifiTotalBytes() {
        return NetAssistantManager.getInstance(this.mContext).getTodayWifiTotalBytes();
    }

    private void setCallStatisticPerDay(RawUserInfoStatistic rawUserInfoStatistic) {
        if (this.mContext != null) {
            int dialTime = 0;
            int receiveTime = 0;
            long duration = 0;
            long dayBeforeNow = System.currentTimeMillis() - DAY_IN_MILLISECOND;
            Cursor callCursor = null;
            try {
                callCursor = this.mContext.getContentResolver().query(CALL_URI, null, "date>?", new String[]{String.valueOf(dayBeforeNow)}, null);
            } catch (RuntimeException e) {
                OPCollectLog.e(TAG, "query call uri failed: " + e.getMessage());
            }
            if (callCursor != null) {
                while (callCursor.moveToNext()) {
                    switch (callCursor.getInt(callCursor.getColumnIndex("type"))) {
                        case 1:
                            receiveTime++;
                            break;
                        case 2:
                            dialTime++;
                            break;
                        default:
                            break;
                    }
                    duration += callCursor.getLong(callCursor.getColumnIndex("duration"));
                }
                callCursor.close();
                rawUserInfoStatistic.setMCallDialNum(Integer.valueOf(dialTime));
                rawUserInfoStatistic.setMCallRecvNum(Integer.valueOf(receiveTime));
                rawUserInfoStatistic.setMCallDurationTime(Integer.valueOf((int) duration));
            }
        }
    }
}
