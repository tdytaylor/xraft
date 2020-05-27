package in.xnnyygn.xraft.core.log.snapshot;

import in.xnnyygn.xraft.core.node.NodeEndpoint;
import java.io.InputStream;
import java.util.Set;
import javax.annotation.Nonnull;

// TODO add doc
public interface Snapshot {

  int getLastIncludedIndex();

  int getLastIncludedTerm();

  @Nonnull
  Set<NodeEndpoint> getLastConfig();

  long getDataSize();

  @Nonnull
  SnapshotChunk readData(int offset, int length);

  @Nonnull
  InputStream getDataStream();

  void close();
}
