import React, { ReactElement } from "react";
import PlayerDisplay from "../player/PlayerDisplay";
import { GameState, SendWSCommand, WSCommandType } from "../types";
import "./CommunistVotePanel.css";

type CommunistVotePanelProps = {
  user: string;
  gameState: GameState;
  sendWSCommand: SendWSCommand;
  label: string;
  headerText: string;
};

/**
 * An always-visible (not modal) panel shown to every alive Communist while
 * the Radicalization or Communist Investigation power is active. Everyone
 * on the team votes for a target by clicking their portrait; votes can be
 * changed freely, and everyone sees everyone else's current vote live.
 * Once every alive Communist's vote points at the same player, the server
 * resolves the power automatically - there's no separate "confirm" step.
 */
export default function CommunistVotePanel(
  props: CommunistVotePanelProps
): ReactElement {
  const { user, gameState, sendWSCommand, label, headerText } = props;
  const aliveCommunists = gameState.aliveCommunists ?? [];
  const votes = gameState.communistVotes ?? {};
  const myVote = votes[user];

  const disableDeadAndCommunists = (name: string, gs: GameState): string => {
    if (!gs.players[name].alive) {
      return "EXECUTED";
    }
    if (aliveCommunists.includes(name)) {
      return "COMMUNIST";
    }
    return "";
  };

  return (
    <div id="communist-vote-panel">
      <h3 id="communist-vote-label">{label}</h3>
      <p id="communist-vote-header">{headerText}</p>
      <PlayerDisplay
        gameState={gameState}
        user={user}
        includeUser={false}
        useAsButtons={true}
        playerDisabledFilter={disableDeadAndCommunists}
        showLabels={false}
        selection={myVote}
        onSelection={(target: string) =>
          sendWSCommand({
            command: WSCommandType.CAST_COMMUNIST_VOTE,
            target,
          })
        }
      />
      <div id="communist-vote-tally">
        {aliveCommunists.map((communistName) => (
          <p id="communist-vote-tally-row" key={communistName}>
            <span id="communist-vote-tally-name">
              {communistName === user ? "You" : communistName}
            </span>
            {": "}
            {votes[communistName] ? votes[communistName] : "no vote yet"}
          </p>
        ))}
      </div>
      <button
        id="communist-vote-retract-button"
        disabled={myVote === undefined}
        onClick={() =>
          sendWSCommand({ command: WSCommandType.RETRACT_COMMUNIST_VOTE })
        }
      >
        RETRACT MY VOTE
      </button>
    </div>
  );
}
