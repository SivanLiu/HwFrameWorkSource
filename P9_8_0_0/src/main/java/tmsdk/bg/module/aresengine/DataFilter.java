package tmsdk.bg.module.aresengine;

import tmsdk.common.module.aresengine.FilterConfig;
import tmsdk.common.module.aresengine.FilterResult;
import tmsdk.common.module.aresengine.TelephonyEntity;
import tmsdkobf.hj;

public abstract class DataFilter<T extends TelephonyEntity> extends hj<T> {
    private DataHandler pT;
    private Object tS = new Object();
    private FilterConfig tT;

    protected abstract FilterResult a(T t, Object... objArr);

    protected void a(DataHandler dataHandler) {
        synchronized (this.tS) {
            this.pT = dataHandler;
        }
    }

    protected void a(T t, FilterResult filterResult, Object... objArr) {
    }

    protected void b(T t, Object... objArr) {
    }

    public abstract FilterConfig defalutFilterConfig();

    public final FilterResult filter(T t, Object... objArr) {
        b(t, objArr);
        Object -l_3_R = a(t, objArr);
        a(t, -l_3_R, objArr);
        synchronized (this.tS) {
            if (this.pT != null) {
                this.pT.sendMessage(-l_3_R);
            }
        }
        return -l_3_R;
    }

    public final synchronized FilterConfig getConfig() {
        return this.tT;
    }

    public final synchronized void setConfig(FilterConfig filterConfig) {
        if (filterConfig != null) {
            this.tT = filterConfig;
        } else {
            throw new NullPointerException("the filter's config can not be null");
        }
    }

    protected void unbind() {
        synchronized (this.tS) {
            this.pT = null;
        }
    }
}
