package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import android.util.Pair;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;

public final class CustomDescription implements Parcelable {
    public static final Creator<CustomDescription> CREATOR = new Creator<CustomDescription>() {
        public CustomDescription createFromParcel(Parcel parcel) {
            Builder builder = new Builder((RemoteViews) parcel.readParcelable(null));
            int[] ids = parcel.createIntArray();
            if (ids != null) {
                InternalTransformation[] values = (InternalTransformation[]) parcel.readParcelableArray(null, InternalTransformation.class);
                int size = ids.length;
                for (int i = 0; i < size; i++) {
                    builder.addChild(ids[i], values[i]);
                }
            }
            return builder.build();
        }

        public CustomDescription[] newArray(int size) {
            return new CustomDescription[size];
        }
    };
    private static final String TAG = "CustomDescription";
    private final RemoteViews mPresentation;
    private final ArrayList<Pair<Integer, InternalTransformation>> mTransformations;

    public static class Builder {
        private final RemoteViews mPresentation;
        private ArrayList<Pair<Integer, InternalTransformation>> mTransformations;

        public Builder(RemoteViews parentPresentation) {
            this.mPresentation = parentPresentation;
        }

        public Builder addChild(int id, Transformation transformation) {
            Preconditions.checkArgument(transformation instanceof InternalTransformation, "not provided by Android System: " + transformation);
            if (this.mTransformations == null) {
                this.mTransformations = new ArrayList();
            }
            this.mTransformations.add(new Pair(Integer.valueOf(id), (InternalTransformation) transformation));
            return this;
        }

        public CustomDescription build() {
            return new CustomDescription();
        }
    }

    private CustomDescription(Builder builder) {
        this.mPresentation = builder.mPresentation;
        this.mTransformations = builder.mTransformations;
    }

    public RemoteViews getPresentation(ValueFinder finder) {
        if (this.mTransformations != null) {
            int size = this.mTransformations.size();
            if (Helper.sDebug) {
                Log.d(TAG, "getPresentation(): applying " + size + " transformations");
            }
            int i = 0;
            while (i < size) {
                Pair<Integer, InternalTransformation> pair = (Pair) this.mTransformations.get(i);
                int id = ((Integer) pair.first).intValue();
                InternalTransformation transformation = pair.second;
                if (Helper.sDebug) {
                    Log.d(TAG, "#" + i + ": " + transformation);
                }
                try {
                    transformation.apply(finder, this.mPresentation, id);
                    i++;
                } catch (Exception e) {
                    Log.e(TAG, "Could not apply transformation " + transformation + ": " + e.getClass());
                    return null;
                }
            }
        }
        return this.mPresentation;
    }

    public String toString() {
        if (Helper.sDebug) {
            return "CustomDescription: [presentation=" + this.mPresentation + ", transformations=" + this.mTransformations + "]";
        }
        return super.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mPresentation, flags);
        if (this.mTransformations == null) {
            dest.writeIntArray(null);
            return;
        }
        int size = this.mTransformations.size();
        int[] ids = new int[size];
        InternalTransformation[] values = new InternalTransformation[size];
        for (int i = 0; i < size; i++) {
            Pair<Integer, InternalTransformation> pair = (Pair) this.mTransformations.get(i);
            ids[i] = ((Integer) pair.first).intValue();
            values[i] = (InternalTransformation) pair.second;
        }
        dest.writeIntArray(ids);
        dest.writeParcelableArray(values, flags);
    }
}
