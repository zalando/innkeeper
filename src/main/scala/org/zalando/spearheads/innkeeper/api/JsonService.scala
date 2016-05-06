package org.zalando.spearheads.innkeeper.api

import akka.NotUsed
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.stream.scaladsl.Source
import com.google.inject.Singleton
import spray.json.{JsonWriter, pimpAny}

/**
 * @author dpersa
 */
@Singleton
class JsonService {

  def sourceToJsonSource[T](source: Source[T, NotUsed])(implicit writer: JsonWriter[T]): Source[ChunkStreamPart, NotUsed] = {

    val commaSeparatedObjects: Source[ChunkStreamPart, NotUsed] =
      source
        .map(t => Some(t.toJson.compactPrint))
        .scan[Option[ChunkStreamPart]](None)({
          case (None, Some(sourceElement)) => Some(ChunkStreamPart(sourceElement))
          case (_, Some(sourceElement))    => Some(ChunkStreamPart(s", $sourceElement"))
        })
        .mapConcat(_.toList)

    Source.single(ChunkStreamPart("["))
      .concat(commaSeparatedObjects)
      .concat(Source.single(ChunkStreamPart("]")))
  }
}
