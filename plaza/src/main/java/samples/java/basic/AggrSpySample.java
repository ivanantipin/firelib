package samples.java.basic;/*
 Copyright (c) 2014, MOEX. All rights reserved.

 Plaza-2 Client Gate API Usage Sample.
 Process orders log stream.

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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ?????? ??????? ?????????? ????????? ??????????????? ??????? ??? ?????????????
 * ??????????? ? isin_id, ???????? ? ????????? ??????.
 * ??????? ?????????:
 * - isin_id (????????????? ???????????), 
 * - ???????????? ?????????? ????????? ?????? (??????? ???????, 0 - ??? ?????? ??? ???????????), 
 * - ???????? ????????? ????? ? 
 * - ??????? ??????????? ?????????? (r).
 * 
 * This sample represents getting an aggregate order book for specific
 * instrument which isin_id is set in command line arguments.
 * Input arguments are:
 *  - isin_id
 *  - order book depth (max quantity of elements in output, 0 for unlimited output), 
 *  - output file name.
 *  - reverse sorting flag
 *
 */
public class AggrSpySample {

	// ????? ?????????? ????????? ??????
	//
	// input params names
	private static final String ISIN_ID_NAME_PARAM = "isinId";
	private static final String DEPTH_ORDER_BOOK_PARAM = "depth";
	private static final String SAVE_FILE_NAME_PARAM = "fileName";
	private static final String REVERSE_PARAM = "r";
	
	// ??? ?????? Enter, ??? ??????? ?? ??????? ?????????? ???????? ??????? ? ????
	//
	// Enter key code, on Enter pressed orderbook is written to file 
	private static final int ENTER_CODE = 10;

	// ?????? ???? ??? ?????
	//
	// data format for logs
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy hh.mm.ss.sss");

	// timer to listen input stream
	Timer timer = new Timer();

	private Connection connection;
	private Listener listener;
	
	// ?????? ??? ???????????? ?????? ?? ???????????? ?????????
	//
	// value to reopen stream from saved state
	private String replState = "";
	
	// aggregate order books
	// Map<price, aggrData>
	private Map<BigDecimal, AggrData> orderBookBuy = new HashMap<BigDecimal, AggrData>();
	private Map<BigDecimal, AggrData> orderBookSell = new HashMap<BigDecimal, AggrData>();

	// indexes of table orders_aggr, initialized on connection opening
	private int ordersAggrTableIndex;
	
	// ??????? ??????, ??? ????? ?????? ?????? ???????????? ? ????
	//
	// current session, on session change order book is written to file
	private int currSessionId = 0;
	// ????? ??????? ???????
	//
	// current revision number
	private static long currRevision = 0;

	// ????????????? ???????????
	//
	// identifier instrument
	private static int isinId;
	// ??????? ???????
	//
	// order book depth
	private static int depthOrderBook;
	// ??? ????????? ?????
	//
	// output file name
	private static String outputFileName;
	private static String outputFileExt;
	// ???? ???????? ??????????
	//
	// reverse sorting flag
	private static boolean reverse = false;

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

			case MessageType.MSG_OPEN:
				log("Stream opened.");
				// ? ?????? ?????????????????
				//
				// in case of reconnect
				cleanup();
				checkScheme();
				break;
				
			// ????????? ? ???????? ??????
			//
			// close stream message processing
			case MessageType.MSG_CLOSE:
				writeOrderBook(outputFileName + outputFileExt);
				log("Stream closed.");
				break;

			case MessageType.MSG_STREAM_DATA:
				StreamDataMessage msg = (StreamDataMessage) message;
				if (msg.getMsgIndex() != ordersAggrTableIndex) {
					break;
				}
				
