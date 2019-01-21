package android.telephony.mbms;

import android.os.Parcel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class ServiceInfo {
    static final int MAP_LIMIT = 1000;
    private final String className;
    private final List<Locale> locales;
    private final Map<Locale, String> names;
    private final String serviceId;
    private final Date sessionEndTime;
    private final Date sessionStartTime;

    public ServiceInfo(Map<Locale, String> newNames, String newClassName, List<Locale> newLocales, String newServiceId, Date start, Date end) {
        StringBuilder stringBuilder;
        if (newNames == null || newClassName == null || newLocales == null || newServiceId == null || start == null || end == null) {
            throw new IllegalArgumentException("Bad ServiceInfo construction");
        } else if (newNames.size() > 1000) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bad map length ");
            stringBuilder.append(newNames.size());
            throw new RuntimeException(stringBuilder.toString());
        } else if (newLocales.size() <= 1000) {
            this.names = new HashMap(newNames.size());
            this.names.putAll(newNames);
            this.className = newClassName;
            this.locales = new ArrayList(newLocales);
            this.serviceId = newServiceId;
            this.sessionStartTime = (Date) start.clone();
            this.sessionEndTime = (Date) end.clone();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bad locales length ");
            stringBuilder.append(newLocales.size());
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    protected ServiceInfo(Parcel in) {
        int mapCount = in.readInt();
        if (mapCount > 1000 || mapCount < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad map length");
            stringBuilder.append(mapCount);
            throw new RuntimeException(stringBuilder.toString());
        }
        this.names = new HashMap(mapCount);
        while (true) {
            int mapCount2 = mapCount - 1;
            if (mapCount <= 0) {
                break;
            }
            this.names.put((Locale) in.readSerializable(), in.readString());
            mapCount = mapCount2;
        }
        this.className = in.readString();
        mapCount = in.readInt();
        if (mapCount > 1000 || mapCount < 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("bad locale length ");
            stringBuilder2.append(mapCount);
            throw new RuntimeException(stringBuilder2.toString());
        }
        this.locales = new ArrayList(mapCount);
        while (true) {
            int localesCount = mapCount - 1;
            if (mapCount > 0) {
                this.locales.add((Locale) in.readSerializable());
                mapCount = localesCount;
            } else {
                this.serviceId = in.readString();
                this.sessionStartTime = (Date) in.readSerializable();
                this.sessionEndTime = (Date) in.readSerializable();
                return;
            }
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        Set<Locale> keySet = this.names.keySet();
        dest.writeInt(keySet.size());
        for (Locale l : keySet) {
            dest.writeSerializable(l);
            dest.writeString((String) this.names.get(l));
        }
        dest.writeString(this.className);
        dest.writeInt(this.locales.size());
        for (Locale l2 : this.locales) {
            dest.writeSerializable(l2);
        }
        dest.writeString(this.serviceId);
        dest.writeSerializable(this.sessionStartTime);
        dest.writeSerializable(this.sessionEndTime);
    }

    public CharSequence getNameForLocale(Locale locale) {
        if (this.names.containsKey(locale)) {
            return (CharSequence) this.names.get(locale);
        }
        throw new NoSuchElementException("Locale not supported");
    }

    public Set<Locale> getNamedContentLocales() {
        return Collections.unmodifiableSet(this.names.keySet());
    }

    public String getServiceClassName() {
        return this.className;
    }

    public List<Locale> getLocales() {
        return this.locales;
    }

    public String getServiceId() {
        return this.serviceId;
    }

    public Date getSessionStartTime() {
        return this.sessionStartTime;
    }

    public Date getSessionEndTime() {
        return this.sessionEndTime;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ServiceInfo)) {
            return false;
        }
        ServiceInfo that = (ServiceInfo) o;
        if (!(Objects.equals(this.names, that.names) && Objects.equals(this.className, that.className) && Objects.equals(this.locales, that.locales) && Objects.equals(this.serviceId, that.serviceId) && Objects.equals(this.sessionStartTime, that.sessionStartTime) && Objects.equals(this.sessionEndTime, that.sessionEndTime))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.names, this.className, this.locales, this.serviceId, this.sessionStartTime, this.sessionEndTime});
    }
}
