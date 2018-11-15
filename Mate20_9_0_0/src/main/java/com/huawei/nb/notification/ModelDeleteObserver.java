package com.huawei.nb.notification;

import java.util.List;

public interface ModelDeleteObserver extends Observer {
    void onDelete(List<Object> list);
}
