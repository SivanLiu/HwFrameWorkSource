package android.graphics.drawable;

import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.Source;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BitmapDrawable$T1BUUqQwU4Z6Ve8DJHFuQvYohkY implements OnHeaderDecodedListener {
    public static final /* synthetic */ -$$Lambda$BitmapDrawable$T1BUUqQwU4Z6Ve8DJHFuQvYohkY INSTANCE = new -$$Lambda$BitmapDrawable$T1BUUqQwU4Z6Ve8DJHFuQvYohkY();

    private /* synthetic */ -$$Lambda$BitmapDrawable$T1BUUqQwU4Z6Ve8DJHFuQvYohkY() {
    }

    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageInfo imageInfo, Source source) {
        imageDecoder.setAllocator(1);
    }
}
