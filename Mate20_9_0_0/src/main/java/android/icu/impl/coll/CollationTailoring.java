package android.icu.impl.coll;

import android.icu.impl.Norm2AllModes;
import android.icu.impl.Trie2_32;
import android.icu.impl.coll.SharedObject.Reference;
import android.icu.text.UnicodeSet;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.VersionInfo;
import java.util.Map;

public final class CollationTailoring {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    public ULocale actualLocale = ULocale.ROOT;
    public CollationData data;
    public Map<Integer, Integer> maxExpansions;
    CollationData ownedData;
    private String rules;
    private UResourceBundle rulesResource;
    public Reference<CollationSettings> settings;
    Trie2_32 trie;
    UnicodeSet unsafeBackwardSet;
    public int version = 0;

    CollationTailoring(Reference<CollationSettings> baseSettings) {
        if (baseSettings != null) {
            this.settings = baseSettings.clone();
        } else {
            this.settings = new Reference(new CollationSettings());
        }
    }

    void ensureOwnedData() {
        if (this.ownedData == null) {
            this.ownedData = new CollationData(Norm2AllModes.getNFCInstance().impl);
        }
        this.data = this.ownedData;
    }

    void setRules(String r) {
        this.rules = r;
    }

    void setRulesResource(UResourceBundle res) {
        this.rulesResource = res;
    }

    public String getRules() {
        if (this.rules != null) {
            return this.rules;
        }
        if (this.rulesResource != null) {
            return this.rulesResource.getString();
        }
        return "";
    }

    static VersionInfo makeBaseVersion(VersionInfo ucaVersion) {
        return VersionInfo.getInstance(VersionInfo.UCOL_BUILDER_VERSION.getMajor(), (ucaVersion.getMajor() << 3) + ucaVersion.getMinor(), ucaVersion.getMilli() << 6, 0);
    }

    void setVersion(int baseVersion, int rulesVersion) {
        int r = (rulesVersion >> 16) & 65280;
        int s = (rulesVersion >> 16) & 255;
        int q = rulesVersion & 255;
        this.version = (((VersionInfo.UCOL_BUILDER_VERSION.getMajor() << 24) | (16760832 & baseVersion)) | (((r >> 6) + r) & 16128)) | ((((((s << 3) + (s >> 5)) + ((rulesVersion >> 8) & 255)) + (q << 4)) + (q >> 4)) & 255);
    }

    int getUCAVersion() {
        return ((this.version >> 12) & 4080) | ((this.version >> 14) & 3);
    }
}
