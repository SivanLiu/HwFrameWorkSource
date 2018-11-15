package com.android.server.hidata.wavemapping.chr.entity;

public class CollectFingerChrInfo {
    private int batchAll;
    private int batchCell;
    private int batchMain;
    private int fingerActiveAll;
    private int fingersCell;
    private int fingersMain;
    private int fingersPassiveAll;
    private int updateAll;
    private int updateCell;
    private int updateMain;

    public int getBatchAll() {
        return this.batchAll;
    }

    public void setBatchAll(int batchAll) {
        this.batchAll = batchAll;
    }

    public int getFingersPassiveAll() {
        return this.fingersPassiveAll;
    }

    public void setFingersPassiveAll(int fingersPassiveAll) {
        this.fingersPassiveAll = fingersPassiveAll;
    }

    public int getFingerActiveAll() {
        return this.fingerActiveAll;
    }

    public void setFingerActiveAll(int fingerActiveAll) {
        this.fingerActiveAll = fingerActiveAll;
    }

    public int getUpdateAll() {
        return this.updateAll;
    }

    public void setUpdateAll(int updateAll) {
        this.updateAll = updateAll;
    }

    public int getBatchMain() {
        return this.batchMain;
    }

    public void setBatchMain(int batchMain) {
        this.batchMain = batchMain;
    }

    public int getFingersMain() {
        return this.fingersMain;
    }

    public void setFingersMain(int fingersMain) {
        this.fingersMain = fingersMain;
    }

    public int getUpdateMain() {
        return this.updateMain;
    }

    public void setUpdateMain(int updateMain) {
        this.updateMain = updateMain;
    }

    public int getBatchCell() {
        return this.batchCell;
    }

    public void setBatchCell(int batchCell) {
        this.batchCell = batchCell;
    }

    public int getFingersCell() {
        return this.fingersCell;
    }

    public void setFingersCell(int fingersCell) {
        this.fingersCell = fingersCell;
    }

    public int getUpdateCell() {
        return this.updateCell;
    }

    public void setUpdateCell(int updateCell) {
        this.updateCell = updateCell;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CollectFingerChrInfo{batchAll=");
        stringBuilder.append(this.batchAll);
        stringBuilder.append(", fingersPassiveAll=");
        stringBuilder.append(this.fingersPassiveAll);
        stringBuilder.append(", fingerActiveAll=");
        stringBuilder.append(this.fingerActiveAll);
        stringBuilder.append(", updateAll=");
        stringBuilder.append(this.updateAll);
        stringBuilder.append(", batchMain=");
        stringBuilder.append(this.batchMain);
        stringBuilder.append(", fingersMain=");
        stringBuilder.append(this.fingersMain);
        stringBuilder.append(", updateMain=");
        stringBuilder.append(this.updateMain);
        stringBuilder.append(", batchCell=");
        stringBuilder.append(this.batchCell);
        stringBuilder.append(", fingersCell=");
        stringBuilder.append(this.fingersCell);
        stringBuilder.append(", updateCell=");
        stringBuilder.append(this.updateCell);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
