package com.openclassrooms.tourguide.helper;

import lombok.Getter;

/**
 * InternalTestHelper is a utility class that provides a way to set the number of internal users
 * for testing purposes. This can be useful for performance testing or simulating high user volumes.
 */
public class InternalTestHelper {

    // Set this default up to 100,000 for testing
    @Getter
    private static int internalUserNumber = 100;

    /**
     * Sets the number of internal users for testing.
     *
     * @param internalUserNumber the number of internal users to set
     */
    public static void setInternalUserNumber(int internalUserNumber) {
        InternalTestHelper.internalUserNumber = internalUserNumber;
    }
}