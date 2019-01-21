package java.util.concurrent;

public class ExecutorCompletionService<V> implements CompletionService<V> {
    private final AbstractExecutorService aes;
    private final BlockingQueue<Future<V>> completionQueue;
    private final Executor executor;

    private static class QueueingFuture<V> extends FutureTask<Void> {
        private final BlockingQueue<Future<V>> completionQueue;
        private final Future<V> task;

        QueueingFuture(RunnableFuture<V> task, BlockingQueue<Future<V>> completionQueue) {
            super(task, null);
            this.task = task;
            this.completionQueue = completionQueue;
        }

        protected void done() {
            this.completionQueue.add(this.task);
        }
    }

    private RunnableFuture<V> newTaskFor(Callable<V> task) {
        if (this.aes == null) {
            return new FutureTask(task);
        }
        return this.aes.newTaskFor(task);
    }

    private RunnableFuture<V> newTaskFor(Runnable task, V result) {
        if (this.aes == null) {
            return new FutureTask(task, result);
        }
        return this.aes.newTaskFor(task, result);
    }

    public ExecutorCompletionService(Executor executor) {
        if (executor != null) {
            this.executor = executor;
            this.aes = executor instanceof AbstractExecutorService ? (AbstractExecutorService) executor : null;
            this.completionQueue = new LinkedBlockingQueue();
            return;
        }
        throw new NullPointerException();
    }

    public ExecutorCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue) {
        if (executor == null || completionQueue == null) {
            throw new NullPointerException();
        }
        this.executor = executor;
        this.aes = executor instanceof AbstractExecutorService ? (AbstractExecutorService) executor : null;
        this.completionQueue = completionQueue;
    }

    public Future<V> submit(Callable<V> task) {
        if (task != null) {
            RunnableFuture<V> f = newTaskFor(task);
            this.executor.execute(new QueueingFuture(f, this.completionQueue));
            return f;
        }
        throw new NullPointerException();
    }

    public Future<V> submit(Runnable task, V result) {
        if (task != null) {
            RunnableFuture<V> f = newTaskFor(task, result);
            this.executor.execute(new QueueingFuture(f, this.completionQueue));
            return f;
        }
        throw new NullPointerException();
    }

    public Future<V> take() throws InterruptedException {
        return (Future) this.completionQueue.take();
    }

    public Future<V> poll() {
        return (Future) this.completionQueue.poll();
    }

    public Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException {
        return (Future) this.completionQueue.poll(timeout, unit);
    }
}
