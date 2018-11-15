package com.huawei.nb.notification;

import java.util.List;

public interface ModelInsertObserver extends Observer {
    void onInsert(List<Object> list);
}
