package game;

public enum GameState {
    SETUP,                                                      // Game is being set up.
    CHANCELLOR_NOMINATION,                                      // President is nominating a chancellor.
    CHANCELLOR_VOTING,                                          // Voting on the chancellor is taking place.
    LEGISLATIVE_PRESIDENT,                                      // In the legislative phase. The president is selecting a card to discard.
    LEGISLATIVE_CHANCELLOR,                                     // In the legislative phase. The chancellor is selecting a card to enact.
    LEGISLATIVE_PRESIDENT_VETO,                                 // Chancellor decided to initiate veto, President chooses whether to allow.
    PRESIDENTIAL_POWER_PEEK,                                    // President may peek at the next three cards in the deck
    PRESIDENTIAL_POWER_INVESTIGATE,                             // President can investigate a party membership
    PRESIDENTIAL_POWER_EXECUTION,                               // President may choose a player to execute
    PRESIDENTIAL_POWER_ELECTION,                                // President chooses the next president, seat continues as normal after.
    PRESIDENTIAL_POWER_COMMUNISTS_INVESTIGATION,                // All the communists may investigate a party membership
    PRESIDENTIAL_POWER_CONFESSION,                              // President must reveal which party they are not a member of to everyone.
    PRESIDENTIAL_POWER_FIVE_YEAR_PLAN,                          // Add two Communist policies and one Liberal policy to the draw deck. Then shuffle the draw deck
    PRESIDENTIAL_POWER_CONGRESS,                                // The newly radicalised Communists Learn who the original Communists are.
    PRESIDENTIAL_POWER_RADICALIZATION,                          // The communists turn another player into a communist by switching their party membership card with a communist one.
    PRESIDENTIAL_POWER_SOCIAL_DEMOCRATIC,                       // The President removes a fascist or communist policy from its tracker.
    PRESIDENTIAL_POWER_PROPAGANDA,                              // The acting player secretly looks at the top card and may discard it.
    PRESIDENTIAL_POWER_IMPEACHMENT,                             // The other officer must reveal their party membership to a chosen player.
    PRESIDENTIAL_POWER_MARK_FOR_EXECUTION,                      // The acting player marks a player to be executed once 3 fascist policies have been enacted in total.
    POST_LEGISLATIVE,                                           // Waiting for the President to end their turn.
    LIBERAL_VICTORY_POLICY,                                     // Liberal Party won through enacting Liberal policies.
    LIBERAL_VICTORY_EXECUTION,                                  // Liberal Party won through executing Hitler.
    FASCIST_VICTORY_POLICY,                                     // Fascist Party won through enacting Fascist policies.
    FASCIST_VICTORY_ELECTION,                                   // Fascist Party won by successfully electing Hitler chancellor.
    COMMUNIST_VICTORY_POLICY,                                   // Communist Party won through enacting Communist policies.
    COMMUNIST_WITH_LIBERAL_VICTORY_EXECUTION,                   // Communist Party won through executing Hitler.
    ANARCHIST_VICTORY_POLICY,                                   // Anarchist won through enacting Anarchist policies.
    ANARCHIST_WITH_COMMUNIST_VICTORY_POLICY,                    // Anarchist won through enacting 2 Anarchist policies with Communists in the game.
    CAPITALIST_VICTORY_WITH_LIBERAL_POLICY,                     // Capitalist won through enacting Liberal policies.
    CAPITALIST_VICTORY_WITH_LIBERAL_EXECUTION,                  // Capitalist won through executing Hitler.
    CAPITALIST_VICTORY_WITH_FASCIST_POLICY,                     // Capitalist won through enacting Fascist policies.
    CAPITALIST_VICTORY_WITH_FASCIST_ELECTION,                   // Capitalist won by successfully electing Hitler chancellor.
    MONARCHIST_VICTORY_WITH_FASCIST_ELECTION,                   // Fascists won by electing Hitler chancellor; the Monarchist's plot failed, so only the Fascists win.
    MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_ELECTION,    // Fascists won by electing Hitler chancellor; the Capitalist wins too, but the Monarchist's plot failed.
    MONARCHIST_VICTORY_WITH_FASCIST_POLICY,                     // Fascists won by policy and Hitler never became chancellor: the Monarchist wins alongside the Fascists.
    MONARCHIST_AND_CAPITALIST_VICTORY_WITH_FASCIST_POLICY       // Fascists won by policy and Hitler never became chancellor: the Monarchist and Capitalist both win.
}
