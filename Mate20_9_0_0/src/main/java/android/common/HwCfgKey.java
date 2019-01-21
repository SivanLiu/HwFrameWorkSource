package android.common;

public class HwCfgKey {
    public String fvalue;
    public String ifkey;
    public String iskey;
    public String itkey;
    public String key;
    public String rkey;
    public int slotid;
    public String svalue;
    public String tvalue;

    public HwCfgKey(String key, String ifkey, String iskey, String itkey, String rkey, String fvalue, String svalue, String tvalue, int slotid) {
        this.key = key;
        this.ifkey = ifkey;
        this.iskey = iskey;
        this.itkey = itkey;
        this.rkey = rkey;
        this.fvalue = fvalue;
        this.svalue = svalue;
        this.tvalue = tvalue;
        this.slotid = slotid;
    }
}
