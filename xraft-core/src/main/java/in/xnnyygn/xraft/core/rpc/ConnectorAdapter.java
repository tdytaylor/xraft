package in.xnnyygn.xraft.core.rpc;

import in.xnnyygn.xraft.core.node.NodeEndpoint;
import in.xnnyygn.xraft.core.rpc.message.AppendEntriesResult;
import in.xnnyygn.xraft.core.rpc.message.AppendEntriesRpc;
import in.xnnyygn.xraft.core.rpc.message.AppendEntriesRpcMessage;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotResult;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpcMessage;
import in.xnnyygn.xraft.core.rpc.message.PreVoteResult;
import in.xnnyygn.xraft.core.rpc.message.PreVoteRpc;
import in.xnnyygn.xraft.core.rpc.message.PreVoteRpcMessage;
import in.xnnyygn.xraft.core.rpc.message.RequestVoteResult;
import in.xnnyygn.xraft.core.rpc.message.RequestVoteRpc;
import in.xnnyygn.xraft.core.rpc.message.RequestVoteRpcMessage;
import java.util.Collection;
import javax.annotation.Nonnull;

public abstract class ConnectorAdapter implements Connector {

  @Override
  public void initialize() {}

  @Override
  public void sendPreVote(
      @Nonnull PreVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints) {}

  @Override
  public void replyPreVote(@Nonnull PreVoteResult result, @Nonnull PreVoteRpcMessage rpcMessage) {}

  @Override
  public void sendRequestVote(
      @Nonnull RequestVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints) {}

  @Override
  public void replyRequestVote(
      @Nonnull RequestVoteResult result, @Nonnull RequestVoteRpcMessage rpcMessage) {}

  @Override
  public void sendAppendEntries(
      @Nonnull AppendEntriesRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {}

  @Override
  public void replyAppendEntries(
      @Nonnull AppendEntriesResult result, @Nonnull AppendEntriesRpcMessage rpcMessage) {}

  @Override
  public void sendInstallSnapshot(
      @Nonnull InstallSnapshotRpc rpc, @Nonnull NodeEndpoint destinationEndpoint) {}

  @Override
  public void replyInstallSnapshot(
      @Nonnull InstallSnapshotResult result, @Nonnull InstallSnapshotRpcMessage rpcMessage) {}

  @Override
  public void resetChannels() {}

  @Override
  public void close() {}
}
