package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawAppProbeHelper extends AEntityHelper<RawAppProbe> {
    private static final RawAppProbeHelper INSTANCE = new RawAppProbeHelper();

    private RawAppProbeHelper() {
    }

    public static RawAppProbeHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, RawAppProbe object) {
        Integer mId = object.getMId();
        if (mId != null) {
            statement.bindLong(1, (long) mId.intValue());
        } else {
            statement.bindNull(1);
        }
        Date mTimeStamp = object.getMTimeStamp();
        if (mTimeStamp != null) {
            statement.bindLong(2, mTimeStamp.getTime());
        } else {
            statement.bindNull(2);
        }
        Integer mEventID = object.getMEventID();
        if (mEventID != null) {
            statement.bindLong(3, (long) mEventID.intValue());
        } else {
            statement.bindNull(3);
        }
        String mPackageName = object.getMPackageName();
        if (mPackageName != null) {
            statement.bindString(4, mPackageName);
        } else {
            statement.bindNull(4);
        }
        String mContent = object.getMContent();
        if (mContent != null) {
            statement.bindString(5, mContent);
        } else {
            statement.bindNull(5);
        }
        String mAppVersion = object.getMAppVersion();
        if (mAppVersion != null) {
            statement.bindString(6, mAppVersion);
        } else {
            statement.bindNull(6);
        }
        Integer mReservedInt = object.getMReservedInt();
        if (mReservedInt != null) {
            statement.bindLong(7, (long) mReservedInt.intValue());
        } else {
            statement.bindNull(7);
        }
        String mReservedText = object.getMReservedText();
        if (mReservedText != null) {
            statement.bindString(8, mReservedText);
        } else {
            statement.bindNull(8);
        }
    }

    public RawAppProbe readObject(Cursor cursor, int offset) {
        return new RawAppProbe(cursor);
    }

    public void setPrimaryKeyValue(RawAppProbe object, long value) {
        object.setMId(Integer.valueOf((int) value));
    }

    public Object getRelationshipObject(String field, RawAppProbe object) {
        return null;
    }

    public int getNumberOfRelationships() {
        return 0;
    }
}
