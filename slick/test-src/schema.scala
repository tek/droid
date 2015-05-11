package slick.test

import scala.slick.driver.SQLiteDriver.simple._
import scala.concurrent.Await
import scala.concurrent.duration._

import com.github.nscala_time.time.Imports.DateTime

import argonaut._
import Argonaut._

import scalaz._, Scalaz._

import slick._
import slick.db._

import org.specs2._
import org.specs2.specification._

abstract class ExtSchemaTest
extends SlickTest
{
  import ExtTestSchema._

  @DBSession def resetDb() {
    metadata.dropAll()
    pendingMetadata.dropAll()
    metadata.createMissingTables()
    pendingMetadata.createMissingTables()
  }

  def createModels = {
    db withSession { implicit s ⇒
      for {
        a ← Alpha.insert(Alpha("something", Flag.On, 4.0))
        a2 ← Alpha.insert(Alpha("something else", Flag.On, 4.0))
        b ← Beta.insert(Beta("yello", None, a.id, a2.id))
        b2 ← Beta.insert(Beta("chello", Some(DateTime.now), a.id, a2.id))
        c ← Gamma.insert(Gamma("hello"))
      } yield (a, b, b.id, c)
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
      c.bet must contain(b)
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
    additions("alphas") === List(Addition(1, 1), Addition(2, 2)) &&
      additions("betas") === List(Addition(1, 3)) &&
      additions("gammas") === List(Addition(1, 4))
  }
}

class DummyHttpClient(responses: Seq[String])
extends HttpClient
{
  val it = Iterator(responses: _*)

  def response = if (it.hasNext) it.next.right else "No response left".left

  override def post(path: String, body: String = "{}") = response
  override def put(path: String, body: String = "{}") = response
  override def delete(path: String, body: String = "{}") = response
  override def get(path: String, body: String = "{}") = response
}

class CompletePendingTest
extends ExtSchemaTest
{
  def is = s2"""
  After completing the first alpha addition, there should be

  one alpha addition left $alpha
  both beta additions left $beta
  """

  import ExtTestSchema._
  import PendingActionsSchema.Addition

  def beforeAll() {
    resetDb()
    val (a, b, bId, c) = models
    db withSession { implicit s ⇒
      Alpha.completeSync(additions("alphas").head)
    }
  }

  def alpha = {
    additions("alphas") === List(Addition(2, 2))
  }

  def beta = {
    additions("betas") === List(Addition(1, 3), Addition(2, 4))
  }
}

class AdvancedExtSchemaTest
extends ExtSchemaTest
{
  def is = s2"""
  Sync of pending actions to a backend

  apply uuids to multiple records in a table $alphaUuids
  apply names to multiple records in a table $alphaNames
  apply multiple changed foreign keys to a record $beta
  apply multiple changed associations to a record $gamma
  apply changed fields to a record $alphaValues
  """

  import ExtTestSchema._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit class `json shortcut`[A: EncodeJson](a: A) {
    def js = a.asJson.spaces2
  }

  def beforeAll() {
    resetDb()
    val (a, b, bId, c) = models
    db withSession { implicit s ⇒
      val a1 = AlphaMapper("response_alpha_1", Flag.Off, 5.0,
        Some("uuid_alpha_1"))
      val a2 = AlphaMapper("response_alpha_2", Flag.Off, 5.0,
        Some("uuid_alpha_2"))
      val b1 = BetaMapper("response_beta_1", None, Some("uuid_beta_1"),
        "uuid_alpha_2", "uuid_alpha_1")
      val b2 = BetaMapper("response_beta_2", None, Some("uuid_beta_2"),
        "uuid_alpha_2", "uuid_alpha_2")
      val c = GammaMapper("response_gamma_1", Some("uuid_gamma_1"),
        List("uuid_beta_1", "uuid_beta_2"), List())
      val responses =
        a1.js ::
        a2.js ::
        Seq(a1, a2).js ::
        b1.js ::
        b2.js ::
        Seq(b1, b2).js ::
        c.js ::
        Seq(c).js ::
        Nil
      val backend = new BackendSync {
        val http = new DummyHttpClient(responses)
      }
      val fut = backend(ExtTestSchema)
      fut onFailure {
        case e ⇒ throw e
      }
      Await.ready(fut, 3 seconds)
    }
  }

  def alphaUuids = {
    db withSession { implicit s ⇒
      val uuids = Alpha.list.map(_.uuid).flatten
      uuids must_== List("uuid_alpha_1", "uuid_alpha_2")
    }
  }

  def alphaNames = {
    db withSession { implicit s ⇒
      val uuids = Alpha.list.map(_.name)
      uuids must_== List("response_alpha_1", "response_alpha_2")
    }
  }

  def beta = {
    db withSession { implicit s ⇒
      val ids = Beta.list.headOption map { b ⇒ (b.alpId, b.alp2Id) }
      ids must beSome((2L, 1L))
    }
  }

  def gamma = {
    db withSession { implicit s ⇒
      val betas = Gamma.list.headOption map {
        c ⇒ (c.loadBets.list, c.loadBet2s.list) } map {
        case (b1, b2) ⇒ (b1.ids, b2.ids) }
      betas must beSome((Set(1, 2), Set()))
    }
  }

  def alphaValues = {
    db withSession { implicit s ⇒
      Alpha.firstOption.map(_.flog) must beSome(Flag.Off)
    }
  }
}
