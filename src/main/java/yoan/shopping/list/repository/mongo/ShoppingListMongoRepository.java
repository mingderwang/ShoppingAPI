/**
 * 
 */
package yoan.shopping.list.repository.mongo;

import static yoan.shopping.infra.db.mongo.MongoDocumentConverter.FIELD_ID;
import static yoan.shopping.infra.rest.error.Level.ERROR;
import static yoan.shopping.infra.util.error.CommonErrorCode.APPLICATION_ERROR;
import static yoan.shopping.list.repository.ShoppingListRepositoryErrorMessage.PROBLEM_CREATION_LIST;
import static yoan.shopping.list.repository.ShoppingListRepositoryErrorMessage.PROBLEM_UPDATE_LIST;
import static yoan.shopping.list.repository.mongo.ShoppingListMongoConverter.FIELD_OWNER_ID;

import java.util.List;
import java.util.UUID;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yoan.shopping.infra.db.Dbs;
import yoan.shopping.infra.db.mongo.MongoDbConnectionFactory;
import yoan.shopping.infra.util.error.ApplicationException;
import yoan.shopping.list.ShoppingList;
import yoan.shopping.list.repository.ShoppingListRepository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

/**
 * Mongo implementation of the shopping list repository
 * @author yoan
 */
@Singleton
public class ShoppingListMongoRepository extends ShoppingListRepository {
	public static final String LIST_COLLECTION = "list";
	
	private final MongoCollection<ShoppingList> listCollection;
	private final ShoppingListMongoConverter listConverter;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingListMongoRepository.class);
	
	@Inject
	public ShoppingListMongoRepository(MongoDbConnectionFactory mongoConnectionFactory) {
		listCollection = mongoConnectionFactory.getCollection(Dbs.SHOPPING, LIST_COLLECTION, ShoppingList.class);
		listConverter = new ShoppingListMongoConverter();
	}
	
	@Override
	protected void processCreate(ShoppingList listToCreate) {
		try {
			listCollection.insertOne(listToCreate);
		} catch(MongoException e) {
			String message = PROBLEM_CREATION_LIST.getDevReadableMessage(e.getMessage());
			LOGGER.error(message, e);
			throw new ApplicationException(ERROR, APPLICATION_ERROR, message, e);
		}
	}

	@Override
	protected ShoppingList processGetById(UUID listId) {
		Bson filter = Filters.eq(FIELD_ID, listId);
		return listCollection.find().filter(filter).first();
	}

	@Override
	protected void processUpdate(ShoppingList listToUpdate) {
		Bson filter = Filters.eq(FIELD_ID, listToUpdate.getId());
		Bson update = listConverter.getListUpdate(listToUpdate);
		try {
			listCollection.updateOne(filter, update);
		} catch(MongoException e) {
			String message = PROBLEM_UPDATE_LIST.getDevReadableMessage(e.getMessage());
			LOGGER.error(message, e);
			throw new ApplicationException(ERROR, APPLICATION_ERROR, message, e);
		}
	}

	@Override
	protected void processDeleteById(UUID listId) {
		Bson filter = Filters.eq(FIELD_ID, listId);
		listCollection.deleteOne(filter);
	}

	@Override
	protected ImmutableList<ShoppingList> processGetByOwner(UUID ownerId) {
		Bson filter = Filters.eq(FIELD_OWNER_ID, ownerId);
		List<ShoppingList> lists = Lists.newArrayList();
		lists = listCollection.find().filter(filter).into(lists);
		
		return ImmutableList.<ShoppingList>copyOf(lists);
	}

}
