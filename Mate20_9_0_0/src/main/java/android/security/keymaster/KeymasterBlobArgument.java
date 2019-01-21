package android.security.keymaster;

import android.os.Parcel;

class KeymasterBlobArgument extends KeymasterArgument {
    public final byte[] blob;

    public KeymasterBlobArgument(int tag, byte[] blob) {
        super(tag);
        int tagType = KeymasterDefs.getTagType(tag);
        if (tagType == Integer.MIN_VALUE || tagType == KeymasterDefs.KM_BYTES) {
            this.blob = blob;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad blob tag ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public KeymasterBlobArgument(int tag, Parcel in) {
        super(tag);
        this.blob = in.createByteArray();
    }

    public void writeValue(Parcel out) {
        out.writeByteArray(this.blob);
    }
}
