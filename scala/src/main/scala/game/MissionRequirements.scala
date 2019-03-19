package game

case class MissionRequirements(
                  people: Int,
                  failuresAllowed: Int = 0
                  )

object MissionRequirements {
  val missions = Map(
    1 -> Seq(MissionRequirements(1),MissionRequirements(1),MissionRequirements(1),MissionRequirements(1),MissionRequirements(1)),
    5 -> Seq(MissionRequirements(2),MissionRequirements(3),MissionRequirements(2),MissionRequirements(3),MissionRequirements(3)),
    6 -> Seq(MissionRequirements(2),MissionRequirements(3),MissionRequirements(4),MissionRequirements(3),MissionRequirements(4)),
    7 -> Seq(MissionRequirements(2),MissionRequirements(3),MissionRequirements(3),MissionRequirements(4,1),MissionRequirements(4)),
    8 -> Seq(MissionRequirements(3),MissionRequirements(4),MissionRequirements(4),MissionRequirements(5,1),MissionRequirements(5)),
    9 -> Seq(MissionRequirements(3),MissionRequirements(4),MissionRequirements(4),MissionRequirements(5,1),MissionRequirements(5)),
    10 -> Seq(MissionRequirements(3),MissionRequirements(4),MissionRequirements(4),MissionRequirements(3,1),MissionRequirements(5))
  )

  def getMissions(playerCount: Int): Seq[MissionRequirements] = missions(playerCount)

  def getPlayersForMission(playerCount: Int, missionIndex: Int): MissionRequirements =
    missions(playerCount)(missionIndex)
}