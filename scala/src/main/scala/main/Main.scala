package main

import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.{Message, MessageChannel, PrivateChannel}
import discord4j.core.event.domain.lifecycle.ReadyEvent
import game._
import reactor.core.publisher.Mono

import scala.util.{Random, Try}

object Main extends App {
  val GeneralCommands = "!"

  val token = "NTU2OTk4NjY0MjU0MzkwMjgy.D3GlOA.7oQwwwBAgXAxf-M-hu16oxBLrhE"
  val client = new DiscordClientBuilder(token).build

  client.getEventDispatcher.on(classOf[ReadyEvent]).subscribe(ready => println("Logged in as " + ready.getSelf.getUsername))

  import discord4j.core.event.domain.message.MessageCreateEvent

  trait State
  case object InitialState extends State
  case class NewGame(roles: Seq[Role]) extends State
  case class CompanionSelection(board: BoardState) extends State
  case class PreMissionVoting(board: BoardState, votes: PreMissionVotes) extends State
  case class SuccessFailureVoting(board: BoardState, votes: PostMissionVotes) extends State
  case class AssassinPickPhase(board: BoardState) extends State

  var state: State = InitialState
  var gameChannel : MessageChannel = _
  val paricipating = scala.collection.concurrent.TrieMap.empty[String, Player]

