package android.graphics.drawable;

import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.Source;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Drawable$wmqxcnFJRLY7tFDmv2eEGR5vtvU implements OnHeaderDecodedListener {
    public static final /* synthetic */ -$$Lambda$Drawable$wmqxcnFJRLY7tFDmv2eEGR5vtvU INSTANCE = new -$$Lambda$Drawable$wmqxcnFJRLY7tFDmv2eEGR5vtvU();

    private /* synthetic */ -$$Lambda$Drawable$wmqxcnFJRLY7tFDmv2eEGR5vtvU() {
    }

    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageInfo imageInfo, Source source) {
        imageDecoder.setAllocator(1);
    }
}
