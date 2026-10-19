package game.datastructures.board;

import game.datastructures.Policy;

public class EightPlayerBoardCommunist extends Board{

    @Override
    public PresidentialPower getActivatedPower() {
        if (getLastEnactedType() == Policy.Type.FASCIST || getLastEnactedType() == Policy.Type.ANTI_COMMUNIST) {
            switch (getNumFascistPolicies()) {
                case 2:
                    return PresidentialPower.INVESTIGATE;
                case 3:
                    return PresidentialPower.ELECTION;
                case 4:
                case 5:
                    return PresidentialPower.EXECUTION;
            }
        } else if (getLastEnactedType() == Policy.Type.COMMUNIST || getLastEnactedType() == Policy.Type.ANTI_FASCIST || getLastEnactedType() == Policy.Type.ANARCHIST) {
            switch (getNumCommunistPolicies()) {
                case 1:
                    return PresidentialPower.COMMUNISTS_INVESTIGATION;
                case 2:
                    return PresidentialPower.RADICALIZATION;
                case 3:
                    return PresidentialPower.FIVE_YEAR_PLAN;
                case 4:
                    return PresidentialPower.CONGRESS;
                case 5:
                    return PresidentialPower.CONFESSION;
            }
        }
        return PresidentialPower.NONE;
    }

}
