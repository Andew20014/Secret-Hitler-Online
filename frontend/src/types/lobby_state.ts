import { PolicyType, Role } from "./game";

export enum LobbyState {
  SETUP = "SETUP",
  CHANCELLOR_NOMINATION = "CHANCELLOR_NOMINATION",
  CHANCELLOR_VOTING = "CHANCELLOR_VOTING", // Voting on the chancellor is taking place.
  LEGISLATIVE_PRESIDENT = "LEGISLATIVE_PRESIDENT", // In the legislative phase. The president is selecting a card to discard.
  LEGISLATIVE_CHANCELLOR = "LEGISLATIVE_CHANCELLOR", // In the legislative phase. The chancellor is selecting a card to enact.
  LEGISLATIVE_PRESIDENT_VETO = "LEGISLATIVE_PRESIDENT_VETO", // Chancellor decided to initiate veto, President chooses whether to allow.
  PP_PEEK = "PRESIDENTIAL_POWER_PEEK", // President may peek at the next three cards in the deck
  PP_INVESTIGATE = "PRESIDENTIAL_POWER_INVESTIGATE", // President can investigate a party membership
  PP_EXECUTION = "PRESIDENTIAL_POWER_EXECUTION", // President may choose a player to execute
  PP_ELECTION = "PRESIDENTIAL_POWER_ELECTION", // President chooses the next president, seat continues as normal after.
  // Secret Hitler XL mod presidential/emergency powers.
  PP_COMMUNISTS_INVESTIGATION = "PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION", // All the communists may investigate a party membership.
  PP_CONFESSION = "PRESIDENTIAL_POWER_CONFESSION", // President must reveal their own party membership to everyone.
  PP_FIVE_YEAR_PLAN = "PRESIDENTIAL_POWER_FIVE_YEAR_PLAN", // Adds communist/liberal policies to the draw deck.
  PP_CONGRESS = "PRESIDENTIAL_POWER_CONGRESS", // Newly radicalized communists learn who the original communists are.
  PP_RADICALIZATION = "PRESIDENTIAL_POWER_RADICALIZATION", // A communist turns another player into a communist.
  PP_SOCIAL_DEMOCRATIC = "PRESIDENTIAL_POWER_SOCIAL_DEMOCRATIC", // President removes a fascist or communist policy from its tracker.
  PP_PROPAGANDA = "PRESIDENTIAL_POWER_PROPAGANDA", // The acting player secretly looks at the top card and may discard it - the emergency power board's 1st slot.
  PP_IMPEACHMENT = "PRESIDENTIAL_POWER_IMPEACHMENT", // The other officer must reveal their party membership to a chosen player.
  PP_MARK_FOR_EXECUTION = "PRESIDENTIAL_POWER_MARK_FOR_EXECUTION", // The acting player marks a player for execution.
  POST_LEGISLATIVE = "POST_LEGISLATIVE", // Waiting for the President to end their turn.
  LIBERAL_VICTORY_POLICY = "LIBERAL_VICTORY_POLICY", // Liberal Party won through enacting Liberal policies.
  LIBERAL_VICTORY_EXECUTION = "LIBERAL_VICTORY_EXECUTION", // Liberal Party won through executing Hitler.
  FASCIST_VICTORY_POLICY = "FASCIST_VICTORY_POLICY", // Fascist Party won through enacting Fascist policies.
  FASCIST_VICTORY_ELECTION = "FASCIST_VICTORY_ELECTION", // Fascist Party won by successfully electing Hitler chancellor.
  // Secret Hitler XL mod victory states.
  COMMUNIST_VICTORY_POLICY = "COMMUNIST_VICTORY_POLICY",
  COMMUNIST_WITH_LIBERAL_VICTORY_EXECUTION = "COMMUNIST_WITH_LIBERAL_VICTORY_EXECUTION",
  ANARCHIST_VICTORY_POLICY = "ANARCHIST_VICTORY_POLICY",
  ANARCHIST_WITH_COMMUNIST_VICTORY_POLICY = "ANARCHIST_WITH_COMMUNIST_VICTORY_POLICY",
  CAPITALIST_VICTORY_WITH_LIBERAL_POLICY = "CAPITALIST_VICTORY_WITH_LIBERAL_POLICY",
  CAPITALIST_VICTORY_WITH_LIBERAL_EXECUTION = "CAPITALIST_VICTORY_WITH_LIBERAL_EXECUTION",
  CAPITALIST_VICTORY_WITH_FASCIST_POLICY = "CAPITALIST_VICTORY_WITH_FASCIST_POLICY",
  CAPITALIST_VICTORY_WITH_FASCIST_ELECTION = "CAPITALIST_VICTORY_WITH_FASCIST_ELECTION",
  MONARCHIST_VICTORY_WITH_FASCIST_ELECTION = "MONARCHIST_VICTORY_WITH_FASCIST_ELECTION",
  MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_ELECTION = "MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_ELECTION",
  MONARCHIST_VICTORY_WITH_FASCIST_POLICY = "MONARCHIST_VICTORY_WITH_FASCIST_POLICY",
  MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_POLICY = "MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_POLICY",
}

