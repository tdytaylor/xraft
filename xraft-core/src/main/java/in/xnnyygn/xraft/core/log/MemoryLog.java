package in.xnnyygn.xraft.core.log;

import in.xnnyygn.xraft.core.log.entry.*;
import in.xnnyygn.xraft.core.log.snapshot.*;
import in.xnnyygn.xraft.core.node.NodeConfig;
import in.xnnyygn.xraft.core.node.NodeId;
import in.xnnyygn.xraft.core.rpc.message.AppendEntriesRpc;
import in.xnnyygn.xraft.core.rpc.message.InstallSnapshotRpc;
import in.xnnyygn.xraft.core.rpc.message.RequestVoteRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class MemoryLog implements Log {

    private static final Logger logger = LoggerFactory.getLogger(MemoryLog.class);
    private static final int INSTALL_SNAPSHOT_RPC_DATA_LENGTH = 10;
    private static final int SNAPSHOT_GENERATE_THRESHOLD = 5;
    private Snapshot snapshot = new EmptySnapshot();
    private EntrySequence entrySequence = new EntrySequence();
    private GroupConfigEntryList groupConfigEntryList = new GroupConfigEntryList();
    private MemorySnapshotBuilder snapshotBuilder;
    private EntryApplier entryApplier = new NullEntryApplier();
    private SnapshotGenerator snapshotGenerator = new NullSnapshotGenerator();
    private SnapshotApplier snapshotApplier = new NullSnapshotApplier();
    private int commitIndex = 0;
    private int lastApplied = 0;

    public MemoryLog() {
        this(new EmptySnapshot(), new EntrySequence());
    }

    MemoryLog(Snapshot snapshot, EntrySequence entrySequence) {
        this.snapshot = snapshot;
        this.entrySequence = entrySequence;
    }

    @Override
    public NoOpEntry appendEntry(int term) {
        return this.entrySequence.append(term);
    }

    @Override
    public GeneralEntry appendEntry(int term, byte[] commandBytes) {
        return this.entrySequence.append(term, commandBytes);
    }

    @Override
    public GroupConfigEntry appendEntry(int term, Set<NodeConfig> nodeConfigs) {
        GroupConfigEntry entry = this.entrySequence.append(term, nodeConfigs);
        this.groupConfigEntryList.add(entry);
        return entry;
    }

    @Override
    public GroupConfigEntry getLastGroupConfigEntry() {
        return this.groupConfigEntryList.getLast();
    }

    @Override
    public int getCommitIndex() {
        return this.commitIndex;
    }

    @Override
    public boolean appendEntries(AppendEntriesRpc rpc) {
        int prevLogIndex = rpc.getPrevLogIndex();
        if (prevLogIndex > 0) {
            if (prevLogIndex < this.snapshot.getLastIncludedIndex()) {
                logger.debug("previous log index < snapshot's last included index");
                return false;
            }

            if (prevLogIndex == this.snapshot.getLastIncludedIndex()) {
                if (rpc.getPrevLogTerm() != this.snapshot.getLastIncludedTerm()) {
                    logger.debug("previous log term != snapshot's last included term");
                    return false;
                }
            } else {
                assert prevLogIndex > this.snapshot.getLastIncludedIndex();
                Entry prevLog = this.entrySequence.getEntry(prevLogIndex);
                if (prevLog == null || prevLog.getTerm() != rpc.getPrevLogTerm()) {
                    logger.debug("previous log {} not found or different term", prevLogIndex);
                    return false;
                }
            }
        }

        mergeEntries(prevLogIndex + 1, rpc.getEntries());
        // move up
        this.advanceCommitIndex(Math.min(rpc.getLeaderCommit(), rpc.getLastEntryIndex()), rpc.getTerm());
        return true;
    }

    @Override
    public RequestVoteRpc createRequestVoteRpc(int term, NodeId selfNodeId) {
        RequestVoteRpc rpc = new RequestVoteRpc();
        rpc.setTerm(term);
        rpc.setCandidateId(selfNodeId);
        if (this.entrySequence.isEmpty()) {
            rpc.setLastLogIndex(this.snapshot.getLastIncludedIndex());
            rpc.setLastLogTerm(this.snapshot.getLastIncludedTerm());
        } else {
            Entry lastEntry = this.entrySequence.getLastEntry();
            rpc.setLastLogIndex(lastEntry.getIndex());
            rpc.setLastLogTerm(lastEntry.getTerm());
        }
        return rpc;
    }

    // TODO Get Last Entry Meta
    @Override
    public void setLastEntryIndexAndTerm(RequestVoteRpc rpc) {
        if (entrySequence.isEmpty()) {
            rpc.setLastLogIndex(snapshot.getLastIncludedIndex());
            rpc.setLastLogTerm(snapshot.getLastIncludedTerm());
        } else {
            Entry lastEntry = entrySequence.getLastEntry();
            rpc.setLastLogIndex(lastEntry.getIndex());
            rpc.setLastLogTerm(lastEntry.getTerm());
        }
    }

    @Override
    public AppendEntriesRpc createAppendEntriesRpc(int term, NodeId selfNodeId, int nextIndex, int maxEntries) {
        if (nextIndex > this.entrySequence.getNextLogIndex()) {
            throw new IllegalArgumentException("illegal next index " + nextIndex);
        }

        AppendEntriesRpc rpc = new AppendEntriesRpc();
        rpc.setTerm(term);
        rpc.setLeaderId(selfNodeId);
        rpc.setLeaderCommit(this.commitIndex);

        if (this.entrySequence.isEmpty()) {
            if (nextIndex < this.entrySequence.getNextLogIndex()) {
                throw new EntryInSnapshotException(nextIndex);
            }
            assert nextIndex == this.entrySequence.getNextLogIndex();
            rpc.setPrevLogIndex(this.snapshot.getLastIncludedIndex());
            rpc.setPrevLogTerm(this.snapshot.getLastIncludedTerm());
            return rpc;
        }

        if (nextIndex < this.entrySequence.getFirstLogIndex()) {
            throw new EntryInSnapshotException(nextIndex);
        }
        if (nextIndex == this.entrySequence.getFirstLogIndex()) {
            rpc.setPrevLogIndex(this.snapshot.getLastIncludedIndex());
            rpc.setPrevLogTerm(this.snapshot.getLastIncludedTerm());
        } else {
            Entry entry = this.entrySequence.getEntry(nextIndex - 1);
            assert entry != null;
            rpc.setPrevLogIndex(entry.getIndex());
            rpc.setPrevLogTerm(entry.getTerm());
        }
        rpc.setEntries(this.entrySequence.subList(
                nextIndex,
                (maxEntries < 0 ? this.entrySequence.getNextLogIndex() : Math.min(this.entrySequence.getNextLogIndex(), nextIndex + maxEntries))
        ));
        return rpc;
    }

    @Override
    public InstallSnapshotRpc createInstallSnapshotRpc(int term, NodeId selfNodeId, int offset) {
        InstallSnapshotRpc rpc = new InstallSnapshotRpc();
        rpc.setTerm(term);
        rpc.setLeaderId(selfNodeId);
        rpc.setLastIncludedIndex(snapshot.getLastIncludedIndex());
        rpc.setLastIncludedTerm(snapshot.getLastIncludedTerm());
        rpc.setOffset(offset);

        SnapshotChunk chunk = snapshot.read(offset, INSTALL_SNAPSHOT_RPC_DATA_LENGTH);
        rpc.setData(chunk.toByteArray());
        rpc.setDone(chunk.isLastChunk());
        return rpc;
    }

    @Override
    public void advanceCommitIndex(int newCommitIndex, int currentTerm) {
        if (newCommitIndex <= this.commitIndex) return;

        // newCommitIndex >= this.entrySequence.getFirstLogIndex() && newCommitIndex <= this.entrySequence.getLastLogIndex();
        assert this.entrySequence.getEntry(newCommitIndex) != null;
        if (this.entrySequence.getEntry(newCommitIndex).getTerm() != currentTerm) return;

        logger.debug("advance commit index from {} to {}", this.commitIndex, newCommitIndex);
        // TODO commit items
        this.commitIndex = newCommitIndex;

        logger.debug("apply log from {} to {}", this.lastApplied + 1, this.commitIndex);
        for (Entry entry : this.entrySequence.subList(this.lastApplied + 1, this.commitIndex + 1)) {
            List<EntryListener> entryListeners = entry.getListeners();
            entryListeners.forEach(l -> l.entryCommitted(entry));
            this.applyEntry(entry);
            entryListeners.forEach(l -> l.entryApplied(entry));
            entry.removeAllListeners();
        }

        if (this.lastApplied - this.entrySequence.getFirstLogIndex() >= SNAPSHOT_GENERATE_THRESHOLD) {
            logger.info("generate snapshot, last included index {}", this.lastApplied);
            Entry lastAppliedEntry = this.entrySequence.getEntry(this.lastApplied);
            assert lastAppliedEntry != null;

            Snapshot snapshot = new MemorySnapshot(
                    lastAppliedEntry.getIndex(), lastAppliedEntry.getTerm(), this.snapshotGenerator.generateSnapshot());
            this.replaceSnapshot(snapshot);
        }
    }

    private void applyEntry(Entry entry) {
        // skip no-op entry and membership-change entry
        if (entry instanceof GeneralEntry) {
            this.entryApplier.applyEntry(entry);
        }
        this.lastApplied = entry.getIndex();
    }

    @Override
    public int getNextLogIndex() {
        return this.entrySequence.getNextLogIndex();
    }

    @Override
    public boolean isNewerThan(int lastLogIndex, int lastLogTerm) {
        // TODO handle snapshot
        Entry lastEntry = this.entrySequence.getLastEntry();
        if (lastEntry == null) {
            logger.debug("no last entry");
            return false;
        }

        logger.debug("last entry ({}, {}), candidate ({}, {})", lastEntry.getIndex(), lastEntry.getTerm(), lastLogIndex, lastLogTerm);
        return lastEntry.getTerm() > lastLogTerm || lastEntry.getIndex() > lastLogIndex;
    }

    @Override
    public void setEntryApplier(EntryApplier applier) {
        this.entryApplier = applier;
    }

    @Override
    public void installSnapshot(InstallSnapshotRpc rpc) {
        if (rpc.getLastIncludedIndex() <= this.snapshot.getLastIncludedIndex()) {
            logger.debug("snapshot's last included index from rpc <= current one, ignore");
            return;
        }

        if (rpc.getOffset() == 0) {
            this.snapshotBuilder = new MemorySnapshotBuilder(rpc);
            return;
        }

        if (this.snapshotBuilder == null) {
            throw new IllegalStateException("no snapshot data with offset 0");
        }

        this.snapshotBuilder.append(rpc);
        if (!rpc.isDone()) return;

        Snapshot newSnapshot = this.snapshotBuilder.build();
        logger.info("install snapshot, last included index {}", newSnapshot.getLastIncludedIndex());
        // TODO refactor
        this.snapshotApplier.applySnapshot(newSnapshot.toByteArray());
        this.replaceSnapshot(newSnapshot);

        if (this.commitIndex < newSnapshot.getLastIncludedIndex()) {
            this.commitIndex = newSnapshot.getLastIncludedIndex();
        }
        if (this.lastApplied <= newSnapshot.getLastIncludedIndex()) {
            this.lastApplied = newSnapshot.getLastIncludedIndex();
        }
    }

    @Override
    public void setSnapshotGenerator(SnapshotGenerator generator) {
        this.snapshotGenerator = generator;
    }

    @Override
    public void setSnapshotApplier(SnapshotApplier applier) {
        this.snapshotApplier = applier;
    }

    private void replaceSnapshot(Snapshot newSnapshot) {
        logger.debug("replace snapshot with {}", newSnapshot);
        this.snapshot = newSnapshot;
        if (newSnapshot.getLastIncludedIndex() < this.entrySequence.getNextLogIndex()) {
            this.entrySequence.clearBefore(newSnapshot.getLastIncludedIndex() + 1);
        } else {
            this.entrySequence = new EntrySequence(newSnapshot.getLastIncludedIndex() + 1);
        }
        logger.debug("current entry sequence {}", this.entrySequence);
    }

    private void mergeEntries(int fromIndex, List<Entry> leaderEntries) {
        if (leaderEntries.isEmpty()) return;

        int copyFrom = 0;
        if (!this.entrySequence.isEmpty() && fromIndex <= this.entrySequence.getLastLogIndex()) {
            List<Entry> followerEntries = this.entrySequence.subList(fromIndex,
                    Math.min(this.entrySequence.getLastLogIndex() + 1, fromIndex + leaderEntries.size()));
            copyFrom = followerEntries.size();
            for (int i = 0; i < Math.min(followerEntries.size(), leaderEntries.size()); i++) {
                Entry followerEntry = followerEntries.get(i);
                Entry leaderEntry = leaderEntries.get(i);
                if (followerEntry.getTerm() != leaderEntry.getTerm()) {
                    logger.debug("remove entries from {}", followerEntry.getIndex());
                    this.entrySequence.clearAfter(followerEntry.getIndex() - 1);
                    copyFrom = i;
                    break;
                }
            }
        }

        if (copyFrom > 0) {
            logger.debug("skip copying {} entries", copyFrom);
        }
        if (copyFrom < leaderEntries.size()) {
            logger.debug("append leader entries from {} to {}", leaderEntries.get(copyFrom).getIndex(), leaderEntries.get(leaderEntries.size() - 1).getIndex());
            this.entrySequence.appendEntries(leaderEntries.subList(copyFrom, leaderEntries.size()));
        }
    }

}