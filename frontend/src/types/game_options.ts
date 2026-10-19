import {
  MIN_PLAYERS,
  MIN_PLAYERS_WITH_COMMUNIST,
  MIN_PLAYERS_WITH_COMMUNIST_AND_ANARCHIST,
  MIN_PLAYERS_WITH_COMMUNIST_AND_MONARCHIST,
  MIN_PLAYERS_WITH_MONARCHIST,
} from "../constants";

/** Mirrors the Secret Hitler XL mod settings configurable from the lobby. */
export type GameOptions = {
  communistsIncluded: boolean;
  anarchistIncluded: boolean;
  capitalistIncluded: boolean;
  monarchistIncluded: boolean;
  /** Requires communistsIncluded - swaps in for a Communist policy. */
  antiFascistPolicyIncluded: boolean;
  /** Swaps in for a Fascist policy. */
  antiCommunistPolicyIncluded: boolean;
  socialDemocraticPolicyIncluded: boolean;
  longerLiberalTrackIncluded: boolean;
  /** Number of Article 48 / Enabling Act emergency power pairs to include (0-3). */
  emergencyPowersIncluded: number;
};

export const DEFAULT_GAME_OPTIONS: GameOptions = {
  communistsIncluded: false,
  anarchistIncluded: false,
  capitalistIncluded: false,
  monarchistIncluded: false,
  antiFascistPolicyIncluded: false,
  antiCommunistPolicyIncluded: false,
  socialDemocraticPolicyIncluded: false,
  longerLiberalTrackIncluded: false,
  emergencyPowersIncluded: 0,
};

/**
 * Returns the minimum number of players required to start a game with the
 * given options. Mirrors the validation in SecretHitlerGame.java.
 */
export function getMinPlayersForOptions(options: GameOptions): number {
  let min = MIN_PLAYERS;
  if (options.communistsIncluded) {
    min = Math.max(min, MIN_PLAYERS_WITH_COMMUNIST);
  }
  if (options.communistsIncluded && options.anarchistIncluded) {
    min = Math.max(min, MIN_PLAYERS_WITH_COMMUNIST_AND_ANARCHIST);
  }
  if (options.monarchistIncluded) {
    min = Math.max(
      min,
      options.communistsIncluded
        ? MIN_PLAYERS_WITH_COMMUNIST_AND_MONARCHIST
        : MIN_PLAYERS_WITH_MONARCHIST
    );
  }
  return min;
}
