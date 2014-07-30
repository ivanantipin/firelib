using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using IniParser;
using UltraFastParser.TimeZone;

namespace UltraFastParser
{
    //
    // Структура, которая содержит настройкий из common.ini файлов.
    //
    public class CommonIniSettings
    {
        public string[] COLUMNFORMAT; // list of tokens: 'D', 'T', '#', 'P', 'V', 'I'.
        public string DATEFORMAT;
        public string NEXTFILE; // ParseAndMergeCommonIni() expanthis this to absolute path!
        public string PREVFILE; // ParseAndMergeCommonIni() expanthis this to absolute path!

        public string TIMEFORMAT;
        public string TIMEZONE;
    }

    public unsafe class UltraFastParser: ISimpleReader
    {
        private UltraFastSingleCsvParser csvParser = null;
        private SymbolCsvFileInfo[] symbolCsvFileInfo = null;
        private int symbolCsvFileInfoIndex = -1;

        public UltraFastParser(string csvFileName)
        {
            if (!File.Exists(csvFileName))
            {
                throw new Exception("Error: input file '" + csvFileName + "' does not exist.");
            }

            var fileNameWithoutExtension = Path.GetFileNameWithoutExtension(csvFileName);

            if (!fileNameWithoutExtension.Contains("_"))
            {
                throw new Exception("Error: wrong filename format, no '_' symbol: '" + csvFileName + "'.");
            }

            string strPath, strSymbol, strExt;
            ParseSymbolFileName(csvFileName, out strPath, out strSymbol, out strExt);
            symbolCsvFileInfo = EnumerateTickerFiles(strPath, strSymbol, strExt);
        }

        public RecordQuote* PQuote
        {
            get { return csvParser.PQuote; }
        }

        private void OpenCurrentCsvFile()
        {
            csvParser = new UltraFastSingleCsvParser(symbolCsvFileInfo[symbolCsvFileInfoIndex].fullFileName, symbolCsvFileInfo[symbolCsvFileInfoIndex].commonIniSettings);
            csvParser.SeekLocal(csvParser.StartDt);
            CurrentTz = symbolCsvFileInfo[symbolCsvFileInfoIndex].commonIniSettings.TIMEZONE;
        }

        public string CurrentTz{get; set; }

        public void UpdateTimeZoneOffset()
        {
            csvParser.UpdateGmtOffset(CurrentTz);
        }

        public bool Seek(DateTime utcDT)
        {
            symbolCsvFileInfoIndex = -1;
            csvParser = null;

            if (symbolCsvFileInfo.Length == 0)
            {
                return true;
            }

            for (var i = 0; i < symbolCsvFileInfo.Length; ++i)
            {
                if (utcDT >= symbolCsvFileInfo[i].utcStartDT)
                {
                    symbolCsvFileInfoIndex = i;
                    break;
                }
            }

            if (symbolCsvFileInfoIndex == -1)
            {
                //
                // dtloc is very very old date. Set index to point to oldest .csv file.
                //
                symbolCsvFileInfoIndex = symbolCsvFileInfo.Length - 1;
            }

            OpenCurrentCsvFile();

            if (utcDT < symbolCsvFileInfo[symbolCsvFileInfoIndex].utcStartDT)
            {
                utcDT = symbolCsvFileInfo[symbolCsvFileInfoIndex].utcStartDT;
            }

            if (utcDT > symbolCsvFileInfo[symbolCsvFileInfoIndex].utcEndDT)
            {
                utcDT = symbolCsvFileInfo[symbolCsvFileInfoIndex].utcEndDT;
            }

            var seekLocal = csvParser.SeekLocal(utcDT.FromGmt(symbolCsvFileInfo[symbolCsvFileInfoIndex].commonIniSettings.TIMEZONE));
            UpdateTimeZoneOffset();
            return seekLocal;
        }

