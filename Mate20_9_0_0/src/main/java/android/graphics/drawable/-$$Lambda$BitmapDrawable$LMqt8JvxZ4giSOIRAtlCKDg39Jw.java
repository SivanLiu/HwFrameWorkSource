package android.graphics.drawable;

import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.Source;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BitmapDrawable$LMqt8JvxZ4giSOIRAtlCKDg39Jw implements OnHeaderDecodedListener {
    public static final /* synthetic */ -$$Lambda$BitmapDrawable$LMqt8JvxZ4giSOIRAtlCKDg39Jw INSTANCE = new -$$Lambda$BitmapDrawable$LMqt8JvxZ4giSOIRAtlCKDg39Jw();

    private /* synthetic */ -$$Lambda$BitmapDrawable$LMqt8JvxZ4giSOIRAtlCKDg39Jw() {
    }

    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageInfo imageInfo, Source source) {
        imageDecoder.setAllocator(1);
    }
}
