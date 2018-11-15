package android.arch.lifecycle;

import android.app.Application;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import java.lang.reflect.InvocationTargetException;

public class ViewModelProvider {
    private static final String DEFAULT_KEY = "android.arch.lifecycle.ViewModelProvider.DefaultKey";
    private final Factory mFactory;
    private final ViewModelStore mViewModelStore;

    public interface Factory {
        @NonNull
        <T extends ViewModel> T create(@NonNull Class<T> cls);
    }

    public static class NewInstanceFactory implements Factory {
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            StringBuilder stringBuilder;
            try {
                return (ViewModel) modelClass.newInstance();
            } catch (InstantiationException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot create an instance of ");
                stringBuilder.append(modelClass);
                throw new RuntimeException(stringBuilder.toString(), e);
            } catch (IllegalAccessException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot create an instance of ");
                stringBuilder.append(modelClass);
                throw new RuntimeException(stringBuilder.toString(), e2);
            }
        }
    }

    public static class AndroidViewModelFactory extends NewInstanceFactory {
        private static AndroidViewModelFactory sInstance;
        private Application mApplication;

        @NonNull
        public static AndroidViewModelFactory getInstance(@NonNull Application application) {
            if (sInstance == null) {
                sInstance = new AndroidViewModelFactory(application);
            }
            return sInstance;
        }

        public AndroidViewModelFactory(@NonNull Application application) {
            this.mApplication = application;
        }

        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            StringBuilder stringBuilder;
            if (!AndroidViewModel.class.isAssignableFrom(modelClass)) {
                return super.create(modelClass);
            }
            try {
                return (ViewModel) modelClass.getConstructor(new Class[]{Application.class}).newInstance(new Object[]{this.mApplication});
            } catch (NoSuchMethodException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot create an instance of ");
                stringBuilder.append(modelClass);
                throw new RuntimeException(stringBuilder.toString(), e);
            } catch (IllegalAccessException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot create an instance of ");
                stringBuilder.append(modelClass);
                throw new RuntimeException(stringBuilder.toString(), e2);
            } catch (InstantiationException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot create an instance of ");
                stringBuilder.append(modelClass);
                throw new RuntimeException(stringBuilder.toString(), e3);
            } catch (InvocationTargetException e4) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cannot create an instance of ");
                stringBuilder.append(modelClass);
                throw new RuntimeException(stringBuilder.toString(), e4);
            }
        }
    }

    public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
        this(owner.getViewModelStore(), factory);
    }

    public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
        this.mFactory = factory;
        this.mViewModelStore = store;
    }

    @MainThread
    @NonNull
    public <T extends ViewModel> T get(@NonNull Class<T> modelClass) {
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("android.arch.lifecycle.ViewModelProvider.DefaultKey:");
            stringBuilder.append(canonicalName);
            return get(stringBuilder.toString(), modelClass);
        }
        throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
    }

    @MainThread
    @NonNull
    public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
        ViewModel viewModel = this.mViewModelStore.get(key);
        if (modelClass.isInstance(viewModel)) {
            return viewModel;
        }
        viewModel = this.mFactory.create(modelClass);
        this.mViewModelStore.put(key, viewModel);
        return viewModel;
    }
}