        public bool SeekLocal(DateTime locDT)
        {
            symbolCsvFileInfoIndex = -1;
            csvParser = null;

            if (symbolCsvFileInfo.Length == 0)
            {
                return true;
            }

            for (var i = 0; i < symbolCsvFileInfo.Length; ++i)
            {
                if (locDT >= symbolCsvFileInfo[i].locStartDT)
                {
                    symbolCsvFileInfoIndex = i;
                    break;
                }
            }

            if (symbolCsvFileInfoIndex == -1)
            {
                //
                // dtloc is very very old date. Set index to point to oldest .csv file.
                //
                symbolCsvFileInfoIndex = symbolCsvFileInfo.Length - 1;
            }

            OpenCurrentCsvFile();

            if (locDT < symbolCsvFileInfo[symbolCsvFileInfoIndex].locStartDT)
            {
                locDT = symbolCsvFileInfo[symbolCsvFileInfoIndex].locStartDT;
            }

            if (locDT > symbolCsvFileInfo[symbolCsvFileInfoIndex].locEndDT)
            {
                locDT = symbolCsvFileInfo[symbolCsvFileInfoIndex].locEndDT;
            }

            var seekLocal = csvParser.SeekLocal(locDT);
            UpdateTimeZoneOffset();
            return seekLocal;
        }

        public bool Read()
        {
            if (csvParser == null)
                return false;

            if (csvParser.Read() && (csvParser.PQuote->Dt <= symbolCsvFileInfo[symbolCsvFileInfoIndex].locEndDT))
            {
                return true;
            }

            //
            // Switch to next .csv file.
            //
            if (symbolCsvFileInfoIndex == 0)
            {
                return false;
            }

            symbolCsvFileInfoIndex--;
            OpenCurrentCsvFile();
            return csvParser.Read();
        }

        public TimeSpan CurrentTimeZoneOffset {
            get
            {
                return csvParser.GmtOffset;
            }
        }

        public void Dispose()
        {
            lock (this)
            {
                csvParser.Dispose();
                csvParser = null;
                symbolCsvFileInfo = null;
                symbolCsvFileInfoIndex = -1;
            }
        }

        //
        // Раскрывает относительный путь PREVFILE или NEXTFILE из common.ini в абсолютный.
        //
        // NOTE: Сделан специальный workaround чтобы учесть что пути вроде:
        //
        //       [CHAIN]
        //       PREVFILE=\201212\
        //
        //       начинающиеся со слэша в common.ini - на самом деле относительные, хотя Path.IsPathRooted()
        //       вполне логично трактует их как абсолютные.
        //
        public static string ExpandRelativeChainPathToAbsolute(string strCommonIniPath, string strChainPath)
        {
            string strBasePath = Path.GetDirectoryName(strCommonIniPath);
            strChainPath = strChainPath.TrimStart('\\');

            if (!Path.IsPathRooted(strChainPath))
            {
                string combinedPath = Path.Combine(strBasePath, strChainPath);
                strChainPath = Path.GetFullPath(combinedPath);
            }

            return strChainPath;
        }

        static object lck = new object();

        //
        // Парзает входной common.ini файл, и мерджит настройки с теми, что уже содержатся в commonIniSettings.
        //
        public static void ParseAndMergeCommonIni(string strFileName, ref CommonIniSettings commonIniSettings)
        {
            if (commonIniSettings == null)
            {
                commonIniSettings = new CommonIniSettings();
            }

            lock (lck)
            {
                var parser = new FileIniDataParser();
                IniData settings = parser.LoadFile(strFileName);

                if (settings.Sections.ContainsSection("CHAIN"))
                {
                    if (settings["CHAIN"].ContainsKey("NEXTFILE"))
                    {
                        commonIniSettings.NEXTFILE = ExpandRelativeChainPathToAbsolute(strFileName,
                                                                                       settings["CHAIN"]["NEXTFILE"]);
                    }

                    if (settings["CHAIN"].ContainsKey("PREVFILE"))
                    {
                        commonIniSettings.PREVFILE = ExpandRelativeChainPathToAbsolute(strFileName,
                                                                                       settings["CHAIN"]["PREVFILE"]);
                    }
                }

                if (settings.Sections.ContainsSection("FORMATS"))
                {
                    if (settings["FORMATS"].ContainsKey("DATEFORMAT"))
                    {
                        commonIniSettings.DATEFORMAT = settings["FORMATS"]["DATEFORMAT"];
                    }

                    if (settings["FORMATS"].ContainsKey("TIMEFORMAT"))
                    {
                        commonIniSettings.TIMEFORMAT = settings["FORMATS"]["TIMEFORMAT"];
                    }

                    if (settings["FORMATS"].ContainsKey("COLUMNFORMAT"))
                    {
                        commonIniSettings.COLUMNFORMAT = settings["FORMATS"]["COLUMNFORMAT"].Split('_');
                    }

                    if (settings["FORMATS"].ContainsKey("TIMEZONE"))
                    {
                        commonIniSettings.TIMEZONE = settings["FORMATS"]["TIMEZONE"];
                    }
                }

            }

        }

