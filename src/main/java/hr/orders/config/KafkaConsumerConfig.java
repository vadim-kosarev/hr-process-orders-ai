package hr.orders.config;

import hr.orders.domain.command.OrderCommand;
import hr.orders.domain.event.OrderEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.consumer.events-group:order-events-group}")
    private String orderEventGroupId;

    @Value("${spring.kafka.consumer.commands-group:order-commands-group}")
    private String orderCommandGroupId;

    @Bean
    ConsumerFactory<String, OrderEvent> orderEventConsumerFactory(
            KafkaProperties props) {

        Map<String, Object> p = props.buildConsumerProperties();
        p.put(ConsumerConfig.GROUP_ID_CONFIG, orderEventGroupId);

        return new DefaultKafkaConsumerFactory<>(p);
    }

    @Bean
    ConsumerFactory<String, OrderCommand> orderCommandConsumerFactory(
            KafkaProperties props) {

        Map<String, Object> p = props.buildConsumerProperties();
        p.put(ConsumerConfig.GROUP_ID_CONFIG, orderCommandGroupId);

        return new DefaultKafkaConsumerFactory<>(p);
    }
}
