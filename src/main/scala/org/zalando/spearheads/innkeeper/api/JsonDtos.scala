package org.zalando.spearheads.innkeeper.api

import java.time.LocalDateTime

import org.zalando.spearheads.innkeeper.api.Endpoint.{ EndpointType, Https, Protocol, ReverseProxy }

import scala.collection.immutable.Seq

/**
 * @author dpersa
 */
case class Route(id: Long, route: NewRoute, createdAt: LocalDateTime,
                 deletedAt: Option[LocalDateTime] = None)

case class Endpoint(hostname: String, path: Option[String] = None,
                    port: Option[Int] = Some(443),
                    protocol: Option[Protocol] = Some(Https),
                    endpointType: Option[EndpointType] = Some(ReverseProxy))



case class Header(name: String, value: String)

case class NewRoute(description: String,
                    pathMatcher: PathMatcher,
                    endpoint: Endpoint,
                    headerMatchers: Option[Seq[Header]] = Some(Seq.empty),
                    methodMatchers: Option[Seq[String]] = Some(Seq("GET")),
                    requestHeaders: Option[Seq[Header]] = Some(Seq.empty),
                    responseHeaders: Option[Seq[Header]] = Some(Seq.empty),
                    pathRewrite: Option[PathRewrite] = None)

case class PathMatcher(matcher: String, matcherType: MatcherType)





case class PathRewrite(matcher: String, replace: String)