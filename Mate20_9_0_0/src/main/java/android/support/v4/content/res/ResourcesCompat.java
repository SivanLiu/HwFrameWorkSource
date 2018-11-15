package android.support.v4.content.res;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.FontRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import android.support.v4.graphics.TypefaceCompat;
import android.support.v4.util.Preconditions;
import android.util.Log;
import android.util.TypedValue;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

public final class ResourcesCompat {
    private static final String TAG = "ResourcesCompat";

    public static abstract class FontCallback {
        public abstract void onFontRetrievalFailed(int i);

        public abstract void onFontRetrieved(@NonNull Typeface typeface);

        @RestrictTo({Scope.LIBRARY_GROUP})
        public final void callbackSuccessAsync(final Typeface typeface, @Nullable Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrieved(typeface);
                }
            });
        }

        @RestrictTo({Scope.LIBRARY_GROUP})
        public final void callbackFailAsync(final int reason, @Nullable Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrievalFailed(reason);
                }
            });
        }
    }

    @Nullable
    public static Drawable getDrawable(@NonNull Resources res, @DrawableRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 21) {
            return res.getDrawable(id, theme);
        }
        return res.getDrawable(id);
    }

    @Nullable
    public static Drawable getDrawableForDensity(@NonNull Resources res, @DrawableRes int id, int density, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 21) {
            return res.getDrawableForDensity(id, density, theme);
        }
        if (VERSION.SDK_INT >= 15) {
            return res.getDrawableForDensity(id, density);
        }
        return res.getDrawable(id);
    }

    @ColorInt
    public static int getColor(@NonNull Resources res, @ColorRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 23) {
            return res.getColor(id, theme);
        }
        return res.getColor(id);
    }

    @Nullable
    public static ColorStateList getColorStateList(@NonNull Resources res, @ColorRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 23) {
            return res.getColorStateList(id, theme);
        }
        return res.getColorStateList(id);
    }

    @Nullable
    public static Typeface getFont(@NonNull Context context, @FontRes int id) throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, new TypedValue(), 0, null, null, false);
    }

    public static void getFont(@NonNull Context context, @FontRes int id, @NonNull FontCallback fontCallback, @Nullable Handler handler) throws NotFoundException {
        Preconditions.checkNotNull(fontCallback);
        if (context.isRestricted()) {
            fontCallback.callbackFailAsync(-4, handler);
            return;
        }
        loadFont(context, id, new TypedValue(), 0, fontCallback, handler, false);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public static Typeface getFont(@NonNull Context context, @FontRes int id, TypedValue value, int style, @Nullable FontCallback fontCallback) throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, value, style, fontCallback, null, true);
    }

    private static Typeface loadFont(@NonNull Context context, int id, TypedValue value, int style, @Nullable FontCallback fontCallback, @Nullable Handler handler, boolean isRequestFromLayoutInflator) {
        Resources resources = context.getResources();
        resources.getValue(id, value, true);
        Typeface typeface = loadFont(context, resources, value, id, style, fontCallback, handler, isRequestFromLayoutInflator);
        if (typeface != null || fontCallback != null) {
            return typeface;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Font resource ID #0x");
        stringBuilder.append(Integer.toHexString(id));
        stringBuilder.append(" could not be retrieved.");
        throw new NotFoundException(stringBuilder.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Typeface loadFont(@NonNull Context context, Resources wrapper, TypedValue value, int id, int style, @Nullable FontCallback fontCallback, @Nullable Handler handler, boolean isRequestFromLayoutInflator) {
        XmlPullParserException e;
        Context context2;
        Typeface typeface;
        String str;
        StringBuilder stringBuilder;
        IOException e2;
        Resources resources = wrapper;
        TypedValue typedValue = value;
        int i = id;
        int i2 = style;
        FontCallback fontCallback2 = fontCallback;
        Handler handler2 = handler;
        if (typedValue.string != null) {
            String file = typedValue.string.toString();
            if (file.startsWith("res/")) {
                Typeface typeface2 = TypefaceCompat.findFromCache(resources, i, i2);
                if (typeface2 != null) {
                    if (fontCallback2 != null) {
                        fontCallback2.callbackSuccessAsync(typeface2, handler2);
                    }
                    return typeface2;
                }
                int i3;
                try {
                    if (file.toLowerCase().endsWith(".xml")) {
                        try {
                            FamilyResourceEntry familyEntry = FontResourcesParserCompat.parse(resources.getXml(i), resources);
                            if (familyEntry == null) {
                                try {
                                    Log.e(TAG, "Failed to find font-family tag");
                                    if (fontCallback2 != null) {
                                        fontCallback2.callbackFailAsync(-3, handler2);
                                    }
                                    return null;
                                } catch (XmlPullParserException e3) {
                                    e = e3;
                                    context2 = context;
                                    typeface = typeface2;
                                    i3 = -3;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to parse xml resource ");
                                    stringBuilder.append(file);
                                    Log.e(str, stringBuilder.toString(), e);
                                    if (fontCallback2 != null) {
                                    }
                                    return null;
                                } catch (IOException e4) {
                                    e2 = e4;
                                    context2 = context;
                                    typeface = typeface2;
                                    i3 = -3;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to read xml resource ");
                                    stringBuilder.append(file);
                                    Log.e(str, stringBuilder.toString(), e2);
                                    if (fontCallback2 != null) {
                                    }
                                    return null;
                                }
                            }
                            i3 = -3;
                            try {
                                return TypefaceCompat.createFromResourcesFamilyXml(context, familyEntry, resources, i, i2, fontCallback2, handler2, isRequestFromLayoutInflator);
                            } catch (XmlPullParserException e5) {
                                e = e5;
                                context2 = context;
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to parse xml resource ");
                                stringBuilder.append(file);
                                Log.e(str, stringBuilder.toString(), e);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            } catch (IOException e6) {
                                e2 = e6;
                                context2 = context;
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to read xml resource ");
                                stringBuilder.append(file);
                                Log.e(str, stringBuilder.toString(), e2);
                                if (fontCallback2 != null) {
                                }
                                return null;
                            }
                        } catch (XmlPullParserException e7) {
                            e = e7;
                            typeface = typeface2;
                            i3 = -3;
                            context2 = context;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to parse xml resource ");
                            stringBuilder.append(file);
                            Log.e(str, stringBuilder.toString(), e);
                            if (fontCallback2 != null) {
                            }
                            return null;
                        } catch (IOException e8) {
                            e2 = e8;
                            typeface = typeface2;
                            i3 = -3;
                            context2 = context;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to read xml resource ");
                            stringBuilder.append(file);
                            Log.e(str, stringBuilder.toString(), e2);
                            if (fontCallback2 != null) {
                            }
                            return null;
                        }
                    }
                    typeface = typeface2;
                    i3 = -3;
                    try {
                        typeface2 = TypefaceCompat.createFromResourcesFontFile(context, resources, i, file, i2);
                        if (fontCallback2 != null) {
                            if (typeface2 != null) {
                                try {
                                    fontCallback2.callbackSuccessAsync(typeface2, handler2);
                                } catch (XmlPullParserException e9) {
                                    e = e9;
                                    typeface = typeface2;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to parse xml resource ");
                                    stringBuilder.append(file);
                                    Log.e(str, stringBuilder.toString(), e);
                                    if (fontCallback2 != null) {
                                    }
                                    return null;
                                } catch (IOException e10) {
                                    e2 = e10;
                                    typeface = typeface2;
                                    str = TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to read xml resource ");
                                    stringBuilder.append(file);
                                    Log.e(str, stringBuilder.toString(), e2);
                                    if (fontCallback2 != null) {
                                    }
                                    return null;
                                }
                            }
                            fontCallback2.callbackFailAsync(i3, handler2);
                        }
                        return typeface2;
                    } catch (XmlPullParserException e11) {
                        e = e11;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to parse xml resource ");
                        stringBuilder.append(file);
                        Log.e(str, stringBuilder.toString(), e);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    } catch (IOException e12) {
                        e2 = e12;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to read xml resource ");
                        stringBuilder.append(file);
                        Log.e(str, stringBuilder.toString(), e2);
                        if (fontCallback2 != null) {
                        }
                        return null;
                    }
                } catch (XmlPullParserException e13) {
                    e = e13;
                    context2 = context;
                    typeface = typeface2;
                    i3 = -3;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to parse xml resource ");
                    stringBuilder.append(file);
                    Log.e(str, stringBuilder.toString(), e);
                    if (fontCallback2 != null) {
                        fontCallback2.callbackFailAsync(i3, handler2);
                    }
                    return null;
                } catch (IOException e14) {
                    e2 = e14;
                    context2 = context;
                    typeface = typeface2;
                    i3 = -3;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to read xml resource ");
                    stringBuilder.append(file);
                    Log.e(str, stringBuilder.toString(), e2);
                    if (fontCallback2 != null) {
                    }
                    return null;
                }
            }
            if (fontCallback2 != null) {
                fontCallback2.callbackFailAsync(-3, handler2);
            }
            return null;
        }
        context2 = context;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Resource \"");
        stringBuilder2.append(resources.getResourceName(i));
        stringBuilder2.append("\" (");
        stringBuilder2.append(Integer.toHexString(id));
        stringBuilder2.append(") is not a Font: ");
        stringBuilder2.append(value);
        throw new NotFoundException(stringBuilder2.toString());
    }

    private ResourcesCompat() {
    }
}
