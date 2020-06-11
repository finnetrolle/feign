package feign.logger;

import feign.Request;

@FunctionalInterface
public interface RequestFormatter {

  String format(Request request);

}
