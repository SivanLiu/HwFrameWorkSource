package com.android.server.hidata.wavemapping.chr.entity;

import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;

public class ChrInfo {
    protected static final byte ALLAP = (byte) 0;
    protected static final byte HOME_LOC = (byte) 0;
    protected static final byte MAINAP = (byte) 1;
    protected static final byte OFFICE_LOC = (byte) 1;
    protected static final byte OTHER_LOC = (byte) 2;
    protected int label;
    protected byte loc;
    protected int modelAll;

    public int getLabel() {
        return this.label;
    }

    public void setLabel(boolean isMainAp) {
        this.label = isMainAp;
    }

    public byte getLoc() {
        return this.loc;
    }

    public void setLoc(String loc) {
        if (loc.equals(Constant.NAME_FREQLOCATION_HOME)) {
            this.loc = (byte) 0;
        } else if (loc.equals(Constant.NAME_FREQLOCATION_OFFICE)) {
            this.loc = (byte) 1;
        } else {
            this.loc = OTHER_LOC;
        }
    }

    public int getModelAll() {
        return this.modelAll;
    }

    public void setModelAll(String modelName) {
        if (modelName != null) {
            try {
                if (!modelName.equals("")) {
                    this.modelAll = Integer.parseInt(modelName);
                    return;
                }
            } catch (NumberFormatException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setModelAll,");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                return;
            }
        }
        this.modelAll = null;
    }
}
