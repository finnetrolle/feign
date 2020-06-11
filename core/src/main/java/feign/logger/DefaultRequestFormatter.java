package feign.logger;


import static feign.Util.valuesOrEmpty;

import feign.Request;
import java.util.Collection;
import java.util.Map;

public class DefaultRequestFormatter implements RequestFormatter {

  private final boolean printHeaders;
  private final boolean printBody;
  public static final DefaultRequestFormatter DEFAULT = DefaultRequestFormatter.builder().build();

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

  private static void addHeaders(Map<String, Collection<String>> headers, StringBuilder sb) {
    for (String field : headers.keySet()) {
      for (String value : valuesOrEmpty(headers, field)) {
        sb.append(String.format("%s: %s\n", field, value));
      }
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
