package uk.gov.ons.ctp.integration.event.generator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.event.EventPublisher;
import uk.gov.ons.ctp.common.event.EventSender;
import uk.gov.ons.ctp.common.event.PubSubEventSender;
import uk.gov.ons.ctp.common.pubsub.PubSubHelper;

@SpringBootApplication
@ComponentScan(basePackages = {"uk.gov.ons.ctp.integration"})
public class EventGeneratorApplication {

  @Value("${spring.cloud.gcp.pubsub.emulator-host:#{null}}")
  String emulatorHost;

  @Value("${spring.cloud.gcp.pubsub.project-id}")
  String projectId;

  public static void main(final String[] args) {
    SpringApplication.run(EventGeneratorApplication.class, args);
  }

  @Bean
  public EventGenerator eventGenerator(EventPublisher eventPublisher) {
    return new EventGenerator(eventPublisher);
  }

  /**
   * Bean used to publish asynchronous event messages
   */
  @Bean
  public EventPublisher eventPublisher(
      @Qualifier("pubSubTemplate") PubSubTemplate pubSubTemplate) {

    EventSender sender =
        new PubSubEventSender(pubSubTemplate, 5);
    return EventPublisher.createWithoutEventPersistence(sender);
  }

  @Bean
  public PubSubHelper pubSub() throws CTPException {
    boolean useEmulator = false;
    if (emulatorHost != null) {
      useEmulator = true;
    }
    return PubSubHelper.instance(projectId,
        false, useEmulator, emulatorHost);
  }
}