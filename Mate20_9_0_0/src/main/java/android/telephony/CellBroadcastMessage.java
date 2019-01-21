package android.telephony;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.format.DateUtils;

public class CellBroadcastMessage implements Parcelable {
    public static final Creator<CellBroadcastMessage> CREATOR = new Creator<CellBroadcastMessage>() {
        public CellBroadcastMessage createFromParcel(Parcel in) {
            return new CellBroadcastMessage(in, null);
        }

        public CellBroadcastMessage[] newArray(int size) {
            return new CellBroadcastMessage[size];
        }
    };
    public static final String SMS_CB_MESSAGE_EXTRA = "com.android.cellbroadcastreceiver.SMS_CB_MESSAGE";
    private final long mDeliveryTime;
    private boolean mIsRead;
    private final SmsCbMessage mSmsCbMessage;
    private int mSubId;

    /* synthetic */ CellBroadcastMessage(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public void setSubId(int subId) {
        this.mSubId = subId;
    }

    public int getSubId() {
        return this.mSubId;
    }

    public CellBroadcastMessage(SmsCbMessage message) {
        this.mSubId = 0;
        this.mSmsCbMessage = message;
        this.mDeliveryTime = System.currentTimeMillis();
        this.mIsRead = false;
    }

    private CellBroadcastMessage(SmsCbMessage message, long deliveryTime, boolean isRead) {
        this.mSubId = 0;
        this.mSmsCbMessage = message;
        this.mDeliveryTime = deliveryTime;
        this.mIsRead = isRead;
    }

    private CellBroadcastMessage(Parcel in) {
        boolean z = false;
        this.mSubId = 0;
        this.mSmsCbMessage = new SmsCbMessage(in);
        this.mDeliveryTime = in.readLong();
        if (in.readInt() != 0) {
            z = true;
        }
        this.mIsRead = z;
        this.mSubId = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        this.mSmsCbMessage.writeToParcel(out, flags);
        out.writeLong(this.mDeliveryTime);
        out.writeInt(this.mIsRead);
        out.writeInt(this.mSubId);
    }

    public static CellBroadcastMessage createFromCursor(Cursor cursor) {
        String plmn;
        int lac;
        SmsCbEtwsInfo etwsInfo;
        SmsCbCmasInfo cmasInfo;
        Cursor cursor2 = cursor;
        int geoScope = cursor2.getInt(cursor2.getColumnIndexOrThrow("geo_scope"));
        int serialNum = cursor2.getInt(cursor2.getColumnIndexOrThrow("serial_number"));
        int category = cursor2.getInt(cursor2.getColumnIndexOrThrow("service_category"));
        String language = cursor2.getString(cursor2.getColumnIndexOrThrow("language"));
        String body = cursor2.getString(cursor2.getColumnIndexOrThrow("body"));
        int format = cursor2.getInt(cursor2.getColumnIndexOrThrow("format"));
        int priority = cursor2.getInt(cursor2.getColumnIndexOrThrow("priority"));
        int plmnColumn = cursor2.getColumnIndex("plmn");
        if (plmnColumn == -1 || cursor2.isNull(plmnColumn)) {
            plmn = null;
        } else {
            plmn = cursor2.getString(plmnColumn);
        }
        String plmn2 = plmn;
        int lacColumn = cursor2.getColumnIndex("lac");
        if (lacColumn == -1 || cursor2.isNull(lacColumn)) {
            lac = -1;
        } else {
            lac = cursor2.getInt(lacColumn);
        }
        int lac2 = lac;
        int cidColumn = cursor2.getColumnIndex("cid");
        if (cidColumn == -1 || cursor2.isNull(cidColumn)) {
            lac = -1;
        } else {
            lac = cursor2.getInt(cidColumn);
        }
        SmsCbLocation location = new SmsCbLocation(plmn2, lac2, lac);
        int etwsWarningTypeColumn = cursor2.getColumnIndex("etws_warning_type");
        if (etwsWarningTypeColumn == -1 || cursor2.isNull(etwsWarningTypeColumn)) {
            etwsInfo = null;
        } else {
            etwsInfo = new SmsCbEtwsInfo(cursor2.getInt(etwsWarningTypeColumn), false, false, false, null);
        }
        int cmasMessageClassColumn = cursor2.getColumnIndex("cmas_message_class");
        int cmasMessageClassColumn2;
        if (cmasMessageClassColumn == -1 || cursor2.isNull(cmasMessageClassColumn)) {
            cmasMessageClassColumn2 = cmasMessageClassColumn;
            cmasInfo = null;
        } else {
            int cmasCategory;
            int responseType;
            int severity;
            int urgency;
            int messageClass = cursor2.getInt(cmasMessageClassColumn);
            int cmasCategoryColumn = cursor2.getColumnIndex("cmas_category");
            cmasMessageClassColumn2 = cmasMessageClassColumn;
            if (cmasCategoryColumn == -1 || cursor2.isNull(cmasCategoryColumn) != 0) {
                cmasCategory = -1;
            } else {
                cmasCategory = cursor2.getInt(cmasCategoryColumn);
            }
            cmasMessageClassColumn = cursor2.getColumnIndex("cmas_response_type");
            if (cmasMessageClassColumn == -1 || cursor2.isNull(cmasMessageClassColumn)) {
                responseType = -1;
            } else {
                responseType = cursor2.getInt(cmasMessageClassColumn);
            }
            cmasCategoryColumn = cursor2.getColumnIndex("cmas_severity");
            int cmasResponseTypeColumn = cmasMessageClassColumn;
            if (cmasCategoryColumn == -1 || cursor2.isNull(cmasCategoryColumn) != 0) {
                severity = -1;
            } else {
                severity = cursor2.getInt(cmasCategoryColumn);
            }
            cmasMessageClassColumn = cursor2.getColumnIndex("cmas_urgency");
            if (cmasMessageClassColumn == -1 || cursor2.isNull(cmasMessageClassColumn)) {
                urgency = -1;
            } else {
                urgency = cursor2.getInt(cmasMessageClassColumn);
            }
            cmasCategoryColumn = cursor2.getColumnIndex("cmas_certainty");
            int cmasUrgencyColumn = cmasMessageClassColumn;
            cmasMessageClassColumn = -1;
            if (!(cmasCategoryColumn == -1 || cursor2.isNull(cmasCategoryColumn))) {
                cmasMessageClassColumn = cursor2.getInt(cmasCategoryColumn);
            }
            SmsCbCmasInfo smsCbCmasInfo = new SmsCbCmasInfo(messageClass, cmasCategory, responseType, severity, urgency, cmasMessageClassColumn);
        }
        return new CellBroadcastMessage(new SmsCbMessage(format, geoScope, serialNum, location, category, language, body, priority, etwsInfo, cmasInfo), cursor2.getLong(cursor2.getColumnIndexOrThrow("date")), cursor2.getInt(cursor2.getColumnIndexOrThrow("read")) != 0);
    }

    public ContentValues getContentValues() {
        ContentValues cv = new ContentValues(16);
        SmsCbMessage msg = this.mSmsCbMessage;
        cv.put("geo_scope", Integer.valueOf(msg.getGeographicalScope()));
        SmsCbLocation location = msg.getLocation();
        if (location.getPlmn() != null) {
            cv.put("plmn", location.getPlmn());
        }
        if (location.getLac() != -1) {
            cv.put("lac", Integer.valueOf(location.getLac()));
        }
        if (location.getCid() != -1) {
            cv.put("cid", Integer.valueOf(location.getCid()));
        }
        cv.put("serial_number", Integer.valueOf(msg.getSerialNumber()));
        cv.put("service_category", Integer.valueOf(msg.getServiceCategory()));
        cv.put("language", msg.getLanguageCode());
        cv.put("body", msg.getMessageBody());
        cv.put("date", Long.valueOf(this.mDeliveryTime));
        cv.put("read", Boolean.valueOf(this.mIsRead));
        cv.put("format", Integer.valueOf(msg.getMessageFormat()));
        cv.put("priority", Integer.valueOf(msg.getMessagePriority()));
        SmsCbEtwsInfo etwsInfo = this.mSmsCbMessage.getEtwsWarningInfo();
        if (etwsInfo != null) {
            cv.put("etws_warning_type", Integer.valueOf(etwsInfo.getWarningType()));
        }
        SmsCbCmasInfo cmasInfo = this.mSmsCbMessage.getCmasWarningInfo();
        if (cmasInfo != null) {
            cv.put("cmas_message_class", Integer.valueOf(cmasInfo.getMessageClass()));
            cv.put("cmas_category", Integer.valueOf(cmasInfo.getCategory()));
            cv.put("cmas_response_type", Integer.valueOf(cmasInfo.getResponseType()));
            cv.put("cmas_severity", Integer.valueOf(cmasInfo.getSeverity()));
            cv.put("cmas_urgency", Integer.valueOf(cmasInfo.getUrgency()));
            cv.put("cmas_certainty", Integer.valueOf(cmasInfo.getCertainty()));
        }
        return cv;
    }

    public void setIsRead(boolean isRead) {
        this.mIsRead = isRead;
    }

    public String getLanguageCode() {
        return this.mSmsCbMessage.getLanguageCode();
    }

    public int getServiceCategory() {
        return this.mSmsCbMessage.getServiceCategory();
    }

    public long getDeliveryTime() {
        return this.mDeliveryTime;
    }

    public String getMessageBody() {
        return this.mSmsCbMessage.getMessageBody();
    }

    public boolean isRead() {
        return this.mIsRead;
    }

    public int getSerialNumber() {
        return this.mSmsCbMessage.getSerialNumber();
    }

    public SmsCbCmasInfo getCmasWarningInfo() {
        return this.mSmsCbMessage.getCmasWarningInfo();
    }

    public SmsCbEtwsInfo getEtwsWarningInfo() {
        return this.mSmsCbMessage.getEtwsWarningInfo();
    }

    public boolean isPublicAlertMessage() {
        return this.mSmsCbMessage.isEmergencyMessage();
    }

    public boolean isEmergencyAlertMessage() {
        return this.mSmsCbMessage.isEmergencyMessage();
    }

    public boolean isEtwsMessage() {
        return this.mSmsCbMessage.isEtwsMessage();
    }

    public boolean isCmasMessage() {
        return this.mSmsCbMessage.isCmasMessage();
    }

    public int getCmasMessageClass() {
        if (this.mSmsCbMessage.isCmasMessage()) {
            return this.mSmsCbMessage.getCmasWarningInfo().getMessageClass();
        }
        return -1;
    }

    public boolean isEtwsPopupAlert() {
        SmsCbEtwsInfo etwsInfo = this.mSmsCbMessage.getEtwsWarningInfo();
        return etwsInfo != null && etwsInfo.isPopupAlert();
    }

    public boolean isEtwsEmergencyUserAlert() {
        SmsCbEtwsInfo etwsInfo = this.mSmsCbMessage.getEtwsWarningInfo();
        return etwsInfo != null && etwsInfo.isEmergencyUserAlert();
    }

    public boolean isEtwsTestMessage() {
        SmsCbEtwsInfo etwsInfo = this.mSmsCbMessage.getEtwsWarningInfo();
        return etwsInfo != null && etwsInfo.getWarningType() == 3;
    }

    public String getDateString(Context context) {
        return DateUtils.formatDateTime(context, this.mDeliveryTime, 527121);
    }

    public String getSpokenDateString(Context context) {
        return DateUtils.formatDateTime(context, this.mDeliveryTime, 17);
    }
}
