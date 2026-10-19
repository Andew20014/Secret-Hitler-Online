import React, { ReactElement, useEffect, useRef, useState } from "react";
import ButtonPrompt from "./ButtonPrompt";
import { SERVER_TIMEOUT } from "../constants";
import { SendWSCommand, ServerRequestPayload, WSCommandType } from "../types";

type SimpleActionPromptProps = {
  label: string;
  headerText: string;
  buttonText: string;
  payload: ServerRequestPayload;
  sendWSCommand: SendWSCommand;
};

/**
 * A ButtonPrompt that sends a single, target-less command to the server when
 * its button is pressed, locking the button for a set duration afterward.
 */
export default function SimpleActionPrompt(
  props: SimpleActionPromptProps
): ReactElement {
  const timeoutID = useRef<NodeJS.Timeout | undefined>(undefined);
  const [isWaitingForServer, setIsWaitingForServer] = useState(false);

  useEffect(() => {
    return () => clearTimeout(timeoutID.current);
  }, []);

  const onButtonClick = () => {
    setIsWaitingForServer(true);
    timeoutID.current = setTimeout(() => {
      setIsWaitingForServer(false);
    }, SERVER_TIMEOUT);
    props.sendWSCommand(props.payload);
  };

  return (
    <ButtonPrompt
      label={props.label}
      headerText={props.headerText}
      buttonText={props.buttonText}
      buttonOnClick={onButtonClick}
      buttonDisabled={isWaitingForServer}
    />
  );
}

// Definitions for some basic templates, for the Secret Hitler XL mod's
// communist powers that don't need the acting player to choose a target.

export const FiveYearPlanPrompt = (
  sendWSCommand: SendWSCommand
): ReactElement => {
  return (
    <SimpleActionPrompt
      label={"FIVE-YEAR PLAN"}
      headerText={
        "Two Communist policies and one Liberal policy will be shuffled into the draw deck."
      }
      buttonText={"OKAY"}
      payload={{ command: WSCommandType.REGISTER_FIVE_YEAR_PLAN }}
      sendWSCommand={sendWSCommand}
    />
  );
};

export const CongressPrompt = (sendWSCommand: SendWSCommand): ReactElement => {
  return (
    <SimpleActionPrompt
      label={"CONGRESS"}
      headerText={
        "The newly radicalized Communists will now learn who the original Communists are."
      }
      buttonText={"OKAY"}
      payload={{ command: WSCommandType.END_CONGRESS }}
      sendWSCommand={sendWSCommand}
    />
  );
};
