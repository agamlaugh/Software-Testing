package uk.ac.ed.ilp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Delivery path for a single dispatch
 * Contains delivery ID and flight path (array of LngLat coordinates)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPath {
    private Integer deliveryId; // ID from MedDispatchRec
    private List<LngLat> flightPath; // Array of LngLat coordinates
}

