package firelib.common

import firelib.backtest.ParamsVariator
import org.junit.{Assert, Test}

import scala.collection.mutable

/**
 * Created by ivan on 7/29/14.
 */
class VariatorTest{

    @Test
    def TestOptVariator() =
    {
        val variator = new ParamsVariator(List(

            new OptimizedParameter("p0",0,2),
            new OptimizedParameter("p1",0,3)
        ))

        Assert.assertEquals(6,variator.Combinations);

        var set = new mutable.HashSet[(Int,Int)]() ++ List(
            (0,0),
            (0,1),
            (1,0),
            (1,1),
            (0,2),
            (1,2))

        var dd =  variator.Next
        while (dd != null)
        {
            var key = (dd("p0"), dd("p1"));
            Assert.assertTrue(set.contains(key));
            set -= key
            dd =  variator.Next
        }
        Assert.assertEquals(0,set.size);
    }

}
