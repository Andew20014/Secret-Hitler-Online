import { GameOptions } from "./game_options";
import { PolicyType } from "./game";

// TODO: Change these all to camelCase
export const enum WSCommandType {
  PING = "ping",
  START_GAME = "start-game",
  GET_STATE = "get-state",
  SET_GAME_OPTIONS = "set-game-options",
  REGISTER_CHANCELLOR_VETO = "chancellor-veto",
  REGISTER_PRESIDENT_VETO = "president-veto",
  REGISTER_PEEK = "register-peek",
  END_TERM = "end-term",
  // Select an icon
  SELECT_ICON = "select-icon",
  // Select a player
  NOMINATE_CHANCELLOR = "nominate-chancellor",
  REGISTER_EXECUTION = "register-execution",
  REGISTER_SPECIAL_ELECTION = "register-special-election",
  GET_INVESTIGATION = "get-investigation",
  // Voting action
  REGISTER_VOTE = "register-vote",
  // Policy action
  REGISTER_CHANCELLOR_CHOICE = "register-chancellor-choice",
  REGISTER_PRESIDENT_CHOICE = "register-president-choice",

  // Secret Hitler XL mod commands.
  // The Communist Investigation and Radicalization powers are resolved by
  // unanimous vote among the alive communists, rather than a single
  // communist unilaterally picking a target.
  CAST_COMMUNIST_VOTE = "cast-communist-vote",
  RETRACT_COMMUNIST_VOTE = "retract-communist-vote",
  REGISTER_CONFESSION = "register-confession",
  REGISTER_FIVE_YEAR_PLAN = "register-five-year-plan",
  END_CONGRESS = "end-congress",
  REGISTER_SOCIAL_DEMOCRATIC = "register-social-democratic",
  REGISTER_PROPAGANDA_DISCARD = "register-propaganda-discard",
  REGISTER_IMPEACHMENT = "register-impeachment",
  REGISTER_MARK_FOR_EXECUTION = "register-mark-for-execution",
}

/** All possible commands and associated parameters. */
export type ServerRequestPayload =
  | { command: WSCommandType.PING }
  | { command: WSCommandType.START_GAME }
  | { command: WSCommandType.GET_STATE }
  | { command: WSCommandType.SET_GAME_OPTIONS; gameOptions: GameOptions }
  | { command: WSCommandType.REGISTER_CHANCELLOR_VETO }
  | { command: WSCommandType.REGISTER_PRESIDENT_VETO; veto: boolean }
  | { command: WSCommandType.REGISTER_PEEK }
  | { command: WSCommandType.END_TERM }
  | { command: WSCommandType.SELECT_ICON; icon: string }
  | { command: WSCommandType.NOMINATE_CHANCELLOR; target: string }
  | { command: WSCommandType.REGISTER_EXECUTION; target: string }
  | { command: WSCommandType.REGISTER_SPECIAL_ELECTION; target: string }
  | { command: WSCommandType.GET_INVESTIGATION; target: string }
  | { command: WSCommandType.REGISTER_VOTE; vote: boolean }
  | { command: WSCommandType.REGISTER_CHANCELLOR_CHOICE; choice: number }
  | { command: WSCommandType.REGISTER_PRESIDENT_CHOICE; choice: number }
  | { command: WSCommandType.CAST_COMMUNIST_VOTE; target: string }
  | { command: WSCommandType.RETRACT_COMMUNIST_VOTE }
  | {
      command: WSCommandType.REGISTER_CONFESSION;
      deniedParty: PolicyType.FASCIST | PolicyType.COMMUNIST;
    }
  | { command: WSCommandType.REGISTER_FIVE_YEAR_PLAN }
  | { command: WSCommandType.END_CONGRESS }
  | {
      command: WSCommandType.REGISTER_SOCIAL_DEMOCRATIC;
      trackerType: PolicyType.FASCIST | PolicyType.COMMUNIST;
    }
  | { command: WSCommandType.REGISTER_PROPAGANDA_DISCARD; discard: boolean }
  | { command: WSCommandType.REGISTER_IMPEACHMENT; target: string }
  | { command: WSCommandType.REGISTER_MARK_FOR_EXECUTION; target: string };

export type SendWSCommand = (payload: ServerRequestPayload) => void;

/**
 * A WebSocket command to send to the server.
 * @param {WSCommandType} command The command type.
 * @param {string} lobby The lobby to send the command to.
 * @param {string} name The name of the player sending the command.
 *
 * Optional, depending on command:
 * @param {number} icon The icon to select.
 * @param {string} target The target of the command. Used for powers and nominations.
 * @param {boolean} vote The vote to register.
 * @param {number} choice The policy index to register.
 */
export type WSCommand = {
  name: string;
  lobby: string;
} & ServerRequestPayload;
