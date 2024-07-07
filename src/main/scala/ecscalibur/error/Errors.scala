package ecscalibur.error

/**
  * Thrown when the requirements of a method regarding its type parameters are not satisfied.
  *
  * @param msg
  *   the error message
  */
class IllegalTypeParameterException(msg: String = "") extends RuntimeException(msg)

/**
  * Thrown when a class has not been defined correctly.
  *
  * @param msg
  *   the error message
  */
class IllegalDefinitionException(msg: String = "") extends RuntimeException(msg)
