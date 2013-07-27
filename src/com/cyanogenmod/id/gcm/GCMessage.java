package com.cyanogenmod.id.gcm;

public class GCMessage {

    private Account account;
    private Args args;
    private String command;
    private String token;

    public Account getAccount() {
        return account;
    }

    public String getCommand() {
        return command;
    }

    public Args getArgs() {
        return args;
    }

    public String getToken() {
        return token;
    }

    public String dump() {
        StringBuffer sb = new StringBuffer("command="+command)
                .append("\n")
                .append("token="+token);
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
        if (args != null) {
            sb.append("\n")
                    .append("args.command="+args.command)
                    .append("\n")
                    .append("sdcard="+args.sdcard);
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

    public static class Args {
        private String command;
        private boolean sdcard;

        public String getCommand() {
            return command;
        }

        public boolean isSdcard() {
            return sdcard;
        }
    }
}
