package com.ds.migration.broker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RabbitMQConfiguration implements RabbitListenerConfigurer {

	public static final String DEAD_LETTER_EXCHANGE_CONSTANT = "x-dead-letter-exchange";

	public static final String DEAD_LETTER_ROUTING_KEY_CONSTANT = "x-dead-letter-routing-key";

	@Value("${migration.queue.ttl}")
	private Integer ttl;// in milliseconds

	@Value("${migration.queue.name}")
	private String workerQueueName;

	@Value("${migration.exchange.name}")
	private String workerExchangeName;

	@Value("${migration.routing.key}")
	private String workerRoutingKey;

	@Value("#{'RETRY_' + '${migration.queue.name}'}")
	private String retryQueueName;

	@Value("#{'RETRY_' + '${migration.exchange.name}'}")
	private String retryExchangeName;

	@Value("#{'RETRY_' + '${migration.routing.key}'}")
	private String retryRoutingKey;

	@Value("#{'DEAD_' + '${migration.queue.name}'}")
	private String deadQueueName;

	@Value("#{'DEAD_' + '${migration.exchange.name}'}")
	private String deadExchangeName;

	@Value("#{'DEAD_' + '${migration.routing.key}'}")
	private String deadRoutingKey;

	@Bean
	Queue workerQueue() {
		return QueueBuilder.durable(workerQueueName).withArgument(DEAD_LETTER_EXCHANGE_CONSTANT, retryExchangeName)
				.withArgument(DEAD_LETTER_ROUTING_KEY_CONSTANT, retryRoutingKey).build();
	}

	@Bean
	Exchange workerExchange() {
		return ExchangeBuilder.directExchange(workerExchangeName).build();
	}

	@Bean
	Binding bindingToWorkBinding() {

		log.debug("workerQueue -> {}, workerExchange -> {}, workerRoutingKey -> {}", workerQueue().getName(),
				workerExchange().getName(), workerRoutingKey);

		return BindingBuilder.bind(workerQueue()).to(workerExchange()).with(workerRoutingKey).noargs();
	}

	@Bean
	Queue retryQueue() {

		return QueueBuilder.durable(retryQueueName).withArgument(DEAD_LETTER_EXCHANGE_CONSTANT, workerExchangeName)
				.withArgument(DEAD_LETTER_ROUTING_KEY_CONSTANT, workerRoutingKey).withArgument("x-message-ttl", ttl)
				.build();
	}

	@Bean
	Exchange retryExchange() {

		return ExchangeBuilder.directExchange(retryExchangeName).build();
	}

	@Bean
	Binding bindingToRetryBinding() {

		log.debug("retryLetterQueue -> {}, retryExchange -> {}, retryRoutingKey -> {}", retryQueue().getName(),
				retryExchange().getName(), retryRoutingKey);
		return BindingBuilder.bind(retryQueue()).to(retryExchange()).with(retryRoutingKey).noargs();
	}

	@Bean
	Queue deadQueue() {

		return QueueBuilder.durable(deadQueueName).build();
	}

	@Bean
	Exchange deadExchange() {

		return ExchangeBuilder.directExchange(deadExchangeName).build();
	}

	@Bean
	Binding bindingToDeadBinding() {

		log.debug("deadQueue -> {}, deadExchange -> {}, deadRoutingKey -> {}", deadQueue().getName(),
				deadExchange().getName(), deadRoutingKey);
		return BindingBuilder.bind(deadQueue()).to(deadExchange()).with(deadRoutingKey).noargs();
	}

	@Bean
	@ConditionalOnProperty(value = "migration.queue.name", havingValue = "CORE_PARALLEL_PROCESS_START_QUEUE")
	Queue holdQueue() {

		return QueueBuilder.durable("HOLD_CORE_PARALLEL_PROCESS_START_QUEUE").build();
	}

	@Bean
	@ConditionalOnProperty(value = "migration.queue.name", havingValue = "CORE_PARALLEL_PROCESS_START_QUEUE")
	Binding bindingToHoldBinding() {

		log.debug("holdQueue -> {}, holdExchange -> {}, holdRoutingKey -> {}", holdQueue().getName(),
				workerExchange().getName(), workerRoutingKey);
		return BindingBuilder.bind(holdQueue()).to(workerExchange()).with("HOLD_CORE_PARALLEL_PROCESS_START_QUEUE")
				.noargs();
	}

	@Bean
	public Jackson2JsonMessageConverter producerJackson2MessageConverter() {

		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

		final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
		return rabbitTemplate;
	}

	@Bean
	public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {

		final RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
		return rabbitAdmin;
	}

	@Override
	public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {

		registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
	}

	@Bean
	MessageHandlerMethodFactory messageHandlerMethodFactory() {

		DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
		messageHandlerMethodFactory.setMessageConverter(consumerJackson2MessageConverter());
		return messageHandlerMethodFactory;
	}

	@Bean
	public MappingJackson2MessageConverter consumerJackson2MessageConverter() {

		return new MappingJackson2MessageConverter();
	}
}