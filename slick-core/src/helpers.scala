package slick

object Helpers
{
  def plural(name: String) = {
    val rules = List(
      ("(\\w*)people$", "$1people"),
      ("(\\w*)children$", "$1children"),
      ("(\\w*)feet$", "$1feet"),
      ("(\\w*)teeth$", "$1teeth"),
      ("(\\w*)men$", "$1men"),
      ("(\\w*)equipment$", "$1equipment"),
      ("(\\w*)information$", "$1information"),
      ("(\\w*)rice$", "$1rice"),
      ("(\\w*)money$", "$1money"),
      ("(\\w*)fish$", "$fish"),
      ("(\\w*)sheep$", "$1sheep"),
      ("(\\w+)(es)$", "$1es"),
      // Check exception special case words
      ("(\\w*)person$", "$1people"),
      ("(\\w*)child$", "$1children"),
      ("(\\w*)foot$", "$1feet"),
      ("(\\w*)tooth$", "$1teeth"),
      ("(\\w*)bus$", "$1buses"),
      ("(\\w*)man$", "$1men"),
      ("(\\w*)(ox|oxen)$", "$1$2"),
      ("(\\w*)(buffal|tomat)o$", "$1$2oes"),
      ("(\\w*)quiz$", "$1$2zes"),
      // Greek endings
      ("(\\w+)(matr|vert|ind)ix|ex$", "$1$2ices"),
      ("(\\w+)(sis)$", "$1ses"),
      ("(\\w+)(um)$", "$1a"),
      // Old English. hoof -> hooves, leaf -> leaves
      ("(\\w*)(fe)$", "$1ves"),
      ("(\\w*)(f)$", "$1ves"),
      ("(\\w*)([m|l])ouse$", "$1$2ice"),
      // Y preceded by a consonant changes to ies
      ("(\\w+)([^aeiou]|qu)y$", "$1$2ies"),
      // Voiced consonants add es instead of s
      ("(\\w+)(z|ch|sh|as|ss|us|x)$", "$1$2es"),
      // Check exception special case words
      ("(\\w*)cactus$", "$1cacti"),
      ("(\\w*)focus$", "$1foci"),
      ("(\\w*)fungus$", "$1fungi"),
      ("(\\w*)octopus$", "$1octopi"),
      ("(\\w*)radius$", "$1radii"),
      // If nothing else matches, and word ends in s, assume plural already
      ("(\\w+)(s)$", "$1s")
    )
    rules.find(it ⇒ name.matches(it._1)).map(it ⇒ name.replaceFirst(it._1, it._2)).getOrElse(name.replaceFirst("([\\w]+)([^s])$", "$1$2s"))
  }

  def singular(name: String) = {
    if (name.lastOption.contains('s')) name.init else name
  }

  def decapitalize(name: String) = {
    if (name.length == 0) name
    else {
      val chars = name.toCharArray()
      var i = 0
      while (i < chars.length && Character.isUpperCase(chars(i))) {
        if (i > 0 && i < chars.length - 1 && Character.isLowerCase(chars(i + 1))) {
        } else {
          chars(i) = Character.toLowerCase(chars(i))
        }
        i = i + 1
      }
      new String(chars)
    }
  }
}
