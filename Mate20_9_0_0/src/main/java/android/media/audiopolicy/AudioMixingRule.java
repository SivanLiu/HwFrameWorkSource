package android.media.audiopolicy;

import android.annotation.SystemApi;
import android.media.AudioAttributes;
import android.os.Parcel;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

@SystemApi
public class AudioMixingRule {
    public static final int RULE_EXCLUDE_ATTRIBUTE_CAPTURE_PRESET = 32770;
    public static final int RULE_EXCLUDE_ATTRIBUTE_USAGE = 32769;
    public static final int RULE_EXCLUDE_UID = 32772;
    private static final int RULE_EXCLUSION_MASK = 32768;
    @SystemApi
    public static final int RULE_MATCH_ATTRIBUTE_CAPTURE_PRESET = 2;
    @SystemApi
    public static final int RULE_MATCH_ATTRIBUTE_USAGE = 1;
    @SystemApi
    public static final int RULE_MATCH_UID = 4;
    private final ArrayList<AudioMixMatchCriterion> mCriteria;
    private final int mTargetMixType;

    static final class AudioMixMatchCriterion {
        final AudioAttributes mAttr;
        final int mIntProp;
        final int mRule;

        AudioMixMatchCriterion(AudioAttributes attributes, int rule) {
            this.mAttr = attributes;
            this.mIntProp = Integer.MIN_VALUE;
            this.mRule = rule;
        }

