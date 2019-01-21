package android.media.update;

import android.graphics.Bitmap;
import android.media.MediaMetadata2;
import android.media.MediaMetadata2.Builder;
import android.media.Rating2;
import android.os.Bundle;
import java.util.Set;

public interface MediaMetadata2Provider {

    public interface BuilderProvider {
        MediaMetadata2 build_impl();

        Builder putBitmap_impl(String str, Bitmap bitmap);

        Builder putFloat_impl(String str, float f);

        Builder putLong_impl(String str, long j);

        Builder putRating_impl(String str, Rating2 rating2);

        Builder putString_impl(String str, String str2);

        Builder putText_impl(String str, CharSequence charSequence);

        Builder setExtras_impl(Bundle bundle);
    }

    boolean containsKey_impl(String str);

    Bitmap getBitmap_impl(String str);

    Bundle getExtras_impl();

    float getFloat_impl(String str);

    long getLong_impl(String str);

    String getMediaId_impl();

    Rating2 getRating_impl(String str);

    String getString_impl(String str);

    CharSequence getText_impl(String str);

    Set<String> keySet_impl();

    int size_impl();

    Bundle toBundle_impl();
}
