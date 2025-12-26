package blockchain;

import java.util.ArrayList;
import java.util.List;

import hashing.Hasher;
import hashing.HashingTools;
import signature.SignedBytes;
import skipGraph.NodeInfo;

public class Transaction extends NodeInfo {

	private static final long serialVersionUID = 1L;
	private final String prev;
	private final int owner;
	private final String cont;// Use random string for this
	private String hash;// Hash
	private List<SignedBytes> sigma;
	private Hasher hasher;
	private int levels;

	// need to add address to transaction

	public Transaction(String prev, int owner, String cont, String address, int levels, int shardID, int maxShards) {
		super(address, 0, prev, shardID);
		this.prev = prev;
		this.owner = owner;
		this.cont = cont;
		this.levels = levels;
		this.sigma = new ArrayList<>();
		hasher = new HashingTools();
		
		String h = hasher.getHash(prev + owner + cont, levels);
    	int baseNum = Integer.parseInt(h, 2);
		
		int ownerShard = Math.floorMod(owner, maxShards); //we want same shard with the owner
		int adjustedNum = (baseNum / maxShards) * maxShards + ownerShard;
			
		String newHash = Integer.toBinaryString(adjustedNum);


		this.hash = newHash; //make the hash equal


		super.setNumID(Integer.parseInt(this.hash, 2));
		super.setShardID(Math.floorMod(super.getNumID(), maxShards));
		//assert super.getShardID() == ownerShard;
	}

	public Transaction(Transaction t) {
		super(t.getAddress(), t.getNumID(), t.getNameID(), t.getShardID());
		hasher = new HashingTools();
		this.prev = t.getPrev();
		this.owner = t.getOwner();
		this.cont = t.getCont();
		this.hash = t.getHash();
		this.sigma = t.getSigma();
		this.levels = t.getLevels();
	}

	public List<SignedBytes> getSigma() {
		return sigma;
	}

	public String getPrev() {
		return prev;
	}

	public int getOwner() {
		return owner;
	}

	public String getCont() {
		return cont;
	}

	public String getHash() {
		return hash;
	}

	public void addSignature(SignedBytes signature) {
		sigma.add(signature);
	}

	public String toString() {
		return prev + owner + cont;
	}
	
	public int getLevels() {
		return levels;
	}

}
