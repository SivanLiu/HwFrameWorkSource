package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.util.regex.Pattern;

public final class RegexValidator extends InternalValidator implements Validator, Parcelable {
    public static final Creator<RegexValidator> CREATOR = new Creator<RegexValidator>() {
        public RegexValidator createFromParcel(Parcel parcel) {
            return new RegexValidator((AutofillId) parcel.readParcelable(null), (Pattern) parcel.readSerializable());
        }

        public RegexValidator[] newArray(int size) {
            return new RegexValidator[size];
        }
    };
    private static final String TAG = "RegexValidator";
    private final AutofillId mId;
    private final Pattern mRegex;

    public RegexValidator(AutofillId id, Pattern regex) {
        this.mId = (AutofillId) Preconditions.checkNotNull(id);
        this.mRegex = (Pattern) Preconditions.checkNotNull(regex);
    }

    public boolean isValid(ValueFinder finder) {
        String value = finder.findByAutofillId(this.mId);
        if (value == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No view for id ");
            stringBuilder.append(this.mId);
            Log.w(str, stringBuilder.toString());
            return false;
        }
        boolean valid = this.mRegex.matcher(value).matches();
        if (Helper.sDebug) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isValid(): ");
            stringBuilder2.append(valid);
            Log.d(str2, stringBuilder2.toString());
        }
        return valid;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RegexValidator: [id=");
        stringBuilder.append(this.mId);
        stringBuilder.append(", regex=");
        stringBuilder.append(this.mRegex);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(this.mId, flags);
        parcel.writeSerializable(this.mRegex);
    }
}
