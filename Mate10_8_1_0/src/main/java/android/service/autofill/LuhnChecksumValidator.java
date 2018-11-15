package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;

public final class LuhnChecksumValidator extends InternalValidator implements Validator, Parcelable {
    public static final Creator<LuhnChecksumValidator> CREATOR = new Creator<LuhnChecksumValidator>() {
        public LuhnChecksumValidator createFromParcel(Parcel parcel) {
            return new LuhnChecksumValidator((AutofillId[]) parcel.readParcelableArray(null, AutofillId.class));
        }

        public LuhnChecksumValidator[] newArray(int size) {
            return new LuhnChecksumValidator[size];
        }
    };
    private static final String TAG = "LuhnChecksumValidator";
    private final AutofillId[] mIds;

    public LuhnChecksumValidator(AutofillId... ids) {
        this.mIds = (AutofillId[]) Preconditions.checkArrayElementsNotNull(ids, "ids");
    }

    private static boolean isLuhnChecksumValid(String number) {
        int sum = 0;
        boolean isDoubled = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - 48;
            if (digit >= 0 && digit <= 9) {
                int addend;
                if (isDoubled) {
                    addend = digit * 2;
                    if (addend > 9) {
                        addend -= 9;
                    }
                } else {
                    addend = digit;
                }
                sum += addend;
                isDoubled ^= 1;
            }
        }
        if (sum % 10 == 0) {
            return true;
        }
        return false;
    }

    public boolean isValid(ValueFinder finder) {
        if (this.mIds == null || this.mIds.length == 0) {
            return false;
        }
        StringBuilder number = new StringBuilder();
        for (AutofillId id : this.mIds) {
            String partialNumber = finder.findByAutofillId(id);
            if (partialNumber == null) {
                if (Helper.sDebug) {
                    Log.d(TAG, "No partial number for id " + id);
                }
                return false;
            }
            number.append(partialNumber);
        }
        return isLuhnChecksumValid(number.toString());
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelableArray(this.mIds, flags);
    }
}
