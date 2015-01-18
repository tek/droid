package tryp.droid

class Fragments
{
  var settings: () ⇒ SettingsFragment = () ⇒ new SettingsFragment
  var drawer: () ⇒ DrawerFragment = () ⇒ new DrawerFragment
}

object Classes
{
  var fragments: Fragments = new Fragments
}