				try {
					int sessId = msg.getField("sess_id").asInt();

					// ??????? ????????? ?? ?????????? ??????
					//
					// skip messages from older sessions
					if (sessId < currSessionId) {
						break;
					}

					if (sessId > currSessionId) {
						if (currSessionId != 0) {
							writeOrderBook(outputFileName + "_" + Integer.toString(currSessionId) + outputFileExt);
							cleanup();
						}
						currSessionId = sessId;
					}

					// ????????? ???????? ? ??????????? ?? ???????????
					//
					// aggregate processing depends on direction
					int dir = msg.getField("dir").asInt();
					if (dir == Dir.BUY.getValue()) {
						updateOrderBook(msg, orderBookBuy);
					} else {
						updateOrderBook(msg, orderBookSell);
					}
				} catch (CGateException ex) {
					ex.printStackTrace();
				}
				break;

			// ???????? ??? ?????? ??? ????? ?????? ?????
			//
			// clear all data when life number is changed
			case MessageType.MSG_P2REPL_LIFENUM:
				P2ReplLifeNumMessage p2ReplLifeNumMessage = (P2ReplLifeNumMessage) message;
				long lifeNum = p2ReplLifeNumMessage.getLifenum();
				log("Life number changed. New life number = " + lifeNum);
				cleanup();
				currSessionId = 0;
				break;

			// ???????? ???????? ?????????? ??????
			//
			// message about mass deletion of old records
			case MessageType.MSG_P2REPL_CLEARDELETED:
				P2ReplClearDeletedMessage p2ReplClearDeletedMessage = (P2ReplClearDeletedMessage) message;
				long clearDeletedRev = p2ReplClearDeletedMessage.getTableRev();
				log("Clear old orders. ClearDeletedRev = " + clearDeletedRev);
				clearOldRevs(orderBookBuy, clearDeletedRev);
				clearOldRevs(orderBookSell, clearDeletedRev);
				break;

