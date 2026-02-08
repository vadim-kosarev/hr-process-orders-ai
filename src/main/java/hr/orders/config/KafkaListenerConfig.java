package hr.orders.config;

import hr.orders.domain.command.OrderCommand;
import hr.orders.domain.event.OrderEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
public class KafkaListenerConfig {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, OrderEvent>
    orderEventListenerFactory(ConsumerFactory<String, OrderEvent> cf) {

        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> f =
                new ConcurrentKafkaListenerContainerFactory<>();

        f.setConsumerFactory(cf);
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return f;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, OrderCommand>
    orderCommandListenerFactory(ConsumerFactory<String, OrderCommand> cf) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCommand> f =
                new ConcurrentKafkaListenerContainerFactory<>();

        f.setConsumerFactory(cf);
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return f;
    }
}
