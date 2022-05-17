package hu.bme;

public enum RequesterRole {
    TRAIN, CAR;

    public static RequesterRole fromString(String role) {
        if (role.equals(TRAIN.name())) {
            return RequesterRole.TRAIN;
        }
        return RequesterRole.CAR;
    }
}
