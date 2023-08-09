package com.cmartin.utils.poc

import zio.stream.ZStream
import zio._

object CategoryPoc
    extends ZIOAppDefault {

  // aliases to aid type researching
  type RepoError = String
  type Group     = String
  type Item      = String

  // repository data simulation
  object Data {
    val family       = List("Abrigo", "Camisa", "Bermuda", "Chaleco")
    val basicFantasy = List("Basic", "Fantasia")
    val planes       = List("A320neo", "A330neo", "A350-900", "B787-10X", "B787-9")

    val categories = Map(
      "planes"       -> planes,
      "basicFantasy" -> basicFantasy,
      "family"       -> family
    )
  }

  trait CategoryRepository {
    def findAllKeys(): ZStream[Any, RepoError, Group]
    def findAllKeyValues(key: String): ZStream[Any, RepoError, Item]
  }

  case class InMemoryCategoryRepository() extends CategoryRepository {

    override def findAllKeys(): ZStream[Any, RepoError, Group] =
      ZStream.fromIterable(Data.categories.keys)

    override def findAllKeyValues(key: String): ZStream[Any, RepoError, Item] =
      ZStream.fromIterable(
        Data.categories.get(key)
          .fold(List.empty[Item])(identity)
      )

  }

  /** Retrieves all items from all categories.
    *   - retrieves all categories
    *   - retrieves all items from each category
    *   - maps to (group, item) tuple
    *   - groups by group (key)
    *   - resolve item Stream to List
    *   - maps to (group, item-list)
    *   - resolve (group, item-list) Stream to Map
    * @param categoryRepository
    *   dummy category repository
    */
  case class ZioFindCategoriesUseCase(categoryRepository: CategoryRepository) {
    def execute(): IO[RepoError, Map[Group, List[Item]]] =
      categoryRepository.findAllKeys()
        .flatMap(group =>
          categoryRepository.findAllKeyValues(group)
            .map(item => (group, item))
        ).groupBy { case (group, item) => ZIO.succeed(group, item) } { case (group, items) =>
          ZStream.fromZIO(items.runFold(List.empty[Item])((list, item) => list :+ item))
            .map(items => (group, items))
        }.runFold(Map.empty[Group, List[Item]]) { case (map, (group, item)) => map + (group -> item) }

  }

  val repo    = InMemoryCategoryRepository()
  val useCase = ZioFindCategoriesUseCase(repo)

  override def run =
    for {
      _   <- ZIO.log("hello categories")
      map <- useCase.execute()
      _   <- ZIO.log(s"category map: ${map.mkString("\n\n", "\n", "\n\n")}")
    } yield ()

}
