import React, { Component } from "react";
import PropTypes from "prop-types";
import ButtonPrompt from "./ButtonPrompt";

import PartyBack from "../assets/party-membership.png";
import PartyLiberal from "../assets/party-membership-liberal.png";
import PartyFascist from "../assets/party-membership-fascist.png";
import PartyAnarchist from "../assets/party-membership-anarchist.png";
import PartyCommunist from "../assets/party-membership-communist.png";
import PartyCapitalist from "../assets/party-membership-capitalist.png";
import { LIBERAL } from "../constants";
import { Role } from "../types";

import "./InvestigationAlert.css";

/**
 * The contents for a CustomAlert that show a party membership card.
 * The card and related text are animated for effect.
 */
class InvestigationAlert extends Component {
  constructor(props) {
    super(props);
    this.state = {
      flipCard: false,
      showText: false,
      disableButton: true,
    };
    setTimeout(() => {
      this.setState({ flipCard: true });
    }, 1000);
    setTimeout(() => {
      this.setState({ showText: true, disableButton: false });
    }, 1500);
  }

  render() {
    let alt =
      this.props.target + " is a member of the " + this.props.party + " party.";
    let cardFrontSrc;
    if (this.props.party === LIBERAL) {
      cardFrontSrc = PartyLiberal;
    } else if (this.props.party === Role.ANARCHIST) {
      cardFrontSrc = PartyAnarchist;
    } else if (this.props.party === Role.COMMUNIST) {
      cardFrontSrc = PartyCommunist;
    } else if (this.props.party === Role.CAPITALIST) {
      cardFrontSrc = PartyCapitalist;
    } else {
      // Monarchist doesn't have dedicated party membership art yet;
      // fall back to the fascist card since it's aligned with that team.
      cardFrontSrc = PartyFascist;
    }
    let footerClass = this.state.showText
      ? "investigation-text-show"
      : "investigation-text-hide";
    let cardClass = this.state.flipCard
      ? "investigation-container-flip"
      : "investigation-container-default";
    return (
      <ButtonPrompt
        label={"INVESTIGATION RESULTS"}
        renderFooter={() => {
          return (
            <p id="investigation-text" className={footerClass}>
              {this.props.target +
                " is a member of the " +
                this.props.party +
                " party."}
            </p>
          );
        }}
        buttonOnClick={this.props.hideAlert}
        buttonDisabled={this.state.disableButton}
        buttonText={"OKAY"}
      >
        <div id={"party-card-container"}>
          <img
            id={"party-card-back"}
            className={cardClass}
            src={PartyBack}
            alt={alt}
          />
          <img
            id={"party-card-front"}
            className={cardClass}
            src={cardFrontSrc}
            alt={alt}
          />
        </div>
      </ButtonPrompt>
    );
  }
}

InvestigationAlert.propTypes = {
  party: PropTypes.string.isRequired,
  target: PropTypes.string.isRequired,
  hideAlert: PropTypes.func.isRequired,
};

export default InvestigationAlert;
