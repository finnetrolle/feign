package feign.logger;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class LoggerConfiguration {

  private final Level level;
  private final Logger logger;
  private final boolean logRequest;
  private final boolean logResponse;
  private final RequestFormatter requestFormatter;
  private final ResponseFormatter responseFormatter;

  public LoggerConfiguration(Level level, Logger logger, boolean logRequest, boolean logResponse,
      RequestFormatter requestFormatter, ResponseFormatter responseFormatter) {
    this.level = level;
    this.logger = logger;
    this.logRequest = logRequest;
    this.logResponse = logResponse;
    this.requestFormatter = requestFormatter;
    this.responseFormatter = responseFormatter;
  }

  public static Builder forLogger(Logger logger) {
    return new Builder(logger);
  }

  public static Builder build() {
    return new Builder(LoggerFactory.getLogger("FeignLogger"));
  }

  public Logger getLogger() {
    return logger;
  }

  public Level getLevel() {
    return level;
  }

  boolean isLogRequest() {
    return logRequest;
  }

  boolean isLogResponse() {
    return logResponse;
  }

  RequestFormatter getRequestFormatter() {
    return requestFormatter;
  }

  ResponseFormatter getResponseFormatter() {
    return responseFormatter;
  }

  public static class Builder {
    private Level level = Level.DEBUG;
    private final Logger logger;
    private RequestFormatter requestFormatter = null;
    private ResponseFormatter responseFormatter = null;
    private boolean unitedRequestAndResponse = false;
    private boolean logRequest = false;
    private boolean logResponse = false;

    public Builder(Logger logger) {
      this.logger = logger;
    }

    public Builder uniteRequestAndResponse() {
      this.unitedRequestAndResponse = true;
      return this;
    }

    public Builder withRequest() {
      this.logRequest = true;
      return this;
    }

    public Builder withRequest(RequestFormatter requestFormatter) {
      this.requestFormatter = requestFormatter;
      return withRequest();
    }

    public Builder withResponse() {
      this.logResponse = true;
      return this;
    }

    public Builder withResponse(ResponseFormatter responseFormatter) {
      this.logResponse = true;
      return withResponse();
    }

    public Builder overrideLevel(Level level) {
      this.level = level;
      return this;
    }

    public FeignLogger build() {
      LoggerConfiguration loggerConfiguration = new LoggerConfiguration(
          level, logger, logRequest, logResponse,
          Optional.ofNullable(requestFormatter).orElse(DefaultRequestFormatter.DEFAULT),
          Optional.ofNullable(responseFormatter).orElse(DefaultResponseFormatter.DEFAULT));
      if (unitedRequestAndResponse) {
        return new UnitedFeignLogger(loggerConfiguration);
      } else {
        return new DefaultFeignLogger(loggerConfiguration);
      }
    }

  }



}
