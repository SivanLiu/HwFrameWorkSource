package android.view.textclassifier;

import com.android.internal.util.Preconditions;
import java.util.List;

public final class TextSelection {
    private final int mEndIndex;
    private final List<String> mEntities;
    private final EntityConfidence<String> mEntityConfidence;
    private final String mLogSource;
    private final int mStartIndex;
    private final String mVersionInfo;

    public static final class Builder {
        private final int mEndIndex;
        private final EntityConfidence<String> mEntityConfidence = new EntityConfidence();
        private String mLogSource = "";
        private final int mStartIndex;
        private String mVersionInfo = "";

        public Builder(int startIndex, int endIndex) {
            boolean z;
            boolean z2 = true;
            if (startIndex >= 0) {
                z = true;
            } else {
                z = false;
            }
            Preconditions.checkArgument(z);
            if (endIndex <= startIndex) {
                z2 = false;
            }
            Preconditions.checkArgument(z2);
            this.mStartIndex = startIndex;
            this.mEndIndex = endIndex;
        }

        public Builder setEntityType(String type, float confidenceScore) {
            this.mEntityConfidence.setEntityType(type, confidenceScore);
            return this;
        }

        Builder setLogSource(String logSource) {
            this.mLogSource = (String) Preconditions.checkNotNull(logSource);
            return this;
        }

        Builder setVersionInfo(String versionInfo) {
            this.mVersionInfo = (String) Preconditions.checkNotNull(versionInfo);
            return this;
        }

        public TextSelection build() {
            return new TextSelection(this.mStartIndex, this.mEndIndex, this.mEntityConfidence, this.mLogSource, this.mVersionInfo);
        }
    }

    private TextSelection(int startIndex, int endIndex, EntityConfidence<String> entityConfidence, String logSource, String versionInfo) {
        this.mStartIndex = startIndex;
        this.mEndIndex = endIndex;
        this.mEntityConfidence = new EntityConfidence(entityConfidence);
        this.mEntities = this.mEntityConfidence.getEntities();
        this.mLogSource = logSource;
        this.mVersionInfo = versionInfo;
    }

    public int getSelectionStartIndex() {
        return this.mStartIndex;
    }

    public int getSelectionEndIndex() {
        return this.mEndIndex;
    }

    public int getEntityCount() {
        return this.mEntities.size();
    }

    public String getEntity(int index) {
        return (String) this.mEntities.get(index);
    }

    public float getConfidenceScore(String entity) {
        return this.mEntityConfidence.getConfidenceScore(entity);
    }

    public String getSourceClassifier() {
        return this.mLogSource;
    }

    public String getVersionInfo() {
        return this.mVersionInfo;
    }

    public String toString() {
        return String.format("TextSelection {%d, %d, %s}", new Object[]{Integer.valueOf(this.mStartIndex), Integer.valueOf(this.mEndIndex), this.mEntityConfidence});
    }
}
