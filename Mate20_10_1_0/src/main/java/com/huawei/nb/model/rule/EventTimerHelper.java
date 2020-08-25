package com.huawei.nb.model.rule;

import android.database.Cursor;
import com.huawei.odmf.database.Statement;
import com.huawei.odmf.model.AEntityHelper;

public class EventTimerHelper extends AEntityHelper<EventTimer> {
    private static final EventTimerHelper INSTANCE = new EventTimerHelper();

    private EventTimerHelper() {
    }

    public static EventTimerHelper getInstance() {
        return INSTANCE;
    }

    public void bindValue(Statement statement, EventTimer object) {
        Long id = object.getId();
        if (id != null) {
            statement.bindLong(1, id.longValue());
        } else {
            statement.bindNull(1);
        }
        Long length = object.getLength();
        if (length != null) {
            statement.bindLong(2, length.longValue());
        } else {
            statement.bindNull(2);
        }
        Integer switchOn = object.getSwitchOn();
        if (switchOn != null) {
            statement.bindLong(3, (long) switchOn.intValue());
        } else {
            statement.bindNull(3);
        }
        Long ruleId = object.getRuleId();
        if (ruleId != null) {
            statement.bindLong(4, ruleId.longValue());
        } else {
            statement.bindNull(4);
        }
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public EventTimer readObject(Cursor cursor, int offset) {
        return new EventTimer(cursor);
    }

    public void setPrimaryKeyValue(EventTimer object, long value) {
        object.setId(Long.valueOf(value));
    }

    public Object getRelationshipObject(String field, EventTimer object) {
        return null;
    }

    @Override // com.huawei.odmf.model.AEntityHelper
    public int getNumberOfRelationships() {
        return 0;
    }
}
