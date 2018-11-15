package com.gsma.services.nfc;

import java.util.ArrayList;
import java.util.List;

public class AidGroup {
    private List<String> mAidList = new ArrayList();
    private String mCategory = null;
    private String mDescription = null;

    AidGroup() {
    }

    AidGroup(String description, String category) {
        this.mDescription = description;
        this.mCategory = category;
    }

    public String getCategory() {
        return this.mCategory;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public void addNewAid(String aid) {
        if (aid == null || aid.isEmpty()) {
            throw new IllegalArgumentException("Invalid AID");
        }
        this.mAidList.add(aid.toUpperCase());
    }

    public void removeAid(String aid) {
        if (aid == null || aid.isEmpty()) {
            throw new IllegalArgumentException("Invalid AID");
        }
        this.mAidList.remove(aid);
    }

    public List<String> getAidList() {
        return this.mAidList;
    }

    public String[] getAids() {
        return (String[]) this.mAidList.toArray(new String[this.mAidList.size()]);
    }

    public String toString() {
        StringBuffer out = new StringBuffer("AidGroup: ");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mDescription: ");
        stringBuilder.append(this.mDescription);
        out.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(", mCategory: ");
        stringBuilder.append(this.mCategory);
        out.append(stringBuilder.toString());
        for (String aid : this.mAidList) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(", aid: ");
            stringBuilder2.append(aid);
            out.append(stringBuilder2.toString());
        }
        return out.toString();
    }
}
