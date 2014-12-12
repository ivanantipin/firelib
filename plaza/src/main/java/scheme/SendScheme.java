package scheme;

public final class SendScheme
{


// ------------------------------------------
// Scheme "message"
// ------------------------------------------

    public static final class FutAddOrder
    {
        public static final int TABLE_INDEX = 0;
        public static final int MSG_ID = 36;
	public static final int MSG_SIZE = 148;
        private java.nio.ByteBuffer data;

        public FutAddOrder() {
            
        }

        public FutAddOrder(java.nio.ByteBuffer data) {
            this.data = data;
        }

        public java.nio.ByteBuffer getData() {
            return data;
        }

        public void setData(java.nio.ByteBuffer data) {
            this.data = data;
        }

    
        public String get_broker_code() {
            data.position(0);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 4);
        }

        public void set_broker_code(String val) {
            data.position(0);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 4);
        }

        
    
        public String get_isin() {
            data.position(5);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 25);
        }

        public void set_isin(String val) {
            data.position(5);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 25);
        }

        
    
        public String get_client_code() {
            data.position(31);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 3);
        }

        public void set_client_code(String val) {
            data.position(31);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 3);
        }

        
    
        public int get_type() {
            data.position(36);
            return data.getInt();
        }

        public void set_type(int val) {
            data.position(36);
            data.putInt(val);
        }

        
    
        public int get_dir() {
            data.position(40);
            return data.getInt();
        }

        public void set_dir(int val) {
            data.position(40);
            data.putInt(val);
        }

        
    
        public int get_amount() {
            data.position(44);
            return data.getInt();
        }

        public void set_amount(int val) {
            data.position(44);
            data.putInt(val);
        }

        
    
        public String get_price() {
            data.position(48);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 17);
        }

        public void set_price(String val) {
            data.position(48);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 17);
        }

        
    
        public String get_comment() {
            data.position(66);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 20);
        }

        public void set_comment(String val) {
            data.position(66);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 20);
        }

        
    
        public String get_broker_to() {
            data.position(87);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 20);
        }

        public void set_broker_to(String val) {
            data.position(87);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 20);
        }

        
    
        public int get_ext_id() {
            data.position(108);
            return data.getInt();
        }

        public void set_ext_id(int val) {
            data.position(108);
            data.putInt(val);
        }

        
    
        public int get_du() {
            data.position(112);
            return data.getInt();
        }

        public void set_du(int val) {
            data.position(112);
            data.putInt(val);
        }

        
    
        public String get_date_exp() {
            data.position(116);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 8);
        }

        public void set_date_exp(String val) {
            data.position(116);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 8);
        }

        
    
        public int get_hedge() {
            data.position(128);
            return data.getInt();
        }

        public void set_hedge(int val) {
            data.position(128);
            data.putInt(val);
        }

        
    
        public int get_dont_check_money() {
            data.position(132);
            return data.getInt();
        }

        public void set_dont_check_money(int val) {
            data.position(132);
            data.putInt(val);
        }

        
    
        public java.util.Date get_local_stamp() {
            data.position(136);
            return ru.micexrts.cgate.P2TypeParser.parseTimeAsDate(data);
        }

        public void set_local_stamp(java.util.Date val) {
            data.position(136);
            ru.micexrts.cgate.P2TypeComposer.composeDateAsTime(data, val);
        }

        
    


    }

    public static final class FORTS_MSG101
    {
        public static final int TABLE_INDEX = 1;
        public static final int MSG_ID = 101;
	public static final int MSG_SIZE = 268;
        private java.nio.ByteBuffer data;

        public FORTS_MSG101() {
            
        }

        public FORTS_MSG101(java.nio.ByteBuffer data) {
            this.data = data;
        }

        public java.nio.ByteBuffer getData() {
            return data;
        }

        public void setData(java.nio.ByteBuffer data) {
            this.data = data;
        }

    
        public int get_code() {
            data.position(0);
            return data.getInt();
        }

        public void set_code(int val) {
            data.position(0);
            data.putInt(val);
        }

        
    
        public String get_message() {
            data.position(4);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 255);
        }

        public void set_message(String val) {
            data.position(4);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 255);
        }

        
    
        public long get_order_id() {
            data.position(260);
            return data.getLong();
        }

        public void set_order_id(long val) {
            data.position(260);
            data.putLong(val);
        }

        
    


    }

    public static final class FORTS_MSG99
    {
        public static final int TABLE_INDEX = 2;
        public static final int MSG_ID = 99;
	public static final int MSG_SIZE = 144;
        private java.nio.ByteBuffer data;

        public FORTS_MSG99() {
            
        }

        public FORTS_MSG99(java.nio.ByteBuffer data) {
            this.data = data;
        }

        public java.nio.ByteBuffer getData() {
            return data;
        }

        public void setData(java.nio.ByteBuffer data) {
            this.data = data;
        }

    
        public int get_queue_size() {
            data.position(0);
            return data.getInt();
        }

        public void set_queue_size(int val) {
            data.position(0);
            data.putInt(val);
        }

        
    
        public int get_penalty_remain() {
            data.position(4);
            return data.getInt();
        }

        public void set_penalty_remain(int val) {
            data.position(4);
            data.putInt(val);
        }

        
    
        public String get_message() {
            data.position(8);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 128);
        }

        public void set_message(String val) {
            data.position(8);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 128);
        }

        
    
        public int get_code() {
            data.position(140);
            return data.getInt();
        }

        public void set_code(int val) {
            data.position(140);
            data.putInt(val);
        }

        
    


    }

    public static final class FORTS_MSG100
    {
        public static final int TABLE_INDEX = 3;
        public static final int MSG_ID = 100;
	public static final int MSG_SIZE = 260;
        private java.nio.ByteBuffer data;

        public FORTS_MSG100() {
            
        }

        public FORTS_MSG100(java.nio.ByteBuffer data) {
            this.data = data;
        }

        public java.nio.ByteBuffer getData() {
            return data;
        }

        public void setData(java.nio.ByteBuffer data) {
            this.data = data;
        }

    
        public int get_code() {
            data.position(0);
            return data.getInt();
        }

        public void set_code(int val) {
            data.position(0);
            data.putInt(val);
        }

        
    
        public String get_message() {
            data.position(4);
            return ru.micexrts.cgate.P2TypeParser.parseCXX(data, 255);
        }

        public void set_message(String val) {
            data.position(4);
            ru.micexrts.cgate.P2TypeComposer.composeCXX(data, val, 255);
        }

        
    


    }

}

