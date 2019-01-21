package android.hardware.radio;

import android.annotation.SystemApi;
import android.hardware.radio.ProgramSelector.Identifier;
import android.hardware.radio.RadioManager.ProgramInfo;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@SystemApi
public final class ProgramList implements AutoCloseable {
    private boolean mIsClosed = false;
    private boolean mIsComplete = false;
    private final List<ListCallback> mListCallbacks = new ArrayList();
    private final Object mLock = new Object();
    private OnCloseListener mOnCloseListener;
    private final List<OnCompleteListener> mOnCompleteListeners = new ArrayList();
    private final Map<Identifier, ProgramInfo> mPrograms = new HashMap();

    public static abstract class ListCallback {
        public void onItemChanged(Identifier id) {
        }

        public void onItemRemoved(Identifier id) {
        }
    }

    interface OnCloseListener {
        void onClose();
    }

    public interface OnCompleteListener {
        void onComplete();
    }

    public static final class Chunk implements Parcelable {
        public static final Creator<Chunk> CREATOR = new Creator<Chunk>() {
            public Chunk createFromParcel(Parcel in) {
                return new Chunk(in, null);
            }

            public Chunk[] newArray(int size) {
                return new Chunk[size];
            }
        };
        private final boolean mComplete;
        private final Set<ProgramInfo> mModified;
        private final boolean mPurge;
        private final Set<Identifier> mRemoved;

        /* synthetic */ Chunk(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public Chunk(boolean purge, boolean complete, Set<ProgramInfo> modified, Set<Identifier> removed) {
            this.mPurge = purge;
            this.mComplete = complete;
            this.mModified = modified != null ? modified : Collections.emptySet();
            this.mRemoved = removed != null ? removed : Collections.emptySet();
        }

        private Chunk(Parcel in) {
            boolean z = false;
            this.mPurge = in.readByte() != (byte) 0;
            if (in.readByte() != (byte) 0) {
                z = true;
            }
            this.mComplete = z;
            this.mModified = Utils.createSet(in, ProgramInfo.CREATOR);
            this.mRemoved = Utils.createSet(in, Identifier.CREATOR);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) this.mPurge);
            dest.writeByte((byte) this.mComplete);
            Utils.writeSet(dest, this.mModified);
            Utils.writeSet(dest, this.mRemoved);
        }

        public int describeContents() {
            return 0;
        }

        public boolean isPurge() {
            return this.mPurge;
        }

        public boolean isComplete() {
            return this.mComplete;
        }

        public Set<ProgramInfo> getModified() {
            return this.mModified;
        }

