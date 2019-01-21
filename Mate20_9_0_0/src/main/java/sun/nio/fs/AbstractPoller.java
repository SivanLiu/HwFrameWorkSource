package sun.nio.fs;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

abstract class AbstractPoller implements Runnable {
    private final LinkedList<Request> requestList = new LinkedList();
    private boolean shutdown = false;

    private static class Request {
        private boolean completed = false;
        private final Object[] params;
        private Object result = null;
        private final RequestType type;

        Request(RequestType type, Object... params) {
            this.type = type;
            this.params = params;
        }

        RequestType type() {
            return this.type;
        }

        Object[] parameters() {
            return this.params;
        }

        void release(Object result) {
            synchronized (this) {
                this.completed = true;
                this.result = result;
                notifyAll();
            }
        }

        Object awaitResult() {
            Object obj;
            boolean interrupted = false;
            synchronized (this) {
                while (!this.completed) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                obj = this.result;
            }
            return obj;
        }
    }

    private enum RequestType {
        REGISTER,
        CANCEL,
        CLOSE
    }

    abstract void implCancelKey(WatchKey watchKey);

    abstract void implCloseAll();

    abstract Object implRegister(Path path, Set<? extends Kind<?>> set, Modifier... modifierArr);

    abstract void wakeup() throws IOException;

    protected AbstractPoller() {
    }

    public void start() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                Thread thr = new Thread(this);
                thr.setDaemon(true);
                thr.start();
                return null;
            }
        });
    }

    final WatchKey register(Path dir, Kind<?>[] events, Modifier... modifiers) throws IOException {
        if (dir != null) {
            Set<Kind<?>> eventSet = new HashSet(events.length);
            for (Kind<?> event : events) {
                if (event == StandardWatchEventKinds.ENTRY_CREATE || event == StandardWatchEventKinds.ENTRY_MODIFY || event == StandardWatchEventKinds.ENTRY_DELETE) {
                    eventSet.add(event);
                } else if (event != StandardWatchEventKinds.OVERFLOW) {
                    if (event == null) {
                        throw new NullPointerException("An element in event set is 'null'");
                    }
                    throw new UnsupportedOperationException(event.name());
                }
            }
            if (eventSet.isEmpty()) {
                throw new IllegalArgumentException("No events to register");
            }
            return (WatchKey) invoke(RequestType.REGISTER, dir, eventSet, modifiers);
        }
        throw new NullPointerException();
    }

    final void cancel(WatchKey key) {
        try {
            invoke(RequestType.CANCEL, key);
        } catch (IOException x) {
            throw new AssertionError(x.getMessage());
        }
    }

    final void close() throws IOException {
        invoke(RequestType.CLOSE, new Object[0]);
    }

    private Object invoke(RequestType type, Object... params) throws IOException {
        Request req = new Request(type, params);
        synchronized (this.requestList) {
            if (this.shutdown) {
                throw new ClosedWatchServiceException();
            }
            this.requestList.add(req);
        }
        wakeup();
        Object result = req.awaitResult();
        if (result instanceof RuntimeException) {
            throw ((RuntimeException) result);
        } else if (!(result instanceof IOException)) {
            return result;
        } else {
            throw ((IOException) result);
        }
    }

    boolean processRequests() {
        synchronized (this.requestList) {
            while (true) {
                Request request = (Request) this.requestList.poll();
                Request req = request;
                if (request != null) {
                    if (this.shutdown) {
                        req.release(new ClosedWatchServiceException());
                    }
                    switch (req.type()) {
                        case REGISTER:
                            Object[] params = req.parameters();
                            req.release(implRegister(params[0], params[1], params[2]));
                            break;
                        case CANCEL:
                            implCancelKey(req.parameters()[0]);
                            req.release(null);
                            break;
                        case CLOSE:
                            implCloseAll();
                            req.release(null);
                            this.shutdown = true;
                            break;
                        default:
                            req.release(new IOException("request not recognized"));
                            break;
                    }
                }
            }
            while (true) {
            }
        }
        return this.shutdown;
    }
}
