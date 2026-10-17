package cmc.mody.admin.domain;

public enum AdminRole {
    VIEWER, OPERATOR, ADMIN;

    public boolean canWrite() {
        return this != VIEWER;
    }
}
