package android.renderscript;

import android.util.SparseArray;
import java.io.UnsupportedEncodingException;

public class Script extends BaseObj {
    private final SparseArray<FieldID> mFIDs = new SparseArray();
    private final SparseArray<InvokeID> mIIDs = new SparseArray();
    long[] mInIdsBuffer = new long[1];
    private final SparseArray<KernelID> mKIDs = new SparseArray();

    public static class Builder {
        RenderScript mRS;

        Builder(RenderScript rs) {
            this.mRS = rs;
        }
    }

    public static class FieldBase {
        protected Allocation mAllocation;
        protected Element mElement;

        protected void init(RenderScript rs, int dimx) {
            this.mAllocation = Allocation.createSized(rs, this.mElement, dimx, 1);
        }

        protected void init(RenderScript rs, int dimx, int usages) {
            this.mAllocation = Allocation.createSized(rs, this.mElement, dimx, 1 | usages);
        }

        protected FieldBase() {
        }

        public Element getElement() {
            return this.mElement;
        }

        public Type getType() {
            return this.mAllocation.getType();
        }

        public Allocation getAllocation() {
            return this.mAllocation;
        }

        public void updateAllocation() {
        }
    }

    public static final class LaunchOptions {
        private int strategy;
        private int xend = 0;
        private int xstart = 0;
        private int yend = 0;
        private int ystart = 0;
        private int zend = 0;
        private int zstart = 0;

        public LaunchOptions setX(int xstartArg, int xendArg) {
            if (xstartArg < 0 || xendArg <= xstartArg) {
                throw new RSIllegalArgumentException("Invalid dimensions");
            }
            this.xstart = xstartArg;
            this.xend = xendArg;
            return this;
        }

        public LaunchOptions setY(int ystartArg, int yendArg) {
            if (ystartArg < 0 || yendArg <= ystartArg) {
                throw new RSIllegalArgumentException("Invalid dimensions");
            }
            this.ystart = ystartArg;
            this.yend = yendArg;
            return this;
        }

        public LaunchOptions setZ(int zstartArg, int zendArg) {
            if (zstartArg < 0 || zendArg <= zstartArg) {
                throw new RSIllegalArgumentException("Invalid dimensions");
            }
            this.zstart = zstartArg;
            this.zend = zendArg;
            return this;
        }

        public int getXStart() {
            return this.xstart;
        }

        public int getXEnd() {
            return this.xend;
        }

        public int getYStart() {
            return this.ystart;
        }

        public int getYEnd() {
            return this.yend;
        }

        public int getZStart() {
            return this.zstart;
        }

        public int getZEnd() {
            return this.zend;
        }
    }

    public static final class FieldID extends BaseObj {
        Script mScript;
        int mSlot;

        FieldID(long id, RenderScript rs, Script s, int slot) {
            super(id, rs);
            this.mScript = s;
            this.mSlot = slot;
        }
    }

    public static final class InvokeID extends BaseObj {
        Script mScript;
        int mSlot;

        InvokeID(long id, RenderScript rs, Script s, int slot) {
            super(id, rs);
            this.mScript = s;
            this.mSlot = slot;
        }
    }

    public static final class KernelID extends BaseObj {
        Script mScript;
        int mSig;
        int mSlot;

        KernelID(long id, RenderScript rs, Script s, int slot, int sig) {
            super(id, rs);
            this.mScript = s;
            this.mSlot = slot;
            this.mSig = sig;
        }
    }

    protected KernelID createKernelID(int slot, int sig, Element ein, Element eout) {
        KernelID k = (KernelID) this.mKIDs.get(slot);
        if (k != null) {
            return k;
        }
        long id = this.mRS.nScriptKernelIDCreate(getID(this.mRS), slot, sig);
        if (id != 0) {
            k = new KernelID(id, this.mRS, this, slot, sig);
            this.mKIDs.put(slot, k);
            return k;
        }
        throw new RSDriverException("Failed to create KernelID");
    }

