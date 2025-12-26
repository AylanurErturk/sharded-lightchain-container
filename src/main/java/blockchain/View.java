package blockchain;

import java.util.concurrent.ConcurrentHashMap;


public class View {

	private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> lastBlk = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, Integer> state = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, Integer> balance = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, Boolean> mode = new ConcurrentHashMap<>();

	/**
	 * Constructor for an empty view
	 */
	public View(int shardID) {
				lastBlk.computeIfAbsent(
				shardID,
				k -> new ConcurrentHashMap<>());
	}

	/**
	 * updates the latest block of the node whose numID is given
	 * 
	 * @param numID    numerical ID of node whose entry is to be updated
	 * @param blkNumID the numerical ID of the latest block of the given node
	 */
	public synchronized void updateLastBlk(int numID, int blkNumID, int shardID) {
		lastBlk.get(shardID).put(numID, blkNumID);
	}


	
	/**
	 * Updates the state of the node whose numerical ID is given
	 * @param numID numerical ID of node whose entry is to be updated
	 * @param newState new state of the node
	 */
	public synchronized void updateState(int numID, int newState) {
		state.put(numID, newState);
	}

	public synchronized void updateBalance(int numID, int newBalance) {
		balance.put(numID, newBalance);
	}

	public synchronized void updateMode(int numID, boolean newMode) {
		mode.put(numID, newMode);
	}

	public synchronized int getLastBlk(int numID, int shardID) {
		return lastBlk.get(shardID).get(numID);
	}

	public synchronized int getState(int numID) {
		return state.get(numID);
	}

	public synchronized int getBalance(int numID) {
		return balance.get(numID);
	}

	public synchronized boolean getMode(int numID) {
		return mode.get(numID);
	}

	public synchronized boolean hasLastBlkEntry(int numID) {
		return lastBlk.containsKey(numID);
	}

	public synchronized boolean hasStateEntry(int numID) {
		return state.containsKey(numID);
	}

	public synchronized boolean hasBalanceEntry(int numID) {
		return balance.containsKey(numID);
	}

	public synchronized boolean hasModeEntry(int numID) {
		return mode.containsKey(numID);
	}

}
