package android.support.v4.graphics;

import android.graphics.Typeface;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

@RequiresApi(28)
@RestrictTo({Scope.LIBRARY_GROUP})
public class TypefaceCompatApi28Impl extends TypefaceCompatApi26Impl {
    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD = "createFromFamiliesWithDefault";
    private static final String DEFAULT_FAMILY = "sans-serif";
    private static final int RESOLVE_BY_FONT_TABLE = -1;
    private static final String TAG = "TypefaceCompatApi28Impl";

    /* JADX WARNING: Removed duplicated region for block: B:3:0x002d A:{Splitter: B:0:0x0000, ExcHandler: java.lang.IllegalAccessException (r0_2 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:3:0x002d, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0033, code:
            throw new java.lang.RuntimeException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Array.set(Array.newInstance(this.mFontFamily, 1), 0, family);
            return (Typeface) this.mCreateFromFamiliesWithDefault.invoke(null, new Object[]{familyArray, DEFAULT_FAMILY, Integer.valueOf(-1), Integer.valueOf(-1)});
        } catch (ReflectiveOperationException e) {
        }
    }

    protected Method obtainCreateFromFamiliesWithDefaultMethod(Class fontFamily) throws NoSuchMethodException {
        Object familyArray = Array.newInstance(fontFamily, 1);
        Method m = Typeface.class.getDeclaredMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD, new Class[]{familyArray.getClass(), String.class, Integer.TYPE, Integer.TYPE});
        m.setAccessible(true);
        return m;
    }
}
