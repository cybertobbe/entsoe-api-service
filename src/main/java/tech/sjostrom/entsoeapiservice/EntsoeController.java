package tech.sjostrom.entsoeapiservice;


import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entsoe")
public class EntsoeController {

    @Autowired
    private ProducerTemplate producerTemplate;

    @GetMapping("/fetch")
    public String fetchNow() {
        producerTemplate.sendBody("direct:fetch-now", null);
        return "Fetch triggered";
    }
}
