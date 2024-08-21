package com.alexisa.tire_reserve.config;

import com.alexisa.tire_reserve.model.domain.enums.ServiceableVehicle;
import com.alexisa.tire_reserve.model.domain.enums.WorkshopId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.config.import=classpath:workshops-test.yaml")
public class WorkshopPropertiesInjectionIntegrationTest {

    @Autowired
    private WorkshopsProperties workshopsProperties;

    @Test
    public void testWorkshopsPropertiesBinding() {

        var workshopsMap = workshopsProperties.map();
        assertThat(workshopsMap).isNotNull();
        assertThat(workshopsMap).containsKey("manchester");

        var manchester = workshopsMap.get("manchester");

        assertThat(manchester.workshopId()).isEqualTo(WorkshopId.MANCHESTER);
        assertThat(manchester.apiPath()).isEqualTo("made up api path");
        assertThat(manchester.city()).isEqualTo("testCity");
        assertThat(manchester.address()).isEqualTo("testAddress");
        assertThat(manchester.phoneNumber()).isEqualTo("testNr");
        assertThat(manchester.email()).isEqualTo("test.manchester@tirereserve.com");

        var serviceableVehicles = manchester.serviceableVehicles();
        assertThat(serviceableVehicles).contains(ServiceableVehicle.CAR);
    }
}
