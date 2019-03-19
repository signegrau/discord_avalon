package main

import com.sedmelluq.discord.lavaplayer.player.{AudioPlayerManager, DefaultAudioPlayerManager}
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.{AudioPlaylist, AudioTrack}
import net.katsstuff.ackcord._
import net.katsstuff.ackcord.commands._
import net.katsstuff.ackcord.syntax._
import akka.NotUsed
import cats.Id
import game._
import net.katsstuff.ackcord.APIMessage.MessageCreate

import scala.util.Random

object Main extends App {
  val GeneralCommands = "!"

  val token = "NTU2OTk4NjY0MjU0MzkwMjgy.D3GlOA.7oQwwwBAgXAxf-M-hu16oxBLrhE"
  val clientSettings = ClientSettings(
    token,
    commandSettings = CommandSettings(needsMention = false, prefixes = Set(GeneralCommands))
  )

  import clientSettings.executionContext

  val futureClient = clientSettings.createClient()

  trait State
  case object InitialState extends State
  case class NewGame(roles: Seq[Role]) extends State
  case class CompanionSelection(board: BoardState) extends State
  case class PreMissionVoting(board: BoardState, votes: PreMissionVotes) extends State
  case class SuccessFailureVoting(board: BoardState, votes: Seq[Player]) extends State

  var state: State = InitialState
  val paricipating = scala.collection.concurrent.TrieMap.empty[String, Player]

  futureClient.foreach { client =>
    import client.sourceRequesterRunner._
    client.onEvent[Id] {
      case APIMessage.Ready(_) => println("Now ready")
      case MessageCreate(message, cache) => {
        println(message)
        message.guildId match {
          case Some(_) => ()
          case None => {
            state match {
              case PreMissionVoting(board, PreMissionVotes(votes)) => {
                if (board.playersOnMission.exists(x => x.uid == message.authorUserId.map(_.toString).mkString)) {
                  if (message.content.contains("!avalon vote ")) {
                    val vote: PreMissionVotePhase = if (message.content.contains("Yes")) {
                      Yes: PreMissionVotePhase
                    } else {
                      No: PreMissionVotePhase
                    }

                    state = PreMissionVoting(board, PreMissionVotes(votes.filter(_._2.uid != message.authorUserId.map(_.toString).mkString)
                      ++ Seq((vote, board.players.find(x => x.uid == message.authorUserId.map(_.toString).mkString).get))))
                  }
                } else {

                }
              }
              case _ => ()
            }
          }
        }
      }
      case _ => ()
    }

    client.onRawCmd[SourceRequest] {
      client.withCache[SourceRequest, RawCmd[Id]] { implicit c =>
      {
        case RawCmd(message, GeneralCommands, "avalon", args, _) =>
          val responsestate = state match {
            case InitialState => args.head match {
              case "newgame" => {
                if (args.tail.isEmpty) {
                  "Need game creation arguments, please consolidate the help section"
                } else {
                  val default = "Creating new game "

                  val argResponse = if (args(1).forall(_.isDigit)) {
                    state = NewGame(Random.shuffle(Role.presets(args(1).toInt)))
                    s"with ${args(1).toInt} player preset"
                  } else {
                    state = NewGame(Random.shuffle(args.tail.map(Role.fromString)))
                    s"with characters ${args.tail.mkString(" ")}"
                  }

                  default + argResponse
                }
              }
              case _ => "Please start a new game before issuing commands"
            }
            case NewGame(roles) => { args.head match {
              case "join" =>
                val r = if (roles.nonEmpty) {
                  val role = roles.head

                  if (paricipating.contains(message.authorUserId.map(x => x.toString).mkString)) {
                    s"You are already in the game ${message.authorUserId.flatMap(_.resolve.value).map(_.username).mkString}"
                  } else {
                    paricipating.update(
                      message.authorUserId.map(x => x.toString).mkString,
                      Player(message.authorUserId.map(_.toString).mkString, message.authorUserId.flatMap(_.resolve.value).map(_.username).mkString, role))

                    state = NewGame(roles.tail)
                    "User joined"
                  }
                } else {""}

                if (roles.size == paricipating.size) {
                  val king = Random.shuffle(paricipating.values.toSeq).head
                  state = CompanionSelection(GameState.newGame(paricipating.values.toSeq, king))
                  s"Game commencing with players\n${paricipating.values.map(x => x.playerName).mkString("\n")}" +
                  s"\nThe king ${king.playerName} may now select the players who will join his quest, by typing\n!avalon invite PLAYERNAME"
                } else {
                  r
                }
              case _ => "We are in join phase, only joining is allowed"
              }}
            case CompanionSelection(board) => { args.head match {
              case "status" => {
                "The invited companions are\n" + board.playersOnMission.map(_.playerName).mkString("\n")
              }
              case "commence" => {
                if (board.playersOnMission.size == MissionRequirements.getPlayersForMission(board.players.size, board.mission).people) {
                  state = PreMissionVoting(board, PreMissionVotes(Seq.empty))

                  for {
                    channel <- optionPure(message.dmChannel[Id].value)
                    _       <- run(channel.sendMessage(content = "Vote with !avalon vote Yes/No"))
                  } yield ()

                  "Commencing mission with\n" + board.playersOnMission.map(_.playerName).mkString("\n")
                } else {
                  "Please select the correct number of companions"
                }
              }
              case "invite" => {
                if (board.playersOnMission.map(_.playerName).contains(args(1))) {
                  "Please select a player who hasn't been invited yet"
                } else {
                  val player = paricipating.find(_._2.playerName == args(1))

                  player.map{ p =>
                    val default = s"Player ${p._2.playerName} has been invited"

                    state = CompanionSelection(board.copy(playersOnMission = board.playersOnMission ++ Seq(p._2)))
                    s"Player ${p._2.playerName} has been invited"
                  }.getOrElse("Player not found")
                }
              }
              case "uninvite" => {
                if (board.playersOnMission.exists(x => x.playerName == args(1))) {
                  val newList = board.playersOnMission.filter(x => x.playerName != args(1))
                  state = CompanionSelection(board.copy(playersOnMission = newList))
                  "The player has been uninvited from the quest"
                } else {
                  "The player has not been invited"
                }
              }
              case _ => s"Please enter a valid command"
            }}
            case PreMissionVoting(board, votes) => { args.head match {
              case _ => ""
            }}
          }

          for {
            channel <- optionPure(message.tGuildChannel[Id].value)
            _       <- run(channel.sendMessage(content = responsestate))
          } yield ()
        case _ => client.sourceRequesterRunner.unit
      }
      }
    }

    client.login()
  }

  println("Hello")
}

