package com.huawei.nb.notification;

import com.huawei.odmf.core.AManagedObject;
import java.util.ArrayList;
import java.util.List;

public abstract class RecordObserver implements ModelObserver {
    /* access modifiers changed from: protected */
    public abstract boolean isEqual(AManagedObject aManagedObject);

    public abstract void onRecordChanged(ChangeNotification changeNotification);

    @Override // com.huawei.nb.notification.ModelObserver
    public final void onModelChanged(ChangeNotification changeNotification) {
        if (changeNotification != null) {
            List insertItems = filterRecord(changeNotification.getInsertedItems());
            List updateItems = filterRecord(changeNotification.getUpdatedItems());
            List deleteItems = filterRecord(changeNotification.getDeletedItems());
            if (insertItems.size() != 0 || updateItems.size() != 0 || deleteItems.size() != 0) {
                onRecordChanged(new ChangeNotification(changeNotification.getType(), changeNotification.isDeleteAll(), insertItems, updateItems, deleteItems));
            }
        }
    }

    private List filterRecord(List src) {
        List<AManagedObject> dst = new ArrayList<>();
        if (src != null) {
            for (Object obj : src) {
                if (isEqual((AManagedObject) obj)) {
                    dst.add((AManagedObject) obj);
                }
            }
        }
        return dst;
    }
}
