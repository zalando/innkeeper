package org.zalando.spearheads.innkeeper.metrics

import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap
import scala.util.{ Try, Success, Failure }

import com.codahale.metrics._

import spray.json._

/**
 * Credits to: https://gist.github.com/rschreijer/75a35fd36a9b63d33b63
 */
object MetricRegistryJsonProtocol extends MetricRegistryFormat

trait MetricRegistryFormat extends AnyFormat {
  private val rateUnit: TimeUnit = TimeUnit.SECONDS
  private val rateFactor: Long = rateUnit.toSeconds(1);
  private val rateUnitStr: String = s"events/${rateUnit.name.toLowerCase.dropRight(1)}"
  private val durationUnit: TimeUnit = TimeUnit.MILLISECONDS
  private val durationFactor: Double = 1d / durationUnit.toNanos(1);
  private val durationUnitStr: String = durationUnit.name.toLowerCase
  private val showSamples: Boolean = true

  implicit object GaugeJsonFormat extends RootJsonFormat[Gauge[_]] {

    def write(gauge: Gauge[_]) = Try(gauge.getValue) match {
      case Success(value) => JsObject(Map("value" -> AnyJsonFormat.write(value)))
      case Failure(err)   => JsObject(Map("error" -> JsString(err.toString)))
    }

    def read(value: JsValue): Gauge[_] = throw new DeserializationException("Cannot deserialize Gauge")

  }

  implicit object CounterJsonFormat extends RootJsonFormat[Counter] {

    def write(counter: Counter) = JsObject(Map("value" -> JsNumber(counter.getCount)))

    def read(value: JsValue): Counter = throw new DeserializationException("Cannot deserialize Counter")

  }

  implicit object HistogramJsonFormat extends RootJsonFormat[Histogram] {

    def write(histogram: Histogram) = {
      val snapshot: Snapshot = histogram.getSnapshot
      JsObject(
        SortedMap(
          "count" -> JsNumber(histogram.getCount),
          "max" -> JsNumber(snapshot.getMax),
          "mean" -> JsNumber(snapshot.getMean),
          "min" -> JsNumber(snapshot.getMin),
          "p50" -> JsNumber(snapshot.getMedian),
          "p75" -> JsNumber(snapshot.get75thPercentile),
          "p95" -> JsNumber(snapshot.get95thPercentile),
          "p98" -> JsNumber(snapshot.get98thPercentile),
          "p99" -> JsNumber(snapshot.get99thPercentile),
          "p999" -> JsNumber(snapshot.get999thPercentile),
          "stddev" -> JsNumber(snapshot.getStdDev)
        )
      )
    }

    def read(value: JsValue): Histogram = throw new DeserializationException("Cannot deserialize Histogram")

  }

  implicit object MeterJsonFormat extends RootJsonFormat[Meter] {

    def write(meter: Meter) = JsObject(
      SortedMap(
        "count" -> JsNumber(meter.getCount),
        "m15_rate" -> JsNumber(meter.getFifteenMinuteRate * rateFactor),
        "m1_rate" -> JsNumber(meter.getOneMinuteRate * rateFactor),
        "m5_rate" -> JsNumber(meter.getFiveMinuteRate * rateFactor),
        "mean_rate" -> JsNumber(meter.getMeanRate * rateFactor),
        "units" -> JsString(rateUnitStr)
      )
    )

    def read(value: JsValue): Meter = throw new DeserializationException("Cannot deserialize Meter")

  }

  implicit object TimerJsonFormat extends RootJsonFormat[Timer] {

    def write(timer: Timer) = {
      val snapshot: Snapshot = timer.getSnapshot
      JsObject(
        SortedMap(
          "count" -> JsNumber(timer.getCount),
          "max" -> JsNumber(snapshot.getMax * durationFactor),
          "mean" -> JsNumber(snapshot.getMean * durationFactor),
          "min" -> JsNumber(snapshot.getMin * durationFactor),
          "p50" -> JsNumber(snapshot.getMedian * durationFactor),
          "p75" -> JsNumber(snapshot.get75thPercentile * durationFactor),
          "p95" -> JsNumber(snapshot.get95thPercentile * durationFactor),
          "p98" -> JsNumber(snapshot.get98thPercentile * durationFactor),
          "p99" -> JsNumber(snapshot.get99thPercentile * durationFactor),
          "p999" -> JsNumber(snapshot.get999thPercentile * durationFactor),
          "duration_units" -> JsString(durationUnitStr),
          "stddev" -> JsNumber(snapshot.getStdDev),
          "m15_rate" -> JsNumber(timer.getFifteenMinuteRate * rateFactor),
          "m1_rate" -> JsNumber(timer.getOneMinuteRate * rateFactor),
          "m5_rate" -> JsNumber(timer.getFiveMinuteRate * rateFactor),
          "mean_rate" -> JsNumber(timer.getMeanRate * rateFactor),
          "rate_units" -> JsString(rateUnitStr)
        )
      )
    }

    def read(value: JsValue): Timer = throw new DeserializationException("Cannot deserialize Timer")

  }

  implicit object MetricRegistryJsonFormat extends RootJsonFormat[MetricRegistry] {

    def write(metricRegistry: MetricRegistry) = JsObject(
      SortedMap[String, JsValue](
        "gauges" -> JsObject(metricRegistry.getGauges.asScala.toMap.map {
          case (name, gauge) => name -> GaugeJsonFormat.write(gauge)
        }),
        "counters" -> JsObject(metricRegistry.getCounters.asScala.toMap.map {
          case (name, counter) => name -> CounterJsonFormat.write(counter)
        }),
        "histograms" -> JsObject(metricRegistry.getHistograms.asScala.toMap.map {
          case (name, histogram) => name -> HistogramJsonFormat.write(histogram)
        }),
        "meters" -> JsObject(metricRegistry.getMeters.asScala.toMap.map {
          case (name, meter) => name -> MeterJsonFormat.write(meter)
        }),
        "timers" -> JsObject(metricRegistry.getTimers.asScala.toMap.map {
          case (name, timer) => name -> TimerJsonFormat.write(timer)
        })
      )
    )

    def read(value: JsValue): MetricRegistry = throw new DeserializationException("Cannot deserialize MetricRegistry")
  }

}

trait AnyFormat {

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(in: Any) = {
      if (in.isInstanceOf[java.util.Collection[_]]) writeCollection(in.asInstanceOf[java.util.Collection[_]])
      else if (in.isInstanceOf[java.util.Map[_, _]]) writeMap(in.asInstanceOf[java.util.Map[_, _]])
      else in match {
        case n: Int     => JsNumber(n)
        case n: Long    => JsNumber(n)
        case f: Float   => JsNumber(f)
        case f: Double  => JsNumber(f)
        case s: String  => JsString(s)
        case b: Boolean => if (b) JsTrue else JsFalse
      }
    }

    private def writeCollection(in: java.util.Collection[_]): JsValue = {
      JsArray(in.asScala.toVector.map(write(_)))
    }

    private def writeMap(in: java.util.Map[_, _]): JsValue = {
      JsObject(in.asScala.toMap.map { case (k, v) => k.toString -> write(v) })
    }

    def read(value: JsValue) = throw new RuntimeException("JSON read as Any? Weird!")
  }

}