  val msgCreate = client.getEventDispatcher.on(classOf[MessageCreateEvent])
    .map[Message](_.getMessage)
    .filter(x => x.getContent.map[Boolean](z => z.startsWith("!avalon")).orElse(false))
    .map[Message]{ msg =>
      val dmChannel = msg.getAuthor.map[PrivateChannel](x => x.getPrivateChannel.block()).get
      val isDmMessage = msg.getChannelId.toString == dmChannel.getId.toString

      val responsestate = Try {
        val authorid = msg.getAuthor.map[String](x => x.getId.asString()).get
        val authorName = msg.getAuthor.map[String](x => x.getUsername).get

        val args = msg.getContent.map[Seq[String]](x => x.split(" ").tail).orElse(Seq.empty)


        state match {
          case InitialState => args.head match {
            case "newgame" => {
              gameChannel = msg.getChannel.block()

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
              if (roles.nonEmpty) {
                val role = roles.head

                if (paricipating.contains(authorid)) {
                  s"You are already in the game ${authorName}"
                } else {
                  paricipating.update(
                    authorid,
                    Player(authorid, authorName, dmChannel, role))

                  val newRoles = roles.tail

                  state = NewGame(newRoles)

                  if (newRoles.isEmpty) {
                    paricipating.values.map(x => x.dmChannel.createMessage(s"You are ${Role.fromRole(x.role)}").block())

                    val king = Random.shuffle(paricipating.values.toSeq).head
                    state = CompanionSelection(GameState.newGame(paricipating.values.toSeq, king))

                    //Merlin sees everyone but mordred
                    val merlin = paricipating.values.find(_.role == Merlin)

                    if (merlin.isEmpty) {
                      "We're gonna restart the game, there is no Merlin"
                    } else {
                      println("Hello1")
                      val evils = paricipating.values.filter { x =>
                        x.role match {
                          case _: Evil => true
                          case _: Good => false
                        }
                      }
                      println("Hello2")

                      evils.foreach { e =>
                        merlin.get.dmChannel.createMessage(s"You see that ${e.playerName} is evil").block()
                      }
                      println("Hello3")

                      val morg = paricipating.values.find(_.role == Morgana)

                      morg.map { morgana =>
                        val morgAndMerlin = Random.shuffle(Seq(merlin.get, morgana))
                        println("Hello4")

                        paricipating.values.find(_.role == Percival).map { p =>
                          p.dmChannel.createMessage("You see two players, one which is Merlin and one of which is Morgana\n" +
                            s"The players are ${morgAndMerlin.head.playerName} and ${morgAndMerlin.last.playerName}").block()
                        }
                      }

                      println("Hello5")

                      val evilButOberon = evils.filter(_.role != Oberon)

                      evilButOberon.toList.combinations(2).foreach { x =>
                        val first = x.head
                        val last = x.last

                        first.dmChannel.createMessage(s"${last.playerName} is evil").block()
                        last.dmChannel.createMessage(s"${first.playerName} is evil").block()
                      }
                      println("Hello6")

                      s"User joined\n\nGame commencing with players\n${paricipating.values.map(x => x.playerName).mkString("\n")}" +
                        s"\nThe king ${king.playerName} may now select the players who will join his quest, by typing\n!avalon invite PLAYERNAME"+
                      s"\nThe required amount of players for this mission is ${MissionRequirements.getPlayersForMission(paricipating.size, 0).people}" +
                        s"\nThe amount of failures required for the evil to win the round is ${MissionRequirements.getPlayersForMission(paricipating.size, 0).failuresAllowed}"
                    }
                  } else {
                    "User joined"
                  }
                }
              } else {""}

            case _ => "We are in join phase, only joining is allowed"
          }}
          case CompanionSelection(board) => { args.head match {
            case "status" => {
              "The invited companions are\n" + board.playersOnMission.map(_.playerName).mkString("\n")
            }
            case "commence" => {
              if (authorid == board.king.uid) {
                if (board.playersOnMission.size == MissionRequirements.getPlayersForMission(board.players.size, board.mission).people) {
                  state = PreMissionVoting(board.copy(gamePhase = PreMissionVote), PreMissionVotes(Seq.empty))

                  paricipating.values.map{ p =>
                    p.dmChannel.createMessage("Please vote with !avalon vote Yes/No").block()
                  }

                  "Commencing mission with\n" + board.playersOnMission.map(_.playerName).mkString("\n")
                } else {
                  s"Please select the correct number of companions " +
                    s"${MissionRequirements.getPlayersForMission(board.players.size, board.mission).people}" +
                  "\nCurrently selected are\n" +
                  board.playersOnMission.map(_.playerName.mkString("\n"))
                }
              } else {
                "You are not the king"
              }
            }
            case "invite" => {
              if (authorid == board.king.uid) {
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
              } else {
                "You are not the king"
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
            case "vote" => {
              if (isDmMessage) {
                if (board.players.exists(p => p.uid == authorid)) {
                  if (args(1).toLowerCase.contains("yes") || args(1).toLowerCase.contains("no")) {
                    val vote: PreMissionVotePhase = if (args(1).toLowerCase.contains("yes")) {
                      Yes: PreMissionVotePhase
                    } else {
                      No: PreMissionVotePhase
                    }

                    val newVotes = PreMissionVotes(votes.votes.filter(_._2.uid != authorid)
                      ++ Seq((vote, board.players.find(x => x.uid == authorid).get)))

                    state = PreMissionVoting(board, newVotes)

                    dmChannel.createMessage("Vote accepted").block()

                    if (newVotes.votes.length == board.players.length) {
                      val newBoard = GameState.performStep(board, newVotes)

                      newBoard match {
                        case Left(e) => e
                        case Right((boardInner, response)) => {
                          val out = boardInner.gamePhase match {
                            case CompanionDelegation => {
                              state = CompanionSelection(boardInner)
                              s"\nThe king ${boardInner.king} may now select the players who will join his quest, by typing\n!avalon invite PLAYERNAME"+
                              s"\nThe required amount of players for this mission is ${MissionRequirements.getPlayersForMission(paricipating.size, boardInner.mission).people}" +
                              s"\nThe amount of failures required for the evil to win the round is ${MissionRequirements.getPlayersForMission(paricipating.size, boardInner.mission).failuresAllowed}"

                            }
                            case PreMissionVote => state = PreMissionVoting(boardInner, PreMissionVotes(Seq.empty))
                            case SuccessFailureVote => {
                              paricipating.values.filter(p => boardInner.playersOnMission.exists(x => x.uid == p.uid)).map{ p =>
                                p.dmChannel.createMessage("Please vote with !avalon vote Success/Failure").block()
                              }

                              state = SuccessFailureVoting(boardInner, PostMissionVotes(Seq.empty))

                              ""
                            }
                          }

                          response + out
                        }
                      }
                    } else {
                      ""
                    }
                  } else {
                    dmChannel.createMessage("Vote Yes/No please").block()

                    ""
                  }
                } else {
                  "You are not allowed to vote"
                }
              } else {
                "Only DM's, you monkey"
              }
            }
            case _ => ""
          }}
          case SuccessFailureVoting(board, votes) => { args.head match {
            case "vote" => {
              if (isDmMessage) {
                if (board.playersOnMission.exists(p => p.uid == authorid)) {
                  if (args(1).contains("Success") || args(1).contains("Failure")) {
                    val vote: PostMissionVote = if (args(1).contains("Success")) {
                      Success: PostMissionVote
                    } else {
                      Failure: PostMissionVote
                    }

                    val newVotes = PostMissionVotes(votes.votes.filter(_._2.uid != authorid)
                      ++ Seq((vote, board.players.find(x => x.uid == authorid).get)))

                    state = SuccessFailureVoting(board, newVotes)

                    dmChannel.createMessage("Vote accepted").block()

                    if (newVotes.votes.length == board.playersOnMission.length) {
                      val newBoard = GameState.performStep(board, newVotes)

                      newBoard match {
                        case Left(e) => e
                        case Right((boardInner, response)) => {
                          val extra = boardInner.gamePhase match {
                            case CompanionDelegation => {
                              state = CompanionSelection(boardInner)

                              s"\nThe king ${boardInner.king} may now select the players who will join his quest, by typing\n!avalon invite PLAYERNAME"+
                                s"\nThe required amount of players for this mission is ${MissionRequirements.getPlayersForMission(paricipating.size, boardInner.mission).people}" +
                                s"\nThe amount of failures required for the evil to win the round is ${MissionRequirements.getPlayersForMission(paricipating.size, boardInner.mission).failuresAllowed}"
                            }
                            case PreMissionVote => state = PreMissionVoting(boardInner, PreMissionVotes(Seq.empty))
                            case SuccessFailureVote => state = SuccessFailureVoting(boardInner, PostMissionVotes(Seq.empty))
                            case GameEnd => state = InitialState
                            case GoodWins => state = AssassinPickPhase(boardInner)
                          }

                          response + extra
                        }
                      }
                    } else {
                      ""
                    }
                  } else {
                    dmChannel.createMessage("Vote Success/Failure please").block()

                    ""
                  }
                } else {
                  "You are not allowed to vote"
                }
              } else {
                "Only DM's, you monkey"
              }
            }
            case _ => ""
          }}
          case AssassinPickPhase(board) => args.head match {
            case "vote" => {
              val assassin = board.players.find(_.role == Assassin).get

              if (authorid == assassin.uid) {
                board.players.find(_.playerName == args(1)) match {
                  case None => "Please select a player who is in the game"
                  case Some(pick) => {
                    val newBoard = GameState.performStep(board, AssassinPick(pick))

                    newBoard match {
                      case Left(e) => e
                      case Right((boardInner, response)) => {
                        boardInner.gamePhase match {
                          case _ => state = InitialState
                        }

                        response
                      }
                    }
                  }
                }
              } else {
                s"You are not the Assassin ${authorName}"
              }
            }
            case _ => ""
          }
        }
      } match {
        case scala.util.Success(s) => s
        case scala.util.Failure(e) => {
          e.printStackTrace()
          e.getMessage
        }
      }

      if (responsestate != "") {
        gameChannel.createMessage(responsestate).block()
      } else {
        msg
      }
    }.subscribe()

/*
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
  }*/

  client.login().block()

  println("Hello")
}