    protected InvokeID createInvokeID(int slot) {
        InvokeID i = (InvokeID) this.mIIDs.get(slot);
        if (i != null) {
            return i;
        }
        long id = this.mRS.nScriptInvokeIDCreate(getID(this.mRS), slot);
        if (id != 0) {
            i = new InvokeID(id, this.mRS, this, slot);
            this.mIIDs.put(slot, i);
            return i;
        }
        throw new RSDriverException("Failed to create KernelID");
    }

    protected FieldID createFieldID(int slot, Element e) {
        FieldID f = (FieldID) this.mFIDs.get(slot);
        if (f != null) {
            return f;
        }
        long id = this.mRS.nScriptFieldIDCreate(getID(this.mRS), slot);
        if (id != 0) {
            f = new FieldID(id, this.mRS, this, slot);
            this.mFIDs.put(slot, f);
            return f;
        }
        throw new RSDriverException("Failed to create FieldID");
    }

    protected void invoke(int slot) {
        this.mRS.nScriptInvoke(getID(this.mRS), slot);
    }

    protected void invoke(int slot, FieldPacker v) {
        if (v != null) {
            this.mRS.nScriptInvokeV(getID(this.mRS), slot, v.getData());
        } else {
            this.mRS.nScriptInvoke(getID(this.mRS), slot);
        }
    }

    protected void forEach(int slot, Allocation ain, Allocation aout, FieldPacker v) {
        forEach(slot, ain, aout, v, null);
    }

    protected void forEach(int slot, Allocation ain, Allocation aout, FieldPacker v, LaunchOptions sc) {
        BaseObj baseObj = ain;
        BaseObj baseObj2 = aout;
        this.mRS.validate();
        this.mRS.validateObject(baseObj);
        this.mRS.validateObject(baseObj2);
        if (baseObj == null && baseObj2 == null && sc == null) {
            throw new RSIllegalArgumentException("At least one of input allocation, output allocation, or LaunchOptions is required to be non-null.");
        }
        long[] in_ids = null;
        if (baseObj != null) {
            in_ids = this.mInIdsBuffer;
            in_ids[0] = baseObj.getID(this.mRS);
        }
        long out_id = 0;
        if (baseObj2 != null) {
            out_id = baseObj2.getID(this.mRS);
        }
        byte[] params = null;
        if (v != null) {
            params = v.getData();
        }
        byte[] params2 = params;
        int[] limits = null;
        if (sc != null) {
            limits = new int[]{sc.xstart, sc.xend, sc.ystart, sc.yend, sc.zstart, sc.zend};
        }
        this.mRS.nScriptForEach(getID(this.mRS), slot, in_ids, out_id, params2, limits);
    }

    protected void forEach(int slot, Allocation[] ains, Allocation aout, FieldPacker v) {
        forEach(slot, ains, aout, v, null);
    }

    protected void forEach(int slot, Allocation[] ains, Allocation aout, FieldPacker v, LaunchOptions sc) {
        int i;
        Allocation[] allocationArr = ains;
        BaseObj baseObj = aout;
        this.mRS.validate();
        if (allocationArr != null) {
            for (Allocation ain : allocationArr) {
                this.mRS.validateObject(ain);
            }
        }
        this.mRS.validateObject(baseObj);
        if (allocationArr == null && baseObj == null) {
            throw new RSIllegalArgumentException("At least one of ain or aout is required to be non-null.");
        }
        long[] jArr;
        if (allocationArr != null) {
            jArr = new long[allocationArr.length];
            for (i = 0; i < allocationArr.length; i++) {
                jArr[i] = allocationArr[i].getID(this.mRS);
            }
        } else {
            jArr = null;
        }
        long[] in_ids = jArr;
        long out_id = 0;
        if (baseObj != null) {
            out_id = baseObj.getID(this.mRS);
        }
        long out_id2 = out_id;
        byte[] params = null;
        if (v != null) {
            params = v.getData();
        }
        int[] limits = null;
        if (sc != null) {
            limits = new int[]{sc.xstart, sc.xend, sc.ystart, sc.yend, sc.zstart, sc.zend};
        }
        this.mRS.nScriptForEach(getID(this.mRS), slot, in_ids, out_id2, params, limits);
    }