			// ????????? ? ?????? ?????? ? online
			//
			// repl online message
			case MessageType.MSG_P2REPL_ONLINE:
				log("Repl is online");
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

			}
			log(message.toString());
			return ErrorCode.OK;
		}
		
		/**
		 * ????? ???????????? ????????? ? ???????? ?????? <br/>
		 * Method handles orderbook
		 * 
		 * @param message
		 * @param orderBook
		 */
		private void updateOrderBook(StreamDataMessage message, Map<BigDecimal, AggrData> orderBook) {
			try {
				BigDecimal price = message.getField("price").asBigDecimal();
				int volume = message.getField("volume").asInt();
				long replRev = message.getField("replRev").asLong();
				
				if (price.equals(BigDecimal.ZERO) || volume == 0) {
					orderBook.remove(price);
				} else {
					orderBook.put(price, new AggrData(replRev, volume));
				}
				currRevision = replRev;
			} catch (CGateException e) {
				e.printStackTrace();
			}
		}
		
		private void checkScheme(){
			// find indexes for tables
			try {
				Scheme scheme = listener.getScheme();
				MessageDesc[] messageDescArray = scheme.getMessages();
				if (messageDescArray.length != 1) {
					log("Incorrect scheme.");
					throw new RuntimeException("Incorrect scheme.");
				}
			} catch (CGateException ex) {
				ex.printStackTrace();
				log(ex.getMessage());
			}
		}

		/**
		 * ????? ????????? ????????? MSG_P2REPL_CLEARDELETED <br/>
		 * Method for MSG_P2REPL_CLEARDELETED message processing
		 * 
		 * @param orderBookMap
		 * @param clearDeletedRev
		 */
		private void clearOldRevs(Map<BigDecimal, AggrData> orderBookMap, long clearDeletedRev) {
			for (Map.Entry<BigDecimal, AggrData> priceItem : orderBookMap.entrySet()) {
				if (priceItem.getValue().getReplRev() < clearDeletedRev) {
					orderBookMap.remove(priceItem.getKey());
				}
			}
		}
		
		/**
		 * ??????? ???? ??????? <br/>
		 * ????? ?????????? ??? ?????? ????? ?????? <br/>
		 * clean all lists <br/>
		 * method is called when new session started <br/>
		 * 
		 */
		private void cleanup() {
			orderBookBuy.clear();
			orderBookSell.clear();
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
		// cmd line arguments parsing
		CmdArgsParser argsParser = new CmdArgsParser();
		argsParser.parseInputParams(args);

		try {
			CGate.open("ini=jaggr.ini;key=11111111");
			connection = new Connection("p2tcp://127.0.0.1:4001;app_name=aggrSpy; timeout=1000");
			listener = new Listener(connection, "p2repl://FORTS_FUTAGGR50_REPL", new Subscriber());
			
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
								System.err.println("Failed opening connection: " + ec);
							} catch (Throwable ex) {
								ex.printStackTrace();
							}
							break;
						case State.ACTIVE:
							int result = connection.process(1);
							if (result != ErrorCode.OK && result != ErrorCode.TIMEOUT) {
								System.err.println(String.format("Warning: connection state request failed: 0x%X", result));
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
								System.err.println("Failed working with listener: " + el);
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
	 * ????? ??????????? ??????? <br/>
	 * Method releases resources.
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
				cgex.printStackTrace();
			}
			try {
				connection.dispose();
			} catch (CGateException cgex) {
				cgex.printStackTrace();
			}
		}
	}

	/**
	 * ????? ?????????? ?????? ? ???? <br/>
	 * write order book data to file
	 */
	private void writeOrderBook(String name) {
		PrintStream aggrStream = null;
		try {
			
			aggrStream = new PrintStream(new File(name));
			aggrStream.println("BUY:");
			
			List<BigDecimal> sortedPrices = new ArrayList<BigDecimal>(orderBookBuy.keySet());
			if (reverse) {
				Collections.sort(sortedPrices);
			} else {
				Collections.sort(sortedPrices, Collections.reverseOrder());
			}
			writeOrderedList(aggrStream, sortedPrices, orderBookBuy);
			
			aggrStream.println("SELL:");
			
			sortedPrices = new ArrayList<BigDecimal>(orderBookSell.keySet());
			if (reverse) {
				Collections.sort(sortedPrices, Collections.reverseOrder());
			} else {
				Collections.sort(sortedPrices);
			}
			writeOrderedList(aggrStream, sortedPrices, orderBookSell);
			
			aggrStream.println();
			aggrStream.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} finally {
			if(aggrStream != null){
				aggrStream.close();
			}
		}
	}
	
	private void writeOrderedList(PrintStream aggrStream, List<BigDecimal> prices, Map<BigDecimal, AggrData> orderBook) {
		
		aggrStream.println("\tISIN_ID " + isinId);
		
		for (int i = 0; i < prices.size(); i++) {
			if (depthOrderBook != 0 && i >= depthOrderBook) {
				break;
			}
			BigDecimal price = prices.get(i);
			aggrStream.println("\t\tprice " + price + "\tamount " + orderBook.get(price));
		}
	}
	
	private void log(String message) {
		ru.micexrts.cgate.impl.CGateImpl.log_infostr(message);
	}

	public static void main(String[] args) throws CGateException, InterruptedException, FileNotFoundException {
		new AggrSpySample().run(args);
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
	 * ????? ??? ?????????????? ?????? <br/>
	 * class for aggregates
	 *
	 */
	private class AggrData {

		private long replRev;
		private int volume;

		private AggrData(long replRev, int volume) {
			super();
			this.replRev = replRev;
			this.volume = volume;
		}

		public long getReplRev() {
			return replRev;
		}

		@Override
		public String toString() {
			return String.valueOf(volume);
		}

	}
	
	/**
	 * ??????????? ?????? (1 - ???????, 2 - ???????) <br/>
	 * order direction (1 - buy, 2 - sell)
	 *
	 */
	enum Dir {

		BUY(1), SELL(2);

		private int value;

		private Dir(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	};
	
}