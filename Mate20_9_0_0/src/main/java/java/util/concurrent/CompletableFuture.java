package java.util.concurrent;

import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import sun.misc.Unsafe;

public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    static final int ASYNC = 1;
    private static final Executor ASYNC_POOL;
    static final int NESTED = -1;
    private static final long NEXT;
    static final AltResult NIL = new AltResult(null);
    private static final long RESULT;
    static final int SPINS;
    private static final long STACK;
    static final int SYNC = 0;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final boolean USE_COMMON_POOL = (ForkJoinPool.getCommonPoolParallelism() > 1);
    volatile Object result;
    volatile Completion stack;

    static final class AltResult {
        final Throwable ex;

        AltResult(Throwable x) {
            this.ex = x;
        }
    }

    public interface AsynchronousCompletionTask {
    }

    static final class Delayer {
        static final ScheduledThreadPoolExecutor delayer;

        static final class DaemonThreadFactory implements ThreadFactory {
            DaemonThreadFactory() {
            }

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("CompletableFutureDelayScheduler");
                return t;
            }
        }

        Delayer() {
        }

        static ScheduledFuture<?> delay(Runnable command, long delay, TimeUnit unit) {
            return delayer.schedule(command, delay, unit);
        }

        static {
            ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new DaemonThreadFactory());
            delayer = scheduledThreadPoolExecutor;
            scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        }
    }

    static final class Canceller implements BiConsumer<Object, Throwable> {
        final Future<?> f;

        Canceller(Future<?> f) {
            this.f = f;
        }

        public void accept(Object ignore, Throwable ex) {
            if (ex == null && this.f != null && !this.f.isDone()) {
                this.f.cancel(false);
            }
        }
    }

    static final class DelayedCompleter<U> implements Runnable {
        final CompletableFuture<U> f;
        final U u;

        DelayedCompleter(CompletableFuture<U> f, U u) {
            this.f = f;
            this.u = u;
        }

        public void run() {
            if (this.f != null) {
                this.f.complete(this.u);
            }
        }
    }

    static final class DelayedExecutor implements Executor {
        final long delay;
        final Executor executor;
        final TimeUnit unit;

        DelayedExecutor(long delay, TimeUnit unit, Executor executor) {
            this.delay = delay;
            this.unit = unit;
            this.executor = executor;
        }

        public void execute(Runnable r) {
            Delayer.delay(new TaskSubmitter(this.executor, r), this.delay, this.unit);
        }
    }

    static final class TaskSubmitter implements Runnable {
        final Runnable action;
        final Executor executor;

        TaskSubmitter(Executor executor, Runnable action) {
            this.executor = executor;
            this.action = action;
        }

        public void run() {
            this.executor.execute(this.action);
        }
    }

    static final class ThreadPerTaskExecutor implements Executor {
        ThreadPerTaskExecutor() {
        }

        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    static final class Timeout implements Runnable {
        final CompletableFuture<?> f;

        Timeout(CompletableFuture<?> f) {
            this.f = f;
        }

        public void run() {
            if (this.f != null && !this.f.isDone()) {
                this.f.completeExceptionally(new TimeoutException());
            }
        }
    }

    static final class AsyncRun extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<Void> dep;
        Runnable fn;

        AsyncRun(CompletableFuture<Void> dep, Runnable fn) {
            this.dep = dep;
            this.fn = fn;
        }

        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void v) {
        }

        public final boolean exec() {
            run();
            return true;
        }

        public void run() {
            CompletableFuture<Void> completableFuture = this.dep;
            CompletableFuture<Void> d = completableFuture;
            if (completableFuture != null) {
                Runnable runnable = this.fn;
                Runnable f = runnable;
                if (runnable != null) {
                    this.dep = null;
                    this.fn = null;
                    if (d.result == null) {
                        try {
                            f.run();
                            d.completeNull();
                        } catch (Throwable ex) {
                            d.completeThrowable(ex);
                        }
                    }
                    d.postComplete();
                }
            }
        }
    }

    static final class AsyncSupply<T> extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        CompletableFuture<T> dep;
        Supplier<? extends T> fn;

        AsyncSupply(CompletableFuture<T> dep, Supplier<? extends T> fn) {
            this.dep = dep;
            this.fn = fn;
        }

        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void v) {
        }

        public final boolean exec() {
            run();
            return true;
        }

        public void run() {
            CompletableFuture<T> completableFuture = this.dep;
            CompletableFuture<T> d = completableFuture;
            if (completableFuture != null) {
                Supplier<? extends T> supplier = this.fn;
                Supplier<? extends T> f = supplier;
                if (supplier != null) {
                    this.dep = null;
                    this.fn = null;
                    if (d.result == null) {
                        try {
                            d.completeValue(f.get());
                        } catch (Throwable ex) {
                            d.completeThrowable(ex);
                        }
                    }
                    d.postComplete();
                }
            }
        }
    }

    static abstract class Completion extends ForkJoinTask<Void> implements Runnable, AsynchronousCompletionTask {
        volatile Completion next;

        abstract boolean isLive();

        abstract CompletableFuture<?> tryFire(int i);

        Completion() {
        }

        public final void run() {
            tryFire(1);
        }

        public final boolean exec() {
            tryFire(1);
            return false;
        }

        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void v) {
        }
    }

    static final class MinimalStage<T> extends CompletableFuture<T> {
        MinimalStage() {
        }

        MinimalStage(Object r) {
            super(r);
        }

        public <U> CompletableFuture<U> newIncompleteFuture() {
            return new MinimalStage();
        }

        public T get() {
            throw new UnsupportedOperationException();
        }

        public T get(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public T getNow(T t) {
            throw new UnsupportedOperationException();
        }

        public T join() {
            throw new UnsupportedOperationException();
        }

        public boolean complete(T t) {
            throw new UnsupportedOperationException();
        }

        public boolean completeExceptionally(Throwable ex) {
            throw new UnsupportedOperationException();
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }

        public void obtrudeValue(T t) {
            throw new UnsupportedOperationException();
        }

        public void obtrudeException(Throwable ex) {
            throw new UnsupportedOperationException();
        }

        public boolean isDone() {
            throw new UnsupportedOperationException();
        }

        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }

        public boolean isCompletedExceptionally() {
            throw new UnsupportedOperationException();
        }

        public int getNumberOfDependents() {
            throw new UnsupportedOperationException();
        }

        public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
            throw new UnsupportedOperationException();
        }

        public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
            throw new UnsupportedOperationException();
        }

        public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public CompletableFuture<T> completeOnTimeout(T t, long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }

    static final class CoCompletion extends Completion {
        BiCompletion<?, ?, ?> base;

        CoCompletion(BiCompletion<?, ?, ?> base) {
            this.base = base;
        }

        final CompletableFuture<?> tryFire(int mode) {
            BiCompletion<?, ?, ?> biCompletion = this.base;
            BiCompletion<?, ?, ?> c = biCompletion;
            if (biCompletion != null) {
                CompletableFuture<?> tryFire = c.tryFire(mode);
                CompletableFuture<?> d = tryFire;
                if (tryFire != null) {
                    this.base = null;
                    return d;
                }
            }
            return null;
        }

        final boolean isLive() {
            BiCompletion<?, ?, ?> biCompletion = this.base;
            return (biCompletion == null || biCompletion.dep == null) ? false : true;
        }
    }

    static final class Signaller extends Completion implements ManagedBlocker {
        final long deadline;
        boolean interrupted;
        final boolean interruptible;
        long nanos;
        volatile Thread thread = Thread.currentThread();

        Signaller(boolean interruptible, long nanos, long deadline) {
            this.interruptible = interruptible;
            this.nanos = nanos;
            this.deadline = deadline;
        }

        final CompletableFuture<?> tryFire(int ignore) {
            Thread thread = this.thread;
            Thread w = thread;
            if (thread != null) {
                this.thread = null;
                LockSupport.unpark(w);
            }
            return null;
        }

        public boolean isReleasable() {
            if (Thread.interrupted()) {
                this.interrupted = true;
            }
            if (this.interrupted && this.interruptible) {
                return true;
            }
            if (this.deadline != 0) {
                if (this.nanos <= 0) {
                    return true;
                }
                long nanoTime = this.deadline - System.nanoTime();
                this.nanos = nanoTime;
                if (nanoTime <= 0) {
                    return true;
                }
            }
            if (this.thread == null) {
                return true;
            }
            return false;
        }

        public boolean block() {
            while (!isReleasable()) {
                if (this.deadline == 0) {
                    LockSupport.park(this);
                } else {
                    LockSupport.parkNanos(this, this.nanos);
                }
            }
            return true;
        }

        final boolean isLive() {
            return this.thread != null;
        }
    }

    static abstract class UniCompletion<T, V> extends Completion {
        CompletableFuture<V> dep;
        Executor executor;
        CompletableFuture<T> src;

        UniCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src) {
            this.executor = executor;
            this.dep = dep;
            this.src = src;
        }

        final boolean claim() {
            Executor e = this.executor;
            if (compareAndSetForkJoinTaskTag((short) 0, (short) 1)) {
                if (e == null) {
                    return true;
                }
                this.executor = null;
                e.execute(this);
            }
            return false;
        }

        final boolean isLive() {
            return this.dep != null;
        }
    }

    static abstract class BiCompletion<T, U, V> extends UniCompletion<T, V> {
        CompletableFuture<U> snd;

        BiCompletion(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(executor, dep, src);
            this.snd = snd;
        }
    }

    static final class UniAccept<T> extends UniCompletion<T, Void> {
        Consumer<? super T> fn;

        UniAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, Consumer<? super T> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> completableFuture = this.dep;
            CompletableFuture<Void> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                if (d.uniAccept(completableFuture2, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    static final class UniApply<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends V> fn;

        UniApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T, ? extends V> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> completableFuture = this.dep;
            CompletableFuture<V> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                if (d.uniApply(completableFuture2, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    static final class UniCompose<T, V> extends UniCompletion<T, V> {
        Function<? super T, ? extends CompletionStage<V>> fn;

        UniCompose(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, Function<? super T, ? extends CompletionStage<V>> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> completableFuture = this.dep;
            CompletableFuture<V> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                if (d.uniCompose(completableFuture2, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    static final class UniExceptionally<T> extends UniCompletion<T, T> {
        Function<? super Throwable, ? extends T> fn;

        UniExceptionally(CompletableFuture<T> dep, CompletableFuture<T> src, Function<? super Throwable, ? extends T> fn) {
            super(null, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> completableFuture = this.dep;
            CompletableFuture<T> d = completableFuture;
            if (completableFuture != null) {
                completableFuture = this.src;
                CompletableFuture<T> a = completableFuture;
                if (d.uniExceptionally(completableFuture, this.fn, this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    static final class UniHandle<T, V> extends UniCompletion<T, V> {
        BiFunction<? super T, Throwable, ? extends V> fn;

        UniHandle(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, BiFunction<? super T, Throwable, ? extends V> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> completableFuture = this.dep;
            CompletableFuture<V> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                if (d.uniHandle(completableFuture2, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    static final class UniRelay<T> extends UniCompletion<T, T> {
        UniRelay(CompletableFuture<T> dep, CompletableFuture<T> src) {
            super(null, dep, src);
        }

        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> completableFuture = this.dep;
            CompletableFuture<T> d = completableFuture;
            if (completableFuture != null) {
                completableFuture = this.src;
                CompletableFuture<T> a = completableFuture;
                if (d.uniRelay(completableFuture)) {
                    this.src = null;
                    this.dep = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    static final class UniRun<T> extends UniCompletion<T, Void> {
        Runnable fn;

        UniRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, Runnable fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> completableFuture = this.dep;
            CompletableFuture<Void> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                if (d.uniRun(completableFuture2, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    static final class UniWhenComplete<T> extends UniCompletion<T, T> {
        BiConsumer<? super T, ? super Throwable> fn;

        UniWhenComplete(Executor executor, CompletableFuture<T> dep, CompletableFuture<T> src, BiConsumer<? super T, ? super Throwable> fn) {
            super(executor, dep, src);
            this.fn = fn;
        }

        final CompletableFuture<T> tryFire(int mode) {
            CompletableFuture<T> completableFuture = this.dep;
            CompletableFuture<T> d = completableFuture;
            if (completableFuture != null) {
                completableFuture = this.src;
                CompletableFuture<T> a = completableFuture;
                if (d.uniWhenComplete(completableFuture, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.fn = null;
                    return d.postFire(a, mode);
                }
            }
            return null;
        }
    }

    static final class BiAccept<T, U> extends BiCompletion<T, U, Void> {
        BiConsumer<? super T, ? super U> fn;

        BiAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd, BiConsumer<? super T, ? super U> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> completableFuture = this.dep;
            CompletableFuture<Void> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                CompletableFuture<U> completableFuture3 = this.snd;
                CompletableFuture<U> b = completableFuture3;
                if (d.biAccept(completableFuture2, completableFuture3, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    static final class BiApply<T, U, V> extends BiCompletion<T, U, V> {
        BiFunction<? super T, ? super U, ? extends V> fn;

        BiApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd, BiFunction<? super T, ? super U, ? extends V> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> completableFuture = this.dep;
            CompletableFuture<V> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                CompletableFuture<U> completableFuture3 = this.snd;
                CompletableFuture<U> b = completableFuture3;
                if (d.biApply(completableFuture2, completableFuture3, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    static final class BiRelay<T, U> extends BiCompletion<T, U, Void> {
        BiRelay(CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(null, dep, src, snd);
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> completableFuture = this.dep;
            CompletableFuture<Void> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                CompletableFuture<U> completableFuture3 = this.snd;
                CompletableFuture<U> b = completableFuture3;
                if (d.biRelay(completableFuture2, completableFuture3)) {
                    this.src = null;
                    this.snd = null;
                    this.dep = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    static final class BiRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable fn;

        BiRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd, Runnable fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> completableFuture = this.dep;
            CompletableFuture<Void> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                CompletableFuture<U> completableFuture3 = this.snd;
                CompletableFuture<U> b = completableFuture3;
                if (d.biRun(completableFuture2, completableFuture3, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    static final class OrAccept<T, U extends T> extends BiCompletion<T, U, Void> {
        Consumer<? super T> fn;

        OrAccept(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd, Consumer<? super T> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> completableFuture = this.dep;
            CompletableFuture<Void> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                CompletableFuture<U> completableFuture3 = this.snd;
                CompletableFuture<U> b = completableFuture3;
                if (d.orAccept(completableFuture2, completableFuture3, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    static final class OrApply<T, U extends T, V> extends BiCompletion<T, U, V> {
        Function<? super T, ? extends V> fn;

        OrApply(Executor executor, CompletableFuture<V> dep, CompletableFuture<T> src, CompletableFuture<U> snd, Function<? super T, ? extends V> fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<V> tryFire(int mode) {
            CompletableFuture<V> completableFuture = this.dep;
            CompletableFuture<V> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                CompletableFuture<U> completableFuture3 = this.snd;
                CompletableFuture<U> b = completableFuture3;
                if (d.orApply(completableFuture2, completableFuture3, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    static final class OrRelay<T, U> extends BiCompletion<T, U, Object> {
        OrRelay(CompletableFuture<Object> dep, CompletableFuture<T> src, CompletableFuture<U> snd) {
            super(null, dep, src, snd);
        }

        final CompletableFuture<Object> tryFire(int mode) {
            CompletableFuture<Object> completableFuture = this.dep;
            CompletableFuture<Object> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                CompletableFuture<U> completableFuture3 = this.snd;
                CompletableFuture<U> b = completableFuture3;
                if (d.orRelay(completableFuture2, completableFuture3)) {
                    this.src = null;
                    this.snd = null;
                    this.dep = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    static final class OrRun<T, U> extends BiCompletion<T, U, Void> {
        Runnable fn;

        OrRun(Executor executor, CompletableFuture<Void> dep, CompletableFuture<T> src, CompletableFuture<U> snd, Runnable fn) {
            super(executor, dep, src, snd);
            this.fn = fn;
        }

        final CompletableFuture<Void> tryFire(int mode) {
            CompletableFuture<Void> completableFuture = this.dep;
            CompletableFuture<Void> d = completableFuture;
            if (completableFuture != null) {
                CompletableFuture<T> completableFuture2 = this.src;
                CompletableFuture<T> a = completableFuture2;
                CompletableFuture<U> completableFuture3 = this.snd;
                CompletableFuture<U> b = completableFuture3;
                if (d.orRun(completableFuture2, completableFuture3, this.fn, mode > 0 ? null : this)) {
                    this.dep = null;
                    this.src = null;
                    this.snd = null;
                    this.fn = null;
                    return d.postFire(a, b, mode);
                }
            }
            return null;
        }
    }

    final boolean internalComplete(Object r) {
        return U.compareAndSwapObject(this, RESULT, null, r);
    }

    final boolean casStack(Completion cmp, Completion val) {
        return U.compareAndSwapObject(this, STACK, cmp, val);
    }

    final boolean tryPushStack(Completion c) {
        Completion h = this.stack;
        lazySetNext(c, h);
        return U.compareAndSwapObject(this, STACK, h, c);
    }

    final void pushStack(Completion c) {
        do {
        } while (!tryPushStack(c));
    }

    static {
        Executor commonPool;
        int i = 0;
        if (USE_COMMON_POOL) {
            commonPool = ForkJoinPool.commonPool();
        } else {
            commonPool = new ThreadPerTaskExecutor();
        }
        ASYNC_POOL = commonPool;
        if (Runtime.getRuntime().availableProcessors() > 1) {
            i = 256;
        }
        SPINS = i;
        try {
            RESULT = U.objectFieldOffset(CompletableFuture.class.getDeclaredField("result"));
            STACK = U.objectFieldOffset(CompletableFuture.class.getDeclaredField("stack"));
            NEXT = U.objectFieldOffset(Completion.class.getDeclaredField("next"));
            Class cls = LockSupport.class;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    final boolean completeNull() {
        return U.compareAndSwapObject(this, RESULT, null, NIL);
    }

    final Object encodeValue(T t) {
        return t == null ? NIL : t;
    }

    final boolean completeValue(T t) {
        Object obj;
        Unsafe unsafe = U;
        long j = RESULT;
        if (t == null) {
            obj = NIL;
        } else {
            obj = t;
        }
        return unsafe.compareAndSwapObject(this, j, null, obj);
    }

    static AltResult encodeThrowable(Throwable x) {
        return new AltResult(x instanceof CompletionException ? x : new CompletionException(x));
    }

    final boolean completeThrowable(Throwable x) {
        return U.compareAndSwapObject(this, RESULT, null, encodeThrowable(x));
    }

    static Object encodeThrowable(Throwable x, Object r) {
        if (!(x instanceof CompletionException)) {
            x = new CompletionException(x);
        } else if ((r instanceof AltResult) && x == ((AltResult) r).ex) {
            return r;
        }
        return new AltResult(x);
    }

    final boolean completeThrowable(Throwable x, Object r) {
        return U.compareAndSwapObject(this, RESULT, null, encodeThrowable(x, r));
    }

    Object encodeOutcome(T t, Throwable x) {
        if (x == null) {
            return t == null ? NIL : t;
        } else {
            return encodeThrowable(x);
        }
    }

    static Object encodeRelay(Object r) {
        if (r instanceof AltResult) {
            Throwable th = ((AltResult) r).ex;
            Throwable x = th;
            if (!(th == null || (x instanceof CompletionException))) {
                return new AltResult(new CompletionException(x));
            }
        }
        return r;
    }

    final boolean completeRelay(Object r) {
        return U.compareAndSwapObject(this, RESULT, null, encodeRelay(r));
    }

    private static <T> T reportGet(Object r) throws InterruptedException, ExecutionException {
        if (r == null) {
            throw new InterruptedException();
        } else if (!(r instanceof AltResult)) {
            return r;
        } else {
            Throwable th = ((AltResult) r).ex;
            Throwable x = th;
            if (th == null) {
                return null;
            }
            if (x instanceof CancellationException) {
                throw ((CancellationException) x);
            }
            if (x instanceof CompletionException) {
                th = x.getCause();
                Throwable cause = th;
                if (th != null) {
                    x = cause;
                }
            }
            throw new ExecutionException(x);
        }
    }

    private static <T> T reportJoin(Object r) {
        if (!(r instanceof AltResult)) {
            return r;
        }
        Throwable th = ((AltResult) r).ex;
        Throwable x = th;
        if (th == null) {
            return null;
        }
        if (x instanceof CancellationException) {
            throw ((CancellationException) x);
        } else if (x instanceof CompletionException) {
            throw ((CompletionException) x);
        } else {
            throw new CompletionException(x);
        }
    }

    static Executor screenExecutor(Executor e) {
        if (!USE_COMMON_POOL && e == ForkJoinPool.commonPool()) {
            return ASYNC_POOL;
        }
        if (e != null) {
            return e;
        }
        throw new NullPointerException();
    }

    static void lazySetNext(Completion c, Completion next) {
        U.putOrderedObject(c, NEXT, next);
    }

    final void postComplete() {
        CompletableFuture<?> f = this;
        while (true) {
            Completion completion = f.stack;
            Completion h = completion;
            if (completion == null) {
                if (f != this) {
                    f = this;
                    completion = this.stack;
                    h = completion;
                    if (completion == null) {
                        return;
                    }
                }
                return;
            }
            completion = h.next;
            Completion t = completion;
            if (f.casStack(h, completion)) {
                if (t != null) {
                    if (f != this) {
                        pushStack(h);
                    } else {
                        h.next = null;
                    }
                }
                CompletableFuture<?> tryFire = h.tryFire(-1);
                f = tryFire == null ? this : tryFire;
            }
        }
    }

    final void cleanStack() {
        Completion p = null;
        Completion q = this.stack;
        while (q != null) {
            Completion s = q.next;
            if (q.isLive()) {
                p = q;
                q = s;
            } else if (p == null) {
                casStack(q, s);
                q = this.stack;
            } else {
                p.next = s;
                if (p.isLive()) {
                    q = s;
                } else {
                    p = null;
                    q = this.stack;
                }
            }
        }
    }

    final void push(UniCompletion<?, ?> c) {
        if (c != null) {
            while (this.result == null && !tryPushStack(c)) {
                lazySetNext(c, null);
            }
        }
    }

    final CompletableFuture<T> postFire(CompletableFuture<?> a, int mode) {
        if (!(a == null || a.stack == null)) {
            if (mode < 0 || a.result == null) {
                a.cleanStack();
            } else {
                a.postComplete();
            }
        }
        if (!(this.result == null || this.stack == null)) {
            if (mode < 0) {
                return this;
            }
            postComplete();
        }
        return null;
    }

    final <S> boolean uniApply(CompletableFuture<S> a, Function<? super S, ? extends T> f, UniApply<S, T> c) {
        if (a != null) {
            Throwable th = a.result;
            Throwable r = th;
            if (!(th == null || f == null)) {
                if (this.result == null) {
                    if (r instanceof AltResult) {
                        th = ((AltResult) r).ex;
                        Throwable x = th;
                        if (th != null) {
                            completeThrowable(x, r);
                        } else {
                            r = null;
                        }
                    }
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex) {
                            completeThrowable(ex);
                        }
                    }
                    completeValue(f.apply(r));
                }
                return true;
            }
        }
        return false;
    }

    private <V> CompletableFuture<V> uniApplyStage(Executor e, Function<? super T, ? extends V> f) {
        if (f != null) {
            CompletableFuture<V> d = newIncompleteFuture();
            if (!(e == null && d.uniApply(this, f, null))) {
                UniApply<T, V> c = new UniApply(e, d, this, f);
                push(c);
                c.tryFire(0);
            }
            return d;
        }
        throw new NullPointerException();
    }

    final <S> boolean uniAccept(CompletableFuture<S> a, Consumer<? super S> f, UniAccept<S> c) {
        if (a != null) {
            Throwable th = a.result;
            Throwable r = th;
            if (!(th == null || f == null)) {
                if (this.result == null) {
                    if (r instanceof AltResult) {
                        th = ((AltResult) r).ex;
                        Throwable x = th;
                        if (th != null) {
                            completeThrowable(x, r);
                        } else {
                            r = null;
                        }
                    }
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex) {
                            completeThrowable(ex);
                        }
                    }
                    f.accept(r);
                    completeNull();
                }
                return true;
            }
        }
        return false;
    }

    private CompletableFuture<Void> uniAcceptStage(Executor e, Consumer<? super T> f) {
        if (f != null) {
            CompletableFuture<Void> d = newIncompleteFuture();
            if (!(e == null && d.uniAccept(this, f, null))) {
                UniAccept<T> c = new UniAccept(e, d, this, f);
                push(c);
                c.tryFire(0);
            }
            return d;
        }
        throw new NullPointerException();
    }

    final boolean uniRun(CompletableFuture<?> a, Runnable f, UniRun<?> c) {
        if (a != null) {
            Object obj = a.result;
            Object r = obj;
            if (!(obj == null || f == null)) {
                if (this.result == null) {
                    if (r instanceof AltResult) {
                        Throwable th = ((AltResult) r).ex;
                        Throwable x = th;
                        if (th != null) {
                            completeThrowable(x, r);
                        }
                    }
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex) {
                            completeThrowable(ex);
                        }
                    }
                    f.run();
                    completeNull();
                }
                return true;
            }
        }
        return false;
    }

    private CompletableFuture<Void> uniRunStage(Executor e, Runnable f) {
        if (f != null) {
            CompletableFuture<Void> d = newIncompleteFuture();
            if (!(e == null && d.uniRun(this, f, null))) {
                UniRun<T> c = new UniRun(e, d, this, f);
                push(c);
                c.tryFire(0);
            }
            return d;
        }
        throw new NullPointerException();
    }

    final boolean uniWhenComplete(CompletableFuture<T> a, BiConsumer<? super T, ? super Throwable> f, UniWhenComplete<T> c) {
        Throwable ex;
        Throwable x = null;
        if (a != null) {
            Throwable th = a.result;
            Throwable r = th;
            if (!(th == null || f == null)) {
                if (this.result == null) {
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex2) {
                            if (x == null) {
                                x = ex2;
                            } else if (x != ex2) {
                                x.addSuppressed(ex2);
                            }
                        }
                    }
                    if (r instanceof AltResult) {
                        x = ((AltResult) r).ex;
                        ex2 = null;
                    } else {
                        ex2 = r;
                    }
                    f.accept(ex2, x);
                    if (x == null) {
                        internalComplete(r);
                        return true;
                    }
                    completeThrowable(x, r);
                }
                return true;
            }
        }
        return false;
    }

    private CompletableFuture<T> uniWhenCompleteStage(Executor e, BiConsumer<? super T, ? super Throwable> f) {
        if (f != null) {
            CompletableFuture<T> d = newIncompleteFuture();
            if (!(e == null && d.uniWhenComplete(this, f, null))) {
                UniWhenComplete<T> c = new UniWhenComplete(e, d, this, f);
                push(c);
                c.tryFire(0);
            }
            return d;
        }
        throw new NullPointerException();
    }

    final <S> boolean uniHandle(CompletableFuture<S> a, BiFunction<? super S, Throwable, ? extends T> f, UniHandle<S, T> c) {
        Throwable ex;
        if (a != null) {
            S s = a.result;
            S r = s;
            if (!(s == null || f == null)) {
                if (this.result == null) {
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex2) {
                            completeThrowable(ex2);
                        }
                    }
                    if ((r instanceof AltResult) != null) {
                        ex2 = ((AltResult) r).ex;
                        s = null;
                    } else {
                        ex2 = null;
                        s = r;
                    }
                    completeValue(f.apply(s, ex2));
                }
                return true;
            }
        }
        return false;
    }

    private <V> CompletableFuture<V> uniHandleStage(Executor e, BiFunction<? super T, Throwable, ? extends V> f) {
        if (f != null) {
            CompletableFuture<V> d = newIncompleteFuture();
            if (!(e == null && d.uniHandle(this, f, null))) {
                UniHandle<T, V> c = new UniHandle(e, d, this, f);
                push(c);
                c.tryFire(0);
            }
            return d;
        }
        throw new NullPointerException();
    }

    final boolean uniExceptionally(CompletableFuture<T> a, Function<? super Throwable, ? extends T> f, UniExceptionally<T> c) {
        if (a != null) {
            Object obj = a.result;
            Object r = obj;
            if (!(obj == null || f == null)) {
                if (this.result == null) {
                    try {
                        if (r instanceof AltResult) {
                            Throwable th = ((AltResult) r).ex;
                            Throwable x = th;
                            if (th != null) {
                                if (c != null && !c.claim()) {
                                    return false;
                                }
                                completeValue(f.apply(x));
                            }
                        }
                        internalComplete(r);
                    } catch (Throwable ex) {
                        completeThrowable(ex);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private CompletableFuture<T> uniExceptionallyStage(Function<Throwable, ? extends T> f) {
        if (f != null) {
            CompletableFuture<T> d = newIncompleteFuture();
            if (!d.uniExceptionally(this, f, null)) {
                UniExceptionally<T> c = new UniExceptionally(d, this, f);
                push(c);
                c.tryFire(0);
            }
            return d;
        }
        throw new NullPointerException();
    }

    final boolean uniRelay(CompletableFuture<T> a) {
        if (a != null) {
            Object obj = a.result;
            Object r = obj;
            if (obj != null) {
                if (this.result == null) {
                    completeRelay(r);
                }
                return true;
            }
        }
        return false;
    }

    private CompletableFuture<T> uniCopyStage() {
        CompletableFuture<T> d = newIncompleteFuture();
        Object obj = this.result;
        Object r = obj;
        if (obj != null) {
            d.completeRelay(r);
        } else {
            UniRelay<T> c = new UniRelay(d, this);
            push(c);
            c.tryFire(0);
        }
        return d;
    }

    private MinimalStage<T> uniAsMinimalStage() {
        Object obj = this.result;
        Object r = obj;
        if (obj != null) {
            return new MinimalStage(encodeRelay(r));
        }
        MinimalStage<T> d = new MinimalStage();
        UniRelay<T> c = new UniRelay(d, this);
        push(c);
        c.tryFire(0);
        return d;
    }

    final <S> boolean uniCompose(CompletableFuture<S> a, Function<? super S, ? extends CompletionStage<T>> f, UniCompose<S, T> c) {
        if (a != null) {
            S s = a.result;
            S r = s;
            if (!(s == null || f == null)) {
                if (this.result == null) {
                    if (r instanceof AltResult) {
                        Throwable th = ((AltResult) r).ex;
                        Throwable x = th;
                        if (th != null) {
                            completeThrowable(x, r);
                        } else {
                            r = null;
                        }
                    }
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex) {
                            completeThrowable(ex);
                        }
                    }
                    CompletableFuture<T> g = ((CompletionStage) f.apply(r)).toCompletableFuture();
                    if (g.result == null || !uniRelay(g)) {
                        UniRelay<T> copy = new UniRelay(this, g);
                        g.push(copy);
                        copy.tryFire(0);
                        if (this.result == null) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    private <V> CompletableFuture<V> uniComposeStage(Executor e, Function<? super T, ? extends CompletionStage<V>> f) {
        if (f != null) {
            CompletableFuture<V> d = newIncompleteFuture();
            if (e == null) {
                T t = this.result;
                T r = t;
                if (t != null) {
                    if (r instanceof AltResult) {
                        Throwable th = ((AltResult) r).ex;
                        Throwable x = th;
                        if (th != null) {
                            d.result = encodeThrowable(x, r);
                            return d;
                        }
                        r = null;
                    }
                    try {
                        CompletableFuture<V> g = ((CompletionStage) f.apply(r)).toCompletableFuture();
                        Object obj = g.result;
                        Object s = obj;
                        if (obj != null) {
                            d.completeRelay(s);
                        } else {
                            UniRelay<V> c = new UniRelay(d, g);
                            g.push(c);
                            c.tryFire(0);
                        }
                        return d;
                    } catch (Throwable ex) {
                        d.result = encodeThrowable(ex);
                        return d;
                    }
                }
            }
            UniCompose<T, V> c2 = new UniCompose(e, d, this, f);
            push(c2);
            c2.tryFire(0);
            return d;
        }
        throw new NullPointerException();
    }

    final void bipush(CompletableFuture<?> b, BiCompletion<?, ?, ?> c) {
        if (c != null) {
            while (true) {
                Object obj = this.result;
                Object r = obj;
                if (obj == null && !tryPushStack(c)) {
                    lazySetNext(c, null);
                } else if (b != null && b != this && b.result == null) {
                    Completion q = r != null ? c : new CoCompletion(c);
                    while (b.result == null && !b.tryPushStack(q)) {
                        lazySetNext(q, null);
                    }
                    return;
                } else {
                    return;
                }
            }
            if (b != null) {
            }
        }
    }

    final CompletableFuture<T> postFire(CompletableFuture<?> a, CompletableFuture<?> b, int mode) {
        if (!(b == null || b.stack == null)) {
            if (mode < 0 || b.result == null) {
                b.cleanStack();
            } else {
                b.postComplete();
            }
        }
        return postFire(a, mode);
    }

    final <R, S> boolean biApply(CompletableFuture<R> a, CompletableFuture<S> b, BiFunction<? super R, ? super S, ? extends T> f, BiApply<R, S, T> c) {
        if (a != null) {
            Throwable th = a.result;
            Throwable r = th;
            if (!(th == null || b == null)) {
                S s = b.result;
                S s2 = s;
                if (!(s == null || f == null)) {
                    if (this.result == null) {
                        Throwable x;
                        if (r instanceof AltResult) {
                            th = ((AltResult) r).ex;
                            x = th;
                            if (th != null) {
                                completeThrowable(x, r);
                            } else {
                                r = null;
                            }
                        }
                        if (s2 instanceof AltResult) {
                            th = ((AltResult) s2).ex;
                            x = th;
                            if (th != null) {
                                completeThrowable(x, s2);
                            } else {
                                s2 = null;
                            }
                        }
                        if (c != null) {
                            try {
                                if (!c.claim()) {
                                    return false;
                                }
                            } catch (Throwable ex) {
                                completeThrowable(ex);
                            }
                        }
                        completeValue(f.apply(r, s2));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private <U, V> CompletableFuture<V> biApplyStage(Executor e, CompletionStage<U> o, BiFunction<? super T, ? super U, ? extends V> f) {
        if (f != null) {
            CompletableFuture<U> toCompletableFuture = o.toCompletableFuture();
            CompletableFuture<U> b = toCompletableFuture;
            if (toCompletableFuture != null) {
                CompletableFuture<V> d = newIncompleteFuture();
                if (!(e == null && d.biApply(this, b, f, null))) {
                    BiApply<T, U, V> c = new BiApply(e, d, this, b, f);
                    bipush(b, c);
                    c.tryFire(0);
                }
                return d;
            }
        }
        throw new NullPointerException();
    }

    final <R, S> boolean biAccept(CompletableFuture<R> a, CompletableFuture<S> b, BiConsumer<? super R, ? super S> f, BiAccept<R, S> c) {
        if (a != null) {
            Throwable th = a.result;
            Throwable r = th;
            if (!(th == null || b == null)) {
                S s = b.result;
                S s2 = s;
                if (!(s == null || f == null)) {
                    if (this.result == null) {
                        Throwable x;
                        if (r instanceof AltResult) {
                            th = ((AltResult) r).ex;
                            x = th;
                            if (th != null) {
                                completeThrowable(x, r);
                            } else {
                                r = null;
                            }
                        }
                        if (s2 instanceof AltResult) {
                            th = ((AltResult) s2).ex;
                            x = th;
                            if (th != null) {
                                completeThrowable(x, s2);
                            } else {
                                s2 = null;
                            }
                        }
                        if (c != null) {
                            try {
                                if (!c.claim()) {
                                    return false;
                                }
                            } catch (Throwable ex) {
                                completeThrowable(ex);
                            }
                        }
                        f.accept(r, s2);
                        completeNull();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private <U> CompletableFuture<Void> biAcceptStage(Executor e, CompletionStage<U> o, BiConsumer<? super T, ? super U> f) {
        if (f != null) {
            CompletableFuture<U> toCompletableFuture = o.toCompletableFuture();
            CompletableFuture<U> b = toCompletableFuture;
            if (toCompletableFuture != null) {
                CompletableFuture<Void> d = newIncompleteFuture();
                if (!(e == null && d.biAccept(this, b, f, null))) {
                    BiAccept<T, U> c = new BiAccept(e, d, this, b, f);
                    bipush(b, c);
                    c.tryFire(0);
                }
                return d;
            }
        }
        throw new NullPointerException();
    }

    final boolean biRun(CompletableFuture<?> a, CompletableFuture<?> b, Runnable f, BiRun<?, ?> c) {
        if (a != null) {
            Object obj = a.result;
            Object r = obj;
            if (!(obj == null || b == null)) {
                obj = b.result;
                Object s = obj;
                if (!(obj == null || f == null)) {
                    if (this.result == null) {
                        Throwable th;
                        Throwable x;
                        if (r instanceof AltResult) {
                            th = ((AltResult) r).ex;
                            x = th;
                            if (th != null) {
                                completeThrowable(x, r);
                            }
                        }
                        if (s instanceof AltResult) {
                            th = ((AltResult) s).ex;
                            x = th;
                            if (th != null) {
                                completeThrowable(x, s);
                            }
                        }
                        if (c != null) {
                            try {
                                if (!c.claim()) {
                                    return false;
                                }
                            } catch (Throwable ex) {
                                completeThrowable(ex);
                            }
                        }
                        f.run();
                        completeNull();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private CompletableFuture<Void> biRunStage(Executor e, CompletionStage<?> o, Runnable f) {
        if (f != null) {
            CompletableFuture<?> toCompletableFuture = o.toCompletableFuture();
            CompletableFuture<?> b = toCompletableFuture;
            if (toCompletableFuture != null) {
                CompletableFuture<Void> d = newIncompleteFuture();
                if (!(e == null && d.biRun(this, b, f, null))) {
                    BiRun<T, ?> c = new BiRun(e, d, this, b, f);
                    bipush(b, c);
                    c.tryFire(0);
                }
                return d;
            }
        }
        throw new NullPointerException();
    }

    boolean biRelay(CompletableFuture<?> a, CompletableFuture<?> b) {
        if (a != null) {
            Object obj = a.result;
            Object r = obj;
            if (!(obj == null || b == null)) {
                obj = b.result;
                Object s = obj;
                if (obj != null) {
                    if (this.result == null) {
                        Throwable th;
                        Throwable x;
                        if (r instanceof AltResult) {
                            th = ((AltResult) r).ex;
                            x = th;
                            if (th != null) {
                                completeThrowable(x, r);
                            }
                        }
                        if (s instanceof AltResult) {
                            th = ((AltResult) s).ex;
                            x = th;
                            if (th != null) {
                                completeThrowable(x, s);
                            }
                        }
                        completeNull();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    static CompletableFuture<Void> andTree(CompletableFuture<?>[] cfs, int lo, int hi) {
        CompletableFuture<Void> d = new CompletableFuture();
        if (lo > hi) {
            d.result = NIL;
        } else {
            CompletableFuture<?> completableFuture;
            int mid = (lo + hi) >>> 1;
            if (lo == mid) {
                completableFuture = cfs[lo];
            } else {
                completableFuture = andTree(cfs, lo, mid);
            }
            CompletableFuture<?> a = completableFuture;
            if (completableFuture != null) {
                completableFuture = lo == hi ? a : hi == mid + 1 ? cfs[hi] : andTree(cfs, mid + 1, hi);
                CompletableFuture<?> b = completableFuture;
                if (completableFuture != null) {
                    if (!d.biRelay(a, b)) {
                        BiRelay<?, ?> c = new BiRelay(d, a, b);
                        a.bipush(b, c);
                        c.tryFire(0);
                    }
                }
            }
            throw new NullPointerException();
        }
        return d;
    }

    final void orpush(CompletableFuture<?> b, BiCompletion<?, ?, ?> c) {
        if (c != null) {
            while (true) {
                if ((b != null && b.result != null) || this.result != null) {
                    return;
                }
                if (!tryPushStack(c)) {
                    lazySetNext(c, null);
                } else if (b != null && b != this && b.result == null) {
                    Completion q = new CoCompletion(c);
                    while (this.result == null && b.result == null && !b.tryPushStack(q)) {
                        lazySetNext(q, null);
                    }
                    return;
                } else {
                    return;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:6:0x000d, code skipped:
            if (r1 != null) goto L_0x000f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final <R, S extends R> boolean orApply(CompletableFuture<R> a, CompletableFuture<S> b, Function<? super R, ? extends T> f, OrApply<R, S, T> c) {
        Throwable ex;
        if (!(a == null || b == null)) {
            Throwable th = a.result;
            Throwable r = th;
            if (th == null) {
                th = b.result;
                r = th;
            }
            if (f != null) {
                th = r;
                if (this.result == null) {
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex2) {
                            completeThrowable(ex2);
                        }
                    }
                    if ((th instanceof AltResult) != null) {
                        ex2 = ((AltResult) th).ex;
                        r = ex2;
                        if (ex2 != null) {
                            completeThrowable(r, th);
                        } else {
                            th = null;
                        }
                    }
                    completeValue(f.apply(th));
                }
                return true;
            }
        }
        return false;
    }

    private <U extends T, V> CompletableFuture<V> orApplyStage(Executor e, CompletionStage<U> o, Function<? super T, ? extends V> f) {
        if (f != null) {
            CompletableFuture<U> toCompletableFuture = o.toCompletableFuture();
            CompletableFuture<U> b = toCompletableFuture;
            if (toCompletableFuture != null) {
                CompletableFuture<V> d = newIncompleteFuture();
                if (!(e == null && d.orApply(this, b, f, null))) {
                    OrApply<T, U, V> c = new OrApply(e, d, this, b, f);
                    orpush(b, c);
                    c.tryFire(0);
                }
                return d;
            }
        }
        throw new NullPointerException();
    }

    /* JADX WARNING: Missing block: B:6:0x000d, code skipped:
            if (r1 != null) goto L_0x000f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final <R, S extends R> boolean orAccept(CompletableFuture<R> a, CompletableFuture<S> b, Consumer<? super R> f, OrAccept<R, S> c) {
        Throwable ex;
        if (!(a == null || b == null)) {
            Throwable th = a.result;
            Throwable r = th;
            if (th == null) {
                th = b.result;
                r = th;
            }
            if (f != null) {
                th = r;
                if (this.result == null) {
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex2) {
                            completeThrowable(ex2);
                        }
                    }
                    if ((th instanceof AltResult) != null) {
                        ex2 = ((AltResult) th).ex;
                        r = ex2;
                        if (ex2 != null) {
                            completeThrowable(r, th);
                        } else {
                            th = null;
                        }
                    }
                    f.accept(th);
                    completeNull();
                }
                return true;
            }
        }
        return false;
    }

    private <U extends T> CompletableFuture<Void> orAcceptStage(Executor e, CompletionStage<U> o, Consumer<? super T> f) {
        if (f != null) {
            CompletableFuture<U> toCompletableFuture = o.toCompletableFuture();
            CompletableFuture<U> b = toCompletableFuture;
            if (toCompletableFuture != null) {
                CompletableFuture<Void> d = newIncompleteFuture();
                if (!(e == null && d.orAccept(this, b, f, null))) {
                    OrAccept<T, U> c = new OrAccept(e, d, this, b, f);
                    orpush(b, c);
                    c.tryFire(0);
                }
                return d;
            }
        }
        throw new NullPointerException();
    }

    /* JADX WARNING: Missing block: B:6:0x000d, code skipped:
            if (r1 != null) goto L_0x000f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final boolean orRun(CompletableFuture<?> a, CompletableFuture<?> b, Runnable f, OrRun<?, ?> c) {
        Throwable ex;
        if (!(a == null || b == null)) {
            Throwable th = a.result;
            Throwable r = th;
            if (th == null) {
                th = b.result;
                r = th;
            }
            if (f != null) {
                th = r;
                if (this.result == null) {
                    if (c != null) {
                        try {
                            if (!c.claim()) {
                                return false;
                            }
                        } catch (Throwable ex2) {
                            completeThrowable(ex2);
                        }
                    }
                    if ((th instanceof AltResult) != null) {
                        ex2 = ((AltResult) th).ex;
                        r = ex2;
                        if (ex2 != null) {
                            completeThrowable(r, th);
                        }
                    }
                    f.run();
                    completeNull();
                }
                return true;
            }
        }
        return false;
    }

    private CompletableFuture<Void> orRunStage(Executor e, CompletionStage<?> o, Runnable f) {
        if (f != null) {
            CompletableFuture<?> toCompletableFuture = o.toCompletableFuture();
            CompletableFuture<?> b = toCompletableFuture;
            if (toCompletableFuture != null) {
                CompletableFuture<Void> d = newIncompleteFuture();
                if (!(e == null && d.orRun(this, b, f, null))) {
                    OrRun<T, ?> c = new OrRun(e, d, this, b, f);
                    orpush(b, c);
                    c.tryFire(0);
                }
                return d;
            }
        }
        throw new NullPointerException();
    }

    /* JADX WARNING: Missing block: B:5:0x000c, code skipped:
            if (r0 == null) goto L_0x0019;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final boolean orRelay(CompletableFuture<?> a, CompletableFuture<?> b) {
        if (!(a == null || b == null)) {
            Object obj = a.result;
            Object r = obj;
            if (obj == null) {
                obj = b.result;
                r = obj;
            }
            obj = r;
            if (this.result == null) {
                completeRelay(obj);
            }
            return true;
        }
        return false;
    }

    static CompletableFuture<Object> orTree(CompletableFuture<?>[] cfs, int lo, int hi) {
        CompletableFuture<Object> d = new CompletableFuture();
        if (lo <= hi) {
            CompletableFuture<?> completableFuture;
            int mid = (lo + hi) >>> 1;
            if (lo == mid) {
                completableFuture = cfs[lo];
            } else {
                completableFuture = orTree(cfs, lo, mid);
            }
            CompletableFuture<?> a = completableFuture;
            if (completableFuture != null) {
                completableFuture = lo == hi ? a : hi == mid + 1 ? cfs[hi] : orTree(cfs, mid + 1, hi);
                CompletableFuture<?> b = completableFuture;
                if (completableFuture != null) {
                    if (!d.orRelay(a, b)) {
                        OrRelay<?, ?> c = new OrRelay(d, a, b);
                        a.orpush(b, c);
                        c.tryFire(0);
                    }
                }
            }
            throw new NullPointerException();
        }
        return d;
    }

    static <U> CompletableFuture<U> asyncSupplyStage(Executor e, Supplier<U> f) {
        if (f != null) {
            CompletableFuture<U> d = new CompletableFuture();
            e.execute(new AsyncSupply(d, f));
            return d;
        }
        throw new NullPointerException();
    }

    static CompletableFuture<Void> asyncRunStage(Executor e, Runnable f) {
        if (f != null) {
            CompletableFuture<Void> d = new CompletableFuture();
            e.execute(new AsyncRun(d, f));
            return d;
        }
        throw new NullPointerException();
    }

    private Object waitingGet(boolean interruptible) {
        Object r;
        Signaller q = null;
        boolean queued = false;
        int spins = SPINS;
        while (true) {
            Object obj = this.result;
            r = obj;
            if (obj == null) {
                if (spins <= 0) {
                    if (q != null) {
                        if (queued) {
                            try {
                                ForkJoinPool.managedBlock(q);
                            } catch (InterruptedException e) {
                                q.interrupted = true;
                            }
                            if (q.interrupted && interruptible) {
                                break;
                            }
                        }
                        queued = tryPushStack(q);
                    } else {
                        q = new Signaller(interruptible, 0, 0);
                    }
                } else if (ThreadLocalRandom.nextSecondarySeed() >= 0) {
                    spins--;
                }
            } else {
                break;
            }
        }
        if (q != null) {
            q.thread = null;
            if (q.interrupted) {
                if (interruptible) {
                    cleanStack();
                } else {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (r != null) {
            postComplete();
        }
        return r;
    }

    private Object timedGet(long nanos) throws TimeoutException {
        if (Thread.interrupted()) {
            return null;
        }
        if (nanos > 0) {
            Object r;
            long d = System.nanoTime() + nanos;
            long deadline = d == 0 ? 1 : d;
            boolean queued = false;
            Signaller q = null;
            while (true) {
                boolean queued2 = queued;
                Object obj = this.result;
                r = obj;
                if (obj != null) {
                    break;
                }
                if (q != null) {
                    if (queued2) {
                        if (q.nanos > 0) {
                            try {
                                ForkJoinPool.managedBlock(q);
                            } catch (InterruptedException ie) {
                                InterruptedException interruptedException = ie;
                                q.interrupted = true;
                            }
                            if (q.interrupted) {
                                break;
                            }
                        }
                        break;
                    }
                    queued = tryPushStack(q);
                } else {
                    q = new Signaller(true, nanos, deadline);
                }
                queued = queued2;
            }
            if (q != null) {
                q.thread = null;
            }
            if (r != null) {
                postComplete();
            } else {
                cleanStack();
            }
            if (r != null || (q != null && q.interrupted)) {
                return r;
            }
        }
        throw new TimeoutException();
    }

    CompletableFuture(Object r) {
        this.result = r;
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return asyncSupplyStage(ASYNC_POOL, supplier);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        return asyncSupplyStage(screenExecutor(executor), supplier);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return asyncRunStage(ASYNC_POOL, runnable);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return asyncRunStage(screenExecutor(executor), runnable);
    }

    public static <U> CompletableFuture<U> completedFuture(U value) {
        return new CompletableFuture(value == null ? NIL : value);
    }

    public boolean isDone() {
        return this.result != null;
    }

    public T get() throws InterruptedException, ExecutionException {
        Object obj = this.result;
        return reportGet(obj == null ? waitingGet(true) : obj);
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long nanos = unit.toNanos(timeout);
        Object obj = this.result;
        return reportGet(obj == null ? timedGet(nanos) : obj);
    }

    public T join() {
        Object obj = this.result;
        return reportJoin(obj == null ? waitingGet(false) : obj);
    }

    public T getNow(T valueIfAbsent) {
        Object obj = this.result;
        return obj == null ? valueIfAbsent : reportJoin(obj);
    }

    public boolean complete(T value) {
        boolean triggered = completeValue(value);
        postComplete();
        return triggered;
    }

    public boolean completeExceptionally(Throwable ex) {
        if (ex != null) {
            boolean triggered = internalComplete(new AltResult(ex));
            postComplete();
            return triggered;
        }
        throw new NullPointerException();
    }

    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return uniApplyStage(null, fn);
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return uniApplyStage(defaultExecutor(), fn);
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return uniApplyStage(screenExecutor(executor), fn);
    }

    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return uniAcceptStage(null, action);
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return uniAcceptStage(defaultExecutor(), action);
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return uniAcceptStage(screenExecutor(executor), action);
    }

    public CompletableFuture<Void> thenRun(Runnable action) {
        return uniRunStage(null, action);
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return uniRunStage(defaultExecutor(), action);
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return uniRunStage(screenExecutor(executor), action);
    }

    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return biApplyStage(null, other, fn);
    }

    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return biApplyStage(defaultExecutor(), other, fn);
    }

    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return biApplyStage(screenExecutor(executor), other, fn);
    }

    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(null, other, action);
    }

    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return biAcceptStage(defaultExecutor(), other, action);
    }

    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return biAcceptStage(screenExecutor(executor), other, action);
    }

    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return biRunStage(null, other, action);
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return biRunStage(defaultExecutor(), other, action);
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return biRunStage(screenExecutor(executor), other, action);
    }

    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return orApplyStage(null, other, fn);
    }

    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return orApplyStage(defaultExecutor(), other, fn);
    }

    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        return orApplyStage(screenExecutor(executor), other, fn);
    }

    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return orAcceptStage(null, other, action);
    }

    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return orAcceptStage(defaultExecutor(), other, action);
    }

    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        return orAcceptStage(screenExecutor(executor), other, action);
    }

    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return orRunStage(null, other, action);
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return orRunStage(defaultExecutor(), other, action);
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return orRunStage(screenExecutor(executor), other, action);
    }

    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return uniComposeStage(null, fn);
    }

    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return uniComposeStage(defaultExecutor(), fn);
    }

    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return uniComposeStage(screenExecutor(executor), fn);
    }

    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(null, action);
    }

    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return uniWhenCompleteStage(defaultExecutor(), action);
    }

    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return uniWhenCompleteStage(screenExecutor(executor), action);
    }

    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return uniHandleStage(null, fn);
    }

    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return uniHandleStage(defaultExecutor(), fn);
    }

    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return uniHandleStage(screenExecutor(executor), fn);
    }

    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return uniExceptionallyStage(fn);
    }

    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        return andTree(cfs, 0, cfs.length - 1);
    }

    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        return orTree(cfs, 0, cfs.length - 1);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = this.result == null && internalComplete(new AltResult(new CancellationException()));
        postComplete();
        if (cancelled || isCancelled()) {
            return true;
        }
        return false;
    }

    public boolean isCancelled() {
        Object obj = this.result;
        return (obj instanceof AltResult) && (((AltResult) obj).ex instanceof CancellationException);
    }

    public boolean isCompletedExceptionally() {
        AltResult altResult = this.result;
        return (altResult instanceof AltResult) && altResult != NIL;
    }

    public void obtrudeValue(T value) {
        this.result = value == null ? NIL : value;
        postComplete();
    }

    public void obtrudeException(Throwable ex) {
        if (ex != null) {
            this.result = new AltResult(ex);
            postComplete();
            return;
        }
        throw new NullPointerException();
    }

    public int getNumberOfDependents() {
        int count = 0;
        for (Completion p = this.stack; p != null; p = p.next) {
            count++;
        }
        return count;
    }

    public String toString() {
        String str;
        Object r = this.result;
        int count = 0;
        for (Completion p = this.stack; p != null; p = p.next) {
            count++;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        if (r == null) {
            if (count == 0) {
                str = "[Not completed]";
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[Not completed, ");
                stringBuilder2.append(count);
                stringBuilder2.append(" dependents]");
                str = stringBuilder2.toString();
            }
        } else if (!(r instanceof AltResult) || ((AltResult) r).ex == null) {
            str = "[Completed normally]";
        } else {
            str = "[Completed exceptionally]";
        }
        stringBuilder.append(str);
        return stringBuilder.toString();
    }

    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture();
    }

    public Executor defaultExecutor() {
        return ASYNC_POOL;
    }

    public CompletableFuture<T> copy() {
        return uniCopyStage();
    }

    public CompletionStage<T> minimalCompletionStage() {
        return uniAsMinimalStage();
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        if (supplier == null || executor == null) {
            throw new NullPointerException();
        }
        executor.execute(new AsyncSupply(this, supplier));
        return this;
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        return completeAsync(supplier, defaultExecutor());
    }

    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        if (unit != null) {
            if (this.result == null) {
                whenComplete(new Canceller(Delayer.delay(new Timeout(this), timeout, unit)));
            }
            return this;
        }
        throw new NullPointerException();
    }

    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
        if (unit != null) {
            if (this.result == null) {
                whenComplete(new Canceller(Delayer.delay(new DelayedCompleter(this, value), timeout, unit)));
            }
            return this;
        }
        throw new NullPointerException();
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
        if (unit != null && executor != null) {
            return new DelayedExecutor(delay, unit, executor);
        }
        throw new NullPointerException();
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        if (unit != null) {
            return new DelayedExecutor(delay, unit, ASYNC_POOL);
        }
        throw new NullPointerException();
    }

    public static <U> CompletionStage<U> completedStage(U value) {
        return new MinimalStage(value == null ? NIL : value);
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        if (ex != null) {
            return new CompletableFuture(new AltResult(ex));
        }
        throw new NullPointerException();
    }

    public static <U> CompletionStage<U> failedStage(Throwable ex) {
        if (ex != null) {
            return new MinimalStage(new AltResult(ex));
        }
        throw new NullPointerException();
    }
}
