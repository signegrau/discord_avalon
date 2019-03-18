defmodule GameRules do
  @moduledoc false
  defmodule Mission do
    defstruct people: 0, failuresNeeded: 1
  end

  # Mission starts at 0
  def getPlayersForMission(playerCount, mission) do
    getMissions(playerCount)|> elem(mission)
  end

  def getMissions(playerCount) do
    allMissions = %{
      5 => {%Mission{people: 2},%Mission{people: 3},%Mission{people: 2},%Mission{people: 3},%Mission{people: 3}},
      6 => {%Mission{people: 2},%Mission{people: 3},%Mission{people: 4},%Mission{people: 3},%Mission{people: 4}},
      7 => {%Mission{people: 2},%Mission{people: 3},%Mission{people: 3},%Mission{people: 4, failuresNeeded: 2},%Mission{people: 4}},
      8 => {%Mission{people: 3},%Mission{people: 4},%Mission{people: 4},%Mission{people: 5, failuresNeeded: 2},%Mission{people: 5}},
      9 => {%Mission{people: 3},%Mission{people: 4},%Mission{people: 4},%Mission{people: 5, failuresNeeded: 2},%Mission{people: 5}},
      10 => {%Mission{people: 3},%Mission{people: 4},%Mission{people: 4},%Mission{people: 5, failuresNeeded: 2},%Mission{people: 5}},
    }

    Map.fetch(allMissions, playerCount)
  end
end
