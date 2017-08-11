package com.openshift.evg.roadshow.parks.rest;

import com.openshift.evg.roadshow.rest.gateway.model.Backend;
import com.openshift.evg.roadshow.rest.gateway.model.Coordinates;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provides information about this backend
 *
 * Created by jmorales on 26/09/16.
 */
@RequestMapping("/ws/info")
@RestController
public class BackendController{
    
    @Autowired
    private Environment env;
    
    @RequestMapping(method = RequestMethod.GET, value = "/", produces = "application/json")
    public Backend get() {
        String whichProdVersion = env.getProperty("prod.env.version", "Not Configured");
        return new Backend("nationalparks","National Parks - " + whichProdVersion, new Coordinates("47.039304", "14.505178"), 4);
    }
}
