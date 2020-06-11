package feign.logger;

import feign.Request;
import feign.Response;
import java.io.IOException;

public class DefaultFeignLogger extends AbstractFeignLogger implements FeignLogger {

  DefaultFeignLogger(LoggerConfiguration configuration) {
    super(configuration);
  }

  @Override
  public String logRequest(Request request) {
    if (configuration.isLogRequest()) {
      logWithLevel(configuration.getRequestFormatter().format(request));
    }
    return null;
  }

  @Override
  public void logResponse(String requestKey, Response response, long elapsedTime) {
    if (configuration.isLogResponse()) {
      logWithLevel(configuration.getResponseFormatter().format(response, elapsedTime));
    }
  }

  @Override
  public void logRetry(String requestKey) {
    if (configuration.isLogRequest()) {
      logWithLevel("---> RETRYING");
    }
  }

  @Override
  public void logIOException(String requestKey, IOException ioe, long elapsedTime) {
    // TODO: 11.06.2020 Add IOException logging
  }
}
