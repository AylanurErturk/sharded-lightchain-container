package skipGraph;

import java.rmi.RemoteException;

public class NodeConfig {
	
	private int maxLevels;
	private int RMIPort;
	private int numID;
	private String nameID;
	private int shardID;
	private int maxShards;
	
	public NodeConfig(int maxLevels, int maxShards, int RMIPort, int numID, String nameID) {
		this.maxLevels = maxLevels;
		this.RMIPort = RMIPort;
		this.numID = numID;
		this.nameID = nameID;
		this.maxShards = maxShards;
	}

	public int getMaxLevels() {
		return maxLevels;
	}

	public void setMaxLevels(int maxLevels) {
		this.maxLevels = maxLevels;
	}

	public int getRMIPort() {
		return RMIPort;
	}

	public void setRMIPort(int rMIPort) {
		RMIPort = rMIPort;
	}

	public int getNumID() {
		return numID;
	}

	public void setNumID(int numID) {
		this.numID = numID;
	}

	public String getNameID() {
		return nameID;
	}

	public void setNameID(String nameID) {
		this.nameID = nameID;
	}
	
	public void setShardID (int shardID) {
		this.shardID = shardID;
	}

	public int getShardID () {
		return this.shardID;
	}
	
	public void setMaxShards(int maxShards) {
		this.maxShards = maxShards;
	}

	public int getMaxShards() {
		return this.maxShards;
	}

	
}
