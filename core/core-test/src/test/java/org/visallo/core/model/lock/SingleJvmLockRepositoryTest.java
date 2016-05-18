package org.visallo.core.model.lock;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SingleJvmLockRepositoryTest extends LockRepositoryTestBase {
    private LockRepository lockRepository = new SingleJvmLockRepository();

    @Test
    public void testCreateLock() throws Exception {
        super.testCreateLock(lockRepository);
    }

    @Test
    public void testLeaderElection() throws Exception {
        List<String> messages = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderOne", i, messages));
        }
        for (int i = 2; i < 5; i++) {
            threads.add(createLeaderElectingThread(lockRepository, "leaderTwo", i, messages));
        }
        for (Thread t : threads) {
            t.start();
        }
        Thread.sleep(1000);
        assertEquals(2, messages.size());
    }
}