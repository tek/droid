package slick.db

import java.sql.Driver

import scala.annotation.StaticAnnotation
import scala.reflect.macros.whitebox.Context

import javax.sql.DataSource
import java.util.Properties

class DBTransaction extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any =
    macro TransactionMacro.implTransaction
}

class DBNewTransaction extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any
  = macro TransactionMacro.implNewTransaction
}

class DBSession extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any =
    macro TransactionMacro.implSession
}

class DBNewSession extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any =
    macro TransactionMacro.implNewSession
}

case class DBConnectionInfo( url: String, driverClassName: String)

class TransactionMacro(val c: Context)
{
  def implTransaction(annottees: c.Expr[Any]*) = {
    impl("withTransaction")(annottees: _*)
  }

  def implNewTransaction(annottees: c.Expr[Any]*) = {
    impl("withDynTransaction")(annottees: _*)
  }

  def implSession(annottees: c.Expr[Any]*) = {
    impl("withSession")(annottees: _*)
  }

  def implNewSession(annottees: c.Expr[Any]*) = {
    impl("withDynSession")(annottees: _*)
  }

  def impl(sessionType: String)(annottees: c.Expr[Any]*) = {
    import c.universe._
    val q"$mods def $name[..$tparams](...$paramss): $tpt = $body" =
      annottees.map(_.tree).toList.head
    q"""
    $mods def $name[..$tparams](...$paramss): $tpt =
      dbLock synchronized {
        implicitly[Database] ${TermName(sessionType)} { implicit session ⇒
          $body
        }
      }
    """
  }
}
