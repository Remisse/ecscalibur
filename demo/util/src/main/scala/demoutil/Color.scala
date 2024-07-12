package demoutil

import scala.util.Random

enum Color:
  case White, Black, Red, Green, Blue

object Color:
  inline def random: Color = Color.fromOrdinal(Random.nextInt(Color.values.length))
