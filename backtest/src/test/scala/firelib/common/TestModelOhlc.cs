using System;
using System.Collections.Generic;
using Fire.Common.Backtest;
using Fire.Common.Domain;
using Fire.Common.Util;
using QuantLib.Domain;

namespace TFWTests
{
    internal class TestModelOhlc : BasketModel
    {
        public static TestModelOhlc instance;
        public DateTime endTime = DateTime.MinValue;

        public List<DateTime> startTimesGmt = new List<DateTime>();
        private ITimeSeries<Ohlc> hist;


        protected override void ApplyProperties(Dictionary<string, string> mprops)
        {
            instance = this;
            hist = MarketDataDistributor.SubscribeForHistory(0, Interval.Min5, 10);
            hist.OnTsUpdate += On5Min;
            dayHist = MarketDataDistributor.SubscribeForHistory(0, Interval.Day, 10);

        }

        public List<Ohlc> bars = new List<Ohlc>();

        private void On5Min(ITimeSeries<Ohlc> hh)
        {
            if (dayHist[0].DtGmtEnd.TimeOfDay.TotalSeconds > 0)
            {
                throw new Exception("time of day ts not correct");
            }

            if (DtGmt != hh[0].DtGmtEnd)
            {
                throw new Exception("time is not equal");
            }
            bars.Add(new Ohlc(hh[0]));
            if (bars.Count > 1)
            {
                Console.WriteLine((hh[0].DtGmtEnd));
                if ((hh[0].DtGmtEnd - hh[-1].DtGmtEnd).TotalMinutes != 5)
                {
                    throw new Exception("not 5 min diff " + hh[0] + " -- " + (hh[0].DtGmtEnd - hh[-1].DtGmtEnd));
                }
            }
            AddOhlc(new Ohlc(hh[0]));
        }

        protected void AddOhlc(Ohlc pQuote)
        { 
            if (uniqTimes.Contains(pQuote.DtGmtEnd))
            {
                throw new Exception("dupe time " + pQuote.DtGmtEnd);
            }
            uniqTimes.Add(pQuote.DtGmtEnd);

            if (startTimesGmt.Count == 0 || startTimesGmt.Last().Date != pQuote.DtGmtEnd.Date)
            {
                startTimesGmt.Add(pQuote.DtGmtEnd);
            }
        }



        private readonly HashSet<DateTime> uniqTimes = new HashSet<DateTime>();
        private ITimeSeries<Ohlc> dayHist;
    }
}