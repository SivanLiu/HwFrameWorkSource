package android.drm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class DrmRights {
    private String mAccountId;
    private byte[] mData;
    private String mMimeType;
    private String mSubscriptionId;

    public DrmRights(String rightsFilePath, String mimeType) {
        instantiate(new File(rightsFilePath), mimeType);
    }

    public DrmRights(String rightsFilePath, String mimeType, String accountId) {
        this(rightsFilePath, mimeType);
        this.mAccountId = accountId;
    }

    public DrmRights(String rightsFilePath, String mimeType, String accountId, String subscriptionId) {
        this(rightsFilePath, mimeType);
        this.mAccountId = accountId;
        this.mSubscriptionId = subscriptionId;
    }

    public DrmRights(File rightsFile, String mimeType) {
        instantiate(rightsFile, mimeType);
    }

    private void instantiate(File rightsFile, String mimeType) {
        try {
            this.mData = DrmUtils.readBytes(rightsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mMimeType = mimeType;
        if (!isValid()) {
            String msg = new StringBuilder();
            msg.append("mimeType: ");
            msg.append(this.mMimeType);
            msg.append(",data: ");
            msg.append(Arrays.toString(this.mData));
            throw new IllegalArgumentException(msg.toString());
        }
    }

    public DrmRights(ProcessedData data, String mimeType) {
        if (data != null) {
            this.mData = data.getData();
            this.mAccountId = data.getAccountId();
            this.mSubscriptionId = data.getSubscriptionId();
            this.mMimeType = mimeType;
            if (!isValid()) {
                String msg = new StringBuilder();
                msg.append("mimeType: ");
                msg.append(this.mMimeType);
                msg.append(",data: ");
                msg.append(Arrays.toString(this.mData));
                throw new IllegalArgumentException(msg.toString());
            }
            return;
        }
        throw new IllegalArgumentException("data is null");
    }

    public byte[] getData() {
        return this.mData;
    }

    public String getMimeType() {
        return this.mMimeType;
    }

    public String getAccountId() {
        return this.mAccountId;
    }

    public String getSubscriptionId() {
        return this.mSubscriptionId;
    }

    boolean isValid() {
        return (this.mMimeType == null || this.mMimeType.equals("") || this.mData == null || this.mData.length <= 0) ? false : true;
    }
}
