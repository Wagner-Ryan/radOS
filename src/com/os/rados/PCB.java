package com.os.rados;

class PCB {
    private int pid;
    private String name;
    private String state; // READY, RUNNING, BLOCKED
    private boolean active;

    public PCB(int pid, String name) {
        this.pid = pid;
        this.name = name;
        this.state = "READY";
        this.active = true;
    }

    public int getPid() { return pid; }
    public String getName() { return name; }
    public String getState() { return state; }
    public boolean isActive() { return active; }

    public void setState(String state) { this.state = state; }
    public void setActive(boolean active) { this.active = active; }
}