        public Set<Identifier> getRemoved() {
            return this.mRemoved;
        }
    }

    public static final class Filter implements Parcelable {
        public static final Creator<Filter> CREATOR = new Creator<Filter>() {
            public Filter createFromParcel(Parcel in) {
                return new Filter(in, null);
            }

            public Filter[] newArray(int size) {
                return new Filter[size];
            }
        };
        private final boolean mExcludeModifications;
        private final Set<Integer> mIdentifierTypes;
        private final Set<Identifier> mIdentifiers;
        private final boolean mIncludeCategories;
        private final Map<String, String> mVendorFilter;

        /* synthetic */ Filter(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public Filter(Set<Integer> identifierTypes, Set<Identifier> identifiers, boolean includeCategories, boolean excludeModifications) {
            this.mIdentifierTypes = (Set) Objects.requireNonNull(identifierTypes);
            this.mIdentifiers = (Set) Objects.requireNonNull(identifiers);
            this.mIncludeCategories = includeCategories;
            this.mExcludeModifications = excludeModifications;
            this.mVendorFilter = null;
        }

        public Filter() {
            this.mIdentifierTypes = Collections.emptySet();
            this.mIdentifiers = Collections.emptySet();
            this.mIncludeCategories = false;
            this.mExcludeModifications = false;
            this.mVendorFilter = null;
        }

        public Filter(Map<String, String> vendorFilter) {
            this.mIdentifierTypes = Collections.emptySet();
            this.mIdentifiers = Collections.emptySet();
            this.mIncludeCategories = false;
            this.mExcludeModifications = false;
            this.mVendorFilter = vendorFilter;
        }

        private Filter(Parcel in) {
            this.mIdentifierTypes = Utils.createIntSet(in);
            this.mIdentifiers = Utils.createSet(in, Identifier.CREATOR);
            boolean z = false;
            this.mIncludeCategories = in.readByte() != (byte) 0;
            if (in.readByte() != (byte) 0) {
                z = true;
            }
            this.mExcludeModifications = z;
            this.mVendorFilter = Utils.readStringMap(in);
        }

        public void writeToParcel(Parcel dest, int flags) {
            Utils.writeIntSet(dest, this.mIdentifierTypes);
            Utils.writeSet(dest, this.mIdentifiers);
            dest.writeByte((byte) this.mIncludeCategories);
            dest.writeByte((byte) this.mExcludeModifications);
            Utils.writeStringMap(dest, this.mVendorFilter);
        }

        public int describeContents() {
            return 0;
        }

        public Map<String, String> getVendorFilter() {
            return this.mVendorFilter;
        }

        public Set<Integer> getIdentifierTypes() {
            return this.mIdentifierTypes;
        }

        public Set<Identifier> getIdentifiers() {
            return this.mIdentifiers;
        }

        public boolean areCategoriesIncluded() {
            return this.mIncludeCategories;
        }

        public boolean areModificationsExcluded() {
            return this.mExcludeModifications;
        }
    }

    ProgramList() {
    }

    public void registerListCallback(final Executor executor, final ListCallback callback) {
        registerListCallback(new ListCallback() {
            public void onItemChanged(Identifier id) {
                executor.execute(new -$$Lambda$ProgramList$1$DVvry5MfhR6n8H2EZn67rvuhllI(callback, id));
            }

            public void onItemRemoved(Identifier id) {
                executor.execute(new -$$Lambda$ProgramList$1$a_xWqo5pESOZhcJIWvpiCd2AXmY(callback, id));
            }
        });
    }

    public void registerListCallback(ListCallback callback) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mListCallbacks.add((ListCallback) Objects.requireNonNull(callback));
        }
    }

    public void unregisterListCallback(ListCallback callback) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mListCallbacks.remove(Objects.requireNonNull(callback));
        }
    }

    static /* synthetic */ void lambda$addOnCompleteListener$0(Executor executor, OnCompleteListener listener) {
        Objects.requireNonNull(listener);
        executor.execute(new -$$Lambda$1DA3e7WM2G0cVcFyFUhdDG0CYnw(listener));
    }

    public void addOnCompleteListener(Executor executor, OnCompleteListener listener) {
        addOnCompleteListener(new -$$Lambda$ProgramList$aDYMynqVdAUqeKXIxfNtN1u67zs(executor, listener));
    }

    /* JADX WARNING: Missing block: B:11:0x001c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addOnCompleteListener(OnCompleteListener listener) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mOnCompleteListeners.add((OnCompleteListener) Objects.requireNonNull(listener));
            if (this.mIsComplete) {
                listener.onComplete();
            }
        }
    }

    public void removeOnCompleteListener(OnCompleteListener listener) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mOnCompleteListeners.remove(Objects.requireNonNull(listener));
        }
    }

    void setOnCloseListener(OnCloseListener listener) {
        synchronized (this.mLock) {
            if (this.mOnCloseListener == null) {
                this.mOnCloseListener = listener;
            } else {
                throw new IllegalStateException("Close callback is already set");
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0028, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void close() {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsClosed = true;
            this.mPrograms.clear();
            this.mListCallbacks.clear();
            this.mOnCompleteListeners.clear();
            if (this.mOnCloseListener != null) {
                this.mOnCloseListener.onClose();
                this.mOnCloseListener = null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:14:0x005a, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void apply(Chunk chunk) {
        synchronized (this.mLock) {
            if (this.mIsClosed) {
                return;
            }
            this.mIsComplete = false;
            if (chunk.isPurge()) {
                new HashSet(this.mPrograms.keySet()).stream().forEach(new -$$Lambda$ProgramList$F-JpTj3vYguKIUQbnLbTePTuqUE(this));
            }
            chunk.getRemoved().stream().forEach(new -$$Lambda$ProgramList$pKu0Zp5jwjix619hfB_Imj8Ke_g(this));
            chunk.getModified().stream().forEach(new -$$Lambda$ProgramList$eY050tMTgAcGV9hiWR-UDxhkfhw(this));
            if (chunk.isComplete()) {
                this.mIsComplete = true;
                this.mOnCompleteListeners.forEach(-$$Lambda$ProgramList$GfCj9jJ5znxw2TV4c2uykq35dgI.INSTANCE);
            }
        }
    }

    private void putLocked(ProgramInfo value) {
        this.mPrograms.put((Identifier) Objects.requireNonNull(value.getSelector().getPrimaryId()), value);
        this.mListCallbacks.forEach(new -$$Lambda$ProgramList$fDnoTVk5UB7qTfD9S7SYPcadYn0(value.getSelector().getPrimaryId()));
    }

    private void removeLocked(Identifier key) {
        ProgramInfo removed = (ProgramInfo) this.mPrograms.remove(Objects.requireNonNull(key));
        if (removed != null) {
            this.mListCallbacks.forEach(new -$$Lambda$ProgramList$fHYelmhnUsVTYl6dFj75fMqCjGs(removed.getSelector().getPrimaryId()));
        }
    }

    public List<ProgramInfo> toList() {
        List list;
        synchronized (this.mLock) {
            list = (List) this.mPrograms.values().stream().collect(Collectors.toList());
        }
        return list;
    }

    public ProgramInfo get(Identifier id) {
        ProgramInfo programInfo;
        synchronized (this.mLock) {
            programInfo = (ProgramInfo) this.mPrograms.get(Objects.requireNonNull(id));
        }
        return programInfo;
    }
}
