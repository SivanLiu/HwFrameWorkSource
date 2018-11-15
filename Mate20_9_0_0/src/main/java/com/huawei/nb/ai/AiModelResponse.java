package com.huawei.nb.ai;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
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
        public AiModelResponse createFromParcel(Parcel in) {
            return new AiModelResponse(in);
        }

        public AiModelResponse[] newArray(int size) {
            return new AiModelResponse[size];
        }
    };
    private List<AiModel> aiModelList;
    private Map<Long, List<Long>> aiModelWeightMap;
    private Map<Long, List<Long>> aiModelWeightMeanMap;
    private String modelFileKey;
    private boolean needDecrypt;
    private String privateKey;

    public AiModelResponse() {
        this.modelFileKey = null;
        this.privateKey = null;
        this.needDecrypt = false;
        this.aiModelList = new ArrayList();
        this.aiModelWeightMap = new HashMap();
        this.aiModelWeightMeanMap = new HashMap();
    }

    void addAiModel(AiModel model) {
        if (this.aiModelList != null && model != null) {
            this.aiModelList.add(model);
        }
    }

    AiModelResponse setNeedDecrypt() {
        this.needDecrypt = true;
        return this;
    }

    AiModelResponse setModelFileKey(String modelFileKey) {
        this.modelFileKey = modelFileKey;
        return this;
    }

    public List<AiModel> getAiModelList() {
        return this.aiModelList;
    }

    public void setPrivateKey(String key) {
        this.privateKey = key;
    }

    private boolean isModelEncrypted(AiModel model) {
        return model.getIs_encrypt() != null && model.getIs_encrypt().intValue() == 1;
    }

    private AiModel decryptMetaData(AiModel model, String privateKey) {
        AiModel newModel = AiModelAttributes.copyAiModel(model);
        List<String> attrsToDecrypt = AiModelAttributes.getEncryptedAttributes();
        Map<String, Supplier<Object>> getAttributes = AiModelAttributes.getAttributes(model);
        Map<String, Consumer<String>> setAttributes = AiModelAttributes.setAttributes(newModel);
        for (String attrKey : attrsToDecrypt) {
            String encryptedStr = (String) ((Supplier) getAttributes.get(attrKey)).get();
            String attrStr = RSAEncryptUtils.decryptString(encryptedStr, privateKey);
            if (encryptedStr == null || attrStr != null) {
                ((Consumer) setAttributes.get(attrKey)).accept(attrStr);
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
        List<AiModel> arrayList = new ArrayList();
        for (Long id : ids) {
            arrayList.add(findModelById(id, models));
        }
        return arrayList;
    }

    private Map<Long, List<Long>> getAiModelMap(int model_type) {
        initModelMapping();
        if (model_type == 3) {
            return this.aiModelWeightMap;
        }
        if (model_type == 4) {
            return this.aiModelWeightMeanMap;
        }
        return null;
    }

    private List<AiModel> getRelatedModels(Long modelId, int model_type) {
        Map<Long, List<Long>> map = getAiModelMap(model_type);
        List<AiModel> resultAiModels = null;
        if (!(map == null || this.aiModelList == null || modelId == null)) {
            List<Long> modelIds = (List) map.get(modelId);
            if (modelIds == null) {
                return null;
            }
            resultAiModels = findModelsByIds(modelIds, this.aiModelList);
        }
        return resultAiModels;
    }

    private void putKVRelation(Long kId, Long vId, Map<Long, List<Long>> map) {
        List<Long> valIds = (List) map.get(kId);
        if (valIds == null) {
            valIds = new ArrayList();
        }
        valIds.add(vId);
        map.put(kId, valIds);
    }

    private void initModelMapping() {
        if (this.aiModelList == null || this.aiModelList.size() <= 0) {
            DSLog.w("No AI Models found response.", new Object[0]);
            return;
        }
        if (this.aiModelWeightMap == null) {
            this.aiModelWeightMap = new HashMap();
        }
        if (this.aiModelWeightMeanMap == null) {
            this.aiModelWeightMeanMap = new HashMap();
        }
        if (this.aiModelWeightMap.size() == 0 && this.aiModelWeightMeanMap.size() == 0) {
            for (AiModel model : this.aiModelList) {
                if (model.getModel_type() != null) {
                    switch (model.getModel_type().intValue()) {
                        case 3:
                            putKVRelation(model.getParent_id(), model.getId(), this.aiModelWeightMap);
                            break;
                        case 4:
                            putKVRelation(model.getParent_id(), model.getId(), this.aiModelWeightMeanMap);
                            break;
                        default:
                            break;
                    }
                }
                DSLog.w("The AI Model type is null, Please confirm that you have the authority to access this data.", new Object[0]);
            }
            this.aiModelWeightMap = Collections.unmodifiableMap(this.aiModelWeightMap);
            this.aiModelWeightMeanMap = Collections.unmodifiableMap(this.aiModelWeightMeanMap);
        }
    }

    public AiModelByteBuffer loadAiModel(AiModel model) {
        if (model.getIs_none().intValue() != 0) {
            DSLog.e("Failed to load ai model, error: model is none.", new Object[0]);
            return null;
        }
        if (this.needDecrypt) {
            model = decryptMetaData(model, this.privateKey);
        }
        byte[] keyByte = null;
        if (isModelEncrypted(model)) {
            keyByte = RSAEncryptUtils.decryptStringToBytes(this.modelFileKey, this.privateKey);
        }
        ByteBuffer byteBuffer = AiModelReader.readAiModel(model.getFile_path(), keyByte);
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
        List<AiModelByteBuffer> modelByteBufferList = new ArrayList(modelList.size());
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

    public AiModelResponse(Parcel in) {
        boolean z = true;
        this.modelFileKey = null;
        this.privateKey = null;
        this.needDecrypt = false;
        if (in.readByte() == (byte) 0) {
            this.aiModelList = null;
        } else {
            this.aiModelList = in.readArrayList(AiModel.class.getClassLoader());
            this.aiModelList = Collections.unmodifiableList(this.aiModelList);
        }
        if (in.readByte() == (byte) 0) {
            this.aiModelWeightMap = null;
        } else {
            this.aiModelWeightMap = in.readHashMap(ArrayList.class.getClassLoader());
        }
        if (in.readByte() == (byte) 0) {
            this.aiModelWeightMeanMap = null;
        } else {
            this.aiModelWeightMeanMap = in.readHashMap(ArrayList.class.getClassLoader());
        }
        if (in.readByte() == (byte) 0) {
            this.modelFileKey = null;
        } else {
            this.modelFileKey = in.readString();
        }
        if (in.readByte() == (byte) 0) {
            this.privateKey = null;
        } else {
            this.privateKey = in.readString();
        }
        if (in.readByte() != (byte) 1) {
            z = false;
        }
        this.needDecrypt = z;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int i) {
        if (this.aiModelList != null) {
            out.writeByte((byte) 1);
            out.writeList(this.aiModelList);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aiModelWeightMap != null) {
            out.writeByte((byte) 1);
            out.writeMap(this.aiModelWeightMap);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.aiModelWeightMeanMap != null) {
            out.writeByte((byte) 1);
            out.writeMap(this.aiModelWeightMeanMap);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.modelFileKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.modelFileKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.privateKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.privateKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.needDecrypt) {
            out.writeByte((byte) 1);
        } else {
            out.writeByte((byte) 0);
        }
    }
}
