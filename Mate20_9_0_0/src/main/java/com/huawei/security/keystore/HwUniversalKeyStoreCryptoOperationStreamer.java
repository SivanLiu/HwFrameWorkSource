package com.huawei.security.keystore;

import android.os.IBinder;
import com.huawei.security.HwKeystoreManager;
import com.huawei.security.keymaster.HwKeymasterDefs;
import com.huawei.security.keymaster.HwOperationResult;
import com.huawei.security.keystore.ArrayUtils.EmptyArray;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.ProviderException;

public class HwUniversalKeyStoreCryptoOperationStreamer {
    private static final int DEFAULT_MAX_CHUNK_SIZE = 65536;
    private byte[] mBuffered;
    private int mBufferedLength;
    private int mBufferedOffset;
    private long mConsumedInputSizeBytes;
    private final Stream mKeyStoreStream;
    private final int mMaxChunkSize;
    private long mProducedOutputSizeBytes;

    interface Stream {
        HwOperationResult finish(byte[] bArr, byte[] bArr2);

        HwOperationResult update(byte[] bArr);
    }

    public static class MainDataStream implements Stream {
        private final HwKeystoreManager mKeyStore;
        private final IBinder mOperationToken;

        public MainDataStream(HwKeystoreManager keyStore, IBinder operationToken) {
            this.mKeyStore = keyStore;
            this.mOperationToken = operationToken;
        }

        public HwOperationResult update(byte[] input) {
            return this.mKeyStore.update(this.mOperationToken, null, input);
        }

        public HwOperationResult finish(byte[] signature, byte[] additionalEntropy) {
            return this.mKeyStore.finish(this.mOperationToken, null, signature, additionalEntropy);
        }
    }

    public HwUniversalKeyStoreCryptoOperationStreamer(Stream operation) {
        this(operation, DEFAULT_MAX_CHUNK_SIZE);
    }

    public HwUniversalKeyStoreCryptoOperationStreamer(Stream operation, int maxChunkSize) {
        this.mBuffered = EmptyArray.BYTE;
        this.mBufferedOffset = 0;
        this.mBufferedLength = 0;
        this.mKeyStoreStream = operation;
        this.mMaxChunkSize = maxChunkSize;
    }

    public byte[] update(byte[] input, int inputOffset, int inputLength) throws HwUniversalKeyStoreException {
        if (inputLength == 0) {
            return EmptyArray.BYTE;
        }
        byte[] chunk;
        ByteArrayOutputStream bufferedOutput = null;
        while (inputLength > 0) {
            int inputBytesInChunk;
            if (this.mBufferedLength + inputLength > this.mMaxChunkSize) {
                inputBytesInChunk = this.mMaxChunkSize - this.mBufferedLength;
                chunk = ArrayUtils.concat(this.mBuffered, this.mBufferedOffset, this.mBufferedLength, input, inputOffset, inputBytesInChunk);
            } else if (this.mBufferedLength == 0 && inputOffset == 0 && inputLength == input.length) {
                chunk = input;
                inputBytesInChunk = input.length;
            } else {
                int inputBytesInChunk2 = inputLength;
                chunk = ArrayUtils.concat(this.mBuffered, this.mBufferedOffset, this.mBufferedLength, input, inputOffset, inputBytesInChunk2);
                inputBytesInChunk = inputBytesInChunk2;
            }
            inputOffset += inputBytesInChunk;
            inputLength -= inputBytesInChunk;
            this.mConsumedInputSizeBytes += (long) inputBytesInChunk;
            HwOperationResult opResult = this.mKeyStoreStream.update(chunk);
            if (opResult == null) {
                throw new ProviderException("Failed to communicate with keystore service");
            } else if (opResult.resultCode == 1) {
                StringBuilder stringBuilder;
                if (opResult.inputConsumed == chunk.length) {
                    this.mBuffered = EmptyArray.BYTE;
                    this.mBufferedOffset = 0;
                    this.mBufferedLength = 0;
                } else if (opResult.inputConsumed <= 0) {
                    if (inputLength <= 0) {
                        this.mBuffered = chunk;
                        this.mBufferedOffset = 0;
                        this.mBufferedLength = chunk.length;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Keystore consumed nothing from max-sized chunk: ");
                        stringBuilder.append(chunk.length);
                        stringBuilder.append(" bytes");
                        throw new HwUniversalKeyStoreException(HwKeymasterDefs.KM_ERROR_UNKNOWN_ERROR, stringBuilder.toString());
                    }
                } else if (opResult.inputConsumed < chunk.length) {
                    this.mBuffered = chunk;
                    this.mBufferedOffset = opResult.inputConsumed;
                    this.mBufferedLength = chunk.length - opResult.inputConsumed;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Keystore consumed more input than provided. Provided: ");
                    stringBuilder.append(chunk.length);
                    stringBuilder.append(", consumed: ");
                    stringBuilder.append(opResult.inputConsumed);
                    throw new HwUniversalKeyStoreException(HwKeymasterDefs.KM_ERROR_UNKNOWN_ERROR, stringBuilder.toString());
                }
                if (opResult.output != null && opResult.output.length > 0) {
                    if (inputLength <= 0) {
                        byte[] result;
                        if (bufferedOutput == null) {
                            result = opResult.output;
                        } else {
                            try {
                                bufferedOutput.write(opResult.output);
                                result = bufferedOutput.toByteArray();
                            } catch (IOException e) {
                                throw new ProviderException("Failed to buffer output", e);
                            }
                        }
                        this.mProducedOutputSizeBytes += (long) result.length;
                        return result;
                    } else if (bufferedOutput == null) {
                        bufferedOutput = new ByteArrayOutputStream();
                        try {
                            bufferedOutput.write(opResult.output);
                        } catch (IOException e2) {
                            throw new ProviderException("Failed to buffer output", e2);
                        }
                    }
                }
            } else {
                throw HwKeystoreManager.getKeyStoreException(opResult.resultCode);
            }
        }
        if (bufferedOutput == null) {
            chunk = EmptyArray.BYTE;
        } else {
            chunk = bufferedOutput.toByteArray();
        }
        this.mProducedOutputSizeBytes += (long) chunk.length;
        return chunk;
    }

