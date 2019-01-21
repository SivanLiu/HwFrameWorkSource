package android.media;

import android.os.Parcel;
import android.util.Log;
import android.util.MathUtils;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TimeZone;

@Deprecated
public class Metadata {
    public static final int ALBUM = 8;
    public static final int ALBUM_ART = 18;
    public static final int ANY = 0;
    public static final int ARTIST = 9;
    public static final int AUDIO_BIT_RATE = 21;
    public static final int AUDIO_CODEC = 26;
    public static final int AUDIO_SAMPLE_RATE = 23;
    public static final int AUTHOR = 10;
    public static final int BIT_RATE = 20;
    public static final int BOOLEAN_VAL = 3;
    public static final int BYTE_ARRAY_VAL = 7;
    public static final int CD_TRACK_MAX = 16;
    public static final int CD_TRACK_NUM = 15;
    public static final int COMMENT = 6;
    public static final int COMPOSER = 11;
    public static final int COPYRIGHT = 7;
    public static final int DATE = 13;
    public static final int DATE_VAL = 6;
    public static final int DOUBLE_VAL = 5;
    public static final int DRM_CRIPPLED = 31;
    public static final int DURATION = 14;
    private static final int FIRST_CUSTOM = 8192;
    public static final int GENRE = 12;
    public static final int INTEGER_VAL = 2;
    private static final int LAST_SYSTEM = 31;
    private static final int LAST_TYPE = 7;
    public static final int LONG_VAL = 4;
    public static final Set<Integer> MATCH_ALL = Collections.singleton(Integer.valueOf(0));
    public static final Set<Integer> MATCH_NONE = Collections.EMPTY_SET;
    public static final int MIME_TYPE = 25;
    public static final int NUM_TRACKS = 30;
    public static final int PAUSE_AVAILABLE = 1;
    public static final int RATING = 17;
    public static final int SEEK_AVAILABLE = 4;
    public static final int SEEK_BACKWARD_AVAILABLE = 2;
    public static final int SEEK_FORWARD_AVAILABLE = 3;
    public static final int STRING_VAL = 1;
    private static final String TAG = "media.Metadata";
    public static final int TITLE = 5;
    public static final int VIDEO_BIT_RATE = 22;
    public static final int VIDEO_CODEC = 27;
    public static final int VIDEO_FRAME = 19;
    public static final int VIDEO_FRAME_RATE = 24;
    public static final int VIDEO_HEIGHT = 28;
    public static final int VIDEO_WIDTH = 29;
    private static final int kInt32Size = 4;
    private static final int kMetaHeaderSize = 8;
    private static final int kMetaMarker = 1296389185;
    private static final int kRecordHeaderSize = 12;
    private final HashMap<Integer, Integer> mKeyToPosMap = new HashMap();
    private Parcel mParcel;

