package android.support.v4.os;

import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import java.util.Locale;

@RestrictTo({Scope.LIBRARY_GROUP})
final class LocaleHelper {
    static Locale forLanguageTag(String str) {
        String[] args;
        if (str.contains("-")) {
            args = str.split("-", -1);
            if (args.length > 2) {
                return new Locale(args[0], args[1], args[2]);
            }
            if (args.length > 1) {
                return new Locale(args[0], args[1]);
            }
            if (args.length == 1) {
                return new Locale(args[0]);
            }
        } else if (!str.contains("_")) {
            return new Locale(str);
        } else {
            args = str.split("_", -1);
            if (args.length > 2) {
                return new Locale(args[0], args[1], args[2]);
            }
            if (args.length > 1) {
                return new Locale(args[0], args[1]);
            }
            if (args.length == 1) {
                return new Locale(args[0]);
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can not parse language tag: [");
        stringBuilder.append(str);
        stringBuilder.append("]");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static String toLanguageTag(Locale locale) {
        StringBuilder buf = new StringBuilder();
        buf.append(locale.getLanguage());
        String country = locale.getCountry();
        if (!(country == null || country.isEmpty())) {
            buf.append("-");
            buf.append(locale.getCountry());
        }
        return buf.toString();
    }

    private LocaleHelper() {
    }
}
