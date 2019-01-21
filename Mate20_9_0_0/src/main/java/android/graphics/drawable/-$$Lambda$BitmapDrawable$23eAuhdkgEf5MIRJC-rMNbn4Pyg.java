package android.graphics.drawable;

import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.Source;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BitmapDrawable$23eAuhdkgEf5MIRJC-rMNbn4Pyg implements OnHeaderDecodedListener {
    public static final /* synthetic */ -$$Lambda$BitmapDrawable$23eAuhdkgEf5MIRJC-rMNbn4Pyg INSTANCE = new -$$Lambda$BitmapDrawable$23eAuhdkgEf5MIRJC-rMNbn4Pyg();

    private /* synthetic */ -$$Lambda$BitmapDrawable$23eAuhdkgEf5MIRJC-rMNbn4Pyg() {
    }

    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageInfo imageInfo, Source source) {
        imageDecoder.setAllocator(1);
    }
}
