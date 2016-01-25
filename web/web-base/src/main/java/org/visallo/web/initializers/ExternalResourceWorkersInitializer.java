package org.visallo.web.initializers;

import com.google.inject.Inject;
import org.visallo.core.concurrent.ThreadRepository;
import org.visallo.core.config.Configuration;
import org.visallo.core.externalResource.ExternalResourceRunner;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.status.StatusRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;

public class ExternalResourceWorkersInitializer extends ApplicationBootstrapInitializer {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(ExternalResourceWorkersInitializer.class);
    private final Configuration config;
    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private final ThreadRepository threadRepository;

    @Inject
    public ExternalResourceWorkersInitializer(
            Configuration config,
            UserRepository userRepository,
            StatusRepository statusRepository,
            ThreadRepository threadRepository
    ) {
        this.config = config;
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.threadRepository = threadRepository;
    }

    @Override
    public void initialize() {
        LOGGER.debug("setupExternalResourceWorkers");

        final User user = userRepository.getSystemUser();
        new ExternalResourceRunner(config, statusRepository, threadRepository, user).startAll();
    }
}
