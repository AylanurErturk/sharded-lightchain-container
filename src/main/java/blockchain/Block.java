package blockchain;

import java.util.ArrayList;
import java.util.List;

import hashing.Hasher;
import hashing.HashingTools;
import signature.SignedBytes;
import skipGraph.NodeInfo;

public class Block extends NodeInfo {

	private static final long serialVersionUID = 1L;
	private final String prev;
	private final int owner;
	private List<Transaction> transactionSet;
	private String hash;
	private List<SignedBytes> sigma;
	private Hasher hasher;
	private final int index;
	private int levels;

	/**
	 * @param prev  the address of the previous block
	 * @param owner the address of the owner of the block
	 */
	public Block(String prev, int owner, String address, int idx, int levels, int shardID, int maxShards) {
		super(address, 0, prev, shardID);
		this.index = idx;
		this.prev = prev;
		this.owner = owner;
		this.transactionSet = new ArrayList<>();
		this.sigma = new ArrayList<>();
		hasher = new HashingTools();

		String h = hasher.getHash(prev + owner, levels);
		int baseNum = Integer.parseInt(h, 2);
		int ownerShard = Math.floorMod(owner, maxShards); // we want same shard with the owner
		int adjustedNum = (baseNum / maxShards) * maxShards + ownerShard;

		String newHash = Integer.toBinaryString(adjustedNum);

		while (newHash.length() < levels) {
			newHash = "0" + newHash;
		}

		this.hash = newHash; // make the hash equal

		super.setNumID(Integer.parseInt(this.hash, 2));
		super.setShardID(Math.floorMod(super.getNumID(), maxShards));
		assert super.getShardID() == ownerShard;

	}

	public Block(String prev, int owner, String address, List<Transaction> tList, int idx, int levels, int shardID,
			int maxShards) {
		super(address, 0, prev, shardID);
		this.index = idx;
		this.prev = prev;
		this.owner = owner;
		this.transactionSet = tList;
		this.levels = levels;
		this.sigma = new ArrayList<>();
		hasher = new HashingTools();

		hasher = new HashingTools();

		String h = hasher.getHash(prev + owner + getTransactionSetString(), levels);
		int baseNum = Integer.parseInt(h, 2);

		int ownerShard = Math.floorMod(owner, maxShards); // we want same shard with the owner
		int adjustedNum = (baseNum / maxShards) * maxShards + ownerShard;

		String newHash = Integer.toBinaryString(adjustedNum);

		while (newHash.length() < levels) {
			newHash = "0" + newHash; //leading 0's
		}

		this.hash = newHash; // make the hash equal

		super.setNumID(Integer.parseInt(this.hash, 2));
		super.setShardID(Math.floorMod(super.getNumID(), maxShards));
		assert super.getShardID() == ownerShard;
	}

	public Block(Block blk) {
		super(blk.getAddress(), blk.getNumID(), blk.getNameID(), blk.getShardID());
		hasher = new HashingTools();
		this.index = blk.getIndex();
		this.prev = blk.getPrev();
		this.owner = blk.getOwner();
		this.transactionSet = blk.getTransactionSet();
		this.hash = blk.getHash();
		this.sigma = blk.getSigma();
		this.levels = blk.getLevels();
	}

	public String getPrev() {
		return prev;
	}

	public int getOwner() {
		return owner;
	}

	public List<Transaction> getTransactionSet() {
		return transactionSet;
	}

	public String getHash() {
		return hash;
	}

	public List<SignedBytes> getSigma() {
		return sigma;
	}

	public void addSignature(SignedBytes signature) {
		sigma.add(signature);
	}

	public void addTransactions(List<Transaction> tList) {
		transactionSet = tList;
	}

	public int getIndex() {
		return index;
	}

	public int getLevels() {
		return levels;
	}

	public String toString() {

		return prev + owner + getTransactionSetString();
	}

	private String getTransactionSetString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < transactionSet.size(); ++i)
			sb.append(transactionSet.get(i).toString());
		return sb.toString();
	}

}
