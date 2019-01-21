package android.hardware.soundtrigger;

import android.util.ArraySet;
import java.util.Locale;

public class KeyphraseMetadata {
    public final int id;
    public final String keyphrase;
    public final int recognitionModeFlags;
    public final ArraySet<Locale> supportedLocales;

    public KeyphraseMetadata(int id, String keyphrase, ArraySet<Locale> supportedLocales, int recognitionModeFlags) {
        this.id = id;
        this.keyphrase = keyphrase;
        this.supportedLocales = supportedLocales;
        this.recognitionModeFlags = recognitionModeFlags;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("id=");
        stringBuilder.append(this.id);
        stringBuilder.append(", keyphrase=");
        stringBuilder.append(this.keyphrase);
        stringBuilder.append(", supported-locales=");
        stringBuilder.append(this.supportedLocales);
        stringBuilder.append(", recognition-modes=");
        stringBuilder.append(this.recognitionModeFlags);
        return stringBuilder.toString();
    }

    public boolean supportsPhrase(String phrase) {
        return this.keyphrase.isEmpty() || this.keyphrase.equalsIgnoreCase(phrase);
    }

    public boolean supportsLocale(Locale locale) {
        return this.supportedLocales.isEmpty() || this.supportedLocales.contains(locale);
    }
}
