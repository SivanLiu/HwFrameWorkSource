package com.android.server.hidata.wavemapping.chr.entity;

public class ModelInvalidChrInfo extends ChrInfo {
    private int identifyAll;
    private int identifyCell;
    private int identifyMain;
    private byte isPassAll;
    private int isPassCell;
    private byte isPassMain;
    private int modelCell;
    private int modelMain;
    private int ref;
    private int updateAll;
    private int updatetCell;
    private int updatetMain;

    public int getIdentifyAll() {
        return this.identifyAll;
    }

    public void setIdentifyAll(int identifyAll) {
        this.identifyAll = identifyAll;
    }

    public byte getIsPassAll() {
        return this.isPassAll;
    }

    public void setIsPassAll(byte isPassAll) {
        this.isPassAll = isPassAll;
    }

    public int getUpdateAll() {
        return this.updateAll;
    }

    public void setUpdateAll(int updateAll) {
        this.updateAll = updateAll;
    }

    public int getIdentifyMain() {
        return this.identifyMain;
    }

    public void setIdentifyMain(int identifyMain) {
        this.identifyMain = identifyMain;
    }

    public byte getIsPassMain() {
        return this.isPassMain;
    }

    public void setIsPassMain(byte isPassMain) {
        this.isPassMain = isPassMain;
    }

    public int getUpdatetMain() {
        return this.updatetMain;
    }

    public void setUpdatetMain(int updatetMain) {
        this.updatetMain = updatetMain;
    }

    public int getModelMain() {
        return this.modelMain;
    }

    public void setModelMain(int modelMain) {
        this.modelMain = modelMain;
    }

    public int getIdentifyCell() {
        return this.identifyCell;
    }

    public void setIdentifyCell(int identifyCell) {
        this.identifyCell = identifyCell;
    }

    public int getIsPassCell() {
        return this.isPassCell;
    }

    public void setIsPassCell(int isPassCell) {
        this.isPassCell = isPassCell;
    }

    public int getUpdatetCell() {
        return this.updatetCell;
    }

    public void setUpdatetCell(int updatetCell) {
        this.updatetCell = updatetCell;
    }

    public int getModelCell() {
        return this.modelCell;
    }

    public void setModelCell(int modelCell) {
        this.modelCell = modelCell;
    }

    public int getRef() {
        return this.ref;
    }

    public void setRef(int ref) {
        this.ref = ref;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ModelInvalidChrInfo{loc=");
        stringBuilder.append(this.loc);
        stringBuilder.append(", identifyAll=");
        stringBuilder.append(this.identifyAll);
        stringBuilder.append(", isPassAll=");
        stringBuilder.append(this.isPassAll);
        stringBuilder.append(", updateAll=");
        stringBuilder.append(this.updateAll);
        stringBuilder.append(", modelAll=");
        stringBuilder.append(this.modelAll);
        stringBuilder.append(", identifyMain=");
        stringBuilder.append(this.identifyMain);
        stringBuilder.append(", isPassMain=");
        stringBuilder.append(this.isPassMain);
        stringBuilder.append(", updatetMain=");
        stringBuilder.append(this.updatetMain);
        stringBuilder.append(", modelMain=");
        stringBuilder.append(this.modelMain);
        stringBuilder.append(", identifyCell=");
        stringBuilder.append(this.identifyCell);
        stringBuilder.append(", isPassCell=");
        stringBuilder.append(this.isPassCell);
        stringBuilder.append(", updatetCell=");
        stringBuilder.append(this.updatetCell);
        stringBuilder.append(", modelCell=");
        stringBuilder.append(this.modelCell);
        stringBuilder.append(", label=");
        stringBuilder.append(this.label);
        stringBuilder.append(", ref=");
        stringBuilder.append(this.ref);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
