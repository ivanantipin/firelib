using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using QuantLib.Accumulators;
using QuantLib.Domain;
using QuantLib.HDF5.Reader;
using QuantLib.Helpers;
using QuantLib.IndicatorBase;
using QuantLib.IndicatorBase.Reader;
using QuantLib.Median;
using UltraFastParser;

namespace TFWTests
{
    [TestClass]
    public class ParserTests
    {
        private static string strTestsRootPath = @"..\..\..\Tests";

        [TestMethod]
        public void TestMethod1()
        {
            var rb = new RingBufferTwoSided<double>(10, 2);
            for (int i = 0; i < 13; i++)
            {
                rb.Add(i);
            }
            Assert.AreEqual(rb[0], 10, 0.001);
            Assert.AreEqual(rb[-9], 1, 0.001);
            Assert.AreEqual(rb[2], 12, 0.001);
        }

        /*
        [TestMethod]
        public unsafe void TestSimpleReader_Basic()
        {
            using (var reader = new SimpleReader(new DateTime(2011, 11, 21), new DateTime(2012, 01, 01),
                                                 Path.Combine(strTestsRootPath, @"TstData\data1_0.csv")))
            {
                Assert.IsNotNull(reader);
                Assert.IsTrue(reader.PQuote != null);
            }
        }

        [TestMethod]
        public unsafe void TestSimpleReader_ForwardSlash()
        {
            //
            // Sten: I modified CSharpWrapper::FileParser::Open() to support forward slashes in filenames, so
            //       the code below must work.
            //
            using (var reader = new SimpleReader(new DateTime(2011, 11, 21), new DateTime(2012, 01, 01),
                                                 Path.Combine(strTestsRootPath, @"TstData/data1_0.csv")))
            {
                Assert.IsNotNull(reader);
                Assert.IsTrue(reader.PQuote != null);
            }
        }

        [TestMethod]
        public unsafe void TestSimpleReader_ParseNextLine1()
        {
            //
            // Test that .csv parser correctly seeks file pointer in .csv database and reads next tick.
            //
            using (var reader = new SimpleReader(new DateTime(1900, 01, 01), new DateTime(2100, 01, 01),
                                                 Path.Combine(strTestsRootPath, @"Seek1/RI#_0.csv"),
                                                 TimeConversion.Local))
            {
                bool result = true;

                result = reader.SeekLocal(new DateTime(2010, 02, 01, 18, 39, 47, 999));
                Assert.IsTrue(result);

                result = reader.Read();
                Assert.IsTrue(result);
                Assert.IsTrue(reader.PQuote != null);

                Assert.IsTrue(reader.PQuote->ID == 136307382);
                Assert.IsTrue(reader.PQuote->TrdPrice == 147735);
                Assert.IsTrue(reader.PQuote->TrdVolume == 8);
            }
        }

        [TestMethod]
        public unsafe void TestSimpleReader_ParseNextLine1A_Regression()
        {
            //
            // Test that .csv parser correctly seeks file pointer in .csv database and reads next tick.
            //
            // Note: the same test as TestSimpleReader_ParseNextLine1(), but here we add a space at the
            //       beginning of the file path. This is actually a regression test. Verified that SimpleReader
            //       can open filenames that start with spaces.
            //
            using (var reader = new SimpleReader(new DateTime(1900, 01, 01), new DateTime(2100, 01, 01),
                                                 " " + Path.Combine(strTestsRootPath, @"Seek1/RI#_0.csv"),
                                                 TimeConversion.Local))
            {
                bool result = true;

                result = reader.SeekLocal(new DateTime(2010, 02, 01, 18, 39, 47, 999));
                Assert.IsTrue(result);

                result = reader.Read();
                Assert.IsTrue(result);
                Assert.IsTrue(reader.PQuote != null);

                Assert.IsTrue(reader.PQuote->ID == 136307382);
                Assert.IsTrue(reader.PQuote->TrdPrice == 147735);
                Assert.IsTrue(reader.PQuote->TrdVolume == 8);
            }
        }

        [TestMethod]
        public unsafe void TestSimpleReader_ParseNextLine2()
        {
            //
            // Test that .csv parser correctly seeks file pointer in .csv database and reads ticks across .csv boundaries.
            //
            using (var reader = new SimpleReader(new DateTime(1900, 01, 01), new DateTime(2100, 01, 01),
                                                 Path.Combine(strTestsRootPath, @"Seek1/RI#_0.csv"),
                                                 TimeConversion.Local))
            {
                bool result = true;

                result = reader.SeekLocal(new DateTime(2010, 02, 01, 18, 39, 47, 999));
                Assert.IsTrue(result);

                //
                // Read the very last 24 ticks from Tests\Seek1\RI#_201012.csv file.
                //
                for (int i = 0; i < 24; ++i)
                {
                    result = reader.Read();
                    Assert.IsTrue(result);
                    Assert.IsTrue(reader.PQuote != null);
                }

                //
                // And the next tick must com from Tests\Seek1\RI#_201112.csv file.
                //
                result = reader.Read();
                Assert.IsTrue(result);
                Assert.IsTrue(reader.PQuote != null);

                Assert.IsTrue(reader.PQuote->ID == 255223074);
                Assert.IsTrue(reader.PQuote->TrdPrice == 177885);
                Assert.IsTrue(reader.PQuote->TrdVolume == 1);
            }
        }

        [TestMethod]
        public void TestSimpleReader_OpenNonExistentFile()
        {
            //
            // Test that .csv parser throws an exception when opening non-existing .csv file.
            //
            bool fCaugthException = false;

            try
            {
                using (new SimpleReader(new DateTime(1900, 01, 01), new DateTime(2100, 01, 01),
                                        Path.Combine(strTestsRootPath, @"Non/Exist_0.csv")))
                {
                }
            }
            catch (Exception)
            {
                //
                // Ok, we caught an exception.
                //
                fCaugthException = true;
            }

            Assert.IsTrue(fCaugthException);
        }

        [TestMethod]
        public void TestSimpleReader_RingBufferPreInited()
        {
            //
            // Tests RingBufferPreInited class functionality.
            //
            int counter = 100;
            var buf = new RingBufferPreInited<IntObj>(5, () => new IntObj(++counter), Interval.Sec1);

            Assert.AreEqual(buf.Last.val, 105);
            Assert.AreEqual(buf[0].val, 105);
            Assert.AreEqual(buf[-1].val, 104);
            Assert.AreEqual(buf[-2].val, 103);
            Assert.AreEqual(buf[-3].val, 102);
            Assert.AreEqual(buf[-4].val, 101);

            IntObj newEl = buf.ShiftAndGetLast();
            newEl.val = 200;

            Assert.AreEqual(buf.Last.val, 200);
            Assert.AreEqual(buf[0].val, 200);
            Assert.AreEqual(buf[-1].val, 105);
            Assert.AreEqual(buf[-2].val, 104);
            Assert.AreEqual(buf[-3].val, 103);
            Assert.AreEqual(buf[-4].val, 102);
        }

         */

