import React from "react";
import "./LobbySettings.css";
import {
  GameOptions,
  SendWSCommand,
  WSCommandType,
  getMinPlayersForOptions,
} from "../types";

type LobbySettingsProps = {
  isVIP: boolean;
  gameOptions: GameOptions;
  numPlayers: number;
  sendWSCommand: SendWSCommand;
};

/**
 * Renders the Secret Hitler XL mod options for the lobby. Only the VIP can
 * change these; every player sees the currently configured options.
 */
function LobbySettings(props: LobbySettingsProps) {
  const { isVIP, gameOptions, numPlayers, sendWSCommand } = props;

  function update(partial: Partial<GameOptions>) {
    sendWSCommand({
      command: WSCommandType.SET_GAME_OPTIONS,
      gameOptions: { ...gameOptions, ...partial },
    });
  }

  const minPlayers = getMinPlayersForOptions(gameOptions);

  return (
    <div id={"lobby-settings-container"}>
      <h3 id={"lobby-settings-header"}>Secret Hitler XL Options</h3>
      {!isVIP && (
        <p id={"lobby-settings-vip-text"}>
          Only the VIP can change these options.
        </p>
      )}

      <label className={"lobby-settings-option"}>
        <input
          type={"checkbox"}
          checked={gameOptions.communistsIncluded}
          disabled={!isVIP}
          onChange={(e) => {
            const communistsIncluded = e.target.checked;
            update({
              communistsIncluded,
              // The Anti-Fascist policy requires Communists to be included,
              // so clear it if Communists are being turned off.
              antiFascistPolicyIncluded:
                communistsIncluded && gameOptions.antiFascistPolicyIncluded,
            });
          }}
        />
        Communists (6+ players)
      </label>

      <label className={"lobby-settings-option"}>
        <input
          type={"checkbox"}
          checked={gameOptions.anarchistIncluded}
          disabled={!isVIP}
          onChange={(e) => update({ anarchistIncluded: e.target.checked })}
        />
        Anarchist
        {gameOptions.communistsIncluded ? " (9+ players with Communists)" : ""}
      </label>

      <label className={"lobby-settings-option"}>
        <input
          type={"checkbox"}
          checked={gameOptions.capitalistIncluded}
          disabled={!isVIP}
          onChange={(e) => update({ capitalistIncluded: e.target.checked })}
        />
        Capitalist
      </label>

      <label className={"lobby-settings-option"}>
        <input
          type={"checkbox"}
          checked={gameOptions.monarchistIncluded}
          disabled={!isVIP}
          onChange={(e) => update({ monarchistIncluded: e.target.checked })}
        />
        Monarchist ({gameOptions.communistsIncluded ? "8+" : "7+"} players)
      </label>

      <label className={"lobby-settings-option"}>
        <input
          type={"checkbox"}
          checked={gameOptions.antiFascistPolicyIncluded}
          disabled={!isVIP || !gameOptions.communistsIncluded}
          onChange={(e) =>
            update({ antiFascistPolicyIncluded: e.target.checked })
          }
        />
        Anti-Fascist policy (requires Communists)
      </label>

      <label className={"lobby-settings-option"}>
        <input
          type={"checkbox"}
          checked={gameOptions.antiCommunistPolicyIncluded}
          disabled={!isVIP}
          onChange={(e) =>
            update({ antiCommunistPolicyIncluded: e.target.checked })
          }
        />
        Anti-Communist policy
      </label>

      <label className={"lobby-settings-option"}>
        <input
          type={"checkbox"}
          checked={gameOptions.socialDemocraticPolicyIncluded}
          disabled={!isVIP}
          onChange={(e) =>
            update({ socialDemocraticPolicyIncluded: e.target.checked })
          }
        />
        Social Democratic policy
      </label>

      <label className={"lobby-settings-option"}>
        <input
          type={"checkbox"}
          checked={gameOptions.longerLiberalTrackIncluded}
          disabled={!isVIP}
          onChange={(e) =>
            update({ longerLiberalTrackIncluded: e.target.checked })
          }
        />
        Longer liberal track
      </label>

      <label className={"lobby-settings-option"}>
        Emergency powers:{" "}
        <select
          value={gameOptions.emergencyPowersIncluded}
          disabled={!isVIP}
          onChange={(e) =>
            update({ emergencyPowersIncluded: parseInt(e.target.value, 10) })
          }
        >
          <option value={0}>None</option>
          <option value={1}>1</option>
          <option value={2}>2</option>
          <option value={3}>3</option>
        </select>
      </label>

      {numPlayers < minPlayers && (
        <p id={"lobby-settings-warning-text"}>
          These options need at least {minPlayers} players (currently{" "}
          {numPlayers}).
        </p>
      )}
    </div>
  );
}

export default LobbySettings;
