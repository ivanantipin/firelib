package firelib.common

import firelib.common.misc.HeapQuantile
import org.apache.commons.math3.distribution.NormalDistribution
import org.junit.{Assert, Test}

class QuantilesTest
    {
        @Test
        def TestHeapOnNormal() =
        {

            var calc = new HeapQuantile(0.1, 30000)

            val gaussian = new NormalDistribution(0,1)



            for (i <- 0 until 60000)
            {
                calc.addMetric(gaussian.sample())
            }

            Assert.assertEquals(gaussian.inverseCumulativeProbability(0.1), calc.value, 0.05);

        }
    }
