defmodule GameStates do
  @moduledoc false
  phases = %{
    :missionCompanionDeligation => 0,
    :preMissionVote => 1,
    :inMissionVote => 2
  }

  defmodule MissionState do
    defstruct playersOnMission: [], phase: Map.fetch(phases, 0), messageBack: nil
  end

  defmodule Board do
    defstruct players: [], failCount: 0, mission: 0, missionState: %MissionState{}, failedMissions: 0, currentKing: 0
  end

  def initGameState(playerCount) do
    %Board{}
  end

  @spec gameStep(board) :: {board, Board}
  def gameStep(board, listOfInfo) do
    #Check phase
    case board.missionState.phase do
      phases.missionCompanionDeligation ->
        #List is deligation
        Map.put(board, :missionState, %MissionState{phase: phases.preMissionVote, playersOnMission: listOfInfo})
      phases.preMissionVote ->
        goVotes = Enum.count(listOfInfo, fn vote -> vote == "Y" end)
        if goVotes > Kernel.length(board.players) do
          Map.put(board, :failCount, board.failCount + 1)
          Map.put(board, :missionState, %MissionState{messageBack: "Failed to get enough votes!"})
        else
          Map.put(board, :failCount, 0)
          Map.put(board, :missionState, %MissionState{phase: phases.inMissionVote, playersOnMission: board.missionState.playersOnMission})
        end
        #Need to show who voted what
        Map.put(board, :currentKing, rem(board.currentKing + 1, Kernel.length(board.players)))
      phases.inMissionVote ->
        if Enum.member?(listOfInfo, "F") do
          Map.put(board, :missionState, %MissionState{messageBack: "Someone voted fail!"})
          Map.put(board, :failedMissions, board.failedMissions + 1)
        else
          Map.put(board, :missionState, %MissionState{})
        end

        Map.put(board, :mission, board.mission + 1)
      _ -> board
    end
  end

  #Do win condition
end
