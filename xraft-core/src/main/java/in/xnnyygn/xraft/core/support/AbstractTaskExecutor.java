package in.xnnyygn.xraft.core.support;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import java.util.Collections;
import javax.annotation.Nonnull;

public abstract class AbstractTaskExecutor implements TaskExecutor {

  @Override
  public void submit(@Nonnull Runnable task, @Nonnull FutureCallback<Object> callback) {
    Preconditions.checkNotNull(task);
    Preconditions.checkNotNull(callback);
    submit(task, Collections.singletonList(callback));
  }
}
