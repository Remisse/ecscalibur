package ecsutil

/** Defines a bidirectional map where each element is mapped to a unique, autoincrementing number.
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

  /** Returns the element corresponding to the given mapping.
    *
    * @param id
    *   the mapping corresponding to the desired element
    * @throws IllegalArgumentException
    *   if this map does not contain the given mapping
    * @return
    *   the element corresponding to the given mapping
    */
  infix def ofId(id: Int): T

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
