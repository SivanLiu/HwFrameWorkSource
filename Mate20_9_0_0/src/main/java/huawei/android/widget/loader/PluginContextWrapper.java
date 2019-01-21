package huawei.android.widget.loader;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.Resources.Theme;

public class PluginContextWrapper extends ContextWrapper {
    private Resources mResource;
    private Theme mTheme;

    public PluginContextWrapper(Context base) {
        super(base);
        this.mResource = ResLoader.getInstance().getResources(base);
        this.mTheme = ResLoader.getInstance().getTheme(base);
    }

    public Resources getResources() {
        return this.mResource;
    }

    public Theme getTheme() {
        return this.mTheme;
    }

    public Object getSystemService(String name) {
        if (name.equals("layout_inflater")) {
            return new PluginLayoutInflater(this);
        }
        return super.getSystemService(name);
    }
}
