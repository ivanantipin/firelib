package firelib.common

import java.time.Instant

import scala.collection.mutable.ArrayBuffer


sealed class Interval private (val Name: String, val durationMs: Int) {

    Interval.intervals += this

    def roundTime(dt:Instant): Instant  = Instant.ofEpochMilli(  roundEpochMs((dt.toEpochMilli)))

    def roundEpochMs(epochMs : Long): Long  =  (epochMs/ durationMs) * durationMs

    override def toString: String = Name
}

object Interval {

    val intervals = new ArrayBuffer[Interval]()

    val Sec1 = new Interval("Sec1", 1000);
    val Sec30 = new Interval("Sec30", 30 * 1000);
    val Min5 = new Interval("Min5", 5 * 60 * 1000);
    val Min15 = new Interval("Min15", 15 * 60 * 1000);
    val Min30 = new Interval("Min30", 30 * 60 * 1000);
    val Min60 = new Interval("Min60", 60 * 60 * 1000);
    val Min240 = new Interval("Min240", 240 * 60 * 1000);
    val Day = new Interval("Min60", 1440 * 60 * 1000);

    def resolveFromMs(ms: Long) = intervals.find(_.durationMs == ms).get

    def resolveFromName(name: String) = intervals.find(_.Name == name).get

}


