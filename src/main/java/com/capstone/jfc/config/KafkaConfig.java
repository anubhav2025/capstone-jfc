package com.capstone.jfc.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.Map;



import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Consumer Config
     */
    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // We will consume everything as plain Strings
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        // Consumer for <String, String>
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    /**
     * Producer Config
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // We will produce everything as plain Strings
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Disable adding any type headers
        // (not strictly needed with StringSerializer, but just to be sure)
        props.put("spring.json.add.type.headers", false);
        return props;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        // Producer for <String, String>
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        // KafkaTemplate for <String, String>
        return new KafkaTemplate<>(producerFactory());
    }
}




// @EnableKafka
// @Configuration
// public class KafkaConfig {

//     @Autowired
//     private KafkaProperties kafkaProperties;

//     @Bean
//     public ConsumerFactory<String, String> consumerFactory() {
//         Map<String, Object> props = kafkaProperties.buildConsumerProperties();
//         return new DefaultKafkaConsumerFactory<>(props);
//     }

//     @Bean
//     public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
//         var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
//         factory.setConsumerFactory(consumerFactory());
//         return factory;
//     }

//     @Bean
//     public ProducerFactory<String, String> producerFactory() {
//         Map<String, Object> props = kafkaProperties.buildProducerProperties();
//         return new DefaultKafkaProducerFactory<>(props);
//     }

//     @Bean
//     public KafkaTemplate<String, String> kafkaTemplate() {
//         return new KafkaTemplate<>(producerFactory());
//     }

//     // We define the topics here if not existing
//     @Bean
//     public NewTopic scanRequestTopic() {
//         return TopicBuilder.name("scan-request-topic").partitions(3).replicas(1).build();
//     }

//     @Bean
//     public NewTopic scanPullTopic() {
//         return TopicBuilder.name("scan-pull-topic").partitions(3).replicas(1).build();
//     }

//     @Bean
//     public NewTopic scanParseTopic() {
//         return TopicBuilder.name("scan-parse-topic").partitions(3).replicas(1).build();
//     }

//     @Bean
//     public NewTopic jobAcknowledgementTopic() {
//         return TopicBuilder.name("job-acknowledgement-topic").partitions(3).replicas(1).build();
//     }
// }