        [TestMethod]
        public void TestSequentialReader()
        {
            //
            // Basic test for SequentialReader() class. Open sample .h5 file and read a first few values from the beginning of the dataset.
            //
            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", Operations.UnixToDT(1325376001000000000)))
            {
                bool bResult = false;
                float val = 0;

                val = reader.Value;
                Assert.AreEqual(val, 100, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);

                val = reader.Value;
                Assert.AreEqual(val, 101, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);

                val = reader.Value;
                Assert.AreEqual(val, 102, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);

                val = reader.Value;
                Assert.AreEqual(val, 103, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);
            }
        }

        [TestMethod]
        public void TestSequentialReader2()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file and read a whole dataset.
            // Verify that last element value in the dataset is 700. And total number of entries is 2678400.
            //
            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", Operations.UnixToDT(1325376001000000000)))
            {
                float lastVal = 0;
                int totalCount = 0;
                float totalSum = 0;

                do
                {
                    lastVal = reader.Value;
                    totalSum += lastVal;
                    ++totalCount;
                } while (reader.Next());

                Assert.AreEqual(lastVal, 700, 0.000001);
                Assert.AreEqual(totalSum, 100 + 101 + 102 + 103 + 700, 0.000001);
                Assert.AreEqual(totalCount, 2678400);
            }
        }

