package com.android.server.mtm.iaware.appmng.rule;

import android.app.mtm.iaware.appmng.AppMngConstant.AppStartSource;
import android.app.mtm.iaware.appmng.AppMngConstant.BroadcastSource;
import android.content.Intent;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.rule.RuleNode.StringIntegerMap;
import com.android.server.mtm.iaware.appmng.rule.RuleNode.XmlValue;
import com.android.server.mtm.iaware.srms.AwareBroadcastPolicy;
import com.android.server.rms.iaware.appmng.AppMngConfig;
import com.android.server.rms.iaware.appmng.AwareAppStartStatusCache;
import com.android.server.rms.iaware.appmng.AwareIntelligentRecg;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.srms.BroadcastExFeature;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import huawei.com.android.server.policy.stylus.StylusGestureSettings;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;

public class RuleParserUtil {
    private static final XmlValue DEFAULT_INT = new XmlValue(-1);
    private static final XmlValue DEFAULT_STRING = new XmlValue(MemoryConstant.MEM_SCENE_DEFAULT);

    public interface TagEnum {
        boolean equals(Object obj);

        String getDesc();

        int hashCode();

        boolean isStringInXml();
    }

    public enum AppMngTag implements TagEnum {
        TRISTATE("tristate", "tri") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, tristate);
            }
        },
        LEVEL(MemoryConstant.MEM_FILECACHE_ITEM_LEVEL, "lvl") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, level);
            }
        },
        TYPE(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE, HwSecDiagnoseConstant.ANTIMAL_APK_TYPE) {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngSpecType(packageName));
            }
        },
        STATUS("status", "status") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngSpecStat(processInfo, childItems.getIntegerMap()));
            }
        },
        RECENT("recent", "rec") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngSpecRecent(packageName, childItems.getIntegerMap()));
            }
        },
        TYPE_TOPN("type_topn", "typen") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngSpecTypeFreqTopN(packageName, childItems.getIntegerMap()));
            }
        },
        HABBIT_TOPN("habbit_topn", "habbitn") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngSpecHabbitTopN(packageName, childItems.getIntegerMap()));
            }
        },
        DEVICELEVEL("devicelevel", "devicelevel") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngDeviceLevel());
            }
        },
        POLICY("policy", "policy") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                LinkedHashMap<Integer, RuleNode> policyArray = childItems.getIntegerMap();
                if (policyArray == null || policyArray.size() != 1) {
                    return null;
                }
                return (RuleNode) ((Entry) policyArray.entrySet().iterator().next()).getValue();
            }
        },
        OVERSEA("oversea", "oversea") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, AwareProcessInfo processInfo, int level, String packageName, int tristate) {
                if (childItems == null || processInfo == null || packageName == null) {
                    return null;
                }
                return AppMngTag.getNodeAt(childItems, AppMngConfig.getRegionCode());
            }
        };
        
        private String mDesc;
        private String mUploadBDTag;

        public abstract RuleNode getAppliedValue(StringIntegerMap stringIntegerMap, AwareProcessInfo awareProcessInfo, int i, String str, int i2);

        private AppMngTag(String desc, String uploadBDTag) {
            this.mDesc = desc;
            this.mUploadBDTag = uploadBDTag;
        }

        public String getDesc() {
            return this.mDesc;
        }

        public String getUploadBDTag() {
            return this.mUploadBDTag;
        }

        public boolean isStringInXml() {
            return false;
        }

        public static TagEnum fromString(String desc) {
            if (desc == null) {
                return null;
            }
            try {
                return valueOf(desc.toUpperCase(Locale.US));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static RuleNode getNodeAt(StringIntegerMap childItems, int key) {
            RuleNode node = childItems.get(new XmlValue(key));
            if (node == null) {
                return childItems.get(RuleParserUtil.DEFAULT_INT);
            }
            return node;
        }
    }

    public enum AppStartTag implements TagEnum {
        TRISTATE("tristate") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, tristate);
            }
        },
        CALLER_ACTION("caller_action") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecCallerAction(packageName, status, childItems.getSortedList(), source));
            }
        },
        CALLER_STATUS("caller_status") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecCallerStatus(packageName, status, childItems.getSortedList()));
            }
        },
        TARGET_STATUS("target_status") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecTargetStatus(packageName, status, childItems.getSortedList()));
            }
        },
        TARGET_TYPE("target_type") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecTargetType(packageName, status, childItems.getSortedList()));
            }
        },
        OVERSEA("oversea") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecVerOversea(packageName, status));
            }
        },
        APPSRCRANGE("appsrcrange") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecAppSrcRange(packageName, status));
            }
        },
        APPOVERSEA("appoversea") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecAppOversea(packageName, status));
            }
        },
        SCREENSTATUS("screenstatus") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecScreenStatus(packageName, status));
            }
        },
        APPRECENT("apprecent") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngSpecRecent(packageName, childItems.getIntegerMap()));
            }
        },
        REGION(StylusGestureSettings.STYLUS_GESTURE_REGION_SUFFIX) {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppStartSpecRegion(packageName, status));
            }
        },
        DEVICELEVEL("devicelevel") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null) {
                    return null;
                }
                return AppStartTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngDeviceLevel());
            }
        },
        BROADCAST("broadcast") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || packageName == null || source == null || status == null || status.mAction == null) {
                    return null;
                }
                RuleNode node = childItems.get(new XmlValue(status.mAction));
                if (node == null) {
                    node = childItems.get(RuleParserUtil.DEFAULT_STRING);
                }
                return node;
            }

            public boolean isStringInXml() {
                return true;
            }
        },
        POLICY("policy") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, String packageName, AppStartSource source, AwareAppStartStatusCache status, int tristate) {
                if (childItems == null || source == null || status == null) {
                    return null;
                }
                LinkedHashMap<Integer, RuleNode> policyArray = childItems.getIntegerMap();
                if (policyArray == null || policyArray.size() != 1) {
                    return null;
                }
                return (RuleNode) ((Entry) policyArray.entrySet().iterator().next()).getValue();
            }
        };
        
        private String mDesc;

        public abstract RuleNode getAppliedValue(StringIntegerMap stringIntegerMap, String str, AppStartSource appStartSource, AwareAppStartStatusCache awareAppStartStatusCache, int i);

        private AppStartTag(String desc) {
            this.mDesc = desc;
        }

        public String getDesc() {
            return this.mDesc;
        }

        public boolean isStringInXml() {
            return false;
        }

        public static TagEnum fromString(String desc) {
            if (desc == null) {
                return null;
            }
            try {
                return valueOf(desc.toUpperCase(Locale.US));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static RuleNode getNodeAt(StringIntegerMap childItems, int key) {
            RuleNode node = childItems.get(new XmlValue(key));
            if (node == null) {
                return childItems.get(RuleParserUtil.DEFAULT_INT);
            }
            return node;
        }
    }

    public enum BroadcastTag implements TagEnum {
        TRISTATE("tristate") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, int tristate) {
                if (childItems == null || processInfo == null || id == null || source == null || intent == null) {
                    return null;
                }
                return BroadcastTag.getNodeAt(childItems, tristate);
            }
        },
        TYPE(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE) {
            public RuleNode getAppliedValue(StringIntegerMap childItems, BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, int tristate) {
                if (childItems == null || processInfo == null || id == null || source == null || intent == null) {
                    return null;
                }
                int appType;
                if (BroadcastExFeature.isBrGoogleApp(id)) {
                    appType = 10000;
                } else {
                    appType = AwareIntelligentRecg.getInstance().getAppMngSpecType(id);
                }
                return BroadcastTag.getNodeAt(childItems, appType);
            }
        },
        PROCSTATUS("procstatus") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, int tristate) {
                if (childItems == null || processInfo == null || id == null || source == null || intent == null) {
                    return null;
                }
                return BroadcastTag.getNodeAt(childItems, processInfo.getState());
            }
        },
        BROADCAST("broadcast") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, int tristate) {
                if (childItems == null || processInfo == null || id == null || source == null || intent == null) {
                    return null;
                }
                return childItems.get(new XmlValue(intent.getAction()));
            }

            public boolean isStringInXml() {
                return true;
            }
        },
        OVERSEA("oversea") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, int tristate) {
                if (childItems == null || processInfo == null || id == null || source == null || intent == null) {
                    return null;
                }
                return BroadcastTag.getNodeAt(childItems, AppMngConfig.getRegionCode());
            }
        },
        GOOGLESTATUS("googlestatus") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, int tristate) {
                if (childItems == null || processInfo == null || id == null || source == null || intent == null) {
                    return null;
                }
                return BroadcastTag.getNodeAt(childItems, AwareBroadcastPolicy.getGoogleConnStat());
            }
        },
        STATUS("status") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, int tristate) {
                if (childItems == null || processInfo == null || id == null || source == null || intent == null) {
                    return null;
                }
                return BroadcastTag.getNodeAt(childItems, AwareIntelligentRecg.getInstance().getAppMngSpecStat(processInfo, childItems.getIntegerMap()));
            }
        },
        POLICY("policy") {
            public RuleNode getAppliedValue(StringIntegerMap childItems, BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, int tristate) {
                if (childItems == null || processInfo == null || id == null || source == null || intent == null) {
                    return null;
                }
                LinkedHashMap<Integer, RuleNode> policyArray = childItems.getIntegerMap();
                if (policyArray == null || policyArray.size() != 1) {
                    return null;
                }
                return (RuleNode) ((Entry) policyArray.entrySet().iterator().next()).getValue();
            }
        };
        
        private String mDesc;

        public abstract RuleNode getAppliedValue(StringIntegerMap stringIntegerMap, BroadcastSource broadcastSource, String str, Intent intent, AwareProcessInfo awareProcessInfo, int i);

        private BroadcastTag(String desc) {
            this.mDesc = desc;
        }

        public String getDesc() {
            return this.mDesc;
        }

        public boolean isStringInXml() {
            return false;
        }

        public static TagEnum fromString(String desc) {
            if (desc == null) {
                return null;
            }
            try {
                return valueOf(desc.toUpperCase(Locale.US));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static RuleNode getNodeAt(StringIntegerMap childItems, int key) {
            RuleNode node = childItems.get(new XmlValue(key));
            if (node == null) {
                return childItems.get(RuleParserUtil.DEFAULT_INT);
            }
            return node;
        }
    }
}
