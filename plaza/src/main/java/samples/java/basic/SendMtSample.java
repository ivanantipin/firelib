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
	Send_mt - ????????????? ?????? ??????? ??????. (??????????: ????????????? ?????? ??? ?????????????, ??????????????? C+
	+11.). ? ????? 1 ?????????? ??????.? ????? 2 ?????????????? reply ?? ?????????? ??????.

	Send_mt - a multistream example of sending order. (Attention! This example can be compiled only by compilers supporting C++11.)
	Stream 1 contains orders. Stream 2 contains replies for the orders sent.
*/


import ru.micexrts.cgate.CGate;
import ru.micexrts.cgate.CGateException;
import ru.micexrts.cgate.Connection;
import ru.micexrts.cgate.ISubscriber;
import ru.micexrts.cgate.Listener;
import ru.micexrts.cgate.MessageKeyType;
import ru.micexrts.cgate.MessageType;
import ru.micexrts.cgate.PublishFlag;
import ru.micexrts.cgate.Publisher;
import ru.micexrts.cgate.messages.DataMessage;
import ru.micexrts.cgate.messages.Message;
import scheme.SendScheme;

import java.nio.ByteBuffer;

public class SendMtSample 
{
	private class StartingPistol
	{
		private int counter;
		
		public StartingPistol(int initialValue)
		{
			counter = initialValue;
		}

		public synchronized void decrement()
		{
			counter--;
		}

		public synchronized int value()
		{
			return counter;
		}
	}
	
	private class ShutdownHook extends Thread
	{
		private volatile boolean exitFlag = false;
		private volatile boolean cleanedUp = false;
		
		public void run()
		{
			exitFlag = true;
			while (!cleanedUp);
		}
		
		public boolean getExitFlag()
		{
			return exitFlag;
		}
	}
	
	private class ReplyListener extends Thread implements ISubscriber 
	{
		private Connection connection;
		private Listener listener;
		private static final int defaultTimeout = 1;
		private ShutdownHook shutdownHook;
		private int cnt = 0;
		
		public ReplyListener(
				String connectionSettings, 
				String listenerSettings, 
				ShutdownHook hook) throws CGateException
		{
			connection = new Connection(connectionSettings);
			listener = new Listener(connection, listenerSettings, this);
			
			this.shutdownHook = hook;
		}
		
		public Connection getConnection()
		{
			return connection;
		}
		
		@Override
		public int onMessage(Connection conn, Listener listener, Message message) 
		{
			switch (message.getType())
			{
			case MessageType.MSG_DATA:
				DataMessage msgData = (DataMessage)message;
				cnt++;
				break;
			case MessageType.MSG_P2MQ_TIMEOUT:
				System.out.println("Timeout message");
				break;
			default:
				System.out.println(message.toString());
			}

			return 0;
		}
		
		public void close()
		{
			try
			{
				listener.close();
				listener.dispose();
				connection.close();
				connection.dispose();
			}
			catch (CGateException ex)
			{
				System.out.println("Exception: " + ex);
			}
		}
		
		private void processListener() throws CGateException
		{
			int state = listener.getState();
			
			if(state == ru.micexrts.cgate.State.ERROR)
			{
				listener.close();
			}
			else if (state == ru.micexrts.cgate.State.CLOSED)
			{
				listener.open();
			}
		}
		
		private void processConnection() throws CGateException
		{
			int state = connection.getState();
			if (state == ru.micexrts.cgate.State.ERROR)
			{
				connection.close();
			}
			else if (state == ru.micexrts.cgate.State.CLOSED)
			{
				connection.open();
			}
			else if (state == ru.micexrts.cgate.State.ACTIVE)
			{
				connection.process(defaultTimeout);
				processListener();
				
				
			}
		}
		
		public void run() 
		{
			long lastTime = System.currentTimeMillis();
			
			while(!shutdownHook.getExitFlag())
			{
				try
				{
					processConnection();
				}
				catch (CGateException ex)
				{
					System.out.println("Exception: " + ex);
				}
				
				if (System.currentTimeMillis()-lastTime >= 1000) 
				{
					System.out.println(String.format("Receive: messages per second: %d", cnt));
					cnt = 0;
					lastTime = System.currentTimeMillis();
				}
			}
		}
	}
	
	private class MessagePublisher extends Thread
	{
		private int cnt = 0;
		
		private StartingPistol startingPistol;
		ShutdownHook shutdownHook;
		
