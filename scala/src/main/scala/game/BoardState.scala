package game

import discord4j.core.`object`.entity.PrivateChannel

trait Phase
case object CompanionDelegation extends Phase
case object PreMissionVote extends Phase
case object SuccessFailureVote extends Phase
case object GoodWins extends Phase
case object GameEnd extends Phase

case class Player(
                 uid: String,
                 playerName: String,
                 dmChannel: PrivateChannel,
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
