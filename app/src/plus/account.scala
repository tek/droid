package tryp.droid

import scalaz._, Scalaz._, concurrent._

import java.net.URL

import android.graphics.drawable.Drawable

import com.google.android.gms
import gms.common.{api ⇒ gapi}
import gms.plus
import gapi.GoogleApiClient

class PlusAccount(apiClient: GoogleApiClient)
{
  import plus.Plus._

  lazy val person = Maybe.fromNullable(PeopleApi.getCurrentPerson(apiClient))

  def email = Maybe.fromNullable(AccountApi.getAccountName(apiClient))

  def name = person map(_.getName) map
    { n ⇒ s"${n.getGivenName} ${n.getFamilyName}" }

  lazy val photo = person flatMap(p ⇒ Maybe.fromNullable(p.getImage))

  lazy val photoUrl = for {
    ph ← photo
    u ← Maybe.fromNullable(ph.getUrl)
  } yield new URL(u)

  lazy val cover = for {
    per ← person
    c ← Maybe.fromNullable(per.getCover)
    ph ← Maybe.fromNullable(c.getCoverPhoto)
  } yield ph

  lazy val coverUrl = cover map { c ⇒ new URL(c.getUrl) }

  def drawable(url: Maybe[URL]) = {
    Task {
      url map { u ⇒
        val stream = u.openConnection().getInputStream
        Drawable.createFromStream(stream, "")
      }
    }
  }

  def photoDrawable = drawable(photoUrl)

  def coverDrawable = drawable(coverUrl)
}
