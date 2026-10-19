import React, { ReactElement, useEffect, useRef, useState } from "react";
import ButtonPrompt from "./ButtonPrompt";
import "./XLPrompts.css";
import { SERVER_TIMEOUT } from "../constants";
import { PolicyType, SendWSCommand, WSCommandType } from "../types";

type SocialDemocraticPromptProps = {
  communistsIncluded: boolean;
  sendWSCommand: SendWSCommand;
};

/**
 * Lets the President choose which tracker (fascist or communist) to remove a
 * policy from, after a Social Democratic policy is enacted.
 */
export default function SocialDemocraticPrompt(
  props: SocialDemocraticPromptProps
): ReactElement {
  const timeoutID = useRef<NodeJS.Timeout | undefined>(undefined);
  const [isWaitingForServer, setIsWaitingForServer] = useState(false);

  useEffect(() => {
    return () => clearTimeout(timeoutID.current);
  }, []);

  const respond = (trackerType: PolicyType.FASCIST | PolicyType.COMMUNIST) => {
    setIsWaitingForServer(true);
    timeoutID.current = setTimeout(() => {
      setIsWaitingForServer(false);
    }, SERVER_TIMEOUT);
    props.sendWSCommand({
      command: WSCommandType.REGISTER_SOCIAL_DEMOCRATIC,
      trackerType,
    });
  };

  return (
    <ButtonPrompt
      label={"SOCIAL DEMOCRATIC"}
      headerText={
        "Remove a policy from either the fascist or the communist tracker. That tracker's power will not trigger again on this slot."
      }
      renderButton={() => (
        <div id={"social-democratic-button-container"}>
          <button
            disabled={isWaitingForServer}
            onClick={() => respond(PolicyType.FASCIST)}
          >
            FASCIST TRACKER
          </button>
          {props.communistsIncluded && (
            <button
              disabled={isWaitingForServer}
              onClick={() => respond(PolicyType.COMMUNIST)}
            >
              COMMUNIST TRACKER
            </button>
          )}
        </div>
      )}
    />
  );
}
