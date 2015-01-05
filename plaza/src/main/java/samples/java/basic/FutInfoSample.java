package samples.java.basic;/*
        Copyright (c) 2012, MICEX-RTS. All rights reserved.

        Plaza-2 Client Gate API Usage Sample.
        Replication DataStream Client.

        All the software and documentation included in this and any
        other MICEX-RTS CGate Releasese is copyrighted by MICEX-RTS.

        Redistribution and use in source and binary forms, with or without
        modification, are permitted only by the terms of a valid
        software license agreement with MICEX-RTS.

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

/* 
	Repl allows to receive data replica for a stream and saves all incoming messages into log file.
	When disconnected, the replica starts over. The example does not have input parameters.
*/


import ru.micexrts.cgate.CGate;
import ru.micexrts.cgate.CGateException;
import ru.micexrts.cgate.Connection;
import ru.micexrts.cgate.ErrorCode;
import ru.micexrts.cgate.ISubscriber;
import ru.micexrts.cgate.Listener;
import ru.micexrts.cgate.State;
import ru.micexrts.cgate.messages.Message;

public class FutInfoSample {

	private class Subscriber implements ISubscriber{

		@Override
		public int onMessage(Connection conn, Listener listener, Message message) {
            String str = message.toString();
            if(str.indexOf("heartbeat") < 0 ){
                System.out.println(str);
            }



			return ErrorCode.OK;
		}
	}

	private Connection connection;
	private Listener listener;


	private static volatile boolean exitFlag = false;
	private static volatile boolean cleanedUp = false;

	private int stringIndex = 0;
	private static final int MaxStringIndex = 6;

	private static void ExitOnInvalidArg()
	{
		System.err.println("Please set a number between 0 and 6");
		throw new RuntimeException("Incorrect sample index.");
	}

	public FutInfoSample(int index)
	{
		if (index > MaxStringIndex)
			ExitOnInvalidArg();
		
		stringIndex = index;
	}

	public void run() throws CGateException {
		Runtime.getRuntime(). addShutdownHook(new Thread() {
			public void run() {
				exitFlag = true;
				while (!cleanedUp);
			}
		});

		try
		{
			CGate.open("ini=jrepl.ini;key=11111111");
			connection = new Connection("p2tcp://127.0.0.1:4001;app_name=jtest_repl");
			
/*
			// listener init string (you can uncomment any line
			// to experiment with various settings)

			String[] lsnStr = new String[MaxStringIndex + 1];
			
			lsnStr[0] = "p2ordbook://FORTS_ORDLOG_REPL;snapshot=FORTS_FUTORDERBOOK_REPL";

			// Defines Plaza-2 replication datastream listener
			// of stream named FORTS_FUTINFO_REPL
			lsnStr[1] = "p2repl://FORTS_FUTINFO_REPL;scheme=|FILE|ini/fut_info.ini|CustReplScheme";

			// Defines Plaza-2 replication datastream listener
			// of stream FORTS_FUTTRADE using custom data scheme
			lsnStr[2] = "p2repl://FORTS_FUTTRADE_REPL";
			lsnStr[3] = "p2repl://FORTS_FUTTRADE_REPL;scheme=|FILE|ini/fut_trades.ini|CustReplScheme";



            //

			// Defines Plaza-2 datastream listener, which provides
			// helper functions to receive order books. See documentation for
			// detailed description of this type of listener.
			lsnStr[4] = "p2ordbook://FORTS_FUTTRADE_REPL;snapshot=FORTS_FUTORDERBOOK_REPL;scheme=|FILE|ini/fut_trades.ini|CustReplScheme";
			lsnStr[5] = "p2ordbook://FORTS_ORDLOG_REPL;snapshot=FORTS_FUTORDERBOOK_REPL;scheme=|FILE|ini/ordLog_trades.ini|CustReplScheme";
			lsnStr[6] = "p2ordbook://FORTS_FUTTRADE_REPL;snapshot=FORTS_FUTORDERBOOK_REPL";
*/

			listener = new Listener(connection, "p2repl://FORTS_FUTTRADE_REPL", new Subscriber());

			while (!exitFlag) {

				int state = connection.getState();
				switch (state) {
					case State.ERROR:
						connection.close();
						break;
					case State.CLOSED:
						try {
							connection.open("");
						}
						catch (CGateException ec) {
							System.err.println("Failed opening connection: " + ec);
						}
						break;
					case State.ACTIVE:
						int result = connection.process(1);

						if (result != ErrorCode.OK && result != ErrorCode.TIMEOUT)
						{
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
						}
						catch (CGateException el) {
							System.err.println("Failed working with listener: " + el);
						}

						break;

				}
			}
		}
		catch (CGateException cgex) {
			System.out.println("Exception: " + cgex);
		}
		finally {
			if (listener != null) {
				try { listener.close(); } catch (CGateException cgex) {}
				try { listener.dispose(); } catch (CGateException cgex) {}
			}
			if (connection != null) {
				try { connection.close(); } catch (CGateException cgex) {}
				try { connection.dispose(); } catch (CGateException cgex) {}
			}
			CGate.close();
			cleanedUp = true;
		}
	}


	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws CGateException, InterruptedException{
		if ((args.length == 0) || (!args[0].contains("index=")))
			new FutInfoSample(0).run();
		else
		{
			int idx = 0;
			try
			{
				String[] parts = args[0].trim().split("=");
				if (parts.length == 2)
					idx = Integer.parseInt(parts[1]);
				else
					ExitOnInvalidArg();
			}
			catch (NumberFormatException ex)
			{
				ExitOnInvalidArg();
			}
			new FutInfoSample(idx).run();
		}
	}

}
