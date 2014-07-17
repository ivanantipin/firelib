using System;
using System.Collections.Generic;
using System.Threading;
using QuantLib.Domain;

namespace Fire.Common.Robot
{
    public class Frequencer
    {
        private readonly Interval interval;

        private readonly List<Action<DateTime>> timeListeners = new List<Action<DateTime>>();

        private Timer timer;

        private long lastTimeTrigger;

        private long precision = 250;

        public void AddListener(Action<DateTime> act)
        {
            lock (timeListeners)
            {
                timeListeners.Add(act);
            }
        }

        private void CheckTime()
        {            
            long epochTick = DateTime.Now.Ticks;
            long rounded = (epochTick/interval.Ticks)*interval.Ticks;
            if (lastTimeTrigger != rounded)
            {
                lastTimeTrigger = rounded;
                NotifyListeners(new DateTime(rounded));                
            }

        }

        public void Start()
        {
            timer = new Timer((o) => CheckTime(), null, precision, precision);
        }

        private void NotifyListeners(DateTime ctime)
        {
            lock (timeListeners)
            {
                foreach (var timeListener in timeListeners)
                {
                    timeListener(ctime);
                }
            }
        }

        public Frequencer(Interval interval)
        {
            this.interval = interval;
        }
    }
}