        //
        // Parses given symbol file name and returns <path>, <symbol> and <ext> components.
        //
        // NOTE: Path is always returned in absolute form.
        //
        // For example, for input string:
        //
        //    .\Tests\TickersSorting\RI#_0.csv
        //
        // Returns:
        //     strPath   = @"C:\GLOBALDATABASE\Tests\TickersSorting"
        //     strSymbol = @"RI#"
        //     strExt    = @".csv"
        //
        public static void ParseSymbolFileName(string fullPathName, out string strPath, out string strSymbol,
                                               out string strExt)
        {
            strPath = Path.GetFullPath(Path.GetDirectoryName(fullPathName));
            strSymbol = Path.GetFileNameWithoutExtension(fullPathName).Split('_')[0];
            strExt = Path.GetExtension(fullPathName);
        }

        //
        // This routine is initially called by EnumerateTickerFiles() with level == 0 and empty visitedPathList.
        // Then EnumerateTickerFilesHelper() recursively calls itself to walk through all PREVFILE references.
        //
        public static SymbolCsvFileInfo[] EnumerateTickerFilesHelper(string strPath, string strSymbol, string strExt,
                                                                     ref DateTime cutDatesHigherThan, ref int level,
                                                                     ref List<string> visitedPathList)
        {
            //
            // Normalize strPath. And check where we already visited it.
            //
            strPath = strPath.TrimEnd('\\');
            strPath = strPath.TrimEnd('/');

            if (visitedPathList.Contains(strPath, StringComparer.OrdinalIgnoreCase))
            {
                throw new Exception("EnumerateTickerFilesHelper() - found a loop!");
            }

            if (level > 1000)
            {
                throw new Exception("EnumerateTickerFilesHelper() - found a loop (level > 1000)!");
            }

            level++;
            visitedPathList.Add(strPath);

            //
            // Ok, enumerate ticker files and read common.ini PREVFILE entries.
            //
            string[] filesList = Directory.GetFiles(strPath, strSymbol + "_*" + strExt, SearchOption.TopDirectoryOnly);

            var commonIniSettings = new CommonIniSettings();
            ParseAndMergeCommonIni(Path.Combine(strPath, "common.ini"), ref commonIniSettings);

            //
            // Walk though files list in current folder and check for PREVFILE in individual .ini files,
            // fill in SymbolCsvFileInfo structures, and generate a sorted list.
            //
            var tmpList = new List<SymbolCsvFileInfo>();

            foreach (string filename in filesList)
            {
                var entry = new SymbolCsvFileInfo();
                entry.fullFileName = filename;

                //
                // Read and merge common.ini settings with individual .ini file settings.
                //
                string strCommonIniFile = Path.Combine(Path.GetDirectoryName(filename), "common.ini");
                string strIndividualIniFile = Path.Combine(Path.GetDirectoryName(filename),
                                                           Path.GetFileNameWithoutExtension(filename) + ".ini");

                ParseAndMergeCommonIni(strCommonIniFile, ref entry.commonIniSettings);

                if (File.Exists(strIndividualIniFile))
                {
                    ParseAndMergeCommonIni(strIndividualIniFile, ref entry.commonIniSettings);
                }

                using (var parser = new UltraFastSingleCsvParser(filename, entry.commonIniSettings))
                {
                    entry.locStartDT = parser.StartDt;
                    entry.locEndDT = parser.EndDt;

                    entry.utcStartDT = entry.locStartDT.ToGmt(entry.commonIniSettings.TIMEZONE);
                    entry.utcEndDT = entry.locEndDT.ToGmt(entry.commonIniSettings.TIMEZONE);
                }

                tmpList.Add(entry);
            }

            tmpList = tmpList.OrderByDescending(x => x.utcStartDT).ToList();

            //
            // Remove all entries that
            //
            var res = new List<SymbolCsvFileInfo>();
            string prevFileReference = commonIniSettings.PREVFILE;

            foreach (SymbolCsvFileInfo entry in tmpList)
            {
                if (entry.utcStartDT > cutDatesHigherThan)
                {
                    continue;
                }

                //
                // Fix endDate for this .csv file entry if last trade date in .csv file if higher than cutDatesHigherThan.
                // (i.e. .csv file partially overlaps with oldest file data in another folder.
                //
                if (entry.utcEndDT != DateTime.MaxValue && entry.utcEndDT > cutDatesHigherThan)
                {
                    entry.utcEndDT = cutDatesHigherThan;
                    entry.locEndDT = DateTimeTzExtension.FromGmt(cutDatesHigherThan, entry.commonIniSettings.TIMEZONE);
                }

                if (entry.utcStartDT != DateTime.MinValue)
                {
                    cutDatesHigherThan = entry.utcStartDT;
                }

                //
                // Sanity check.
                //
                if (entry.locEndDT < entry.locStartDT)
                {
                    throw new Exception("Error: endDT < startDT for file: " + entry.fullFileName);
                }

                if (entry.utcEndDT < entry.utcStartDT)
                {
                    throw new Exception("Error: utcEndDT < utcStartDT for file: " + entry.fullFileName);
                }

                //
                // Only add non-zero length .csv files to results array (since we can not determine start/end date
                // for files that do not contain at least one line).
                //
                if (entry.locStartDT != DateTime.MinValue || entry.locEndDT != DateTime.MaxValue)
                {
                    res.Add(entry);
                }

                //
                // Handle PREVFILE reference from individual .ini file.
                //
                string strIndividualIniFile = Path.Combine(Path.GetDirectoryName(entry.fullFileName),
                                                           Path.GetFileNameWithoutExtension(entry.fullFileName) + ".ini");

                if (File.Exists(strIndividualIniFile))
                {
                    var individualIniSettings = new CommonIniSettings();
                    ParseAndMergeCommonIni(strIndividualIniFile, ref individualIniSettings);

                    if (!String.IsNullOrEmpty(individualIniSettings.PREVFILE))
                    {
                        //
                        // Save found PREVFILE reference and stop processing here, since all files older than
                        // cutDatesHigherThan should come from another directory.
                        //
                        prevFileReference = individualIniSettings.PREVFILE;
                        break;
                    }
                }
            }

            //
            // Process PREVFILE references recursively.
            //
            if (!string.IsNullOrEmpty(prevFileReference))
            {
                if (File.Exists(prevFileReference))
                {
                    //
                    // For PREVFILE reference on individual files symbol name and extension can differ.
                    // So extract them here from symbol file name.
                    //
                    string tmpPath;
                    ParseSymbolFileName(prevFileReference, out tmpPath, out strSymbol, out strExt);

                    //
                    // If we have a reference to file, remove filename and leave only directory part.
                    //
                    prevFileReference = Path.GetDirectoryName(prevFileReference);
                }

                if (Directory.Exists(prevFileReference))
                {
                    res.AddRange(EnumerateTickerFilesHelper(prevFileReference, strSymbol, strExt,
                                                            ref cutDatesHigherThan, ref level, ref visitedPathList));
                }
                else
                {
                    throw new Exception("Error: invalid PREVFILE reference, dir or file does not exist: " + prevFileReference);
                }
            }

            return res.ToArray();
        }