		private Connection connection;
		private Publisher publisher;
		
		private SendScheme.FutAddOrder cFutAddOrder = new SendScheme.FutAddOrder();
		
		public MessagePublisher(
				Connection conn, 
				String publisherSettings,
				ShutdownHook hook, 
				StartingPistol pistol) throws CGateException
		{
			this.connection = conn;
			this.publisher = new Publisher(this.connection, publisherSettings);
			this.shutdownHook = hook;
			this.startingPistol = pistol;
		}
		
		public void close() throws CGateException
		{
			publisher.close();
			publisher.dispose();
		}
		
		private void sendRequest(int counter) throws CGateException {
			
			DataMessage message = (DataMessage)publisher.newMessage(
				MessageKeyType.KEY_NAME, "FutAddOrder");
			
			try
			{
				ByteBuffer data = message.getData();
				message.setUserId(counter);
				cFutAddOrder.setData(data);

				cFutAddOrder.set_broker_code("HB00");
				cFutAddOrder.set_client_code("000");
				cFutAddOrder.set_isin("RTS-6.12");
				cFutAddOrder.set_dir(1);
				cFutAddOrder.set_type(1);
				cFutAddOrder.set_amount(1);
				cFutAddOrder.set_price("1700");
				cFutAddOrder.set_ext_id(0);
				
				publisher.post(message, PublishFlag.NEED_REPLY);
			}
			finally
			{
				cFutAddOrder.setData(null);
				message.dispose();
			}
		}
		
		private void doLogic()
		{
			try
			{
				sendRequest(cnt);
				cnt++;
			}
			catch (CGateException ex)
			{
				System.out.println("Exception: " + ex);
			}
		}
		
		private void processPublisher() throws CGateException
		{
			if (connection.getState() != ru.micexrts.cgate.State.ACTIVE)
			{
				return;
			}

			int state = publisher.getState();
			if (state == ru.micexrts.cgate.State.ERROR)
			{
				publisher.close();
			}
			else if (state == ru.micexrts.cgate.State.CLOSED)
			{
				publisher.open();
			}
			else if (state == ru.micexrts.cgate.State.ACTIVE)
			{
				doLogic();
			}
		}
		
		public void run()
		{
			startingPistol.decrement();
			while(startingPistol.value() != 0);
			
			long lastTime = System.currentTimeMillis();
			
			while (!shutdownHook.getExitFlag())
			{
				try
				{
					processPublisher();
				}
				catch (CGateException ex)
				{
					System.out.println("Exception: " + ex);
				}
				
				if (System.currentTimeMillis() - lastTime >= 1000) 
				{
					System.out.println(String.format("Post: messages per second: %d", cnt));
					cnt = 0;
					lastTime = System.currentTimeMillis();
				}
			}
		}
		
	}
	
	final int nThreads = 2;
	
	public void run() throws CGateException, InterruptedException
	{
		CGate.open("ini=jsend_mt.ini;key=11111111");
		
		ShutdownHook shutdownHook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	
		StartingPistol startingPistol = new StartingPistol(nThreads);
		ReplyListener replyListener = new ReplyListener(
			"p2tcp://127.0.0.1:4001;app_name=send_mt", 
			"p2mqreply://;ref=srvlink0", 
			shutdownHook);
		
		MessagePublisher publishers[] = new MessagePublisher[nThreads];
		for (int i = 0; i < nThreads; ++i)
		{
			publishers[i] = new MessagePublisher(
				replyListener.getConnection(),
				String.format(
					"p2mq://FORTS_SRV;category=FORTS_MSG;name=srvlink%d;timeout=5000;scheme=|FILE|forts_messages.ini|message", i),
				shutdownHook,
				startingPistol);
		}
		System.out.println("All threads started");
		
		try
		{
			replyListener.start();
			
			for (int i = 0; i < nThreads; ++i)
			{
				publishers[i].start();
			}
		
			replyListener.join();
			for (int i = 0; i < nThreads; ++i)
			{
				publishers[i].join();
			}
			System.out.println("All threads finished");
		}
		catch (InterruptedException ex)
		{
			System.out.println("Finish: " + ex);
		}
		finally
		{
			for (int i = 0; i < nThreads; ++i)
			{
				publishers[i].close();
			}
			replyListener.close();
			CGate.close();
		}
	}
	
	public static void main(String[] args) throws CGateException, InterruptedException 
	{
		new SendMtSample().run();
	}

}
