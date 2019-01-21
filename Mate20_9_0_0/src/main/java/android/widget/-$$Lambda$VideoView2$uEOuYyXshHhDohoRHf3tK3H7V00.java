package android.widget;

import android.media.update.ApiLoader;
import android.media.update.ViewGroupHelper;
import android.media.update.ViewGroupHelper.ProviderCreator;
import android.media.update.ViewGroupProvider;
import android.util.AttributeSet;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$VideoView2$uEOuYyXshHhDohoRHf3tK3H7V00 implements ProviderCreator {
    private final /* synthetic */ AttributeSet f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$VideoView2$uEOuYyXshHhDohoRHf3tK3H7V00(AttributeSet attributeSet, int i, int i2) {
        this.f$0 = attributeSet;
        this.f$1 = i;
        this.f$2 = i2;
    }

    public final ViewGroupProvider createProvider(ViewGroupHelper viewGroupHelper, ViewGroupProvider viewGroupProvider, ViewGroupProvider viewGroupProvider2) {
        return ApiLoader.getProvider().createVideoView2((VideoView2) viewGroupHelper, viewGroupProvider, viewGroupProvider2, this.f$0, this.f$1, this.f$2);
    }
}
