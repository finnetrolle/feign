package feign.logger;

public abstract class AbstractFeignLogger implements FeignLogger {

  protected final LoggerConfiguration configuration;

  protected AbstractFeignLogger(LoggerConfiguration configuration) {
    this.configuration = configuration;
  }

  protected void logWithLevel(String message) {
    switch (configuration.getLevel()) {
      case INFO: configuration.getLogger().info(message); break;
      case WARN: configuration.getLogger().warn(message); break;
      case DEBUG: configuration.getLogger().debug(message); break;
      case ERROR: configuration.getLogger().error(message); break;
      case TRACE: configuration.getLogger().trace(message); break;
    }
  }

}
