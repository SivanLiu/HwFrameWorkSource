package android.service.autofill;

import android.app.ActivityThread;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.autofill.Helper;
import com.android.internal.util.Preconditions;
import java.io.PrintWriter;
import java.util.ArrayList;

public final class UserData implements Parcelable {
    public static final Creator<UserData> CREATOR = new Creator<UserData>() {
        public UserData createFromParcel(Parcel parcel) {
            String id = parcel.readString();
            String[] categoryIds = parcel.readStringArray();
            String[] values = parcel.readStringArray();
            Builder builder = new Builder(id, values[0], categoryIds[0]).setFieldClassificationAlgorithm(parcel.readString(), parcel.readBundle());
            for (int i = 1; i < categoryIds.length; i++) {
                builder.add(values[i], categoryIds[i]);
            }
            return builder.build();
        }

        public UserData[] newArray(int size) {
            return new UserData[size];
        }
    };
    private static final int DEFAULT_MAX_CATEGORY_COUNT = 10;
    private static final int DEFAULT_MAX_FIELD_CLASSIFICATION_IDS_SIZE = 10;
    private static final int DEFAULT_MAX_USER_DATA_SIZE = 50;
    private static final int DEFAULT_MAX_VALUE_LENGTH = 100;
    private static final int DEFAULT_MIN_VALUE_LENGTH = 3;
    private static final String TAG = "UserData";
    private final String mAlgorithm;
    private final Bundle mAlgorithmArgs;
    private final String[] mCategoryIds;
    private final String mId;
    private final String[] mValues;

    public static final class Builder {
        private String mAlgorithm;
        private Bundle mAlgorithmArgs;
        private final ArrayList<String> mCategoryIds;
        private boolean mDestroyed;
        private final String mId;
        private final ArraySet<String> mUniqueCategoryIds = new ArraySet(UserData.getMaxCategoryCount());
        private final ArrayList<String> mValues;

        public Builder(String id, String value, String categoryId) {
            this.mId = checkNotEmpty("id", id);
            checkNotEmpty("categoryId", categoryId);
            checkValidValue(value);
            int maxUserDataSize = UserData.getMaxUserDataSize();
            this.mCategoryIds = new ArrayList(maxUserDataSize);
            this.mValues = new ArrayList(maxUserDataSize);
            addMapping(value, categoryId);
        }

        public Builder setFieldClassificationAlgorithm(String name, Bundle args) {
            throwIfDestroyed();
            this.mAlgorithm = name;
            this.mAlgorithmArgs = args;
            return this;
        }

