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

/** Connector. */
public interface Connector {

  /**
   * Initialize connector.
   *
   * <p>SHOULD NOT call more than one.
   */
  void initialize();

  void sendPreVote(@Nonnull PreVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints);

  void replyPreVote(@Nonnull PreVoteResult result, @Nonnull PreVoteRpcMessage rpcMessage);

  /**
   * Send request vote rpc.
   *
   * <p>Remember to exclude self node before sending.
   *
   * <p>Do nothing if destination endpoints is empty.
   *
   * @param rpc rpc
   * @param destinationEndpoints destination endpoints
   */
  void sendRequestVote(
      @Nonnull RequestVoteRpc rpc, @Nonnull Collection<NodeEndpoint> destinationEndpoints);

  /**
   * Reply request vote result.
   *
   * @param result result
   * @param rpcMessage rpc message
   */
  void replyRequestVote(
      @Nonnull RequestVoteResult result, @Nonnull RequestVoteRpcMessage rpcMessage);

  /**
   * Send append entries rpc.
   *
   * @param rpc rpc
   * @param destinationEndpoint destination endpoint
   */
  void sendAppendEntries(@Nonnull AppendEntriesRpc rpc, @Nonnull NodeEndpoint destinationEndpoint);

  /**
   * Reply append entries result.
   *
   * @param result result
   * @param rpcMessage rpc message
   */
  void replyAppendEntries(
      @Nonnull AppendEntriesResult result, @Nonnull AppendEntriesRpcMessage rpcMessage);

  /**
   * Send install snapshot rpc.
   *
   * @param rpc rpc
   * @param destinationEndpoint destination endpoint
   */
  void sendInstallSnapshot(
      @Nonnull InstallSnapshotRpc rpc, @Nonnull NodeEndpoint destinationEndpoint);

  /**
   * Reply install snapshot result.
   *
   * @param result result
   * @param rpcMessage rpc message
   */
  void replyInstallSnapshot(
      @Nonnull InstallSnapshotResult result, @Nonnull InstallSnapshotRpcMessage rpcMessage);

  /**
   * Called when node becomes leader.
   *
   * <p>Connector may use this chance to close inbound channels.
   */
  void resetChannels();

  /** Close connector. */
  void close();
}
