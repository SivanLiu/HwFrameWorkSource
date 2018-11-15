package com.huawei.nb.ai;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import com.huawei.nb.model.aimodel.AiModel;

public class AiModelRequest implements Parcelable {
    public static final Creator<AiModelRequest> CREATOR = new Creator<AiModelRequest>() {
        public AiModelRequest createFromParcel(Parcel in) {
            return new AiModelRequest(in);
        }

        public AiModelRequest[] newArray(int size) {
            return new AiModelRequest[size];
        }
    };
    private AiModel aiModel = null;
    private boolean needEncrypt = false;
    private boolean needLatestVersion = false;
    private boolean needMeanModel = false;
    private boolean needWeightModel = false;
    private String publicKey = null;

    public AiModel getAiModel() {
        return this.aiModel;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public boolean isNeedEncrypt() {
        return this.needEncrypt;
    }

    public boolean isNeedWeightModel() {
        return this.needWeightModel;
    }

    public boolean isNeedMeanModel() {
        return this.needMeanModel;
    }

    public boolean isNeedLatestVersion() {
        return this.needLatestVersion;
    }

    public AiModelRequest setNeedWeightModel() {
        this.needWeightModel = true;
        return this;
    }

    public AiModelRequest setNeedMeanModel() {
        this.needMeanModel = true;
        return this;
    }

    public AiModelRequest setNeedEncrypt() {
        this.needEncrypt = true;
        return this;
    }

    public AiModelRequest setNeedLatestVersion() {
        this.needLatestVersion = true;
        return this;
    }

    public AiModelRequest setAiModel(AiModel aiModel) {
        this.aiModel = aiModel;
        return this;
    }

    public AiModelRequest setPublicKey(String key) {
        this.publicKey = key;
        return this;
    }

    public boolean isValid() {
        return this.aiModel != null && this.aiModel.getIs_none().intValue() == 0;
    }

    protected AiModelRequest(Parcel in) {
        boolean z;
        boolean z2 = true;
        String str = null;
        if (in.readByte() == (byte) 0) {
            this.aiModel = null;
        } else {
            this.aiModel = new AiModel(in);
        }
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.publicKey = str;
        if (in.readByte() != (byte) 0) {
            z = true;
        } else {
            z = false;
        }
        this.needEncrypt = z;
        if (in.readByte() != (byte) 0) {
            z = true;
        } else {
            z = false;
        }
        this.needWeightModel = z;
        if (in.readByte() == (byte) 0) {
            z2 = false;
        }
        this.needMeanModel = z2;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int i) {
        if (this.aiModel != null) {
            out.writeByte((byte) 1);
            this.aiModel.writeToParcel(out, i);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.publicKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.publicKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.needEncrypt) {
            out.writeByte((byte) 1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.needWeightModel) {
            out.writeByte((byte) 1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.needMeanModel) {
            out.writeByte((byte) 1);
        } else {
            out.writeByte((byte) 0);
        }
    }
}
