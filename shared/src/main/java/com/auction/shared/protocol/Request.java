package com.auction.shared.protocol;

import java.io.Serializable;
import java.util.Map;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private CommandType command;
    private Map<String, Object> data;
    private String token;

    public Request() {}

    public Request(CommandType command, Map<String, Object> data) {
        this.command = command;
        this.data = data;
    }

    public Request(CommandType command, Map<String, Object> data, String token) {
        this.command = command;
        this.data = data;
        this.token = token;
    }

    // Getters and Setters
    public CommandType getCommand() { return command; }
    public void setCommand(CommandType command) { this.command = command; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

}
