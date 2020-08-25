package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class HiPlayUserAction1Helper extends AEntityHelper<HiPlayUserAction1> {
    private static final HiPlayUserAction1Helper INSTANCE = new HiPlayUserAction1Helper();

    private HiPlayUserAction1Helper() {
    }

    public static HiPlayUserAction1Helper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, HiPlayUserAction1 object) {
        Long id = object.getId();
        if (id != null) {
            statement.bindLong(1, id.longValue());
        } else {
            statement.bindNull(1);
        }
        String business = object.getBusiness();
        if (business != null) {
            statement.bindString(2, business);
        } else {
            statement.bindNull(2);
        }
        String sub_business = object.getSub_business();
        if (sub_business != null) {
            statement.bindString(3, sub_business);
        } else {
            statement.bindNull(3);
        }
        Long timestamp = object.getTimestamp();
        if (timestamp != null) {
            statement.bindLong(4, timestamp.longValue());
        } else {
            statement.bindNull(4);
        }
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public HiPlayUserAction1 readObject(Cursor cursor, int offset) {
        return new HiPlayUserAction1(cursor);
    }

    public void setPrimaryKeyValue(HiPlayUserAction1 object, long value) {
        object.setId(Long.valueOf(value));
    }

    public Object getRelationshipObject(String field, HiPlayUserAction1 object) {
        return null;
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public int getNumberOfRelationships() {
        return 0;
    }
}
