package org.visallo.core.model.lock;

import com.google.inject.Inject;
import org.visallo.core.concurrent.ThreadRepository;

import java.util.concurrent.Callable;

public class NonLockingLockRepository extends LockRepository {

    private final ThreadRepository threadRepository;

    @Inject
    public NonLockingLockRepository(ThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }

    @Override
    public Lock createLock(String lockName) {
        return new Lock(lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                try {
                    return callable.call();
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to run in lock", ex);
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderListener listener) {
        threadRepository.startDaemon(new Runnable() {
            @Override
            public void run() {
                listener.isLeader();
            }
        }, NonLockingLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
    }
}
