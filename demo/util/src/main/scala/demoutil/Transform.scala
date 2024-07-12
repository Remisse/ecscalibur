package demoutil 

object transform:
  case class Vector2(x: Float, y: Float):
    inline def +(that: Vector2): Vector2 =
      Vector2(x + that.x, y + that.y)

    inline def -(that: Vector2): Vector2 =
      Vector2(x - that.x, y - that.y)

    inline def *(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)

    inline def distance(that: Vector2): Float =
      math.sqrt(math.pow(x + that.x, 2) + math.pow(y + that.y, 2)).toFloat

    inline def opposite: Vector2 = Vector2(-x, -y)

    inline def magnitude: Float =
      math.sqrt(x * x + y * y).toFloat

  object Vector2:
    inline def zero: Vector2 = Vector2(0f, 0f)
