package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractExecutorService implements ExecutorService {
    static final /* synthetic */ boolean $assertionsDisabled = false;

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask(callable);
    }

    public Future<?> submit(Runnable task) {
        if (task != null) {
            RunnableFuture<Void> ftask = newTaskFor(task, null);
            execute(ftask);
            return ftask;
        }
        throw new NullPointerException();
    }

    public <T> Future<T> submit(Runnable task, T result) {
        if (task != null) {
            RunnableFuture<T> ftask = newTaskFor(task, result);
            execute(ftask);
            return ftask;
        }
        throw new NullPointerException();
    }

    public <T> Future<T> submit(Callable<T> task) {
        if (task != null) {
            RunnableFuture<T> ftask = newTaskFor(task);
            execute(ftask);
            return ftask;
        }
        throw new NullPointerException();
    }

    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos) throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks != null) {
            int ntasks = tasks.size();
            if (ntasks != 0) {
                long deadline;
                ArrayList<Future<T>> futures = new ArrayList(ntasks);
                ExecutorCompletionService<T> ecs = new ExecutorCompletionService(this);
                ExecutionException ee = null;
                if (timed) {
                    try {
                        deadline = System.nanoTime() + nanos;
                    } catch (ExecutionException eex) {
                        ee = eex;
                    } catch (RuntimeException rex) {
                        ee = new ExecutionException(rex);
                    } catch (Throwable th) {
                        cancelAll(futures);
                    }
                } else {
                    deadline = 0;
                }
                Iterator<? extends Callable<T>> it = tasks.iterator();
                futures.add(ecs.submit((Callable) it.next()));
                ntasks--;
                int active = 1;
                while (true) {
                    Future<T> f = ecs.poll();
                    if (f == null) {
                        if (ntasks > 0) {
                            ntasks--;
                            futures.add(ecs.submit((Callable) it.next()));
                            active++;
                        } else if (active == 0) {
                            if (ee == null) {
                                ee = new ExecutionException();
                            }
                            throw ee;
                        } else if (timed) {
                            f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                            if (f != null) {
                                nanos = deadline - System.nanoTime();
                            } else {
                                throw new TimeoutException();
                            }
                        } else {
                            f = ecs.take();
                        }
                    }
                    if (f != null) {
                        active--;
                        Object obj = f.get();
                        cancelAll(futures);
                        return obj;
                    }
                }
            } else {
                throw new IllegalArgumentException();
            }
        }
        throw new NullPointerException();
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        try {
            return doInvokeAny(tasks, false, 0);
        } catch (TimeoutException e) {
            return null;
        }
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return doInvokeAny(tasks, true, unit.toNanos(timeout));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (tasks != null) {
            ArrayList<Future<T>> futures = new ArrayList(tasks.size());
            try {
                for (Callable<T> t : tasks) {
                    RunnableFuture<T> f = newTaskFor(t);
                    futures.add(f);
                    execute(f);
                }
                int size = futures.size();
                for (int i = 0; i < size; i++) {
                    Future<T> f2 = (Future) futures.get(i);
                    if (!f2.isDone()) {
                        try {
                            f2.get();
                        } catch (CancellationException | ExecutionException e) {
                        }
                    }
                }
                return futures;
            } catch (Throwable th) {
                cancelAll(futures);
            }
        } else {
            throw new NullPointerException();
        }
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        if (tasks != null) {
            long nanos = unit.toNanos(timeout);
            long deadline = System.nanoTime() + nanos;
            ArrayList<Future<T>> futures = new ArrayList(tasks.size());
            int i = 0;
            int j = 0;
            try {
                for (Callable<T> t : tasks) {
                    futures.add(newTaskFor(t));
                }
                int size = futures.size();
                while (i < size) {
                    if ((i == 0 ? nanos : deadline - System.nanoTime()) <= 0) {
                        cancelAll(futures, j);
                        return futures;
                    }
                    execute((Runnable) futures.get(i));
                    i++;
                }
                while (j < size) {
                    Future<T> f = (Future) futures.get(j);
                    if (!f.isDone()) {
                        try {
                            f.get(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                        } catch (CancellationException | ExecutionException e) {
                        } catch (TimeoutException e2) {
                        }
                    }
                    j++;
                }
                return futures;
            } catch (Throwable th) {
                cancelAll(futures);
            }
        } else {
            long j2 = timeout;
            TimeUnit timeUnit = unit;
            throw new NullPointerException();
        }
    }

    private static <T> void cancelAll(ArrayList<Future<T>> futures) {
        cancelAll(futures, 0);
    }

    private static <T> void cancelAll(ArrayList<Future<T>> futures, int j) {
        int size = futures.size();
        while (j < size) {
            ((Future) futures.get(j)).cancel(true);
            j++;
        }
    }
}
