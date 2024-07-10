package ecsutil

import scala.annotation.targetName

/** Defines a map where each element is mapped to a unique, autoincrementing number.
  */
trait ProgressiveMap[T]:
  /** Adds an element to this map.
    *
    * @param elem
    *   the element to add
    * @throws IllegalArgumentException
    *   if this map does not contain the given element
    * @return
    *   this map
    */
  @targetName("add")
  def +=(elem: T): ProgressiveMap[T]

  /** Removes an element from this map.
    *
    * @param elem
    *   the element to remove
    * @throws IllegalArgumentException
    *   if this map does not contain the given element
    * @return
    *   this map
    */
  @targetName("remove")
  def -=(elem: T): ProgressiveMap[T]

  /** Checks whether this map contains the given element.
    *
    * @param elem
    *   element to perform the check on
    * @return
    *   true if this map contains the given element, false otherwise.
    */
  infix def contains(elem: T): Boolean

  /** Returns the mapping of the given element.
    *
    * @param elem
    *   element whose mapping must be returned
    * @throws IllegalArgumentException
    *   if this map does not contain the given element
    * @return
    *   the given element's mapping
    */
  def apply(elem: T): Int

  /** Iterates over all elements in this map and executes `f` for each of them
    *
    * @param f
    *   the function to execute on each element of this map
    */
  infix def foreach(f: (T, Int) => Unit): Unit

  /** Returns the size of this map.
    *
    * @return
    *   the size of this map
    */
  def size: Int

  /** Checks whether this map is empty.
    *
    * @return
    *   true if this map is empty, false otherwise
    */
  def isEmpty: Boolean

object ProgressiveMap:
  /** Creates an empty [[ProgressiveMap]] of T.
    *
    * @tparam the
    *   type of the elements of this map
    * @return
    *   an empty ProgressiveMap
    */
  def apply[T](): ProgressiveMap[T] = ProgressiveMapImpl[T]()

  /** Creates a [[ProgressiveMap]] of T populated with the given elements.
    *
    * @param elems
    *   elements the new map will be populated with
    * @return
    *   a new ProgressiveMap populated with the given elements
    */
  def from[T](elems: T*): ProgressiveMap[T] =
    val res = ProgressiveMap[T]()
    elems.foreach: e =>
      val _ = res += e
    res

  private inline val Uninitialized = -1

  private final class ProgressiveMapImpl[T] extends ProgressiveMap[T]:
    import scala.collection.mutable

    private val map: mutable.Map[T, Int] = mutable.Map.empty.withDefaultValue(Uninitialized)
    private val idGenerator = IdGenerator()

    @targetName("add")
    override def +=(elem: T): ProgressiveMap[T] =
      require(!contains(elem), s"Element $elem has already been mapped.")
      val t = elem -> idGenerator.next
      map += t
      this

    @targetName("remove")
    override def -=(elem: T): ProgressiveMap[T] =
      val id = map(elem)
      require(id != Uninitialized, s"Element $elem has not been mapped.")
      idGenerator.erase(id)
      map -= elem
      this

    override inline def contains(elem: T): Boolean = map(elem) != Uninitialized

    override def apply(elem: T): Int =
      val res = map(elem)
      require(res != Uninitialized, s"Element $elem not mapped.")
      res

    override def foreach(f: (T, Int) => Unit): Unit =
      for e <- map do
        f(e._1, e._2)

    override def size: Int = map.size

    override def isEmpty: Boolean = map.isEmpty
