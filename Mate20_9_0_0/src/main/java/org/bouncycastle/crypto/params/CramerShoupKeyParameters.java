package org.bouncycastle.crypto.params;

public class CramerShoupKeyParameters extends AsymmetricKeyParameter {
    private CramerShoupParameters params;

    protected CramerShoupKeyParameters(boolean z, CramerShoupParameters cramerShoupParameters) {
        super(z);
        this.params = cramerShoupParameters;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof CramerShoupKeyParameters)) {
            return false;
        }
        CramerShoupKeyParameters cramerShoupKeyParameters = (CramerShoupKeyParameters) obj;
        if (this.params != null) {
            return this.params.equals(cramerShoupKeyParameters.getParameters());
        }
        if (cramerShoupKeyParameters.getParameters() == null) {
            z = true;
        }
        return z;
    }

    public CramerShoupParameters getParameters() {
        return this.params;
    }

    public int hashCode() {
        int isPrivate = isPrivate() ^ 1;
        return this.params != null ? isPrivate ^ this.params.hashCode() : isPrivate;
    }
}
