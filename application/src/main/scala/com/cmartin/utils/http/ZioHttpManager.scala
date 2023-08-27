package com.cmartin.utils.http

import com.cmartin.utils.domain.HttpManager
import com.cmartin.utils.domain.HttpManager.GavResults
import com.cmartin.utils.domain.HttpManager.retrieveFirstMajor
import com.cmartin.utils.domain.Model.DomainError.{NetworkError, WebClientError}
import com.cmartin.utils.domain.Model._
import com.cmartin.utils.http.ZioHttpManager._
import just.semver.SemVer
import just.semver.SemVer.render
import sttp.capabilities.zio.ZioStreams
import sttp.client4._
import sttp.client4.ziojson._
import zio._

final case class ZioHttpManager(client: HttpClient)
    extends HttpManager {

  private def checkResponseCode(response: Response[_]) =
    if (response.code.isSuccess) ZIO.succeed(response.code)
    else ZIO.fail(WebClientError(s"Response error code: ${response.code}"))

  val result: IO[DomainError, Unit] = for {
    response <- basicRequest.get(uri"dummy-uri").response(asJson[MavenSearchResult]).send(client).mapError(th =>
                  WebClientError(th.getMessage)
                )
    _        <- checkResponseCode(response)
    _        <- ZIO.fromEither(response.body).mapError(th => WebClientError(th.getMessage))
  } yield ()

  override def checkDependencies(gavList: Iterable[Gav]): UIO[GavResults] =
    ZIO.partitionPar(gavList)(getDependency).withParallelism(4)
      .map(GavResults.tupled)

  private def getDependency(gav: Gav): IO[DomainError, GavPair] =
    for {
      response      <- makeRequest(gav).send(client)
                         .mapError(e => NetworkError(e.getMessage)) // TODO refactor
      _             <- ZIO.logDebug(s"status code: ${response.code}")
      remoteGavList <- extractDependencies(response.body)
      _             <- logRemoteGavList(gav, remoteGavList)
      remoteGav     <- retrieveFirstMajor(remoteGavList, gav)
    } yield GavPair(gav, remoteGav)

  private def makeRequest(gav: Gav): Request[MavenSearchResult] =
    basicRequest
      .get(uri"${buildUriFromGav(gav)}")
      .response(asJson[MavenSearchResult].getRight)

  // TODO: manage parse errors, just semver parsing
  // validate artifact properties, fail with domain error
  private def extractDependencies(results: MavenSearchResult): UIO[Seq[Gav]] =
    ZIO.succeed(results.response.docs.map(viewToModel))

  private def logRemoteGavList(gav: Gav, gavs: Seq[Gav]): UIO[Unit] =
    if (gavs.nonEmpty)
      ZIO.logDebug(s"remote versions for (g,a)=(${gav.group},${gav.artifact}) -> ${gavs.map(_.version)}")
    else ZIO.logWarning(s"no remote artifacts for: $gav")

}

object ZioHttpManager {

  type HttpClient = WebSocketStreamBackend[Task, ZioStreams]

  private def viewToModel(a: Artifact): Gav = {
    val parsedVersion = SemVer.parse(a.latestVersion).fold(_.toString, render)

    Gav(group = a.g, artifact = a.a, parsedVersion)
  }
  val scheme                                = "https"
  private val host                          = "search.maven.org"
  val path                                  = "solrsearch/select"

  /* curl -s "https://search.maven.org/solrsearch/select?q=g:dev.zio+AND+a:zio_2.13&wt=json" | jq
   */
  private def buildUriFromGav(gav: Gav): String =
    s"$scheme://$host/$path?q=g:${gav.group}+AND+a:${gav.artifact}&wt=json"

  val layer: URLayer[HttpClient, ZioHttpManager] =
    ZLayer.fromFunction(client => ZioHttpManager(client))
}
