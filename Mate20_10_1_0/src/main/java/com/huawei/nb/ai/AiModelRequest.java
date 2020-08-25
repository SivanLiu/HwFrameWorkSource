package com.huawei.nb.ai;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.nb.model.aimodel.AiModel;

public class AiModelRequest implements Parcelable {
    public static final Creator<AiModelRequest> CREATOR = new Creator<AiModelRequest>() {
        /* class com.huawei.nb.ai.AiModelRequest.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AiModelRequest createFromParcel(Parcel in) {
            return new AiModelRequest(in);
        }

        @Override // android.os.Parcelable.Creator
        public AiModelRequest[] newArray(int size) {
            return new AiModelRequest[size];
        }
    };
    private AiModel mAiModel = null;
    private boolean mIsNeedEncrypt = false;
    private boolean mIsNeedLatestVersion = false;
    private boolean mIsNeedMeanModel = false;
    private boolean mIsNeedWeightModel = false;
    private String mPublicKey = null;

    public AiModelRequest() {
    }

    protected AiModelRequest(Parcel in) {
        boolean z;
        boolean z2;
        boolean z3 = true;
        String str = null;
        if (in.readByte() == 0) {
            this.mAiModel = null;
        } else {
            this.mAiModel = new AiModel(in);
        }
        this.mPublicKey = in.readByte() != 0 ? in.readString() : str;
        if (in.readByte() != 0) {
            z = true;
        } else {
            z = false;
        }
        this.mIsNeedEncrypt = z;
        if (in.readByte() != 0) {
            z2 = true;
        } else {
            z2 = false;
        }
        this.mIsNeedWeightModel = z2;
        this.mIsNeedMeanModel = in.readByte() == 0 ? false : z3;
    }

    public AiModel getAiModel() {
        return this.mAiModel;
    }

    public String getPublicKey() {
        return this.mPublicKey;
    }

    public boolean isNeedEncrypt() {
        return this.mIsNeedEncrypt;
    }

    public boolean isNeedWeightModel() {
        return this.mIsNeedWeightModel;
    }

    public boolean isNeedMeanModel() {
        return this.mIsNeedMeanModel;
    }

    public boolean isNeedLatestVersion() {
        return this.mIsNeedLatestVersion;
    }

    public AiModelRequest setNeedWeightModel() {
        this.mIsNeedWeightModel = true;
        return this;
    }

    public AiModelRequest setNeedMeanModel() {
        this.mIsNeedMeanModel = true;
        return this;
    }

    public AiModelRequest setNeedEncrypt() {
        this.mIsNeedEncrypt = true;
        return this;
    }

    public AiModelRequest setNeedLatestVersion() {
        this.mIsNeedLatestVersion = true;
        return this;
    }

    public AiModelRequest setAiModel(AiModel aiModel) {
        this.mAiModel = aiModel;
        return this;
    }

    public AiModelRequest setPublicKey(String key) {
        this.mPublicKey = key;
        return this;
    }

    public boolean isValid() {
        return this.mAiModel != null && this.mAiModel.getIs_none().intValue() == 0;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int i) {
        if (this.mAiModel != null) {
            out.writeByte((byte) 1);
            this.mAiModel.writeToParcel(out, i);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mPublicKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mPublicKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mIsNeedEncrypt) {
            out.writeByte((byte) 1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mIsNeedWeightModel) {
            out.writeByte((byte) 1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mIsNeedMeanModel) {
            out.writeByte((byte) 1);
        } else {
            out.writeByte((byte) 0);
        }
    }
}
