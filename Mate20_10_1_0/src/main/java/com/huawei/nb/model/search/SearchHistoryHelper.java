package com.huawei.nb.model.search;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class SearchHistoryHelper extends AEntityHelper<SearchHistory> {
    private static final SearchHistoryHelper INSTANCE = new SearchHistoryHelper();

    private SearchHistoryHelper() {
    }

    public static SearchHistoryHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, SearchHistory object) {
        Integer id = object.getId();
        if (id != null) {
            statement.bindLong(1, (long) id.intValue());
        } else {
            statement.bindNull(1);
        }
        String keyword = object.getKeyword();
        if (keyword != null) {
            statement.bindString(2, keyword);
        } else {
            statement.bindNull(2);
        }
        String searchTime = object.getSearchTime();
        if (searchTime != null) {
            statement.bindString(3, searchTime);
        } else {
            statement.bindNull(3);
        }
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public SearchHistory readObject(Cursor cursor, int offset) {
        return new SearchHistory(cursor);
    }

    public void setPrimaryKeyValue(SearchHistory object, long value) {
        object.setId(Integer.valueOf((int) value));
    }

    public Object getRelationshipObject(String field, SearchHistory object) {
        return null;
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public int getNumberOfRelationships() {
        return 0;
    }
}
