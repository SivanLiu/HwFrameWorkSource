package android.content;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ContentProviderOperation implements Parcelable {
    public static final Creator<ContentProviderOperation> CREATOR = new Creator<ContentProviderOperation>() {
        public ContentProviderOperation createFromParcel(Parcel source) {
            return new ContentProviderOperation(source, null);
        }

        public ContentProviderOperation[] newArray(int size) {
            return new ContentProviderOperation[size];
        }
    };
    private static final String TAG = "ContentProviderOperation";
    public static final int TYPE_ASSERT = 4;
    public static final int TYPE_DELETE = 3;
    public static final int TYPE_INSERT = 1;
    public static final int TYPE_UPDATE = 2;
    private final Integer mExpectedCount;
    private final String mSelection;
    private final String[] mSelectionArgs;
    private final Map<Integer, Integer> mSelectionArgsBackReferences;
    private final int mType;
    private final Uri mUri;
    private final ContentValues mValues;
    private final ContentValues mValuesBackReferences;
    private final boolean mYieldAllowed;

    public static class Builder {
        private Integer mExpectedCount;
        private String mSelection;
        private String[] mSelectionArgs;
        private Map<Integer, Integer> mSelectionArgsBackReferences;
        private final int mType;
        private final Uri mUri;
        private ContentValues mValues;
        private ContentValues mValuesBackReferences;
        private boolean mYieldAllowed;

        /* synthetic */ Builder(int x0, Uri x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private Builder(int type, Uri uri) {
            if (uri != null) {
                this.mType = type;
                this.mUri = uri;
                return;
            }
            throw new IllegalArgumentException("uri must not be null");
        }

        public ContentProviderOperation build() {
            if (this.mType == 2 && ((this.mValues == null || this.mValues.isEmpty()) && (this.mValuesBackReferences == null || this.mValuesBackReferences.isEmpty()))) {
                throw new IllegalArgumentException("Empty values");
            } else if (this.mType != 4 || ((this.mValues != null && !this.mValues.isEmpty()) || ((this.mValuesBackReferences != null && !this.mValuesBackReferences.isEmpty()) || this.mExpectedCount != null))) {
                return new ContentProviderOperation(this, null);
            } else {
                throw new IllegalArgumentException("Empty values");
            }
        }

        public Builder withValueBackReferences(ContentValues backReferences) {
            if (this.mType == 1 || this.mType == 2 || this.mType == 4) {
                this.mValuesBackReferences = backReferences;
                return this;
            }
            throw new IllegalArgumentException("only inserts, updates, and asserts can have value back-references");
        }

        public Builder withValueBackReference(String key, int previousResult) {
            if (this.mType == 1 || this.mType == 2 || this.mType == 4) {
                if (this.mValuesBackReferences == null) {
                    this.mValuesBackReferences = new ContentValues();
                }
                this.mValuesBackReferences.put(key, Integer.valueOf(previousResult));
                return this;
            }
            throw new IllegalArgumentException("only inserts, updates, and asserts can have value back-references");
        }

        public Builder withSelectionBackReference(int selectionArgIndex, int previousResult) {
            if (this.mType == 2 || this.mType == 3 || this.mType == 4) {
                if (this.mSelectionArgsBackReferences == null) {
                    this.mSelectionArgsBackReferences = new HashMap();
                }
                this.mSelectionArgsBackReferences.put(Integer.valueOf(selectionArgIndex), Integer.valueOf(previousResult));
                return this;
            }
            throw new IllegalArgumentException("only updates, deletes, and asserts can have selection back-references");
        }

        public Builder withValues(ContentValues values) {
            if (this.mType == 1 || this.mType == 2 || this.mType == 4) {
                if (this.mValues == null) {
                    this.mValues = new ContentValues();
                }
                this.mValues.putAll(values);
                return this;
            }
            throw new IllegalArgumentException("only inserts, updates, and asserts can have values");
        }

        public Builder withValue(String key, Object value) {
            if (this.mType == 1 || this.mType == 2 || this.mType == 4) {
                if (this.mValues == null) {
                    this.mValues = new ContentValues();
                }
                if (value == null) {
                    this.mValues.putNull(key);
                } else if (value instanceof String) {
                    this.mValues.put(key, (String) value);
                } else if (value instanceof Byte) {
                    this.mValues.put(key, (Byte) value);
                } else if (value instanceof Short) {
                    this.mValues.put(key, (Short) value);
                } else if (value instanceof Integer) {
                    this.mValues.put(key, (Integer) value);
                } else if (value instanceof Long) {
                    this.mValues.put(key, (Long) value);
                } else if (value instanceof Float) {
                    this.mValues.put(key, (Float) value);
                } else if (value instanceof Double) {
                    this.mValues.put(key, (Double) value);
                } else if (value instanceof Boolean) {
                    this.mValues.put(key, (Boolean) value);
                } else if (value instanceof byte[]) {
                    this.mValues.put(key, (byte[]) value);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("bad value type: ");
                    stringBuilder.append(value.getClass().getName());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                return this;
            }
            throw new IllegalArgumentException("only inserts and updates can have values");
        }

        public Builder withSelection(String selection, String[] selectionArgs) {
            if (this.mType == 2 || this.mType == 3 || this.mType == 4) {
                this.mSelection = selection;
                if (selectionArgs == null) {
                    this.mSelectionArgs = null;
                } else {
                    this.mSelectionArgs = new String[selectionArgs.length];
                    System.arraycopy(selectionArgs, 0, this.mSelectionArgs, 0, selectionArgs.length);
                }
                return this;
            }
            throw new IllegalArgumentException("only updates, deletes, and asserts can have selections");
        }

        public Builder withExpectedCount(int count) {
            if (this.mType == 2 || this.mType == 3 || this.mType == 4) {
                this.mExpectedCount = Integer.valueOf(count);
                return this;
            }
            throw new IllegalArgumentException("only updates, deletes, and asserts can have expected counts");
        }

        public Builder withYieldAllowed(boolean yieldAllowed) {
            this.mYieldAllowed = yieldAllowed;
            return this;
        }
    }

    private ContentProviderOperation(Builder builder) {
        this.mType = builder.mType;
        this.mUri = builder.mUri;
        this.mValues = builder.mValues;
        this.mSelection = builder.mSelection;
        this.mSelectionArgs = builder.mSelectionArgs;
        this.mExpectedCount = builder.mExpectedCount;
        this.mSelectionArgsBackReferences = builder.mSelectionArgsBackReferences;
        this.mValuesBackReferences = builder.mValuesBackReferences;
        this.mYieldAllowed = builder.mYieldAllowed;
    }

    private ContentProviderOperation(Parcel source) {
        ContentValues contentValues;
        this.mType = source.readInt();
        this.mUri = (Uri) Uri.CREATOR.createFromParcel(source);
        Map map = null;
        this.mValues = source.readInt() != 0 ? (ContentValues) ContentValues.CREATOR.createFromParcel(source) : null;
        this.mSelection = source.readInt() != 0 ? source.readString() : null;
        this.mSelectionArgs = source.readInt() != 0 ? source.readStringArray() : null;
        this.mExpectedCount = source.readInt() != 0 ? Integer.valueOf(source.readInt()) : null;
        if (source.readInt() != 0) {
            contentValues = (ContentValues) ContentValues.CREATOR.createFromParcel(source);
        } else {
            contentValues = null;
        }
        this.mValuesBackReferences = contentValues;
        if (source.readInt() != 0) {
            map = new HashMap();
        }
        this.mSelectionArgsBackReferences = map;
        boolean z = false;
        if (this.mSelectionArgsBackReferences != null) {
            int count = source.readInt();
            for (int i = 0; i < count; i++) {
                this.mSelectionArgsBackReferences.put(Integer.valueOf(source.readInt()), Integer.valueOf(source.readInt()));
            }
        }
        if (source.readInt() != 0) {
            z = true;
        }
        this.mYieldAllowed = z;
    }

    public ContentProviderOperation(ContentProviderOperation cpo, Uri withUri) {
        this.mType = cpo.mType;
        this.mUri = withUri;
        this.mValues = cpo.mValues;
        this.mSelection = cpo.mSelection;
        this.mSelectionArgs = cpo.mSelectionArgs;
        this.mExpectedCount = cpo.mExpectedCount;
        this.mSelectionArgsBackReferences = cpo.mSelectionArgsBackReferences;
        this.mValuesBackReferences = cpo.mValuesBackReferences;
        this.mYieldAllowed = cpo.mYieldAllowed;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mType);
        Uri.writeToParcel(dest, this.mUri);
        if (this.mValues != null) {
            dest.writeInt(1);
            this.mValues.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        if (this.mSelection != null) {
            dest.writeInt(1);
            dest.writeString(this.mSelection);
        } else {
            dest.writeInt(0);
        }
        if (this.mSelectionArgs != null) {
            dest.writeInt(1);
            dest.writeStringArray(this.mSelectionArgs);
        } else {
            dest.writeInt(0);
        }
        if (this.mExpectedCount != null) {
            dest.writeInt(1);
            dest.writeInt(this.mExpectedCount.intValue());
        } else {
            dest.writeInt(0);
        }
        if (this.mValuesBackReferences != null) {
            dest.writeInt(1);
            this.mValuesBackReferences.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        if (this.mSelectionArgsBackReferences != null) {
            dest.writeInt(1);
            dest.writeInt(this.mSelectionArgsBackReferences.size());
            for (Entry<Integer, Integer> entry : this.mSelectionArgsBackReferences.entrySet()) {
                dest.writeInt(((Integer) entry.getKey()).intValue());
                dest.writeInt(((Integer) entry.getValue()).intValue());
            }
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.mYieldAllowed);
    }

    public static Builder newInsert(Uri uri) {
        return new Builder(1, uri, null);
    }

    public static Builder newUpdate(Uri uri) {
        return new Builder(2, uri, null);
    }

    public static Builder newDelete(Uri uri) {
        return new Builder(3, uri, null);
    }

    public static Builder newAssertQuery(Uri uri) {
        return new Builder(4, uri, null);
    }

    public Uri getUri() {
        return this.mUri;
    }

    public boolean isYieldAllowed() {
        return this.mYieldAllowed;
    }

    public int getType() {
        return this.mType;
    }

    public boolean isInsert() {
        return this.mType == 1;
    }

    public boolean isDelete() {
        return this.mType == 3;
    }

    public boolean isUpdate() {
        return this.mType == 2;
    }

    public boolean isAssertQuery() {
        return this.mType == 4;
    }

    public boolean isWriteOperation() {
        return this.mType == 3 || this.mType == 1 || this.mType == 2;
    }

    public boolean isReadOperation() {
        return this.mType == 4;
    }

    /* JADX WARNING: Missing block: B:26:0x008a, code skipped:
            if (r8 != null) goto L_0x008c;
     */
    /* JADX WARNING: Missing block: B:28:0x0090, code skipped:
            if (r1.moveToNext() == false) goto L_0x00db;
     */
    /* JADX WARNING: Missing block: B:29:0x0092, code skipped:
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:31:0x0094, code skipped:
            if (r3 >= r8.length) goto L_0x008c;
     */
    /* JADX WARNING: Missing block: B:32:0x0096, code skipped:
            r4 = r1.getString(r3);
            r5 = r0.getAsString(r8[r3]);
     */
    /* JADX WARNING: Missing block: B:33:0x00a4, code skipped:
            if (android.text.TextUtils.equals(r4, r5) == false) goto L_0x00a9;
     */
    /* JADX WARNING: Missing block: B:34:0x00a6, code skipped:
            r3 = r3 + 1;
     */
    /* JADX WARNING: Missing block: B:35:0x00a9, code skipped:
            android.util.Log.e(TAG, toString());
            r9 = new java.lang.StringBuilder();
            r9.append("Found value ");
            r9.append(r4);
            r9.append(" when expected ");
            r9.append(r5);
            r9.append(" for column ");
            r9.append(r8[r3]);
     */
    /* JADX WARNING: Missing block: B:36:0x00da, code skipped:
            throw new android.content.OperationApplicationException(r9.toString());
     */
    /* JADX WARNING: Missing block: B:37:0x00db, code skipped:
            r1.close();
            r1 = r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ContentProviderResult apply(ContentProvider provider, ContentProviderResult[] backRefs, int numBackRefs) throws OperationApplicationException {
        ContentValues values = resolveValueBackReferences(backRefs, numBackRefs);
        String[] selectionArgs = resolveSelectionArgsBackReferences(backRefs, numBackRefs);
        if (this.mType == 1) {
            Uri newUri = provider.insert(this.mUri, values);
            if (newUri != null) {
                return new ContentProviderResult(newUri);
            }
            throw new OperationApplicationException("insert failed");
        }
        int cursor;
        if (this.mType == 3) {
            cursor = provider.delete(this.mUri, this.mSelection, selectionArgs);
        } else if (this.mType == 2) {
            cursor = provider.update(this.mUri, values, this.mSelection, selectionArgs);
        } else if (this.mType == 4) {
            String[] projection = null;
            if (values != null) {
                ArrayList<String> projectionList = new ArrayList();
                for (Entry<String, Object> entry : values.valueSet()) {
                    projectionList.add((String) entry.getKey());
                }
                projection = (String[]) projectionList.toArray(new String[projectionList.size()]);
            }
            String[] projection2 = projection;
            Cursor cursor2 = provider.query(this.mUri, projection2, this.mSelection, selectionArgs, null);
            try {
                int numRows = cursor2.getCount();
            } catch (Throwable th) {
                cursor2.close();
            }
        } else {
            Log.e(TAG, toString());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad type, ");
            stringBuilder.append(this.mType);
            throw new IllegalStateException(stringBuilder.toString());
        }
        if (this.mExpectedCount == null || this.mExpectedCount.intValue() == cursor) {
            return new ContentProviderResult(cursor);
        }
        Log.e(TAG, toString());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("wrong number of rows: ");
        stringBuilder2.append(cursor);
        throw new OperationApplicationException(stringBuilder2.toString());
    }

    public ContentValues resolveValueBackReferences(ContentProviderResult[] backRefs, int numBackRefs) {
        if (this.mValuesBackReferences == null) {
            return this.mValues;
        }
        ContentValues values;
        if (this.mValues == null) {
            values = new ContentValues();
        } else {
            values = new ContentValues(this.mValues);
        }
        for (Entry<String, Object> entry : this.mValuesBackReferences.valueSet()) {
            String key = (String) entry.getKey();
            Integer backRefIndex = this.mValuesBackReferences.getAsInteger(key);
            if (backRefIndex != null) {
                values.put(key, Long.valueOf(backRefToValue(backRefs, numBackRefs, backRefIndex)));
            } else {
                Log.e(TAG, toString());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("values backref ");
                stringBuilder.append(key);
                stringBuilder.append(" is not an integer");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return values;
    }

    public String[] resolveSelectionArgsBackReferences(ContentProviderResult[] backRefs, int numBackRefs) {
        if (this.mSelectionArgsBackReferences == null) {
            return this.mSelectionArgs;
        }
        String[] newArgs = new String[this.mSelectionArgs.length];
        System.arraycopy(this.mSelectionArgs, 0, newArgs, 0, this.mSelectionArgs.length);
        for (Entry<Integer, Integer> selectionArgBackRef : this.mSelectionArgsBackReferences.entrySet()) {
            newArgs[((Integer) selectionArgBackRef.getKey()).intValue()] = String.valueOf(backRefToValue(backRefs, numBackRefs, Integer.valueOf(((Integer) selectionArgBackRef.getValue()).intValue())));
        }
        return newArgs;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mType: ");
        stringBuilder.append(this.mType);
        stringBuilder.append(", mUri: ");
        stringBuilder.append(this.mUri);
        stringBuilder.append(", mSelection: ");
        stringBuilder.append(this.mSelection);
        stringBuilder.append(", mExpectedCount: ");
        stringBuilder.append(this.mExpectedCount);
        stringBuilder.append(", mYieldAllowed: ");
        stringBuilder.append(this.mYieldAllowed);
        stringBuilder.append(", mValues: ");
        stringBuilder.append(this.mValues);
        stringBuilder.append(", mValuesBackReferences: ");
        stringBuilder.append(this.mValuesBackReferences);
        stringBuilder.append(", mSelectionArgsBackReferences: ");
        stringBuilder.append(this.mSelectionArgsBackReferences);
        return stringBuilder.toString();
    }

    private long backRefToValue(ContentProviderResult[] backRefs, int numBackRefs, Integer backRefIndex) {
        if (backRefIndex.intValue() < numBackRefs) {
            ContentProviderResult backRef = backRefs[backRefIndex.intValue()];
            if (backRef.uri != null) {
                return ContentUris.parseId(backRef.uri);
            }
            return (long) backRef.count.intValue();
        }
        Log.e(TAG, toString());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("asked for back ref ");
        stringBuilder.append(backRefIndex);
        stringBuilder.append(" but there are only ");
        stringBuilder.append(numBackRefs);
        stringBuilder.append(" back refs");
        throw new ArrayIndexOutOfBoundsException(stringBuilder.toString());
    }

    public int describeContents() {
        return 0;
    }
}
