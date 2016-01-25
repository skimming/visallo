package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.concurrent.ThreadRepository;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.longRunningProcess.LongRunningProcessRunner;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class LongRunningProcessRunnerInitializer extends ApplicationBootstrapInitializer {
    public static final String CONFIG_THREAD_COUNT = LongRunningProcessRunnerInitializer.class.getName() + ".threadCount";
    public static final int DEFAULT_THREAD_COUNT = 1;
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(LongRunningProcessRunnerInitializer.class);
    private final Configuration config;
    private final ThreadRepository threadRepository;

    @Inject
    public LongRunningProcessRunnerInitializer(Configuration config, ThreadRepository threadRepository) {
        this.config = config;
        this.threadRepository = threadRepository;
    }

    @Override
    public void initialize() {
        LOGGER.debug("setupLongRunningProcessRunner");

        int threadCount = config.getInt(CONFIG_THREAD_COUNT, DEFAULT_THREAD_COUNT);

        LOGGER.debug("long running process runners: %d", threadCount);
        for (int i = 0; i < threadCount; i++) {
            threadRepository.startDaemon(new Runnable() {
                @Override
                public void run() {
                    delayStart();
                    final LongRunningProcessRunner longRunningProcessRunner = InjectHelper.getInstance(LongRunningProcessRunner.class);
                    longRunningProcessRunner.prepare(config.toMap());
                    try {
                        longRunningProcessRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running long running process runner", ex);
                    }
                }
            }, "long-running-process-runner-" + i);
        }
    }
}
