package uk.gov.ons.ctp.integration.event.generator.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.ctp.common.endpoint.CTPEndpoint;
import uk.gov.ons.ctp.common.event.EventTopic;
import uk.gov.ons.ctp.common.event.TopicType;
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

  @Autowired
  PubSubHelper pubSub;

  private ObjectMapper mapper = new ObjectMapper();

  @RequestMapping(value = "/pubsub/create/{eventType}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> createSubscription(
      @PathVariable(value = "eventType") final String eventTypeAsString) throws Exception {
    log.info("Creating subscription for events of type: '" + eventTypeAsString + "'");
    TopicType eventType = TopicType.valueOf(eventTypeAsString);
    String subscriptionName = pubSub.createSubscription(eventType);
    return ResponseEntity.ok(subscriptionName);
  }

  @RequestMapping(value = "/pubsub/flush/{subscriptionName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<HttpStatus> flushSubscription(
      @PathVariable(value = "subscriptionName") final String subscriptionName) throws Exception {
    log.info("Flushing subscription: '" + subscriptionName + "'");
    pubSub.flushSubscription(getEventType(subscriptionName));
    return ResponseEntity.ok().build();
  }

  @RequestMapping(value = "/pubsub/get/{subscriptionName}", method = RequestMethod.GET)
  @ResponseStatus(value = HttpStatus.OK)
  public ResponseEntity<String> get(
      @PathVariable(value = "subscriptionName") final String subscriptionName,
      @RequestParam String timeout) throws Exception {

    log.info("Getting from subscription: '" + subscriptionName
        + "' with timeout of '" + timeout + "'");

    String messageBody = pubSub.getMessage(subscriptionName,
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

    TopicType eventType = getEventType(subscriptionName);
    if (eventType == null) {
      return ResponseEntity.notFound().build();
    }

    // Read message as object
    Class<?> clazz = Class.forName(clazzName);
    Object resultAsObject =
        pubSub.getMessage(eventType, clazz, TimeoutParser.parseTimeoutString(timeout));

    // Bail out if no object read from subscription.
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

  private TopicType getEventType(String subscriptionName) {
    for (EventTopic topic : EventTopic.values()) {
      if (subscriptionName.contains(topic.getTopic())) {
        return topic.getType();
      }
    }
    return null;
  }

}
