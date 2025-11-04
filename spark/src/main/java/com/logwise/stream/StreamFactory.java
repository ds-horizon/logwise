package com.logwise.stream;

import com.logwise.constants.StreamName;
import com.logwise.guice.injectors.ApplicationInjector;
import com.logwise.stream.impl.ApplicationLogsStreamToS3;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StreamFactory {
  public Stream getStream(StreamName streamName) {
    switch (streamName) {
      case APPLICATION_LOGS_STREAM_TO_S3:
        return ApplicationInjector.getInstance(ApplicationLogsStreamToS3.class);
      default:
        throw new IllegalArgumentException("Invalid stream name: " + streamName);
    }
  }
}
