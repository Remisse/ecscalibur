package ecsutil

trait ProgressiveMap[T] extends Iterable[(T, Int)]:
  def +=(elem: T): ProgressiveMap[T]

  def -=(elem: T): ProgressiveMap[T]

  infix def contains(elem: T): Boolean

  def apply(elem: T): Int

  infix def ofId(id: Int): T

  infix def foreach(f: (T, Int) => Unit): Unit

  def size: Int

  def isEmpty: Boolean

object ProgressiveMap:
  def apply[T](): ProgressiveMap[T] = ProgressiveMapImpl[T]()

  def from[T](elems: T*): ProgressiveMap[T] =
    val res = ProgressiveMap[T]()
    elems.foreach: e =>
      val _ = res += e
    res

  final class ProgressiveMapImpl[T] extends ProgressiveMap[T]:
    import scala.collection.mutable

    private var effectiveSize = 0
    private val map: mutable.Map[T, Int] = mutable.Map.empty
    private val reverse: mutable.Map[Int, T] = mutable.Map.empty
    private val idGenerator = IdGenerator()

    override def +=(elem: T): ProgressiveMap[T] = 
      require(!contains(elem), s"Element $elem has already been mapped.")
      val id = idGenerator.next
      map += (elem -> id)
      reverse += (id -> elem)
      effectiveSize += 1
      this

    override def -=(elem: T): ProgressiveMap[T] = 
      require(contains(elem), s"Element $elem has not been mapped.")
      val id = map.remove(elem).head
      reverse -= id
      idGenerator.erase(id)
      effectiveSize -= 1
      this

    override inline def contains(elem: T): Boolean = map.contains(elem)

    override def apply(elem: T): Int = 
        require(contains(elem), s"Element $elem not mapped.")
        map(elem)

    override def ofId(id: Int): T =
      require(reverse.contains(id), s"No elements with id $id.")
      reverse(id)

    override def foreach(f: (T, Int) => Unit): Unit =
      map foreach: (elem, id) =>
        f(elem, id)

    override def size: Int = effectiveSize

    override def isEmpty: Boolean = effectiveSize == 0

    override def iterator: Iterator[(T, Int)] = map.iterator
