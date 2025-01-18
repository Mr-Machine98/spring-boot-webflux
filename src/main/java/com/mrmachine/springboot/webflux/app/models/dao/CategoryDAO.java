package com.mrmachine.springboot.webflux.app.models.dao;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.mrmachine.springboot.webflux.app.models.documents.Category;

public interface CategoryDAO extends ReactiveMongoRepository<Category, String> {

}
