package org.visallo.core.model.lock;

import com.google.inject.Inject;
import org.visallo.core.concurrent.ThreadRepository;
import org.visallo.core.exception.VisalloException;

import java.util.concurrent.Callable;

public class SingleJvmLockRepository extends LockRepository {

    private final ThreadRepository threadRepository;

    @Inject
    public SingleJvmLockRepository(ThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }

    @Override
    public Lock createLock(String lockName) {
        final Object synchronizationObject = getSynchronizationObject(lockName);
        return new Lock(lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                try {
                    synchronized (synchronizationObject) {
                        return callable.call();
                    }
                } catch (Exception ex) {
                    throw new VisalloException("Failed to run in lock", ex);
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderListener listener) {
        final Object synchronizationObject = getSynchronizationObject(lockName);
        threadRepository.startDaemon(new Runnable() {
            @Override
            public void run() {
                synchronized (synchronizationObject) {
                    listener.isLeader();
                }
            }
        }, SingleJvmLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
    }

    @Override
    public void shutdown() {
        // no implementation required
    }
}
