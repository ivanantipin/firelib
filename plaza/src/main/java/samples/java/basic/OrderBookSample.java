package samples.java.basic;/*
Copyright (c) 2014, MOEX. All rights reserved.

Plaza-2 Client Gate API Usage Sample.
Replication DataStream Client.

All the software and documentation included in this and any
other MOEX CGate Release is copyrighted by MOEX.

Redistribution and use in source and binary forms, with or without
modification, are permitted only by the terms of a valid
software license agreement with MOEX.

THIS SOFTWARE IS PROVIDED "AS IS" AND MICEX-RTS DISCLAIMS ALL WARRANTIES
EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION, ANY IMPLIED WARRANTIES OF
NON-INFRINGEMENT, MERCHANTABILITY OR FITNESS FOR A PARTICULAR
PURPOSE.  MICEX-RTS DOES NOT WARRANT THAT USE OF THE SOFTWARE WILL BE
UNINTERRUPTED OR ERROR-FREE.  MICEX-RTS SHALL NOT, UNDER ANY CIRCUMSTANCES, BE
LIABLE TO LICENSEE FOR LOST PROFITS, CONSEQUENTIAL, INCIDENTAL, SPECIAL OR
INDIRECT DAMAGES ARISING OUT OF OR RELATED TO THIS AGREEMENT OR THE
TRANSACTIONS CONTEMPLATED HEREUNDER, EVEN IF MICEX-RTS HAS BEEN APPRISED OF
THE LIKELIHOOD OF SUCH DAMAGES.
*/

import ru.micexrts.cgate.CGate;
import ru.micexrts.cgate.CGateException;
import ru.micexrts.cgate.Connection;
import ru.micexrts.cgate.ErrorCode;
import ru.micexrts.cgate.ISubscriber;
import ru.micexrts.cgate.Listener;
import ru.micexrts.cgate.MessageDesc;
import ru.micexrts.cgate.MessageType;
import ru.micexrts.cgate.Scheme;
import ru.micexrts.cgate.State;
import ru.micexrts.cgate.messages.Message;
import ru.micexrts.cgate.messages.P2ReplClearDeletedMessage;
import ru.micexrts.cgate.messages.P2ReplLifeNumMessage;
import ru.micexrts.cgate.messages.P2ReplStateMessage;
import ru.micexrts.cgate.messages.StreamDataMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ????????? ?????? ?????????? ?????? ????????? ??????? ??? ?????????????
 * ???????????, ??? isin_id ?????? ? ?????????? ????????? ??????. ?????
 * ??????????? 
 * - ???????????? ?????????? ????????? ?????? (??????? ???????, 0 - ??? ?????? ??? ???????????), 
 * - ???????? ????????? ????? ? 
 * - ??????? ??????????? ?????????? (r).
 * 
 * This sample represents working with order book for specific instrument which
 * isin_id is set in command line arguments. The other arguments are: 
 * - order book depth (max quantity of elements in output, 0 for unlimited output), 
 * - output file name and 
 * - reverse sort flag (r).
 */
public class OrderBookSample {
	
	// input params names
	private static final String ISIN_ID_NAME_PARAM = "isinId";
	private static final String DEPTH_ORDER_BOOK_PARAM = "depth";
	private static final String SAVE_FILE_NAME_PARAM = "fileName";
	private static final String REVERSE_PARAM = "r";
	
	// ?????? ???? ??? ?????
	//
	// data format for logs
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy hh.mm.ss.sss");
	// ??? ????? ??? ?????
	// 
	// ??? ?????? Enter, ??? ??????? ?? ??????? ?????????? ???????? ??????? ? ????
	//
	// Enter key code, on Enter pressed orderbook is written to file 
	private static final int ENTER_CODE = 10;

	// ?????? ???????????? ??????
	//
	// status of non-system order
	private final int STATUS_NONSYSTEM = 0x04;
	private final int STATUS_COMMIT = 0x1000;

