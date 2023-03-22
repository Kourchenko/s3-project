
package com.sample.demo;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * The module containing all dependencies required by the {@link App}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of S3Client
     */
    public static S3Client s3Client() {
        return S3Client.builder()
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                       .httpClientBuilder(UrlConnectionHttpClient.builder())
                       .build();
    }
}
