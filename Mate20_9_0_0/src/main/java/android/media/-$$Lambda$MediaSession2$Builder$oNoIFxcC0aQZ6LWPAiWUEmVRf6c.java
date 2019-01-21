package android.media;

import android.content.Context;
import android.media.MediaSession2.Builder;
import android.media.update.ApiLoader;
import android.media.update.ProviderCreator;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaSession2$Builder$oNoIFxcC0aQZ6LWPAiWUEmVRf6c implements ProviderCreator {
    private final /* synthetic */ Context f$0;

    public /* synthetic */ -$$Lambda$MediaSession2$Builder$oNoIFxcC0aQZ6LWPAiWUEmVRf6c(Context context) {
        this.f$0 = context;
    }

    public final Object createProvider(Object obj) {
        return ApiLoader.getProvider().createMediaSession2Builder(this.f$0, (Builder) ((BuilderBase) obj));
    }
}
