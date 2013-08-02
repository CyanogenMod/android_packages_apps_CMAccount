package com.cyanogenmod.id.gcm.model;

public class WipeStartedMessage extends EncryptedMessage {
    private final String command = "wipe_started";

    private Params params;

    private static class Params {
        private int sequence;

        public Params(int sequence) {
            this.sequence = sequence;
        }
    }

    public void setSequence(int sequence) {
        this.params = new Params(sequence);
    }
}
