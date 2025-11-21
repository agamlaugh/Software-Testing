package uk.ac.ed.ilp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ed.ilp.model.Drone;
import uk.ac.ed.ilp.model.DroneForServicePoint;
import uk.ac.ed.ilp.model.RestrictedArea;
import uk.ac.ed.ilp.model.ServicePoint;

import java.util.List;

/**
 * HTTP client service to fetch data from ILP REST service
 * Fetches fresh data on every call 
 */
@Service
public class IlpRestClient {

    private final String ilpEndpoint;
    private final RestTemplate restTemplate;

    @Autowired
    public IlpRestClient(String ilpEndpoint) {
        this.ilpEndpoint = ilpEndpoint;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetch all drones from ILP REST service
     * Endpoint: /drones
     */
    public List<Drone> fetchDrones() {
        String url = ilpEndpoint + "drones";
        ResponseEntity<List<Drone>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Drone>>() {}
        );
        return response.getBody();
    }

    /**
     * Fetch all service points from ILP REST service
     * Endpoint: /service-points
     */
    public List<ServicePoint> fetchServicePoints() {
        String url = ilpEndpoint + "service-points";
        ResponseEntity<List<ServicePoint>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ServicePoint>>() {}
        );
        return response.getBody();
    }

    /**
     * Fetch all restricted areas from ILP REST service
     * Endpoint: /restricted-areas
     */
    public List<RestrictedArea> fetchRestrictedAreas() {
        String url = ilpEndpoint + "restricted-areas";
        ResponseEntity<List<RestrictedArea>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<RestrictedArea>>() {}
        );
        return response.getBody();
    }

    /**
     * Fetch drones for service points from ILP REST service
     * Endpoint: /drones-for-service-points
     */
    public List<DroneForServicePoint> fetchDronesForServicePoints() {
        String url = ilpEndpoint + "drones-for-service-points";
        ResponseEntity<List<DroneForServicePoint>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<DroneForServicePoint>>() {}
        );
        return response.getBody();
    }
}

