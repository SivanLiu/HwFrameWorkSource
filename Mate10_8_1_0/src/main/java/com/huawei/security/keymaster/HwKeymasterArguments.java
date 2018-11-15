package com.huawei.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HwKeymasterArguments implements Parcelable {
    public static final Creator<HwKeymasterArguments> CREATOR = new Creator<HwKeymasterArguments>() {
        public HwKeymasterArguments createFromParcel(Parcel in) {
            return new HwKeymasterArguments(in);
        }

        public HwKeymasterArguments[] newArray(int size) {
            return new HwKeymasterArguments[size];
        }
    };
    public static final long UINT32_MAX_VALUE = 4294967295L;
    private static final long UINT32_RANGE = 4294967296L;
    public static final BigInteger UINT64_MAX_VALUE = UINT64_RANGE.subtract(BigInteger.ONE);
    private static final BigInteger UINT64_RANGE = BigInteger.ONE.shiftLeft(64);
    private List<HwKeymasterArgument> mArguments;

    public HwKeymasterArguments() {
        this.mArguments = new ArrayList();
    }

    private HwKeymasterArguments(Parcel in) {
        this.mArguments = in.createTypedArrayList(HwKeymasterArgument.CREATOR);
    }

    public void addEnum(int tag, int value) {
        int tagType = HwKeymasterDefs.getTagType(tag);
        if (tagType == HwKeymasterDefs.KM_ENUM || tagType == HwKeymasterDefs.KM_ENUM_REP) {
            addEnumTag(tag, value);
            return;
        }
        throw new IllegalArgumentException("Not an enum or repeating enum tag: " + tag);
    }

    public void addEnums(int tag, int... values) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_ENUM_REP) {
            throw new IllegalArgumentException("Not a repeating enum tag: " + tag);
        }
        for (int value : values) {
            addEnumTag(tag, value);
        }
    }

    public int getEnum(int tag, int defaultValue) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_ENUM) {
            throw new IllegalArgumentException("Not an enum tag: " + tag);
        }
        HwKeymasterArgument arg = getArgumentByTag(tag);
        if (arg == null) {
            return defaultValue;
        }
        return getEnumTagValue(arg);
    }

    public List<Integer> getEnums(int tag) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_ENUM_REP) {
            throw new IllegalArgumentException("Not a repeating enum tag: " + tag);
        }
        List<Integer> values = new ArrayList();
        for (HwKeymasterArgument arg : this.mArguments) {
            if (arg.tag == tag) {
                values.add(Integer.valueOf(getEnumTagValue(arg)));
            }
        }
        return values;
    }

    private void addEnumTag(int tag, int value) {
        this.mArguments.add(new HwKeymasterIntArgument(tag, value));
    }

    private int getEnumTagValue(HwKeymasterArgument arg) {
        return ((HwKeymasterIntArgument) arg).value;
    }

    public void addUnsignedInt(int tag, long value) {
        int tagType = HwKeymasterDefs.getTagType(tag);
        if (tagType != HwKeymasterDefs.KM_UINT && tagType != HwKeymasterDefs.KM_UINT_REP) {
            throw new IllegalArgumentException("Not an int or repeating int tag: " + tag);
        } else if (value < 0 || value > UINT32_MAX_VALUE) {
            throw new IllegalArgumentException("Int tag value out of range: " + value);
        } else {
            this.mArguments.add(new HwKeymasterIntArgument(tag, (int) value));
        }
    }

    public long getUnsignedInt(int tag, long defaultValue) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_UINT) {
            throw new IllegalArgumentException("Not an int tag: " + tag);
        }
        HwKeymasterArgument arg = getArgumentByTag(tag);
        if (arg == null) {
            return defaultValue;
        }
        return ((long) ((HwKeymasterIntArgument) arg).value) & UINT32_MAX_VALUE;
    }

    public void addUnsignedLong(int tag, BigInteger value) {
        int tagType = HwKeymasterDefs.getTagType(tag);
        if (tagType == HwKeymasterDefs.KM_ULONG || tagType == HwKeymasterDefs.KM_ULONG_REP) {
            addLongTag(tag, value);
            return;
        }
        throw new IllegalArgumentException("Not a long or repeating long tag: " + tag);
    }

    public List<BigInteger> getUnsignedLongs(int tag) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_ULONG_REP) {
            throw new IllegalArgumentException("Tag is not a repeating long: " + tag);
        }
        List<BigInteger> values = new ArrayList();
        for (HwKeymasterArgument arg : this.mArguments) {
            if (arg.tag == tag) {
                values.add(getLongTagValue(arg));
            }
        }
        return values;
    }

    private void addLongTag(int tag, BigInteger value) {
        if (value.signum() == -1 || value.compareTo(UINT64_MAX_VALUE) > 0) {
            throw new IllegalArgumentException("Long tag value out of range: " + value);
        }
        this.mArguments.add(new HwKeymasterLongArgument(tag, value.longValue()));
    }

    private BigInteger getLongTagValue(HwKeymasterArgument arg) {
        return toUint64(((HwKeymasterLongArgument) arg).value);
    }

    public void addBoolean(int tag) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_BOOL) {
            throw new IllegalArgumentException("Not a boolean tag: " + tag);
        }
        this.mArguments.add(new HwKeymasterBooleanArgument(tag));
    }

    public boolean getBoolean(int tag) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_BOOL) {
            throw new IllegalArgumentException("Not a boolean tag: " + tag);
        } else if (getArgumentByTag(tag) == null) {
            return false;
        } else {
            return true;
        }
    }

    public void addBytes(int tag, byte[] value) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_BYTES) {
            throw new IllegalArgumentException("Not a bytes tag: " + tag);
        } else if (value == null) {
            throw new NullPointerException("value == nulll");
        } else {
            this.mArguments.add(new HwKeymasterBlobArgument(tag, value));
        }
    }

    public byte[] getBytes(int tag, byte[] defaultValue) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_BYTES) {
            throw new IllegalArgumentException("Not a bytes tag: " + tag);
        }
        HwKeymasterArgument arg = getArgumentByTag(tag);
        if (arg == null) {
            return defaultValue;
        }
        return ((HwKeymasterBlobArgument) arg).blob;
    }

    public void addDate(int tag, Date value) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_DATE) {
            throw new IllegalArgumentException("Not a date tag: " + tag);
        } else if (value == null) {
            throw new NullPointerException("value == nulll");
        } else if (value.getTime() < 0) {
            throw new IllegalArgumentException("Date tag value out of range: " + value);
        } else {
            this.mArguments.add(new HwKeymasterDateArgument(tag, value));
        }
    }

    public void addDateIfNotNull(int tag, Date value) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_DATE) {
            throw new IllegalArgumentException("Not a date tag: " + tag);
        } else if (value != null) {
            addDate(tag, value);
        }
    }

    public Date getDate(int tag, Date defaultValue) {
        if (HwKeymasterDefs.getTagType(tag) != HwKeymasterDefs.KM_DATE) {
            throw new IllegalArgumentException("Tag is not a date type: " + tag);
        }
        HwKeymasterArgument arg = getArgumentByTag(tag);
        if (arg == null) {
            return defaultValue;
        }
        Date result = ((HwKeymasterDateArgument) arg).date;
        if (result.getTime() >= 0) {
            return result;
        }
        throw new IllegalArgumentException("Tag value too large. Tag: " + tag);
    }

    private HwKeymasterArgument getArgumentByTag(int tag) {
        for (HwKeymasterArgument arg : this.mArguments) {
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
        in.readTypedList(this.mArguments, HwKeymasterArgument.CREATOR);
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
