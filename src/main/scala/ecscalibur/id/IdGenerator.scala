package ecscalibur.id

trait IdGenerator:
  def next: Int
  def erase(id: Int): Boolean
  def isValid(id: Int): Boolean

object IdGenerator:
  def apply(): IdGenerator = IdGeneratorImpl()

private class IdGeneratorImpl extends IdGenerator:
  private var highestAvailableIdx = 0
  import scala.collection.mutable.ArrayBuffer
  private val erasedIds = ArrayBuffer.empty[Int]

  override def next: Int =
    var res: Int = -1
    erasedIds.length match
      case 0 =>
        res = highestAvailableIdx
        highestAvailableIdx = highestAvailableIdx + 1
      case _ => res = erasedIds.remove(erasedIds.length - 1)
    res

  override def erase(id: Int): Boolean =
    if isValid(id) then
      erasedIds += id
      return true
    false

  override def isValid(id: Int): Boolean =
    id < highestAvailableIdx && !erasedIds.contains(id)