	// action constants
	private static final int ACTION_REMOVE = 0; // removed
	private static final int ACTION_ADD = 1; // added
	private static final int ACTION_UPDATE = 2; // reduced to deal
	
	// ???????? ?????????? ????????? ??????
	//
	// command line args values
	// ????????????? ???????????
	//
	// identifier instrument
	private static int isinId;
	// ??????? ???????
	//
	// order book depth
	private static int depthOrderBook;
	
	// ???? ???????? ??????????
	//
	// reverse sorting flag
	private static boolean reverse = false;
	
	// timer for listen input stream
	private Timer timer = new Timer();

	// ??????? ??????, ??????? ?? ????????????
	// ???????????????? ?? ???????? ??????
	//
	// indexes of tables we're going to work with
	// initialized on stream opening
	private int ordersLogTableIndex;
	private int ordersTableIndex;
	private int sysEventsTableIndex;

	// session id. The messages with sessId less then current one will be ignored.
	private int currSessionId = 0;
	// ????? ??????? ???????
	//
	// current revision number
	private static long currRevision = 0;

	// ??????, ? ??????? ???????? ?????? ?? ????????????? ??????????
	//
	// orders before the end of business transaction are stored here
	private List<Order> transSellOrders = new LinkedList<Order>();
	private List<Order> transBuyOrders = new LinkedList<Order>();

	// Order book contains orders to find appropriate order for updating or removing
	// key - id_ord
	private Map<Long, Order> ordersBook = new HashMap<Long, Order>();

	// aggregated data for purchase and sell
	private Map<BigDecimal, Integer> buyAggrOrders = new HashMap<BigDecimal, Integer>();
	private Map<BigDecimal, Integer> sellAggrOrders = new HashMap<BigDecimal, Integer>();

	// value to reopen stream from saved state
	private String replState = "";

	private Connection connection;
	private Listener listener;
	
	// ??? ????????? ?????
	//
	// output file name
	private static String outputFileName;
	private static String outputFileExt;
	
	/**
	 * ? ?????? ?????????????? ?????????? ? ?????? ????????? ?????? ?????????. <br/>
	 * Class processes received in callback messages.
	 * 
	 */
	private class Subscriber implements ISubscriber {

		@Override
		public int onMessage(Connection conn, Listener listener, Message message) {
			int messageType = message.getType();
			switch (messageType) {
			// ??? ???????? ?????? ???????????? ??????? ??????
			//
			// define table indexes on stream opening
			case MessageType.MSG_OPEN:
				try {
					initTableIndexes();
					restart(0);
				} catch (CGateException ex) {
					ex.printStackTrace();
					throw new RuntimeException(ex);
				}
				break;
				
			// ????????? ? ???????? ??????
			//
			// close stream message processing
			case MessageType.MSG_CLOSE:
				writeOrderBook(outputFileName + outputFileExt);
				log("Close stream.");
				break;
				
			case MessageType.MSG_STREAM_DATA:
				StreamDataMessage msg = (StreamDataMessage) message;
				processStreamData(msg, msg.getMsgIndex());
				break;
				
			// ????????? ???????? ????? ????????? ??????
			// ??????????? ????????? ??? ???????????? ? ???? ??????
			//
			// receive message before stream closing
			// save state in order to reopen stream from it
			case MessageType.MSG_P2REPL_REPLSTATE:
				P2ReplStateMessage p2ReplStatemessage = (P2ReplStateMessage) message;
				replState = p2ReplStatemessage.getReplState();
				break;
				
			// ???????? ??? ?????? ??? ????? ?????? ?????
			//
			// clear all data when life number is changed
			case MessageType.MSG_P2REPL_LIFENUM:
				P2ReplLifeNumMessage p2ReplLifeNumMessage = (P2ReplLifeNumMessage) message;
				long lifeNum = p2ReplLifeNumMessage.getLifenum();
				log("Life number changed. New life number is " + lifeNum);
				restart(0);
				break;
				
			// ???????? ???????? ?????????? ??????
			//
			// message about mass deletion of old records
			case MessageType.MSG_P2REPL_CLEARDELETED:
				P2ReplClearDeletedMessage p2ReplClearDeletedMessage = (P2ReplClearDeletedMessage) message;
				int tableIndex = p2ReplClearDeletedMessage.getTableIdx();
				long clearDeletedRev = p2ReplClearDeletedMessage.getTableRev();
				log("Clear old orders. Table index = " + tableIndex
						+ ", clearDeletedRev = " + clearDeletedRev);
				processClearDeleted(tableIndex, clearDeletedRev);
				break;
			
			// ????????? ? ?????? ?????? ? online
			//
			// repl online message
			case MessageType.MSG_P2REPL_ONLINE:
				log("Repl online received.");
				break;
			}
			log(message.toString());
			return ErrorCode.OK;
		}
		
