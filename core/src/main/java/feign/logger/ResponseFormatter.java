package feign.logger;

import feign.Response;

@FunctionalInterface
public interface ResponseFormatter {

  String format(Response response, long elapsedTime);

}
