package android.graphics.drawable;

import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.Source;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AnimatedImageDrawable$Cgt3NliB7ZYUONyDd-eQGdYbEKc implements OnHeaderDecodedListener {
    public static final /* synthetic */ -$$Lambda$AnimatedImageDrawable$Cgt3NliB7ZYUONyDd-eQGdYbEKc INSTANCE = new -$$Lambda$AnimatedImageDrawable$Cgt3NliB7ZYUONyDd-eQGdYbEKc();

    private /* synthetic */ -$$Lambda$AnimatedImageDrawable$Cgt3NliB7ZYUONyDd-eQGdYbEKc() {
    }

    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageInfo imageInfo, Source source) {
        AnimatedImageDrawable.lambda$updateStateFromTypedArray$0(imageDecoder, imageInfo, source);
    }
}
