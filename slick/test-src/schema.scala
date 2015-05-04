package slick.test

import scala.slick.driver.SQLiteDriver.simple._
import scala.concurrent.Await
import scala.concurrent.duration._

import slick._
import slick.db._

import org.specs2._
import org.specs2.specification._

@Schema()
object SimpleTestSchema
{
  case class Alpha(name: String)
  case class Beta(name: String, alp: Alpha)
  case class Gamma(name: String, bet: List[Beta])
}

@SyncSchema()
object ExtTestSchema
extends schema.Timestamps
with schema.Uuids
with schema.Sync
{
  case class Alpha(name: String)
  case class Beta(name: String, alp: Alpha, alp2: Alpha)
  case class Gamma(name: String, bet: List[Beta], bet2: List[Beta])
}

class SimpleSchemaTest
extends Specification
{
  def is = s2"""
  The simple schema should

  work $e1
  """

  def e1 = {
    1 must_== 1
  }
}

abstract class ExtSchemaTest
extends Specification
{
  import ExtTestSchema._

  implicit val dbInfo = slick.db.DBConnectionInfo(
    url = s"jdbc:sqlite:slick/target/slick_test.db",
    driverClassName = "org.sqlite.JDBC"
  )

  def db = Database.forURL(dbInfo.url, null, null, null,
    dbInfo.driverClassName)

  @DBSession def resetDb() {
    metadata.dropAll()
    pendingMetadata.dropAll()
    metadata.createMissingTables()
    pendingMetadata.createMissingTables()
  }

  def createModels = {
    db withSession { implicit s ⇒
      for {
        a ← alphas.insert(Alpha(None, "something"))
        aId ← a.id
        a2 ← alphas.insert(Alpha(None, "something else"))
        a2Id ← a2.id
        b ← betas.insert(Beta(None, "yello", aId, a2Id))
        bId ← b.id
        c ← gammas.insert(Gamma(None, "hello"))
      } yield (a, b, bId, c)
    }
  }

  lazy val models = createModels.get

  def additions(model: String) = {
    db withSession { implicit s ⇒
      val adds = for {
        a ← pendingActions if a.model === model
        add ← a.additions
      } yield add
      adds.list
    }
  }
}

class BasicExtSchemaTest
extends ExtSchemaTest
with BeforeAll
{
  def is = s2"""
  The extended schema should

  create models $checkModels
  add list associations $listAssoc
  add flat associations $flatAssoc
  create pending actions $pendingAct
  """

  import ExtTestSchema._

  def beforeAll() {
    resetDb()
  }

  def checkModels() = Try(models) must beSuccessfulTry

  def listAssoc = {
    db withSession { implicit s ⇒
    val (a, b, bId, c) = models
      c.addBet(bId)
      c.addBet2(bId)
      c.loadBets.list must contain(b)
    }
  }

  def flatAssoc = {
    db withSession { implicit s ⇒
      val (a, b, bId, c) = models
      b.loadAlp must_!= b.loadAlp2
    }
  }

  def pendingAct = {
    import PendingActionsSchema.Addition
    val (a, b, bId, c) = models
    additions("alphas") === List(Addition(Some(1), 1), Addition(Some(2), 2)) &&
      additions("betas") === List(Addition(Some(3), 1)) &&
      additions("gammas") === List(Addition(Some(4), 1))
  }
}

class DummyHttpClient
extends HttpClient
{
  override def post(path: String, body: String = "{}") = "{}"
  override def put(path: String, body: String = "{}") = "{}"
  override def delete(path: String, body: String = "{}") = "{}"
}

class AdvancedExtSchemaTest
extends ExtSchemaTest
with BeforeEach
{
  def is = s2"""
  The extended schema should also

  resolve pending actions $resolve
  sync pending actions to a backend $sync
  """

  import ExtTestSchema._

  def before() {
    resetDb()
  }

  def resolve = {
    import PendingActionsSchema.Addition
    val (a, b, bId, c) = models
    db withSession { implicit s ⇒
      a.completeSync()
    }
    additions("alphas") === List(Addition(Some(2), 2)) &&
      additions("betas") === List(Addition(Some(3), 1))
  }

  def sync = {
    val (a, b, bId, c) = models
    val backend = new BackendSync {
      def http = new DummyHttpClient
    }
    val fut = db withSession { implicit s ⇒
      backend(ExtTestSchema)
    }
    Await.ready(fut, 3 seconds)
    1 must_== 1
  }
}
