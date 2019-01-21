package android.renderscript;

import android.renderscript.Program.BaseProgramBuilder;
import android.renderscript.Program.TextureType;

public class ProgramFragmentFixedFunction extends ProgramFragment {

    public static class Builder {
        public static final int MAX_TEXTURE = 2;
        int mNumTextures;
        boolean mPointSpriteEnable = false;
        RenderScript mRS;
        String mShader;
        Slot[] mSlots = new Slot[2];
        boolean mVaryingColorEnable;

        public enum EnvMode {
            REPLACE(1),
            MODULATE(2),
            DECAL(3);
            
            int mID;

            private EnvMode(int id) {
                this.mID = id;
            }
        }

        public enum Format {
            ALPHA(1),
            LUMINANCE_ALPHA(2),
            RGB(3),
            RGBA(4);
            
            int mID;

            private Format(int id) {
                this.mID = id;
            }
        }

        private class Slot {
            EnvMode env;
            Format format;

            Slot(EnvMode _env, Format _fmt) {
                this.env = _env;
                this.format = _fmt;
            }
        }

        private void buildShaderString() {
            this.mShader = "//rs_shader_internal\n";
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mShader);
            stringBuilder.append("varying lowp vec4 varColor;\n");
            this.mShader = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mShader);
            stringBuilder.append("varying vec2 varTex0;\n");
            this.mShader = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mShader);
            stringBuilder.append("void main() {\n");
            this.mShader = stringBuilder.toString();
            if (this.mVaryingColorEnable) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.mShader);
                stringBuilder.append("  lowp vec4 col = varColor;\n");
                this.mShader = stringBuilder.toString();
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.mShader);
                stringBuilder.append("  lowp vec4 col = UNI_Color;\n");
                this.mShader = stringBuilder.toString();
            }
            if (this.mNumTextures != 0) {
                if (this.mPointSpriteEnable) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mShader);
                    stringBuilder.append("  vec2 t0 = gl_PointCoord;\n");
                    this.mShader = stringBuilder.toString();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mShader);
                    stringBuilder.append("  vec2 t0 = varTex0.xy;\n");
                    this.mShader = stringBuilder.toString();
                }
            }
            for (int i = 0; i < this.mNumTextures; i++) {
                StringBuilder stringBuilder2;
                switch (this.mSlots[i].env) {
                    case REPLACE:
                        switch (this.mSlots[i].format) {
                            case ALPHA:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.mShader);
                                stringBuilder2.append("  col.a = texture2D(UNI_Tex0, t0).a;\n");
                                this.mShader = stringBuilder2.toString();
                                break;
                            case LUMINANCE_ALPHA:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.mShader);
                                stringBuilder2.append("  col.rgba = texture2D(UNI_Tex0, t0).rgba;\n");
                                this.mShader = stringBuilder2.toString();
                                break;
                            case RGB:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.mShader);
                                stringBuilder2.append("  col.rgb = texture2D(UNI_Tex0, t0).rgb;\n");
                                this.mShader = stringBuilder2.toString();
                                break;
                            case RGBA:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.mShader);
                                stringBuilder2.append("  col.rgba = texture2D(UNI_Tex0, t0).rgba;\n");
                                this.mShader = stringBuilder2.toString();
                                break;
                            default:
                                break;
                        }
                    case MODULATE:
                        switch (this.mSlots[i].format) {
                            case ALPHA:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.mShader);
                                stringBuilder2.append("  col.a *= texture2D(UNI_Tex0, t0).a;\n");
                                this.mShader = stringBuilder2.toString();
                                break;
                            case LUMINANCE_ALPHA:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.mShader);
                                stringBuilder2.append("  col.rgba *= texture2D(UNI_Tex0, t0).rgba;\n");
                                this.mShader = stringBuilder2.toString();
                                break;
                            case RGB:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.mShader);
                                stringBuilder2.append("  col.rgb *= texture2D(UNI_Tex0, t0).rgb;\n");
                                this.mShader = stringBuilder2.toString();
                                break;
                            case RGBA:
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(this.mShader);
                                stringBuilder2.append("  col.rgba *= texture2D(UNI_Tex0, t0).rgba;\n");
                                this.mShader = stringBuilder2.toString();
                                break;
                            default:
                                break;
                        }
                    case DECAL:
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.mShader);
                        stringBuilder2.append("  col = texture2D(UNI_Tex0, t0);\n");
                        this.mShader = stringBuilder2.toString();
                        break;
                    default:
                        break;
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mShader);
            stringBuilder.append("  gl_FragColor = col;\n");
            this.mShader = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mShader);
            stringBuilder.append("}\n");
            this.mShader = stringBuilder.toString();
        }

        public Builder(RenderScript rs) {
            this.mRS = rs;
        }

        public Builder setTexture(EnvMode env, Format fmt, int slot) throws IllegalArgumentException {
            if (slot < 0 || slot >= 2) {
                throw new IllegalArgumentException("MAX_TEXTURE exceeded.");
            }
            this.mSlots[slot] = new Slot(env, fmt);
            return this;
        }

        public Builder setPointSpriteTexCoordinateReplacement(boolean enable) {
            this.mPointSpriteEnable = enable;
            return this;
        }

        public Builder setVaryingColor(boolean enable) {
            this.mVaryingColorEnable = enable;
            return this;
        }

        public ProgramFragmentFixedFunction create() {
            InternalBuilder sb = new InternalBuilder(this.mRS);
            this.mNumTextures = 0;
            for (int i = 0; i < 2; i++) {
                if (this.mSlots[i] != null) {
                    this.mNumTextures++;
                }
            }
            buildShaderString();
            sb.setShader(this.mShader);
            Type constType = null;
            if (!this.mVaryingColorEnable) {
                android.renderscript.Element.Builder b = new android.renderscript.Element.Builder(this.mRS);
                b.add(Element.F32_4(this.mRS), "Color");
                android.renderscript.Type.Builder typeBuilder = new android.renderscript.Type.Builder(this.mRS, b.create());
                typeBuilder.setX(1);
                constType = typeBuilder.create();
                sb.addConstant(constType);
            }
            for (int i2 = 0; i2 < this.mNumTextures; i2++) {
                sb.addTexture(TextureType.TEXTURE_2D);
            }
            ProgramFragmentFixedFunction pf = sb.create();
            pf.mTextureCount = 2;
            if (!this.mVaryingColorEnable) {
                Allocation constantData = Allocation.createTyped(this.mRS, constType);
                FieldPacker fp = new FieldPacker(16);
                fp.addF32(new Float4(1.0f, 1.0f, 1.0f, 1.0f));
                constantData.setFromFieldPacker(0, fp);
                pf.bindConstants(constantData, 0);
            }
            return pf;
        }
    }

    static class InternalBuilder extends BaseProgramBuilder {
        public InternalBuilder(RenderScript rs) {
            super(rs);
        }

        public ProgramFragmentFixedFunction create() {
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
                    ProgramFragmentFixedFunction pf = new ProgramFragmentFixedFunction(this.mRS.nProgramFragmentCreate(this.mShader, texNames, tmp), this.mRS);
                    initProgram(pf);
                    return pf;
                }
            }
        }
    }

    ProgramFragmentFixedFunction(long id, RenderScript rs) {
        super(id, rs);
    }
}
