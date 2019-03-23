package game

trait Role {
  def isEvil: Boolean
  def isGood: Boolean
}

trait Evil extends Role {
  def isEvil: Boolean = true
  def isGood: Boolean = false
}
case object Mordred extends Evil
case object Assassin extends Evil
case object Morgana extends Evil
case object Oberon extends Evil
case object Minion extends Evil

trait Good extends Role{
  def isEvil: Boolean = false
  def isGood: Boolean = true
}
case object Merlin extends Good
case object Percival extends Good
case object Servant extends Good

object Role {
  val presets = Map[Int, Seq[Role]](
    5 -> Seq(Mordred, Assassin, Merlin, Servant, Servant)
  )

  def fromRole(role: Role): String = role match {
    case Mordred => "Mordred"
    case Assassin => "Assassin"
    case Morgana => "Morgana"
    case Oberon => "Oberon"
    case Minion => "Minion"
    case Merlin => "Merlin"
    case Percival => "Percival"
    case Servant => "Servant"
  }

  def fromString(role: String): Role = role match {
    case "Mordred"  => Mordred
    case "Assassin"  => Assassin
    case "Morgana"  => Morgana
    case "Oberon"  => Oberon
    case "Minion"  => Minion
    case "Merlin"  => Merlin
    case "Percival"  => Percival
    case "Servant"  => Servant
  }
}