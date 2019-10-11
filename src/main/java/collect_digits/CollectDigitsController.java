/* 
 * AFTER RUNNING PROJECT WITH COMMAND: 
 * `gradle build && java -Dserver.port=0080 -jar build/libs/gs-spring-boot-0.1.0.jar`
 * CALL NUMBER ASSOCIATED WITH THIS Persephony App (CONFIGURED IN PERSEPHONY DASHBOARD)
 * EXPECT A PROMPT TO ENTER A KEY ASSOCIATED WITH A COLOR,
 * THEN EXPECT A MESSAGE WHICH REPEATS THE COLOR THAT WAS SELECTED.
*/

package main.java.collect_digits;

import org.springframework.web.bind.annotation.RestController;
import com.vailsys.persephony.api.PersyException;
import com.vailsys.persephony.api.call.CallStatus;
import com.vailsys.persephony.percl.Language;
import com.vailsys.persephony.percl.Pause;
import com.vailsys.persephony.percl.PerCLScript;
import com.vailsys.persephony.percl.Say;
import com.vailsys.persephony.percl.GetDigits;
import com.vailsys.persephony.percl.GetDigitsNestable;
import com.vailsys.persephony.webhooks.application.ApplicationVoiceCallback;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.LinkedList;

import com.vailsys.persephony.webhooks.percl.GetDigitsActionCallback;
import com.vailsys.persephony.percl.Hangup;

@RestController
public class CollectDigitsController {
  // Get base URL from environment variables
  private String baseUrl = System.getenv("HOST");

  // To properly communicate with Persephony's API, set your Persephony app's
  // VoiceURL endpoint to '{yourApplicationURL}/InboundCall' for this example
  // Your Persephony app can be configured in the Persephony Dashboard
  @RequestMapping(value = {
      "/InboundCall" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> inboundCall(@RequestBody String str) {
    ApplicationVoiceCallback applicationVoiceCallback;
    // Convert JSON into application voice callback object
    try {
      applicationVoiceCallback = ApplicationVoiceCallback.createFromJson(str);
    } catch (PersyException pe) {
      PerCLScript exceptionScript = new PerCLScript();
      Say errorSay = new Say("There was an error handling the incoming call.");
      exceptionScript.add(errorSay);
      return new ResponseEntity<>(exceptionScript.toJson(), HttpStatus.OK);
    }

    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    // Verify call is in the Ringing state
    if (applicationVoiceCallback.getCallStatus() == CallStatus.RINGING) {

      // Create PerCL say script with US English as the language
      Say sayGreeting = new Say("Hello");
      sayGreeting.setLanguage(Language.ENGLISH_US);

      // Add PerCL say script to PerCL container
      script.add(sayGreeting);

      // Create PerCL pause script
      Pause pause = new Pause(100);

      // Add PerCL pause script to PerCL container
      script.add(pause);

      // // Create PerCL getdigits script
      GetDigits getDigits = new GetDigits(baseUrl + "/ColorSelectionDone");
      // Set maximum digits to 1
      getDigits.setMaxDigits(1);
      // Set minimum digits to 1
      getDigits.setMinDigits(1);
      // Set buffer flush to true
      getDigits.setFlushBuffer(true);

      // Create an empty GetDigitsNestable list
      LinkedList<GetDigitsNestable> prompts = new LinkedList<>();

      // Create PerCL say script with US English as the language
      Say sayPrompt = new Say("Please select a color. Enter one for green, two for red, and three for blue.");
      sayPrompt.setLanguage(Language.ENGLISH_US);

      // Add PerCL say script to GetDigitsNestable list
      prompts.add(sayPrompt);

      // Add GetDigitsNestable list to PerCL getdigits script
      getDigits.setPrompts(prompts);

      // Add PerCL getdigits script to PerCL container
      script.add(getDigits);
    }

    // Respond with the PerCL script
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }

  @RequestMapping(value = {
      "/ColorSelectionDone" }, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<?> ColorSelectionDone(@RequestBody String str) {
    GetDigitsActionCallback getDigitsCallback;
    // Convert JSON into getdigits action callback object
    try {
      getDigitsCallback = GetDigitsActionCallback.createFromJson(str);
    } catch (PersyException pe) {
      PerCLScript errorScript = new PerCLScript();
      Say sayError = new Say("There was an error processing the color selection.");
      errorScript.add(sayError);
      return new ResponseEntity<>(errorScript.toJson(), HttpStatus.OK);
    }

    // Create an empty PerCL script container
    PerCLScript script = new PerCLScript();

    // Verify one DTMF was received
    if (getDigitsCallback.getDigits() != null && getDigitsCallback.getDigits().length() == 1) {

      // Create PerCL say script based on the selected DTMF with US English as the
      // language
      Say say;
      switch (getDigitsCallback.getDigits()) {
      case "1":
        say = new Say("You selected green. Goodbye.");
        break;
      case "2":
        say = new Say("You selected red. Goodbye.");
        break;
      case "3":
        say = new Say("You selected blue. Goodbye.");
        break;
      default:
        say = new Say("Invalid selection. Goodbye.");
        break;
      }
      say.setLanguage(Language.ENGLISH_US);

      // Add PerCL say script to PerCL container
      script.add(say);

      // Create PerCL hangup script and add it to PerCL container
      script.add(new Hangup());
    }

    // Respond with the PerCL script
    return new ResponseEntity<>(script.toJson(), HttpStatus.OK);
  }
}
