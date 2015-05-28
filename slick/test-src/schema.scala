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

  val objectIds = scala.collection.mutable.Map[String, ObjectId]()

  def oid(name: String) = objectIds.getOrElseUpdate(name, new ObjectId)

  def createModels = {
    db withSession { implicit s ⇒
      for {
        a ← Alpha.insert(Alpha("alpha1", Flag.On, 4.0, oid("a1")))
        a2 ← Alpha.insert(Alpha("alpha2", Flag.On, 4.0, oid("a2")))
        b ← Beta.insert(Beta("beta1", None, a.id, a2.id, oid("b1")))
        b2 ← Beta.insert(Beta("beta2", Some(DateTime.now), a.id, a2.id,
          oid("b2")))
        c ← Gamma.insert(Gamma("gamma1", oid("c1")))
        c2 ← Gamma.insert(Gamma("gamma2", oid("c2")))
      } yield (a, b, b.id, c)
    }
  }

  lazy val models = createModels.get

  def additionTargets(model: String) = {
    db withSession { implicit s ⇒
      val adds = for {
        a ← pendingActions if a.model === model
        add ← a.additions
      } yield add.target
      adds.list
    }
  }

  def additions(model: String) = {
    db withSession { implicit s ⇒
      val adds = for {
        a ← pendingActions if a.model === model
        add ← a.additions
      } yield add
      adds.list
    }
  }

  @DBSession def alphas = Alpha.list
  @DBSession def betas = Beta.list
  @DBSession def gammas = Gamma.list
}

class BasicExtSchemaTest
extends ExtSchemaTest
{
  def is = s2"""
  The extended schema should

  create models $checkModels
  add list associations $listAssoc
  add flat associations $flatAssoc
  create pending actions for alphas $pendingAlpha
  create pending actions for betas $pendingBeta
  create pending actions for gammas $pendingGamma
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
      c.bets must contain(b)
    }
  }

  def flatAssoc = {
    db withSession { implicit s ⇒
      val (a, b, bId, c) = models
      b.loadAlp must_!= b.loadAlp2
    }
  }

  import PendingActionsSchema.Addition

  def pendingAlpha = {
    additionTargets("alphas") === alphas.map(_.id)
  }

  def pendingBeta = {
    additionTargets("betas") === betas.map(_.id)
  }

  def pendingGamma = {
    additionTargets("gammas") === gammas.map(_.id)
  }
}

class DummyRestClient(responses: Seq[String])
extends RestClient
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
  both gamma additions left $gamma
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
    additionTargets("alphas") === alphas.tail.map(_.id)
  }

  def beta = {
    additionTargets("betas") === betas.map(_.id)
  }

  def gamma = {
    additionTargets("gammas") === gammas.map(_.id)
  }
}

class AdvancedExtSchemaTest
extends ExtSchemaTest
{
  def is = s2"""
  Sync of pending actions to a backend

  apply names to multiple records in a table $alphaNames
  apply multiple changed foreign keys to a record $beta
  apply multiple changed associations to a record $gammaAssoc
  apply changed fields to a record $alphaValues
  create no new pending actions $pendingActions
  delete one record $deleteGamma
  """

  import ExtTestSchema._
  import PendingActionsSchema._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit class `json shortcut`[A: EncodeJson](a: A) {
    def js = a.asJson.spaces2
  }

  def beforeAll() {
    resetDb()
    val (a, b, bId, c) = models
    db withSession { implicit s ⇒
      val a1 = AlphaMapper("response_alpha_1", Flag.Off, 5.0,
        oid("a1"))
      val a2 = AlphaMapper("response_alpha_2", Flag.Off, 5.0,
        oid("a2"))
      val b1 = BetaMapper("response_beta_1", None, oid("a2"), oid("a1"),
        oid("b1"))
      val b2 = BetaMapper("response_beta_2", None, oid("a2"), oid("a2"),
        oid("b2"))
      val c = GammaMapper("response_gamma_1", List(oid("b1"), oid("b2")),
        List(), oid("c1"))
      val c2 = GammaMapper("response_gamma_2", List(), List(),
        oid("c2"))
      val responses =
        a1.js ::
        a2.js ::
        Seq(a1, a2).js ::
        b1.js ::
        b2.js ::
        Seq(b1, b2).js ::
        c.js ::
        c2.js ::
        Seq(c).js ::
        Nil
      val backend = new BackendSync {
        val rest = new DummyRestClient(responses)
      }
      val fut = backend(ExtTestSchema)
      fut onFailure {
        case e ⇒ throw e
      }
      Await.ready(fut, 3 seconds)
    }
  }

  def alphaNames = {
    db withSession { implicit s ⇒
      val names = Alpha.list.map(_.name)
      names must_== Set("response_alpha_1", "response_alpha_2")
    }
  }

  def beta = {
    db withSession { implicit s ⇒
      val ids = Beta.list.headOption map { b ⇒ (b.alpId, b.alp2Id) }
      ids must beSome((oid("a2"), oid("a1")))
    }
  }

  def gammaAssoc = {
    db withSession { implicit s ⇒
      val betas = Gamma.list.headOption map {
        c ⇒ (c.loadBets.list, c.loadBet2s.list) } map {
        case (b1, b2) ⇒ (b1.ids, b2.ids) }
      betas must beSome((Set(oid("b1"), oid("b2")), Set()))
    }
  }

  def alphaValues = {
    db withSession { implicit s ⇒
      Alpha.firstOption.map(_.flog) must beSome(Flag.Off)
    }
  }

  def pendingActions = {
    db withSession { implicit s ⇒
      Update.list must be empty
    }
  }

  def deleteGamma = {
    db withSession { implicit s ⇒
      Gamma.list.ids must_== List(oid("c1"))
    }
  }
}
