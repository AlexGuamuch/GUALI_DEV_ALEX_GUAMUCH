package edu.umg.guali.compilador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CompiladorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompiladorApplication.class, args);
    }
}
