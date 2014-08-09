using System;

namespace QuantLib.Rolling.HeapQuantile
{
    public interface IQuantileCalculator
    {
        double FindQuantile(double metric);
        void AddMetric(double val);
        double GetQuantile(int idx);
    }

    public static class QCalc
    {
        public static int Cmp(this IQuantileCalculator calc, double metric, double quntile)
        {
            double qq = calc.FindQuantile(metric);

            if (Math.Abs(qq - quntile) < 0.0001)
            {
                return 0;
            }
            return Math.Sign(qq - quntile);
        }
    }
}