    protected void reduce(int slot, Allocation[] ains, Allocation aout, LaunchOptions sc) {
        Allocation[] allocationArr = ains;
        Allocation allocation = aout;
        this.mRS.validate();
        if (allocationArr == null || allocationArr.length < 1) {
            throw new RSIllegalArgumentException("At least one input is required.");
        } else if (allocation != null) {
            int i;
            for (Allocation ain : allocationArr) {
                this.mRS.validateObject(ain);
            }
            long[] in_ids = new long[allocationArr.length];
            for (i = 0; i < allocationArr.length; i++) {
                in_ids[i] = allocationArr[i].getID(this.mRS);
            }
            long out_id = allocation.getID(this.mRS);
            int[] limits = null;
            if (sc != null) {
                limits = new int[]{sc.xstart, sc.xend, sc.ystart, sc.yend, sc.zstart, sc.zend};
            }
            this.mRS.nScriptReduce(getID(this.mRS), slot, in_ids, out_id, limits);
        } else {
            throw new RSIllegalArgumentException("aout is required to be non-null.");
        }
    }

    Script(long id, RenderScript rs) {
        super(id, rs);
        this.guard.open("destroy");
    }

    public void bindAllocation(Allocation va, int slot) {
        BaseObj baseObj = va;
        this.mRS.validate();
        this.mRS.validateObject(baseObj);
        if (baseObj != null) {
            if (this.mRS.getApplicationContext().getApplicationInfo().targetSdkVersion >= 20) {
                Type t = baseObj.mType;
                if (t.hasMipmaps() || t.hasFaces() || t.getY() != 0 || t.getZ() != 0) {
                    throw new RSIllegalArgumentException("API 20+ only allows simple 1D allocations to be used with bind.");
                }
            }
            this.mRS.nScriptBindAllocation(getID(this.mRS), baseObj.getID(this.mRS), slot);
            return;
        }
        this.mRS.nScriptBindAllocation(getID(this.mRS), 0, slot);
    }

    public void setVar(int index, float v) {
        this.mRS.nScriptSetVarF(getID(this.mRS), index, v);
    }

    public float getVarF(int index) {
        return this.mRS.nScriptGetVarF(getID(this.mRS), index);
    }

    public void setVar(int index, double v) {
        this.mRS.nScriptSetVarD(getID(this.mRS), index, v);
    }

    public double getVarD(int index) {
        return this.mRS.nScriptGetVarD(getID(this.mRS), index);
    }

    public void setVar(int index, int v) {
        this.mRS.nScriptSetVarI(getID(this.mRS), index, v);
    }

    public int getVarI(int index) {
        return this.mRS.nScriptGetVarI(getID(this.mRS), index);
    }

    public void setVar(int index, long v) {
        this.mRS.nScriptSetVarJ(getID(this.mRS), index, v);
    }

    public long getVarJ(int index) {
        return this.mRS.nScriptGetVarJ(getID(this.mRS), index);
    }

    public void setVar(int index, boolean v) {
        this.mRS.nScriptSetVarI(getID(this.mRS), index, v);
    }

    public boolean getVarB(int index) {
        return this.mRS.nScriptGetVarI(getID(this.mRS), index) > 0;
    }

    public void setVar(int index, BaseObj o) {
        this.mRS.validate();
        this.mRS.validateObject(o);
        this.mRS.nScriptSetVarObj(getID(this.mRS), index, o == null ? 0 : o.getID(this.mRS));
    }

    public void setVar(int index, FieldPacker v) {
        this.mRS.nScriptSetVarV(getID(this.mRS), index, v.getData());
    }

    public void setVar(int index, FieldPacker v, Element e, int[] dims) {
        this.mRS.nScriptSetVarVE(getID(this.mRS), index, v.getData(), e.getID(this.mRS), dims);
    }

    public void getVarV(int index, FieldPacker v) {
        this.mRS.nScriptGetVarV(getID(this.mRS), index, v.getData());
    }

    public void setTimeZone(String timeZone) {
        this.mRS.validate();
        try {
            this.mRS.nScriptSetTimeZone(getID(this.mRS), timeZone.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
