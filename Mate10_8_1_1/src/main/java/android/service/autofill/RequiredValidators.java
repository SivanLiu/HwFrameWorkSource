package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;

final class RequiredValidators extends InternalValidator {
    public static final Creator<RequiredValidators> CREATOR = new Creator<RequiredValidators>() {
        public RequiredValidators createFromParcel(Parcel parcel) {
            return new RequiredValidators((InternalValidator[]) parcel.readParcelableArray(null, InternalValidator.class));
        }

        public RequiredValidators[] newArray(int size) {
            return new RequiredValidators[size];
        }
    };
    private final InternalValidator[] mValidators;

    RequiredValidators(InternalValidator[] validators) {
        this.mValidators = (InternalValidator[]) Preconditions.checkArrayElementsNotNull(validators, "validators");
    }

    public boolean isValid(ValueFinder finder) {
        for (InternalValidator validator : this.mValidators) {
            if (!validator.isValid(finder)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        if (Helper.sDebug) {
            return "RequiredValidators: [validators=" + this.mValidators + "]";
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
