package com.c2.protocol;

import org.json.JSONException;
import org.json.JSONObject;

public record ProtocolMessage(
        String type,
        String id,
        String target,
        String command,
        String status,
        String data
) {

    public static final String TYPE_COMMAND = "command";
    public static final String TYPE_RESPONSE = "response";
    public static final String TYPE_HEARTBEAT = "heartbeat";

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

    public static ProtocolMessage command(String id, String target, String command) {
        return new ProtocolMessage(TYPE_COMMAND, id, target, command, null, null);
    }

    public static ProtocolMessage heartbeat(String id, String target) {
        return new ProtocolMessage(TYPE_HEARTBEAT, id, target, null, null, null);
    }

    public static ProtocolMessage success(String id, String target, String data) {
        return new ProtocolMessage(TYPE_RESPONSE, id, target, null, STATUS_SUCCESS, data);
    }

    public static ProtocolMessage error(String id, String target, String data) {
        return new ProtocolMessage(TYPE_RESPONSE, id, target, null, STATUS_ERROR, data);
    }

    public static ProtocolMessage fromJson(String rawJson) throws JSONException {
        JSONObject json = new JSONObject(rawJson);

        return new ProtocolMessage(
                json.optString("type", null),
                json.optString("id", null),
                json.optString("target", null),
                json.optString("command", null),
                json.optString("status", null),
                json.optString("data", null)
        );
    }

    public String toJson() {
        JSONObject json = new JSONObject();

        putIfPresent(json, "type", type);
        putIfPresent(json, "id", id);
        putIfPresent(json, "target", target);
        putIfPresent(json, "command", command);
        putIfPresent(json, "status", status);
        putIfPresent(json, "data", data);

        return json.toString();
    }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equals(status);
    }

    private static void putIfPresent(JSONObject json, String key, String value) {
        if (value != null) {
            json.put(key, value);
        }
    }
}
