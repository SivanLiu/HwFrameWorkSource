package com.huawei.nb.ai;

import java.nio.ByteBuffer;

public class AiModelByteBuffer {
    private ByteBuffer modelByteBuffer = null;
    private final AiModelResponse response;

    public AiModelByteBuffer(AiModelResponse response) {
        this.response = response;
    }

    public AiModelResponse getAiModelResponse() {
        return this.response;
    }

    public int getBufferSize() {
        if (this.modelByteBuffer != null) {
            return this.modelByteBuffer.capacity();
        }
        return 0;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.modelByteBuffer = byteBuffer;
    }

    public ByteBuffer getByteBuffer() {
        return this.modelByteBuffer;
    }
}
