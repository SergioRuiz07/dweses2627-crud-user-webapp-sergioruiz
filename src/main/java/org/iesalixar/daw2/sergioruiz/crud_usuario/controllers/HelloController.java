package org.iesalixar.daw2.sergioruiz.crud_usuario.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public String hello(@RequestParam(value="name",defaultValue = "world")String name){
        logger.info("Request received to/hello endpoint with parameter name: {}",name);
        String greeting = String.format("Hello %s!",name);
        logger.debug("Greeting message to be returned: {}",greeting);
        return greeting;
    }
}