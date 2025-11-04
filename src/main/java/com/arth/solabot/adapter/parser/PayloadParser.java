package com.arth.solabot.adapter.parser;

import com.arth.solabot.core.bot.dto.ParsedPayloadDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface PayloadParser {

    ParsedPayloadDTO parseRawToDTO(JsonNode root, String raw) throws JsonProcessingException;
}
