package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class RaEvent implements Parcelable {
    public static final Creator<RaEvent> CREATOR = new Creator<RaEvent>() {
        public RaEvent createFromParcel(Parcel in) {
            return new RaEvent(in, null);
        }

        public RaEvent[] newArray(int size) {
            return new RaEvent[size];
        }
    };
    public static final long NO_LIFETIME = -1;
    public final long dnsslLifetime;
    public final long prefixPreferredLifetime;
    public final long prefixValidLifetime;
    public final long rdnssLifetime;
    public final long routeInfoLifetime;
    public final long routerLifetime;

    public static class Builder {
        long dnsslLifetime = -1;
        long prefixPreferredLifetime = -1;
        long prefixValidLifetime = -1;
        long rdnssLifetime = -1;
        long routeInfoLifetime = -1;
        long routerLifetime = -1;

        public RaEvent build() {
            return new RaEvent(this.routerLifetime, this.prefixValidLifetime, this.prefixPreferredLifetime, this.routeInfoLifetime, this.rdnssLifetime, this.dnsslLifetime);
        }

        public Builder updateRouterLifetime(long lifetime) {
            this.routerLifetime = updateLifetime(this.routerLifetime, lifetime);
            return this;
        }

        public Builder updatePrefixValidLifetime(long lifetime) {
            this.prefixValidLifetime = updateLifetime(this.prefixValidLifetime, lifetime);
            return this;
        }

        public Builder updatePrefixPreferredLifetime(long lifetime) {
            this.prefixPreferredLifetime = updateLifetime(this.prefixPreferredLifetime, lifetime);
            return this;
        }

        public Builder updateRouteInfoLifetime(long lifetime) {
            this.routeInfoLifetime = updateLifetime(this.routeInfoLifetime, lifetime);
            return this;
        }

        public Builder updateRdnssLifetime(long lifetime) {
            this.rdnssLifetime = updateLifetime(this.rdnssLifetime, lifetime);
            return this;
        }

        public Builder updateDnsslLifetime(long lifetime) {
            this.dnsslLifetime = updateLifetime(this.dnsslLifetime, lifetime);
            return this;
        }

        private long updateLifetime(long currentLifetime, long newLifetime) {
            if (currentLifetime == -1) {
                return newLifetime;
            }
            return Math.min(currentLifetime, newLifetime);
        }
    }

    /* synthetic */ RaEvent(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public RaEvent(long routerLifetime, long prefixValidLifetime, long prefixPreferredLifetime, long routeInfoLifetime, long rdnssLifetime, long dnsslLifetime) {
        this.routerLifetime = routerLifetime;
        this.prefixValidLifetime = prefixValidLifetime;
        this.prefixPreferredLifetime = prefixPreferredLifetime;
        this.routeInfoLifetime = routeInfoLifetime;
        this.rdnssLifetime = rdnssLifetime;
        this.dnsslLifetime = dnsslLifetime;
    }

    private RaEvent(Parcel in) {
        this.routerLifetime = in.readLong();
        this.prefixValidLifetime = in.readLong();
        this.prefixPreferredLifetime = in.readLong();
        this.routeInfoLifetime = in.readLong();
        this.rdnssLifetime = in.readLong();
        this.dnsslLifetime = in.readLong();
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.routerLifetime);
        out.writeLong(this.prefixValidLifetime);
        out.writeLong(this.prefixPreferredLifetime);
        out.writeLong(this.routeInfoLifetime);
        out.writeLong(this.rdnssLifetime);
        out.writeLong(this.dnsslLifetime);
    }

    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("RaEvent(lifetimes: ");
        stringBuilder.append(String.format("router=%ds, ", new Object[]{Long.valueOf(this.routerLifetime)}));
        stringBuilder.append(String.format("prefix_valid=%ds, ", new Object[]{Long.valueOf(this.prefixValidLifetime)}));
        stringBuilder.append(String.format("prefix_preferred=%ds, ", new Object[]{Long.valueOf(this.prefixPreferredLifetime)}));
        stringBuilder.append(String.format("route_info=%ds, ", new Object[]{Long.valueOf(this.routeInfoLifetime)}));
        stringBuilder.append(String.format("rdnss=%ds, ", new Object[]{Long.valueOf(this.rdnssLifetime)}));
        stringBuilder.append(String.format("dnssl=%ds)", new Object[]{Long.valueOf(this.dnsslLifetime)}));
        return stringBuilder.toString();
    }
}