    public byte[] doFinal(byte[] input, int inputOffset, int inputLength, byte[] signature, byte[] additionalEntropy) throws HwUniversalKeyStoreException {
        if (inputLength == 0) {
            input = EmptyArray.BYTE;
            inputOffset = 0;
        }
        byte[] output = ArrayUtils.concat(update(input, inputOffset, inputLength), flush());
        HwOperationResult opResult = this.mKeyStoreStream.finish(signature, additionalEntropy);
        if (opResult == null) {
            throw new ProviderException("Failed to communicate with keystore service");
        } else if (opResult.resultCode == 1) {
            this.mProducedOutputSizeBytes += (long) opResult.output.length;
            return ArrayUtils.concat(output, opResult.output);
        } else {
            throw HwKeystoreManager.getKeyStoreException(opResult.resultCode);
        }
    }

    public byte[] flush() throws HwUniversalKeyStoreException {
        if (this.mBufferedLength <= 0) {
            return EmptyArray.BYTE;
        }
        byte[] chunk;
        ByteArrayOutputStream bufferedOutput = null;
        while (this.mBufferedLength > 0) {
            chunk = ArrayUtils.subarray(this.mBuffered, this.mBufferedOffset, this.mBufferedLength);
            HwOperationResult opResult = this.mKeyStoreStream.update(chunk);
            if (opResult == null) {
                throw new ProviderException("Failed to communicate with keystore service");
            } else if (opResult.resultCode != 1) {
                throw HwKeystoreManager.getKeyStoreException(opResult.resultCode);
            } else if (opResult.inputConsumed <= 0) {
                break;
            } else {
                if (opResult.inputConsumed >= chunk.length) {
                    this.mBuffered = EmptyArray.BYTE;
                    this.mBufferedOffset = 0;
                    this.mBufferedLength = 0;
                } else {
                    this.mBuffered = chunk;
                    this.mBufferedOffset = opResult.inputConsumed;
                    this.mBufferedLength = chunk.length - opResult.inputConsumed;
                }
                if (opResult.inputConsumed > chunk.length) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Keystore consumed more input than provided. Provided: ");
                    stringBuilder.append(chunk.length);
                    stringBuilder.append(", consumed: ");
                    stringBuilder.append(opResult.inputConsumed);
                    throw new HwUniversalKeyStoreException(HwKeymasterDefs.KM_ERROR_UNKNOWN_ERROR, stringBuilder.toString());
                } else if (opResult.output != null && opResult.output.length > 0) {
                    if (bufferedOutput == null) {
                        if (this.mBufferedLength == 0) {
                            this.mProducedOutputSizeBytes += (long) opResult.output.length;
                            return opResult.output;
                        }
                        bufferedOutput = new ByteArrayOutputStream();
                    }
                    try {
                        bufferedOutput.write(opResult.output);
                    } catch (IOException e) {
                        throw new ProviderException("Failed to buffer output", e);
                    }
                }
            }
        }
        if (this.mBufferedLength > 0) {
            String stringBuilder2;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Keystore failed to consume last ");
            if (this.mBufferedLength != 1) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(this.mBufferedLength);
                stringBuilder4.append(" bytes");
                stringBuilder2 = stringBuilder4.toString();
            } else {
                stringBuilder2 = "byte";
            }
            stringBuilder3.append(stringBuilder2);
            stringBuilder3.append(" of input");
            throw new HwUniversalKeyStoreException(HwKeymasterDefs.KM_ERROR_UNKNOWN_ERROR, stringBuilder3.toString());
        }
        chunk = bufferedOutput != null ? bufferedOutput.toByteArray() : EmptyArray.BYTE;
        this.mProducedOutputSizeBytes += (long) chunk.length;
        return chunk;
    }

    public long getConsumedInputSizeBytes() {
        return this.mConsumedInputSizeBytes;
    }

    public long getProducedOutputSizeBytes() {
        return this.mProducedOutputSizeBytes;
    }
}
