# Secret Hitler XL Mod

This branch adds a homebrew "Secret Hitler XL" mod on top of the base game:
new factions, policy types, presidential powers, an Emergency Powers system,
and a number of UI/UX and correctness fixes discovered during live
playtesting. All mod content is opt-in per lobby (see **Lobby Settings**
below) and defaults to vanilla Secret Hitler when nothing is enabled.

## New factions

Configurable independently, with minimum player counts enforced by
`SecretHitlerGame.getMinPlayersForOptions()`:

- **Communists** (6+ players) — a third team with its own policy tracker,
  win condition, and presidential powers. Communists are initially known
  only to each other (see **Identity knowledge**).
- **Anarchist** (9+ players with Communists, otherwise a standalone variant)
  — wins outright if an Anarchist policy is forced through the election
  tracker. Enacting an Anarchist policy normally cascades: a new Anarchist
  policy is shuffled into the deck and the next policy is immediately forced
  through (see `SecretHitlerGame.onEnactPolicy()`), surfaced to clients as
  `anarchistCascadeForcedPolicy` so the UI can show both results.
- **Capitalist** — wins whenever the Liberals or Fascists do (by policy or
  execution), piggybacking on their victory.
- **Monarchist** (7+ players, 8+ with Communists) — fascist-aligned; wins
  alongside the Fascists specifically when Hitler is elected Chancellor
  (not on a pure policy-count Fascist win).

New `GameState` victory states cover every combination
(`COMMUNIST_VICTORY_POLICY`, `ANARCHIST_VICTORY_POLICY`,
`ANARCHIST_WITH_COMMUNIST_VICTORY_POLICY`,
`CAPITALIST_VICTORY_WITH_*`, `MONARCHIST_*_VICTORY_WITH_FASCIST_*`, etc.).

## New policy types

- **Anti-Fascist** / **Anti-Communist** — swap in for a Communist/Fascist
  card respectively; removes a policy from the *opposing* tracker when
  enacted and suppresses that tracker's power the next time it re-fills the
  vacated slot.
- **Social Democratic** — swaps in for a Liberal card; grants a presidential
  power letting the President remove a policy from either the Fascist or
  Communist tracker.
- **Article 48** / **Enabling Act** — see **Emergency Powers** below.
- **Longer Liberal Track** — an optional 6-slot Liberal tracker instead of
  the standard 5.

## Communist presidential powers

Resolved automatically by tracker slot (`Board.getActivatedPower()`), with
dedicated board variants for 6/7/8/9/10 players
(`SixPlayerBoardCommunist.java` … `TenPlayerBoardCommunist.java`):

- **Bugging** (Communist Investigation) — communists investigate a
  player's party membership.
- **Radicalization** — converts a non-Hitler, non-Capitalist player to
  Communist. Resolved by **unanimous vote** among all alive Communists
  (`castCommunistVote`/`retractCommunistVote`/`getUnanimousCommunistVoteTarget`),
  not a single communist unilaterally picking a target.
- **Five-Year Plan** — shuffles 2 Communist + 1 Liberal policy into the
  draw deck.
- **Congress** — the point at which Radicalized Communists learn who the
  *original* Communists are (see **Identity knowledge**).
- **Confession** — the sitting President must publicly declare a party
  they are **not** a member of. Nobody can ever deny being Liberal
  (everyone plays along as if they might be one); Fascist-aligned players
  (Fascist/Hitler/Monarchist/Capitalist) can only deny Communist,
  Communist-aligned players (Communist/Anarchist) can only deny Fascist,
  and only Liberals get a free choice (`isValidDeniedParty()`). The claim
  is public and persists on the player's portrait for the rest of the game.

## Emergency Powers (Article 48 / Enabling Act)

An optional 0–3 count of each card type, mixed into the deck
(`emergencyPowersIncluded`). Enacting one fills the next slot on a
dedicated 6-slot board and resolves a power automatically by slot number
(`EMERGENCY_POWER_SLOT_ORDER`) — there is no player choice of which power to
use:

1. **Propaganda** — secretly peek the top card, optionally discard it.
2. **Policy Peek** — same as the base game's power.
3. **Impeachment** — the other officer must reveal their party membership
   to a chosen player.
4. **Marked For Execution** — marks a player who is executed once **3
   Fascist policies have been enacted in total** (an absolute count, not
   relative to the moment of marking).
5. **Execution** — same as the base game's power.
6. A double-wide final slot, specific to card type: **Presidential Pardon**
   (Article 48 — clears a pending Marked-For-Execution) or **Vote of No
   Confidence** (Enabling Act — enacts whatever the President discarded
   this round instead).

A card forced through by the election tracker (or the Anarchist's cascade)
still visibly fills its board slot, but its power never resolves — matching
how every other forced policy behaves.

## Veto

Veto now unlocks under the same "one policy from winning" rule for **both**
trackers: the Fascist tracker at 5/6, and the Communist tracker at one
below its win threshold (4/5 for 6–7 players, 5/6 for 8+), via
`SecretHitlerGame.isVetoAvailable()`.

## Identity knowledge

Who learns which players' identities, and when, lives in
`GameToJSONConverter.convert()`:

- Fascists/Hitler/Monarchist know each other and Hitler from the start
  (Hitler only in games of 6 or fewer players) — standard base-game rule.
- **Original** Communists (dealt at setup) recognize each other, and any
  later Radicalization target, immediately. A Radicalization target does
  **not** recognize the original team back until **Congress** resolves —
  that's Congress's entire purpose. This is tracked via
  `Player.wasCommunist()`/`Player.getOriginalIdentity()`, fixed once at
  role assignment (`Player.lockInOriginalIdentity()`), independent of the
  player's current (possibly Radicalized) identity.
- A Radicalized player's **former Fascist teammates** keep recognizing them
  as Fascist forever (`Player.wasFascist()`), even after their identity
  changes — that knowledge is never silently revoked.
- The live Communist vote roster (`aliveCommunists`/`communistVotes`) is
  only sent while a Radicalization/Investigation vote is actively in
  progress, so a player who was just Radicalized by that vote doesn't
  retroactively learn the team's roster from it.

## UI/UX changes of note

- Dedicated board token art for every tracker slot type (Anarchist,
  Communist, Anti-Fascist, Anti-Communist, Social Democratic, Article 48,
  Enabling Act), sized and placed identically to the Liberal/Fascist
  convention (full card, not a cropped icon) — including a centered,
  widened token for the Emergency Powers board's shared 6th slot.
- "POLICY ENACTED" alerts now fire for every tracker and every power state
  (previously several Communist and Emergency Power states were missing
  from the gate and silently produced no alert).
- A persistent portrait banner (`#player-top-banner`) shows public,
  permanent per-player facts: confirmed-not-Hitler and the Confession
  power's result. Marked-For-Execution gets its own icon badge.
- Lobby settings (mod toggles) use the same `vmin`-scaled fonts as the rest
  of the app instead of unstyled browser defaults.
- A debug-only (`DEBUG_MODE`) HTTP endpoint, `GET /debug/deck?lobby=CODE`,
  exposes the true draw/discard pile order for local playtesting.

## Testing

`backend/src/test/java/game/testSecretHitlerGameXL.java` is a new,
extensive integration test suite driving full games through the public API
for every mechanic above — Communists, anti-policies, Social Democratic,
Emergency Powers, Radicalization/Congress identity knowledge, Confession,
veto availability, and more. Tests that depend on randomness retry with a
fresh game within an attempt budget rather than relying on a single
playthrough.
