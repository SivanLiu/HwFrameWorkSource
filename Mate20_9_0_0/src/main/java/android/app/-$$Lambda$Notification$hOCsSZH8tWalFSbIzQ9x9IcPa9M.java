package android.app;

import android.app.PendingIntent.OnMarshaledListener;
import android.os.Parcel;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Notification$hOCsSZH8tWalFSbIzQ9x9IcPa9M implements OnMarshaledListener {
    private final /* synthetic */ Notification f$0;
    private final /* synthetic */ Parcel f$1;

    public /* synthetic */ -$$Lambda$Notification$hOCsSZH8tWalFSbIzQ9x9IcPa9M(Notification notification, Parcel parcel) {
        this.f$0 = notification;
        this.f$1 = parcel;
    }

    public final void onMarshaled(PendingIntent pendingIntent, Parcel parcel, int i) {
        Notification.lambda$writeToParcel$0(this.f$0, this.f$1, pendingIntent, parcel, i);
    }
}