        //
        // Enumerates all ticker files for given strPath, strSymbol, strExt.
        //
        // Sorts ticker files names in historical order.
        // Returns tickerFileNames array. tickerFileNames[0] contains the newest file.
        //
        public static SymbolCsvFileInfo[] EnumerateTickerFiles(string strPath, string strSymbol, string strExt)
        {
            int level = 0;
            var visitedPathList = new List<string>();
            DateTime cutDatesHigherThan = DateTime.MaxValue;
            strPath = Path.GetFullPath(strPath);

            SymbolCsvFileInfo[] filesList = EnumerateTickerFilesHelper(strPath, strSymbol, strExt,
                                                                       ref cutDatesHigherThan, ref level,
                                                                       ref visitedPathList);
            return filesList.OrderByDescending(x => x.utcStartDT).ToArray();
        }

        //
        // Структура, которая описывает .csv файл с данными. Местоположение, стартовый и конечный интервал дат,
        // которые берутся из этого файла, смердженные настройки из common.ini и индивидуального .ini файла.
        //
        public class SymbolCsvFileInfo
        {
            public CommonIniSettings commonIniSettings;
                                     // Настройки из common.ini смердженного с индивидуальным .ini файлом.

            public string fullFileName; // Абсолютный путь и имя .csv файла.

            public DateTime locStartDT;
            public DateTime locEndDT;

            public DateTime utcStartDT;
            public DateTime utcEndDT;
        }

    }
}
