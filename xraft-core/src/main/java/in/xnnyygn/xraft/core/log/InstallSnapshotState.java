package in.xnnyygn.xraft.core.log;

import in.xnnyygn.xraft.core.node.NodeEndpoint;
import java.util.Set;

public class InstallSnapshotState {

  private final StateName stateName;
  private Set<NodeEndpoint> lastConfig;
  public InstallSnapshotState(StateName stateName) {
    this.stateName = stateName;
  }

  public InstallSnapshotState(StateName stateName, Set<NodeEndpoint> lastConfig) {
    this.stateName = stateName;
    this.lastConfig = lastConfig;
  }

  public StateName getStateName() {
    return stateName;
  }

  public Set<NodeEndpoint> getLastConfig() {
    return lastConfig;
  }

  public enum StateName {
    ILLEGAL_INSTALL_SNAPSHOT_RPC,
    INSTALLING,
    INSTALLED
  }
}
