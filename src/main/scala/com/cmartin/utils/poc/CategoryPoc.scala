package com.cmartin.utils.poc

import zio.stream.ZStream
import zio._

/** Running steps:
  *   - git clone https://github.com/butcherless/dependency-analyzer.git
  *   - cd dependency-analyzer
  *   - sdk use java 17... / 20...
  *   - sdk install sbt 1.9.3
  *   - sbt "runMain com.cmartin.utils.poc.CategoryPoc"
  */
object CategoryPoc
    extends ZIOAppDefault {

  // aliases to aid type researching
  type RepoError = String
  type Group     = String
  type Product   = String

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
    def findAllKeyValues(key: String): ZStream[Any, RepoError, Product]
  }

  case class InMemoryCategoryRepository() extends CategoryRepository {

    override def findAllKeys(): ZStream[Any, RepoError, Group] =
      ZStream.fromIterable(Data.categories.keys)

    override def findAllKeyValues(key: String): ZStream[Any, RepoError, Product] =
      generateFamily(key)

    /* Data generator
      ZStream.fromIterable(
        Data.categories.get(key)
          .fold(List.empty[Product])(identity)
      )
     */

  }

  def generateRandomInt(): Int =
    scala.util.Random.between(5, 100 + 1)

  def generateFamily(name: String): ZStream[Any, RepoError, Product] =
    ZStream.iterate(1)(_ + 1).takeWhile(_ <= 100)
      .schedule(Schedule.spaced(generateRandomInt().milliseconds))
      .map(a => s"$name-$a")
      // .foreach(a => Console.printLine(s"element: $a"))
      .mapError(e => e.toString())

  /** Retrieves all Products from all categories.
    *   - retrieves all categories
    *   - retrieves all Products from each category
    *   - maps to (group, product) tuple
    *   - groups by group (key)
    *   - resolve product stream to list
    *   - maps to (group, product-list)
    *   - resolve (group, product-list) stream to map
    *
    * @param categoryRepository
    *   dummy category repository
    */
  case class ZioFindCategoriesUseCase(categoryRepository: CategoryRepository) {
    private def streamToList(products: ZStream[Any, RepoError, Product]): ZIO[Any, RepoError, List[Product]] =
      products.runFold(List.empty[Product])((list, product) => list :+ product)

    def execute(): IO[RepoError, Map[Group, List[Product]]] =
      categoryRepository.findAllKeys()
        .flatMapPar(3)(groupKey =>
          categoryRepository.findAllKeyValues(groupKey)
            .map(product => (groupKey, product))
            .tap(a => ZIO.log(s"elem: $a"))
        // .tap(a => Console.printLine(s"elem: $a").orDie)
        ).groupBy { case (group, product) =>
          ZIO.succeed(group, product)
        } { case (group, products) =>
          ZStream.fromZIO(streamToList(products))
            .map(products => (group, products))
        }.runFold(Map.empty[Group, List[Product]]) { case (map, (group, product)) =>
          map + (group -> product)
        }

  }

  val repo    = InMemoryCategoryRepository()
  val useCase = ZioFindCategoriesUseCase(repo)

  override def run =
    for {
      _   <- ZIO.log("hello categories")
      map <- useCase.execute()
      _   <- ZIO.log(s"category map: ${map.mkString("\n\n", "\n", "\n\n")}")
      // _   <- generateFamily("number")
    } yield ()

}

/* Streams analysis
   - cartesian prouct: category x product
   - parallelism

(ca, pa1) + (ca, pa2) + (ca, pa3)  t = 4s
(cb, pb1) + (cb, pb2)              t = 1s
(cc, pc1) + (cc, pc1) + (cc, pc3)  t = 2s

--------
--
----

(ca, pa1) + (cc, pc1) + (cb, pb1) + (cb, pb2) ...... cartesian prouct, parallelism
groupByKey( function )
toMap

 */
