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

@Component
@Order(0)
public class S3BucketInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(S3BucketInitializer.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3BucketInitializer(
            S3Client s3Client,
            @Value("${s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

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
