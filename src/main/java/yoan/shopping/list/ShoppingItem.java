package yoan.shopping.list;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static yoan.shopping.list.ItemState.TO_BUY;

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import yoan.shopping.infra.db.WithId;
import yoan.shopping.infra.util.GenericBuilder;

import com.google.common.base.MoreObjects;

/**
 * Shopping item
 * @author yoan
 */
public class ShoppingItem implements Bson, WithId {
	/** Default item ID */
	public static final UUID DEFAULT_ID = UUID.fromString("ced72f58-26fd-493f-9126-b8c122dfeeae");
	/** Default shopping item */
	public static final ShoppingItem DEFAULT = Builder.createDefault().build();
	
	/** item unique ID */
	private final UUID id;
	/** Item name */
	private final String name;
	/** Quantity of this item */
	private final int quantity;
	/** Current item state */
	private final ItemState state;
	
	protected ShoppingItem(UUID id, String name, int quantity, ItemState state) {
		super();
		this.id = requireNonNull(id, "Item Id is mandatory");
		checkArgument(StringUtils.isNotBlank(name), "Invalid item name");
		this.name = name;
		this.quantity = quantity;
		this.state = requireNonNull(state, "Invalid item state");
	}
	
	public static class Builder implements GenericBuilder<ShoppingItem> {
		private UUID id = DEFAULT_ID;
		private String name = "Default name";
		private int quantity = 1;
		private ItemState state = TO_BUY;
		
		private Builder() { }
		
		/**
         * The default item is DEFAULT
         *
         * @return DEFAULT item
         */
        public static Builder createDefault() {
            return new Builder();
        }
        
        /**
         * Duplicate an existing builder
         *
         * @param otherBuilder
         * @return builder
         */
        public static Builder createFrom(final Builder otherBuilder) {
            Builder builder = new Builder();

            builder.id = otherBuilder.id;
            builder.name = otherBuilder.name;
            builder.quantity = otherBuilder.quantity;
            builder.state = otherBuilder.state;
            
            return builder;
        }
        
        /**
         * Get a builder based on an existing ShoppingItem instance
         *
         * @param user
         * @return builder
         */
        public static Builder createFrom(final ShoppingItem item) {
            Builder builder = new Builder();

            builder.id = item.id;
            builder.name = item.name;
            builder.quantity = item.quantity;
            builder.state = item.state;
            
            return builder;
        }
        
		@Override
		public ShoppingItem build() {
			return new ShoppingItem(id, name, quantity, state);
		}
		
		public Builder withId(UUID id) {
            this.id = requireNonNull(id);
            return this;
        }

        /**
         * Set a random item ID
         *
         * @return builder
         */
        public Builder withRandomId() {
            this.id = UUID.randomUUID();
            return this;
        }
        
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder withQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }
        
        public Builder withState(ItemState state) {
            this.state = state;
            return this;
        }
	}

	@Override
	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getQuantity() {
		return quantity;
	}

	public ItemState getState() {
		return state;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(id, name, quantity, state);
	}

	@Override
	public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ShoppingItem that = (ShoppingItem) obj;
        return Objects.equals(this.id, that.id)
            && Objects.equals(this.name, that.name)
            && Objects.equals(this.quantity, that.quantity)
            && Objects.equals(this.state, that.state);
    }
	
	@Override
	public final String toString() {
		return MoreObjects.toStringHelper(this)
			.add("id", id).add("name", name)
			.add("quantity", quantity)
			.add("state", state)
			.toString();
	}

	@Override
	public <TDocument> BsonDocument toBsonDocument(Class<TDocument> documentClass, CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<ShoppingItem>(this, codecRegistry.get(ShoppingItem.class));
	}
}