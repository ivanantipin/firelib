package firelib.utils

import java.nio.file.{Files, Paths, StandardOpenOption}

import scala.collection.JavaConversions._

object StatFileDumper
    {
        def AppendRow(ff : String, row : String)
        {
            Files.write(Paths.get(ff), List(row).toStream, StandardOpenOption.APPEND);
        }

        def appendRows(ff : String, rows : Seq[String])
        {
            Files.write(Paths.get(ff), rows.toStream, StandardOpenOption.APPEND);
        }

        def writeRows(ff : String, rows : Seq[String])
        {
            Files.write(Paths.get(ff), rows.toStream, StandardOpenOption.WRITE);
        }


/*
        public static void DumpSeries(string fileName, List<FactorsPoint> series, List<string> header = null)
        {
            using (var fs = GetStandardStreamWriter(fileName))
            {
                if (header != null)
                {
                    fs.WriteLine(string.Join(",", header.ToArray()));
                }

                int cnt = series.Count;
                for (int i = 0; i < cnt; i++)
                {
                    series[i].Write(fs);
                }
                fs.Flush();
            }
        }
*/

    }
