/*
 * SteVe - SteckdosenVerwaltung - https://github.com/steve-community/steve
 * Copyright (C) 2013-2025 SteVe Community Team
 * All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.rwth.idsg.steve.ocpp.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sevket Goekay <sevketgokay@gmail.com>
 * @since 10.05.2018
 */
@Slf4j
public final class WebSocketLogger {

    private static final KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String KAFKA_INCOMING_TOPIC = "incoming";
    private static final String KAFKA_OUTGOING_TOPIC = "outgoing";
    private static final String KAFKA_SERVER_ADDRESS = "localhost:9092";

    private WebSocketLogger() { }

    public static void connected(String chargeBoxId, WebSocketSession session) {
        log.info("[chargeBoxId={}, sessionId={}] Connection is established", chargeBoxId, session.getId());
    }

    public static void closed(String chargeBoxId, WebSocketSession session, CloseStatus closeStatus) {
        log.warn("[chargeBoxId={}, sessionId={}] Connection is closed, status: {}", chargeBoxId, session.getId(), closeStatus);
    }

    public static void sending(String chargeBoxId, WebSocketSession session, String msg) {
        log.info("[chargeBoxId={}, sessionId={}] Sending: {}", chargeBoxId, session.getId(), msg);
        try {
            String kafkaMessage = mapper.writeValueAsString(new KafkaMessage(session.getId(), chargeBoxId, msg));
            kafkaTemplate.send(KAFKA_OUTGOING_TOPIC, chargeBoxId, kafkaMessage);
        } catch (JsonProcessingException ignored) {}
    }

    public static void sendingPing(String chargeBoxId, WebSocketSession session) {
        log.debug("[chargeBoxId={}, sessionId={}] Sending ping message", chargeBoxId, session.getId());
    }

    public static void receivedPong(String chargeBoxId, WebSocketSession session) {
        log.debug("[chargeBoxId={}, sessionId={}] Received pong message", chargeBoxId, session.getId());
    }

    public static void receivedText(String chargeBoxId, WebSocketSession session, String msg) {
        log.info("[chargeBoxId={}, sessionId={}] Received: {}", chargeBoxId, session.getId(), msg);
        try {
            String kafkaMessage = mapper.writeValueAsString(new KafkaMessage(session.getId(), chargeBoxId, msg));
            kafkaTemplate.send(KAFKA_INCOMING_TOPIC, chargeBoxId, kafkaMessage);
        } catch (JsonProcessingException ignored) {}
    }

    public static void receivedEmptyText(String chargeBoxId, WebSocketSession session) {
        log.warn("[chargeBoxId={}, sessionId={}] Received empty text message. Will pretend this never happened.", chargeBoxId, session.getId());
    }

    public static void pingError(String chargeBoxId, WebSocketSession session, Throwable t) {
        if (log.isErrorEnabled()) {
            log.error("[chargeBoxId=" + chargeBoxId + ", sessionId=" + session.getId() + "] Ping error", t);
        }
    }

    public static void transportError(String chargeBoxId, WebSocketSession session, Throwable t) {
        if (log.isErrorEnabled()) {
            log.error("[chargeBoxId=" + chargeBoxId + ", sessionId=" + session.getId() + "] Transport error", t);
        }
    }

    private static ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER_ADDRESS);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    private static KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    record KafkaMessage(String sessionId, String chargeBoxId,String message) {}
}
