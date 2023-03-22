package com.sample.demo;

import org.junit.jupiter.api.Test;
import com.amazonaws.services.lambda.runtime.events.S3Event;

public class HandlerTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        Handler function = new Handler();
        S3Event s3Event = new S3Event();
        function.handleRequest(s3Event, null);
    }
}
