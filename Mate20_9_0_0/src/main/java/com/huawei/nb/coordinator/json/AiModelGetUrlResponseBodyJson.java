package com.huawei.nb.coordinator.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class AiModelGetUrlResponseBodyJson {
    private String abTest;
    private JsonArray appVersion;
    private String chipset;
    private String chipsetVendor;
    private String decryptedKey;
    private String district;
    private String emuiFamily;
    private JsonObject extra;
    private String interfaceVersion;
    @SerializedName("package")
    private String packageX;
    private String param1;
    private String param2;
    private String product;
    private String productFamily;
    private String productModel;
    private String resid;
    private String teams;
    private boolean type;
    private boolean update;
    private String url;
    private String version;
    private String xpu;
    private String zipSha256;

    public String getResid() {
        return this.resid;
    }

    public void setResid(String resid) {
        this.resid = resid;
    }

    public String getPackageX() {
        return this.packageX;
    }

    public void setPackageX(String packageX) {
        this.packageX = packageX;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTeams() {
        return this.teams;
    }

    public void setTeams(String teams) {
        this.teams = teams;
    }

    public String getZipSha256() {
        return this.zipSha256;
    }

    public void setZipSha256(String zipSha256) {
        this.zipSha256 = zipSha256;
    }

    public String getDecryptedKey() {
        return this.decryptedKey;
    }

    public void setDecryptedKey(String decryptedKey) {
        this.decryptedKey = decryptedKey;
    }

    public boolean isType() {
        return this.type;
    }

    public void setType(boolean type) {
        this.type = type;
    }

    public boolean isUpdate() {
        return this.update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public String getXpu() {
        return this.xpu;
    }

    public void setXpu(String xpu) {
        this.xpu = xpu;
    }

    public String getEmuiFamily() {
        return this.emuiFamily;
    }

    public void setEmuiFamily(String emuiFamily) {
        this.emuiFamily = emuiFamily;
    }

    public String getProductFamily() {
        return this.productFamily;
    }

    public void setProductFamily(String productFamily) {
        this.productFamily = productFamily;
    }

    public String getChipsetVendor() {
        return this.chipsetVendor;
    }

    public void setChipsetVendor(String chipsetVendor) {
        this.chipsetVendor = chipsetVendor;
    }

    public String getChipset() {
        return this.chipset;
    }

    public void setChipset(String chipset) {
        this.chipset = chipset;
    }

    public String getProduct() {
        return this.product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProductModel() {
        return this.productModel;
    }

    public void setProductModel(String productModel) {
        this.productModel = productModel;
    }

    public String getDistrict() {
        return this.district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAbTest() {
        return this.abTest;
    }

    public void setAbTest(String abTest) {
        this.abTest = abTest;
    }

    public String getInterfaceVersion() {
        return this.interfaceVersion;
    }

    public void setInterfaceVersion(String interfaceVersion) {
        this.interfaceVersion = interfaceVersion;
    }

    public String getParam1() {
        return this.param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    public String getParam2() {
        return this.param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
    }

    public JsonObject getExtra() {
        return this.extra;
    }

    public void setExtra(JsonObject extra) {
        this.extra = extra;
    }

    public JsonArray getAppVersion() {
        return this.appVersion;
    }

    public void setAppVersion(JsonArray appVersion) {
        this.appVersion = appVersion;
    }
}
