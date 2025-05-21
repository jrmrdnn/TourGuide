package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.dto.AttractionDto;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.location.VisitedLocation;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tripPricer.Provider;

import java.util.List;

/**
 * TourGuideController is a REST controller that provides endpoints for the Tour Guide application.
 * It allows users to get their location, nearby attractions, rewards, and trip deals.
 */
@RestController
@AllArgsConstructor
public class TourGuideController {

    TourGuideService tourGuideService;

    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }

    @RequestMapping("/getLocation")
    public VisitedLocation getLocation(@RequestParam String userName) {
        return tourGuideService.getUserLocation(getUser(userName));
    }

    @RequestMapping("/getNearbyAttractions")
    public List<AttractionDto> getNearbyAttractions(@RequestParam String userName) {
        return tourGuideService.getListAttractionsDto(userName);
    }

    @RequestMapping("/getRewards")
    public List<UserReward> getRewards(@RequestParam String userName) {
        return tourGuideService.getUserRewards(getUser(userName));
    }

    @RequestMapping("/getTripDeals")
    public List<Provider> getTripDeals(@RequestParam String userName) {
        return tourGuideService.getTripDeals(getUser(userName));
    }

    /**
     * Helper method to retrieve a User object by username.
     *
     * @param userName the username of the user
     * @return the User object associated with the given username
     */
    private User getUser(String userName) {
        return tourGuideService.getUser(userName);
    }

}