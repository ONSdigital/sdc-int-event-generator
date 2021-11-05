package uk.gov.ons.ctp.integration.event.generator.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.event.TopicType;
import uk.gov.ons.ctp.common.domain.Source;

@Data
public class GeneratorRequest {
  private TopicType topicType;
  private Source source;
  private Channel channel;

  private List<Map<String, String>> contexts;
}
