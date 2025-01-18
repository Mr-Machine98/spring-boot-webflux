package com.mrmachine.springboot.webflux.app.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mrmachine.springboot.webflux.app.models.documents.Product;
import com.mrmachine.springboot.webflux.app.models.seervices.ProductService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {
	
	@Autowired
	private ProductService service;
	
	private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);

	@GetMapping("/all")
	public Flux<Product> all() {
		LOG.info("Find products:");
		Flux<Product> products = this.service
			.findAllUpperCaseName()
			.doOnNext( p -> LOG.info(p.toString()));
		return products;
	}
	
	@GetMapping("/{id}")
	public Mono<Product> findById(@PathVariable(name = "id") String id) {		
		Mono<Product> product = this.findById(id);
		return product;
	}

}
