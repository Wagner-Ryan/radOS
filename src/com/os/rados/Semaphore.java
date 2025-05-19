package com.os.rados;

class Semaphore {
    private int value;

    public Semaphore(int value) {
        this.value = value;
    }

    public synchronized void waitSem() {
        while (value <= 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        value--;
    }

    public synchronized void signal() {
        value++;
        notify();
    }
}