        public Builder add(String value, String categoryId) {
            throwIfDestroyed();
            checkNotEmpty("categoryId", categoryId);
            checkValidValue(value);
            boolean z = false;
            if (!this.mUniqueCategoryIds.contains(categoryId)) {
                boolean z2 = this.mUniqueCategoryIds.size() < UserData.getMaxCategoryCount();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("already added ");
                stringBuilder.append(this.mUniqueCategoryIds.size());
                stringBuilder.append(" unique category ids");
                Preconditions.checkState(z2, stringBuilder.toString());
            }
            Preconditions.checkState(this.mValues.contains(value) ^ 1, "already has entry with same value");
            if (this.mValues.size() < UserData.getMaxUserDataSize()) {
                z = true;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("already added ");
            stringBuilder2.append(this.mValues.size());
            stringBuilder2.append(" elements");
            Preconditions.checkState(z, stringBuilder2.toString());
            addMapping(value, categoryId);
            return this;
        }

        private void addMapping(String value, String categoryId) {
            this.mCategoryIds.add(categoryId);
            this.mValues.add(value);
            this.mUniqueCategoryIds.add(categoryId);
        }

        private String checkNotEmpty(String name, String value) {
            Preconditions.checkNotNull(value);
            Preconditions.checkArgument(TextUtils.isEmpty(value) ^ 1, "%s cannot be empty", new Object[]{name});
            return value;
        }

        private void checkValidValue(String value) {
            Preconditions.checkNotNull(value);
            int length = value.length();
            int minValueLength = UserData.getMinValueLength();
            int maxValueLength = UserData.getMaxValueLength();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("value length (");
            stringBuilder.append(length);
            stringBuilder.append(")");
            Preconditions.checkArgumentInRange(length, minValueLength, maxValueLength, stringBuilder.toString());
        }

        public UserData build() {
            throwIfDestroyed();
            this.mDestroyed = true;
            return new UserData(this, null);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    /* synthetic */ UserData(Builder x0, AnonymousClass1 x1) {
        this(x0);
    }

    private UserData(Builder builder) {
        this.mId = builder.mId;
        this.mAlgorithm = builder.mAlgorithm;
        this.mAlgorithmArgs = builder.mAlgorithmArgs;
        this.mCategoryIds = new String[builder.mCategoryIds.size()];
        builder.mCategoryIds.toArray(this.mCategoryIds);
        this.mValues = new String[builder.mValues.size()];
        builder.mValues.toArray(this.mValues);
    }

    public String getFieldClassificationAlgorithm() {
        return this.mAlgorithm;
    }

    public String getId() {
        return this.mId;
    }

    public Bundle getAlgorithmArgs() {
        return this.mAlgorithmArgs;
    }

    public String[] getCategoryIds() {
        return this.mCategoryIds;
    }

    public String[] getValues() {
        return this.mValues;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("id: ");
        pw.print(this.mId);
        pw.print(prefix);
        pw.print("Algorithm: ");
        pw.print(this.mAlgorithm);
        pw.print(" Args: ");
        pw.println(this.mAlgorithmArgs);
        pw.print(prefix);
        pw.print("Field ids size: ");
        pw.println(this.mCategoryIds.length);
        int i = 0;
        for (int i2 = 0; i2 < this.mCategoryIds.length; i2++) {
            pw.print(prefix);
            pw.print(prefix);
            pw.print(i2);
            pw.print(": ");
            pw.println(Helper.getRedacted(this.mCategoryIds[i2]));
        }
        pw.print(prefix);
        pw.print("Values size: ");
        pw.println(this.mValues.length);
        while (i < this.mValues.length) {
            pw.print(prefix);
            pw.print(prefix);
            pw.print(i);
            pw.print(": ");
            pw.println(Helper.getRedacted(this.mValues[i]));
            i++;
        }
    }

    public static void dumpConstraints(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("maxUserDataSize: ");
        pw.println(getMaxUserDataSize());
        pw.print(prefix);
        pw.print("maxFieldClassificationIdsSize: ");
        pw.println(getMaxFieldClassificationIdsSize());
        pw.print(prefix);
        pw.print("maxCategoryCount: ");
        pw.println(getMaxCategoryCount());
        pw.print(prefix);
        pw.print("minValueLength: ");
        pw.println(getMinValueLength());
        pw.print(prefix);
        pw.print("maxValueLength: ");
        pw.println(getMaxValueLength());
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder builder = new StringBuilder("UserData: [id=");
        builder.append(this.mId);
        builder.append(", algorithm=");
        builder = builder.append(this.mAlgorithm);
        builder.append(", categoryIds=");
        Helper.appendRedacted(builder, this.mCategoryIds);
        builder.append(", values=");
        Helper.appendRedacted(builder, this.mValues);
        builder.append("]");
        return builder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.mId);
        parcel.writeStringArray(this.mCategoryIds);
        parcel.writeStringArray(this.mValues);
        parcel.writeString(this.mAlgorithm);
        parcel.writeBundle(this.mAlgorithmArgs);
    }

    public static int getMaxUserDataSize() {
        return getInt(Secure.AUTOFILL_USER_DATA_MAX_USER_DATA_SIZE, 50);
    }

    public static int getMaxFieldClassificationIdsSize() {
        return getInt(Secure.AUTOFILL_USER_DATA_MAX_FIELD_CLASSIFICATION_IDS_SIZE, 10);
    }

    public static int getMaxCategoryCount() {
        return getInt(Secure.AUTOFILL_USER_DATA_MAX_CATEGORY_COUNT, 10);
    }

    public static int getMinValueLength() {
        return getInt(Secure.AUTOFILL_USER_DATA_MIN_VALUE_LENGTH, 3);
    }

    public static int getMaxValueLength() {
        return getInt(Secure.AUTOFILL_USER_DATA_MAX_VALUE_LENGTH, 100);
    }

    private static int getInt(String settings, int defaultValue) {
        ContentResolver cr = null;
        ActivityThread at = ActivityThread.currentActivityThread();
        if (at != null) {
            cr = at.getApplication().getContentResolver();
        }
        if (cr != null) {
            return Secure.getInt(cr, settings, defaultValue);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Could not read from ");
        stringBuilder.append(settings);
        stringBuilder.append("; hardcoding ");
        stringBuilder.append(defaultValue);
        Log.w("UserData", stringBuilder.toString());
        return defaultValue;
    }
}
