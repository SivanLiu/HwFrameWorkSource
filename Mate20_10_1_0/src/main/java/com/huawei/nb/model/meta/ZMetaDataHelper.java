package com.huawei.nb.model.meta;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class ZMetaDataHelper extends AEntityHelper<ZMetaData> {
    private static final ZMetaDataHelper INSTANCE = new ZMetaDataHelper();

    private ZMetaDataHelper() {
    }

    public static ZMetaDataHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, ZMetaData object) {
        Integer id = object.getId();
        if (id != null) {
            statement.bindLong(1, (long) id.intValue());
        } else {
            statement.bindNull(1);
        }
        String name = object.getName();
        if (name != null) {
            statement.bindString(2, name);
        } else {
            statement.bindNull(2);
        }
        String value = object.getValue();
        if (value != null) {
            statement.bindString(3, value);
        } else {
            statement.bindNull(3);
        }
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public ZMetaData readObject(Cursor cursor, int offset) {
        return new ZMetaData(cursor);
    }

    public void setPrimaryKeyValue(ZMetaData object, long value) {
        object.setId(Integer.valueOf((int) value));
    }

    public Object getRelationshipObject(String field, ZMetaData object) {
        return null;
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public int getNumberOfRelationships() {
        return 0;
    }
}
