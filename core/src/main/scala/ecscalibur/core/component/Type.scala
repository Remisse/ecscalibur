package ecscalibur.core.component

import scala.util.hashing.MurmurHash3
import scala.reflect.ClassTag

private[ecscalibur] object tpe:
  /** Generates a ComponentId for the given class.
    *
    * @param cls
    *   the class for which a ComponentId will be generated
    * @return
    *   a unique ComponentId
    */
  inline def getId(cls: Class[?]): ComponentId = ComponentId(createId(cls.getName))

  /** Generates a ComponentId for the given class name.
    *
    * @param cls
    *   the class for which a ComponentId will be generated
    * @return
    *   a unique ComponentId
    */
  inline def getId(clsName: String): ComponentId = ComponentId(createId(clsName))

  private def createId(clsName: String): Int = MurmurHash3.stringHash(base(clsName))

  // Should be faster than replaceAll
  private inline def base(clsName: String): String = clsName.replace(".", "").replace("$", "")

  /** Retrieves the ComponentId of T, regardless of whether T is 0- or higher-kinded.
    *
    * For instance, if T corresponds to the component class 'B', then B's ComponentId will be
    * returned. However, if T is A[B], then A's ComponentId will be returned.
    *
    * @tparam T
    *   the type for which the ComponentId must be retrieved
    * @return
    *   the ComponentId of the outermost type of the given type parameter.
    */
  inline def id0K[T <: WithType: ClassTag]: ComponentId = getId(summon[ClassTag[T]].runtimeClass)
