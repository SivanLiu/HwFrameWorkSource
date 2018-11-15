package com.android.server.mtm.iaware.appmng.rule;

import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppStartTag;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.TagEnum;
import com.android.server.rms.iaware.appmng.AppStartPolicyCfg.AppStartCallerAction;
import com.android.server.rms.iaware.appmng.AppStartPolicyCfg.AppStartCallerStatus;
import com.android.server.rms.iaware.appmng.AppStartPolicyCfg.AppStartTargetStat;
import com.android.server.rms.iaware.appmng.AppStartPolicyCfg.AppStartTargetType;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class RuleNode {
    private static final String TAG = "RuleNode";
    private StringIntegerMap mChildItems = new StringIntegerMap();
    private TagEnum mChildType;
    private TagEnum mCurrentType;
    private XmlValue mValue;

    public static class StringIntegerMap {
        private LinkedHashMap<Integer, RuleNode> mIntegerMap;
        boolean mIsStringMap;
        private ArrayList<Integer> mSortedList;
        private ArrayMap<String, RuleNode> mStringMap;

        public void put(String key, RuleNode item) {
            if (this.mStringMap == null) {
                this.mStringMap = new ArrayMap();
            }
            this.mStringMap.put(key, item);
            this.mIsStringMap = true;
        }

        public void put(int key, RuleNode item) {
            if (this.mIntegerMap == null) {
                this.mIntegerMap = new LinkedHashMap();
            }
            this.mIntegerMap.put(Integer.valueOf(key), item);
            this.mIsStringMap = false;
        }

        public RuleNode get(XmlValue key) {
            if (key == null) {
                return null;
            }
            if (key.isString()) {
                if (this.mStringMap != null) {
                    return (RuleNode) this.mStringMap.get(key.getStringValue());
                }
            } else if (this.mIntegerMap != null) {
                return (RuleNode) this.mIntegerMap.get(Integer.valueOf(key.getIntValue()));
            }
            return null;
        }

        public boolean isEmpty() {
            if (this.mStringMap != null) {
                return this.mStringMap.isEmpty();
            }
            boolean z = true;
            if (this.mIntegerMap == null) {
                return true;
            }
            if (this.mIntegerMap.size() != 0) {
                z = false;
            }
            return z;
        }

        public boolean isStringMap() {
            return this.mIsStringMap;
        }

        public ArrayMap<String, RuleNode> getStringMap() {
            return this.mStringMap;
        }

        public LinkedHashMap<Integer, RuleNode> getIntegerMap() {
            return this.mIntegerMap;
        }

        public ArrayList<Integer> getSortedList() {
            return this.mSortedList;
        }

        private boolean couldSort(AppStartTag tag, int value) {
            switch (tag) {
                case CALLER_ACTION:
                    if (value < 0 || value >= AppStartCallerAction.values().length) {
                        return false;
                    }
                case CALLER_STATUS:
                    if (value < 0 || value >= AppStartCallerStatus.values().length) {
                        return false;
                    }
                case TARGET_STATUS:
                    if (value < 0 || value >= AppStartTargetStat.values().length) {
                        return false;
                    }
                case TARGET_TYPE:
                    if (value < 0 || value >= AppStartTargetType.values().length) {
                        return false;
                    }
                default:
                    return false;
            }
            return true;
        }

        public void addToSortedList(TagEnum tag, int value) {
            if (!(tag instanceof AppStartTag) || !couldSort((AppStartTag) tag, value)) {
                return;
            }
            if (this.mSortedList == null) {
                this.mSortedList = new ArrayList();
                this.mSortedList.add(Integer.valueOf(value));
                return;
            }
            ArrayList<Integer> result = new ArrayList();
            boolean isAdded = false;
            int listSize = this.mSortedList.size();
            for (int i = 0; i < listSize; i++) {
                Integer val = (Integer) this.mSortedList.get(i);
                if (!isAdded) {
                    switch ((AppStartTag) tag) {
                        case CALLER_ACTION:
                            if (AppStartCallerAction.values()[value].getPriority() >= AppStartCallerAction.values()[val.intValue()].getPriority()) {
                                break;
                            }
                            result.add(Integer.valueOf(value));
                            isAdded = true;
                            break;
                        case CALLER_STATUS:
                            if (AppStartCallerStatus.values()[value].getPriority() >= AppStartCallerStatus.values()[val.intValue()].getPriority()) {
                                break;
                            }
                            result.add(Integer.valueOf(value));
                            isAdded = true;
                            break;
                        case TARGET_STATUS:
                            if (AppStartTargetStat.values()[value].getPriority() >= AppStartTargetStat.values()[val.intValue()].getPriority()) {
                                break;
                            }
                            result.add(Integer.valueOf(value));
                            isAdded = true;
                            break;
                        case TARGET_TYPE:
                            if (AppStartTargetType.values()[value].getPriority() >= AppStartTargetType.values()[val.intValue()].getPriority()) {
                                break;
                            }
                            result.add(Integer.valueOf(value));
                            isAdded = true;
                            break;
                        default:
                            return;
                    }
                }
                result.add(val);
            }
            if (!isAdded) {
                result.add(Integer.valueOf(value));
            }
            this.mSortedList = result;
        }
    }

    public static class XmlValue {
        private int mHwStop = -1;
        private String mIndex;
        private int mIntValue;
        private boolean mIsString;
        private String mStringValue;
        private int mWeight;

        public XmlValue(String value) {
            this.mStringValue = value;
            this.mIsString = true;
        }

        public XmlValue(int value) {
            this.mIntValue = value;
            this.mIsString = false;
        }

        public void setWeight(int weight) {
            this.mWeight = weight;
        }

        public int getWeight() {
            return this.mWeight;
        }

        public void setIndex(String index) {
            this.mIndex = index;
        }

        public void setHwStop(int hwStop) {
            this.mHwStop = hwStop;
        }

        public String getIndex() {
            return this.mIndex;
        }

        public int getHwStop() {
            return this.mHwStop;
        }

        public String getStringValue() {
            return this.mStringValue;
        }

        public int getIntValue() {
            return this.mIntValue;
        }

        public boolean isString() {
            return this.mIsString;
        }

        public String toString() {
            if (this.mIsString) {
                return this.mStringValue;
            }
            return Integer.toString(this.mIntValue);
        }
    }

    public RuleNode(TagEnum currentType, XmlValue value) {
        this.mCurrentType = currentType;
        this.mValue = value;
    }

    private boolean setChildType(TagEnum childType) {
        if (this.mChildType != null && !this.mChildType.equals(childType)) {
            return false;
        }
        this.mChildType = childType;
        return true;
    }

    private boolean addChildItemInternal(TagEnum childType, RuleNode item, boolean isSorted) {
        if (item == null || !setChildType(childType)) {
            return false;
        }
        XmlValue value = item.getValue();
        if (value != null) {
            if (value.isString()) {
                this.mChildItems.put(value.getStringValue(), item);
            } else {
                this.mChildItems.put(value.getIntValue(), item);
                if (isSorted) {
                    this.mChildItems.addToSortedList(childType, value.getIntValue());
                }
            }
        }
        return true;
    }

    public boolean addChildItem(TagEnum childType, RuleNode item) {
        return addChildItemInternal(childType, item, false);
    }

    public boolean addChildItemSorted(TagEnum childType, RuleNode item) {
        return addChildItemInternal(childType, item, true);
    }

    public XmlValue getValue() {
        return this.mValue;
    }

    public RuleNode getChild(XmlValue key) {
        return this.mChildItems.get(key);
    }

    public StringIntegerMap getChilds() {
        return this.mChildItems;
    }

    public TagEnum getChildType() {
        return this.mChildType;
    }

    public boolean hasChild() {
        return this.mChildItems.isEmpty() ^ 1;
    }

    public TagEnum getCurrentType() {
        return this.mCurrentType;
    }
}
