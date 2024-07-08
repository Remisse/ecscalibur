package ecsutil

trait ProgressiveMap[T]:
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
    import com.google.common.collect.HashBiMap
    import com.google.common.collect.BiMap

    private var effectiveSize = 0
    private val bimap: BiMap[T, Int] = HashBiMap.create()
    private val idGenerator = IdGenerator()

    override def +=(elem: T): ProgressiveMap[T] =
      require(!contains(elem), s"Element $elem has already been mapped.")
      bimap.put(elem, idGenerator.next)
      effectiveSize += 1
      this

    override def -=(elem: T): ProgressiveMap[T] =
      require(contains(elem), s"Element $elem has not been mapped.")
      val id = bimap.remove(elem)
      idGenerator.erase(id)
      effectiveSize -= 1
      this

    override inline def contains(elem: T): Boolean = bimap.containsKey(elem)

    override def apply(elem: T): Int =
      require(contains(elem), s"Element $elem not mapped.")
      bimap.get(elem)

    override def ofId(id: Int): T =
      require(bimap.containsValue(id), s"No elements with id $id.")
      bimap.inverse().get(id)

    override def foreach(f: (T, Int) => Unit): Unit =
      bimap.forEach: (k, v) =>
        f(k, v)

    override def size: Int = effectiveSize

    override def isEmpty: Boolean = effectiveSize == 0
