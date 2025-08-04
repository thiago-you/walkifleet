package you.thiago.walkifleet.mock;

import you.thiago.walkifleet.Random;

public final class Util {
    private Util() {
        // Prevent instantiation
    }

    public static String getRandomUser() {
        return "USER" + (Random.nextInt(19) + 1);
    }
}
