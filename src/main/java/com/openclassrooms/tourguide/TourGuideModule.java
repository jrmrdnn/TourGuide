package com.openclassrooms.tourguide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.service.RewardsService;

/**
 * Configuration class for the Tour Guide module. It defines and manages the essential beans
 * required for the functionality of the application, including services related to GPS utilities
 * and rewards management.
 */
@Configuration
public class TourGuideModule {

    /**
     * Creates and returns a GpsUtil bean, which provides utilities for GPS-related services such as
     * retrieving user locations and nearby attractions.
     *
     * @return a new instance of GpsUtil
     */
    @Bean
    public GpsUtil getGpsUtil() {
        return new GpsUtil();
    }

    /**
     * Creates and returns a RewardsService bean. The RewardsService is responsible for
     * calculating rewards for users based on their visited locations and nearby attractions.
     * It utilizes GpsUtil for accessing GPS data and RewardCentral for managing reward points.
     *
     * @return a new instance of RewardsService
     */
    @Bean
    public RewardsService getRewardsService() {
        return new RewardsService(getGpsUtil(), getRewardCentral());
    }

    /**
     * Creates and returns a RewardCentral bean. RewardCentral is used for managing the reward points
     * associated with attractions visited by users.
     *
     * @return a new instance of RewardCentral
     */
    @Bean
    public RewardCentral getRewardCentral() {
        return new RewardCentral();
    }

}
