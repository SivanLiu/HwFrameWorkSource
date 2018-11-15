package com.huawei.nb.model.search;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class SearchConfigHelper extends AEntityHelper<SearchConfig> {
    private static final SearchConfigHelper INSTANCE = new SearchConfigHelper();

    private SearchConfigHelper() {
    }

    public static SearchConfigHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, SearchConfig object) {
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

    public SearchConfig readObject(Cursor cursor, int offset) {
        return new SearchConfig(cursor);
    }

    public void setPrimaryKeyValue(SearchConfig object, long value) {
        object.setId(Integer.valueOf((int) value));
    }

    public Object getRelationshipObject(String field, SearchConfig object) {
        return null;
    }

    public int getNumberOfRelationships() {
        return 0;
    }
}
