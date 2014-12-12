package samples.java.basic;

import ru.micexrts.cgate.*;
import ru.micexrts.cgate.messages.*;
import scheme.SendScheme;

import java.nio.ByteBuffer;


public class SendSample {


	private class ReplyListener implements ISubscriber{

		@Override
		public int onMessage(Connection conn, Listener listener, Message message) {
			switch (message.getType())
			{
			case MessageType.MSG_DATA:
				DataMessage msgData = (DataMessage)message;
				System.out.println(String.format("Reply message. ID=%d, User-Id=%d, content=%s", 
							msgData.getMsgId(), msgData.getUserId(), message));
				break;
			case MessageType.MSG_P2MQ_TIMEOUT:
				System.out.println("Timeout message");
				break;
			default:
				System.out.println(message);
			}



			
            return 0;
		}					
	}
	
	private Connection connection;
	private Publisher publisher;
	private Listener listener;
	private SendScheme.FutAddOrder cFutAddOrder = new SendScheme.FutAddOrder();
	

	public void sendRequest(int counter) throws CGateException {
						
		DataMessage message = (DataMessage)publisher.newMessage(MessageKeyType.KEY_NAME, "FutAddOrder");
		
		try{
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
		finally{
			cFutAddOrder.setData(null);
			message.dispose();
		}
	}
	
	private static volatile boolean exitFlag = false;
	private static volatile boolean cleanedUp = false;
	
	public void run() throws CGateException {	
		Runtime.getRuntime(). addShutdownHook(new Thread() {
			public void run() {
				exitFlag = true;
				while (!cleanedUp);
			}
		});

		try
		{
			CGate.open("ini=jsend.ini;key=11111111");
			connection = new Connection("p2tcp://127.0.0.1:4001;app_name=jtest_send");
			publisher = new Publisher(connection, "p2mq://FORTS_SRV;category=FORTS_MSG;timeout=5000;scheme=|FILE|forts_messages.ini|message;name=cmd");
		
			listener = new Listener(connection, "p2mqreply://;ref=cmd", new ReplyListener());
			
			connection.open("");

			// wait for connection to become ready
			while (true) {
				int state = connection.getState();
				if (state == State.ERROR)
					throw new RuntimeException("Connection failed");
				else
				if (state == State.ACTIVE)
					break;
			}

			
			publisher.open("");
			listener.open("");
			
			long lastTime = 0;
			int counter = 0;
			while (!exitFlag) {
			
				int err = connection.process(0);
				if (err != ErrorCode.OK && err != ErrorCode.TIMEOUT)
					throw new RuntimeException("Failed to process connection");
			
				if (System.currentTimeMillis()-lastTime > 1000) {
					sendRequest(counter ++);
					lastTime = System.currentTimeMillis();
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
			if (publisher != null) {
				try { publisher.close(); } catch (CGateException cgex) {}
				try { publisher.dispose(); } catch (CGateException cgex) {}
			}
			if (connection != null) {
				try { connection.close(); } catch (CGateException cgex) {}
				try { connection.dispose(); } catch (CGateException cgex) {}
			}
			CGate.close();
			cleanedUp = true;
		}
	}
	
	public static void main(String[] args) throws CGateException, InterruptedException{
				
		new SendSample().run();
	}


}
