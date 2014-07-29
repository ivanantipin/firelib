using System.Threading;

namespace Fire.Common.Robot
{
    public interface IOrderIdService
    {
        long GetNextId();
    }

    public class OrderIdService : IOrderIdService
    {
        private long id;

        public long GetNextId()
        {
            return Interlocked.Increment(ref id);
        }
    }
}