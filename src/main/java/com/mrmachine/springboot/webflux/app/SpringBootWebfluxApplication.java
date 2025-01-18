package com.mrmachine.springboot.webflux.app;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.mrmachine.springboot.webflux.app.models.documents.Category;
import com.mrmachine.springboot.webflux.app.models.documents.Product;
import com.mrmachine.springboot.webflux.app.models.seervices.ProductService;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {
	
	@Autowired
	private ProductService service;
	
	@Autowired
	private ReactiveMongoTemplate mongoTemplate;
	
	private static final Logger LOG = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		this.mongoTemplate
			.dropCollection("Products")
			.subscribe();
		
		this.mongoTemplate
		.dropCollection("Categories")
		.subscribe();
		
		Category pc = new Category("PC");
		Category smartphone = new Category("Smartphone");
		
		Flux.
			just(
				pc,
				smartphone
			)
			.flatMap(c -> service.save(c))
			.doOnNext(c -> LOG.info("Insert Category: " + c))
			.thenMany(
				Flux
				.just(
					new Product("Laptop", 1500.00, pc),
				    new Product("Smartphone", 899.99, smartphone),
				    new Product("Tablet", 499.99, smartphone),
				    new Product("Smartwatch", 199.99, smartphone),
				    new Product("Headphones", 79.99, smartphone),
				    new Product("Keyboard", 49.99, pc),
				    new Product("Mouse", 25.00, pc),
				    new Product("Monitor", 299.99, pc),
				    new Product("Printer", 159.99, pc),
				    new Product("External Hard Drive", 89.99, pc)
				)
				.flatMap(p -> {
					p.setCreateAt(LocalDate.now());
					return service.save(p); // This return a Mono flux then, we need using flatmap. 
				})	
					
			)
			.subscribe(p -> LOG.info("Insert Product: " + p));
	}

}