package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Pair;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;

public final class CustomDescription implements Parcelable {
    public static final Creator<CustomDescription> CREATOR = new Creator<CustomDescription>() {
        public CustomDescription createFromParcel(Parcel parcel) {
            RemoteViews parentPresentation = (RemoteViews) parcel.readParcelable(null);
            if (parentPresentation == null) {
                return null;
            }
            int size;
            Builder builder = new Builder(parentPresentation);
            int[] ids = parcel.createIntArray();
            int i = 0;
            if (ids != null) {
                InternalTransformation[] values = (InternalTransformation[]) parcel.readParcelableArray(null, InternalTransformation.class);
                size = ids.length;
                for (int i2 = 0; i2 < size; i2++) {
                    builder.addChild(ids[i2], values[i2]);
                }
            }
            InternalValidator[] conditions = (InternalValidator[]) parcel.readParcelableArray(null, InternalValidator.class);
            if (conditions != null) {
                BatchUpdates[] updates = (BatchUpdates[]) parcel.readParcelableArray(null, BatchUpdates.class);
                size = conditions.length;
                while (i < size) {
                    builder.batchUpdate(conditions[i], updates[i]);
                    i++;
                }
            }
            return builder.build();
        }

        public CustomDescription[] newArray(int size) {
            return new CustomDescription[size];
        }
    };
    private final RemoteViews mPresentation;
    private final ArrayList<Pair<Integer, InternalTransformation>> mTransformations;
    private final ArrayList<Pair<InternalValidator, BatchUpdates>> mUpdates;

    public static class Builder {
        private boolean mDestroyed;
        private final RemoteViews mPresentation;
        private ArrayList<Pair<Integer, InternalTransformation>> mTransformations;
        private ArrayList<Pair<InternalValidator, BatchUpdates>> mUpdates;

        public Builder(RemoteViews parentPresentation) {
            this.mPresentation = (RemoteViews) Preconditions.checkNotNull(parentPresentation);
        }

        public Builder addChild(int id, Transformation transformation) {
            throwIfDestroyed();
            boolean z = transformation instanceof InternalTransformation;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("not provided by Android System: ");
            stringBuilder.append(transformation);
            Preconditions.checkArgument(z, stringBuilder.toString());
            if (this.mTransformations == null) {
                this.mTransformations = new ArrayList();
            }
            this.mTransformations.add(new Pair(Integer.valueOf(id), (InternalTransformation) transformation));
            return this;
        }

        public Builder batchUpdate(Validator condition, BatchUpdates updates) {
            throwIfDestroyed();
            boolean z = condition instanceof InternalValidator;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("not provided by Android System: ");
            stringBuilder.append(condition);
            Preconditions.checkArgument(z, stringBuilder.toString());
            Preconditions.checkNotNull(updates);
            if (this.mUpdates == null) {
                this.mUpdates = new ArrayList();
            }
            this.mUpdates.add(new Pair((InternalValidator) condition, updates));
            return this;
        }

        public CustomDescription build() {
            throwIfDestroyed();
            this.mDestroyed = true;
            return new CustomDescription(this, null);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    /* synthetic */ CustomDescription(Builder x0, AnonymousClass1 x1) {
        this(x0);
    }

    private CustomDescription(Builder builder) {
        this.mPresentation = builder.mPresentation;
        this.mTransformations = builder.mTransformations;
        this.mUpdates = builder.mUpdates;
    }

    public RemoteViews getPresentation() {
        return this.mPresentation;
    }

    public ArrayList<Pair<Integer, InternalTransformation>> getTransformations() {
        return this.mTransformations;
    }

    public ArrayList<Pair<InternalValidator, BatchUpdates>> getUpdates() {
        return this.mUpdates;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder stringBuilder = new StringBuilder("CustomDescription: [presentation=");
        stringBuilder.append(this.mPresentation);
        stringBuilder.append(", transformations=");
        stringBuilder.append(this.mTransformations == null ? "N/A" : Integer.valueOf(this.mTransformations.size()));
        stringBuilder.append(", updates=");
        stringBuilder.append(this.mUpdates == null ? "N/A" : Integer.valueOf(this.mUpdates.size()));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mPresentation, flags);
        if (this.mPresentation != null) {
            int size;
            int i = 0;
            if (this.mTransformations == null) {
                dest.writeIntArray(null);
            } else {
                size = this.mTransformations.size();
                int[] ids = new int[size];
                InternalTransformation[] values = new InternalTransformation[size];
                for (int i2 = 0; i2 < size; i2++) {
                    Pair<Integer, InternalTransformation> pair = (Pair) this.mTransformations.get(i2);
                    ids[i2] = ((Integer) pair.first).intValue();
                    values[i2] = (InternalTransformation) pair.second;
                }
                dest.writeIntArray(ids);
                dest.writeParcelableArray(values, flags);
            }
            if (this.mUpdates == null) {
                dest.writeParcelableArray(null, flags);
            } else {
                size = this.mUpdates.size();
                InternalValidator[] conditions = new InternalValidator[size];
                BatchUpdates[] updates = new BatchUpdates[size];
                while (i < size) {
                    Pair<InternalValidator, BatchUpdates> pair2 = (Pair) this.mUpdates.get(i);
                    conditions[i] = (InternalValidator) pair2.first;
                    updates[i] = (BatchUpdates) pair2.second;
                    i++;
                }
                dest.writeParcelableArray(conditions, flags);
                dest.writeParcelableArray(updates, flags);
            }
        }
    }
}
