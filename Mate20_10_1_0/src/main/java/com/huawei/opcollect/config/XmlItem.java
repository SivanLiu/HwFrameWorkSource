package com.huawei.opcollect.config;

import com.huawei.opcollect.utils.OPCollectLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlItem {
    private static final String TAG = "XmlItem";
    private List<XmlItem> childXmlItems = null;
    private String itemText = null;
    private Map<String, String> propList = null;
    private String tagName = null;

    public void setTagName(String name) {
        this.tagName = name;
    }

    public String getTagName() {
        return this.tagName;
    }

    public void setText(String text) {
        this.itemText = text;
    }

    public String getText() {
        return this.itemText;
    }

    public void setProp(String key, String value) {
        if (key == null || value == null) {
            OPCollectLog.e(TAG, "setProp,key or value is null");
            return;
        }
        if (this.propList == null) {
            this.propList = new HashMap();
        }
        this.propList.put(key, value);
    }

    public String getProp(String key) {
        if (this.propList == null) {
            return null;
        }
        return this.propList.get(key);
    }

    public void setChildItem(XmlItem item) {
        if (item == null) {
            OPCollectLog.e(TAG, "childItem is null");
            return;
        }
        if (this.childXmlItems == null) {
            this.childXmlItems = new ArrayList();
        }
        this.childXmlItems.add(item);
    }

    public XmlItem getChildItem(String tag) {
        if (this.childXmlItems == null || tag == null) {
            return null;
        }
        for (XmlItem xmlItem : this.childXmlItems) {
            if (tag.equals(xmlItem.getTagName())) {
                return xmlItem;
            }
        }
        return null;
    }

    public List<XmlItem> getChildItemList() {
        return this.childXmlItems;
    }
}
