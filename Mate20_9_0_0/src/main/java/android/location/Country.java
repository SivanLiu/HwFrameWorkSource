package android.location;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.SystemClock;
import java.util.Locale;

public class Country implements Parcelable {
    public static final int COUNTRY_SOURCE_LOCALE = 3;
    public static final int COUNTRY_SOURCE_LOCATION = 1;
    public static final int COUNTRY_SOURCE_NETWORK = 0;
    public static final int COUNTRY_SOURCE_SIM = 2;
    public static final Creator<Country> CREATOR = new Creator<Country>() {
        public Country createFromParcel(Parcel in) {
            return new Country(in.readString(), in.readInt(), in.readLong(), null);
        }

        public Country[] newArray(int size) {
            return new Country[size];
        }
    };
    public final String mCountryIso;
    private int mHashCode;
    private final int mSource;
    private final long mTimestamp;

    /* synthetic */ Country(String x0, int x1, long x2, AnonymousClass1 x3) {
        this(x0, x1, x2);
    }

    public Country(String countryIso, int source) {
        if (countryIso == null || source < 0 || source > 3) {
            throw new IllegalArgumentException();
        }
        this.mCountryIso = countryIso.toUpperCase(Locale.US);
        this.mSource = source;
        this.mTimestamp = SystemClock.elapsedRealtime();
    }

    private Country(String countryIso, int source, long timestamp) {
        if (countryIso == null || source < 0 || source > 3) {
            throw new IllegalArgumentException();
        }
        this.mCountryIso = countryIso.toUpperCase(Locale.US);
        this.mSource = source;
        this.mTimestamp = timestamp;
    }

    public Country(Country country) {
        this.mCountryIso = country.mCountryIso;
        this.mSource = country.mSource;
        this.mTimestamp = country.mTimestamp;
    }

    public final String getCountryIso() {
        return this.mCountryIso;
    }

    public final int getSource() {
        return this.mSource;
    }

    public final long getTimestamp() {
        return this.mTimestamp;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mCountryIso);
        parcel.writeInt(this.mSource);
        parcel.writeLong(this.mTimestamp);
    }

    public boolean equals(Object object) {
        boolean z = true;
        if (object == this) {
            return true;
        }
        if (!(object instanceof Country)) {
            return false;
        }
        Country c = (Country) object;
        if (!(this.mCountryIso.equals(c.getCountryIso()) && this.mSource == c.getSource())) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        if (this.mHashCode == 0) {
            this.mHashCode = (((17 * 13) + this.mCountryIso.hashCode()) * 13) + this.mSource;
        }
        return this.mHashCode;
    }

    public boolean equalsIgnoreSource(Country country) {
        return country != null && this.mCountryIso.equals(country.getCountryIso());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Country {ISO=");
        stringBuilder.append(this.mCountryIso);
        stringBuilder.append(", source=");
        stringBuilder.append(this.mSource);
        stringBuilder.append(", time=");
        stringBuilder.append(this.mTimestamp);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
