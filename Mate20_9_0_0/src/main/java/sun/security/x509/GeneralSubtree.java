package sun.security.x509;

import java.io.IOException;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class GeneralSubtree {
    private static final int MIN_DEFAULT = 0;
    private static final byte TAG_MAX = (byte) 1;
    private static final byte TAG_MIN = (byte) 0;
    private int maximum = -1;
    private int minimum = 0;
    private int myhash = -1;
    private GeneralName name;

    public GeneralSubtree(GeneralName name, int min, int max) {
        this.name = name;
        this.minimum = min;
        this.maximum = max;
    }

    public GeneralSubtree(DerValue val) throws IOException {
        if (val.tag == (byte) 48) {
            this.name = new GeneralName(val.data.getDerValue(), true);
            while (val.data.available() != 0) {
                DerValue opt = val.data.getDerValue();
                if (opt.isContextSpecific((byte) 0) && !opt.isConstructed()) {
                    opt.resetTag((byte) 2);
                    this.minimum = opt.getInteger();
                } else if (!opt.isContextSpecific((byte) 1) || opt.isConstructed()) {
                    throw new IOException("Invalid encoding of GeneralSubtree.");
                } else {
                    opt.resetTag((byte) 2);
                    this.maximum = opt.getInteger();
                }
            }
            return;
        }
        throw new IOException("Invalid encoding for GeneralSubtree.");
    }

    public GeneralName getName() {
        return this.name;
    }

    public int getMinimum() {
        return this.minimum;
    }

    public int getMaximum() {
        return this.maximum;
    }

    public String toString() {
        StringBuilder stringBuilder;
        String s = new StringBuilder();
        s.append("\n   GeneralSubtree: [\n    GeneralName: ");
        s.append(this.name == null ? "" : this.name.toString());
        s.append("\n    Minimum: ");
        s.append(this.minimum);
        s = s.toString();
        if (this.maximum == -1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(s);
            stringBuilder.append("\t    Maximum: undefined");
            s = stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(s);
            stringBuilder.append("\t    Maximum: ");
            stringBuilder.append(this.maximum);
            s = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(s);
        stringBuilder.append("    ]\n");
        return stringBuilder.toString();
    }

    public boolean equals(Object other) {
        if (!(other instanceof GeneralSubtree)) {
            return false;
        }
        GeneralSubtree otherGS = (GeneralSubtree) other;
        if (this.name == null) {
            if (otherGS.name != null) {
                return false;
            }
        } else if (!this.name.equals(otherGS.name)) {
            return false;
        }
        if (this.minimum == otherGS.minimum && this.maximum == otherGS.maximum) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        if (this.myhash == -1) {
            this.myhash = 17;
            if (this.name != null) {
                this.myhash = (this.myhash * 37) + this.name.hashCode();
            }
            if (this.minimum != 0) {
                this.myhash = (this.myhash * 37) + this.minimum;
            }
            if (this.maximum != -1) {
                this.myhash = (37 * this.myhash) + this.maximum;
            }
        }
        return this.myhash;
    }

    public void encode(DerOutputStream out) throws IOException {
        DerOutputStream tmp;
        DerOutputStream seq = new DerOutputStream();
        this.name.encode(seq);
        if (this.minimum != 0) {
            tmp = new DerOutputStream();
            tmp.putInteger(this.minimum);
            seq.writeImplicit(DerValue.createTag(Byte.MIN_VALUE, false, (byte) 0), tmp);
        }
        if (this.maximum != -1) {
            tmp = new DerOutputStream();
            tmp.putInteger(this.maximum);
            seq.writeImplicit(DerValue.createTag(Byte.MIN_VALUE, false, (byte) 1), tmp);
        }
        out.write((byte) 48, seq);
    }
}
