package feign.logger;


import static feign.Util.valuesOrEmpty;

import feign.Response;
import java.util.Collection;
import java.util.Map;

public class DefaultResponseFormatter implements ResponseFormatter {

  public static final DefaultResponseFormatter DEFAULT = DefaultResponseFormatter.builder().build();

  @Override
  public String format(Response response, long elapsedTime) {
    StringBuilder sb = new StringBuilder();
    String reason = response.reason() != null
        ? " " + response.reason()
        : "";
    sb.append(String.format("\n<--- HTTP/1.1 %s%s (%sms)", response.status(), reason, elapsedTime));
    if (printHeaders) {
      sb.append("\n");
      addHeaders(response.headers(), sb);
    }
    if (printBody) {
      sb.append("\n");
      addBody(response, sb);
    }
    return sb.toString();
  }

  private static void addBody(Response response, StringBuilder sb) {
    int status = response.status();
    if (response.body() != null && !(status == 204 || status == 205)) {
      // HTTP 204 No Content "...response MUST NOT include a message-body"
      // HTTP 205 Reset Content "...response MUST NOT include an entity"
      sb.append("\n");
      String body = response.body().toString();
      sb.append(String.format("%s", body));
      sb.append(String.format("\n<--- END HTTP (%s-byte body)", body.length()));
    } else {
      sb.append(String.format("<--- END HTTP (%s-byte body)", 0));
    }
  }

  private static void addHeaders(Map<String, Collection<String>> headers, StringBuilder sb) {
    for (String field : headers.keySet()) {
      for (String value : valuesOrEmpty(headers, field)) {
        sb.append(String.format("%s: %s\n", field, value));
      }
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
