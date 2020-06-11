package feign.logger;

import feign.Request;
import feign.Response;
import java.io.IOException;

public interface FeignLogger {

  String logRequest(Request request);

  void logResponse(String requestKey, Response response, long elapsedTime);

  void logRetry(String requestKey);

  void logIOException(String requestKey, IOException ioe, long elapsedTime);

}
