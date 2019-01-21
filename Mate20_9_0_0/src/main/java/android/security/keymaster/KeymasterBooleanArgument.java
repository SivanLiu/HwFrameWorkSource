package android.security.keymaster;

import android.os.Parcel;

class KeymasterBooleanArgument extends KeymasterArgument {
    public final boolean value = true;

    public KeymasterBooleanArgument(int tag) {
        super(tag);
        if (KeymasterDefs.getTagType(tag) != KeymasterDefs.KM_BOOL) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad bool tag ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public KeymasterBooleanArgument(int tag, Parcel in) {
        super(tag);
    }

    public void writeValue(Parcel out) {
    }
}