		/**
		 * ????? ???????????? stream data message.
		 * Method processes stream data message.
		 * 
		 * @param msg
		 * @param index
		 */
		private void processStreamData(StreamDataMessage msg, int index)
		{
			Order order = null;
			
			// ???? ???? ?? ?????????????? ??????
			//
			// if one of the processing tables
			if (index == ordersLogTableIndex || index == ordersTableIndex) {
				order = new Order(msg, index);
			} else if (index == sysEventsTableIndex) {
				try {
					int sessId = msg.getField("sess_id").asInt();
					if (currSessionId < sessId) {
						if (currSessionId != 0) {
							restart(sessId);
						} else {
							currSessionId = sessId;
						}
					}
				} catch (CGateException ex) {
					ex.printStackTrace();
				}
			}
			if (order != null && order.getIsinId() == isinId) {
				
				// ?????????? ???????????? ??????
				//
				// skip nonsystem orders
				if ((order.getStatus() & STATUS_NONSYSTEM) != 0) {
					return;
				}

				// ?????????? ?????? ?? ????????? ??????
				//
				// skip order from previous sessions
				if (order.getSessId() < currSessionId) {
					return;
				}
				
				// if sessId is higher  or == 0 then set sessId of current session
				// and clean all lists, maps and change current session
				if (currSessionId == 0 || order.getSessId() > currSessionId) {
					restart(order.getSessId());
				}
				
				if (order.getDir() == Dir.BUY.getValue()) {
					transBuyOrders.add(order);
				} else {
					transSellOrders.add(order);
				}
				if (order.getAction() == ACTION_ADD
						&& order.getReplAct() == 0) {
					ordersBook.put(order.getIdOrd(), order);
				}
				if ((order.getStatus() & STATUS_COMMIT) != 0) {
					executeTransaction(transSellOrders, sellAggrOrders);
					executeTransaction(transBuyOrders, buyAggrOrders);
					return;
				}
				
				currRevision = order.replRev;
			}
		}
		
		/**
		 * ?????????? ???????? ?????????? ?????????. <br/>
		 * Process clear deleted message.
		 * 
		 * @param tableIndex
		 * @param clearDeletedRev
		 */
		private void processClearDeleted(int tableIndex, long clearDeletedRev) {
			// ???? clear deleted ??????? ?? ?????? ??????? - ??????????
			//
			// if another table - do not remove anything
			if (tableIndex != ordersLogTableIndex
					&& tableIndex != ordersLogTableIndex) {
				return;
			}

			removeTransOldRevMessages(transSellOrders, tableIndex,
					clearDeletedRev);
			removeTransOldRevMessages(transBuyOrders, tableIndex,
					clearDeletedRev);

			// ??????? ????????? ?? ??????? ? ??????????? ?????????????? ?????
			//
			// remove messages from orderBook and recalculate aggregated amount
			for (Iterator<Map.Entry<Long, Order>> iter = ordersBook
					.entrySet().iterator(); iter.hasNext();) {
				
				Order order = iter.next().getValue();
				if (order.getReplRev() < clearDeletedRev
						&& order.getTableIndex() == tableIndex) {
					
					if (order.getDir() == Dir.BUY.getValue()) {
						recalculateAggrOrder(buyAggrOrders,
								order.getPrice(),
								order.getAmountRest());
					} else {
						recalculateAggrOrder(sellAggrOrders,
								order.getPrice(),
								order.getAmountRest());
					}
					iter.remove();
				}
			}
		}
		
