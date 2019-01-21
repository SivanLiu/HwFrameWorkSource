package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Slog;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextValueSanitizer extends InternalSanitizer implements Sanitizer, Parcelable {
    public static final Creator<TextValueSanitizer> CREATOR = new Creator<TextValueSanitizer>() {
        public TextValueSanitizer createFromParcel(Parcel parcel) {
            return new TextValueSanitizer((Pattern) parcel.readSerializable(), parcel.readString());
        }

        public TextValueSanitizer[] newArray(int size) {
            return new TextValueSanitizer[size];
        }
    };
    private static final String TAG = "TextValueSanitizer";
    private final Pattern mRegex;
    private final String mSubst;

    public TextValueSanitizer(Pattern regex, String subst) {
        this.mRegex = (Pattern) Preconditions.checkNotNull(regex);
        this.mSubst = (String) Preconditions.checkNotNull(subst);
    }

    public AutofillValue sanitize(AutofillValue value) {
        if (value == null) {
            Slog.w(TAG, "sanitize() called with null value");
            return null;
        } else if (value.isText()) {
            String str;
            StringBuilder stringBuilder;
            try {
                Matcher matcher = this.mRegex.matcher(value.getTextValue());
                if (matcher.matches()) {
                    return AutofillValue.forText(matcher.replaceAll(this.mSubst));
                }
                if (Helper.sDebug) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sanitize(): ");
                    stringBuilder.append(this.mRegex);
                    stringBuilder.append(" failed for ");
                    stringBuilder.append(value);
                    Slog.d(str, stringBuilder.toString());
                }
                return null;
            } catch (Exception e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception evaluating ");
                stringBuilder.append(this.mRegex);
                stringBuilder.append("/");
                stringBuilder.append(this.mSubst);
                stringBuilder.append(": ");
                stringBuilder.append(e);
                Slog.w(str, stringBuilder.toString());
                return null;
            }
        } else {
            if (Helper.sDebug) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("sanitize() called with non-text value: ");
                stringBuilder2.append(value);
                Slog.d(str2, stringBuilder2.toString());
            }
            return null;
        }
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TextValueSanitizer: [regex=");
        stringBuilder.append(this.mRegex);
        stringBuilder.append(", subst=");
        stringBuilder.append(this.mSubst);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeSerializable(this.mRegex);
        parcel.writeString(this.mSubst);
    }
}
