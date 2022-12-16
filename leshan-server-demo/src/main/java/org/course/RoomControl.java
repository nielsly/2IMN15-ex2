/*
 *  Extension to leshan-server-demo for application code.
 */

package org.course;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.io.PrintWriter;

import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.SingleObservation;


public class RoomControl {

    //
    // Static reference to the server.
    //
    private static LeshanServer lwServer;

    // 2IMN15:  TODO  : fill in
    // Declare variables to keep track of the state of the room.
    private static String roomName;
    private static int maxPeakRoomPower;
    private static Map<String, Integer> peakPowerMap;
    private static Map<String, Registration> registrationMap;

    public static void Initialize(LeshanServer server) {
        // Register the LWM2M server object for future use
        lwServer = server;

        // Initialize the state variables.
        maxPeakRoomPower = 0;
        peakPowerMap = new HashMap<>();
        registrationMap = new HashMap<>();
    }

    //
    // Suggested support methods:
    //
    // * set the dim level of all luminaires.
    // * set the power flag of all luminaires.
    // * show the status of the room.

    // Set dim level of all registered luminaires to dimLevel
    public static void setAllDim(int dimLevel) {
        registrationMap.forEach((key, value) -> {
            writeInteger(value, Constants.LUMINAIRE_ID, 0, Constants.RES_DIM_LEVEL, Math.max(0, Math.min(dimLevel, 100)));
        });
    }

    // Set power of all registered luminaires on/off
    public static void setAllPower(boolean power) {
        registrationMap.forEach((key, value) -> {
            writeBoolean(value, Constants.LUMINAIRE_ID, 0, Constants.RES_POWER, power);
        });
    }

    // Get room status since it was a suggested method, but I do not know how/where to use it
    public static void getRoomStatus() {
        //TODO scenario 2?
    }


    public static void handleRegistration(Registration registration) {
        // Check which objects are available.
        Map<Integer, org.eclipse.leshan.core.LwM2m.Version> supportedObject =
                registration.getSupportedObject();

        if (supportedObject.get(Constants.PRESENCE_DETECTOR_ID) != null) {
            System.out.println("Presence Detector");
            // Process the registration of a new Presence Detector.
            String presence = registerPresenceDetector(registration);
        }

        if (supportedObject.get(Constants.LUMINAIRE_ID) != null) {
            System.out.println("Luminaire");
            // Process the registration of a new Luminaire.
            int peakPower = registerLuminaire(registration);
        }

        if (supportedObject.get(Constants.DEMAND_RESPONSE_ID) != null) {
            System.out.println("Demand Response");
            // The registerDemandResponse() method contains example code
            // on how handle a registration.
            int powerBudget = registerDemandResponse(registration);
        }
    }


    public static void handleDeregistration(Registration registration) {
        //
        // 2IMN15:  TODO  :  fill in
        //
        // The device identified by the given registration will
        // disappear.  Update the state accordingly.

        // Check which objects are available.
        Map<Integer, org.eclipse.leshan.core.LwM2m.Version> supportedObject =
                registration.getSupportedObject();

        // If the deregistering object is a luminaire, update the map and max peak room power
        if (supportedObject.get(Constants.LUMINAIRE_ID) != null) {
            System.out.println("old maxPeakPower: " + maxPeakRoomPower);
            int peakPower = peakPowerMap.get(registration.getEndpoint());
            maxPeakRoomPower -= peakPower;
            peakPowerMap.remove(registration.getEndpoint());
            registrationMap.remove(registration.getEndpoint());
            System.out.println("Luminaire " + registration.getEndpoint() + " deregistered");
            System.out.println("new maxPeakPower: " + maxPeakRoomPower);
        } else {
            System.out.println("Other device " + registration.getEndpoint() + " deregistered");
        }
    }

    public static void handleObserveResponse(SingleObservation observation,
                                             Registration registration,
                                             ObserveResponse response) {
        if (registration != null && observation != null && response != null) {
            //
            // 2IMN15:  TODO  :  fill in
            //
            // When the registration and observation are known,
            // process the value contained in the response.
            int newPresence = observedPresenceDetector(observation, response);
            int newPeakPower = observedLuminaire(observation, response, registration);

            // For processing an update of the Demand Response object.
            // It contains some example code.
            int newPowerBudget = observedDemandResponse(observation, response);
            setAllDim((int)(((double) newPowerBudget/(double) maxPeakRoomPower)*100.0));
        }
    }

