package android.media.effect.effects;

import android.filterfw.core.Frame;
import android.media.effect.EffectContext;
import android.media.effect.FilterEffect;

public class IdentityEffect extends FilterEffect {
    public IdentityEffect(EffectContext context, String name) {
        super(context, name);
    }

    public void apply(int inputTexId, int width, int height, int outputTexId) {
        beginGLEffect();
        Frame inputFrame = frameFromTexture(inputTexId, width, height);
        Frame outputFrame = frameFromTexture(outputTexId, width, height);
        outputFrame.setDataFromFrame(inputFrame);
        inputFrame.release();
        outputFrame.release();
        endGLEffect();
    }

    public void setParameter(String parameterKey, Object value) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown parameter ");
        stringBuilder.append(parameterKey);
        stringBuilder.append(" for IdentityEffect!");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void release() {
    }
}
