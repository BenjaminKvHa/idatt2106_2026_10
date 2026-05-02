package com.nettdet.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * {@code S3BucketInitializer} is a Spring Boot {@link ApplicationRunner} responsible for
 * ensuring that the primary S3 storage bucket is provisioned before the rest of the
 * application starts.
 * <p>
 * This component is marked with {@code @Order(0)}, making it the first runner to execute.
 * This priority is essential because subsequent seeders (e.g., {@link AvatarAssetInitializer})
 * depend on the existence of the bucket to upload initial assets.
 * </p>
 * <p>
 * The initialization is idempotent: it first attempts to "head" the bucket to check for
 * its existence. If missing (or if a 404 status is returned), it attempts to create the
 * bucket. It also handles edge cases where the bucket might be created concurrently by
 * another process (e.g., in a clustered environment) by catching the
 * {@code BucketAlreadyOwnedByYou} error code.
 * </p>
 */
@Component
@Order(0)
public class S3BucketInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(S3BucketInitializer.class);

    private final S3Client s3Client;
    private final String bucket;

    /**
     * Constructs a new {@code S3BucketInitializer} with the required S3 client and bucket name.
     *
     * @param s3Client the AWS SDK S3 client used to interact with the storage service
     * @param bucket the name of the bucket to initialize, injected from {@code s3.bucket} property
     */
    public S3BucketInitializer(
            S3Client s3Client,
            @Value("${s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    /**
     * Verifies the existence of the configured S3 bucket and creates it if it does not exist.
     *
     * @param args incoming application arguments (unused)
     * @throws S3Exception if an unexpected error occurs during bucket inspection or creation
     */
    @Override
    public void run(ApplicationArguments args) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            logger.info("S3 bucket '{}' already exists", bucket);
            return;
        } catch (NoSuchBucketException ignored) {
            // Create the bucket below.
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
        }

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            logger.info("Created S3 bucket '{}'", bucket);
        } catch (S3Exception exception) {
            String errorCode = exception.awsErrorDetails() != null
                    ? exception.awsErrorDetails().errorCode()
                    : null;
            if (!"BucketAlreadyOwnedByYou".equals(errorCode)) {
                throw exception;
            }
            logger.info("S3 bucket '{}' already exists for this account", bucket);
        }
    }
}
