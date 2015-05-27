package slick

object Helpers
{
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
