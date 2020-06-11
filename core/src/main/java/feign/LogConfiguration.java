package feign;

import static feign.Util.valuesOrEmpty;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public class LogConfiguration {

  private final org.slf4j.event.Level level;
  private final boolean logRequest;
  private final boolean logResponse;
  private final org.slf4j.Logger logger;
  private final boolean relatedResponse;
  private final Map<String, Request> postponedRequests = new ConcurrentHashMap<>();
  private final RequestFormatter requestFormatter;
  private final ResponseFormatter responseFormatter;
  private final UnitedFormatter unitedFormatter;

  private void logWithLevel(String message) {
    switch (level) {
      case INFO: logger.info(message); break;
      case WARN: logger.warn(message); break;
      case DEBUG: logger.debug(message); break;
      case ERROR: logger.error(message); break;
      case TRACE: logger.trace(message); break;
    }
  }

  String logRequest(Supplier<Request> requestSupplier) {
    if (relatedResponse) {
      String key = UUID.randomUUID().toString();
      postponedRequests.put(key, requestSupplier.get());
      return key;
    } else if (logRequest) {
      logWithLevel(requestFormatter.format(requestSupplier.get()));
    }
    return null;
  }

  void logResponse(Supplier<Response> responseSupplier, long elapsedTime, String requestKey) {
    if (relatedResponse) {
      Request request = Optional.ofNullable(requestKey)
          .map(postponedRequests::get)
          .orElse(null);
      Response response = responseSupplier.get();
      logWithLevel(unitedFormatter.format(request, response, elapsedTime));
    } else if (logResponse) {
      logWithLevel(responseFormatter.format(responseSupplier.get(), elapsedTime));
    }
  }

  public static Builder forLogger(org.slf4j.Logger logger) {
    return new Builder(logger);
  }

  private LogConfiguration(Builder builder) {
    this.level = builder.level;
    this.logger = builder.logger;
    this.logRequest = builder.requestFormatter != null;
    this.logResponse = builder.responseFormatter != null;
    this.relatedResponse = builder.unitedFormatter != null;
    this.requestFormatter = builder.requestFormatter;
    this.responseFormatter = builder.responseFormatter;
    this.unitedFormatter = builder.unitedFormatter;
  }

  public static class Builder {

    private org.slf4j.event.Level level = Level.DEBUG;
    private org.slf4j.Logger logger;
    private RequestFormatter requestFormatter = null;
    private ResponseFormatter responseFormatter = null;
    private UnitedFormatter unitedFormatter = null;

    public Builder(Logger logger) {
      this.logger = logger;
    }

    public Builder withRequest(RequestFormatter requestFormatter) {
      this.requestFormatter = requestFormatter;
      return this;
    }

    public Builder withResponse(ResponseFormatter responseFormatter) {
      this.responseFormatter = responseFormatter;
      return this;
    }

    public Builder withRequestResponseUnited(UnitedFormatter unitedFormatter) {
      this.unitedFormatter = unitedFormatter;
      return this;
    }

    public Builder withLevel(org.slf4j.event.Level level) {
      this.level = level;
      return this;
    }

    public LogConfiguration build() {
      if (unitedFormatter != null && (requestFormatter != null || responseFormatter != null)) {
        throw new IllegalStateException("Only one way to log can be used: United or separated");
      }
      return new LogConfiguration(this);
    }

  }

  @FunctionalInterface
  public interface RequestFormatter {
    String format(Request request);
  }

  @FunctionalInterface
  public interface ResponseFormatter {
    String format(Response response, long elapsedTime);
  }

  @FunctionalInterface
  public interface UnitedFormatter {
    String format(Request request, Response response, long elapsedTime);
  }

  public static class DefaultUnitedFormatter implements  UnitedFormatter {

    private final RequestFormatter requestFormatter;
    private final ResponseFormatter responseFormatter;

    public DefaultUnitedFormatter(RequestFormatter requestFormatter,
        ResponseFormatter responseFormatter) {
      this.requestFormatter = requestFormatter;
      this.responseFormatter = responseFormatter;
    }

    @Override
    public String format(Request request, Response response, long elapsedTime) {
      return requestFormatter.format(request)
          + "\n"
          + responseFormatter.format(response, elapsedTime);
    }
  }

  public static class DefaultRequestFormatter implements RequestFormatter {

    private final boolean printHeaders;
    private final boolean printBody;

    private DefaultRequestFormatter(Builder builder) {
      this.printHeaders = builder.printHeaders;
      this.printBody = builder.printBody;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private boolean printHeaders = false;
      private boolean printBody = false;

      public Builder withHeaders() {
        this.printHeaders = true;
        return this;
      }

      public Builder withBody() {
        this.printBody = true;
        return this;
      }

      public DefaultRequestFormatter build() {
        return new DefaultRequestFormatter(this);
      }

    }

    @Override
    public String format(Request request) {
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("\n---> %s %s HTTP/1.1", request.httpMethod().name(), request.url()));
      if (printHeaders) {
        sb.append("\n");
        addHeaders(request.headers(), sb);
      }
      if (printBody) {
        sb.append("\n");
        addBody(request, sb);
      }
      return sb.toString();
    }

    private static void addBody(Request request, StringBuilder sb) {
      int bodyLength = 0;
      if (request.body() != null) {
        bodyLength = request.length();
        String bodyText = request.charset() != null
            ? new String(request.body(), request.charset())
            : null;
        sb
            .append("\n")
            .append(String.format("%s", bodyText != null ? bodyText : "Binary data"));
      }
      sb.append(String.format("\n---> END HTTP (%s-byte body)", bodyLength));
    }

  }

  private static void addHeaders(Map<String, Collection<String>> headers, StringBuilder sb) {
    for (String field : headers.keySet()) {
      for (String value : valuesOrEmpty(headers, field)) {
        sb.append(String.format("%s: %s\n", field, value));
      }
    }
  }

  public static class DefaultResponseFormatter implements ResponseFormatter {

    @Override
    public String format(Response response, long elapsedTime) {
      StringBuilder sb = new StringBuilder();
      String reason = response.reason() != null
          ? " " + response.reason()
          : "";
      sb.append(String.format("\n<--- HTTP/1.1 %s%s (%sms)", response.status(), reason, elapsedTime));
      addHeaders(response.headers(), sb);
      addBody(response, sb);
      return sb.toString();
    }

    private static void addBody(Response response, StringBuilder sb) {
      int bodyLength = 0;
      int status = response.status();
      if (response.body() != null && !(status == 204 || status == 205)) {
        // HTTP 204 No Content "...response MUST NOT include a message-body"
        // HTTP 205 Reset Content "...response MUST NOT include an entity"
        sb.append("\n");
        String body = response.body().toString();
        sb.append(String.format("%s", body));
        sb.append(String.format("\n<--- END HTTP (%s-byte body)", bodyLength));
      } else {
        sb.append(String.format("<--- END HTTP (%s-byte body)", bodyLength));
      }
    }

    private final boolean printHeaders;
    private final boolean printBody;

    private DefaultResponseFormatter(Builder builder) {
      this.printHeaders = builder.printHeaders;
      this.printBody = builder.printBody;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private boolean printHeaders = false;
      private boolean printBody = false;

      public Builder withHeaders() {
        this.printHeaders = true;
        return this;
      }

      public Builder withBody() {
        this.printBody = true;
        return this;
      }

      public DefaultResponseFormatter build() {
        return new DefaultResponseFormatter(this);
      }

    }

  }

}
