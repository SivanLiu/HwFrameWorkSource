package defpackage;

import com.huawei.android.feature.tasks.OnFailureListener;
import com.huawei.android.feature.tasks.OnSuccessListener;
import java.util.concurrent.CountDownLatch;

/* renamed from: aa  reason: default package */
public final class aa implements OnFailureListener, OnSuccessListener<Object> {
    public final CountDownLatch F;

    private aa() {
        this.F = new CountDownLatch(1);
    }

    public /* synthetic */ aa(byte b) {
        this();
    }

    @Override // com.huawei.android.feature.tasks.OnFailureListener
    public final void onFailure(Exception exc) {
        this.F.countDown();
    }

    @Override // com.huawei.android.feature.tasks.OnSuccessListener
    public final void onSuccess(Object obj) {
        this.F.countDown();
    }
}
