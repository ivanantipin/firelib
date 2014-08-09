using System.Collections.Generic;

namespace QuantLib.Rolling.HeapQuantile
{
    public class HeapQuantilesWrapper : IQuantileCalculator
    {
        private readonly List<HeapQuantile> quantiles = new List<HeapQuantile>();

        public HeapQuantilesWrapper(List<double> quantilesProb, int metricsCnt)
        {
            quantilesProb.ForEach(p => quantiles.Add(new HeapQuantile(p, metricsCnt)));
        }

        public double FindQuantile(double metric)
        {
            for (int i = 0; i < quantiles.Count; i++)
            {
                if (quantiles[i].GetValue() > metric)
                {
                    return quantiles[i].Quant;
                }
            }
            return 1;
        }

        public void AddMetric(double val)
        {
            for (int i = 0; i < quantiles.Count; i++)
            {
                quantiles[i].AddMetric(val);
            }
        }

        public double GetQuantile(int idx)
        {
            return quantiles[idx].GetValue();
        }
    }
}