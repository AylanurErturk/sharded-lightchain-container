package simulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import blockchain.Block;
import blockchain.LightChainNode;
import blockchain.Parameters;
import skipGraph.LookupTable;
import skipGraph.NodeInfo;
import util.Const;
import util.Util;

public class Simulation {

	private static final ConcurrentHashMap<Integer, LookupTable> LOOKUPS = new ConcurrentHashMap<>(); // sid -> l.table
	private static final ConcurrentHashMap<Integer, String> shardIntroducers = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Integer, Boolean> shardInserted = new ConcurrentHashMap<>();
	private static final List<LightChainNode> introducers = new ArrayList<>();
	private static final List<LightChainNode> allNodes = new ArrayList<>();


	public static void startSimulation(Parameters params, int nodeCount, int iterations, int pace) {
		final int maxShards = params.getMaxShards();
		final ExecutorService pool = Executors.newFixedThreadPool(
				Math.max(2, Runtime.getRuntime().availableProcessors()));
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		int stripes = Math.min(10, Runtime.getRuntime().availableProcessors());
		

		
		for (int i = 0; i < nodeCount; i++) {
			if (i < params.getMaxShards()) {
				buildNodeNoThrow(params, null, true);
				continue;
			}
			buildNodeNoThrow(params, shardIntroducers.get(0), false);

		}

		ConcurrentHashMap<Integer, Block> genesisByShard = new ConcurrentHashMap<>(maxShards);
		

		for (int i = 0; i < params.getMaxShards(); i++) {
			Block b = introducers.get(i).insertGenesis();
			introducers.get(i).logLevel(0);
			introducers.get(i).insertFlagNode(b, b.getShardID());
			
		}

		List<ExecutorService> stripeExecs = IntStream.range(0, stripes)
				.mapToObj(i -> Executors.newSingleThreadExecutor(r -> {
					Thread t = new Thread(r, "stripe-" + i);
					t.setDaemon(true);
					return t;
				}))
				.toList();

		
			ConcurrentHashMap<NodeInfo, SimLog> map = new ConcurrentHashMap<>();

			Function<LightChainNode, ExecutorService> execFor = n -> {
				int idx = Math.floorMod((n.getNumID()), stripes);
				return stripeExecs.get(idx);
			};

			Map<Integer, List<LightChainNode>> groups = allNodes.stream()
					.collect(Collectors.groupingBy(n -> n.getShardID()));

			long start = System.currentTimeMillis();

			try {
				for (int shard = 0; shard <= params.getMaxShards(); shard++) {
				List<LightChainNode> group = groups.getOrDefault(shard, List.of());

				List<CompletableFuture<Void>> cfList = group.stream()
						.map(n -> CompletableFuture.supplyAsync(
								() -> n.startSimSync(iterations, pace))
								//execFor.apply(n))
								.thenAccept(lg -> map.put(n.getPeer(), lg))
								.exceptionally(ex -> {
									Util.log("Node " + n.getPeer() + " failed: " + ex);
									return null;
								})) 
						.toList();

				CompletableFuture.allOf(cfList.toArray(new CompletableFuture[0])).join();
			}

			long end = System.currentTimeMillis();
			Util.log("Simulation Done. Time Taken " + (end - start) + " ms");
			processData(map, iterations);
				
			} finally {
				stripeExecs.forEach(ExecutorService::shutdown);
			}

			


	}

	private static LightChainNode buildNodeNoThrow(Parameters p, String introAddr, boolean isIntro) {
		try {
			return buildNode(p, introAddr, isIntro);
		} catch (Exception e) {
			throw new CompletionException(e);
		}
	}