    private boolean scanAllRecords(Parcel parcel, int bytesLeft) {
        int recCount = 0;
        boolean error = false;
        this.mKeyToPosMap.clear();
        while (bytesLeft > 12) {
            int start = parcel.dataPosition();
            int size = parcel.readInt();
            if (size <= 12) {
                Log.e(TAG, "Record is too short");
                error = true;
                break;
            }
            int metadataId = parcel.readInt();
            if (!checkMetadataId(metadataId)) {
                error = true;
                break;
            } else if (this.mKeyToPosMap.containsKey(Integer.valueOf(metadataId))) {
                Log.e(TAG, "Duplicate metadata ID found");
                error = true;
                break;
            } else {
                this.mKeyToPosMap.put(Integer.valueOf(metadataId), Integer.valueOf(parcel.dataPosition()));
                int metadataType = parcel.readInt();
                if (metadataType <= 0 || metadataType > 7) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid metadata type ");
                    stringBuilder.append(metadataType);
                    Log.e(str, stringBuilder.toString());
                    error = true;
                    break;
                }
                try {
                    parcel.setDataPosition(MathUtils.addOrThrow(start, size));
                    bytesLeft -= size;
                    recCount++;
                } catch (IllegalArgumentException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid size: ");
                    stringBuilder2.append(e.getMessage());
                    Log.e(str2, stringBuilder2.toString());
                    error = true;
                }
            }
        }
        if (bytesLeft == 0 && !error) {
            return true;
        }
        String str3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Ran out of data or error on record ");
        stringBuilder3.append(recCount);
        Log.e(str3, stringBuilder3.toString());
        this.mKeyToPosMap.clear();
        return false;
    }

    public boolean parse(Parcel parcel) {
        if (parcel.dataAvail() < 8) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not enough data ");
            stringBuilder.append(parcel.dataAvail());
            Log.e(str, stringBuilder.toString());
            return false;
        }
        int pin = parcel.dataPosition();
        int size = parcel.readInt();
        if (parcel.dataAvail() + 4 < size || size < 8) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Bad size ");
            stringBuilder2.append(size);
            stringBuilder2.append(" avail ");
            stringBuilder2.append(parcel.dataAvail());
            stringBuilder2.append(" position ");
            stringBuilder2.append(pin);
            Log.e(str2, stringBuilder2.toString());
            parcel.setDataPosition(pin);
            return false;
        }
        int kShouldBeMetaMarker = parcel.readInt();
        if (kShouldBeMetaMarker != kMetaMarker) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Marker missing ");
            stringBuilder3.append(Integer.toHexString(kShouldBeMetaMarker));
            Log.e(str3, stringBuilder3.toString());
            parcel.setDataPosition(pin);
            return false;
        } else if (scanAllRecords(parcel, size - 8)) {
            this.mParcel = parcel;
            return true;
        } else {
            parcel.setDataPosition(pin);
            return false;
        }
    }

    public Set<Integer> keySet() {
        return this.mKeyToPosMap.keySet();
    }

    public boolean has(int metadataId) {
        if (checkMetadataId(metadataId)) {
            return this.mKeyToPosMap.containsKey(Integer.valueOf(metadataId));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid key: ");
        stringBuilder.append(metadataId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public String getString(int key) {
        checkType(key, 1);
        return this.mParcel.readString();
    }

    public int getInt(int key) {
        checkType(key, 2);
        return this.mParcel.readInt();
    }

    public boolean getBoolean(int key) {
        checkType(key, 3);
        return this.mParcel.readInt() == 1;
    }

    public long getLong(int key) {
        checkType(key, 4);
        return this.mParcel.readLong();
    }

    public double getDouble(int key) {
        checkType(key, 5);
        return this.mParcel.readDouble();
    }

    public byte[] getByteArray(int key) {
        checkType(key, 7);
        return this.mParcel.createByteArray();
    }

    public Date getDate(int key) {
        checkType(key, 6);
        long timeSinceEpoch = this.mParcel.readLong();
        String timeZone = this.mParcel.readString();
        if (timeZone.length() == 0) {
            return new Date(timeSinceEpoch);
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
        cal.setTimeInMillis(timeSinceEpoch);
        return cal.getTime();
    }

    public static int lastSytemId() {
        return 31;
    }

    public static int firstCustomId() {
        return 8192;
    }

    public static int lastType() {
        return 7;
    }

    private boolean checkMetadataId(int val) {
        if (val > 0 && (31 >= val || val >= 8192)) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid metadata ID ");
        stringBuilder.append(val);
        Log.e(str, stringBuilder.toString());
        return false;
    }

    private void checkType(int key, int expectedType) {
        this.mParcel.setDataPosition(((Integer) this.mKeyToPosMap.get(Integer.valueOf(key))).intValue());
        int type = this.mParcel.readInt();
        if (type != expectedType) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Wrong type ");
            stringBuilder.append(expectedType);
            stringBuilder.append(" but got ");
            stringBuilder.append(type);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public void recycleParcel() {
        if (this.mParcel != null) {
            this.mParcel.recycle();
        }
    }
}
