package com.example.conferenceservice;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.http.HttpStatus;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootApplication
@Slf4j
@EnableBinding(Sink.class)
public class ConferenceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConferenceServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(PersonRepository repo) {
		return args -> {
			Stream.of("Harry|Potter", "Ron|Weasley", "Lord|Voldemort", "Hermione|Granger", "Albus|Dumbledore",
					"Severus|Snape").forEach(s -> {
				String[] names = s.toLowerCase().split("\\|");
				Person p = new Person();
				p.setFirstName(names[0]);
				p.setLastName(names[1]);
				log.info("saving person {}", p);
				repo.save(p);
				log.info("saved person {}", p);
			});

		};
	}
}



@RefreshScope
@RestController
@Data
class Controller {

	private final Props props;
	private final PersonService personService;

	@GetMapping("hello")
	public String hello() {
		return String.format("Hello Tech Summit %s Attendees!", props.getYear());
	}

	@GetMapping("person")
	public Iterable<Person> getUsers() {
		return personService.findAll();
	}

	@GetMapping("person/byname")
	public Person findByName(@RequestParam String fname){
		return personService.findByName(fname).orElseThrow(()->new IllegalStateException("Cannot find "+fname));
	}

	@PostMapping("person")
	public Person createPerson(@RequestBody Person p){
		return personService.createPerson(p);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String error(Exception e){
		return e.getMessage();
	}

}

@Component
@ConfigurationProperties(prefix = "summit")
@Data
class Props {
	private String year;
}

@Data
@Service
class PersonService {
	private final PersonRepository personRepository;

	Iterable<Person> findAll() {
		return personRepository.findAll();
	}

	Optional<Person> findByName(String name){
		return personRepository.findByFirstNameIgnoreCase(name);
	}


	public Person createPerson(Person p) {
		personRepository.save(p);
		return p;
	}
}

@RepositoryRestResource
interface PersonRepository extends CrudRepository<Person, UUID> {
	Optional<Person> findByFirstNameIgnoreCase(@Param("name") String firstName);
}

@Slf4j
@Data
@MessageEndpoint
class PersonProcessor{

	private final PersonService service;

	@ServiceActivator(inputChannel = Sink.INPUT)
	public void createPerson(String personName){
		log.info("received message with nme {}",personName);
		String[] name = personName.split("\\|");
		Person p = new Person();
		p.setFirstName(name[0]);
		p.setLastName(name[1]);
		service.createPerson(p);
	}
}

@Entity
@Data
class Person {
	@Id
	@GeneratedValue
	private UUID id;

	private String firstName;
	private String lastName;
}