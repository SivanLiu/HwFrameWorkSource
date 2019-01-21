package android.renderscript;

import android.renderscript.Program.BaseProgramBuilder;

public class ProgramFragment extends Program {

    public static class Builder extends BaseProgramBuilder {
        public Builder(RenderScript rs) {
            super(rs);
        }

        public ProgramFragment create() {
            int i;
            int idx;
            this.mRS.validate();
            long[] tmp = new long[((((this.mInputCount + this.mOutputCount) + this.mConstantCount) + this.mTextureCount) * 2)];
            String[] texNames = new String[this.mTextureCount];
            int i2 = 0;
            int idx2 = 0;
            for (i = 0; i < this.mInputCount; i++) {
                idx = idx2 + 1;
                tmp[idx2] = (long) ProgramParam.INPUT.mID;
                idx2 = idx + 1;
                tmp[idx] = this.mInputs[i].getID(this.mRS);
            }
            for (i = 0; i < this.mOutputCount; i++) {
                idx = idx2 + 1;
                tmp[idx2] = (long) ProgramParam.OUTPUT.mID;
                idx2 = idx + 1;
                tmp[idx] = this.mOutputs[i].getID(this.mRS);
            }
            for (i = 0; i < this.mConstantCount; i++) {
                idx = idx2 + 1;
                tmp[idx2] = (long) ProgramParam.CONSTANT.mID;
                idx2 = idx + 1;
                tmp[idx] = this.mConstants[i].getID(this.mRS);
            }
            while (true) {
                i = i2;
                if (i < this.mTextureCount) {
                    i2 = idx2 + 1;
                    tmp[idx2] = (long) ProgramParam.TEXTURE_TYPE.mID;
                    idx2 = i2 + 1;
                    tmp[i2] = (long) this.mTextureTypes[i].mID;
                    texNames[i] = this.mTextureNames[i];
                    i2 = i + 1;
                } else {
                    ProgramFragment pf = new ProgramFragment(this.mRS.nProgramFragmentCreate(this.mShader, texNames, tmp), this.mRS);
                    initProgram(pf);
                    return pf;
                }
            }
        }
    }

    ProgramFragment(long id, RenderScript rs) {
        super(id, rs);
    }
}