    private static String registerPresenceDetector(Registration registration) {
        String presence = readString(registration,
                Constants.PRESENCE_DETECTOR_ID,
                0,
                Constants.RES_PRESENCE);
        System.out.println(presence);
        System.out.println(registration.getEndpoint());

        // Observe presence information for updates.
        try {
            ObserveRequest obRequest =
                    new ObserveRequest(Constants.PRESENCE_DETECTOR_ID,
                            0,
                            Constants.RES_PRESENCE);
            System.out.println(">>ObserveRequest PD created << ");
            ObserveResponse coResponse =
                    lwServer.send(registration, obRequest, 1000);
            System.out.println(">>ObserveRequest PD sent << ");
            if (coResponse == null) {
                System.out.println(">>ObserveRequest null << ");
            }
        } catch (Exception e) {
            System.out.println("Observe request failed for Presence Detector.");
        }
        return presence;
    }

    // Registers a luminaire to the room and observes its updates.
    // Does not register it if it is of the wrong type.
    // Returns value of peak power, returns -1 if invalid type.
    private static int registerLuminaire(Registration registration) {
        String peakPower = readString(registration,
                Constants.LUMINAIRE_ID,
                0,
                Constants.RES_PEAK_POWER);
        System.out.println(peakPower);

        String type = readString(registration,
                Constants.LUMINAIRE_ID,
                0,
                Constants.RES_TYPE);


        // Register object in mapping and update peak room power if it is of a valid type
        if (type.equals("LED") || type.equals("Halogen")) {
            System.out.println("Correct type detected: " + type);
        } else {
            System.out.println("Unspecified type detected");
        }
        // Update registration maps and max peak room power
        int peakLong = Integer.parseInt(peakPower);
        System.out.println("old maxPeakRoomPower: " + maxPeakRoomPower);
        maxPeakRoomPower += peakLong;
        peakPowerMap.put(registration.getEndpoint(), peakLong);
        registrationMap.put(registration.getEndpoint(), registration);
        System.out.println("new maxPeakRoomPower: " + maxPeakRoomPower);

        // Observe peak power information for updates.
        try {
            ObserveRequest obRequest =
                    new ObserveRequest(Constants.LUMINAIRE_ID,
                            0,
                            Constants.RES_PEAK_POWER);
            System.out.println(">>ObserveRequest Lum created << ");
            ObserveResponse coResponse =
                    lwServer.send(registration, obRequest, 1000);
            System.out.println(">>ObserveRequest Lum sent << ");
            if (coResponse == null) {
                System.out.println(">>ObserveRequest null << ");
            }
        } catch (Exception e) {
            System.out.println("Observe request failed for Luminaire.");
        }
        return peakLong;
    }

    // Support functions for reading and writing resources of
    // certain types.

    // Returns the current power budget.
    private static int registerDemandResponse(Registration registration) {
        int powerBudget = readInteger(registration,
                Constants.DEMAND_RESPONSE_ID,
                0,
                Constants.RES_TOTAL_BUDGET);
        System.out.println("Power budget is " + powerBudget);

        // Observe the total budget information for updates.
        try {
            ObserveRequest obRequest =
                    new ObserveRequest(Constants.DEMAND_RESPONSE_ID,
                            0,
                            Constants.RES_TOTAL_BUDGET);
            System.out.println(">>ObserveRequest DR created << ");
            ObserveResponse coResponse =
                    lwServer.send(registration, obRequest, 1000);
            System.out.println(">>ObserveRequest DR sent << ");
            if (coResponse == null) {
                System.out.println(">>ObserveRequest null << ");
            }
        } catch (Exception e) {
            System.out.println("Observe request failed for Demand Response.");
        }
        return powerBudget;
    }

    // If the response contains a new presence, it returns that value.
    // Otherwise, it returns -1.
    private static int observedPresenceDetector(SingleObservation observation,
                                                ObserveResponse response) {
        // Alternative code:
        // String obsRes = observation.getPath().toString();
        // if (obsRes.equals("/33002/0/30005"))
        LwM2mPath obsPath = observation.getPath();
        if ((obsPath.getObjectId() == Constants.PRESENCE_DETECTOR_ID) &&
                (obsPath.getResourceId() == Constants.RES_PRESENCE)) {
            String strValue = ((LwM2mResource) response.getContent()).getValue().toString();
            try {
                int newPresence = -1;
                if (strValue.equals("false")) {
                    newPresence = 0;
                    // turn all luminaires off
                    setAllPower(false);
                } else if (strValue.equals("true")) {
                    newPresence = 1;
                    // turn all luminaires on
                    setAllPower(true);
                } else {
                    System.out.println("An error has occured in reading new presence");
                }
                System.out.println("New presence is " + newPresence);
                return newPresence;
            } catch (Exception e) {
                System.out.println("Exception in reading presence detector:" + e.getMessage());
            }
        }
        return -1;
    }

