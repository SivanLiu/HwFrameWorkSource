package com.huawei.nb.notification;

import java.util.List;

public interface ModelUpdateObserver extends Observer {
    void onUpdate(List<Object> list);
}
