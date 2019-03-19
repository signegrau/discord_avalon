package game

trait Phase
case object CompanionDelegation extends Phase
case object PreMissionVote extends Phase
case object SuccessFailureVote extends Phase

case class Player(
                 uid: String,
                 playerName: String,
                 role: Role
                 )

case class BoardState (
                      players: Seq[Player],
                      gamePhase: Phase,
                      playersOnMission: Seq[Player],
                      king: Player,
                      failedMissions: Int,
                      mission: Int
                      )
