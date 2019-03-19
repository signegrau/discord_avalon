package game

trait Role

trait Evil extends Role
case object Mordred extends Evil
case object Assassin extends Evil
case object Morgana extends Evil
case object Oberon extends Evil
case object Minion extends Evil

trait Good extends Role
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