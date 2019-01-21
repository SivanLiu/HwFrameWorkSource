package android.view.textclassifier;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.ArrayMap;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class EntityConfidence implements Parcelable {
    public static final Creator<EntityConfidence> CREATOR = new Creator<EntityConfidence>() {
        public EntityConfidence createFromParcel(Parcel in) {
            return new EntityConfidence(in, null);
        }

        public EntityConfidence[] newArray(int size) {
            return new EntityConfidence[size];
        }
    };
    private final ArrayMap<String, Float> mEntityConfidence;
    private final ArrayList<String> mSortedEntities;

    /* synthetic */ EntityConfidence(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    EntityConfidence() {
        this.mEntityConfidence = new ArrayMap();
        this.mSortedEntities = new ArrayList();
    }

    EntityConfidence(EntityConfidence source) {
        this.mEntityConfidence = new ArrayMap();
        this.mSortedEntities = new ArrayList();
        Preconditions.checkNotNull(source);
        this.mEntityConfidence.putAll(source.mEntityConfidence);
        this.mSortedEntities.addAll(source.mSortedEntities);
    }

    EntityConfidence(Map<String, Float> source) {
        this.mEntityConfidence = new ArrayMap();
        this.mSortedEntities = new ArrayList();
        Preconditions.checkNotNull(source);
        this.mEntityConfidence.ensureCapacity(source.size());
        for (Entry<String, Float> it : source.entrySet()) {
            if (((Float) it.getValue()).floatValue() > 0.0f) {
                this.mEntityConfidence.put((String) it.getKey(), Float.valueOf(Math.min(1.0f, ((Float) it.getValue()).floatValue())));
            }
        }
        resetSortedEntitiesFromMap();
    }

    public List<String> getEntities() {
        return Collections.unmodifiableList(this.mSortedEntities);
    }

    public float getConfidenceScore(String entity) {
        if (this.mEntityConfidence.containsKey(entity)) {
            return ((Float) this.mEntityConfidence.get(entity)).floatValue();
        }
        return 0.0f;
    }

    public String toString() {
        return this.mEntityConfidence.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mEntityConfidence.size());
        for (Entry<String, Float> entry : this.mEntityConfidence.entrySet()) {
            dest.writeString((String) entry.getKey());
            dest.writeFloat(((Float) entry.getValue()).floatValue());
        }
    }

    private EntityConfidence(Parcel in) {
        this.mEntityConfidence = new ArrayMap();
        this.mSortedEntities = new ArrayList();
        int numEntities = in.readInt();
        this.mEntityConfidence.ensureCapacity(numEntities);
        for (int i = 0; i < numEntities; i++) {
            this.mEntityConfidence.put(in.readString(), Float.valueOf(in.readFloat()));
        }
        resetSortedEntitiesFromMap();
    }

    private void resetSortedEntitiesFromMap() {
        this.mSortedEntities.clear();
        this.mSortedEntities.ensureCapacity(this.mEntityConfidence.size());
        this.mSortedEntities.addAll(this.mEntityConfidence.keySet());
        this.mSortedEntities.sort(new -$$Lambda$EntityConfidence$YPh8hwgSYYK8OyQ1kFlQngc71Q0(this));
    }
}
