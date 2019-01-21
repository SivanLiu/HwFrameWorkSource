package android.service.autofill;

import android.icu.text.DateFormat;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.view.autofill.AutofillValue;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.util.Date;

public final class DateValueSanitizer extends InternalSanitizer implements Sanitizer, Parcelable {
    public static final Creator<DateValueSanitizer> CREATOR = new Creator<DateValueSanitizer>() {
        public DateValueSanitizer createFromParcel(Parcel parcel) {
            return new DateValueSanitizer((DateFormat) parcel.readSerializable());
        }

        public DateValueSanitizer[] newArray(int size) {
            return new DateValueSanitizer[size];
        }
    };
    private static final String TAG = "DateValueSanitizer";
    private final DateFormat mDateFormat;

    public DateValueSanitizer(DateFormat dateFormat) {
        this.mDateFormat = (DateFormat) Preconditions.checkNotNull(dateFormat);
    }

    public AutofillValue sanitize(AutofillValue value) {
        if (value == null) {
            Log.w(TAG, "sanitize() called with null value");
            return null;
        } else if (value.isDate()) {
            String converted;
            try {
                Date date = new Date(value.getDateValue());
                converted = this.mDateFormat.format(date);
                if (Helper.sDebug) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Transformed ");
                    stringBuilder.append(date);
                    stringBuilder.append(" to ");
                    stringBuilder.append(converted);
                    Log.d(str, stringBuilder.toString());
                }
                Date sanitized = this.mDateFormat.parse(converted);
                if (Helper.sDebug) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Sanitized to ");
                    stringBuilder2.append(sanitized);
                    Log.d(str2, stringBuilder2.toString());
                }
                return AutofillValue.forDate(sanitized.getTime());
            } catch (Exception e) {
                converted = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Could not apply ");
                stringBuilder3.append(this.mDateFormat);
                stringBuilder3.append(" to ");
                stringBuilder3.append(value);
                stringBuilder3.append(": ");
                stringBuilder3.append(e);
                Log.w(converted, stringBuilder3.toString());
                return null;
            }
        } else {
            if (Helper.sDebug) {
                String str3 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(value);
                stringBuilder4.append(" is not a date");
                Log.d(str3, stringBuilder4.toString());
            }
            return null;
        }
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DateValueSanitizer: [dateFormat=");
        stringBuilder.append(this.mDateFormat);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeSerializable(this.mDateFormat);
    }
}
