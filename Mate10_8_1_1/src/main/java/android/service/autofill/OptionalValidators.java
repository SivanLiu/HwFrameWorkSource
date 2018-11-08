package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;

final class OptionalValidators extends InternalValidator {
    public static final Creator<OptionalValidators> CREATOR = new Creator<OptionalValidators>() {
        public OptionalValidators createFromParcel(Parcel parcel) {
            return new OptionalValidators((InternalValidator[]) parcel.readParcelableArray(null, InternalValidator.class));
        }

        public OptionalValidators[] newArray(int size) {
            return new OptionalValidators[size];
        }
    };
    private final InternalValidator[] mValidators;

    OptionalValidators(InternalValidator[] validators) {
        this.mValidators = (InternalValidator[]) Preconditions.checkArrayElementsNotNull(validators, "validators");
    }

    public boolean isValid(ValueFinder finder) {
        for (InternalValidator validator : this.mValidators) {
            if (validator.isValid(finder)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        if (Helper.sDebug) {
            return "OptionalValidators: [validators=" + this.mValidators + "]";
        }
        return super.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelableArray(this.mValidators, flags);
    }
}
