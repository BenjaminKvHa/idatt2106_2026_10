package com.nettdet.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @deprecated Task seeding is now handled by {@link TaskSeeder}.
 * This class is kept for backward compatibility but does nothing.
 * TaskSeeder handles both file uploads to MinIO and task creation.
 */
@Component
@Order(2)
@Deprecated
public class TaskAssetInitializor implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssetInitializor.class);

    @Override
    public void run(ApplicationArguments args) {
        // Task seeding has been moved to TaskSeeder.java
        // This class is kept for backward compatibility
        logger.debug("TaskAssetInitializor is deprecated. Task seeding is handled by TaskSeeder.");
    }
}
