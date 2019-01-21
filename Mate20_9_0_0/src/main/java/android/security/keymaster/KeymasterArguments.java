package android.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KeymasterArguments implements Parcelable {
    public static final Creator<KeymasterArguments> CREATOR = new Creator<KeymasterArguments>() {
        public KeymasterArguments createFromParcel(Parcel in) {
            return new KeymasterArguments(in, null);
        }

        public KeymasterArguments[] newArray(int size) {
            return new KeymasterArguments[size];
        }
    };
    public static final long UINT32_MAX_VALUE = 4294967295L;
    private static final long UINT32_RANGE = 4294967296L;
    public static final BigInteger UINT64_MAX_VALUE = UINT64_RANGE.subtract(BigInteger.ONE);
    private static final BigInteger UINT64_RANGE = BigInteger.ONE.shiftLeft(64);
    private List<KeymasterArgument> mArguments;

    /* synthetic */ KeymasterArguments(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public KeymasterArguments() {
        this.mArguments = new ArrayList();
    }

    private KeymasterArguments(Parcel in) {
        this.mArguments = in.createTypedArrayList(KeymasterArgument.CREATOR);
    }

    public void addEnum(int tag, int value) {
        int tagType = KeymasterDefs.getTagType(tag);
        if (tagType == 268435456 || tagType == 536870912) {
            addEnumTag(tag, value);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not an enum or repeating enum tag: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void addEnums(int tag, int... values) {
        if (KeymasterDefs.getTagType(tag) == 536870912) {
            for (int value : values) {
                addEnumTag(tag, value);
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not a repeating enum tag: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int getEnum(int tag, int defaultValue) {
        if (KeymasterDefs.getTagType(tag) == 268435456) {
            KeymasterArgument arg = getArgumentByTag(tag);
            if (arg == null) {
                return defaultValue;
            }
            return getEnumTagValue(arg);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not an enum tag: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public List<Integer> getEnums(int tag) {
        if (KeymasterDefs.getTagType(tag) == 536870912) {
            List<Integer> values = new ArrayList();
            for (KeymasterArgument arg : this.mArguments) {
                if (arg.tag == tag) {
                    values.add(Integer.valueOf(getEnumTagValue(arg)));
                }
            }
            return values;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not a repeating enum tag: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void addEnumTag(int tag, int value) {
        this.mArguments.add(new KeymasterIntArgument(tag, value));
    }

    private int getEnumTagValue(KeymasterArgument arg) {
        return ((KeymasterIntArgument) arg).value;
    }

    public void addUnsignedInt(int tag, long value) {
        int tagType = KeymasterDefs.getTagType(tag);
        StringBuilder stringBuilder;
        if (tagType != 805306368 && tagType != 1073741824) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not an int or repeating int tag: ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (value < 0 || value > 4294967295L) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Int tag value out of range: ");
            stringBuilder.append(value);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            this.mArguments.add(new KeymasterIntArgument(tag, (int) value));
        }
    }

    public long getUnsignedInt(int tag, long defaultValue) {
        if (KeymasterDefs.getTagType(tag) == 805306368) {
            KeymasterArgument arg = getArgumentByTag(tag);
            if (arg == null) {
                return defaultValue;
            }
            return ((long) ((KeymasterIntArgument) arg).value) & 4294967295L;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not an int tag: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void addUnsignedLong(int tag, BigInteger value) {
        int tagType = KeymasterDefs.getTagType(tag);
        if (tagType == KeymasterDefs.KM_ULONG || tagType == KeymasterDefs.KM_ULONG_REP) {
            addLongTag(tag, value);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not a long or repeating long tag: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public List<BigInteger> getUnsignedLongs(int tag) {
        if (KeymasterDefs.getTagType(tag) == KeymasterDefs.KM_ULONG_REP) {
            List<BigInteger> values = new ArrayList();
            for (KeymasterArgument arg : this.mArguments) {
                if (arg.tag == tag) {
                    values.add(getLongTagValue(arg));
                }
            }
            return values;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tag is not a repeating long: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void addLongTag(int tag, BigInteger value) {
        if (value.signum() == -1 || value.compareTo(UINT64_MAX_VALUE) > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Long tag value out of range: ");
            stringBuilder.append(value);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.mArguments.add(new KeymasterLongArgument(tag, value.longValue()));
    }

    private BigInteger getLongTagValue(KeymasterArgument arg) {
        return toUint64(((KeymasterLongArgument) arg).value);
    }

    public void addBoolean(int tag) {
        if (KeymasterDefs.getTagType(tag) == KeymasterDefs.KM_BOOL) {
            this.mArguments.add(new KeymasterBooleanArgument(tag));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not a boolean tag: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean getBoolean(int tag) {
        if (KeymasterDefs.getTagType(tag) != KeymasterDefs.KM_BOOL) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not a boolean tag: ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (getArgumentByTag(tag) == null) {
            return false;
        } else {
            return true;
        }
    }

    public void addBytes(int tag, byte[] value) {
        if (KeymasterDefs.getTagType(tag) != KeymasterDefs.KM_BYTES) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not a bytes tag: ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (value != null) {
            this.mArguments.add(new KeymasterBlobArgument(tag, value));
        } else {
            throw new NullPointerException("value == nulll");
        }
    }

    public byte[] getBytes(int tag, byte[] defaultValue) {
        if (KeymasterDefs.getTagType(tag) == KeymasterDefs.KM_BYTES) {
            KeymasterArgument arg = getArgumentByTag(tag);
            if (arg == null) {
                return defaultValue;
            }
            return ((KeymasterBlobArgument) arg).blob;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Not a bytes tag: ");
        stringBuilder.append(tag);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void addDate(int tag, Date value) {
        StringBuilder stringBuilder;
        if (KeymasterDefs.getTagType(tag) != KeymasterDefs.KM_DATE) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not a date tag: ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (value == null) {
            throw new NullPointerException("value == nulll");
        } else if (value.getTime() >= 0) {
            this.mArguments.add(new KeymasterDateArgument(tag, value));
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Date tag value out of range: ");
            stringBuilder.append(value);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void addDateIfNotNull(int tag, Date value) {
        if (KeymasterDefs.getTagType(tag) != KeymasterDefs.KM_DATE) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not a date tag: ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (value != null) {
            addDate(tag, value);
        }
    }

    public Date getDate(int tag, Date defaultValue) {
        if (KeymasterDefs.getTagType(tag) == KeymasterDefs.KM_DATE) {
            KeymasterArgument arg = getArgumentByTag(tag);
            if (arg == null) {
                return defaultValue;
            }
            Date result = ((KeymasterDateArgument) arg).date;
            if (result.getTime() >= 0) {
                return result;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Tag value too large. Tag: ");
            stringBuilder.append(tag);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Tag is not a date type: ");
        stringBuilder2.append(tag);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private KeymasterArgument getArgumentByTag(int tag) {
        for (KeymasterArgument arg : this.mArguments) {
            if (arg.tag == tag) {
                return arg;
            }
        }
        return null;
    }

    public boolean containsTag(int tag) {
        return getArgumentByTag(tag) != null;
    }

    public int size() {
        return this.mArguments.size();
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeTypedList(this.mArguments);
    }

    public void readFromParcel(Parcel in) {
        in.readTypedList(this.mArguments, KeymasterArgument.CREATOR);
    }

    public int describeContents() {
        return 0;
    }

    public static BigInteger toUint64(long value) {
        if (value >= 0) {
            return BigInteger.valueOf(value);
        }
        return BigInteger.valueOf(value).add(UINT64_RANGE);
    }
}
