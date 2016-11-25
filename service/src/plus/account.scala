package tryp
package droid
package state

import scalaz.Maybe

import java.net.URL

import android.graphics.drawable.Drawable

import com.google.android.gms
import gms.common.{api => gapi}
import gms.plus
import gapi.GoogleApiClient

class PlusAccount(apiClient: GoogleApiClient)
{
  import plus.Plus._

  // lazy val person = Maybe.fromNullable(PeopleApi.getCurrentPerson(apiClient))
  lazy val person: Maybe[plus.model.people.Person] = Maybe.empty

  // def email = Maybe.fromNullable(AccountApi.getAccountName(apiClient))
  def email: Maybe[String] = Maybe.empty

  def name = person map(_.getName) map
    { n => s"${n.getGivenName} ${n.getFamilyName}" }

  lazy val photo = person flatMap(p => Maybe.fromNullable(p.getImage))

  lazy val photoUrl = for {
    ph <- photo
    u <- Maybe.fromNullable(ph.getUrl)
  } yield new URL(u)

  lazy val cover = for {
    per <- person
    c <- Maybe.fromNullable(per.getCover)
    ph <- Maybe.fromNullable(c.getCoverPhoto)
  } yield ph

  lazy val coverUrl = cover map { c => new URL(c.getUrl) }

  def drawable(url: Maybe[URL]) = {
    Task {
      url map { u =>
        val stream = u.openConnection().getInputStream
        Drawable.createFromStream(stream, "")
      }
    }
  }

  def photoDrawable = drawable(photoUrl)

  def coverDrawable = drawable(coverUrl)
}
