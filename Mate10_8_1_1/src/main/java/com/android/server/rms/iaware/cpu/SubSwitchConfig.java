package com.android.server.rms.iaware.cpu;

import android.os.SystemProperties;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import java.util.List;
import java.util.Map;

/* compiled from: CPUXmlConfiguration */
class SubSwitchConfig extends CPUCustBaseConfig {
    private static final int BIT_POS_MAX = 31;
    private static final int BIT_POS_MIN = 0;
    private static final String CONFIG_SUBSWITCH = "sub_switch";
    private static final int DEFAULT_SUBSWITCH = 0;
    private static final String ITEM_PROP_TYPE = "type";
    private static final String ITEM_TYPE_SWITCH = "switch";
    private static final String PROP_SUBSWITCH = "persist.sys.cpuset.subswitch";
    private static final String SUBITEM_PROP_BIT = "bit";
    private static final String TAG = "SubSwitchConfig";
    private int mSubSwitch;

    private int setBit(int r1, java.lang.String r2, int r3) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.server.rms.iaware.cpu.SubSwitchConfig.setBit(int, java.lang.String, int):int
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.rms.iaware.cpu.SubSwitchConfig.setBit(int, java.lang.String, int):int");
    }

    public SubSwitchConfig() {
        this.mSubSwitch = 0;
        this.mSubSwitch = getSubSwitch();
    }

    public void setConfig(CPUFeature feature) {
        try {
            SystemProperties.set(PROP_SUBSWITCH, Integer.toString(this.mSubSwitch));
        } catch (IllegalArgumentException e) {
            AwareLog.e(TAG, "set subswitch failed," + e.toString());
        }
    }

    private int getSubSwitch() {
        List<Item> awareConfigItemList = getItemList(CONFIG_SUBSWITCH);
        if (awareConfigItemList == null) {
            AwareLog.d(TAG, "can not get subswitch config");
            return 0;
        }
        for (Item item : awareConfigItemList) {
            Map<String, String> configPropertries = item.getProperties();
            if (configPropertries != null) {
                String itemType = (String) configPropertries.get("type");
                if (itemType != null && itemType.equals(ITEM_TYPE_SWITCH)) {
                    return getSubSwitchFromSubItem(item);
                }
            }
        }
        return 0;
    }

    private int getSubSwitchFromSubItem(Item item) {
        List<SubItem> subItemList = getSubItem(item);
        if (subItemList == null) {
            AwareLog.d(TAG, "can not get subswitch config subitem");
            return 0;
        }
        int subSwitch = 0;
        for (SubItem subItem : subItemList) {
            String itemValue = subItem.getValue();
            if (itemValue != null) {
                Map<String, String> subItemProps = subItem.getProperties();
                if (subItemProps != null) {
                    String bitPosStr = (String) subItemProps.get(SUBITEM_PROP_BIT);
                    if (bitPosStr != null) {
                        try {
                            int bitPos = Integer.parseInt(bitPosStr);
                            if (isInBitPosRange(bitPos)) {
                                subSwitch = setBit(subSwitch, itemValue, bitPos);
                            }
                        } catch (NumberFormatException e) {
                            AwareLog.d(TAG, "parse bitpos failed:" + bitPosStr);
                        }
                    }
                }
            }
        }
        return subSwitch;
    }

    private boolean isInBitPosRange(int bitPos) {
        return bitPos >= 0 && bitPos <= 31;
    }
}
