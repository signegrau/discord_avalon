package game

trait StepInfo

case object MissionDelegation extends StepInfo

trait PreMissionVotePhase
case object Yes extends PreMissionVotePhase
case object No extends PreMissionVotePhase
case class PreMissionVotes(votes: Seq[(PreMissionVotePhase, Player)]) extends StepInfo


trait PostMissionVote
case object Success extends PostMissionVote
case object Failure extends PostMissionVote
case class PostMissionVotes(votes: Seq[PostMissionVote]) extends StepInfo

import scala.util.Random

object GameState {
  def newGame(players: Seq[Player], king: Player) = BoardState(
    players = Random.shuffle(players),
    gamePhase = CompanionDelegation,
    playersOnMission = Seq.empty,
    king = king,
    failedMissions = 0,
    mission = 0
  )

  def performStep(boardState: BoardState, vote: StepInfo): Either[String, (BoardState, String)] =
    boardState.gamePhase match {
      case CompanionDelegation => vote match {
        case MissionDelegation => {
          val del = boardState.playersOnMission
          val mis = MissionRequirements.getPlayersForMission(boardState.players.length, boardState.mission)
          if (mis.people < del.length) {
            Left("Too many votes")
          } else if (mis.people > del.length) {
            Left("Not enough votes")
          } else {
            Right((boardState.copy(gamePhase = PreMissionVote), "We will now go on a journey"))
          }
        }
        case _ => Left("Semantic error")
      }

      case PreMissionVote => vote match {
        case PreMissionVotes(votes) => if (votes.count{
          case (Yes, _) => false
          case (No, _) => true
        } > boardState.players.length / 2) {
          Right((boardState.copy(
            playersOnMission = Seq.empty,
            gamePhase = CompanionDelegation,
            king = boardState.players((boardState.players.indexOf(boardState.king) + 1) % boardState.players.length),
            failedMissions = boardState.failedMissions + 1
          ),"Vote failed\n" + votes.map(x => s"${x._2.playerName} voted ${x._1 match {
              case Yes => "Yes"
              case No => "No"
            }}").mkString("\n")))
        } else {
          Right((boardState.copy(gamePhase = SuccessFailureVote),
            "Vote succeeded\n" + votes.map(x => s"${x._2.playerName} voted ${x._1 match {
              case Yes => "Yes"
              case No => "No"
            }}").mkString("\n")))
        }
        case _ => Left("Semantic error")
      }

      case SuccessFailureVote => vote match {
        case PostMissionVotes(votes) => {
          val mis = MissionRequirements.getPlayersForMission(boardState.players.length, boardState.mission)

          val r = if (votes.count{
            case Failure => true
            case Success => false
          } > mis.failuresAllowed) {
            Right((boardState.copy(failedMissions = boardState.failedMissions + 1), s"Failure found \n${Random.shuffle(votes).map{
              case Success => "Success"
              case Failure => "Failure"
            }.mkString("\n")}"))
          } else {
            Right((boardState, "Success!"))
          }

          r.map{b => (b._1.copy(
            gamePhase = CompanionDelegation,
            playersOnMission = Seq.empty,
            king = boardState.players((boardState.players.indexOf(boardState.king) + 1) % boardState.players.length),
            mission = boardState.mission + 1
          ), b._2)}
        }
        case _ => Left("Semantic error")
      }
    }
}
