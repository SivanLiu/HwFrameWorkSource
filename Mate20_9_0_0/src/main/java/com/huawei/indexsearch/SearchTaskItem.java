package com.huawei.indexsearch;

import java.util.List;

public class SearchTaskItem {
    private List<String> ids;
    private int op;
    private String pkgName;

    public String getPkgName() {
        return this.pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public List<String> getIds() {
        return this.ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public int getOp() {
        return this.op;
    }

    public void setOp(int op) {
        this.op = op;
    }
}
