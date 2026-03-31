package com.auction.shared.protocol;

import java.io.Serializable;
import java.util.Map;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private CommandType command;
    private boolean success;
    private String message;
    private Map<String, Object> data;

    public Response() {}

    public Response(CommandType command, boolean success, String message) {
        this.command = command;
        this.success = success;
        this.message = message;
    }

    public Response(CommandType command, boolean success, String message, Map<String, Object> data) {
        this.command = command;
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Getters and Setters
    public CommandType getCommand() { return command; }
    public void setCommand(CommandType command) { this.command = command; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

}
