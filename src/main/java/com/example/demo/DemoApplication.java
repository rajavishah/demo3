package com.example.demo;

import com.example.demo.service.IndexingListener;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
public class DemoApplication {
	public static final String topicExchangeName = "topic-exchange";

	public static final String queueName = "indexing-queue";

	@Bean
	Queue queue() {
		return new Queue(queueName, false);
	}

	@Bean
	TopicExchange exchange() {
		return new TopicExchange(topicExchangeName);
	}

	@Bean
	Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(queueName);
	}

	@Bean
	MessageListenerAdapter listenerAdapter(IndexingListener receiver) {
		return new MessageListenerAdapter(receiver, "receiveMessage");
	}

	@Bean
	SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
											 MessageListenerAdapter listenerAdapter) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.setQueueNames(queueName);
		container.setMessageListener(listenerAdapter);
		return container;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
}

/*
 * https://www.baeldung.com/jedis-java-redis-client-library
 * https://docs.spring.io/spring-data/redis/docs/current/api/org/springframework/data/redis/connection/jedis/JedisConnectionFactory.html
 * https://www.baeldung.com/spring-data-redis-tutorial
 * https://spring.io/projects/spring-data-redis
 * https://docs.spring.io/spring-data/data-redis/docs/2.1.5.RELEASE/reference/html/#new-in-2.1.0
 */