    // If the response contains a new peak power, it returns that value.
    // Otherwise, it returns -1.
    private static int observedLuminaire(SingleObservation observation,
                                         ObserveResponse response, Registration registration) {
        // Alternative code:
        // String obsRes = observation.getPath().toString();
        // if (obsRes.equals("/33002/0/30005"))
        LwM2mPath obsPath = observation.getPath();
        if ((obsPath.getObjectId() == Constants.LUMINAIRE_ID) &&
                (obsPath.getResourceId() == Constants.RES_PEAK_POWER)) {
            String strValue = ((LwM2mResource) response.getContent()).getValue().toString();
            try {
                int newPeakPower = Integer.parseInt(strValue);
                // update mapping and total peak room power
                maxPeakRoomPower -= peakPowerMap.get(registration.getEndpoint());
                maxPeakRoomPower += newPeakPower;
                peakPowerMap.put((registration.getEndpoint()), newPeakPower);

                System.out.println("Peak power is " + newPeakPower);
                System.out.println("maxPeakPower of room is: " + maxPeakRoomPower);
                return newPeakPower;
            } catch (Exception e) {
                System.out.println("Exception in reading luminaire:" + e.getMessage());
            }
        }
        return -1;
    }

    // If the response contains a new power budget, it returns that value.
    // Otherwise, it returns -1.
    private static int observedDemandResponse(SingleObservation observation,
                                              ObserveResponse response) {
        // Alternative code:
        // String obsRes = observation.getPath().toString();
        // if (obsRes.equals("/33002/0/30005"))
        LwM2mPath obsPath = observation.getPath();
        if ((obsPath.getObjectId() == Constants.DEMAND_RESPONSE_ID) &&
                (obsPath.getResourceId() == Constants.RES_TOTAL_BUDGET)) {
            String strValue = ((LwM2mResource) response.getContent()).getValue().toString();
            try {
                int newPowerBudget = Integer.parseInt(strValue);
                System.out.println("Power budget is " + newPowerBudget);

                return newPowerBudget;
            } catch (Exception e) {
                System.out.println("Exception in reading demand response:" + e.getMessage());
            }
        }
        return -1;
    }


    private static int readInteger(Registration registration, int objectId, int instanceId, int resourceId) {
        try {
            ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
            ReadResponse cResponse = lwServer.send(registration, request, 5000);
            if (cResponse.isSuccess()) {
                String sValue = ((LwM2mResource) cResponse.getContent()).getValue().toString();
                try {
                    int iValue = Integer.parseInt(((LwM2mResource) cResponse.getContent()).getValue().toString());
                    return iValue;
                } catch (Exception e) {
                }
                float fValue = Float.parseFloat(((LwM2mResource) cResponse.getContent()).getValue().toString());
                return (int) fValue;
            } else {
                return 0;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("readInteger: exception");
            return 0;
        }
    }

    private static String readString(Registration registration, int objectId, int instanceId, int resourceId) {
        try {
            ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
            ReadResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                String value = ((LwM2mResource) cResponse.getContent()).getValue().toString();
                return value;
            } else {
                return "";
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("readString: exception");
            return "";
        }
    }

    private static void writeInteger(Registration registration, int objectId, int instanceId, int resourceId, int value) {
        try {
            WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
            WriteResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                System.out.println("writeInteger: Success");
            } else {
                System.out.println("writeInteger: Failed, " + cResponse.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("writeInteger: exception");
        }
    }

    private static void writeString(Registration registration, int objectId, int instanceId, int resourceId, String value) {
        try {
            WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
            WriteResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                System.out.println("writeString: Success");
            } else {
                System.out.println("writeString: Failed, " + cResponse.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("writeString: exception");
        }
    }

    private static void writeBoolean(Registration registration, int objectId, int instanceId, int resourceId, boolean value) {
        try {
            WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
            WriteResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                System.out.println("writeBoolean: Success");
            } else {
                System.out.println("writeBoolean: Failed, " + cResponse.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("writeBoolean: exception");
        }
    }

}
