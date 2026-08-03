package com.devrick.pos.security.bootstrap;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final BootstrapAdminService bootstrapAdminService;

    public BootstrapAdminInitializer(BootstrapAdminService bootstrapAdminService) {
        this.bootstrapAdminService = bootstrapAdminService;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrapAdminService.bootstrap();
    }
}
