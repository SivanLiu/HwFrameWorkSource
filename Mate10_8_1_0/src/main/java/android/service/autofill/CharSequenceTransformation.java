package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.telecom.Logging.Session;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CharSequenceTransformation extends InternalTransformation implements Transformation, Parcelable {
    public static final Creator<CharSequenceTransformation> CREATOR = new Creator<CharSequenceTransformation>() {
        public CharSequenceTransformation createFromParcel(Parcel parcel) {
            AutofillId[] ids = (AutofillId[]) parcel.readParcelableArray(null, AutofillId.class);
            Pattern[] regexs = (Pattern[]) parcel.readSerializable();
            String[] substs = parcel.createStringArray();
            Builder builder = new Builder(ids[0], regexs[0], substs[0]);
            int size = ids.length;
            for (int i = 1; i < size; i++) {
                builder.addField(ids[i], regexs[i], substs[i]);
            }
            return builder.build();
        }

        public CharSequenceTransformation[] newArray(int size) {
            return new CharSequenceTransformation[size];
        }
    };
    private static final String TAG = "CharSequenceTransformation";
    private final ArrayMap<AutofillId, Pair<Pattern, String>> mFields;

    public static class Builder {
        private boolean mDestroyed;
        private final ArrayMap<AutofillId, Pair<Pattern, String>> mFields = new ArrayMap();

        public Builder(AutofillId id, Pattern regex, String subst) {
            addField(id, regex, subst);
        }

        public Builder addField(AutofillId id, Pattern regex, String subst) {
            throwIfDestroyed();
            Preconditions.checkNotNull(id);
            Preconditions.checkNotNull(regex);
            Preconditions.checkNotNull(subst);
            this.mFields.put(id, new Pair(regex, subst));
            return this;
        }

        public CharSequenceTransformation build() {
            throwIfDestroyed();
            this.mDestroyed = true;
            return new CharSequenceTransformation();
        }

        private void throwIfDestroyed() {
            Preconditions.checkState(this.mDestroyed ^ 1, "Already called build()");
        }
    }

    private CharSequenceTransformation(Builder builder) {
        this.mFields = builder.mFields;
    }

    public void apply(ValueFinder finder, RemoteViews parentTemplate, int childViewId) throws Exception {
        StringBuilder converted = new StringBuilder();
        int size = this.mFields.size();
        if (Helper.sDebug) {
            Log.d(TAG, size + " multiple fields on id " + childViewId);
        }
        int i = 0;
        while (i < size) {
            AutofillId id = (AutofillId) this.mFields.keyAt(i);
            Pair<Pattern, String> field = (Pair) this.mFields.valueAt(i);
            String value = finder.findByAutofillId(id);
            if (value == null) {
                Log.w(TAG, "No value for id " + id);
                return;
            }
            try {
                Matcher matcher = ((Pattern) field.first).matcher(value);
                if (matcher.find()) {
                    converted.append(matcher.replaceAll((String) field.second));
                    i++;
                } else {
                    if (Helper.sDebug) {
                        Log.d(TAG, "match for " + field.first + " failed on id " + id);
                    }
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "Cannot apply " + ((Pattern) field.first).pattern() + Session.SUBSESSION_SEPARATION_CHAR + ((String) field.second) + " to " + "field with autofill id" + id + ": " + e.getClass());
                throw e;
            }
        }
        parentTemplate.setCharSequence(childViewId, "setText", converted);
    }

    public String toString() {
        if (Helper.sDebug) {
            return "MultipleViewsCharSequenceTransformation: [fields=" + this.mFields + "]";
        }
        return super.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        int size = this.mFields.size();
        AutofillId[] ids = new AutofillId[size];
        Pattern[] regexs = new Pattern[size];
        String[] substs = new String[size];
        for (int i = 0; i < size; i++) {
            ids[i] = (AutofillId) this.mFields.keyAt(i);
            Pair<Pattern, String> pair = (Pair) this.mFields.valueAt(i);
            regexs[i] = (Pattern) pair.first;
            substs[i] = (String) pair.second;
        }
        parcel.writeParcelableArray(ids, flags);
        parcel.writeSerializable(regexs);
        parcel.writeStringArray(substs);
    }
}
