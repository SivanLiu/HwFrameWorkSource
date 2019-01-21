package android.content;

public class UndoOwner {
    Object mData;
    final UndoManager mManager;
    int mOpCount;
    int mSavedIdx;
    int mStateSeq;
    final String mTag;

    UndoOwner(String tag, UndoManager manager) {
        if (tag == null) {
            throw new NullPointerException("tag can't be null");
        } else if (manager != null) {
            this.mTag = tag;
            this.mManager = manager;
        } else {
            throw new NullPointerException("manager can't be null");
        }
    }

    public String getTag() {
        return this.mTag;
    }

    public Object getData() {
        return this.mData;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UndoOwner:[mTag=");
        stringBuilder.append(this.mTag);
        stringBuilder.append(" mManager=");
        stringBuilder.append(this.mManager);
        stringBuilder.append(" mData=");
        stringBuilder.append(this.mData);
        stringBuilder.append(" mData=");
        stringBuilder.append(this.mData);
        stringBuilder.append(" mOpCount=");
        stringBuilder.append(this.mOpCount);
        stringBuilder.append(" mStateSeq=");
        stringBuilder.append(this.mStateSeq);
        stringBuilder.append(" mSavedIdx=");
        stringBuilder.append(this.mSavedIdx);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
