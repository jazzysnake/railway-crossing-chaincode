package hu.bme;

public enum CrossingState {
    FREE_TO_CROSS, LOCKED;

    public static CrossingState fromString(String state) {
        if (state.equals(FREE_TO_CROSS.name())) {
            return CrossingState.FREE_TO_CROSS;
        }
        return CrossingState.LOCKED;
    }
}