		/**
		 * ????? ??????? ?????? ?? ????????? ?????????????? ??????.
		 * ??? ?????? ?? ?????????? ? ???????? ??????? ?????? ??? ??????????
		 * ??? ?? ???? ?????????. <br/>
		 * 
		 * Method removes orders from temporary transaction lists . Orders from this
		 * lists aren't contained in ordersBook because transaction isn't
		 * completed yet and aren't calculated in aggregate order book.
		 * 
		 * @param tableIndex
		 * @param clearDeletedRev
		 * @param transOrders
		 */
		private void removeTransOldRevMessages(List<Order> transOrders,
				int tableIndex, long clearDeletedRev) {
			for (Iterator<Order> iterator = transOrders.iterator(); iterator
					.hasNext();) {
				Order order = iterator.next();
				if (order.getReplRev() < clearDeletedRev
						&& order.getTableIndex() == tableIndex) {
					iterator.remove();
				}
			}
		}

		/**
		 * ????? ???????????? ?????? ?? ????? ?????????? ??????????. <br/>
		 * Method processes new received transaction.
		 * 
		 * @param transOrders
		 * @param aggrOrders
		 */
		private void executeTransaction(List<Order> transOrders,
				Map<BigDecimal, Integer> aggrOrders) {
			for (Order item : transOrders) {
				if (item.getReplAct() != 0) {
					removeOrder(item, aggrOrders);
					break;
				}
				int action = item.getAction();
				switch (action) {
				case ACTION_ADD:
					addOrder(item, aggrOrders);
					break;
				case ACTION_UPDATE:
					updateOrder(item, aggrOrders);
					break;
				case ACTION_REMOVE:
					removeOrder(item, aggrOrders);
					break;
				}
			}
			transOrders.clear();
		}

		/**
		 * ????? ???????????? ?????? ?? ??????????. <br/>
		 * Method processes add order.
		 * 
		 * @param item
		 * @param ordersList
		 */
		private void addOrder(Order item, Map<BigDecimal, Integer> aggrOrders) {
			Integer aggrAmount = aggrOrders.get(item.getPrice());
			if (aggrAmount != null) {
				aggrAmount += item.getAmountRest();
				aggrOrders.put(item.getPrice(), aggrAmount);
			} else{
				aggrOrders.put(item.getPrice(), item.getAmountRest());
			}
		}

		/**
		 * ????? ???????????? ?????? ?? ?????????. <br/>
		 * Method processes update order.
		 * 
		 * @param updateOrder
		 * @param ordersList
		 */
		private void updateOrder(Order updateOrder,
				Map<BigDecimal, Integer> aggrOrders) {

			BigDecimal price = updateOrder.getPrice();

			Order oldOrder = ordersBook.get(updateOrder.getIdOrd());
			if (oldOrder == null) {
				log("Order with id_ord = " + updateOrder.getIdOrd()
						+ " not found");
				return;
			}

			int amountDiff = oldOrder.getAmountRest()
					- updateOrder.getAmountRest();
			// update old order from ordersBook
			oldOrder.setAmountRest(updateOrder.getAmountRest());
			if (oldOrder.getAmountRest() == 0) {
				ordersBook.remove(oldOrder.getIdOrd());
			}
			
			recalculateAggrOrder(aggrOrders, price, amountDiff);
		}

