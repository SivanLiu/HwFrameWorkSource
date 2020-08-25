package com.huawei.nb.model.pengine;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class RoutineCoreDataHelper extends AEntityHelper<RoutineCoreData> {
    private static final RoutineCoreDataHelper INSTANCE = new RoutineCoreDataHelper();

    private RoutineCoreDataHelper() {
    }

    public static RoutineCoreDataHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, RoutineCoreData object) {
        Integer id = object.getId();
        if (id != null) {
            statement.bindLong(1, (long) id.intValue());
        } else {
            statement.bindNull(1);
        }
        Long timestamp = object.getTimestamp();
        if (timestamp != null) {
            statement.bindLong(2, timestamp.longValue());
        } else {
            statement.bindNull(2);
        }
        String coreData = object.getCoreData();
        if (coreData != null) {
            statement.bindString(3, coreData);
        } else {
            statement.bindNull(3);
        }
        String column0 = object.getColumn0();
        if (column0 != null) {
            statement.bindString(4, column0);
        } else {
            statement.bindNull(4);
        }
        String column1 = object.getColumn1();
        if (column1 != null) {
            statement.bindString(5, column1);
        } else {
            statement.bindNull(5);
        }
        String column2 = object.getColumn2();
        if (column2 != null) {
            statement.bindString(6, column2);
        } else {
            statement.bindNull(6);
        }
        String column3 = object.getColumn3();
        if (column3 != null) {
            statement.bindString(7, column3);
        } else {
            statement.bindNull(7);
        }
        String column4 = object.getColumn4();
        if (column4 != null) {
            statement.bindString(8, column4);
        } else {
            statement.bindNull(8);
        }
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public RoutineCoreData readObject(Cursor cursor, int offset) {
        return new RoutineCoreData(cursor);
    }

    public void setPrimaryKeyValue(RoutineCoreData object, long value) {
        object.setId(Integer.valueOf((int) value));
    }

    public Object getRelationshipObject(String field, RoutineCoreData object) {
        return null;
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public int getNumberOfRelationships() {
        return 0;
    }
}
