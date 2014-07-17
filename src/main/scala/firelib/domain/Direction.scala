package firelib.domain

object DirectionEnum {
    val Up = new Direction("Up")
    val Down = new Direction("Down")
    val None = new Direction("None")

}

sealed class Direction(val Name: String) {
    override def toString: String = Name
}
