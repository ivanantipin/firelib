using System.Collections.Generic;
using MathNet.Numerics.Distributions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using QuantLib.Rolling.HeapQuantile;

namespace UtilsTest
{
    [TestClass]
    public class EWSATest
    {
        [TestMethod]
        public void TestHeapOnNormal()
        {
            var normal = new Normal(0, 1);
            TestHeap(normal, 30000, 60000);
        }

        private static void TestHeap(Normal normal, int sampleSize, int cntOfMetrics)
        {
            var calc = new HeapQuantilesWrapper(new List<double> {0.1, 0.9}, sampleSize);
            for (var i = 0; i < cntOfMetrics; i++)
            {
                calc.AddMetric(normal.Sample());
            }


            Assert.AreEqual(normal.InverseCumulativeDistribution(0.1), calc.GetQuantile(0), 0.05);
            Assert.AreEqual(normal.InverseCumulativeDistribution(0.9), calc.GetQuantile(1), 0.05);


        }
    }
}