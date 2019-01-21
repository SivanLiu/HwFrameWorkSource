package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;

final class NegationValidator extends InternalValidator {
    public static final Creator<NegationValidator> CREATOR = new Creator<NegationValidator>() {
        public NegationValidator createFromParcel(Parcel parcel) {
            return new NegationValidator((InternalValidator) parcel.readParcelable(null));
        }

        public NegationValidator[] newArray(int size) {
            return new NegationValidator[size];
        }
    };
    private final InternalValidator mValidator;

    NegationValidator(InternalValidator validator) {
        this.mValidator = (InternalValidator) Preconditions.checkNotNull(validator);
    }

    public boolean isValid(ValueFinder finder) {
        return this.mValidator.isValid(finder) ^ 1;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NegationValidator: [validator=");
        stringBuilder.append(this.mValidator);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mValidator, flags);
    }
}
