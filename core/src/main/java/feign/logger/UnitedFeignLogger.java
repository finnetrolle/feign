package feign.logger;

import feign.Request;
import feign.Response;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UnitedFeignLogger extends AbstractFeignLogger implements FeignLogger {

  private final ConcurrentHashMap<String, Request> savedRequests = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicInteger> retryMap = new ConcurrentHashMap<>();

  protected UnitedFeignLogger(LoggerConfiguration configuration) {
    super(configuration);
  }

  @Override
  public String logRequest(Request request) {
    String key = UUID.randomUUID().toString();
    savedRequests.put(key, request);
    return key;
  }

  @Override
  public void logResponse(String requestKey, Response response, long elapsedTime) {
    logRequest(requestKey);
    logWithLevel(configuration.getResponseFormatter().format(response, elapsedTime));
  }

  @Override
  public void logRetry(String requestKey) {
    retryMap.putIfAbsent(requestKey, new AtomicInteger());
    retryMap.get(requestKey).incrementAndGet();
  }

  @Override
  public void logIOException(String requestKey, IOException ioe, long elapsedTime) {
    logRequest(requestKey);
    // TODO: 11.06.2020 ADD IOException logging here
  }

  private void logRequest(String requestKey) {
    Request request = savedRequests.get(requestKey);
    if (request != null) {
      logWithLevel(configuration.getRequestFormatter().format(request));
      savedRequests.remove(requestKey);
    }
    AtomicInteger retries = retryMap.get(requestKey);
    if (retries != null) {
      logWithLevel(String.format("---> RETRY ATTEMPTS AMOUNT: %d", retries.get()));
    }
  }


}
