package it.eng.idsa.businesslogic.web.rest;

import it.eng.idsa.businesslogic.service.HashService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.*;

/**
 * 
 * @author Milan Karajovic and Gabriele De Luca
 *
 */

@RestController
@EnableAutoConfiguration
@RequestMapping({ "/about" })
public class UtilResource {
	//@Autowired
	//BuildProperties buildProperties;

	@GetMapping("/version")
    @ResponseBody
    public String getVersion() {
        return "1.0";
    }


}
