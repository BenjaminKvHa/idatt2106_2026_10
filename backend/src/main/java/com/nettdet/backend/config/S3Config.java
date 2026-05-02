package com.nettdet.backend.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * {@code S3Config} provides the Spring configuration for the Amazon S3 client.
 * <p>
 * This class establishes the connection parameters for the object storage service used
 * by the application. It is designed to be flexible, supporting both production AWS
 * environments and local development environments (e.g., LocalStack or MinIO) through
 * endpoint overrides and path-style access.
 * </p>
 */
@Configuration
public class S3Config {

    /**
     * Creates and configures an {@link S3Client} bean.
     * <p>
     * The client is configured with:
     * <ul>
     *     <li>An endpoint override for custom storage providers.</li>
     *     <li>A specific region for resource locality.</li>
     *     <li>Path-style access enabled (required for many S3-compatible local services).</li>
     *     <li>Static credentials provider for explicit access/secret key authentication.</li>
     * </ul>
     * </p>
     *
     * @param endpoint the URI of the S3 service (e.g., LocalStack URL or AWS endpoint)
     * @param accessKey the access key for authentication
     * @param secretKey the secret key for authentication
     * @param region the AWS region to use (e.g., "eu-north-1")
     * @return a configured {@link S3Client} instance
     */
    @Bean
    public S3Client s3Client(
            @Value("${s3.endpoint}") String endpoint,
            @Value("${s3.access-key}") String accessKey,
            @Value("${s3.secret-key}") String secretKey,
            @Value("${s3.region}") String region) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
