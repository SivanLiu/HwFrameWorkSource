package android.hardware.camera2.impl;

import android.hardware.camera2.impl.CameraDeviceImpl.CameraDeviceCallbacks;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CameraDeviceImpl$CameraDeviceCallbacks$Sm85frAzwGZVMAK-NE_gwckYXVQ implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$CameraDeviceImpl$CameraDeviceCallbacks$Sm85frAzwGZVMAK-NE_gwckYXVQ INSTANCE = new -$$Lambda$CameraDeviceImpl$CameraDeviceCallbacks$Sm85frAzwGZVMAK-NE_gwckYXVQ();

    private /* synthetic */ -$$Lambda$CameraDeviceImpl$CameraDeviceCallbacks$Sm85frAzwGZVMAK-NE_gwckYXVQ() {
    }

    public final void accept(Object obj, Object obj2) {
        ((CameraDeviceCallbacks) obj).notifyError(((Integer) obj2).intValue());
    }
}
