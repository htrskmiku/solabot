package com.arth.bot.adapter.parser;

import com.arth.bot.core.common.dto.ParsedPayloadDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface PayloadParser {

    ParsedPayloadDTO parseRawToDTO(JsonNode root, String raw) throws JsonProcessingException;
}
