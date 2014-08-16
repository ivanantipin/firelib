package firelib.common;

import java.net.URL;

public class ParserTests
    {



        String getfile(String name){
            URL resource = this.getClass().getClassLoader().getResource(name);
            return resource.getFile();
        }


        /*private static String strTestsRootPath = @"..\..\..\Tests";



        //-------------------------------------------------------------





        @Test
        public void TestUltraFastSingleCsvParser_4()
        {
            //
            // Test that parser.StartDt and parser.EndDt contain DateTime.MinValue, DateTime.MaxValue for zero length files.
            //
            CommonIniSettings commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new String[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/ZerolenFile/zerolen_0.csv"),
                                             commonIniSettings);

            Assert.assertEquals(parser.StartDt, DateTime.MinValue);
            Assert.assertEquals(parser.EndDt, DateTime.MaxValue);
        }

        @Test
        public void TestUltraFastSingleCsvParser_5()
        {
            //
            // Test that parser.StartDt and parser.EndDt contain DateTime.MinValue, DateTime.MaxValue
            // for file with incomplete first String that contains only date and time.
            //
            CommonIniSettings commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new String[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"};
            commonIniSettings.TIMEZONE = "NY";

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/LabudaFile/labuda_0.csv"),
                                             commonIniSettings);

            Assert.assertEquals(parser.StartDt, new Instant( 2011, 11, 21, 2, 0, 0, 0));
            Assert.assertEquals(parser.EndDt, new Instant( 2011, 11, 21, 2, 0, 0, 0));
        }


        @Test
        public void TestUltraFastSingleCsvParser_SKIP_SYMBOL()
        {
            //
            // Check that IQFEED format parsing works.
            //
            CommonIniSettings commonIniSettings = new CommonIniSettings();
            commonIniSettings.DATEFORMAT = "DD.MM.YYYY";
            commonIniSettings.TIMEFORMAT = "HHMMSS";
            commonIniSettings.COLUMNFORMAT = new[] {"D", "T", "O", "H", "L", "C", "V", "#", "#", "A", "B", "P", "U", "I"};
            //D_T_O_H_L_C_V_#_#_A_B_P_U_I
            commonIniSettings.TIMEZONE = "LONDON";

            *//*
11.11.2010,161100,1401.548,1401.892,1400.197,1400.947,1,1402.52,1402.352,1400.703,1401.453,1,111
11.11.2010,161200,1400.992,1401.598,1400.992,1401.347,1,1401.453,1402.102,1401.453,1401.853,1,102
11.11.2010,161300,1401.397,1401.397,1399.997,1400.448,1,1401.858,1401.858,1400.502,1400.952,1,116
             *//*

            var parser =
                new UltraFastSingleCsvParser(Path.Combine(strTestsRootPath, @"UltraFastParser/BarData_DUKAS_SKIP_SYMBOL/XAUUSD_1.csv"),
                                             commonIniSettings);

            parser.SeekLocal(parser.StartDt);

            Assert.IsTrue(parser.Read());
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 11, 11, 16, 11, 0));
            Assert.assertEquals(parser.PQuote->Close, 1400.947, 0.001);
            Assert.assertEquals(parser.PQuote->Volume, 1);
            Assert.assertEquals(parser.PQuote->AskPrice, 1400.703, 0.001);
            Assert.IsTrue(parser.Read());
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 11, 11, 16, 12, 0));
            Assert.assertEquals(parser.PQuote->Close, 1401.347, 0.001);
            Assert.assertEquals(parser.PQuote->AskPrice, 1401.453, 0.001);
            Assert.IsTrue(parser.Read());
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 11, 11, 16, 13, 0));
            Assert.assertEquals(parser.PQuote->Close, 1400.448, 0.001);
            Assert.assertEquals(parser.PQuote->AskPrice, 1400.502, 0.001);
        }

        @Test
        public void TestUltraFastSingleCsvParser_ANFUTURES_7()
        {
            //
            // Check that ANFUTURES format parsing works.
            //
            CommonIniSettings commonIniSettings = new CommonIniSettings();
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
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 3, 10, 16, 30, 0, 0));
            Assert.assertEquals(parser.PQuote->TrdPrice, 1140.50);
            Assert.assertEquals(parser.PQuote->Volume, 18);
            Assert.assertEquals(parser.PQuote->AskPrice, 0);
            Assert.assertEquals(parser.PQuote->BidPrice, 0);
            Assert.assertEquals(parser.PQuote->CumVolume, 0);
            Assert.assertEquals(parser.PQuote->Id, 0UL);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 3, 10, 16, 30, 10, 0));
            Assert.assertEquals(parser.PQuote->TrdPrice, 1140.50);
            Assert.assertEquals(parser.PQuote->Volume, 1);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 4, 16, 16, 14, 59, 0));
            Assert.assertEquals(parser.PQuote->TrdPrice, 1189.75);
            Assert.assertEquals(parser.PQuote->Volume, 4);
        }

        @Test
        public void TestUltraFastSingleCsvParser_ANFUTURES_8_Dummy_Fields()
        {
            //
            // Check that ANFUTURES format parsing works.
            //
            CommonIniSettings commonIniSettings = new CommonIniSettings();
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
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 3, 10, 16, 30, 0, 0));
            Assert.assertEquals(parser.PQuote->TrdPrice, 1140.50);
            Assert.assertEquals(parser.PQuote->Volume, 18);

            //
            // The following fields do not present in COLUMNFORMAT.
            // Verify they are set to zeroes.
            //
            Assert.assertEquals(parser.PQuote->AskPrice, 0);
            Assert.assertEquals(parser.PQuote->BidPrice, 0);
            Assert.assertEquals(parser.PQuote->CumVolume, 0);
            Assert.assertEquals(parser.PQuote->Id, 0UL);

            //
            // The following fields are defined only for bars data, and must be set to zeroes
            // for ANFUTURES.COM tick data.
            //
            Assert.assertEquals(parser.PQuote->Open, 0);
            Assert.assertEquals(parser.PQuote->High, 0);
            Assert.assertEquals(parser.PQuote->Low, 0);
            Assert.assertEquals(parser.PQuote->Close, 0);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 3, 10, 16, 30, 10, 0));
            Assert.assertEquals(parser.PQuote->TrdPrice, 1140.50);
            Assert.assertEquals(parser.PQuote->Volume, 1);

            Assert.assertEquals(parser.PQuote->AskPrice, 0);
            Assert.assertEquals(parser.PQuote->BidPrice, 0);
            Assert.assertEquals(parser.PQuote->CumVolume, 0);
            Assert.assertEquals(parser.PQuote->Id, 0UL);
        }

        @Test
        public void TestUltraFastSingleCsvParser_TICKDATA_8()
        {
            //
            // Check that TICKDATA format parsing works.
            //
            CommonIniSettings commonIniSettings = new CommonIniSettings();
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
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2004, 12, 1, 3, 0, 49, 0));
            Assert.assertEquals(parser.PQuote->TrdPrice, 4138.0);
            Assert.assertEquals(parser.PQuote->Volume, 12);

            isOk = parser.Read();

            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2004, 12, 1, 3, 2, 3, 0));
            Assert.assertEquals(parser.PQuote->TrdPrice, 4140.0);
            Assert.assertEquals(parser.PQuote->Volume, 1);
        }



        @Test
        public void TestUltraFastParser_ParseSymbolFileName()
        {
            String strPath;
            String strSymbol;
            String strExt;

            UltraFastParser.UltraFastParser.ParseSymbolFileName(
                Path.Combine(strTestsRootPath, @"Tests/TickersSorting/RI#_0.csv"), out strPath, out strSymbol,
                out strExt);

            String strAbsPath = Path.GetFullPath(Path.Combine(strTestsRootPath, @"Tests/TickersSorting"));

            Assert.assertEquals(strPath, strAbsPath);
            Assert.assertEquals(strSymbol, "RI#");
            Assert.assertEquals(strExt, ".csv");
        }

        @Test
        public void TestUltraFastParser_ParseAndMergeCommonIni_1()
        {
            CommonIniSettings commonIniSettings = new CommonIniSettings();
            UltraFastParser.UltraFastParser.ParseAndMergeCommonIni(Path.Combine(strTestsRootPath, @"Seek1/common.ini"),
                                                                   ref commonIniSettings);

            Assert.assertEquals(commonIniSettings.DATEFORMAT, "YYYY-MM-DD");
            Assert.assertEquals(commonIniSettings.TIMEFORMAT, "HH:MM:SS");
            Assert.IsTrue(commonIniSettings.COLUMNFORMAT.SequenceEqual(new[] {"D", "T", "#", "P", "V", "I"}));
            Assert.assertEquals(commonIniSettings.TIMEZONE, "MOSCOW");
        }

        @Test
        public void TestUltraFastParser_ParseAndMergeCommonIni_2()
        {
            CommonIniSettings commonIniSettings = new CommonIniSettings();
            UltraFastParser.UltraFastParser.ParseAndMergeCommonIni(
                Path.Combine(strTestsRootPath, @"CommonIni1/common.ini"),
                ref commonIniSettings);

            Assert.IsFalse(String.IsNullOrEmpty(commonIniSettings.PREVFILE));
            Assert.IsTrue(Directory.Exists(commonIniSettings.PREVFILE));
            Assert.IsTrue(Path.IsPathRooted(commonIniSettings.PREVFILE));

            Assert.assertEquals(commonIniSettings.DATEFORMAT, "DD.MM.YYYY");
            Assert.assertEquals(commonIniSettings.TIMEFORMAT, "HHMMSS");
            Assert.IsTrue(
                commonIniSettings.COLUMNFORMAT.SequenceEqual(new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"}));
            Assert.assertEquals(commonIniSettings.TIMEZONE, "NY");
        }

        @Test
        public void TestUltraFastParser_ParseAndMergeCommonIni_3()
        {
            CommonIniSettings commonIniSettings = new CommonIniSettings();
            UltraFastParser.UltraFastParser.ParseAndMergeCommonIni(
                Path.Combine(strTestsRootPath, @"CommonIni2/common.ini"),
                ref commonIniSettings);

            Assert.IsFalse(String.IsNullOrEmpty(commonIniSettings.NEXTFILE));
            Assert.IsTrue(Directory.Exists(commonIniSettings.NEXTFILE));
            Assert.IsTrue(Path.IsPathRooted(commonIniSettings.NEXTFILE));

            Assert.assertEquals(commonIniSettings.DATEFORMAT, "DD.MM.YYYY");
            Assert.assertEquals(commonIniSettings.TIMEFORMAT, "HHMMSS");
            Assert.IsTrue(
                commonIniSettings.COLUMNFORMAT.SequenceEqual(new[] {"D", "T", "#", "P", "V", "U", "B", "A", "I"}));
            Assert.assertEquals(commonIniSettings.TIMEZONE, "NY");
        }

        @Test
        public void TestUltraFastParser_EnumerateTickerFiles()
        {
            UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] symbolData = UltraFastParser.UltraFastParser
                                                                                            .EnumerateTickerFiles(
                                                                                                Path.Combine(
                                                                                                    strTestsRootPath,
                                                                                                    @"CommonIni1"),
                                                                                                "@ES#", ".csv");

            Assert.IsTrue(symbolData.Length == 16);
            Assert.assertEquals(Path.GetFileName(symbolData[0].fullFileName), "@ES#_0.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[1].fullFileName), "@ES#_201212.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[2].fullFileName), "@ES#_201210.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[3].fullFileName), "@ES#_201208.csv");

            Assert.assertEquals(Path.GetFileName(symbolData[15].fullFileName), "@ES#_201005.csv");
        }

        @Test
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

        @Test
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
            Assert.assertEquals(Path.GetFileName(symbolData[0].fullFileName), "@ES#_0.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[1].fullFileName), "@ES#_201112.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[2].fullFileName), "@ES#_201102.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[3].fullFileName), "@ES#_201010.csv");
        }

        @Test
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
            Assert.assertEquals(Path.GetFileName(symbolData[0].fullFileName), "@ES#_0.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[1].fullFileName), "@ES#_201112.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[2].fullFileName), "@ES#_201010.csv");
        }

        @Test
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
            Assert.assertEquals(Path.GetFileName(symbolData[0].fullFileName), "@ES#_0.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[1].fullFileName), "@ES#_201212.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[2].fullFileName), "@ES#_201210.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[3].fullFileName), "@ES#_201010.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[4].fullFileName), "@ES#_201005.csv");

            Assert.assertEquals(Path.GetFileName(symbolData[5].fullFileName), "ES_201006.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[6].fullFileName), "ES_201003.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[7].fullFileName), "ES_200912.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[8].fullFileName), "ES_200909.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[9].fullFileName), "ES_200906.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[10].fullFileName), "ES_200903.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[11].fullFileName), "ES_200812.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[12].fullFileName), "ES_200809.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[13].fullFileName), "ES_200806.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[14].fullFileName), "ES_200803.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[15].fullFileName), "ES_200712.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[16].fullFileName), "ES_200709.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[17].fullFileName), "ES_200706.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[18].fullFileName), "ES_200703.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[19].fullFileName), "ES_200612.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[20].fullFileName), "ES_200609.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[21].fullFileName), "ES_200606.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[22].fullFileName), "ES_200603.csv");

            Assert.assertEquals(symbolData[5].locStartDT, new Instant( 2010, 3, 10, 16, 30, 0));
            Assert.assertEquals(symbolData[5].locEndDT, new Instant( 2010, 5, 1, 2, 0, 8, 700));

            Assert.assertEquals(symbolData[5].utcStartDT, new Instant( 2010, 3, 10, 21, 30, 0));
            Assert.assertEquals(symbolData[5].utcEndDT, new Instant( 2010, 5, 1, 6, 0, 8, 700));
        }

        @Test
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
            Assert.assertEquals(Path.GetFileName(symbolData[0].fullFileName), "XG#_0.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[1].fullFileName), "XG#_201212.csv");
            Assert.assertEquals(Path.GetFileName(symbolData[2].fullFileName), "XG#_0.csv");

            Assert.assertEquals(symbolData[2].locStartDT, new Instant( 2004, 12, 1, 3, 0, 49, 0));
            Assert.assertEquals(symbolData[2].locEndDT, new Instant( 2010, 3, 30, 2, 0, 2, 0));

            Assert.assertEquals(symbolData[2].utcStartDT, new Instant( 2004, 12, 1, 8, 0, 49, 0));
            Assert.assertEquals(symbolData[2].utcEndDT, new Instant( 2010, 3, 30, 6, 0, 2, 0));
        }

        @Test
        public void TestUltraFastParser_MergeCommonIni_1()
        {
            UltraFastParser.UltraFastParser.SymbolCsvFileInfo[] symbolData = UltraFastParser.UltraFastParser
                                                                                            .EnumerateTickerFiles(
                                                                                                Path.Combine(
                                                                                                    strTestsRootPath,
                                                                                                    @"UltraFastParser/MergeCommonIni1"),
                                                                                                "@ES#", ".csv");

            Assert.IsTrue(symbolData.Length == 15);

            Assert.assertEquals(Path.GetFileName(symbolData[0].fullFileName), "@ES#_201212.csv");
            Assert.assertEquals(symbolData[0].commonIniSettings.TIMEZONE, "NY");

            //
            // TIMEFORMAT for @ES#_201005.csv is overriden in @ES#_201005.ini file.
            //
            Assert.assertEquals(Path.GetFileName(symbolData[14].fullFileName), "@ES#_201005.csv");
            Assert.assertEquals(symbolData[14].commonIniSettings.TIMEZONE, "America/New_York");
        }

        @Test
        public void TestUltraFastParser_Seek_1()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Chain4_DSlike/IQFEED/TICKS/FUT/XG#_0.csv"));

            var isOk = parser.SeekLocal(new Instant( 2013, 1, 2, 2, 0, 5, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2013, 1, 2, 2, 0, 5, 000));
            Assert.assertEquals(parser.PQuote->TrdPrice, 7744.50);
            Assert.assertEquals(parser.PQuote->BidPrice, 7744.00);
            Assert.assertEquals(parser.PQuote->AskPrice, 7744.50);
            Assert.assertEquals(parser.PQuote->Volume, 654);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2013, 1, 2, 2, 0, 5, 000));
            Assert.assertEquals(parser.PQuote->TrdPrice, 7744.00);
            Assert.assertEquals(parser.PQuote->BidPrice, 7744.00);
            Assert.assertEquals(parser.PQuote->AskPrice, 7744.50);
            Assert.assertEquals(parser.PQuote->Volume, 1);
        }

        @Test
        public void TestUltraFastParser_Seek_2()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Chain4_DSlike/IQFEED/TICKS/FUT/XG#_0.csv"));

            var isOk = parser.SeekLocal(new Instant( 2010, 3, 30, 2, 0, 2, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 3, 30, 2, 0, 2, 000));
            Assert.assertEquals(parser.PQuote->TrdPrice, 6165.50);
            Assert.assertEquals(parser.PQuote->BidPrice, 0);
            Assert.assertEquals(parser.PQuote->AskPrice, 0);
            Assert.assertEquals(parser.PQuote->Volume, 152);
        }

        @Test
        public void TestUltraFastParser_Seek_3_ZeroLenCsv()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath,
                                                                 "UltraFastParser/ZerolenFile/zerolen_0.csv"));

            var isOk = parser.SeekLocal(new Instant( 2010, 3, 30, 2, 0, 2, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsFalse(isOk);
        }

        @Test
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

        @Test
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

        @Test
        public void TestUltraFastParser_Seek_4_VeryOldDT()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Chain4_DSlike/IQFEED/TICKS/FUT/XG#_0.csv"));

            var isOk = parser.SeekLocal(new Instant( 1950, 1, 1, 0, 0, 0, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2004, 12, 1, 3, 0, 49, 000));
            Assert.assertEquals(parser.PQuote->TrdPrice, 4138.0);
            Assert.assertEquals(parser.PQuote->BidPrice, 0);
            Assert.assertEquals(parser.PQuote->AskPrice, 0);
            Assert.assertEquals(parser.PQuote->Volume, 12);
        }

        @Test
        public void TestUltraFastParser_Seek_5()
        {
            var parser = new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Seek2/RI#_0.csv"));

            var isOk = parser.SeekLocal(new Instant( 2011, 1, 12, 10, 0, 0));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2011, 3, 3, 17, 5, 8, 210));
            Assert.assertEquals(parser.PQuote->TrdPrice, 201445);
            Assert.assertEquals(parser.PQuote->Volume, 2);
            Assert.assertEquals(parser.PQuote->Id, 280218208UL);
        }

        @Test
        public void TestUltraFastParser_Seek_6_UTC()
        {
            var parser = new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Seek2/RI#_0.csv"));

            var isOk = parser.Seek(new Instant( 2010, 1, 11, 7, 30, 0, 81));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 1, 11, 10, 30, 0, 173));
            Assert.assertEquals(parser.PQuote->TrdPrice, 151900);
            Assert.assertEquals(parser.PQuote->Volume, 5);
            Assert.assertEquals(parser.PQuote->Id, 129633023UL);
        }

        @Test
        public void TestUltraFastParser_BarData_IQFEED()
        {
            //
            // Test that we can correctly parse IQFEED 1min bar data.
            //
            var parser = new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/BarData_IQFEED/@ES#_1.csv"));

            var isOk = parser.SeekLocal(new Instant( 2013, 8, 30, 17, 2, 0));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2013, 8, 30, 17, 2, 0));
            Assert.assertEquals(parser.PQuote->Open, 1632.25);
            Assert.assertEquals(parser.PQuote->High, 1632.50);
            Assert.assertEquals(parser.PQuote->Low, 1632.25);
            Assert.assertEquals(parser.PQuote->Close, 1632.25);
            Assert.assertEquals(parser.PQuote->Volume, 92);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2013, 8, 30, 17, 3, 0));
            Assert.assertEquals(parser.PQuote->Open, 1632.25);
            Assert.assertEquals(parser.PQuote->High, 1632.50);
            Assert.assertEquals(parser.PQuote->Low, 1632.25);
            Assert.assertEquals(parser.PQuote->Close, 1632.50);
            Assert.assertEquals(parser.PQuote->Volume, 179);
        }

        @Test
        public void TestUltraFastParser_BarData_IQFEED_Seek()
        {
            //
            // Test that we can correctly parse IQFEED 1min bar data.
            //
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath,
                                                                 "UltraFastParser/BarData_IQFEED/@ES#_1.csv"));

            var isOk = parser.SeekLocal(new Instant( 2013, 8, 30, 17, 3, 0));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2013, 8, 30, 17, 3, 0));
            Assert.assertEquals(parser.PQuote->Open, 1632.25);
            Assert.assertEquals(parser.PQuote->High, 1632.50);
            Assert.assertEquals(parser.PQuote->Low, 1632.25);
            Assert.assertEquals(parser.PQuote->Close, 1632.50);
            Assert.assertEquals(parser.PQuote->Volume, 179);
        }

        @Test
        public void TestUltraFastParser_ReadSwitchCsvFile()
        {
            var parser =
                new UltraFastParser.UltraFastParser(Path.Combine(strTestsRootPath, "UltraFastParser/Chain4_DSlike/IQFEED/TICKS/FUT/XG#_0.csv"));

            var isOk = parser.SeekLocal(new Instant( 1950, 1, 1, 0, 0, 0, 000));
            Assert.IsTrue(isOk);

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2004, 12, 1, 3, 0, 49, 000));
            Assert.assertEquals(parser.PQuote->TrdPrice, 4138.0);
            Assert.assertEquals(parser.PQuote->BidPrice, 0);
            Assert.assertEquals(parser.PQuote->AskPrice, 0);
            Assert.assertEquals(parser.PQuote->Volume, 12);

            for (int i = 0; i < 6; ++i)
            {
                isOk = parser.Read();
                Assert.IsTrue(isOk);
            }

            isOk = parser.Read();
            Assert.IsTrue(isOk);
            Assert.assertEquals(parser.PQuote->Dt, new Instant( 2010, 3, 30, 2, 0, 2, 000));
            Assert.assertEquals(parser.PQuote->TrdPrice, 6165.50);
            Assert.assertEquals(parser.PQuote->BidPrice, 0);
            Assert.assertEquals(parser.PQuote->AskPrice, 0);
            Assert.assertEquals(parser.PQuote->Volume, 152);
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
*/    }

