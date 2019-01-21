package android.view.inputmethod;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.SpannedString;
import android.text.TextUtils;
import android.view.inputmethod.SparseRectFArray.SparseRectFArrayBuilder;
import java.util.Arrays;
import java.util.Objects;

public final class CursorAnchorInfo implements Parcelable {
    public static final Creator<CursorAnchorInfo> CREATOR = new Creator<CursorAnchorInfo>() {
        public CursorAnchorInfo createFromParcel(Parcel source) {
            return new CursorAnchorInfo(source);
        }

        public CursorAnchorInfo[] newArray(int size) {
            return new CursorAnchorInfo[size];
        }
    };
    public static final int FLAG_HAS_INVISIBLE_REGION = 2;
    public static final int FLAG_HAS_VISIBLE_REGION = 1;
    public static final int FLAG_IS_RTL = 4;
    private final SparseRectFArray mCharacterBoundsArray;
    private final CharSequence mComposingText;
    private final int mComposingTextStart;
    private final int mHashCode;
    private final float mInsertionMarkerBaseline;
    private final float mInsertionMarkerBottom;
    private final int mInsertionMarkerFlags;
    private final float mInsertionMarkerHorizontal;
    private final float mInsertionMarkerTop;
    private final float[] mMatrixValues;
    private final int mSelectionEnd;
    private final int mSelectionStart;

    public static final class Builder {
        private SparseRectFArrayBuilder mCharacterBoundsArrayBuilder = null;
        private CharSequence mComposingText = null;
        private int mComposingTextStart = -1;
        private float mInsertionMarkerBaseline = Float.NaN;
        private float mInsertionMarkerBottom = Float.NaN;
        private int mInsertionMarkerFlags = 0;
        private float mInsertionMarkerHorizontal = Float.NaN;
        private float mInsertionMarkerTop = Float.NaN;
        private boolean mMatrixInitialized = false;
        private float[] mMatrixValues = null;
        private int mSelectionEnd = -1;
        private int mSelectionStart = -1;

        public Builder setSelectionRange(int newStart, int newEnd) {
            this.mSelectionStart = newStart;
            this.mSelectionEnd = newEnd;
            return this;
        }

        public Builder setComposingText(int composingTextStart, CharSequence composingText) {
            this.mComposingTextStart = composingTextStart;
            if (composingText == null) {
                this.mComposingText = null;
            } else {
                this.mComposingText = new SpannedString(composingText);
            }
            return this;
        }

        public Builder setInsertionMarkerLocation(float horizontalPosition, float lineTop, float lineBaseline, float lineBottom, int flags) {
            this.mInsertionMarkerHorizontal = horizontalPosition;
            this.mInsertionMarkerTop = lineTop;
            this.mInsertionMarkerBaseline = lineBaseline;
            this.mInsertionMarkerBottom = lineBottom;
            this.mInsertionMarkerFlags = flags;
            return this;
        }

        public Builder addCharacterBounds(int index, float left, float top, float right, float bottom, int flags) {
            if (index >= 0) {
                if (this.mCharacterBoundsArrayBuilder == null) {
                    this.mCharacterBoundsArrayBuilder = new SparseRectFArrayBuilder();
                }
                this.mCharacterBoundsArrayBuilder.append(index, left, top, right, bottom, flags);
                return this;
            }
            throw new IllegalArgumentException("index must not be a negative integer.");
        }

        public Builder setMatrix(Matrix matrix) {
            if (this.mMatrixValues == null) {
                this.mMatrixValues = new float[9];
            }
            (matrix != null ? matrix : Matrix.IDENTITY_MATRIX).getValues(this.mMatrixValues);
            this.mMatrixInitialized = true;
            return this;
        }

        public CursorAnchorInfo build() {
            if (!this.mMatrixInitialized) {
                boolean hasCharacterBounds = (this.mCharacterBoundsArrayBuilder == null || this.mCharacterBoundsArrayBuilder.isEmpty()) ? false : true;
                if (!(!hasCharacterBounds && Float.isNaN(this.mInsertionMarkerHorizontal) && Float.isNaN(this.mInsertionMarkerTop) && Float.isNaN(this.mInsertionMarkerBaseline) && Float.isNaN(this.mInsertionMarkerBottom))) {
                    throw new IllegalArgumentException("Coordinate transformation matrix is required when positional parameters are specified.");
                }
            }
            return new CursorAnchorInfo(this, null);
        }

