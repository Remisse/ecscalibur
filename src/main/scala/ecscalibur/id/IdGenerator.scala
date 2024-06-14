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

  override inline def next: Int =
    var id: Int = -1
    erasedIds.length match
      case 0 =>
        id = highestAvailableIdx
        highestAvailableIdx = highestAvailableIdx + 1
      case _ => id = erasedIds.remove(0)
    id

  override inline def erase(id: Int): Boolean =
    var res = false
    if isValid(id) then
      erasedIds += id
      res = true
    res

  override inline def isValid(id: Int): Boolean =
    id < highestAvailableIdx && !erasedIds.contains(id)
