package org.jahia.bundles.cache.client.cloudfront;

import org.jahia.bundles.cache.client.api.ClientCacheInvalidationProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResponse;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.Paths;

import java.util.List;

/**
 * @author Jerome Blanchard
 */
@Component(service = {CloudFrontClientService.class, ClientCacheInvalidationProvider.class}, configurationPid = "org.jahia.bundles.cache.client.cloudfront", immediate = true)
@Designate(ocd = CloudFrontClientService.Config.class)
public class CloudFrontClientService implements ClientCacheInvalidationProvider {

    public static final String NAME = "cloudfront";
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFrontClientService.class);

    @ObjectClassDefinition( name = "org.jahia.bundles.cache.client.cloudfront")
    public @interface Config {

        @AttributeDefinition(name = "distribution.id", description = "The AWS ditribution ID")
        String id_distribution() default "0";

        @AttributeDefinition(name = "key.access", description = "The AWS Access Key")
        String access_key() default "";

        @AttributeDefinition(name = "key.secret", description = "The AWS Secret Key")
        String secret_key() default "";

    }

    private CloudFrontClient client;
    private String distributionId;
    private AwsBasicCredentials awsCreds;

    @Activate
    @Modified
    public void setup(Config config) {
        LOGGER.info("Activate/Update CloudFront Client Service...");
        this.distributionId = config.id_distribution();
        this.awsCreds = AwsBasicCredentials.create(config.access_key(), config.secret_key());
        this.client = CloudFrontClient.builder().region(Region.AWS_GLOBAL) // CloudFront is global
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    @Deactivate
    public void teardown() {
        LOGGER.debug("Deactivate CloudFront Client Service...");
        this.distributionId = "";
        this.awsCreds = null;
        if (client != null) {
            client.close();
        }
    }

    @Override public String getName() {
        return NAME;
    }

    @Override
    public void purge() throws CloudFrontClientServiceException {
        this.internalInvalidate(List.of("/*"));
    }

    @Override public void invalidate(String path) throws CloudFrontClientServiceException {
        this.internalInvalidate(List.of(path));
    }

    @Override public void invalidate(String[] paths) throws CloudFrontClientServiceException {
        this.internalInvalidate(List.of(paths));
    }

    private void internalInvalidate(List<String> paths) throws CloudFrontClientServiceException {
        Paths invalidationPaths = Paths.builder().items(paths).quantity(1).build();
        InvalidationBatch invalidationBatch = InvalidationBatch.builder().paths(invalidationPaths).callerReference("jahia").build();
        CreateInvalidationRequest createInvalidationRequest = CreateInvalidationRequest.builder()
                .distributionId(distributionId)
                .invalidationBatch(invalidationBatch)
                .build();
        CreateInvalidationResponse response = client.createInvalidation(createInvalidationRequest);
        if (response.sdkHttpResponse().isSuccessful()) {
            String id = response.invalidation().id();
            LOGGER.debug("Invalidation request sent successfully with id: {}", id);
        } else {
            LOGGER.error("Failed to send invalidation request: {}", response.sdkHttpResponse().statusText());
            throw new CloudFrontClientServiceException("Failed to send invalidation request: " + response.sdkHttpResponse().statusText());
        }
    }

}
