package android.service.autofill;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Pair;
import android.view.autofill.Helper;
import android.widget.RemoteViews;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;

public final class BatchUpdates implements Parcelable {
    public static final Creator<BatchUpdates> CREATOR = new Creator<BatchUpdates>() {
        public BatchUpdates createFromParcel(Parcel parcel) {
            Builder builder = new Builder();
            int[] ids = parcel.createIntArray();
            if (ids != null) {
                InternalTransformation[] values = (InternalTransformation[]) parcel.readParcelableArray(null, InternalTransformation.class);
                int size = ids.length;
                for (int i = 0; i < size; i++) {
                    builder.transformChild(ids[i], values[i]);
                }
            }
            RemoteViews updates = (RemoteViews) parcel.readParcelable(null);
            if (updates != null) {
                builder.updateTemplate(updates);
            }
            return builder.build();
        }

        public BatchUpdates[] newArray(int size) {
            return new BatchUpdates[size];
        }
    };
    private final ArrayList<Pair<Integer, InternalTransformation>> mTransformations;
    private final RemoteViews mUpdates;

    public static class Builder {
        private boolean mDestroyed;
        private ArrayList<Pair<Integer, InternalTransformation>> mTransformations;
        private RemoteViews mUpdates;

        public Builder updateTemplate(RemoteViews updates) {
            throwIfDestroyed();
            this.mUpdates = (RemoteViews) Preconditions.checkNotNull(updates);
            return this;
        }

        public Builder transformChild(int id, Transformation transformation) {
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

        public BatchUpdates build() {
            throwIfDestroyed();
            boolean z = (this.mUpdates == null && this.mTransformations == null) ? false : true;
            Preconditions.checkState(z, "must call either updateTemplate() or transformChild() at least once");
            this.mDestroyed = true;
            return new BatchUpdates(this, null);
        }

        private void throwIfDestroyed() {
            if (this.mDestroyed) {
                throw new IllegalStateException("Already called #build()");
            }
        }
    }

    /* synthetic */ BatchUpdates(Builder x0, AnonymousClass1 x1) {
        this(x0);
    }

    private BatchUpdates(Builder builder) {
        this.mTransformations = builder.mTransformations;
        this.mUpdates = builder.mUpdates;
    }

    public ArrayList<Pair<Integer, InternalTransformation>> getTransformations() {
        return this.mTransformations;
    }

    public RemoteViews getUpdates() {
        return this.mUpdates;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder stringBuilder = new StringBuilder("BatchUpdates: [");
        stringBuilder.append(", transformations=");
        stringBuilder.append(this.mTransformations == null ? "N/A" : Integer.valueOf(this.mTransformations.size()));
        stringBuilder.append(", updates=");
        stringBuilder.append(this.mUpdates);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (this.mTransformations == null) {
            dest.writeIntArray(null);
        } else {
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
        dest.writeParcelable(this.mUpdates, flags);
    }
}
