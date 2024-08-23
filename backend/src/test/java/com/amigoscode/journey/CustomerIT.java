package com.amigoscode.journey;

import com.amigoscode.TestConfig;
import com.amigoscode.TestSecurityConfig;
import com.amigoscode.customer.CustomerDTO;
import com.amigoscode.customer.CustomerRegistrationRequest;
import com.amigoscode.customer.CustomerUpdateRequest;
import com.amigoscode.customer.Gender;
import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
@Import({TestSecurityConfig.class, TestConfig.class}) // Import the custom test security configuration
@TestConfiguration
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public class CustomerIT {

    @Autowired
    private WebTestClient webTestClient;

    private static final Random RANDOM = new Random();
    private static final String CUSTOMER_PATH = "/api/v1/customers";

    @Test
    void canRegisterCustomer() {
        Faker faker = new Faker();
        Name fakerName = faker.name();

        String name = fakerName.fullName();
        String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@amigoscode.com";
        int age = RANDOM.nextInt(1, 100);

        Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                name, email, "password", age, gender
        );

        // Send a post request
        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH + "/register") // Ensure the correct endpoint
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst(AUTHORIZATION);

        // Get all customers
        List<CustomerDTO> allCustomers = webTestClient.get()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(CustomerDTO.class)
                .returnResult()
                .getResponseBody();

        int id = allCustomers.stream()
                .filter(customer -> customer.email().equals(email))
                .map(CustomerDTO::id)
                .findFirst()
                .orElseThrow();

        // Make sure that the customer is present
        CustomerDTO expectedCustomer = new CustomerDTO(
                id,
                name,
                email,
                gender,
                age,
                List.of("ROLE_USER"),
                email,
                null
        );

        assertThat(allCustomers).contains(expectedCustomer);

        // Get customer by id
        webTestClient.get()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CustomerDTO.class)
                .isEqualTo(expectedCustomer);
    }

    @Test
    void canDeleteCustomer() {
        Faker faker = new Faker();
        Name fakerName = faker.name();

        String name = fakerName.fullName();
        String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@amigoscode.com";
        int age = RANDOM.nextInt(1, 100);

        Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

        CustomerRegistrationRequest request1 = new CustomerRegistrationRequest(
                name, email, "password", age, gender
        );

        CustomerRegistrationRequest request2 = new CustomerRegistrationRequest(
                name, email + ".uk", "password", age, gender
        );

        // Send a post request to create customer 1
        webTestClient.post()
                .uri(CUSTOMER_PATH + "/register")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request1), CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk();

        // Send a post request to create customer 2
        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH + "/register")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request2), CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst(AUTHORIZATION);

        // Get all customers
        List<CustomerDTO> allCustomers = webTestClient.get()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(CustomerDTO.class)
                .returnResult()
                .getResponseBody();

        int id = allCustomers.stream()
                .filter(customer -> customer.email().equals(email))
                .map(CustomerDTO::id)
                .findFirst()
                .orElseThrow();

        // Customer 2 deletes customer 1
        webTestClient.delete()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();

        // Customer 2 gets customer 1 by id
        webTestClient.get()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void canUpdateCustomer() {
        Faker faker = new Faker();
        Name fakerName = faker.name();

        String name = fakerName.fullName();
        String email = fakerName.lastName() + "-" + UUID.randomUUID() + "@amigoscode.com";
        int age = RANDOM.nextInt(1, 100);

        Gender gender = age % 2 == 0 ? Gender.MALE : Gender.FEMALE;

        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
                name, email, "password", age, gender
        );

        // Send a post request
        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH + "/register")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst(AUTHORIZATION);

        // Get all customers
        List<CustomerDTO> allCustomers = webTestClient.get()
                .uri(CUSTOMER_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(CustomerDTO.class)
                .returnResult()
                .getResponseBody();

        int id = allCustomers.stream()
                .filter(customer -> customer.email().equals(email))
                .map(CustomerDTO::id)
                .findFirst()
                .orElseThrow();

        // Update customer
        String newName = "Ali";

        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                newName, null, null
        );

        webTestClient.put()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateRequest), CustomerUpdateRequest.class)
                .exchange()
                .expectStatus()
                .isOk();

        // Get customer by id
        CustomerDTO updatedCustomer = webTestClient.get()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CustomerDTO.class)
                .returnResult()
                .getResponseBody();

        CustomerDTO expected = new CustomerDTO(
                id, newName, email, gender, age, List.of("ROLE_USER"), email, null
        );

        assertThat(updatedCustomer).isEqualTo(expected);
    }
}
