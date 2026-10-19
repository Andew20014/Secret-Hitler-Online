import React, { ReactElement, useEffect, useRef, useState } from "react";
import ButtonPrompt from "./ButtonPrompt";
import PolicyDisplay from "../util/PolicyDisplay";
import "./XLPrompts.css";
import { SERVER_TIMEOUT } from "../constants";
import { PolicyType, SendWSCommand, WSCommandType } from "../types";

type PropagandaPromptProps = {
  policy: PolicyType;
  sendWSCommand: SendWSCommand;
};

/**
 * Shows the acting officer the top policy of the draw deck, letting them
 * discard it or leave it in place, as part of the Propaganda emergency power.
 */
export default function PropagandaPrompt(
  props: PropagandaPromptProps
): ReactElement {
  const timeoutID = useRef<NodeJS.Timeout | undefined>(undefined);
  const [isWaitingForServer, setIsWaitingForServer] = useState(false);

  useEffect(() => {
    return () => clearTimeout(timeoutID.current);
  }, []);

  const respond = (discard: boolean) => {
    setIsWaitingForServer(true);
    timeoutID.current = setTimeout(() => {
      setIsWaitingForServer(false);
    }, SERVER_TIMEOUT);
    props.sendWSCommand({
      command: WSCommandType.REGISTER_PROPAGANDA_DISCARD,
      discard,
    });
  };

  return (
    <ButtonPrompt
      label={"PROPAGANDA"}
      headerText={
        "This is the top policy of the draw deck. Discard it, or leave it in place?"
      }
      renderButton={() => (
        <div id={"propaganda-button-container"}>
          <button disabled={isWaitingForServer} onClick={() => respond(true)}>
            DISCARD
          </button>
          <button
            disabled={isWaitingForServer}
            onClick={() => respond(false)}
          >
            LEAVE IT
          </button>
        </div>
      )}
    >
      <PolicyDisplay
        policies={[props.policy]}
        onClick={() => {}}
        allowSelection={false}
      />
    </ButtonPrompt>
  );
}
