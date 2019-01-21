package android.icu.text;

import android.icu.util.ULocale;
import android.icu.util.ULocale.Type;
import java.text.Format;

public abstract class UFormat extends Format {
    private static final long serialVersionUID = -4964390515840164416L;
    private ULocale actualLocale;
    private ULocale validLocale;

    public final ULocale getLocale(Type type) {
        return type == ULocale.ACTUAL_LOCALE ? this.actualLocale : this.validLocale;
    }

    final void setLocale(ULocale valid, ULocale actual) {
        Object obj = null;
        Object obj2 = valid == null ? 1 : null;
        if (actual == null) {
            obj = 1;
        }
        if (obj2 == obj) {
            this.validLocale = valid;
            this.actualLocale = actual;
            return;
        }
        throw new IllegalArgumentException();
    }
}
