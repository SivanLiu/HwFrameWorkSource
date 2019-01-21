package android.graphics.drawable;

import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.Source;
import android.graphics.Rect;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NinePatchDrawable$yQvfm7FAkslD5wdGFysjgwt8cLE implements OnHeaderDecodedListener {
    private final /* synthetic */ Rect f$0;

    public /* synthetic */ -$$Lambda$NinePatchDrawable$yQvfm7FAkslD5wdGFysjgwt8cLE(Rect rect) {
        this.f$0 = rect;
    }

    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageInfo imageInfo, Source source) {
        NinePatchDrawable.lambda$updateStateFromTypedArray$0(this.f$0, imageDecoder, imageInfo, source);
    }
}
