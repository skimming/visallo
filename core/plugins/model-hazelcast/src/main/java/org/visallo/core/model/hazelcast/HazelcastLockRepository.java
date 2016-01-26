package org.visallo.core.model.hazelcast;

import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.ILock;
import org.visallo.core.concurrent.ThreadRepository;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.lock.LeaderListener;
import org.visallo.core.model.lock.Lock;
import org.visallo.core.model.lock.LockRepository;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

import java.util.concurrent.Callable;

public class HazelcastLockRepository extends LockRepository {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(HazelcastLockRepository.class);
    private final HazelcastRepository hazelcastRepository;
    private final ThreadRepository threadRepository;

    @Inject
    public HazelcastLockRepository(HazelcastRepository hazelcastRepository, ThreadRepository threadRepository) {
        this.hazelcastRepository = hazelcastRepository;
        this.threadRepository = threadRepository;
    }

    @Override
    public Lock createLock(String lockName) {
        return new Lock(lockName) {
            @Override
            public <T> T run(Callable<T> callable) {
                ILock lock = hazelcastRepository.getHazelcastInstance().getLock(getLockName());
                lock.lock();
                try {
                    return callable.call();
                } catch (Exception ex) {
                    throw new VisalloException("Failed to run in lock", ex);
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    @Override
    public void leaderElection(String lockName, final LeaderListener listener) {
        final ILock lock = hazelcastRepository.getHazelcastInstance().getLock(lockName);
        threadRepository.startDaemon(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        try {
                            if (lock.isLockedByCurrentThread()) {
                                Thread.sleep(1000);
                                continue;
                            }
                            lock.lock();
                            listener.isLeader();
                        } catch (HazelcastInstanceNotActiveException exInner) {
                            handleHazelcastInstanceNotActiveException();
                        } catch (InterruptedException ex) {
                            throw ex;
                        } catch (Throwable ex) {
                            LOGGER.error("Could not elect leader", ex);
                            try {
                                lock.unlock();
                            } catch (HazelcastInstanceNotActiveException exInner) {
                                handleHazelcastInstanceNotActiveException();
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    // exiting
                }
            }

            private void handleHazelcastInstanceNotActiveException()
                    throws InterruptedException{
                LOGGER.debug("Hazelcast is already shutdown and will release the lock for us");
                Thread.sleep(1000);
            }
        }, HazelcastLockRepository.class.getSimpleName() + "-LeaderElection-" + lockName);
    }
}
