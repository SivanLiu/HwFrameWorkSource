package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class RawAppProbeCountHelper extends AEntityHelper<RawAppProbeCount> {
    private static final RawAppProbeCountHelper INSTANCE = new RawAppProbeCountHelper();

    private RawAppProbeCountHelper() {
    }

    public static RawAppProbeCountHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, RawAppProbeCount object) {
        Integer mId = object.getMId();
        if (mId != null) {
            statement.bindLong(1, (long) mId.intValue());
        } else {
            statement.bindNull(1);
        }
        String mPackageName = object.getMPackageName();
        if (mPackageName != null) {
            statement.bindString(2, mPackageName);
        } else {
            statement.bindNull(2);
        }
        Integer mCount = object.getMCount();
        if (mCount != null) {
            statement.bindLong(3, (long) mCount.intValue());
        } else {
            statement.bindNull(3);
        }
        Integer mEventID = object.getMEventID();
        if (mEventID != null) {
            statement.bindLong(4, (long) mEventID.intValue());
        } else {
            statement.bindNull(4);
        }
        Integer mReservedInt = object.getMReservedInt();
        if (mReservedInt != null) {
            statement.bindLong(5, (long) mReservedInt.intValue());
        } else {
            statement.bindNull(5);
        }
        String mReservedText = object.getMReservedText();
        if (mReservedText != null) {
            statement.bindString(6, mReservedText);
        } else {
            statement.bindNull(6);
        }
    }

    public RawAppProbeCount readObject(Cursor cursor, int offset) {
        return new RawAppProbeCount(cursor);
    }

    public void setPrimaryKeyValue(RawAppProbeCount object, long value) {
        object.setMId(Integer.valueOf((int) value));
    }

    public Object getRelationshipObject(String field, RawAppProbeCount object) {
        return null;
    }

    public int getNumberOfRelationships() {
        return 0;
    }
}
