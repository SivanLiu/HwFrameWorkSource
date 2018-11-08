package android.telephony.mbms;

import android.os.Parcel;
import android.text.TextUtils;
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
        if (newNames == null || newNames.isEmpty() || TextUtils.isEmpty(newClassName) || newLocales == null || newLocales.isEmpty() || TextUtils.isEmpty(newServiceId) || start == null || end == null) {
            throw new IllegalArgumentException("Bad ServiceInfo construction");
        } else if (newNames.size() > 1000) {
            throw new RuntimeException("bad map length " + newNames.size());
        } else if (newLocales.size() > 1000) {
            throw new RuntimeException("bad locales length " + newLocales.size());
        } else {
            this.names = new HashMap(newNames.size());
            this.names.putAll(newNames);
            this.className = newClassName;
            this.locales = new ArrayList(newLocales);
            this.serviceId = newServiceId;
            this.sessionStartTime = (Date) start.clone();
            this.sessionEndTime = (Date) end.clone();
        }
    }

    protected ServiceInfo(Parcel in) {
        int mapCount = in.readInt();
        if (mapCount > 1000 || mapCount < 0) {
            throw new RuntimeException("bad map length" + mapCount);
        }
        this.names = new HashMap(mapCount);
        int mapCount2 = mapCount;
        while (true) {
            mapCount = mapCount2 - 1;
            if (mapCount2 <= 0) {
                break;
            }
            this.names.put((Locale) in.readSerializable(), in.readString());
            mapCount2 = mapCount;
        }
        this.className = in.readString();
        int localesCount = in.readInt();
        if (localesCount > 1000 || localesCount < 0) {
            throw new RuntimeException("bad locale length " + localesCount);
        }
        this.locales = new ArrayList(localesCount);
        int localesCount2 = localesCount;
        while (true) {
            localesCount = localesCount2 - 1;
            if (localesCount2 > 0) {
                this.locales.add((Locale) in.readSerializable());
                localesCount2 = localesCount;
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
        boolean z = false;
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ServiceInfo)) {
            return false;
        }
        ServiceInfo that = (ServiceInfo) o;
        if (Objects.equals(this.names, that.names) && Objects.equals(this.className, that.className) && Objects.equals(this.locales, that.locales) && Objects.equals(this.serviceId, that.serviceId) && Objects.equals(this.sessionStartTime, that.sessionStartTime)) {
            z = Objects.equals(this.sessionEndTime, that.sessionEndTime);
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.names, this.className, this.locales, this.serviceId, this.sessionStartTime, this.sessionEndTime});
    }
}