	private static LightChainNode buildNode(Parameters params, String introducerAddr, boolean isIntroducer)
			throws Exception {
		int attempts = 0;
		while (true) {
			attempts++;
			int port = ThreadLocalRandom.current().nextInt(1024, 65535);
			try {
				if (isIntroducer) {
					LightChainNode n = new LightChainNode(params, port, Const.DUMMY_INTRODUCER, true);
					shardInserted.put(n.getShardID(), true);
					shardIntroducers.put(n.getShardID(), n.getAddress());
					introducers.add(n);
					allNodes.add(n);
					return n;
				} else {
					LightChainNode n = new LightChainNode(params, port, introducerAddr, false);
					allNodes.add(n);
					return n;
				}
			} catch (Exception e) {
				if (attempts >= 15)
					throw e;
				try {
					Thread.sleep(ThreadLocalRandom.current().nextInt(5, 25));
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
	}

	private static <T> List<T> waitAll(String label, List<CompletableFuture<T>> cfs, long timeout, TimeUnit unit) {
		CompletableFuture<Void> all = CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new));
		try {
			all.orTimeout(timeout, unit).join();
			return cfs.stream().map(CompletableFuture::join).toList();
		} catch (CompletionException ce) {
			cfs.forEach(f -> f.cancel(true));
			throw new RuntimeException(label + " timed out/failed", ce.getCause());
		}
	}

	private static void waitAllVoid(String label,
			List<CompletableFuture<Void>> cfs,
			long timeout, TimeUnit unit) {
		CompletableFuture<Void> all = CompletableFuture.allOf(cfs.toArray(CompletableFuture[]::new));
		try {
			all.orTimeout(timeout, unit).join();
		} catch (CompletionException ce) {
			cfs.forEach(f -> f.cancel(true));
			throw new RuntimeException(label + " timed out/failed", ce.getCause());
		}
	}

	private static void processData(ConcurrentHashMap<NodeInfo, SimLog> map, int iterations) {
		processTransactions(map, iterations);
		processMineAttempts(map, iterations);
	}

	private static void processMineAttempts(ConcurrentHashMap<NodeInfo, SimLog> map, int iterations) {

		try {
			String logPath = System.getProperty("user.dir") + File.separator + "Logs" + File.separator
					+ "MineAttempts.csv";
			File logFile = new File(logPath);

			logFile.getParentFile().mkdirs();
			PrintWriter writer;

			writer = new PrintWriter(logFile);

			StringBuilder sb = new StringBuilder();

			sb.append("NumID," + "Honest," + "total time," + "foundTxMin," + "Validation time," + "successful\n");

			int successSum = 0;

			for (NodeInfo cur : map.keySet()) {
				SimLog log = map.get(cur);
				List<MineAttemptLog> validMine = log.getValidMineAttemptLog();
				List<MineAttemptLog> failedMine = log.getFailedMineAttemptLog();

				sb.append(cur.getNumID() + "," + log.getMode() + ",");
				for (int i = 0; i < validMine.size(); i++) {
					if (i != 0)
						sb.append(",,");
					sb.append(validMine.get(i));
				}
				successSum += validMine.size();
				for (int i = 0; i < failedMine.size(); i++) {
					sb.append(",,");
					sb.append(failedMine.get(i));
				}
				sb.append('\n');
			}
			double successRate = (double) successSum / (1.0 * iterations * map.keySet().size()) * 100;
			sb.append("Success Rate = " + successRate + "\n");

			writer.write(sb.toString());
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void processTransactions(ConcurrentHashMap<NodeInfo, SimLog> map, int iterations) {

		try {
			String logPath = System.getProperty("user.dir") + File.separator + "Logs" + File.separator
					+ "TransactionValidationAttempts.csv";
			File logFile = new File(logPath);

			logFile.getParentFile().mkdirs();
			PrintWriter writer;

			writer = new PrintWriter(logFile);

			StringBuilder sb = new StringBuilder();

			sb.append("NumID," + "Honest," + "Transaction Trials," + "Transaction Success,"
					+ "Transaction time(per)," + "Authenticated count," + "Sound count," + "Correct count,"
					+ "Has Balance count," + "Successful," + "Timer Per Validator(ms)\n");

			int successSum = 0;

			for (NodeInfo cur : map.keySet()) {
				SimLog log = map.get(cur);
				List<TransactionLog> validTransactions = log.getValidTransactions();
				List<TransactionLog> failedTransactions = log.getFailedTransactions();

				sb.append(cur.getNumID() + "," + log.getMode() + "," + log.getTotalTransactionTrials() + ","
						+ log.getValidTransactionTrials() + ",");
				for (int i = 0; i < validTransactions.size(); i++) {
					if (i != 0)
						sb.append(",,,,");
					sb.append(validTransactions.get(i));
				}
				successSum += validTransactions.size();
				for (int i = 0; i < failedTransactions.size(); i++) {
					sb.append(",,,,");
					sb.append(failedTransactions.get(i));
				}
				sb.append('\n');
			}
			double successRate = (double) (successSum * 100.0) / (1.0 * iterations * map.keySet().size());
			sb.append("Success Rate = " + successRate + "\n");
			writer.write(sb.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	public static LookupTable lt(int sid, int levels, int maxShards) {
		return LOOKUPS.computeIfAbsent(
				sid,
				k -> new LookupTable(levels));
	}

	public static void putIntroducers(int sid, String address) {
		shardIntroducers.put(sid, address);
	}

	public static void shardInserted(int sid) {
		shardInserted.put(sid, true);
	}

	public static String getIntroducer(int sid) {
		return shardIntroducers.get(sid);
	}

	public static boolean getIsInserted(int sid) {
		return shardInserted.getOrDefault(sid, false);
	}

}
