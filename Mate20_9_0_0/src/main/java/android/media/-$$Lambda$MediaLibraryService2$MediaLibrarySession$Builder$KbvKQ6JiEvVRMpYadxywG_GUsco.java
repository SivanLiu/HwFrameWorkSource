package android.media;

import android.media.MediaLibraryService2.MediaLibrarySession.Builder;
import android.media.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import android.media.update.ApiLoader;
import android.media.update.ProviderCreator;
import java.util.concurrent.Executor;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MediaLibraryService2$MediaLibrarySession$Builder$KbvKQ6JiEvVRMpYadxywG_GUsco implements ProviderCreator {
    private final /* synthetic */ MediaLibraryService2 f$0;
    private final /* synthetic */ Executor f$1;
    private final /* synthetic */ MediaLibrarySessionCallback f$2;

    public /* synthetic */ -$$Lambda$MediaLibraryService2$MediaLibrarySession$Builder$KbvKQ6JiEvVRMpYadxywG_GUsco(MediaLibraryService2 mediaLibraryService2, Executor executor, MediaLibrarySessionCallback mediaLibrarySessionCallback) {
        this.f$0 = mediaLibraryService2;
        this.f$1 = executor;
        this.f$2 = mediaLibrarySessionCallback;
    }

    public final Object createProvider(Object obj) {
        return ApiLoader.getProvider().createMediaLibraryService2Builder(this.f$0, (Builder) ((BuilderBase) obj), this.f$1, this.f$2);
    }
}
