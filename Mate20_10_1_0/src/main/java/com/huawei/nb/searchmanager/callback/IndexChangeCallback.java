package com.huawei.nb.searchmanager.callback;

import com.huawei.nb.searchmanager.callback.IIndexChangeCallback;
import com.huawei.nb.searchmanager.client.listener.IIndexChangeListener;
import com.huawei.nb.searchmanager.client.model.ChangedIndexContent;
import java.util.Objects;

public class IndexChangeCallback extends IIndexChangeCallback.Stub {
    private IIndexChangeListener indexChangeListener;

    public IndexChangeCallback(IIndexChangeListener indexChangeListener2) {
        this.indexChangeListener = indexChangeListener2;
    }

    @Override // com.huawei.nb.searchmanager.callback.IIndexChangeCallback
    public void onDataChanged(String pkgName, ChangedIndexContent changedIndexContent) {
        if (this.indexChangeListener != null) {
            this.indexChangeListener.onDataChanged(pkgName, changedIndexContent);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass() || !(obj instanceof IndexChangeCallback)) {
            return false;
        }
        return Objects.equals(this.indexChangeListener, ((IndexChangeCallback) obj).indexChangeListener);
    }

    public int hashCode() {
        return Objects.hash(this.indexChangeListener);
    }
}
