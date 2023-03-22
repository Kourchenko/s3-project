package com.sample.demo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Handler value: example.Handler
public class Handler implements RequestHandler<S3Event, String> {
  private static final Logger logger = LoggerFactory.getLogger(Handler.class);

  private static final float MAX_DIMENSION = 100;
  private final String REGEX = ".*\\.([^\\.]*)";
  private final String JPG_TYPE = "jpg";
  private final String JPG_MIME = "image/jpeg";
  private final String PNG_TYPE = "png";
  private final String PNG_MIME = "image/png";

  private final S3Client s3Client;

  public Handler() {
      // Initialize the SDK client outside of the handler method so that it can be reused for subsequent invocations.
      // It is initialized when the class is loaded.
      s3Client = DependencyFactory.s3Client();
      // Consider invoking a simple api here to pre-warm up the application, eg: dynamodb#listTables
      s3Client.listBuckets();
  }

    @Override
    public String handleRequest(S3Event s3event, Context context) {
        try {
            S3EventNotificationRecord record = s3event.getRecords().get(0);

            String srcBucket = record.getS3().getBucket().getName();

            // Object key may have spaces or unicode non-ASCII characters.
            String srcKey = record.getS3().getObject().getUrlDecodedKey();

            // Infer the image type.
            Matcher matcher = Pattern.compile(REGEX).matcher(srcKey);
            if (!matcher.matches()) {
                logger.info("Unable to infer image type for key " + srcKey);
                return "";
            }

            String imageType = matcher.group(1);
                if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
                logger.info("Skipping non-image " + srcKey);
                return "";
            }

            // ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // InputStream s3Object = getObject(s3Client, srcBucket, srcKey);

            // Upload new image to S3
            // putObject(s3Client, outputStream, srcBucket, "resized/" + srcKey, imageType);

            return formatS3ObjectUrl(srcBucket, srcKey, "us-west-2");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getObject(S3Client s3Client, String bucket, String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build();
        return s3Client.getObject(getObjectRequest);
      }

    private void putObject(S3Client s3Client, ByteArrayOutputStream outputStream, String bucket, String key, String imageType) {
        Map<String, String> metadata = new HashMap<>();
            metadata.put("Content-Length", Integer.toString(outputStream.size()));
        if (JPG_TYPE.equals(imageType)) {
            metadata.put("Content-Type", JPG_MIME);
        } else if (PNG_TYPE.equals(imageType)) {
            metadata.put("Content-Type", PNG_MIME);
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .metadata(metadata)
            .build();

        // Uploading to S3 destination bucket
        logger.info("Writing to: " + bucket + "/" + key);
        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(outputStream.toByteArray()));
        }
            catch(AwsServiceException e)
        {
            logger.error(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public String formatS3ObjectUrl(String region, String srcBucket, String srcKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://").append(srcBucket).append(".s3.").append(region).append(".amazonaws.com/").append(srcKey);
        return sb.toString();
    }
}