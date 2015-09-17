package org.zalando.spearheads.innkeeper.api

import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.stream.scaladsl.Source
import com.google.inject.Singleton
import spray.json._

/**
 * @author dpersa
 */
@Singleton
class JsonService {

  def sourceToJsonSource[T](source: Source[T, Unit])(implicit writer: JsonWriter[T]): Source[ChunkStreamPart, ((Unit, Unit), Unit)] = {

    val commaSeparatedRoutes: Source[ChunkStreamPart, Unit] =
      source
        .map(t => Some(t.toJson.compactPrint))
        .scan[Option[ChunkStreamPart]](None)({
          case (None, Some(sourceElement)) => Some(ChunkStreamPart(sourceElement))
          case (_, Some(sourceElement))    => Some(ChunkStreamPart(s", $sourceElement"))
        })
        .mapConcat(_.toList)

    Source.single(ChunkStreamPart("[")) ++ commaSeparatedRoutes ++ Source.single(ChunkStreamPart("]"))
  }
}
