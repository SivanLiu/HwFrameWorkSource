package android.support.v4.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.CancellationSignal;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.ProviderResourceEntry;
import android.support.v4.content.res.ResourcesCompat.FontCallback;
import android.support.v4.os.BuildCompat;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.provider.FontsContractCompat.FontInfo;
import android.support.v4.util.LruCache;

@RestrictTo({Scope.LIBRARY_GROUP})
public class TypefaceCompat {
    private static final String TAG = "TypefaceCompat";
    private static final LruCache<String, Typeface> sTypefaceCache = new LruCache(16);
    private static final TypefaceCompatBaseImpl sTypefaceCompatImpl;

    static {
        if (BuildCompat.isAtLeastP()) {
            sTypefaceCompatImpl = new TypefaceCompatApi28Impl();
        } else if (VERSION.SDK_INT >= 26) {
            sTypefaceCompatImpl = new TypefaceCompatApi26Impl();
        } else if (VERSION.SDK_INT >= 24 && TypefaceCompatApi24Impl.isUsable()) {
            sTypefaceCompatImpl = new TypefaceCompatApi24Impl();
        } else if (VERSION.SDK_INT >= 21) {
            sTypefaceCompatImpl = new TypefaceCompatApi21Impl();
        } else {
            sTypefaceCompatImpl = new TypefaceCompatBaseImpl();
        }
    }

    private TypefaceCompat() {
    }

    @Nullable
    public static Typeface findFromCache(@NonNull Resources resources, int id, int style) {
        return (Typeface) sTypefaceCache.get(createResourceUid(resources, id, style));
    }

    private static String createResourceUid(Resources resources, int id, int style) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(resources.getResourcePackageName(id));
        stringBuilder.append("-");
        stringBuilder.append(id);
        stringBuilder.append("-");
        stringBuilder.append(style);
        return stringBuilder.toString();
    }

    @Nullable
    public static Typeface createFromResourcesFamilyXml(@NonNull Context context, @NonNull FamilyResourceEntry entry, @NonNull Resources resources, int id, int style, @Nullable FontCallback fontCallback, @Nullable Handler handler, boolean isRequestFromLayoutInflator) {
        Typeface typeface;
        Resources resources2;
        int i;
        FamilyResourceEntry familyResourceEntry = entry;
        FontCallback fontCallback2 = fontCallback;
        Handler handler2 = handler;
        if (familyResourceEntry instanceof ProviderResourceEntry) {
            ProviderResourceEntry providerEntry = (ProviderResourceEntry) familyResourceEntry;
            boolean isBlocking = isRequestFromLayoutInflator ? providerEntry.getFetchStrategy() != 0 : fontCallback2 != null;
            typeface = FontsContractCompat.getFontSync(context, providerEntry.getRequest(), fontCallback2, handler2, isBlocking, isRequestFromLayoutInflator ? providerEntry.getTimeout() : -1, style);
            Context context2 = context;
            resources2 = resources;
            i = style;
        } else {
            resources2 = resources;
            i = style;
            typeface = sTypefaceCompatImpl.createFromFontFamilyFilesResourceEntry(context, (FontFamilyFilesResourceEntry) familyResourceEntry, resources2, i);
            if (fontCallback2 != null) {
                if (typeface != null) {
                    fontCallback2.callbackSuccessAsync(typeface, handler2);
                } else {
                    fontCallback2.callbackFailAsync(-3, handler2);
                }
            }
        }
        if (typeface != null) {
            sTypefaceCache.put(createResourceUid(resources2, id, i), typeface);
        }
        return typeface;
    }

    @Nullable
    public static Typeface createFromResourcesFontFile(@NonNull Context context, @NonNull Resources resources, int id, String path, int style) {
        Typeface typeface = sTypefaceCompatImpl.createFromResourcesFontFile(context, resources, id, path, style);
        if (typeface != null) {
            sTypefaceCache.put(createResourceUid(resources, id, style), typeface);
        }
        return typeface;
    }

    @Nullable
    public static Typeface createFromFontInfo(@NonNull Context context, @Nullable CancellationSignal cancellationSignal, @NonNull FontInfo[] fonts, int style) {
        return sTypefaceCompatImpl.createFromFontInfo(context, cancellationSignal, fonts, style);
    }
}
