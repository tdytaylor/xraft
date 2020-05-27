package in.xnnyygn.xraft.core.node.config;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;

public interface NodeConfigLoader {

  @Nonnull
  NodeConfig load(@Nonnull InputStream input) throws IOException;
}
