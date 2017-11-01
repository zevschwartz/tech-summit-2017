package com.example.conferenceclient;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
@EnableFeignClients
@EnableCircuitBreaker
@EnableBinding(Source.class)
public class ConferenceClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConferenceClientApplication.class, args);
    }
}

@RestController
@Data
class Controller {

    private final ClientService service;

    @GetMapping("/user")
    public List<Person> findAllUsers() {
        return service.userList();
    }

    @GetMapping("easy/user")
    public List<Person> findAllUsersEasy() {
        return service.easyList();
    }

    @PostMapping("/user/{firstName}/{lastName}")
    public Person createUser(@PathVariable String firstName, @PathVariable String lastName) {
        Person p = new Person();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        return service.createPerson(p);
    }

    @PostMapping("cloud/user/{firstName}/{lastName}")
    public void createCloudUser(@PathVariable String firstName, @PathVariable String lastName) {
        Person p = new Person();
        p.setFirstName(firstName);
        p.setLastName(lastName);
        service.createPersonInTheCloud(p);
    }



}

@Service
@Data
@Slf4j
class ClientService {
    @Value("${summit.url:http://localhost:8081/conference-service}")
    private String backingServerUrl;
    private final RemoteService remoteService;
    private final RemoteServiceGateWay remoteServiceGateWay;

    @HystrixCommand(fallbackMethod = "giveUp")
    public List<Person> userList() {
        return new RestTemplate().getForObject(backingServerUrl + "/person", List.class);
    }

    public List<Person> easyList() {
        return remoteService.getAllUsers();
    }

    public Person createPerson(Person p) {
        return remoteService.createPerson(p);
    }

    public void createPersonInTheCloud(Person p){
        remoteServiceGateWay.createNew(p.getFirstName()+"|"+p.getLastName());
    }

    List<Person> giveUp() {
        log.info("I got nothing");
        return new ArrayList<>();
    }

}

@FeignClient(name = "remote", url = "${summit.url:http://localhost:8081/conference-service}")
interface RemoteService {

    @GetMapping("/person")
    List<Person> getAllUsers();

    @PostMapping("/person")
    Person createPerson(@RequestBody Person p);
}

@MessagingGateway
interface RemoteServiceGateWay{
    @Gateway(requestChannel = Source.OUTPUT)
    void createNew(String personName);
}

@Data
class Person {
    private UUID id;
    private String firstName;
    private String lastName;
}