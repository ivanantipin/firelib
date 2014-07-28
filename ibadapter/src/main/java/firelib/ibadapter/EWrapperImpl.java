package firelib.ibadapter;

import com.ib.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EWrapperImpl implements EWrapper 
    {
        protected EClientSocket clientSocket;

        Logger log = LoggerFactory.getLogger(getClass());

       
        public EWrapperImpl()
        {
            clientSocket = new EClientSocket(this);
        }

/*
        public EClientSocket ClientSocket
        {
            get { return clientSocket; }
            set { clientSocket = value; }
        }
*/

        public  void error(Exception e)
        {
            log.error("Error: " + e,e);
        }
        
        public  void error(String str)
        {
            log.error("Error: " + str);
        }
        
        public  void error(int id, int errorCode, String errorMsg)
        {
            log.error("Error. Id: " + id + ", Code: " + errorCode + ", Msg: " + errorMsg + "\n");
        }
        
        public  void connectionClosed()
        {
            log.info("Connection closed.\n");
        }
        
        public  void currentTime(long time) 
        {
            log.info("Current Time: " + time + "\n");
        }

        public  void tickPrice(int tickerId, int field, double price, int canAutoExecute) 
        {
          //  Console.WriteLine("Tick Price. Ticker Id:"+tickerId+", Field: "+field+", Price: "+price+", CanAutoExecute: "+canAutoExecute+"\n");
        }
        
        public  void tickSize(int tickerId, int field, int size)
        {
//            Console.WriteLine("Tick Size. Ticker Id:" + tickerId + ", Field: " + field + ", Size: " + size+"\n");
        }
        
        public  void tickString(int tickerId, int tickType, String value)
        {
  //          Console.WriteLine("Tick String. Ticker Id:" + tickerId + ", Type: " + tickType + ", Value: " + value+"\n");
        }

        public  void tickGeneric(int tickerId, int field, double value)
        {
    //        Console.WriteLine("Tick Generic. Ticker Id:" + tickerId + ", Field: " + field + ", Value: " + value+"\n");
        }

        public  void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureExpiry, double dividendImpact, double dividendsToExpiry)
        {
            //Console.WriteLine("TickEFP. "+tickerId+", Type: "+tickType+", BasisPoints: "+basisPoints+", FormattedBasisPoints: "+formattedBasisPoints+", ImpliedFuture: "+impliedFuture+", HoldDays: "+holdDays+", FutureExpiry: "+futureExpiry+", DividendImpact: "+dividendImpact+", DividendsToExpiry: "+dividendsToExpiry+"\n");
        }

        public  void tickSnapshotEnd(int tickerId)
        {
            //Console.WriteLine("TickSnapshotEnd: "+tickerId+"\n");
        }

        public  void nextValidId(int orderId) 
        {
            //Console.WriteLine("Next Valid Id: "+orderId+"\n");
        }

        public  void deltaNeutralValidation(int reqId, UnderComp underComp)
        {
            //Console.WriteLine("DeltaNeutralValidation. "+reqId+", ConId: "+underComp.ConId+", Delta: "+underComp.Delta+", Price: "+underComp.Price+"\n");
        }

        public  void managedAccounts(String accountsList) 
        {
            log.info("Account list: "+accountsList+"\n");
        }

        public  void tickOptionComputation(int tickerId, int field, double impliedVolatility, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice)
        {
            //Console.WriteLine("TickOptionComputation. TickerId: "+tickerId+", field: "+field+", ImpliedVolatility: "+impliedVolatility+", Delta: "+delta
            //                  +", OptionPrice: "+optPrice+", pvDividend: "+pvDividend+", Gamma: "+gamma+", Vega: "+vega+", Theta: "+theta+", UnderlyingPrice: "+undPrice+"\n");
        }

        public  void accountSummary(int reqId, String account, String tag, String value, String currency)
        {
            log.info("Acct Summary. ReqId: " + reqId + ", Acct: " + account + ", Tag: " + tag + ", Value: " + value + ", Currency: " + currency + "\n");
        }

        public  void accountSummaryEnd(int reqId)
        {
            log.info("AccountSummaryEnd. Req Id: " + reqId + "\n");
        }

        @Override
        public void verifyMessageAPI(String apiData) {

        }

        @Override
        public void verifyCompleted(boolean isSuccessful, String errorText) {

        }

        @Override
        public void displayGroupList(int reqId, String groups) {

        }

        @Override
        public void displayGroupUpdated(int reqId, String contractInfo) {

        }

        public  void updateAccountValue(String key, String value, String currency, String accountName)
        {
            log.info("UpdateAccountValue. Key: " + key + ", Value: " + value + ", Currency: " + currency + ", AccountName: " + accountName + "\n");
        }

        public  void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealisedPNL, double realisedPNL, String accountName)
        {
            log.info("UpdatePortfolio. "+contract.m_symbol+", "+contract.m_secType +" @ "+contract.m_exchange
                              +": Position: "+position+", MarketPrice: "+marketPrice+", MarketValue: "+marketValue+", AverageCost: "+averageCost
                              +", UnrealisedPNL: "+unrealisedPNL+", RealisedPNL: "+realisedPNL+", AccountName: "+accountName+"\n");
        }

        public  void updateAccountTime(String timestamp)
        {
            log.info("UpdateAccountTime. Time: " + timestamp + "\n");
        }

        public  void accountDownloadEnd(String account)
        {
            log.info("Account download finished: " + account + "\n");
        }

        public  void orderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld)
        {
            
        }

        public  void openOrder(int orderId, Contract contract, Order order, OrderState orderState)
        {
            log.info("OpenOrder. ID: " + orderId + ", " + contract.m_symbol + ", " + contract.m_secType + " @ " + contract.m_exchange + ": " + order.m_action + ", " + order.m_orderType + " " + order.m_totalQuantity + ", " + orderState.m_status + "\n");
        }

        public  void openOrderEnd()
        {
            
        }

        public  void contractDetails(int reqId, ContractDetails contractDetails)
        {
            log.info("ContractDetails. ReqId: " + reqId + " - " + contractDetails.m_summary.m_symbol + ", " + contractDetails.m_summary.m_secType + ", ConId: " + contractDetails.m_summary.m_conId + " @ " + contractDetails.m_summary.m_exchange + "\n");
        }

        @Override
        public void bondContractDetails(int reqId, ContractDetails contractDetails) {

        }

        public  void contractDetailsEnd(int reqId)
        {
            log.info("ContractDetailsEnd. " + reqId + "\n");
        }

        public  void execDetails(int reqId, Contract contract, Execution execution)
        {
            log.info("ExecDetails. " + reqId + " - " + contract.m_symbol + ", " + contract.m_secType + ", " + contract.m_currency + " - " + execution.m_execId + ", " + execution.m_orderId + ", " + execution.m_shares);
        }

        public  void execDetailsEnd(int reqId)
        {
            log.info("ExecDetailsEnd. " + reqId + "\n");
        }

        public  void commissionReport(CommissionReport commissionReport)
        {
            log.info("CommissionReport. " + commissionReport.m_execId + " - " + commissionReport.m_commission + " " + commissionReport.m_currency + " RPNL " + commissionReport.m_realizedPNL + "\n");
        }

        @Override
        public void position(String account, Contract contract, int pos, double avgCost) {

        }

        public  void fundamentalData(int reqId, String data)
        {
            log.info("FundamentalData. " + reqId + "" + data + "\n");
        }

        public  void historicalData(int reqId, String date, double open, double high, double low, double close, int volume, int count, double WAP, boolean hasGaps)
        {
            //Console.WriteLine("HistoricalData. "+reqId+" - Date: "+date+", Open: "+open+", High: "+high+", Low: "+low+", Close: "+close+", Volume: "+volume+", Count: "+count+", WAP: "+WAP+", HasGaps: "+hasGaps+"\n");
        }

        public void historicalDataEnd(int reqId, String start, String end)
        {
            
        }

        public  void marketDataType(int reqId, int marketDataType)
        {
            //WARN: when we request this, we never send a requestId
            //This is also not returning anything when invoked
            //Console.WriteLine("MarketDataType. "+reqId+", Type: "+marketDataType+"\n");
        }

        public  void updateMktDepth(int tickerId, int position, int operation, int side, double price, int size)
        {
            //Console.WriteLine("UpdateMarketDepth. " + tickerId + " - Position: " + position + ", Operation: " + operation + ", Side: " + side + ", Price: " + price + ", Size" + size+"\n");
        }

        //WARN: Could not test!
        public  void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, int size)
        {
            //Console.WriteLine("UpdateMarketDepthL2. " + tickerId + " - Position: " + position + ", Operation: " + operation + ", Side: " + side + ", Price: " + price + ", Size" + size+"\n");
        }

        //WARN: Could not test!
        public  void updateNewsBulletin(int msgId, int msgType, String message, String origExchange)
        {
            //Console.WriteLine("News Bulletins. "+msgId+" - Type: "+msgType+", Message: "+message+", Exchange of Origin: "+origExchange+"\n");
        }

        public  void position(String account, Contract contract, int pos)
        {
            log.info("Position. " + account + " - Symbol: " + contract.m_symbol + ", SecType: " + contract.m_secType + ", Currency: " + contract.m_currency + ", Position: " + pos + "\n");
        }

        public  void positionEnd()
        {
         //   Console.WriteLine("PositionEnd \n");
        }

        public  void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume, double WAP, int count)
        {
            //Console.WriteLine("RealTimeBars. " + reqId + " - Time: " + time + ", Open: " + open + ", High: " + high + ", Low: " + low + ", Close: " + close + ", Volume: " + volume + ", Count: " + count + ", WAP: " + WAP+"\n");
        }

        public  void scannerParameters(String xml)
        {
         //   Console.WriteLine("ScannerParameters. "+xml+"\n");
        }

        public  void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr)
        {
            //Console.WriteLine("ScannerData. "+reqId+" - Rank: "+rank+", Symbol: "+contractDetails.Summary.Symbol+", SecType: "+contractDetails.Summary.SecType+", Currency: "+contractDetails.Summary.Currency
              //                +", Distance: "+distance+", Benchmark: "+benchmark+", Projection: "+projection+", Legs String: "+legsStr+"\n");
        }

        public  void scannerDataEnd(int reqId)
        {
            //Console.WriteLine("ScannerDataEnd. "+reqId+"\n");
        }

        public  void receiveFA(int faDataType, String faXmlData)
        {
         //   Console.WriteLine("Receing FA: "+faDataType+" - "+faXmlData+"\n");
        }
    }