		/**
		 * ????? ???????????? ?????? ?? ????????. <br/>
		 * Method processes remove order.
		 * 
		 * @param removeOrder
		 * @param ordersList
		 */
		private void removeOrder(Order removeOrder,
				Map<BigDecimal, Integer> aggrOrders) {
			BigDecimal price = removeOrder.getPrice();
			Order oldOrder = ordersBook.remove(removeOrder.getIdOrd());
			if (oldOrder == null) {
				log("Order with id_ord = " + removeOrder.getIdOrd()
						+ " not found");
				return;
			}

			recalculateAggrOrder(aggrOrders, price, oldOrder.getAmountRest());
		}
		
		/**
		 * ????? ????????????? ?????????????? ????? ? ??????? ?????? ?? ????,
		 * ???? ?? ????? 0. <br/>
		 * Method recalculates aggregated amount and removes record from map if
		 * aggr amount equals 0.
		 * 
		 * @param commonOrder
		 * @param aggrOrders
		 */
		private void recalculateAggrOrder(
				Map<BigDecimal, Integer> aggrOrders, BigDecimal price,
				int diffAmount) {
			Integer aggrAmount = aggrOrders.get(price);

			if (aggrAmount == null) {
				log("Aggregated amount with isin_id = " + isinId + " for price = "
						+ price + " not found");
				return;
			}
			
			aggrAmount -= diffAmount;
			
			if (aggrAmount <= 0) {
				aggrOrders.remove(price);
			} else {
				aggrOrders.put(price, aggrAmount);
			}
		}
	}
	
	
	public void run(String[] args) throws CGateException {
		// clean before exit
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				resourcesFree();
			}
		});

		// ??????? ?????????? ????????? ??????
		//
		// command line arguments parsing
		CmdArgsParser argsParser = new CmdArgsParser();
		argsParser.parseInputParams(args);
		
		try {
			CGate.open("ini=jorderbook.ini;key=11111111");
			connection = new Connection(
					"p2tcp://127.0.0.1:4001;app_name=orderBook; timeout=1000");
			listener = new Listener(connection, "p2ordbook://FORTS_ORDLOG_REPL;snapshot=FORTS_FUTORDERBOOK_REPL", new Subscriber()); //FORTS_FUTCOMMON_REPL
			// ????? ??? ? ??????? ????????? ?????? ?? ???????? ??????. ???? ???? ?????? ??????? Enter - ????????? ?????? ? ????
			// 
			// Thread reads input data once per second. If Enter key was pressed orderbook will be written to file.
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						int key;
						while ((key = System.in.read()) != -1)
						if (key == ENTER_CODE) {
							writeOrderBook(outputFileName + "_" + Integer.toString(currSessionId) + "." + Long.toString(currRevision) + outputFileExt);
							log("Enter code = " + key);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, 1000);
			// ????????? ?????????? ? ?????
			//
			// working with connection in the loop
			while (true) {
				try {
					int state = connection.getState();
					switch (state) {
					case State.ERROR:
						connection.close();
						break;
					case State.CLOSED:
						try {
							connection.open(replState);
						} catch (CGateException ec) {
							System.err.println("Failed opening connection: "
									+ ec);
						} catch (Throwable ex) {
							ex.printStackTrace();
						}
						break;
					case State.ACTIVE:
						int result = connection.process(1);
						if (result != ErrorCode.OK
								&& result != ErrorCode.TIMEOUT) {
							System.err
									.println(String
											.format("Warning: connection state request failed: 0x%X",
													result));
						}

						try {
							int listenerState = listener.getState();
							switch (listenerState) {
							case State.ERROR:
								listener.close();
								break;
							case State.CLOSED:
								listener.open("");
								break;
							}
						} catch (CGateException el) {
							System.err.println("Failed working with listener: "
									+ el);
						}
						break;
					}
				} catch (Throwable ex) {
					ex.printStackTrace();
				}
			}
		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			resourcesFree();
		}
	}
	
	/**
	 * ????? ?????????? ??????? ??????, ? ???????? ?? ????????. <br/>
	 * Method defines indexes of tables which we process.
	 * 
	 * @throws ru.micexrts.cgate.CGateException
	 */
	private void initTableIndexes() throws CGateException {
		Scheme scheme = listener.getScheme();
		MessageDesc[] messageDescArray = scheme.getMessages();
		for (int count = 0; count < messageDescArray.length; count++) {
			MessageDesc messageDesc = messageDescArray[count];
			if ("orders_log".equals(messageDesc.getName())) {
				ordersLogTableIndex = count;
			} else if ("orders".equals(messageDesc.getName())) {
				ordersTableIndex = count;
			} else if ("sys_events".equals(messageDesc.getName())) {
				sysEventsTableIndex = count;
			}
		}
	}

	/**
	 * ?????? ??????? ? ????. <br/>
	 * Write order book to file.
	 */
	private void writeOrderBook(String name) {
		PrintStream saveStream = null;
		try {
			saveStream = new PrintStream(new File(name));
			saveStream.println("BUY:");

			List<BigDecimal> sortedPrices = new ArrayList<BigDecimal>(
					buyAggrOrders.keySet());
			if (reverse) {
				Collections.sort(sortedPrices);
			} else {
				Collections.sort(sortedPrices, Collections.reverseOrder());
			}
			writeOrderedList(saveStream, sortedPrices, buyAggrOrders);

			saveStream.println("SELL:");

			sortedPrices = new ArrayList<BigDecimal>(sellAggrOrders.keySet());
			if (reverse) {
				Collections.sort(sortedPrices, Collections.reverseOrder());
			} else {
				Collections.sort(sortedPrices);
			}

			writeOrderedList(saveStream, sortedPrices, sellAggrOrders);

		} catch (FileNotFoundException ex) {
			System.err.println(ex.getMessage());
			ex.printStackTrace();
		} finally {
			if (saveStream != null) {
				saveStream.close();
			}
		}
	}
	
	private void writeOrderedList(PrintStream saveStream,
			List<BigDecimal> sortedPrices, Map<BigDecimal, Integer> aggrOrders) {

		saveStream.println("\tISIN_ID " + isinId);

		for (int i = 0; i < sortedPrices.size(); i++) {
			if (depthOrderBook != 0 && i >= depthOrderBook) {
				break;
			}
			BigDecimal price = sortedPrices.get(i);
			saveStream.println("\t\tprice " + price + "\tamount "
					+ aggrOrders.get(price));
		}
	}
	
	
	/**
	 * Clean all lists.
	 * Method is called when new sessId received.
	 * 
	 * @param sessionId
	 */
	private void restart(int sessionId) {
		
		if (currSessionId > 0){
			writeOrderBook(outputFileName + "_" + Integer.toString(currSessionId) + outputFileExt);
		}
		
		transBuyOrders.clear();
		transSellOrders.clear();
		ordersBook.clear();
		buyAggrOrders.clear();
		sellAggrOrders.clear();		
		
		log("SessionId changed from " + this.currSessionId + " to " + sessionId);
		this.currSessionId = sessionId;
	}

	/**
	 * ????? ??????????? ??????? <br/>
	 * Method releases resources
	 */
	private void resourcesFree() {
		if (listener != null) {
			try {
				listener.close();
			} catch (CGateException cgex) {
			}
			try {
				listener.dispose();
			} catch (CGateException cgex) {
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (CGateException cgex) {
			}
			try {
				connection.dispose();
			} catch (CGateException cgex) {
			}
		}
		if (timer != null) {
			timer.cancel();
		}
	}
	
	private void log(String message) {
		ru.micexrts.cgate.impl.CGateImpl.log_infostr(message);
	}
	
	public static void main(String[] args) throws CGateException,
			InterruptedException, FileNotFoundException {
		new OrderBookSample().run(args);
	}
	
	/**
	 * ??????????????? ????? ??? ???????? ? ????????? ?????????? ?????????
	 * ??????. <br/>
	 * Helper class for parsing and validation of command line args.
	 * 
	 */
	class CmdArgsParser {
		/**
		 * ????? ?????? ????????? ????????? ?????? <br/>
		 * Method parses command line arguments
		 * 
		 * @param args
		 */
		private void parseInputParams(String[] args) {
			Map<String, String> inputParams = createInputParamsMap(args);
			parseIsinId(inputParams);
			parseDepthOrderBook(inputParams);
			verifyOutputFile(inputParams);
			parseReverseFlag(inputParams);
		}

		/**
		 * ????? ??????? ?? ??????? ?????????? ????????? ?????? ???? <br/>
		 * Method creates a map from command line args.
		 * 
		 * @param args
		 * @return
		 */
		private Map<String, String> createInputParamsMap(String[] args) {

			if (args.length < 3 || args.length > 4
					|| !args[0].contains("isinId=")
					|| !args[1].contains("depth=")
					|| !args[2].contains("fileName=")) {
				notifyArgsError();
			}

			if (args.length == 4 && !args[3].contains("r=")) {
				notifyArgsError();
			}

			Map<String, String> inputParams = new HashMap<String, String>();
			for (String arg : args) {
				String[] parts = arg.trim().split("=");
				if (parts.length == 2) {
					inputParams.put(parts[0], parts[1]);
				}
			}
			return inputParams;
		}

		/**
		 * ????? ??????????? ???????????? ? ??????? ??????? ?????????? ?????????
		 * ??????. <br/>
		 * Method describes a correct format of command line arguments.
		 */
		private String printUsage() {
			String msg = "Example:\n isinId= is unique instrument ID (e.g. 167566)\n"
					+ "depth= is max orderbook size (0 for unlimited size)\n"
					+ "fileName= is output file name\n"
					+ "r= is reverse sorting (optional, e.g. true or false)";
			log(msg);
			return msg;
		}
		
		private void notifyArgsError() {
			String msg = printUsage();
			throw new RuntimeException(msg);
		}

		/**
		 * ????? ?????? ? ????????? ?????????? ?????????????? ???????????,
		 * ??????????? ? ???????? ????????? ????????? ??????. <br/>
		 * Method parses and validates isinId from cmd line arguments
		 * 
		 * @param inputParams
		 */
		private void parseIsinId(Map<String, String> inputParams) {
			try {
				isinId = Integer.parseInt(inputParams.get(ISIN_ID_NAME_PARAM));
			} catch (NumberFormatException ex) {
				log("Incorrect input param " + ISIN_ID_NAME_PARAM);
				String usage = printUsage();
				log(usage);
				throw new RuntimeException("Incorrect input param "
						+ ISIN_ID_NAME_PARAM, ex);
			}

			if (isinId <= 0) {
				String error = "Incorrect input param: isinId value must be positive, isinId=" + isinId;
				log(error);
				throw new RuntimeException(error);
			}
		}

		/**
		 * ????? ?????? ? ????????? ?????????? ??????? ???????, ??????????? ?
		 * ???????? ????????? ????????? ??????. <br/>
		 * Method parses and validates order book depth, taken from command line
		 * args.
		 * 
		 * @param inputParams
		 */
		private void parseDepthOrderBook(Map<String, String> inputParams) {
			try {
				depthOrderBook = Integer.parseInt(inputParams
						.get(DEPTH_ORDER_BOOK_PARAM));
			} catch (NumberFormatException ex) {
				String error = "Incorrect input param "
						+ DEPTH_ORDER_BOOK_PARAM;
				log(error);
				String usage = printUsage();
				log(usage);
				throw new RuntimeException(error, ex);
			}

			if (depthOrderBook < 0) {
				String error = "Incorrect input param: isinId value must be positive, depth="
						+ depthOrderBook;
				log(error);
				throw new RuntimeException(error);
			}
		}

		/**
		 * ????? ????????? ?????????? ????? ????? ??? ???????? ???????,
		 * ??????????? ? ???????? ????????? ????????? ?????? <br/>
		 * Method validates file name for writing order book in (set in cmd line
		 * agrs).
		 * 
		 * @param inputParams
		 */
		private void verifyOutputFile(Map<String, String> inputParams) {

			String fileName = inputParams.get(SAVE_FILE_NAME_PARAM);
			if (fileName == null || fileName.isEmpty()) {
				String usage = printUsage();
				log(usage);
				throw new IllegalArgumentException("Incorrect input param "
						+ SAVE_FILE_NAME_PARAM);
			}
			outputFileName = "";
			outputFileExt = "";
			StringTokenizer st = new StringTokenizer(fileName, ".");
			if (st.countTokens() == 1)
			{
				outputFileName = fileName;
			}
			else if (st.countTokens() > 1)
			{
				outputFileName = st.nextToken();
				while (st.countTokens() != 1)
				{
					outputFileName += "." + st.nextToken();
				}
				outputFileExt = "." + st.nextToken();
			}
		}

		/**
		 * ????? ?????? ? ?????????? ???????? ????? reverse, ?????????? ?
		 * ?????????? ????????? ??????. <br/>
		 * Method parses and validates reverse flag from command line args.
		 * 
		 * @param inputParams
		 */
		private void parseReverseFlag(Map<String, String> inputParams) {
			String reverseInput = inputParams.get(REVERSE_PARAM);
			if (reverseInput != null && !reverseInput.isEmpty()) {
				reverse = Boolean.parseBoolean(reverseInput);
			}
		}
	}
	
	/**
	 * ????? ????? ?????? ??? ?????? ?? ?????? orders_log ? multileg_orders_log. <br/>
	 * Common order class for orders from orders_log and multileg_orders_log
	 * tables.
	 * 
	 */
	class Order {
		private long replID;
		private long idOrd;
		private long replAct;
		private int isinId;
		private BigDecimal price;
		private int sessId;
		private byte dir;
		private long replRev;
		private int amountRest;
		private int action;
		private int status;
		private int tableIndex;

		public Order(StreamDataMessage message, int tableIndex) {
			try {
				this.replID = message.getField("replID").asLong();
				this.idOrd = message.getField("id_ord").asLong();
				this.replAct = message.getField("replAct").asLong();
				this.isinId = message.getField("isin_id").asInt();
				this.price = message.getField("price").asBigDecimal();
				this.sessId = message.getField("sess_id").asInt();
				this.dir = message.getField("dir").asByte();
				this.replRev = message.getField("replRev").asLong();
				this.amountRest = message.getField("amount_rest").asInt();
				this.action = message.getField("action").asInt();
				this.status = message.getField("status").asInt();
				this.tableIndex = tableIndex;
			} catch (CGateException e) {
				e.printStackTrace();
			}
		}

		public long getReplID() {
			return replID;
		}

		public long getIdOrd() {
			return idOrd;
		}

		public long getReplAct() {
			return replAct;
		}

		public int getIsinId() {
			return isinId;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public int getSessId() {
			return sessId;
		}

		public byte getDir() {
			return dir;
		}

		public long getReplRev() {
			return replRev;
		}

		public int getAmountRest() {
			return amountRest;
		}

		public int getAction() {
			return action;
		}

		public int getStatus() {
			return status;
		}

		public void setAmountRest(int amountRest) {
			this.amountRest = amountRest;
		}
		
		public int getTableIndex() {
			return tableIndex;
		}

		@Override
		public String toString() {
			return "replId " + replID + " action " + action + "; dir " + dir
					+ "; sess_id " + sessId + "; isin_id " + isinId;
		}

	}

	/**
	 * Order direction (1 - buy, 2 - sell)
	 *
	 */
	enum Dir {

		BUY((byte) 1), SELL((byte) 2);

		private byte value;

		private Dir(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}
	};
}