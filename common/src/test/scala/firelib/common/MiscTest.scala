package firelib.common

import java.time.Instant

import org.junit.{Assert, Test}

/**
 * Created by ivan on 9/6/14.
 */
class MiscTest {

    @Test
    def testPosAdj(): Unit ={
        val order: Order = new Order(OrderType.Market, 1.0, 10,Side.Buy)
        val trade: Trade = new Trade(5, 1.0, Side.Buy, order, Instant.now(), "")
        val pos = trade.adjustPositionByThisTrade(10)
        Assert.assertEquals(pos , 15)
    }

}
