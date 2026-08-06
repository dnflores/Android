package com.quvii.publico.entity;

public class PortInfo {
    private int port;
    private int type;

    public PortInfo() {}

    public PortInfo(int port, int type) {
        this.port = port;
        this.type = type;
    }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    @Override
    public String toString() {
        return "PortInfo{port=" + port + ", type=" + type + "}";
    }
}
