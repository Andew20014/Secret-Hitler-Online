import React, { Component } from "react";
import RoleHitler from "../assets/role-hitler.png";
import RoleLiberal1 from "../assets/role-liberal-1.png";
import RoleLiberal2 from "../assets/role-liberal-2.png";
import RoleLiberal3 from "../assets/role-liberal-3.png";
import RoleLiberal4 from "../assets/role-liberal-4.png";
import RoleLiberal5 from "../assets/role-liberal-5.png";
import RoleLiberal6 from "../assets/role-liberal-6.png";
import RoleLiberal7 from "../assets/role-liberal-7.png";
import RoleFascist1 from "../assets/role-fascist-1.png";
import RoleFascist2 from "../assets/role-fascist-2.png";
import RoleFascist3 from "../assets/role-fascist-3.png";
import RoleFascist4 from "../assets/role-fascist-4.png";
import RoleAnarchist1 from "../assets/role-anarchist-1.png";
import RoleCommunist1 from "../assets/role-communist-1.png";
import RoleCommunist2 from "../assets/role-communist-2.png";
import RoleCommunist3 from "../assets/role-communist-3.png";
import RoleCommunist4 from "../assets/role-communist-4.png";
import RoleCapitalist from "../assets/role-capitalist.png";
import RoleMonarchist from "../assets/role-monarchist.png";

import "./RoleAlert.css";
import { GameState, Role } from "../types";

const LiberalImages = [
  RoleLiberal1,
  RoleLiberal2,
  RoleLiberal3,
  RoleLiberal4,
  RoleLiberal5,
  RoleLiberal6,
  RoleLiberal7,
];
const LiberalImagesAltText = [
  "Your secret role is LIBERAL. The card shows a bespectacled man with a pipe giving a side-eye.",
  "Your secret role is LIBERAL. The card shows an elegant woman with curly hair and pearls.",
  "Your secret role is LIBERAL. The card shows a round-chinned man in a pilgrim-esque hat gazing quizzically at the camera.",
  "Your secret role is LIBERAL. The card shows a sharp-suited man sporting a fedora and a neatly-trimmed mustache.",
  "Your secret role is LIBERAL. The card shows an elderly woman with comically large glasses holding a chihuahua.",
  "Your secret role is LIBERAL. The card shows a woman with a large sun hat and shoulder-length bob smirking.",
  "Your secret role is LIBERAL. The card shows a mustached man with round glasses and side-swept hair, smoking a pipe.",
];
const HitlerImages = [RoleHitler];
const HitlerImagesAltText = [
  "Your secret role is HITLER. The card shows a crocodile in a suit and WW2 German military hat glaring at the camera.",
];
const FascistImages = [RoleFascist1, RoleFascist2, RoleFascist3, RoleFascist4];
const FascistImagesAltText = [
  "Your secret role is FASCIST. The card shows a snake emerging from a suit covered in military medals.",
  "Your secret role is FASCIST. The card shows an iguana in a German military hat and suit with fangs bared.",
  "Your secret role is FASCIST. The card shows an iguana in a German military hat and suit with fangs bared.",
  "Your secret role is FASCIST. The card shows an iguana facing forward in a peaked cap with bared fangs and lightning-bolt collar tabs.",
];
const AnarchistImages = [RoleAnarchist1];
const AnarchistImagesAltText = [
  "Your secret role is ANARCHIST. The card shows a black cat with red eyes wearing a white shirt and suspenders.",
];
const CommunistImages = [
  RoleCommunist1,
  RoleCommunist2,
  RoleCommunist3,
  RoleCommunist4,
];
const CommunistImagesAltText = [
  "Your secret role is COMMUNIST. The card shows a stern brown bear wearing an ushanka hat with a red star.",
  "Your secret role is COMMUNIST. The card shows a dark brown bear with round glasses, wearing a maroon suit and red tie.",
  "Your secret role is COMMUNIST. The card shows a panda wearing an ushanka hat with a red star and a maroon jacket.",
  "Your secret role is COMMUNIST. The card shows a polar bear in profile wearing an ushanka hat with a red star.",
];
const CapitalistImages = [RoleCapitalist];
const CapitalistImagesAltText = [
  "Your secret role is CAPITALIST. The card shows a smug pig wearing a top hat and a monocle on a chain.",
];
const MonarchistImages = [RoleMonarchist];
const MonarchistImagesAltText = [
  "Your secret role is MONARCHIST. The card shows a screeching eagle wearing a spiked German military helmet.",
];

