package android.content.res;

import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.Source;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ResourcesImpl$99dm2ENnzo9b0SIUjUj2Kl3pi90 implements OnHeaderDecodedListener {
    public static final /* synthetic */ -$$Lambda$ResourcesImpl$99dm2ENnzo9b0SIUjUj2Kl3pi90 INSTANCE = new -$$Lambda$ResourcesImpl$99dm2ENnzo9b0SIUjUj2Kl3pi90();

    private /* synthetic */ -$$Lambda$ResourcesImpl$99dm2ENnzo9b0SIUjUj2Kl3pi90() {
    }

    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageInfo imageInfo, Source source) {
        imageDecoder.setAllocator(1);
    }
}
