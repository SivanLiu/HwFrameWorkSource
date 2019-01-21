package android.media.audiopolicy;

import android.app.backup.FullBackup;
import android.media.AudioFormat;
import android.media.audiopolicy.AudioMix.Builder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class AudioPolicyConfig implements Parcelable {
    public static final Creator<AudioPolicyConfig> CREATOR = new Creator<AudioPolicyConfig>() {
        public AudioPolicyConfig createFromParcel(Parcel p) {
            return new AudioPolicyConfig(p, null);
        }

        public AudioPolicyConfig[] newArray(int size) {
            return new AudioPolicyConfig[size];
        }
    };
    private static final String TAG = "AudioPolicyConfig";
    protected int mDuckingPolicy;
    private int mMixCounter;
    protected final ArrayList<AudioMix> mMixes;
    private String mRegistrationId;

    /* synthetic */ AudioPolicyConfig(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    protected AudioPolicyConfig(AudioPolicyConfig conf) {
        this.mDuckingPolicy = 0;
        this.mRegistrationId = null;
        this.mMixCounter = 0;
        this.mMixes = conf.mMixes;
    }

    AudioPolicyConfig(ArrayList<AudioMix> mixes) {
        this.mDuckingPolicy = 0;
        this.mRegistrationId = null;
        this.mMixCounter = 0;
        this.mMixes = mixes;
    }

    public void addMix(AudioMix mix) throws IllegalArgumentException {
        if (mix != null) {
            this.mMixes.add(mix);
            return;
        }
        throw new IllegalArgumentException("Illegal null AudioMix argument");
    }

    public ArrayList<AudioMix> getMixes() {
        return this.mMixes;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.mMixes});
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMixes.size());
        Iterator it = this.mMixes.iterator();
        while (it.hasNext()) {
            AudioMix mix = (AudioMix) it.next();
            dest.writeInt(mix.getRouteFlags());
            dest.writeInt(mix.mCallbackFlags);
            dest.writeInt(mix.mDeviceSystemType);
            dest.writeString(mix.mDeviceAddress);
            dest.writeInt(mix.getFormat().getSampleRate());
            dest.writeInt(mix.getFormat().getEncoding());
            dest.writeInt(mix.getFormat().getChannelMask());
            ArrayList<AudioMixMatchCriterion> criteria = mix.getRule().getCriteria();
            dest.writeInt(criteria.size());
            Iterator it2 = criteria.iterator();
            while (it2.hasNext()) {
                ((AudioMixMatchCriterion) it2.next()).writeToParcel(dest);
            }
        }
    }

    private AudioPolicyConfig(Parcel in) {
        this.mDuckingPolicy = 0;
        this.mRegistrationId = null;
        this.mMixCounter = 0;
        this.mMixes = new ArrayList();
        int nbMixes = in.readInt();
        for (int i = 0; i < nbMixes; i++) {
            Builder mixBuilder = new Builder();
            mixBuilder.setRouteFlags(in.readInt());
            mixBuilder.setCallbackFlags(in.readInt());
            mixBuilder.setDevice(in.readInt(), in.readString());
            int sampleRate = in.readInt();
            mixBuilder.setFormat(new AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(in.readInt()).setEncoding(in.readInt()).build());
            int nbRules = in.readInt();
            AudioMixingRule.Builder ruleBuilder = new AudioMixingRule.Builder();
            for (int j = 0; j < nbRules; j++) {
                ruleBuilder.addRuleFromParcel(in);
            }
            mixBuilder.setMixingRule(ruleBuilder.build());
            this.mMixes.add(mixBuilder.build());
        }
    }

    public String toLogFriendlyString() {
        String textDump = new String("android.media.audiopolicy.AudioPolicyConfig:\n");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(textDump);
        stringBuilder.append(this.mMixes.size());
        stringBuilder.append(" AudioMix: ");
        stringBuilder.append(this.mRegistrationId);
        stringBuilder.append("\n");
        textDump = stringBuilder.toString();
        Iterator it = this.mMixes.iterator();
        while (it.hasNext()) {
            AudioMix mix = (AudioMix) it.next();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(textDump);
            stringBuilder2.append("* route flags=0x");
            stringBuilder2.append(Integer.toHexString(mix.getRouteFlags()));
            stringBuilder2.append("\n");
            textDump = stringBuilder2.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(textDump);
            stringBuilder2.append("  rate=");
            stringBuilder2.append(mix.getFormat().getSampleRate());
            stringBuilder2.append("Hz\n");
            textDump = stringBuilder2.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(textDump);
            stringBuilder2.append("  encoding=");
            stringBuilder2.append(mix.getFormat().getEncoding());
            stringBuilder2.append("\n");
            textDump = stringBuilder2.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(textDump);
            stringBuilder2.append("  channels=0x");
            textDump = stringBuilder2.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(textDump);
            stringBuilder2.append(Integer.toHexString(mix.getFormat().getChannelMask()).toUpperCase());
            stringBuilder2.append("\n");
            textDump = stringBuilder2.toString();
            Iterator it2 = mix.getRule().getCriteria().iterator();
            while (it2.hasNext()) {
                StringBuilder stringBuilder3;
                AudioMixMatchCriterion criterion = (AudioMixMatchCriterion) it2.next();
                switch (criterion.mRule) {
                    case 1:
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append("  match usage ");
                        textDump = stringBuilder3.toString();
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append(criterion.mAttr.usageToString());
                        textDump = stringBuilder3.toString();
                        break;
                    case 2:
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append("  match capture preset ");
                        textDump = stringBuilder3.toString();
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append(criterion.mAttr.getCapturePreset());
                        textDump = stringBuilder3.toString();
                        break;
                    case 4:
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append("  match UID ");
                        textDump = stringBuilder3.toString();
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append(criterion.mIntProp);
                        textDump = stringBuilder3.toString();
                        break;
                    case 32769:
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append("  exclude usage ");
                        textDump = stringBuilder3.toString();
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append(criterion.mAttr.usageToString());
                        textDump = stringBuilder3.toString();
                        break;
                    case 32770:
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append("  exclude capture preset ");
                        textDump = stringBuilder3.toString();
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append(criterion.mAttr.getCapturePreset());
                        textDump = stringBuilder3.toString();
                        break;
                    case AudioMixingRule.RULE_EXCLUDE_UID /*32772*/:
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append("  exclude UID ");
                        textDump = stringBuilder3.toString();
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append(criterion.mIntProp);
                        textDump = stringBuilder3.toString();
                        break;
                    default:
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(textDump);
                        stringBuilder3.append("invalid rule!");
                        textDump = stringBuilder3.toString();
                        break;
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(textDump);
                stringBuilder3.append("\n");
                textDump = stringBuilder3.toString();
            }
        }
        return textDump;
    }

    protected void setRegistration(String regId) {
        boolean newRegNull = true;
        boolean currentRegNull = this.mRegistrationId == null || this.mRegistrationId.isEmpty();
        if (!(regId == null || regId.isEmpty())) {
            newRegNull = false;
        }
        if (currentRegNull || newRegNull || this.mRegistrationId.equals(regId)) {
            this.mRegistrationId = regId == null ? "" : regId;
            Iterator it = this.mMixes.iterator();
            while (it.hasNext()) {
                setMixRegistration((AudioMix) it.next());
            }
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid registration transition from ");
        stringBuilder.append(this.mRegistrationId);
        stringBuilder.append(" to ");
        stringBuilder.append(regId);
        Log.e(str, stringBuilder.toString());
    }

    private void setMixRegistration(AudioMix mix) {
        if (this.mRegistrationId.isEmpty()) {
            mix.setRegistration("");
        } else if ((mix.getRouteFlags() & 2) == 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mRegistrationId);
            stringBuilder.append("mix");
            stringBuilder.append(mixTypeId(mix.getMixType()));
            stringBuilder.append(":");
            stringBuilder.append(this.mMixCounter);
            mix.setRegistration(stringBuilder.toString());
        } else if ((mix.getRouteFlags() & 1) == 1) {
            mix.setRegistration(mix.mDeviceAddress);
        }
        this.mMixCounter++;
    }

    @GuardedBy("mMixes")
    protected void add(ArrayList<AudioMix> mixes) {
        Iterator it = mixes.iterator();
        while (it.hasNext()) {
            AudioMix mix = (AudioMix) it.next();
            setMixRegistration(mix);
            this.mMixes.add(mix);
        }
    }

    @GuardedBy("mMixes")
    protected void remove(ArrayList<AudioMix> mixes) {
        Iterator it = mixes.iterator();
        while (it.hasNext()) {
            this.mMixes.remove((AudioMix) it.next());
        }
    }

    private static String mixTypeId(int type) {
        if (type == 0) {
            return TtmlUtils.TAG_P;
        }
        if (type == 1) {
            return FullBackup.ROOT_TREE_TOKEN;
        }
        return "i";
    }

    protected String getRegistration() {
        return this.mRegistrationId;
    }
}
