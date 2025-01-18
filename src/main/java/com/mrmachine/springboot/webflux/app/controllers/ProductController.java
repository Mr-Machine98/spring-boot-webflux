package com.mrmachine.springboot.webflux.app.controllers;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

import com.mrmachine.springboot.webflux.app.models.documents.Category;
import com.mrmachine.springboot.webflux.app.models.documents.Product;
import com.mrmachine.springboot.webflux.app.models.seervices.ProductService;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@SessionAttributes("product")
@Controller
public class ProductController {
	
	@Autowired
	private ProductService service;
	
	@Value("${config.uploads.path}")
	private String path;
	
	private static final Logger LOG = LoggerFactory.getLogger(ProductController.class);
	
	// This method works to send additional information to Model View
	@ModelAttribute("categories")
	public Flux<Category> categories() {
		return this.service.findAllCategory();
	}
	
	@GetMapping("/uploads/img/{picture:.+}")
	public Mono<ResponseEntity<Resource>> viewPicture(@PathVariable String picture) throws MalformedURLException {
		
		Path p = Paths
				.get(this.path)
				.resolve(picture)
				.toAbsolutePath();
		
		Resource img = new UrlResource(p.toUri());
		
		return Mono.just(
			ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + img.getFilename() +"\"")
				.body(img)
		);
	}
	
	//Details method
	@GetMapping("/detail/{id}")
	public Mono<String> detail(Model model, @PathVariable String id) {
		return this.service
			.findById(id)
			.doOnNext(p -> {
				model.addAttribute("product", p);
				model.addAttribute("title", "Details Product");
			})
			.switchIfEmpty(Mono.just(new Product()))
			.flatMap( p -> {
				if (p.getId() == null) {
					return Mono.error(new InterruptedException("The Product doesn't exixts"));
				}
				return Mono.just(p);
			})
			.then(Mono.just("detail"))
			.onErrorResume(ex -> Mono.just("redirect:/all"));
	}
	
	@GetMapping({"/all", "/findAllProducts", "/"})
	private Mono<String> findAll(Model model) {
		
		Flux<Product> products = this.service
				.findAllUpperCaseName();
		products.subscribe(p -> LOG.info("find product -> " + p.getName() + " ,picture " + p.getPicture()));
		model.addAttribute("products", products);
		model.addAttribute("title", "List of Products");
		
		return Mono.just("listProducts");
	}
	
	@GetMapping("/form")
	private Mono<String> create(Model model) {
		model.addAttribute("product", new Product());
		model.addAttribute("title", "Product Form");
		model.addAttribute("botom", "Create");
		return Mono.just("form");
	}
	
	@GetMapping("/form/{id}")
	public Mono<String> edit(@PathVariable String id, Model model) {
		Mono<Product> product = this.service
			.findById(id)
			.doOnNext(p -> LOG.info("Edit Method - Product found -> " + p))
			.defaultIfEmpty(new Product());
		model.addAttribute("title", "Edit Product");
		model.addAttribute("botom", "Update");
		model.addAttribute("product", product);
		return Mono.just("form");
	}
	
	@PostMapping("/form")
	private Mono<String> save(@Valid Product product, BindingResult result, SessionStatus status, Model model, @RequestPart(name = "file") FilePart file) {
		
		if (result.hasErrors()) {
			
			model.addAttribute("title", "Save process has failed.");
			model.addAttribute("botom", "Save");
			return Mono.just("form");
			
		} else {	
			
			status.setComplete();
			if (product.getCreateAt() == null) {
				product.setCreateAt(LocalDate.now());
			}
			
			LOG.info(product.toString());
			
			Mono<Category> category = service.findCategoryById(product.getCategory().getId());
			return category.flatMap(c -> {
				
				if (!file.filename().isEmpty()) {
					product
						.setPicture(
							 UUID.randomUUID().toString() + file.filename()
								.replace(" ", "")
								.replace(":", "")
								.replace("\\", "")
						);
				}
				
				product.setCategory(c);
				return this.service.save(product);
			})
			.doOnNext(p -> LOG.info("Saving -> " + p))
			.flatMap(p -> {
				if (!file.filename().isEmpty()) {
					LOG.info("Picture about product -> " + p.getPicture());
					return file
							.transferTo(new File(this.path + p.getPicture()));
				}
				return Mono.empty();
			})
			.thenReturn("redirect:/all");
		}
		
	}
	
	@GetMapping("/delete/{id}")
	private Mono<String> delete(@PathVariable String id) {
		return this.service
				.findById(id)
				.defaultIfEmpty(new Product())
				.flatMap(p -> {
					if (p.getId() == null) {
						LOG.error("There had been a mistake when it has ittempted to find Product.");
						return Mono.error(new InterruptedException("Product doesn't exist."));
					}
					return Mono.just(p);
				})
				.flatMap(p -> this.service.delete(p))
				.then(Mono.just("redirect:/all"))
				.onErrorResume(ex -> Mono.just("redirect:/all"));
	}
	
	@GetMapping("/all-datadriver")
	private String findAllDataDriver(Model model) {
		
		Flux<Product> products = this.service
				.findAll()
				.map( p -> {
					p.setName(p.getName().toUpperCase());
					return p;
				})
				.delayElements(Duration.ofSeconds(1));
	
		products.subscribe(p -> LOG.info("find product -> " + p.getName()));

		model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 2));
		model.addAttribute("title", "List of Products");
		
		return "listProducts";
	}
	
	@GetMapping("/all-chunkfull")
	private String findAllChunkfull(Model model) {
		
		Flux<Product> products = this.service
				.findAll()
				.map( p -> {
					p.setName(p.getName().toUpperCase());
					return p;
				})
				.repeat(5000);
	
		products.subscribe(p -> LOG.info("find product -> " + p.getName()));

		model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 2));
		model.addAttribute("title", "List of Products");
		
		return "listProducts";
	}
	
	@GetMapping("/all-chunked")
	private String findAllChunked(Model model) {
		
		Flux<Product> products = this.service
				.findAll()
				.map( p -> {
					p.setName(p.getName().toUpperCase());
					return p;
				})
				.repeat(5000);
	
		products.subscribe(p -> LOG.info("find product -> " + p.getName()));

		model.addAttribute("products", new ReactiveDataDriverContextVariable(products, 2));
		model.addAttribute("title", "List of Products");
		
		return "listProducts-chunked";
	}

}
