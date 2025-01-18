package com.mrmachine.springboot.webflux.app.models.seervices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mrmachine.springboot.webflux.app.models.dao.CategoryDAO;
import com.mrmachine.springboot.webflux.app.models.dao.ProductDAO;
import com.mrmachine.springboot.webflux.app.models.documents.Category;
import com.mrmachine.springboot.webflux.app.models.documents.Product;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductServiceImpl implements ProductService {
	
	@Autowired
	private ProductDAO dao;
	
	@Autowired
	private CategoryDAO categoryDao;

	@Override
	public Flux<Product> findAll() {
		return this.dao.findAll();
	}

	@Override
	public Mono<Product> findById(String id) {
		return this.dao.findById(id);
	}

	@Override
	public Mono<Product> save(Product p) {
		return this.dao.save(p);
	}

	@Override
	public Mono<Void> delete(Product p) {
		return this.dao.delete(p);
	}

	@Override
	public Flux<Product> findAllUpperCaseName() {
		return this.dao.findAll().map( p -> {
			p.setName(p.getName().toUpperCase());
			return p;
		});
	}

	@Override
	public Flux<Category> findAllCategory() {
		return this.categoryDao.findAll();
	}

	@Override
	public Mono<Category> findCategoryById(String id) {
		return this.categoryDao.findById(id);
	}

	@Override
	public Mono<Category> save(Category c) {
		return this.categoryDao.save(c);
	}

}