        public void reset() {
            this.mSelectionStart = -1;
            this.mSelectionEnd = -1;
            this.mComposingTextStart = -1;
            this.mComposingText = null;
            this.mInsertionMarkerFlags = 0;
            this.mInsertionMarkerHorizontal = Float.NaN;
            this.mInsertionMarkerTop = Float.NaN;
            this.mInsertionMarkerBaseline = Float.NaN;
            this.mInsertionMarkerBottom = Float.NaN;
            this.mMatrixInitialized = false;
            if (this.mCharacterBoundsArrayBuilder != null) {
                this.mCharacterBoundsArrayBuilder.reset();
            }
        }
    }

    /* synthetic */ CursorAnchorInfo(Builder x0, AnonymousClass1 x1) {
        this(x0);
    }

    public CursorAnchorInfo(Parcel source) {
        this.mHashCode = source.readInt();
        this.mSelectionStart = source.readInt();
        this.mSelectionEnd = source.readInt();
        this.mComposingTextStart = source.readInt();
        this.mComposingText = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
        this.mInsertionMarkerFlags = source.readInt();
        this.mInsertionMarkerHorizontal = source.readFloat();
        this.mInsertionMarkerTop = source.readFloat();
        this.mInsertionMarkerBaseline = source.readFloat();
        this.mInsertionMarkerBottom = source.readFloat();
        this.mCharacterBoundsArray = (SparseRectFArray) source.readParcelable(SparseRectFArray.class.getClassLoader());
        this.mMatrixValues = source.createFloatArray();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mHashCode);
        dest.writeInt(this.mSelectionStart);
        dest.writeInt(this.mSelectionEnd);
        dest.writeInt(this.mComposingTextStart);
        TextUtils.writeToParcel(this.mComposingText, dest, flags);
        dest.writeInt(this.mInsertionMarkerFlags);
        dest.writeFloat(this.mInsertionMarkerHorizontal);
        dest.writeFloat(this.mInsertionMarkerTop);
        dest.writeFloat(this.mInsertionMarkerBaseline);
        dest.writeFloat(this.mInsertionMarkerBottom);
        dest.writeParcelable(this.mCharacterBoundsArray, flags);
        dest.writeFloatArray(this.mMatrixValues);
    }

    public int hashCode() {
        return this.mHashCode;
    }

    private static boolean areSameFloatImpl(float a, float b) {
        boolean z = true;
        if (Float.isNaN(a) && Float.isNaN(b)) {
            return true;
        }
        if (a != b) {
            z = false;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:44:0x0094, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:45:0x0095, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:46:0x0096, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CursorAnchorInfo)) {
            return false;
        }
        CursorAnchorInfo that = (CursorAnchorInfo) obj;
        if (hashCode() != that.hashCode() || this.mSelectionStart != that.mSelectionStart || this.mSelectionEnd != that.mSelectionEnd || this.mInsertionMarkerFlags != that.mInsertionMarkerFlags || !areSameFloatImpl(this.mInsertionMarkerHorizontal, that.mInsertionMarkerHorizontal) || !areSameFloatImpl(this.mInsertionMarkerTop, that.mInsertionMarkerTop) || !areSameFloatImpl(this.mInsertionMarkerBaseline, that.mInsertionMarkerBaseline) || !areSameFloatImpl(this.mInsertionMarkerBottom, that.mInsertionMarkerBottom) || !Objects.equals(this.mCharacterBoundsArray, that.mCharacterBoundsArray) || this.mComposingTextStart != that.mComposingTextStart || !Objects.equals(this.mComposingText, that.mComposingText) || this.mMatrixValues.length != that.mMatrixValues.length) {
            return false;
        }
        for (int i = 0; i < this.mMatrixValues.length; i++) {
            if (this.mMatrixValues[i] != that.mMatrixValues[i]) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CursorAnchorInfo{mHashCode=");
        stringBuilder.append(this.mHashCode);
        stringBuilder.append(" mSelection=");
        stringBuilder.append(this.mSelectionStart);
        stringBuilder.append(",");
        stringBuilder.append(this.mSelectionEnd);
        stringBuilder.append(" mComposingTextStart=");
        stringBuilder.append(this.mComposingTextStart);
        stringBuilder.append(" mComposingText=");
        stringBuilder.append(Objects.toString(this.mComposingText));
        stringBuilder.append(" mInsertionMarkerFlags=");
        stringBuilder.append(this.mInsertionMarkerFlags);
        stringBuilder.append(" mInsertionMarkerHorizontal=");
        stringBuilder.append(this.mInsertionMarkerHorizontal);
        stringBuilder.append(" mInsertionMarkerTop=");
        stringBuilder.append(this.mInsertionMarkerTop);
        stringBuilder.append(" mInsertionMarkerBaseline=");
        stringBuilder.append(this.mInsertionMarkerBaseline);
        stringBuilder.append(" mInsertionMarkerBottom=");
        stringBuilder.append(this.mInsertionMarkerBottom);
        stringBuilder.append(" mCharacterBoundsArray=");
        stringBuilder.append(Objects.toString(this.mCharacterBoundsArray));
        stringBuilder.append(" mMatrix=");
        stringBuilder.append(Arrays.toString(this.mMatrixValues));
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private CursorAnchorInfo(Builder builder) {
        this.mSelectionStart = builder.mSelectionStart;
        this.mSelectionEnd = builder.mSelectionEnd;
        this.mComposingTextStart = builder.mComposingTextStart;
        this.mComposingText = builder.mComposingText;
        this.mInsertionMarkerFlags = builder.mInsertionMarkerFlags;
        this.mInsertionMarkerHorizontal = builder.mInsertionMarkerHorizontal;
        this.mInsertionMarkerTop = builder.mInsertionMarkerTop;
        this.mInsertionMarkerBaseline = builder.mInsertionMarkerBaseline;
        this.mInsertionMarkerBottom = builder.mInsertionMarkerBottom;
        this.mCharacterBoundsArray = builder.mCharacterBoundsArrayBuilder != null ? builder.mCharacterBoundsArrayBuilder.build() : null;
        this.mMatrixValues = new float[9];
        if (builder.mMatrixInitialized) {
            System.arraycopy(builder.mMatrixValues, 0, this.mMatrixValues, 0, 9);
        } else {
            Matrix.IDENTITY_MATRIX.getValues(this.mMatrixValues);
        }
        this.mHashCode = (Objects.hashCode(this.mComposingText) * 31) + Arrays.hashCode(this.mMatrixValues);
    }

    public int getSelectionStart() {
        return this.mSelectionStart;
    }

    public int getSelectionEnd() {
        return this.mSelectionEnd;
    }

    public int getComposingTextStart() {
        return this.mComposingTextStart;
    }

    public CharSequence getComposingText() {
        return this.mComposingText;
    }

    public int getInsertionMarkerFlags() {
        return this.mInsertionMarkerFlags;
    }

    public float getInsertionMarkerHorizontal() {
        return this.mInsertionMarkerHorizontal;
    }

    public float getInsertionMarkerTop() {
        return this.mInsertionMarkerTop;
    }

    public float getInsertionMarkerBaseline() {
        return this.mInsertionMarkerBaseline;
    }

    public float getInsertionMarkerBottom() {
        return this.mInsertionMarkerBottom;
    }

    public RectF getCharacterBounds(int index) {
        if (this.mCharacterBoundsArray == null) {
            return null;
        }
        return this.mCharacterBoundsArray.get(index);
    }

    public int getCharacterBoundsFlags(int index) {
        if (this.mCharacterBoundsArray == null) {
            return 0;
        }
        return this.mCharacterBoundsArray.getFlags(index, 0);
    }

    public Matrix getMatrix() {
        Matrix matrix = new Matrix();
        matrix.setValues(this.mMatrixValues);
        return matrix;
    }

    public int describeContents() {
        return 0;
    }
}
