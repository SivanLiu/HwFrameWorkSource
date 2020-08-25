package com.huawei.nb.ai;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.nb.model.aimodel.AiModel;
import com.huawei.nb.security.RSAEncryptUtils;
import com.huawei.nb.utils.logger.DSLog;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AiModelResponse implements Parcelable {
    public static final Creator<AiModelResponse> CREATOR = new Creator<AiModelResponse>() {
        /* class com.huawei.nb.ai.AiModelResponse.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AiModelResponse createFromParcel(Parcel in) {
            return new AiModelResponse(in);
        }

        @Override // android.os.Parcelable.Creator
        public AiModelResponse[] newArray(int size) {
            return new AiModelResponse[size];
        }
    };
    private List<AiModel> mAiModelList;
    private Map<Long, List<Long>> mAiModelWeightMap;
    private Map<Long, List<Long>> mAiModelWeightMeanMap;
    private boolean mIsNeedDecrypt;
    private String mModelFileKey;
    private String mPrivateKey;

    public AiModelResponse() {
        this.mModelFileKey = null;
        this.mPrivateKey = null;
        this.mIsNeedDecrypt = false;
        this.mAiModelList = new ArrayList();
        this.mAiModelWeightMap = new HashMap();
        this.mAiModelWeightMeanMap = new HashMap();
    }

    public AiModelResponse(Parcel in) {
        boolean z = true;
        this.mModelFileKey = null;
        this.mPrivateKey = null;
        this.mIsNeedDecrypt = false;
        if (in.readByte() == 0) {
            this.mAiModelList = null;
        } else {
            this.mAiModelList = in.readArrayList(AiModel.class.getClassLoader());
            this.mAiModelList = Collections.unmodifiableList(this.mAiModelList);
        }
        if (in.readByte() == 0) {
            this.mAiModelWeightMap = null;
        } else {
            this.mAiModelWeightMap = in.readHashMap(ArrayList.class.getClassLoader());
        }
        if (in.readByte() == 0) {
            this.mAiModelWeightMeanMap = null;
        } else {
            this.mAiModelWeightMeanMap = in.readHashMap(ArrayList.class.getClassLoader());
        }
        if (in.readByte() == 0) {
            this.mModelFileKey = null;
        } else {
            this.mModelFileKey = in.readString();
        }
        if (in.readByte() == 0) {
            this.mPrivateKey = null;
        } else {
            this.mPrivateKey = in.readString();
        }
        this.mIsNeedDecrypt = in.readByte() != 1 ? false : z;
    }

    /* access modifiers changed from: package-private */
    public void addAiModel(AiModel model) {
        if (this.mAiModelList != null && model != null) {
            this.mAiModelList.add(model);
        }
    }

    /* access modifiers changed from: package-private */
    public AiModelResponse setNeedDecrypt() {
        this.mIsNeedDecrypt = true;
        return this;
    }

    /* access modifiers changed from: package-private */
    public AiModelResponse setModelFileKey(String modelFileKey) {
        this.mModelFileKey = modelFileKey;
        return this;
    }

    public List<AiModel> getAiModelList() {
        return this.mAiModelList;
    }

    public void setPrivateKey(String key) {
        this.mPrivateKey = key;
    }

    private boolean isModelEncrypted(AiModel model) {
        return model.getIs_encrypt() != null && model.getIs_encrypt().intValue() == 1;
    }

    private AiModel decryptMetaData(AiModel model, String privateKey) {
        AiModel newModel = AiModelAttributes.copyAiModel(model);
        List<String> toDecryptAttrsList = AiModelAttributes.getEncryptedAttributes();
        Map<String, Supplier<Object>> getAttributes = AiModelAttributes.getAttributes(model);
        Map<String, Consumer<String>> setAttributes = AiModelAttributes.setAttributes(newModel);
        for (String attrKey : toDecryptAttrsList) {
            String encryptedStr = (String) getAttributes.get(attrKey).get();
            String attrStr = RSAEncryptUtils.decryptString(encryptedStr, privateKey);
            if (encryptedStr == null || attrStr != null) {
                setAttributes.get(attrKey).accept(attrStr);
            } else {
                DSLog.w("Failed to decrypt meta data for AI Model [" + (model.getId() != null ? model.getId() : "Unknown ID") + "].", new Object[0]);
            }
        }
        return newModel;
    }

    private AiModel findModelById(Long id, List<AiModel> models) {
        for (AiModel model : models) {
            if (model.getId().equals(id)) {
                return model;
            }
        }
        return null;
    }

    private List<AiModel> findModelsByIds(List<Long> ids, List<AiModel> models) {
        List<AiModel> arrayList = new ArrayList<>();
        for (Long id : ids) {
            arrayList.add(findModelById(id, models));
        }
        return arrayList;
    }

    private Map<Long, List<Long>> getAiModelMap(int modelType) {
        initModelMapping();
        if (modelType == 3) {
            return this.mAiModelWeightMap;
        }
        if (modelType == 4) {
            return this.mAiModelWeightMeanMap;
        }
        return null;
    }

    private List<AiModel> getRelatedModels(Long modelId, int modelType) {
        Map<Long, List<Long>> map = getAiModelMap(modelType);
        List<AiModel> resultAiModels = null;
        if (!(map == null || this.mAiModelList == null || modelId == null)) {
            List<Long> modelIds = map.get(modelId);
            if (modelIds == null) {
                return null;
            }
            resultAiModels = findModelsByIds(modelIds, this.mAiModelList);
        }
        return resultAiModels;
    }

    private void putKVRelation(Long kId, Long vId, Map<Long, List<Long>> map) {
        List<Long> valIds = map.get(kId);
        if (valIds == null) {
            valIds = new ArrayList<>(0);
        }
        valIds.add(vId);
        map.put(kId, valIds);
    }

    private void initModelMapping() {
        if (this.mAiModelList == null || this.mAiModelList.size() <= 0) {
            DSLog.w("No AI Models found response.", new Object[0]);
            return;
        }
        if (this.mAiModelWeightMap == null) {
            this.mAiModelWeightMap = new HashMap(0);
        }
        if (this.mAiModelWeightMeanMap == null) {
            this.mAiModelWeightMeanMap = new HashMap(0);
        }
        if (this.mAiModelWeightMap.size() == 0 && this.mAiModelWeightMeanMap.size() == 0) {
            for (AiModel model : this.mAiModelList) {
                if (model.getModel_type() == null) {
                    DSLog.w("The AI Model type is null, Please confirm that you have the authority to access this data.", new Object[0]);
                } else {
                    switch (model.getModel_type().intValue()) {
                        case 3:
                            putKVRelation(model.getParent_id(), model.getId(), this.mAiModelWeightMap);
                            continue;
                        case 4:
                            putKVRelation(model.getParent_id(), model.getId(), this.mAiModelWeightMeanMap);
                            continue;
                    }
                }
            }
            this.mAiModelWeightMap = Collections.unmodifiableMap(this.mAiModelWeightMap);
            this.mAiModelWeightMeanMap = Collections.unmodifiableMap(this.mAiModelWeightMeanMap);
        }
    }

    public AiModelByteBuffer loadAiModel(AiModel model) {
        if (model.getIs_none().intValue() != 0) {
            DSLog.e("Failed to load ai model, error: model is none.", new Object[0]);
            return null;
        }
        AiModel aiModel = model;
        if (this.mIsNeedDecrypt) {
            aiModel = decryptMetaData(model, this.mPrivateKey);
        }
        byte[] keyBytes = null;
        if (isModelEncrypted(aiModel)) {
            keyBytes = RSAEncryptUtils.decryptStringToBytes(this.mModelFileKey, this.mPrivateKey);
        }
        ByteBuffer byteBuffer = AiModelReader.readAiModel(aiModel.getFile_path(), keyBytes);
        if (byteBuffer == null) {
            return null;
        }
        AiModelByteBuffer aiModelByteBuffer = new AiModelByteBuffer(this);
        aiModelByteBuffer.setByteBuffer(byteBuffer);
        return aiModelByteBuffer;
    }

    public List<AiModelByteBuffer> loadAiModel(List<AiModel> modelList) {
        if (modelList == null) {
            return null;
        }
        List<AiModelByteBuffer> modelByteBufferList = new ArrayList<>(modelList.size());
        for (AiModel model : modelList) {
            modelByteBufferList.add(loadAiModel(model));
        }
        return modelByteBufferList;
    }

    public List<AiModel> getRelatedWeightModels(Long modelId) {
        return getRelatedModels(modelId, 3);
    }

    public List<AiModel> getRelatedWeightModels(AiModel model) {
        if (model != null) {
            return getRelatedWeightModels(model.getId());
        }
        return null;
    }

    public List<AiModel> getRelatedMeanModels(Long weightModelId) {
        return getRelatedModels(weightModelId, 4);
    }

    public List<AiModel> getRelatedMeanModels(AiModel weightModel) {
        if (weightModel != null) {
            return getRelatedMeanModels(weightModel.getId());
        }
        return null;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int i) {
        if (this.mAiModelList != null) {
            out.writeByte((byte) 1);
            out.writeList(this.mAiModelList);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mAiModelWeightMap != null) {
            out.writeByte((byte) 1);
            out.writeMap(this.mAiModelWeightMap);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mAiModelWeightMeanMap != null) {
            out.writeByte((byte) 1);
            out.writeMap(this.mAiModelWeightMeanMap);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mModelFileKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mModelFileKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mPrivateKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mPrivateKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mIsNeedDecrypt) {
            out.writeByte((byte) 1);
        } else {
            out.writeByte((byte) 0);
        }
    }
}