        [TestMethod]
        public void TestSequentialReader3()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file and read starting from some offset.
            //
            DateTime dt = Operations.UnixToDT(1325376001000000000).AddSeconds(1);

            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", dt))
            {
                bool bResult = false;
                float val = 0;

                val = reader.Value;
                Assert.AreEqual(val, 101, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);

                val = reader.Value;
                Assert.AreEqual(val, 102, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);
            }
        }

        [TestMethod]
        public void TestSequentialReader4()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file and read the very last row.
            //
            DateTime dt = Operations.UnixToDT(1325376001000000000).AddSeconds(2678399);

            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", dt))
            {
                float val = 0;

                val = reader.Value;
                Assert.AreEqual(val, 700, 0.000001);
            }
        }

        [TestMethod]
        public void TestSequentialReader5()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file and read the very last row.
            //
            // Note: use very small buffer of 3 elements for caching.
            //
            DateTime dt = Operations.UnixToDT(1325376001000000000).AddSeconds(2678399);

            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", dt, 3))
            {
                float val = 0;

                val = reader.Value;
                Assert.AreEqual(val, 700, 0.000001);
            }
        }

        [TestMethod]
        public void TestSequentialReader6()
        {
            //
            // Basic test for SequentialReader() class. Open sample .h5 file and read a first few values from the beginning of the dataset.
            //
            // Note: use very small buffer of 3 elements for caching.
            //
            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", Operations.UnixToDT(1325376001000000000), 3)
                )
            {
                bool bResult = false;
                float val = 0;

                val = reader.Value;
                Assert.AreEqual(val, 100, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);

                val = reader.Value;
                Assert.AreEqual(val, 101, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);

                val = reader.Value;
                Assert.AreEqual(val, 102, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);

                val = reader.Value;
                Assert.AreEqual(val, 103, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);
            }
        }

        [TestMethod]
        public void TestSequentialReader7()
        {
            for (int i = 0; i < 10; ++i)
            {
                //
                // Test for SequentialReader() class. Open sample .h5 file and read a whole dataset.
                // Verify that last element value in the dataset is 700. And total number of entries is 2678400.
                //
                // Note: repeat test 10 times. This is actually a REGRESSION test. We found some subtle bugs in SequentialReader
                //       class: Cleanup() method could be called twice at a row, class did not support IDisposable interface.
                //
                using (
                    var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                             @"/HL", Operations.UnixToDT(1325376001000000000),
                                                             10000)
                    )
                {
                    float lastVal = 0;
                    int totalCount = 0;
                    float totalSum = 0;

                    do
                    {
                        lastVal = reader.Value;
                        totalSum += lastVal;
                        ++totalCount;
                    } while (reader.Next());

                    Assert.AreEqual(lastVal, 700, 0.000001);
                    Assert.AreEqual(totalSum, 100 + 101 + 102 + 103 + 700, 0.000001);
                    Assert.AreEqual(totalCount, 2678400);
                }
            }
        }

        [TestMethod]
        public void TestSequentialReader7A()
        {
            for (int i = 0; i < 1000; ++i)
            {
                //
                // Test for SequentialReader() class. Open sample .h5 file and read a whole dataset.
                //
                // The test is same as TestSequentialReader7(), but uses very small buffer of 3 elements,
                // number of iterations is set to 1000, and reads only firsts 1000 rows.
                //
                // Note: repeat test 10 times. This is actually a REGRESSION test. We found some subtle bugs in SequentialReader
                //       class: Cleanup() method could be called twice at a row, class did not support IDisposable interface.
                //
                using (
                    var
                        reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                             @"/HL", Operations.UnixToDT(1325376001000000000),
                                                             3)
                    )
                {
                    float lastVal = 0;
                    int totalCount = 0;
                    float totalSum = 0;

                    do
                    {
                        lastVal = reader.Value;
                        totalSum += lastVal;
                        ++totalCount;

                        if (totalCount >= 1000)
                            break;
                    } while (reader.Next());
                }
            }
        }

        [TestMethod]
        public void TestSequentialReader8()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file and read starting from some offset.
            //
            // Note: use very small buffer of 3 elements for caching.
            //
            DateTime dt = Operations.UnixToDT(1325376001000000000).AddSeconds(1);

            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", dt))
            {
                bool bResult = false;
                float val = 0;

                val = reader.Value;
                dt = reader.CurrTime;
                Assert.AreEqual(val, 101, 0.000001);
                Assert.AreEqual(dt, Operations.UnixToDT(1325376001000000000).AddSeconds(1));
                bResult = reader.Next();
                Assert.IsTrue(bResult);

                val = reader.Value;
                dt = reader.CurrTime;
                Assert.AreEqual(val, 102, 0.000001);
                Assert.AreEqual(dt, Operations.UnixToDT(1325376001000000000).AddSeconds(2));
                bResult = reader.Next();
                Assert.IsTrue(bResult);
            }
        }

        [TestMethod]
        public void TestSequentialReader9()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file and read starting from some offset just before the end of the buffer.
            //
            // Note: use very small buffer of 3 elements for caching.
            //
            DateTime dt = Operations.UnixToDT(1325376001000000000).AddSeconds(2);

            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", dt))
            {
                bool bResult = false;
                float val = 0;

                val = reader.Value;
                Assert.AreEqual(val, 102, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);
            }
        }

        [TestMethod]
        public void TestSequentialReader10()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file and read starting from some offset just after start of the buffer.
            //
            // Note: use very small buffer of 3 elements for caching.
            //
            DateTime dt = Operations.UnixToDT(1325376001000000000).AddSeconds(3);

            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", dt))
            {
                bool bResult = false;
                float val = 0;

                val = reader.Value;
                Assert.AreEqual(val, 103, 0.000001);
                bResult = reader.Next();
                Assert.IsTrue(bResult);
            }
        }

        [TestMethod]
        public void TestSequentialReader11()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file, read the very last row, verify that Eof() event is triggered.
            //
            DateTime dt = Operations.UnixToDT(1325376001000000000).AddSeconds(2678399);

            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", dt, 3))
            {
                bool fEofTriggered = false;
                reader.Eof += () => fEofTriggered = true;

                reader.Next();
                Assert.IsTrue(fEofTriggered);
            }
        }

        [TestMethod]
        public void TestSequentialReader12()
        {
            //
            // Test for SequentialReader() class. Open sample .h5 file, test that Inverval property returns correct value.
            //
            DateTime dt = Operations.UnixToDT(1325376001000000000).AddSeconds(2678399);

            using (var reader = new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                            @"/HL", dt, 3))
            {
                Interval interval = reader.Interval;
                Assert.AreEqual(interval.TotalMilliseconds, 1000);
            }
        }

        [TestMethod]
        public void TestSequentialReader13()
        {
            for (int i = 0; i < 100; ++i)
            {
                //
                // Test for SequentialReader() class. Open sample .h5 file and read it using two different readers simultaneously.
                //
                using (
                    SequentialReader<float> reader1 =
                        new SequentialReader<float>(Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                    @"/HL", Operations.UnixToDT(1325376001000000000), 3),
                                            reader2 =
                                                new SequentialReader<float>(
                                                    Path.Combine(strTestsRootPath, @"SimpleH5\IntrAct1.h5"),
                                                    @"/HL", Operations.UnixToDT(1325376001000000000), 3)
                    )
                {
                    bool bResult = false;
                    float val = 0;

                    //
                    // Reader1.
                    //
                    val = reader1.Value;
                    Assert.AreEqual(val, 100, 0.000001);
                    bResult = reader1.Next();
                    Assert.IsTrue(bResult);

                    val = reader1.Value;
                    Assert.AreEqual(val, 101, 0.000001);
                    bResult = reader1.Next();
                    Assert.IsTrue(bResult);

                    //
                    // Reader2.
                    //
                    val = reader2.Value;
                    Assert.AreEqual(val, 100, 0.000001);
                    bResult = reader2.Next();
                    Assert.IsTrue(bResult);

                    val = reader2.Value;
                    Assert.AreEqual(val, 101, 0.000001);
                    bResult = reader2.Next();
                    Assert.IsTrue(bResult);

                    //
                    // Reader1.
                    //
                    val = reader1.Value;
                    Assert.AreEqual(val, 102, 0.000001);
                    bResult = reader1.Next();
                    Assert.IsTrue(bResult);

                    val = reader1.Value;
                    Assert.AreEqual(val, 103, 0.000001);
                    bResult = reader1.Next();
                    Assert.IsTrue(bResult);

                    //
                    // Reader2.
                    //
                    val = reader2.Value;
                    Assert.AreEqual(val, 102, 0.000001);
                    bResult = reader2.Next();
                    Assert.IsTrue(bResult);

                    val = reader2.Value;
                    Assert.AreEqual(val, 103, 0.000001);
                    bResult = reader2.Next();
                    Assert.IsTrue(bResult);
                }
            }
        }

        [TestMethod]
        public void TestSimpleMedian1()
        {
            double[] buf = {1, 8, 5, 13, 9, 4};
            double median = SimpleMedian.GetMedian(buf);

            Assert.AreEqual(median, 6.5, 0.000001);
        }

        [TestMethod]
        public void TestSimpleMedian2()
        {
            double[] buf = {1, 5, 13, 9, 4};
            double median = SimpleMedian.GetMedian(buf);

            Assert.AreEqual(median, 5, 0.000001);
        }

        [TestMethod]
        public unsafe void TestAverageAccumulator1()
        {
            RecordQuote trade;
            var accumulator = new AverageAccumulator();

            accumulator.ChunkStart();

            Assert.IsTrue(accumulator.Interpolated);

            trade.TrdPrice = 100;
            accumulator.Add(&trade);

            Assert.IsFalse(accumulator.Interpolated);

            trade.TrdPrice = 101;
            accumulator.Add(&trade);

            trade.TrdPrice = 102;
            accumulator.Add(&trade);

            Assert.IsFalse(accumulator.Interpolated);

            accumulator.ChunkComplete();

            Assert.AreEqual(accumulator.Value, 101, 0.000001);
            Assert.AreEqual(accumulator.N, 3);
        }

        [TestMethod]
        public unsafe void TestOHLCAccumulator1()
        {
            RecordQuote trade;
            var accumulator = new OHLCAccumulator();

            accumulator.ChunkStart();

            Assert.IsTrue(accumulator.Interpolated);

            trade.TrdPrice = 100;
            trade.Volume = 1;

            accumulator.Add(&trade);

            Assert.IsFalse(accumulator.Interpolated);

            trade.TrdPrice = 101;
            trade.Volume = 1;
            accumulator.Add(&trade);

            trade.TrdPrice = 102;
            trade.Volume = 2;
            accumulator.Add(&trade);

            Assert.IsFalse(accumulator.Interpolated);

            accumulator.ChunkComplete();

            Assert.AreEqual(accumulator.Open, 100, 0.000001);
            Assert.AreEqual(accumulator.High, 102, 0.000001);
            Assert.AreEqual(accumulator.Low, 100, 0.000001);
            Assert.AreEqual(accumulator.Close, 102, 0.000001);
            Assert.AreEqual(accumulator.Trades, 3);
            Assert.AreEqual(accumulator.N, 3);
            Assert.AreEqual(accumulator.Volume, 4);
        }

        [TestMethod]
        public unsafe void TestPriceChangesAccumulator1()
        {
            RecordQuote trade;
            var accumulator = new PriceChangesAccumulator();

            accumulator.ChunkStart();

            Assert.IsTrue(accumulator.Interpolated);

            trade.TrdPrice = 100;
            trade.Volume = 1;

            accumulator.Add(&trade);

            Assert.IsFalse(accumulator.Interpolated);

            trade.TrdPrice = 101;
            trade.Volume = 1;
            accumulator.Add(&trade);

            trade.TrdPrice = 101;
            trade.Volume = 1;
            accumulator.Add(&trade);

            trade.TrdPrice = 102;
            trade.Volume = 2;
            accumulator.Add(&trade);

            Assert.IsFalse(accumulator.Interpolated);

            accumulator.ChunkComplete();

            Assert.AreEqual(accumulator.N, 3);
        }

        [TestMethod]
        public void TestSimpleAccumulator1()
        {
            var accumulator = new SimpleAccumulator();

            accumulator.Init();

            accumulator.Add(100, 1);
            accumulator.Add(99, 4);
            accumulator.Add(100, 1);
            accumulator.Add(101, 1);
            accumulator.Add(102, 1);

            Assert.AreEqual(accumulator.Open, 100, 0.000001);
            Assert.AreEqual(accumulator.High, 102, 0.000001);
            Assert.AreEqual(accumulator.Low, 99, 0.000001);
            Assert.AreEqual(accumulator.Close, 102, 0.000001);
            Assert.AreEqual(accumulator.HL, 3, 0.000001);
            Assert.AreEqual(accumulator.Deals, 5);
            Assert.AreEqual(accumulator.AvgSize, 8.0/5, 0.000001);
        }

        [TestMethod]
        public void TestSimpleAccumulatorEx1()
        {
            var accumulator = new SimpleAccumulatorEx();

            accumulator.Init();

            accumulator.Add(100, 1, new DateTime(2013, 7, 1, 15, 0, 1));
            accumulator.Add(99, 4, new DateTime(2013, 7, 1, 15, 0, 2));
            accumulator.Add(100, 1, new DateTime(2013, 7, 1, 15, 0, 3));
            accumulator.Add(101, 1, new DateTime(2013, 7, 1, 15, 0, 4));
            accumulator.Add(102, 1, new DateTime(2013, 7, 1, 15, 0, 5));

            Assert.AreEqual(accumulator.Open, 100, 0.000001);
            Assert.AreEqual(accumulator.High, 102, 0.000001);
            Assert.AreEqual(accumulator.Low, 99, 0.000001);
            Assert.AreEqual(accumulator.Close, 102, 0.000001);
            Assert.AreEqual(accumulator.HL, 3, 0.000001);
            Assert.AreEqual(accumulator.Deals, 5);
            Assert.AreEqual(accumulator.AvgSize, 8.0/5, 0.000001);

            Assert.AreEqual(accumulator.DealsTimeSpanSeconds, 4, 0.000001);
            Assert.AreEqual(accumulator.FirstDealDT, new DateTime(2013, 7, 1, 15, 0, 1));
            Assert.AreEqual(accumulator.LastDealDT, new DateTime(2013, 7, 1, 15, 0, 5));
        }

        [TestMethod]
        public unsafe void TestTickAccumulator1()
        {
            var accumulator = new TickAccumulator();

            accumulator.ChunkStart();

            accumulator.Add(null);
            accumulator.Add(null);
            accumulator.Add(null);
            accumulator.Add(null);
            accumulator.Add(null);

            accumulator.ChunkComplete();

            Assert.AreEqual(accumulator.N, 5);
            Assert.AreEqual(accumulator.Value, 5, 0.000001);
        }

        [TestMethod]
        public unsafe void TestVolumeAccumulator1()
        {
            RecordQuote trade;
            var accumulator = new VolumeAccumulator();

            accumulator.ChunkStart();

            trade.Volume = 5;
            accumulator.Add(&trade);

            trade.Volume = 4;
            accumulator.Add(&trade);

            trade.Volume = 1;
            accumulator.Add(&trade);

            trade.Volume = 3;
            accumulator.Add(&trade);

            accumulator.ChunkComplete();

            Assert.AreEqual(accumulator.N, 4);
            Assert.AreEqual(accumulator.Value, 13, 0.000001);
        }

        [TestMethod]
        public void TestRingBuffer()
        {
            var bufferPreInited = new HistoryCircular<long[]>(3, () => new[] {0L}, Interval.Sec1);
            long[] nl = bufferPreInited.ShiftAndGetLast();
            nl[0] = 1;
            Assert.AreEqual(1L, bufferPreInited[0][0]);

            nl = bufferPreInited.ShiftAndGetLast();
            nl[0] = 2;
            Assert.AreEqual(1L, bufferPreInited[-1][0]);
            Assert.AreEqual(2L, bufferPreInited[0][0]);

            nl = bufferPreInited.ShiftAndGetLast();
            nl[0] = 3;
            nl = bufferPreInited.ShiftAndGetLast();
            nl[0] = 4;
            Assert.AreEqual(2L, bufferPreInited[-2][0]);
            Assert.AreEqual(4L, bufferPreInited[0][0]);
        }

        //-------------------------------------------------------------

        [TestMethod]
        public void TestNavigatedWindow()
        {
            var lst = new List<long>();
            var dts = new List<DateTime>();
            DateTime startDt = DateTime.Now;

            for (int i = 0; i < 10; i++)
            {
                lst.Add(i);
                dts.Add(startDt.AddMilliseconds(i*Interval.Sec1.TotalMilliseconds));
            }
            TestNavigatedWithOutFuture(lst, dts, startDt);
            TestNavigatedWithFutureAndCurrent(lst, dts, startDt);
        }

        [TestMethod]
        public void TestUltraFastSingleCsvParser_1()
        {
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/TstData2/data1_0.csv"),
                                             commonIniSettings);

            Assert.AreEqual(parser.StartDt, new DateTime(2011, 11, 21, 2, 0, 8, 700));
            Assert.AreEqual(parser.EndDt, new DateTime(2012, 9, 26, 3, 55, 21, 222));
        }

        [TestMethod]
        public void TestUltraFastSingleCsvParser_2()
        {
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/TstData2/data2_0.csv"),
                                             commonIniSettings);

            Assert.AreEqual(parser.StartDt, new DateTime(2011, 11, 21, 2, 0, 8, 700));
            Assert.AreEqual(parser.EndDt, new DateTime(2011, 11, 21, 2, 0, 8, 700));
        }

        [TestMethod]
        public void TestUltraFastSingleCsvParser_3()
        {
            //
            // Test that two parsers are able to open the same .csv file simultaneously.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
            commonIniSettings.TIMEZONE = "NY";

            var parser1 =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/TstData2/data2_0.csv"),
                                             commonIniSettings);
            var parser2 =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/TstData2/data2_0.csv"),
                                             commonIniSettings);

            Assert.AreEqual(parser1.StartDt, new DateTime(2011, 11, 21, 2, 0, 8, 700));
            Assert.AreEqual(parser1.EndDt, new DateTime(2011, 11, 21, 2, 0, 8, 700));

            Assert.AreEqual(parser2.StartDt, new DateTime(2011, 11, 21, 2, 0, 8, 700));
            Assert.AreEqual(parser2.EndDt, new DateTime(2011, 11, 21, 2, 0, 8, 700));
        }

        [TestMethod]
        public void TestUltraFastSingleCsvParser_4()
        {
            //
            // Test that parser.StartDt and parser.EndDt contain DateTime.MinValue, DateTime.MaxValue for zero length files.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/ZerolenFile/zerolen_0.csv"),
                                             commonIniSettings);

            Assert.AreEqual(parser.StartDt, DateTime.MinValue);
            Assert.AreEqual(parser.EndDt, DateTime.MaxValue);
        }

        [TestMethod]
        public void TestUltraFastSingleCsvParser_5()
        {
            //
            // Test that parser.StartDt and parser.EndDt contain DateTime.MinValue, DateTime.MaxValue
            // for file with incomplete first string that contains only date and time.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/LabudaFile/labuda_0.csv"),
                                             commonIniSettings);

            Assert.AreEqual(parser.StartDt, new DateTime(2011, 11, 21, 2, 0, 0, 0));
            Assert.AreEqual(parser.EndDt, new DateTime(2011, 11, 21, 2, 0, 0, 0));
        }

        [TestMethod]
        public unsafe void TestUltraFastSingleCsvParser_IQFEED_6()
        {
            //
            // Check that IQFEED format parsing works.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/TstData2/data1_0.csv"),
                                             commonIniSettings);

            parser.SeekLocal(parser.StartDt);

            var isOk = parser.Read();

            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2011, 11, 21, 2, 0, 8, 700));
        }

        [TestMethod]
        public unsafe void TestUltraFastSingleCsvParser_SKIP_SYMBOL()
        {
            //
            // Check that IQFEED format parsing works.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "O", "H", "L", "C", "V", "#", "#", "A", "B", "P", "U", "I"};
            //D_T_O_H_L_C_V_#_#_A_B_P_U_I
            commonIniSettings.TIMEZONE = "LONDON";

            /*
11.11.2010,161100,1401.548,1401.892,1400.197,1400.947,1,1402.52,1402.352,1400.703,1401.453,1,111
11.11.2010,161200,1400.992,1401.598,1400.992,1401.347,1,1401.453,1402.102,1401.453,1401.853,1,102
11.11.2010,161300,1401.397,1401.397,1399.997,1400.448,1,1401.858,1401.858,1400.502,1400.952,1,116
             */

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/BarData_DUKAS_SKIP_SYMBOL/XAUUSD_1.csv"),
                                             commonIniSettings);

            parser.SeekLocal(parser.StartDt);

            Assert.IsTrue(parser.Read());
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 11, 11, 16, 11, 0));
            Assert.AreEqual(parser.PQuote->Close, 1400.947, 0.001);
            Assert.AreEqual(parser.PQuote->Volume, 1);
            Assert.AreEqual(parser.PQuote->AskPrice, 1400.703, 0.001);
            Assert.IsTrue(parser.Read());
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 11, 11, 16, 12, 0));
            Assert.AreEqual(parser.PQuote->Close, 1401.347, 0.001);
            Assert.AreEqual(parser.PQuote->AskPrice, 1401.453, 0.001);
            Assert.IsTrue(parser.Read());
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 11, 11, 16, 13, 0));
            Assert.AreEqual(parser.PQuote->Close, 1400.448, 0.001);
            Assert.AreEqual(parser.PQuote->AskPrice, 1400.502, 0.001);
        }

        [TestMethod]
        public unsafe void TestUltraFastSingleCsvParser_ANFUTURES_7()
        {
            //
            // Check that ANFUTURES format parsing works.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "YYMMDD";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "P", "V"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser\Chain3_DSlike\ANFUTURES.COM\TICKS\FUT\ES_201006.csv"),
                                             commonIniSettings);

            parser.SeekLocal(parser.StartDt);

            var isOk = parser.Read();

            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 3, 10, 16, 30, 0, 0));
            Assert.AreEqual(parser.PQuote->TrdPrice, 1140.50);
            Assert.AreEqual(parser.PQuote->Volume, 18);
            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->CumVolume, 0);
            Assert.AreEqual(parser.PQuote->Id, 0UL);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 3, 10, 16, 30, 10, 0));
            Assert.AreEqual(parser.PQuote->TrdPrice, 1140.50);
            Assert.AreEqual(parser.PQuote->Volume, 1);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 4, 16, 16, 14, 59, 0));
            Assert.AreEqual(parser.PQuote->TrdPrice, 1189.75);
            Assert.AreEqual(parser.PQuote->Volume, 4);
        }

        [TestMethod]
        public unsafe void TestUltraFastSingleCsvParser_ANFUTURES_8_Dummy_Fields()
        {
            //
            // Check that ANFUTURES format parsing works.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "YYMMDD";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "P", "V"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(
                    Path.Combine(strTestsRootPath,
                                 @"UltraFastParser\Chain3_DSlike\ANFUTURES.COM\TICKS\FUT\ES_201006.csv"),
                    commonIniSettings);

            parser.SeekLocal(parser.StartDt);

            var isOk = parser.Read();

            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 3, 10, 16, 30, 0, 0));
            Assert.AreEqual(parser.PQuote->TrdPrice, 1140.50);
            Assert.AreEqual(parser.PQuote->Volume, 18);

            //
            // The following fields do not present in COLUMNFORMAT.
            // Verify they are set to zeroes.
            //
            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->CumVolume, 0);
            Assert.AreEqual(parser.PQuote->Id, 0UL);

            //
            // The following fields are defined only for bars data, and must be set to zeroes
            // for ANFUTURES.COM tick data.
            //
            Assert.AreEqual(parser.PQuote->Open, 0);
            Assert.AreEqual(parser.PQuote->High, 0);
            Assert.AreEqual(parser.PQuote->Low, 0);
            Assert.AreEqual(parser.PQuote->Close, 0);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 3, 10, 16, 30, 10, 0));
            Assert.AreEqual(parser.PQuote->TrdPrice, 1140.50);
            Assert.AreEqual(parser.PQuote->Volume, 1);

            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->CumVolume, 0);
            Assert.AreEqual(parser.PQuote->Id, 0UL);
        }

        [TestMethod]
        public unsafe void TestUltraFastSingleCsvParser_TICKDATA_8()
        {
            //
            // Check that TICKDATA format parsing works.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "P", "V"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(
                    Path.Combine(strTestsRootPath,
                                 @"UltraFastParser\Chain4_DSlike\TICKDATA.COM\TICKS\FUT\XG#_0.csv"),
                    commonIniSettings);

            parser.SeekLocal(parser.StartDt);

            var isOk = parser.Read();

            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2004, 12, 1, 3, 0, 49, 0));
            Assert.AreEqual(parser.PQuote->TrdPrice, 4138.0);
            Assert.AreEqual(parser.PQuote->Volume, 12);

            isOk = parser.Read();

            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2004, 12, 1, 3, 2, 3, 0));
            Assert.AreEqual(parser.PQuote->TrdPrice, 4140.0);
            Assert.AreEqual(parser.PQuote->Volume, 1);
        }

        [TestMethod]
        public unsafe void TestUltraFastSingleCsvParser_FORTS_FUT()
        {
            //
            // Check that FORTS FUT format parsing works.
            //
            var commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "YYYY-MM-DD";
            commonIniSettings.TIMEFORMAT = "HH:MM:SS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "#", "P", "V", "I"};
            commonIniSettings.TIMEZONE = "MOSCOW";

            var parser =
                new UltraFastSingleCsvParser(
                    Path.Combine(strTestsRootPath,
                                 @"UltraFastParser\Seek2\RI#_0.csv"),
                    commonIniSettings);

            parser.SeekLocal(parser.StartDt);

            var isOk = parser.Read();

            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2012, 1, 3, 10, 0, 0, 53));
            Assert.AreEqual(parser.PQuote->TrdPrice, 137495);
            Assert.AreEqual(parser.PQuote->Volume, 1);
            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->CumVolume, 0);
            Assert.AreEqual(parser.PQuote->Id, 483738513UL);

            isOk = parser.Read();

            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2012, 1, 3, 10, 0, 0, 53));
            Assert.AreEqual(parser.PQuote->TrdPrice, 137500);
            Assert.AreEqual(parser.PQuote->Volume, 1);
            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->CumVolume, 0);
            Assert.AreEqual(parser.PQuote->Id, 483738514UL);
        }

        [TestMethod]
        public void TestUltraFastParser_ParseSymbolFileName()
        {
            string strPath;
            string strSymbol;
            string strExt;

            UltraFastParser.UltraFastParser.ParseSymbolFileName(
                Path.Combine(strTestsRootPath, @"Tests/TickersSorting/RI#_0.csv"), out strPath, out strSymbol,
                out strExt);

            string strAbsPath = Path.GetFullPath(Path.Combine(strTestsRootPath, @"Tests/TickersSorting"));

            Assert.AreEqual(strPath, strAbsPath);
            Assert.AreEqual(strSymbol, "RI#");
            Assert.AreEqual(strExt, ".csv");
        }

        [TestMethod]
        public void TestUltraFastParser_ParseAndMergeCommonIni_1()
        {
            var commonIniSettings = new CommonIniSettings();
            UltraFastParser.UltraFastParser.ParseAndMergeCommonIni(Path.Combine(strTestsRootPath, @"Seek1/common.ini"),
                                                                   ref commonIniSettings);

            Assert.AreEqual(commonIniSettings.DATEFORMAT, "YYYY-MM-DD");
            Assert.AreEqual(commonIniSettings.TIMEFORMAT, "HH:MM:SS");
            Assert.IsTrue(commonIniSettings.COLUMNFORMAT.SequenceEqual(new[] {"D", "T", "#", "P", "V", "I"}));
            Assert.AreEqual(commonIniSettings.TIMEZONE, "MOSCOW");
        }

        [TestMethod]
        public void TestUltraFastParser_ParseAndMergeCommonIni_2()
        {
            var commonIniSettings = new CommonIniSettings();
            UltraFastParser.UltraFastParser.ParseAndMergeCommonIni(
                Path.Combine(strTestsRootPath, @"CommonIni1/common.ini"),
                ref commonIniSettings);

            Assert.IsFalse(String.IsNullOrEmpty(commonIniSettings.PREVFILE));
            Assert.IsTrue(Directory.Exists(commonIniSettings.PREVFILE));
            Assert.IsTrue(Path.IsPathRooted(commonIniSettings.PREVFILE));

            Assert.AreEqual(commonIniSettings.DATEFORMAT, "DD.MM.YYYY");
            Assert.AreEqual(commonIniSettings.TIMEFORMAT, "HHMMSS");
            Assert.IsTrue(
                commonIniSettings.COLUMNFORMAT.SequenceEqual(new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"}));
            Assert.AreEqual(commonIniSettings.TIMEZONE, "NY");
        }

        [TestMethod]
        public void TestUltraFastParser_ParseAndMergeCommonIni_3()
        {
            var commonIniSettings = new CommonIniSettings();
            UltraFastParser.UltraFastParser.ParseAndMergeCommonIni(
                Path.Combine(strTestsRootPath, @"CommonIni2/common.ini"),
                ref commonIniSettings);

            Assert.IsFalse(String.IsNullOrEmpty(commonIniSettings.NEXTFILE));
            Assert.IsTrue(Directory.Exists(commonIniSettings.NEXTFILE));
            Assert.IsTrue(Path.IsPathRooted(commonIniSettings.NEXTFILE));

            Assert.AreEqual(commonIniSettings.DATEFORMAT, "DD.MM.YYYY");
            Assert.AreEqual(commonIniSettings.TIMEFORMAT, "HHMMSS");
            Assert.IsTrue(
                commonIniSettings.COLUMNFORMAT.SequenceEqual(new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"}));
            Assert.AreEqual(commonIniSettings.TIMEZONE, "NY");
        }

        [TestMethod]
        public void TestUltraFastParser_EnumerateTickerFiles()
        {
            UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] symbolData = UltraFastParser.UltraFastParser
                                                                                            .EnumerateTickerFiles(
                                                                                                Path.Combine(
                                                                                                    strTestsRootPath,
                                                                                                    @"CommonIni1"),
                                                                                                "@ES#", ".csv");

            Assert.IsTrue(symbolData.Length == 16);
            Assert.AreEqual(Path.GetFileName(symbolData[0].fullFileName), "@ES#_0.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[1].fullFileName), "@ES#_201212.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[2].fullFileName), "@ES#_201210.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[3].fullFileName), "@ES#_201208.csv");

            Assert.AreEqual(Path.GetFileName(symbolData[15].fullFileName), "@ES#_201005.csv");
        }

        [TestMethod]
        public void TestUltraFastParser_EnumerateTickerFiles2()
        {
            bool fException = false;

            try
            {
                UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] tickerFileNames = UltraFastParser.UltraFastParser
                                                                                                     .EnumerateTickerFiles
                    (
                        Path.Combine(strTestsRootPath, @"LoopChain1"), "@ES#", ".csv");
            }
            catch (Exception)
            {
                //
                // We expect EnumerateTickerFiles() generates 'loop found' exception while procesing LoopChain1 folder.
                //
                fException = true;
            }

            Assert.IsTrue(fException);
        }

        [TestMethod]
        public void TestUltraFastParser_EnumerateTickerFiles3()
        {
            //
            // We expect Chain1\201212\@ES#_201210.csv, Chain1\201212\@ES#_201212.csv files
            // to be skipped by enumeration algorithm.
            //
            UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] symbolData = UltraFastParser.UltraFastParser
                                                                                            .EnumerateTickerFiles(
                                                                                                Path.Combine(
                                                                                                    strTestsRootPath,
                                                                                                    @"Chain1"), "@ES#",
                                                                                                ".csv");

            Assert.IsTrue(symbolData.Length == 4);
            Assert.AreEqual(Path.GetFileName(symbolData[0].fullFileName), "@ES#_0.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[1].fullFileName), "@ES#_201112.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[2].fullFileName), "@ES#_201102.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[3].fullFileName), "@ES#_201010.csv");
        }

        [TestMethod]
        public void TestUltraFastParser_EnumerateTickerFiles4()
        {
            //
            // UltraFastParser\Chain2_WithZeroLenCsv contains zero-length @ES#_201102.csv file in the middle of the chain.
            // We expect enumeration algorithm to throw out this file (but PREVFILE reference from individual .ini file
            // have to be processed).
            //
            UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] symbolData = UltraFastParser.UltraFastParser
                                                                                            .EnumerateTickerFiles(
                                                                                                Path.Combine(
                                                                                                    strTestsRootPath,
                                                                                                    @"UltraFastParser/Chain2_WithZeroLenCsv"), "@ES#",
                                                                                                ".csv");

            Assert.IsTrue(symbolData.Length == 3);
            Assert.AreEqual(Path.GetFileName(symbolData[0].fullFileName), "@ES#_0.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[1].fullFileName), "@ES#_201112.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[2].fullFileName), "@ES#_201010.csv");
        }

        [TestMethod]
        public void TestUltraFastParser_EnumerateTickerFiles5()
        {
            //
            // Complete test for DS-like folder structure enumeration, ES symbol.
            //
            UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] symbolData = UltraFastParser.UltraFastParser
                                                                                            .EnumerateTickerFiles(
                                                                                                Path.Combine(
                                                                                                    strTestsRootPath,
                                                                                                    @"UltraFastParser\Chain3_DSlike\IQFEED\TICKS\FUT"),
                                                                                                "@ES#",
                                                                                                ".csv");

            Assert.IsTrue(symbolData.Length == 23);
            Assert.AreEqual(Path.GetFileName(symbolData[0].fullFileName), "@ES#_0.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[1].fullFileName), "@ES#_201212.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[2].fullFileName), "@ES#_201210.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[3].fullFileName), "@ES#_201010.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[4].fullFileName), "@ES#_201005.csv");

            Assert.AreEqual(Path.GetFileName(symbolData[5].fullFileName), "ES_201006.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[6].fullFileName), "ES_201003.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[7].fullFileName), "ES_200912.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[8].fullFileName), "ES_200909.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[9].fullFileName), "ES_200906.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[10].fullFileName), "ES_200903.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[11].fullFileName), "ES_200812.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[12].fullFileName), "ES_200809.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[13].fullFileName), "ES_200806.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[14].fullFileName), "ES_200803.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[15].fullFileName), "ES_200712.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[16].fullFileName), "ES_200709.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[17].fullFileName), "ES_200706.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[18].fullFileName), "ES_200703.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[19].fullFileName), "ES_200612.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[20].fullFileName), "ES_200609.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[21].fullFileName), "ES_200606.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[22].fullFileName), "ES_200603.csv");

            Assert.AreEqual(symbolData[5].locStartDT, new DateTime(2010, 3, 10, 16, 30, 0));
            Assert.AreEqual(symbolData[5].locEndDT, new DateTime(2010, 5, 1, 2, 0, 8, 700));

            Assert.AreEqual(symbolData[5].utcStartDT, new DateTime(2010, 3, 10, 21, 30, 0));
            Assert.AreEqual(symbolData[5].utcEndDT, new DateTime(2010, 5, 1, 6, 0, 8, 700));
        }

        [TestMethod]
        public void TestUltraFastParser_EnumerateTickerFiles6()
        {
            //
            // Complete test for DS-like folder structure enumeration, XG symbol.
            //
            UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] symbolData = UltraFastParser.UltraFastParser
                                                                                            .EnumerateTickerFiles(
                                                                                                Path.Combine(
                                                                                                    strTestsRootPath,
                                                                                                    @"UltraFastParser\Chain4_DSlike\IQFEED\TICKS\FUT"),
                                                                                                "XG#", ".csv");

            Assert.IsTrue(symbolData.Length == 3);
            Assert.AreEqual(Path.GetFileName(symbolData[0].fullFileName), "XG#_0.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[1].fullFileName), "XG#_201212.csv");
            Assert.AreEqual(Path.GetFileName(symbolData[2].fullFileName), "XG#_0.csv");

            Assert.AreEqual(symbolData[2].locStartDT, new DateTime(2004, 12, 1, 3, 0, 49, 0));
            Assert.AreEqual(symbolData[2].locEndDT, new DateTime(2010, 3, 30, 2, 0, 2, 0));

            Assert.AreEqual(symbolData[2].utcStartDT, new DateTime(2004, 12, 1, 8, 0, 49, 0));
            Assert.AreEqual(symbolData[2].utcEndDT, new DateTime(2010, 3, 30, 6, 0, 2, 0));
        }

        [TestMethod]
        public void TestUltraFastParser_MergeCommonIni_1()
        {
            UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] symbolData = UltraFastParser.UltraFastParser
                                                                                            .EnumerateTickerFiles(
                                                                                                Path.Combine(
                                                                                                    strTestsRootPath,
                                                                                                    @"UltraFastParser/MergeCommonIni1"),
                                                                                                "@ES#", ".csv");

            Assert.IsTrue(symbolData.Length == 15);

            Assert.AreEqual(Path.GetFileName(symbolData[0].fullFileName), "@ES#_201212.csv");
            Assert.AreEqual(symbolData[0].commonIniSettings.TIMEZONE, "NY");

            //
            // TIMEFORMAT for @ES#_201005.csv is overriden in @ES#_201005.ini file.
            //
            Assert.AreEqual(Path.GetFileName(symbolData[14].fullFileName), "@ES#_201005.csv");
            Assert.AreEqual(symbolData[14].commonIniSettings.TIMEZONE, "America/New_York");
        }

        [TestMethod]
        public unsafe void TestUltraFastParser_Seek_1()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Chain4_DSlike/IQFEED/TICKS/FUT/XG#_0.csv"));

            var isOk = parser.SeekLocal(new DateTime(2013, 1, 2, 2, 0, 5, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2013, 1, 2, 2, 0, 5, 000));
            Assert.AreEqual(parser.PQuote->TrdPrice, 7744.50);
            Assert.AreEqual(parser.PQuote->BidPrice, 7744.00);
            Assert.AreEqual(parser.PQuote->AskPrice, 7744.50);
            Assert.AreEqual(parser.PQuote->Volume, 654);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2013, 1, 2, 2, 0, 5, 000));
            Assert.AreEqual(parser.PQuote->TrdPrice, 7744.00);
            Assert.AreEqual(parser.PQuote->BidPrice, 7744.00);
            Assert.AreEqual(parser.PQuote->AskPrice, 7744.50);
            Assert.AreEqual(parser.PQuote->Volume, 1);
        }

        [TestMethod]
        public unsafe void TestUltraFastParser_Seek_2()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Chain4_DSlike/IQFEED/TICKS/FUT/XG#_0.csv"));

            var isOk = parser.SeekLocal(new DateTime(2010, 3, 30, 2, 0, 2, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 3, 30, 2, 0, 2, 000));
            Assert.AreEqual(parser.PQuote->TrdPrice, 6165.50);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->Volume, 152);
        }

        [TestMethod]
        public void TestUltraFastParser_Seek_3_ZeroLenCsv()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath,
                                                                 "UltraFastParser/ZerolenFile/zerolen_0.csv"));

            var isOk = parser.SeekLocal(new DateTime(2010, 3, 30, 2, 0, 2, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsFalse(isOk);
        }

        [TestMethod]
        public void TestUltraFastParser_OpenNonExistentFile()
        {
            //
            // Test that UltraFastParser throws an exception when opening non-existing .csv file.
            //
            bool fCaugthException = false;

            try
            {
                using (new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, @"Non/Exist_0.csv")))
                {
                }
            }
            catch (Exception)
            {
                //
                // Ok, we caught an exception.
                //
                fCaugthException = true;
            }

            Assert.IsTrue(fCaugthException);
        }

        [TestMethod]
        public void TestUltraFastParser_WrongFileNameFormat()
        {
            //
            // Test that UltraFastParser throws an exception when ticker file name is in wrong format (contains no "_" symbol).
            //
            bool fCaugthException = false;

            try
            {
                using (new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, @"UltraFastParser/WrongFileName/WrongFileName.csv")))
                {
                }
            }
            catch (Exception)
            {
                //
                // Ok, we caught an exception.
                //
                fCaugthException = true;
            }

            Assert.IsTrue(fCaugthException);
        }

        [TestMethod]
        public unsafe void TestUltraFastParser_Seek_4_VeryOldDT()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Chain4_DSlike/IQFEED/TICKS/FUT/XG#_0.csv"));

            var isOk = parser.SeekLocal(new DateTime(1950, 1, 1, 0, 0, 0, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2004, 12, 1, 3, 0, 49, 000));
            Assert.AreEqual(parser.PQuote->TrdPrice, 4138.0);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->Volume, 12);
        }

        [TestMethod]
        public unsafe void TestUltraFastParser_Seek_5()
        {
            var parser = new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Seek2/RI#_0.csv"));

            var isOk = parser.SeekLocal(new DateTime(2011, 1, 12, 10, 0, 0));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2011, 3, 3, 17, 5, 8, 210));
            Assert.AreEqual(parser.PQuote->TrdPrice, 201445);
            Assert.AreEqual(parser.PQuote->Volume, 2);
            Assert.AreEqual(parser.PQuote->Id, 280218208UL);
        }

        [TestMethod]
        public unsafe void TestUltraFastParser_Seek_6_UTC()
        {
            var parser = new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Seek2/RI#_0.csv"));

            var isOk = parser.Seek(new DateTime(2010, 1, 11, 7, 30, 0, 81));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 1, 11, 10, 30, 0, 173));
            Assert.AreEqual(parser.PQuote->TrdPrice, 151900);
            Assert.AreEqual(parser.PQuote->Volume, 5);
            Assert.AreEqual(parser.PQuote->Id, 129633023UL);
        }

        [TestMethod]
        public unsafe void TestUltraFastParser_BarData_IQFEED()
        {
            //
            // Test that we can correctly parse IQFEED 1min bar data.
            //
            var parser = new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/BarData_IQFEED/@ES#_1.csv"));

            var isOk = parser.SeekLocal(new DateTime(2013, 8, 30, 17, 2, 0));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2013, 8, 30, 17, 2, 0));
            Assert.AreEqual(parser.PQuote->Open, 1632.25);
            Assert.AreEqual(parser.PQuote->High, 1632.50);
            Assert.AreEqual(parser.PQuote->Low, 1632.25);
            Assert.AreEqual(parser.PQuote->Close, 1632.25);
            Assert.AreEqual(parser.PQuote->Volume, 92);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2013, 8, 30, 17, 3, 0));
            Assert.AreEqual(parser.PQuote->Open, 1632.25);
            Assert.AreEqual(parser.PQuote->High, 1632.50);
            Assert.AreEqual(parser.PQuote->Low, 1632.25);
            Assert.AreEqual(parser.PQuote->Close, 1632.50);
            Assert.AreEqual(parser.PQuote->Volume, 179);
        }

        [TestMethod]
        public unsafe void TestUltraFastParser_BarData_IQFEED_Seek()
        {
            //
            // Test that we can correctly parse IQFEED 1min bar data.
            //
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath,
                                                                 "UltraFastParser/BarData_IQFEED/@ES#_1.csv"));

            var isOk = parser.SeekLocal(new DateTime(2013, 8, 30, 17, 3, 0));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2013, 8, 30, 17, 3, 0));
            Assert.AreEqual(parser.PQuote->Open, 1632.25);
            Assert.AreEqual(parser.PQuote->High, 1632.50);
            Assert.AreEqual(parser.PQuote->Low, 1632.25);
            Assert.AreEqual(parser.PQuote->Close, 1632.50);
            Assert.AreEqual(parser.PQuote->Volume, 179);
        }

        [TestMethod]
        public unsafe void TestUltraFastParser_ReadSwitchCsvFile()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Chain4_DSlike/IQFEED/TICKS/FUT/XG#_0.csv"));

            var isOk = parser.SeekLocal(new DateTime(1950, 1, 1, 0, 0, 0, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2004, 12, 1, 3, 0, 49, 000));
            Assert.AreEqual(parser.PQuote->TrdPrice, 4138.0);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->Volume, 12);

            for (int i = 0; i < 6; ++i)
            {
                isOk = parser.Read();
                Assert.IsTrue(isOk);
            }

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.AreEqual(parser.PQuote->Dt, new DateTime(2010, 3, 30, 2, 0, 2, 000));
            Assert.AreEqual(parser.PQuote->TrdPrice, 6165.50);
            Assert.AreEqual(parser.PQuote->BidPrice, 0);
            Assert.AreEqual(parser.PQuote->AskPrice, 0);
            Assert.AreEqual(parser.PQuote->Volume, 152);
        }

        private static void TestNavigatedWithOutFuture(List<long> lst, List<DateTime> dts, DateTime startDt)
        {
            var iwn = new IndicatorWindowNavigated<long>(5, 0,
                                                         new SequentialReaderMock<long>(lst.ToArray(), dts.ToArray(),
                                                                                        Interval.Sec1), -1);
            iwn.NavigateToDate(startDt);

            Assert.AreEqual(iwn[0], (object) lst[0], "Case1");
            Assert.AreEqual(iwn.Filled, (object) false, "Case1");

            iwn.NavigateToDate(startDt.AddMilliseconds(Interval.Sec1.TotalMilliseconds - 1));

            Assert.AreEqual(iwn[0], (object) lst[0], "Case2");

            iwn.NavigateToDate(startDt.AddMilliseconds(Interval.Sec1.TotalMilliseconds + 1));

            Assert.AreEqual(iwn[0], (object) lst[1], "Case3");
            Assert.AreEqual(iwn[-1], (object) lst[0], "Case3");

            iwn.NavigateToDate(startDt.AddMilliseconds(4*Interval.Sec1.TotalMilliseconds));

            Assert.AreEqual(iwn[0], (object) lst[4], "Case4");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(999)], (object) lst[4], "Case4");

            Assert.AreEqual(iwn[-1], (object) lst[3], "Case4");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(-1)], (object) lst[3], "Case4");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(-1000)], (object) lst[3], "Case4");

            Assert.AreEqual(iwn[-2], (object) lst[2], "Case4");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(-1001)], (object) lst[2], "Case4");

            Assert.AreEqual(iwn[-3], (object) lst[1], "Case4");
            Assert.AreEqual(iwn[-4], (object) lst[0], "Case4");
            Assert.AreEqual(true, (object) iwn.Filled, "Case4");
        }

        private static void TestNavigatedWithFutureAndCurrent(List<long> lst, List<DateTime> dts, DateTime startDt)
        {
            var iwn = new IndicatorWindowNavigated<long>(1, 3,
                                                         new SequentialReaderMock<long>(lst.ToArray(), dts.ToArray(),
                                                                                        Interval.Sec1));
            iwn.NavigateToDate(startDt);

            Assert.AreEqual(iwn[0], (object) lst[0], "Case1");
            Assert.AreEqual(iwn[1], (object) lst[1], "Case1");
            Assert.AreEqual(iwn[2], (object) lst[2], "Case1");

            iwn.NavigateToDate(startDt.AddMilliseconds(Interval.Sec1.TotalMilliseconds - 1));

            Assert.AreEqual(iwn[0], (object) lst[0], "Case2");
            Assert.AreEqual(iwn[1], (object) lst[1], "Case2");
            Assert.AreEqual(iwn[2], (object) lst[2], "Case2");

            iwn.NavigateToDate(startDt.AddMilliseconds(4*Interval.Sec1.TotalMilliseconds));

            Assert.AreEqual(iwn[0], (object) lst[4], "Case3");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(999)], (object) lst[4], "Case3");
            Assert.AreEqual(iwn[1], (object) lst[5], "Case3");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(1000)], (object) lst[5], "Case3");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(1001)], (object) lst[5], "Case3");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(1999)], (object) lst[5], "Case3");
            Assert.AreEqual(iwn[2], (object) lst[6], "Case3");
            Assert.AreEqual(iwn[TimeSpan.FromMilliseconds(2000)], (object) lst[6], "Case3");
        }

        private class IntObj
        {
            //
            // We need this class to wrap "int" into some reference type (to properly test ring buffer).
            //
            public IntObj()
            {
                val = 0;
            }

            public IntObj(int a)
            {
                val = a;
            }

            public int val { get; set; }
        }
    }
}
