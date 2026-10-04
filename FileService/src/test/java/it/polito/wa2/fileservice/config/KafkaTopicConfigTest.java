package it.polito.wa2.fileservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

@SpringBootTest(classes = {KafkaTopicConfig.class})
public class KafkaTopicConfigTest {

    @Autowired
    @Qualifier("fileUploadTopic")
    private NewTopic topic;

    @Autowired
    private ApplicationContext ctx;

    @Test
    public void beanFileUploadTopicIsCreated() {
        Assertions.assertNotNull(topic, "Bean fileUploadTopic should not be null");
    }

    @Test
    public void fileUploadTopicHasExpectedNamePartitionsAndReplicas() {
        Assertions.assertEquals("file-upload.completed", topic.name(), "Topic name does not match");
        Assertions.assertEquals(1, topic.numPartitions(), "Number of partition does not match");
        int replicasValue = topic.replicationFactor();
        Assertions.assertEquals(1, replicasValue, "Number of replicas does not match");
    }

    @Test
    public void fileUploadTopicBeanIsSingleAndPresentInContext() {
        Map<String, NewTopic> topics = ctx.getBeansOfType(NewTopic.class);
        Assertions.assertTrue(!topics.isEmpty(), "No new bean NewTopic found in context");
        Assertions.assertEquals(1, topics.size(), "Only one NewTopic bean should be present in context");
    }

    @Test
    public void fileUploadTopicPropertiesAreValidAndPositive() {
        Assertions.assertNotNull(topic, "The bean fileUploadTopic should not be null");
        Assertions.assertEquals("file-upload.completed", topic.name(), "Topic name does not match");
        Assertions.assertTrue(topic.numPartitions() > 0, "Number of partitions should be greater than 0");
        int replicasValue = topic.replicationFactor();
        Assertions.assertTrue(replicasValue > 0, "Number of replicas should be greater than 0");
    }

    @Test
    public void topicIsInstanceOfNewTopic() {
        Assertions.assertTrue(topic instanceof NewTopic, "The bean fileUploadTopic should be an instance of NewTopic");
    }
}
