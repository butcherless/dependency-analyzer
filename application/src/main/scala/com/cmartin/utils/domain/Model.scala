package com.cmartin.utils.domain

import just.semver.SemVer
import zio.json.{DeriveJsonDecoder, JsonDecoder}

import scala.util.matching.Regex

object Model {
  /*
    CONSTANT MESSAGES
   */

  val OPEN_FILE_ERROR   = "Error while opening the file"
  val FILE_BUFFER_ERROR = "Error while creating the file buffer"

  sealed trait DomainError
      extends Product with Serializable

  object DomainError {

    final case class FileIOError(message: String) extends DomainError

    final case class ConfigError(message: String)    extends DomainError
    final case class NetworkError(message: String)   extends DomainError
    final case class WebClientError(message: String) extends DomainError
    final case class ResponseError(message: String)  extends DomainError
    final case class DecodeError(message: String)    extends DomainError

    final case class UnknownError(m: String) extends DomainError
  }

  /*
   Version comparator
   */
  sealed trait ComparatorResult
      extends Product with Serializable

  object ComparatorResult {

    case object Older extends ComparatorResult

    case object Same extends ComparatorResult

    case object Newer extends ComparatorResult
  }

  final case class GavPair(local: Gav, remote: Gav) {
    def hasNewVersion: Boolean =
      local.version != remote.version
  }

  /** It represents a maven dependency
    *
    * @param group
    *   dependency group
    * @param artifact
    *   dependency artifact
    * @param version
    *   dependency version
    */
  final case class Gav(group: String, artifact: String, version: String) {
    def key: String = s"$group:$artifact"

    def formatShort: String = s"$group:$artifact:$version"
  }

  /** Companion Object for Gav case class
    */
  object Gav {
    implicit val decoder: JsonDecoder[Gav] = DeriveJsonDecoder.gen[Gav]

    /*implicit*/
    val ordOld: Ordering[Gav] = (d1: Gav, d2: Gav) =>
      d1.version.compareTo(d2.version)

    /* validated versions, safe run
       reverse order, descending order, greatest first
       inverse compare result: -1 => 1, 1 => -1
     */
    implicit val ord: Ordering[Gav] = (d1: Gav, d2: Gav) => {
      val comparisonEither = for {
        v1 <- SemVer.parse(d1.version)
        v2 <- SemVer.parse(d2.version)
      } yield -1 * v1.compare(v2)

      // d1.version.compareTo(d2.version)
      comparisonEither.getOrElse(0)
    }

    def fromRegexMatch(regexMatch: Regex.Match): Gav =
      Gav(
        regexMatch.group(1), // group
        regexMatch.group(2), // artifact
        regexMatch.group(3)  // version
      )
  }

  final case class MavenSearchResult(
      responseHeader: ResponseHeader,
      response: MavenResponse
  )

  object MavenSearchResult {
    implicit val decoder: JsonDecoder[MavenSearchResult] = DeriveJsonDecoder.gen[MavenSearchResult]

  }

  final case class ResponseHeader(
      status: Int,
      params: Params
  )

  object ResponseHeader {
    implicit val decoder: JsonDecoder[ResponseHeader] = DeriveJsonDecoder.gen[ResponseHeader]
  }

  /** Represents the parameters used for a query.
    *
    * @param q
    *   The query string.
    * @param core
    *   The core name to query.
    * @param fl
    *   The comma-separated list of fields to retrieve.
    * @param sort
    *   The field to sort the results by.
    * @param rows
    *   The maximum number of rows to retrieve.
    * @param wt
    *   The response format. Can be "json", "xml", etc.
    * @param version
    *   The version of the query parameters.
    */

  final case class Params(
      q: String,
      core: String,
      fl: String,
      sort: String,
      rows: Int,
      wt: String,
      version: String
  )

  object Params {
    implicit val decoder: JsonDecoder[Params] = DeriveJsonDecoder.gen[Params]
  }

  final case class MavenResponse(
      numFound: Int,
      start: Int,
      docs: Seq[Artifact]
  )

  object MavenResponse {
    implicit val decoder: JsonDecoder[MavenResponse] = DeriveJsonDecoder.gen[MavenResponse]
  }

  final case class Artifact(
      id: String,
      g: String,
      a: String,
      latestVersion: String,
      p: String,
      timestamp: Long,
      ec: Seq[String]
  )

  object Artifact {
    implicit val decoder: JsonDecoder[Artifact] = DeriveJsonDecoder.gen[Artifact]
  }

}
