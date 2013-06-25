package com.cyanogenmod.id.gcm;

public class GCMessage {

    private Account account;
    private String command;

    public Account getAccount() {
        return account;
    }

    public String getCommand() {
        return command;
    }

    public String dump() {
        StringBuffer sb = new StringBuffer("command="+command);
        if (account != null) {
            sb.append("\n")
              .append("id="+account.id)
              .append("\n")
              .append("username="+account.username)
              .append("\n")
              .append("email="+account.email)
              .append("\n")
              .append("first_name="+account.first_name)
              .append("\n")
              .append("last_name="+account.last_name);
        }
        return sb.toString();
    }

    public static class Account {
        private String id;
        private String username;
        private String first_name;
        private String last_name;
        private String email;

        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstName() {
            return first_name;
        }

        public String getLastName() {
            return last_name;
        }
    }
}
