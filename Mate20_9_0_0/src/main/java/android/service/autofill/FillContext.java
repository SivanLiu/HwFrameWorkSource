package android.service.autofill;

import android.app.assist.AssistStructure;
import android.app.assist.AssistStructure.ViewNode;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.ArrayMap;
import android.util.SparseIntArray;
import android.view.autofill.AutofillId;
import android.view.autofill.Helper;
import java.util.LinkedList;

public final class FillContext implements Parcelable {
    public static final Creator<FillContext> CREATOR = new Creator<FillContext>() {
        public FillContext createFromParcel(Parcel parcel) {
            return new FillContext(parcel, null);
        }

        public FillContext[] newArray(int size) {
            return new FillContext[size];
        }
    };
    private final int mRequestId;
    private final AssistStructure mStructure;
    private ArrayMap<AutofillId, ViewNode> mViewNodeLookupTable;

    public FillContext(int requestId, AssistStructure structure) {
        this.mRequestId = requestId;
        this.mStructure = structure;
    }

    private FillContext(Parcel parcel) {
        this(parcel.readInt(), (AssistStructure) parcel.readParcelable(null));
    }

    public int getRequestId() {
        return this.mRequestId;
    }

    public AssistStructure getStructure() {
        return this.mStructure;
    }

    public String toString() {
        if (!Helper.sDebug) {
            return super.toString();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("FillContext [reqId=");
        stringBuilder.append(this.mRequestId);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.mRequestId);
        parcel.writeParcelable(this.mStructure, flags);
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x0099 A:{LOOP_END, LOOP:4: B:29:0x0093->B:31:0x0099} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ViewNode[] findViewNodesByAutofillIds(AutofillId[] ids) {
        int i;
        int lookupTableIndex;
        LinkedList<ViewNode> nodesToProcess = new LinkedList();
        ViewNode[] foundNodes = new ViewNode[ids.length];
        SparseIntArray missingNodeIndexes = new SparseIntArray(ids.length);
        int i2 = 0;
        for (i = 0; i < ids.length; i++) {
            if (this.mViewNodeLookupTable != null) {
                lookupTableIndex = this.mViewNodeLookupTable.indexOfKey(ids[i]);
                if (lookupTableIndex >= 0) {
                    foundNodes[i] = (ViewNode) this.mViewNodeLookupTable.valueAt(lookupTableIndex);
                } else {
                    missingNodeIndexes.put(i, 0);
                }
            } else {
                missingNodeIndexes.put(i, 0);
            }
        }
        i = this.mStructure.getWindowNodeCount();
        for (lookupTableIndex = 0; lookupTableIndex < i; lookupTableIndex++) {
            nodesToProcess.add(this.mStructure.getWindowNodeAt(lookupTableIndex).getRootViewNode());
        }
        while (missingNodeIndexes.size() > 0 && !nodesToProcess.isEmpty()) {
            ViewNode node = (ViewNode) nodesToProcess.removeFirst();
            int i3 = 0;
            while (i3 < missingNodeIndexes.size()) {
                int index = missingNodeIndexes.keyAt(i3);
                AutofillId id = ids[index];
                if (id.equals(node.getAutofillId())) {
                    foundNodes[index] = node;
                    if (this.mViewNodeLookupTable == null) {
                        this.mViewNodeLookupTable = new ArrayMap(ids.length);
                    }
                    this.mViewNodeLookupTable.put(id, node);
                    missingNodeIndexes.removeAt(i3);
                    for (i3 = 0; i3 < node.getChildCount(); i3++) {
                        nodesToProcess.addLast(node.getChildAt(i3));
                    }
                } else {
                    i3++;
                }
            }
            while (i3 < node.getChildCount()) {
            }
        }
        while (i2 < missingNodeIndexes.size()) {
            if (this.mViewNodeLookupTable == null) {
                this.mViewNodeLookupTable = new ArrayMap(missingNodeIndexes.size());
            }
            this.mViewNodeLookupTable.put(ids[missingNodeIndexes.keyAt(i2)], null);
            i2++;
        }
        return foundNodes;
    }
}
