package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;
import com.android.internal.util.Preconditions;
import java.util.Arrays;

public final class ReprocessFormatsMap {
    private final int[] mEntry;
    private final int mInputCount;

    public ReprocessFormatsMap(int[] entry) {
        Preconditions.checkNotNull(entry, "entry must not be null");
        int left = entry.length;
        int numInputs = 0;
        int i = 0;
        while (i < entry.length) {
            int inputFormat = StreamConfigurationMap.checkArgumentFormatInternal(entry[i]);
            left--;
            i++;
            if (left >= 1) {
                int length = entry[i];
                left--;
                i++;
                for (int j = 0; j < length; j++) {
                    StreamConfigurationMap.checkArgumentFormatInternal(entry[i + j]);
                }
                if (length > 0) {
                    if (left >= length) {
                        i += length;
                        left -= length;
                    } else {
                        throw new IllegalArgumentException(String.format("Input %x had too few output formats listed (actual: %d, expected: %d)", new Object[]{Integer.valueOf(inputFormat), Integer.valueOf(left), Integer.valueOf(length)}));
                    }
                }
                numInputs++;
            } else {
                throw new IllegalArgumentException(String.format("Input %x had no output format length listed", new Object[]{Integer.valueOf(inputFormat)}));
            }
        }
        this.mEntry = entry;
        this.mInputCount = numInputs;
    }

    public int[] getInputs() {
        int[] inputs = new int[this.mInputCount];
        int i = 0;
        int left = this.mEntry.length;
        int j = 0;
        while (i < this.mEntry.length) {
            int format = this.mEntry[i];
            left--;
            i++;
            if (left >= 1) {
                int length = this.mEntry[i];
                left--;
                i++;
                if (length > 0) {
                    if (left >= length) {
                        i += length;
                        left -= length;
                    } else {
                        throw new AssertionError(String.format("Input %x had too few output formats listed (actual: %d, expected: %d)", new Object[]{Integer.valueOf(format), Integer.valueOf(left), Integer.valueOf(length)}));
                    }
                }
                inputs[j] = format;
                j++;
            } else {
                throw new AssertionError(String.format("Input %x had no output format length listed", new Object[]{Integer.valueOf(format)}));
            }
        }
        return StreamConfigurationMap.imageFormatToPublic(inputs);
    }

    public int[] getOutputs(int format) {
        int k = 0;
        int left = this.mEntry.length;
        int i = 0;
        while (i < this.mEntry.length) {
            int inputFormat = this.mEntry[i];
            left--;
            i++;
            if (left >= 1) {
                int length = this.mEntry[i];
                left--;
                i++;
                if (length > 0 && left < length) {
                    throw new AssertionError(String.format("Input %x had too few output formats listed (actual: %d, expected: %d)", new Object[]{Integer.valueOf(format), Integer.valueOf(left), Integer.valueOf(length)}));
                } else if (inputFormat == format) {
                    int[] outputs = new int[length];
                    while (k < length) {
                        outputs[k] = this.mEntry[i + k];
                        k++;
                    }
                    return StreamConfigurationMap.imageFormatToPublic(outputs);
                } else {
                    i += length;
                    left -= length;
                }
            } else {
                throw new AssertionError(String.format("Input %x had no output format length listed", new Object[]{Integer.valueOf(format)}));
            }
        }
        throw new IllegalArgumentException(String.format("Input format %x was not one in #getInputs", new Object[]{Integer.valueOf(format)}));
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ReprocessFormatsMap)) {
            return false;
        }
        return Arrays.equals(this.mEntry, ((ReprocessFormatsMap) obj).mEntry);
    }

    public int hashCode() {
        return HashCodeHelpers.hashCode(this.mEntry);
    }
}
