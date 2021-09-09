package uk.gov.ons.ctp.integration.event.generator.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventTopic;
import uk.gov.ons.ctp.common.event.EventType;
import uk.gov.ons.ctp.common.pubsub.PubSubHelper;
import uk.gov.ons.ctp.integration.event.generator.util.TimeoutParser;

/**
 * This endpoint gives command line access to PubSub. It basically delegates to PubSub support
 * class, so it can also be used for manual testing of the PubSub support code.
 */

@Slf4j
@RestController
@RequestMapping(produces = "application/json")
public class PubSubEndpoint implements CTPEndpoint {

  @Value("${spring.cloud.gcp.pubsub.emulator-host:#{null}}")
  String emulatorHost;

  @Value("${spring.cloud.gcp.pubsub.project-id}")
  String projectId;

  private ObjectMapper mapper = new ObjectMapper();

  @RequestMapping(value = "/pubsub/create/{eventType}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> createQueue(
      @PathVariable(value = "eventType") final String eventTypeAsString) throws Exception {
    log.info("Creating queue for events of type: '" + eventTypeAsString + "'");
    EventType eventType = EventType.valueOf(eventTypeAsString);
    String subscriptionName = pubSub().createSubscription(eventType);
    return ResponseEntity.ok(subscriptionName);
  }

  @RequestMapping(value = "/pubsub/flush/{subscriptionName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<HttpStatus> flushQueue(
      @PathVariable(value = "subscriptionName") final String subscriptionName) throws Exception {
    log.info("Flushing queue: '" + subscriptionName + "'");
    pubSub().flushTopic(getEventType(subscriptionName));
    return ResponseEntity.ok().build();
  }

  @RequestMapping(value = "/pubsub/get/{subscriptionName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> get(
      @PathVariable(value = "subscriptionName") final String subscriptionName,
      @RequestParam String timeout) throws Exception {

    log.info("Getting from queue: '" + subscriptionName + "' with timeout of '" + timeout + "'");

    String messageBody = pubSub().getMessage(subscriptionName,
        TimeoutParser.parseTimeoutString(timeout));

    if (messageBody == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(messageBody);
  }

  @RequestMapping(value = "/pubsub/get/object/{subscriptionName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> get(
      @PathVariable(value = "subscriptionName") final String subscriptionName,
      @RequestParam String clazzName,
      @RequestParam String timeout)
      throws Exception {

    log.info(
        "Getting from subscription: '"
            + subscriptionName
            + "' and converting to an object of type '"
            + clazzName
            + "', with timeout of '"
            + timeout
            + "'");

    EventType eventType = getEventType(subscriptionName);
    if (eventType == null)  {
      return ResponseEntity.notFound().build();
    }

    // Read message as object
    Class<?> clazz = Class.forName(clazzName);
    Object resultAsObject =
        pubSub().getMessage(eventType, clazz, TimeoutParser.parseTimeoutString(timeout));

    // Bail out if no object read from queue.
    if (resultAsObject == null) {
      return ResponseEntity.notFound().build();
    }

    // Convert object to string
    String messageBody = mapper.writeValueAsString(resultAsObject);

    if (messageBody == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(messageBody);
  }

  @RequestMapping(value = "/pubsub/close", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> close() throws Exception {
    log.info("Closing PubSub channel connection");
    PubSubHelper.destroy();
    return ResponseEntity.ok("Connection closed");
  }

  private PubSubHelper pubSub() throws CTPException {
    boolean useEmulator = false;
    if(emulatorHost != null) {
      useEmulator = true;
    }
      return PubSubHelper.instance(projectId,
        false, useEmulator, emulatorHost);
  }

  private EventType getEventType(String subscriptionName) {
    for (EventTopic topic : EventTopic.values()) {
      if (subscriptionName.contains(topic.getTopic())) {
        return topic.getType();
      }
    }
    return null;
  }

}
