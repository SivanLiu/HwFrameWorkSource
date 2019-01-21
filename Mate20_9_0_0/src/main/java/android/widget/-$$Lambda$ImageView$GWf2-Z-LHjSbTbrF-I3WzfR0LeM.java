package android.widget;

import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.ImageInfo;
import android.graphics.ImageDecoder.OnHeaderDecodedListener;
import android.graphics.ImageDecoder.Source;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImageView$GWf2-Z-LHjSbTbrF-I3WzfR0LeM implements OnHeaderDecodedListener {
    public static final /* synthetic */ -$$Lambda$ImageView$GWf2-Z-LHjSbTbrF-I3WzfR0LeM INSTANCE = new -$$Lambda$ImageView$GWf2-Z-LHjSbTbrF-I3WzfR0LeM();

    private /* synthetic */ -$$Lambda$ImageView$GWf2-Z-LHjSbTbrF-I3WzfR0LeM() {
    }

    public final void onHeaderDecoded(ImageDecoder imageDecoder, ImageInfo imageInfo, Source source) {
        imageDecoder.setAllocator(1);
    }
}
