package in.xnnyygn.xraft.core.schedule;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;

public class NullScheduledFuture implements ScheduledFuture<Object> {

  @Override
  public long getDelay(@Nonnull TimeUnit unit) {
    return 0;
  }

  @Override
  public int compareTo(@Nonnull Delayed o) {
    return 0;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public Object get() throws InterruptedException, ExecutionException {
    return null;
  }

  @Override
  public Object get(long timeout, @Nonnull TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return null;
  }
}