export type PlayerState = {
  id?: Role;
  alive: boolean;
  investigated: boolean;
  /** True once this player has survived a Chancellorship that would have won
   * the game for the Fascists had they been Hitler - publicly known, since
   * anyone can deduce it from the fascist policy count and election result. */
  confirmedNotHitler: boolean;
};

export type GameState = {
  state: LobbyState;
  lastState: LobbyState;
  playerOrder: string[];
  players: Record<string, PlayerState>;
  chancellor: string;
  president: string;
  lastChancellor: string;
  lastPresident: string;
  electionTracker: number;
  electionTrackerAdvanced: boolean;
  userVotes: Record<string, boolean>;
  liberalPolicies: number;
  fascistPolicies: number;
  /** Number of communist (and anarchist) policies enacted; only meaningful when communistsIncluded. */
  communistPolicies: number;
  /** Number of anarchist policies enacted, on its own (not merged with communist policies). */
  anarchistPolicies: number;

  // The specific PolicyType enacted into each tracker slot, in order (e.g.
  // an anti-communist policy shows up in fascistTrackerHistory alongside
  // plain fascist policies), so the board can render the exact card art
  // per slot instead of a generic per-tracker icon.
  liberalTrackerHistory: PolicyType[];
  fascistTrackerHistory: PolicyType[];
  communistTrackerHistory: PolicyType[];
  /** The Article 48/Enabling Act policy type that filled each of the
   * printed Emergency Powers board's 6 slots, in left-to-right order.
   * There is no player choice - the slot number alone determines which
   * power automatically applies (see the backend's EMERGENCY_POWER_SLOT_ORDER). */
  emergencyPowerTrackerHistory: PolicyType[];

  drawSize: number;
  discardSize: number;

  // How many policies of each type are still left to be enacted (currently
  // in the draw or discard pile). Computed directly by the server rather
  // than a fixed starting total, since the Anarchist's cascading mechanic
  // can grow its remaining pool over the course of the game.
  remainingFascistPolicies: number;
  remainingLiberalPolicies: number;
  remainingCommunistPolicies: number;
  remainingAnarchistPolicies: number;
  remainingAntiFascistPolicies: number;
  remainingAntiCommunistPolicies: number;
  remainingSocialDemocraticPolicies: number;
  remainingArticle48Policies: number;
  remainingEnablingActPolicies: number;

  /** How many players (alive or dead) hold each identity. Publicly known,
   * like the base game's role breakdown - only who holds which role is
   * secret. Keyed by Role (e.g. "LIBERAL", "CAPITALIST"). */
  roleCounts: Record<string, number>;

  /** Live tally of each communist's current vote (voter username -> target
   * username) for who to Radicalize/Investigate. Only sent to communist
   * viewers, and only while that power is active. */
  communistVotes?: Record<string, string>;
  /** Every alive communist's username, for rendering the vote UI (who
   * still needs to vote, who's excluded as a target). Only sent to
   * communist viewers. Not the same as each player's "id" field, which has
   * its own separate (Congress-gated) visibility rules. */
  aliveCommunists?: string[];

  // Secret Hitler XL mod options in effect for this game.
  communistsIncluded: boolean;
  anarchistIncluded: boolean;
  capitalistIncluded: boolean;
  monarchistIncluded: boolean;
  antiFascistPolicyIncluded: boolean;
  antiCommunistPolicyIncluded: boolean;
  socialDemocraticPolicyIncluded: boolean;
  longerLiberalTrackIncluded: boolean;
  emergencyPowersIncluded: number;
  /** The username marked for execution by an emergency power, if any. */
  markedForExecution?: string;
  /**
   * The officer (President for Article 48, Chancellor for the Enabling Act)
   * currently authorized to act while an emergency power is being resolved.
   * Undefined outside of that flow.
   */
  emergencyPowerActingOfficer?: string;
  /** The top policy of the draw deck, only present during the Propaganda emergency power. */
  propagandaPeek?: PolicyType;
  /**
   * The username of whoever has used the Communist "Confession" power, and
   * the party (FASCIST or COMMUNIST - never LIBERAL) they publicly denied
   * being a member of. Public, and persists for the rest of the game once
   * set. Both undefined until Confession is used.
   */
  confessionUsername?: string;
  confessionDeniedParty?: PolicyType.FASCIST | PolicyType.COMMUNIST;
  // TODO: Make GameState type more complex, correlating
  // these fields with certain LobbyStates.
  // This actually is a little more complicated, since GameState currently represents the
  // the full packet data sent by the server. See `Lobby.java` `updateUser()`
  // for how all of this is packaged.
  presidentChoices?: PolicyType[];
  chancellorChoices?: PolicyType[];
  targetUser?: string;
  lastPolicy: string;
  /**
   * The policy type forced through by the Anarchist's cascade mechanic,
   * present only for the single update right after a normal (non-forced)
   * Anarchist policy enactment triggers it.
   */
  anarchistCascadeForcedPolicy?: PolicyType;
  vetoOccurred: boolean;
  peek?: PolicyType[];

  usernames?: string[];
  /** Maps from usernames to icon keys */
  icon: Record<string, string>;
};
