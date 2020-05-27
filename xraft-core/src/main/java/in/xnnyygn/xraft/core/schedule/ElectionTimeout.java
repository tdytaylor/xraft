package in.xnnyygn.xraft.core.schedule;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElectionTimeout {

  public static final ElectionTimeout NONE = new ElectionTimeout(new NullScheduledFuture());
  private static final Logger logger = LoggerFactory.getLogger(ElectionTimeout.class);
  private final ScheduledFuture<?> scheduledFuture;

  public ElectionTimeout(ScheduledFuture<?> scheduledFuture) {
    this.scheduledFuture = scheduledFuture;
  }

  public void cancel() {
    logger.debug("cancel election timeout");
    this.scheduledFuture.cancel(false);
  }

  @Override
  public String toString() {
    if (this.scheduledFuture.isCancelled()) {
      return "ElectionTimeout(state=cancelled)";
    }
    if (this.scheduledFuture.isDone()) {
      return "ElectionTimeout(state=done)";
    }
    return "ElectionTimeout{delay=" + scheduledFuture.getDelay(TimeUnit.MILLISECONDS) + "ms}";
  }
}
