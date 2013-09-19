/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.socialsignin.spring.data.dynamodb.repository.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.socialsignin.spring.data.dynamodb.repository.DynamoDBCrudRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.util.Assert;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.KeyPair;

/**
 * Default implementation of the
 * {@link org.springframework.data.repository.CrudRepository} interface.
 * 
 * @author Michael Lavelle
 * 
 * @param <T>
 *            the type of the entity to handle
 * @param <ID>
 *            the type of the entity's identifier
 */
public class SimpleDynamoDBCrudRepository<T, ID extends Serializable> implements DynamoDBCrudRepository<T, ID> {

	protected DynamoDBMapper dynamoDBMapper;

	protected DynamoDBEntityInformation<T, ID> entityInformation;

	protected Class<T> domainType;

	public SimpleDynamoDBCrudRepository(DynamoDBEntityInformation<T, ID> entityInformation,
			DynamoDBMapper dynamoDBMapper) {
		Assert.notNull(entityInformation);
		Assert.notNull(dynamoDBMapper);
		this.entityInformation = entityInformation;
		this.dynamoDBMapper = dynamoDBMapper;
		this.domainType = entityInformation.getJavaType();
	}

	@Override
	public T findOne(ID id) {
		if (entityInformation.isRangeKeyAware()) {
			return dynamoDBMapper.load(domainType, entityInformation.getHashKey(id), entityInformation.getRangeKey(id));
		} else {
			return dynamoDBMapper.load(domainType, entityInformation.getHashKey(id));
		}
	}

	@SuppressWarnings("unchecked")
	public List<T> findAll(Iterable<ID> ids) {
		Map<Class<?>, List<KeyPair>> keyPairsMap = new HashMap<Class<?>, List<KeyPair>>();
		List<KeyPair> keyPairs = new ArrayList<KeyPair>();
		for (ID id : ids) {
			if (entityInformation.isRangeKeyAware()) {
				keyPairs.add(new KeyPair().withHashKey(entityInformation.getHashKey(id)).withRangeKey(
						entityInformation.getRangeKey(id)));
			} else {
				keyPairs.add(new KeyPair().withHashKey(id));
			}
		}
		keyPairsMap.put(domainType, keyPairs);
		return (List<T>) dynamoDBMapper.batchLoad(keyPairsMap).get(domainType);
	}

	protected T load(ID id) {
		if (entityInformation.isRangeKeyAware()) {
			return dynamoDBMapper.load(domainType, entityInformation.getHashKey(id), entityInformation.getRangeKey(id));
		} else {
			return dynamoDBMapper.load(domainType, entityInformation.getHashKey(id));
		}
	}

	@SuppressWarnings("unchecked")
	protected List<T> loadBatch(Iterable<ID> ids) {
		Map<Class<?>, List<KeyPair>> keyPairsMap = new HashMap<Class<?>, List<KeyPair>>();
		List<KeyPair> keyPairs = new ArrayList<KeyPair>();
		for (ID id : ids) {
			if (entityInformation.isRangeKeyAware()) {
				keyPairs.add(new KeyPair().withHashKey(entityInformation.getHashKey(id)).withRangeKey(
						entityInformation.getRangeKey(id)));
			} else {
				keyPairs.add(new KeyPair().withHashKey(id));

			}
		}
		keyPairsMap.put(domainType, keyPairs);
		return (List<T>) dynamoDBMapper.batchLoad(keyPairsMap).get(domainType);
	}

	@Override
	public <S extends T> S save(S entity) {
		dynamoDBMapper.save(entity);
		return entity;
	}

	@Override
	public <S extends T> List<S> save(Iterable<S> entities) {
		final List<S> entityList = new ArrayList<S>();
		for (S entity : entities) {
			entityList.add(entity);
		}
		dynamoDBMapper.batchSave(entityList);
		return entityList;
	}

	@Override
	public boolean exists(ID id) {

		Assert.notNull(id, "The given id must not be null!");
		return findOne(id) != null;
	}

	@Override
	public List<T> findAll() {

		DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		return dynamoDBMapper.scan(domainType, scanExpression);
	}

	@Override
	public long count() {
		final DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
		return dynamoDBMapper.count(domainType, scanExpression);
	}

	@Override
	public void delete(ID id) {

		Assert.notNull(id, "The given id must not be null!");

		T entity = findOne(id);
		if (entity == null) {
			throw new EmptyResultDataAccessException(String.format("No %s entity with id %s exists!", domainType, id),
					1);
		}
		dynamoDBMapper.delete(entity);
	}

	@Override
	public void delete(T entity) {
		Assert.notNull(entity, "The entity must not be null!");
		dynamoDBMapper.delete(entity);
	}

	@Override
	public void delete(Iterable<? extends T> entities) {

		Assert.notNull(entities, "The given Iterable of entities not be null!");

		List<T> entityList = new ArrayList<T>();
		for (T entity : entities) {
			entityList.add(entity);
		}
		dynamoDBMapper.batchDelete(entityList);
	}

	@Override
	public void deleteAll() {
		dynamoDBMapper.batchDelete(findAll());
	}

}