        AudioMixMatchCriterion(Integer intProp, int rule) {
            this.mAttr = null;
            this.mIntProp = intProp.intValue();
            this.mRule = rule;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.mAttr, Integer.valueOf(this.mIntProp), Integer.valueOf(this.mRule)});
        }

        void writeToParcel(Parcel dest) {
            dest.writeInt(this.mRule);
            int match_rule = this.mRule & -32769;
            if (match_rule != 4) {
                switch (match_rule) {
                    case 1:
                        dest.writeInt(this.mAttr.getUsage());
                        return;
                    case 2:
                        dest.writeInt(this.mAttr.getCapturePreset());
                        return;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown match rule");
                        stringBuilder.append(match_rule);
                        stringBuilder.append(" when writing to Parcel");
                        Log.e("AudioMixMatchCriterion", stringBuilder.toString());
                        dest.writeInt(-1);
                        return;
                }
            }
            dest.writeInt(this.mIntProp);
        }
    }

    @SystemApi
    public static class Builder {
        private ArrayList<AudioMixMatchCriterion> mCriteria = new ArrayList();
        private int mTargetMixType = -1;

        @SystemApi
        public Builder addRule(AudioAttributes attrToMatch, int rule) throws IllegalArgumentException {
            if (AudioMixingRule.isValidAttributesSystemApiRule(rule)) {
                return checkAddRuleObjInternal(rule, attrToMatch);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal rule value ");
            stringBuilder.append(rule);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        @SystemApi
        public Builder excludeRule(AudioAttributes attrToMatch, int rule) throws IllegalArgumentException {
            if (AudioMixingRule.isValidAttributesSystemApiRule(rule)) {
                return checkAddRuleObjInternal(32768 | rule, attrToMatch);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal rule value ");
            stringBuilder.append(rule);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        @SystemApi
        public Builder addMixRule(int rule, Object property) throws IllegalArgumentException {
            if (AudioMixingRule.isValidSystemApiRule(rule)) {
                return checkAddRuleObjInternal(rule, property);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal rule value ");
            stringBuilder.append(rule);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        @SystemApi
        public Builder excludeMixRule(int rule, Object property) throws IllegalArgumentException {
            if (AudioMixingRule.isValidSystemApiRule(rule)) {
                return checkAddRuleObjInternal(32768 | rule, property);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal rule value ");
            stringBuilder.append(rule);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        private Builder checkAddRuleObjInternal(int rule, Object property) throws IllegalArgumentException {
            if (property == null) {
                throw new IllegalArgumentException("Illegal null argument for mixing rule");
            } else if (!AudioMixingRule.isValidRule(rule)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal rule value ");
                stringBuilder.append(rule);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (AudioMixingRule.isAudioAttributeRule(-32769 & rule)) {
                if (property instanceof AudioAttributes) {
                    return addRuleInternal((AudioAttributes) property, null, rule);
                }
                throw new IllegalArgumentException("Invalid AudioAttributes argument");
            } else if (property instanceof Integer) {
                return addRuleInternal(null, (Integer) property, rule);
            } else {
                throw new IllegalArgumentException("Invalid Integer argument");
            }
        }

        /* JADX WARNING: Missing block: B:58:0x00ee, code skipped:
            return r7;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private Builder addRuleInternal(AudioAttributes attrToMatch, Integer intProp, int rule) throws IllegalArgumentException {
            if (this.mTargetMixType == -1) {
                if (AudioMixingRule.isPlayerRule(rule)) {
                    this.mTargetMixType = 0;
                } else {
                    this.mTargetMixType = 1;
                }
            } else if ((this.mTargetMixType == 0 && !AudioMixingRule.isPlayerRule(rule)) || (this.mTargetMixType == 1 && AudioMixingRule.isPlayerRule(rule))) {
                throw new IllegalArgumentException("Incompatible rule for mix");
            }
            synchronized (this.mCriteria) {
                Iterator<AudioMixMatchCriterion> crIterator = this.mCriteria.iterator();
                int match_rule = -32769 & rule;
                while (crIterator.hasNext()) {
                    AudioMixMatchCriterion criterion = (AudioMixMatchCriterion) crIterator.next();
                    StringBuilder stringBuilder;
                    if (match_rule != 4) {
                        switch (match_rule) {
                            case 1:
                                if (criterion.mAttr.getUsage() != attrToMatch.getUsage()) {
                                    break;
                                } else if (criterion.mRule == rule) {
                                    return this;
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Contradictory rule exists for ");
                                    stringBuilder.append(attrToMatch);
                                    throw new IllegalArgumentException(stringBuilder.toString());
                                }
                            case 2:
                                if (criterion.mAttr.getCapturePreset() != attrToMatch.getCapturePreset()) {
                                    break;
                                } else if (criterion.mRule == rule) {
                                    return this;
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Contradictory rule exists for ");
                                    stringBuilder.append(attrToMatch);
                                    throw new IllegalArgumentException(stringBuilder.toString());
                                }
                            default:
                                break;
                        }
                    } else if (criterion.mIntProp == intProp.intValue()) {
                        if (criterion.mRule == rule) {
                            return this;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Contradictory rule exists for UID ");
                        stringBuilder.append(intProp);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                if (match_rule != 4) {
                    switch (match_rule) {
                        case 1:
                        case 2:
                            this.mCriteria.add(new AudioMixMatchCriterion(attrToMatch, rule));
                            break;
                        default:
                            throw new IllegalStateException("Unreachable code in addRuleInternal()");
                    }
                }
                this.mCriteria.add(new AudioMixMatchCriterion(intProp, rule));
            }
        }

        Builder addRuleFromParcel(Parcel in) throws IllegalArgumentException {
            int rule = in.readInt();
            int match_rule = -32769 & rule;
            AudioAttributes attr = null;
            Integer intProp = null;
            if (match_rule != 4) {
                switch (match_rule) {
                    case 1:
                        attr = new android.media.AudioAttributes.Builder().setUsage(in.readInt()).build();
                        break;
                    case 2:
                        attr = new android.media.AudioAttributes.Builder().setInternalCapturePreset(in.readInt()).build();
                        break;
                    default:
                        in.readInt();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Illegal rule value ");
                        stringBuilder.append(rule);
                        stringBuilder.append(" in parcel");
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            intProp = new Integer(in.readInt());
            return addRuleInternal(attr, intProp, rule);
        }

        public AudioMixingRule build() {
            return new AudioMixingRule(this.mTargetMixType, this.mCriteria);
        }
    }

    private AudioMixingRule(int mixType, ArrayList<AudioMixMatchCriterion> criteria) {
        this.mCriteria = criteria;
        this.mTargetMixType = mixType;
    }

    boolean isAffectingUsage(int usage) {
        Iterator it = this.mCriteria.iterator();
        while (it.hasNext()) {
            AudioMixMatchCriterion criterion = (AudioMixMatchCriterion) it.next();
            if ((criterion.mRule & 1) != 0 && criterion.mAttr != null && criterion.mAttr.getUsage() == usage) {
                return true;
            }
        }
        return false;
    }

    private static boolean areCriteriaEquivalent(ArrayList<AudioMixMatchCriterion> cr1, ArrayList<AudioMixMatchCriterion> cr2) {
        boolean z = false;
        if (cr1 == null || cr2 == null) {
            return false;
        }
        if (cr1 == cr2) {
            return true;
        }
        if (cr1.size() != cr2.size()) {
            return false;
        }
        if (cr1.hashCode() == cr2.hashCode()) {
            z = true;
        }
        return z;
    }

    int getTargetMixType() {
        return this.mTargetMixType;
    }

    ArrayList<AudioMixMatchCriterion> getCriteria() {
        return this.mCriteria;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioMixingRule that = (AudioMixingRule) o;
        if (!(this.mTargetMixType == that.mTargetMixType && areCriteriaEquivalent(this.mCriteria, that.mCriteria))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.mTargetMixType), this.mCriteria});
    }

    private static boolean isValidSystemApiRule(int rule) {
        if (rule != 4) {
            switch (rule) {
                case 1:
                case 2:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private static boolean isValidAttributesSystemApiRule(int rule) {
        switch (rule) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }

    private static boolean isValidRule(int rule) {
        int match_rule = -32769 & rule;
        if (match_rule != 4) {
            switch (match_rule) {
                case 1:
                case 2:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private static boolean isPlayerRule(int rule) {
        int match_rule = -32769 & rule;
        if (match_rule == 1 || match_rule == 4) {
            return true;
        }
        return false;
    }

    private static boolean isAudioAttributeRule(int match_rule) {
        switch (match_rule) {
            case 1:
            case 2:
                return true;
            default:
                return false;
        }
    }
}
