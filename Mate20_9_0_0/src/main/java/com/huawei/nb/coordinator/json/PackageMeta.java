package com.huawei.nb.coordinator.json;

import java.util.List;

public class PackageMeta {
    private String name;
    private String packageHashes;
    private String packageHashesSign;
    private List<PackagesBean> packages;

    public static class PackagesBean {
        private String name;
        private String size;

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSize() {
            return this.size;
        }

        public void setSize(String size) {
            this.size = size;
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageHashes() {
        return this.packageHashes;
    }

    public void setPackageHashes(String packageHashes) {
        this.packageHashes = packageHashes;
    }

    public String getPackageHashesSign() {
        return this.packageHashesSign;
    }

    public void setPackageHashesSign(String packageHashesSign) {
        this.packageHashesSign = packageHashesSign;
    }

    public List<PackagesBean> getPackages() {
        return this.packages;
    }

    public void setPackages(List<PackagesBean> packages) {
        this.packages = packages;
    }
}
