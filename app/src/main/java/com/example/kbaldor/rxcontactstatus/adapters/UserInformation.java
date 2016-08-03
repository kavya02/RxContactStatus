package com.example.kbaldor.rxcontactstatus.adapters;

/**
 * Created by root on 30.07.2016.
 */
public class UserInformation {
    String userName;
    boolean isOnline;

    public UserInformation(String userName, boolean isOnline) {
        this.userName = userName;
        this.isOnline = isOnline;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setIsOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
