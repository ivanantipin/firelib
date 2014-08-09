using System;
using System.Collections.Generic;
using System.Linq;
using C5;


namespace QuantLib.Rolling.HeapQuantile
{

    public class HeapQuantile
    {
        private readonly double quantile;

        private readonly TreeBag<Node> left = new TreeBag<Node>(new NodeComparer());
        private readonly TreeBag<Node> right = new TreeBag<Node>(new NodeComparer());
        private readonly CircularQueue<Node> ring;

        private int cnt = 0;
        readonly int length;
        public HeapQuantile(double quantile, int count)
        {
            this.quantile = quantile;
            ring = new CircularQueue<Node>(count);
            length = count;
        }

        class Node
        {
            public readonly double Value;
            public bool Left;

            public Node(double value, bool left)
            {
                this.Value = value;
                this.Left = left;
            }
        }

        class NodeComparer : IComparer<Node>
        {
            public int Compare(Node n0, Node n1)
            {
                return n0.Value.CompareTo(n1.Value);
            }

        }

        public int Count
        {
            get { return cnt; }
        }

        public double Quant
        {
            get { return quantile; }
        }

        public double GetValue()
        {
            if (left.IsEmpty && right.IsEmpty)
            {
                return 0;
            }
            if (left.IsEmpty)
            {
                return right.FindMin().Value;
            }
            if (right.IsEmpty)
            {
                return left.FindMax().Value;
            }

            return (left.FindMax().Value + right.FindMin().Value) / 2;
        }

        public void AddMetric(double m)
        {
            if (ring.Count >= length)
            {
                Node node = ring.Dequeue();
                if (node.Left)
                {
                    left.Remove(node);
                }
                else
                {
                    right.Remove(node);
                }
            }
            cnt++;

            var val = GetValue();

            if (m > val)
            {
                var n = new Node(m, false);
                ring.Enqueue(n);
                right.Add(n);
            }
            else
            {
                var n = new Node(m, true);
                ring.Enqueue(n);
                left.Add(n);
            }
            Balance();
        }

        void Balance()
        {
            var diff = left.Count / quantile - right.Count / (1 - quantile);
            if (diff > 0)
            {
                var amDiff = (left.Count - 1) / quantile - (right.Count + 1) / (1 - quantile);
                if (Math.Abs(amDiff) < diff)
                {
                    var leftMax = left.DeleteMax();
                    right.Add(leftMax);
                    leftMax.Left = false;
                }
            }
            else
            {
                var amDiff = (left.Count + 1) / quantile - (right.Count - 1) / (1 - quantile);
                if (Math.Abs(amDiff) < -diff)
                {
                    var rightMin = right.DeleteMin();
                    left.Add(rightMin);
                    rightMin.Left = true;
                }
            }
        }
    }
}