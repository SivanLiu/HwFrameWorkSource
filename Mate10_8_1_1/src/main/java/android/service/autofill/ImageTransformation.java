package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.util.Pair;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.regex.Pattern;

public final class ImageTransformation extends InternalTransformation implements Transformation, Parcelable {
    public static final Creator<ImageTransformation> CREATOR = new Creator<ImageTransformation>() {
        public ImageTransformation createFromParcel(Parcel parcel) {
            AutofillId id = (AutofillId) parcel.readParcelable(null);
            Pattern[] regexs = (Pattern[]) parcel.readSerializable();
            int[] resIds = parcel.createIntArray();
            Builder builder = new Builder(id, regexs[0], resIds[0]);
            int size = regexs.length;
            for (int i = 1; i < size; i++) {
                builder.addOption(regexs[i], resIds[i]);
            }
            return builder.build();
        }

        public ImageTransformation[] newArray(int size) {
            return new ImageTransformation[size];
        }
    };
    private static final String TAG = "ImageTransformation";
    private final AutofillId mId;
    private final ArrayList<Pair<Pattern, Integer>> mOptions;

    public static class Builder {
        private boolean mDestroyed;
        private final AutofillId mId;
        private final ArrayList<Pair<Pattern, Integer>> mOptions = new ArrayList();

        public Builder(AutofillId id, Pattern regex, int resId) {
            this.mId = (AutofillId) Preconditions.checkNotNull(id);
            addOption(regex, resId);
        }

        public Builder addOption(Pattern regex, int resId) {
            boolean z = false;
            throwIfDestroyed();
            Preconditions.checkNotNull(regex);
            if (resId != 0) {
                z = true;
            }
            Preconditions.checkArgument(z);
            this.mOptions.add(new Pair(regex, Integer.valueOf(resId)));
            return this;
        }

        public ImageTransformation build() {
            throwIfDestroyed();
            this.mDestroyed = true;
            return new ImageTransformation();
        }

        private void throwIfDestroyed() {
            Preconditions.checkState(this.mDestroyed ^ 1, "Already called build()");
        }
    }

    private ImageTransformation(Builder builder) {
        this.mId = builder.mId;
        this.mOptions = builder.mOptions;
    }

    public void apply(ValueFinder finder, RemoteViews parentTemplate, int childViewId) throws Exception {
        String value = finder.findByAutofillId(this.mId);
        if (value == null) {
            Log.w(TAG, "No view for id " + this.mId);
            return;
        }
        int size = this.mOptions.size();
        if (Helper.sDebug) {
            Log.d(TAG, size + " multiple options on id " + childViewId + " to compare against");
        }
        int i = 0;
        while (i < size) {
            Pair<Pattern, Integer> option = (Pair) this.mOptions.get(i);
            try {
                if (((Pattern) option.first).matcher(value).matches()) {
                    Log.d(TAG, "Found match at " + i + ": " + option);
                    parentTemplate.setImageViewResource(childViewId, ((Integer) option.second).intValue());
                    return;
                }
                i++;
            } catch (Exception e) {
                Log.w(TAG, "Error matching regex #" + i + "(" + ((Pattern) option.first).pattern() + ") on id " + option.second + ": " + e.getClass());
                throw e;
            }
        }
        if (Helper.sDebug) {
            Log.d(TAG, "No match for " + value);
        }
    }

    public String toString() {
        if (Helper.sDebug) {
            return "ImageTransformation: [id=" + this.mId + ", options=" + this.mOptions + "]";
        }
        return super.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(this.mId, flags);
        int size = this.mOptions.size();
        Pattern[] regexs = new Pattern[size];
        int[] resIds = new int[size];
        for (int i = 0; i < size; i++) {
            Pair<Pattern, Integer> regex = (Pair) this.mOptions.get(i);
            regexs[i] = (Pattern) regex.first;
            resIds[i] = ((Integer) regex.second).intValue();
        }
        parcel.writeSerializable(regexs);
        parcel.writeIntArray(resIds);
    }
}