const LiberalText = [
  "You win if the board fills with liberal policies, or if Hitler is executed.",
  "You lose if the board fills with fascist policies, or if Hitler is elected chancellor after 3 fascist policies are passed.",
  "Keep your eyes open and look for suspicious actions. Suss out Hitler, and remember that anyone might be lying!",
];
const FascistText = [
  "You win if Hitler is successfully elected chancellor once 3 fascist policies are on the board, or if the board fills with fascist policies.",
  "You lose if the board fills with liberal policies or if Hitler is executed.",
  "Keep suspicion off of Hitler and look for ways to throw confusion into the game.",
];
const HitlerText = [
  "You win if you are successfully elected chancellor once 3 fascist policies are on the board, or if the board fills with fascist policies.",
  "You lose if the board fills with liberal policies or if you are executed.",
  "Try to gain trust and rely on the other fascists to open opportunities for you.",
];
const AnarchistText = [
  "You win if an Anarchist policy is forced through by the election tracker - either after 3 failed votes, or immediately after you enact one yourself.",
  "You aren't on the Liberal or Fascist team, and lose if either of them reaches their own victory condition first.",
  "Push for gridlock and failed votes. Anarchist policies leave the outcome up to chance, and every one played shuffles another into the deck.",
];
const CommunistText = [
  "You win if the Communist policy tracker fills, or if Hitler is executed.",
  "You aren't on the Liberal or Fascist team, and lose if either of them (or the Capitalist, or an Anarchist without you) reaches their own victory condition first.",
  "Work with your fellow Communists using investigation, radicalization, and Congress to find each other without tipping off the other teams.",
];
const CapitalistText = [
  "You win alongside whichever of the Liberals or Fascists wins first - by policy, by executing Hitler, or by electing Hitler chancellor.",
  "You lose if the Communists or an Anarchist reach their own victory condition first.",
  "You can't be radicalized by the Communists. Play both sides, and let the Liberals and Fascists fight it out - either outcome works for you.",
];
const MonarchistText = [
  "You win alongside the Fascists, but only if the Fascist policy tracker fills before Hitler is ever elected chancellor.",
  "You lose if Hitler is elected chancellor - even though the Fascists still win in that case, your own plot to keep power for yourself has failed.",
  "Support the Fascist agenda, but be wary of letting Hitler get too close to the chancellorship.",
];

type RoleAlertProps = {
  role?: Role;
  name: string;
  gameState: GameState;
  onClick: () => void;
};

/**
 * CustomAlert content that shows the player's current role and a quick guide on how to play
 * the game.
 * Parameters:
 *      - {@code role} [String]: The role of the player. Should be either LIBERAL, FASCIST, or HITLER.
 *      - {@code roleID} [int]: The integer roleID of the player. This is used to show unique role cards.
 *          The roleID can range from [1, 7] for LIBERALS, [1, 4] for FASCISTS, and [1] for HITLER. If out of bounds,
 *          the value is set to 1 (default).
 *      - {@code onClick} [()]: The callback function for when confirmation button ("OKAY") is pressed.
 */
class RoleAlert extends Component<RoleAlertProps> {
  getRoleImageAndAlt(): { image: string; alt: string } {
    let images: string[];
    let imageAlts: string[];
    switch (this.props.role) {
      case Role.LIBERAL:
        images = LiberalImages;
        imageAlts = LiberalImagesAltText;
        break;
      case Role.FASCIST:
        images = FascistImages;
        imageAlts = FascistImagesAltText;
        break;
      case Role.ANARCHIST:
        images = AnarchistImages;
        imageAlts = AnarchistImagesAltText;
        break;
      case Role.COMMUNIST:
        images = CommunistImages;
        imageAlts = CommunistImagesAltText;
        break;
      case Role.CAPITALIST:
        images = CapitalistImages;
        imageAlts = CapitalistImagesAltText;
        break;
      case Role.MONARCHIST:
        images = MonarchistImages;
        imageAlts = MonarchistImagesAltText;
        break;
      default: // Hitler
        images = HitlerImages;
        imageAlts = HitlerImagesAltText;
    }
    const playerIndex = this.props.gameState.playerOrder.indexOf(
      this.props.name
    );
    const roleId = playerIndex % images.length;

    return {
      image: images[roleId],
      alt: imageAlts[roleId],
    };
  }

  render() {
    let roleText = HitlerText;
    if (this.props.role === Role.FASCIST) {
      roleText = FascistText;
    } else if (this.props.role === Role.LIBERAL) {
      roleText = LiberalText;
    } else if (this.props.role === Role.ANARCHIST) {
      roleText = AnarchistText;
    } else if (this.props.role === Role.COMMUNIST) {
      roleText = CommunistText;
    } else if (this.props.role === Role.CAPITALIST) {
      roleText = CapitalistText;
    } else if (this.props.role === Role.MONARCHIST) {
      roleText = MonarchistText;
    }

    const { image, alt } = this.getRoleImageAndAlt();

    return (
      <div>
        <div>
          <h2 id="alert-header" className={"left-align"}>
            YOU ARE: {this.props.role}
          </h2>
          <img id="role" src={image} alt={alt} />

          <p className={"left-align"}>{roleText[0]}</p>
          <p className={"left-align"}>{roleText[1]}</p>
          <p className="highlight left-align">{roleText[2]}</p>
        </div>

        <button onClick={this.props.onClick}>OKAY</button>
      </div>
    );
  }
}

export default RoleAlert;
