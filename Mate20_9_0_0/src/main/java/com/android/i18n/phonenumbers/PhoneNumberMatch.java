package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import gov.nist.core.Separators;
import java.util.Arrays;

public final class PhoneNumberMatch {
    private final PhoneNumber number;
    private final String rawString;
    private final int start;

    PhoneNumberMatch(int start, String rawString, PhoneNumber number) {
        if (start < 0) {
            throw new IllegalArgumentException("Start index must be >= 0.");
        } else if (rawString == null || number == null) {
            throw new NullPointerException();
        } else {
            this.start = start;
            this.rawString = rawString;
            this.number = number;
        }
    }

    public PhoneNumber number() {
        return this.number;
    }

    public int start() {
        return this.start;
    }

    public int end() {
        return this.start + this.rawString.length();
    }

    public String rawString() {
        return this.rawString;
    }

    public int hashCode() {
        return Arrays.hashCode(new Object[]{Integer.valueOf(this.start), this.rawString, this.number});
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PhoneNumberMatch)) {
            return false;
        }
        PhoneNumberMatch other = (PhoneNumberMatch) obj;
        if (!(this.rawString.equals(other.rawString) && this.start == other.start && this.number.equals(other.number))) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PhoneNumberMatch [");
        stringBuilder.append(start());
        stringBuilder.append(Separators.COMMA);
        stringBuilder.append(end());
        stringBuilder.append(") ");
        stringBuilder.append(this.rawString);
        return stringBuilder.toString();
    }
}
