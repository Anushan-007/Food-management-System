package citybites.data;

import citybites.model.CartItem;
import java.util.ArrayList;

/**
 * In-memory session store.
 * Only the cart is held here — food items, customers and orders
 * are persisted in MySQL via the DAO / service layer.
 */
public class DataStore {

    /** The current customer's shopping cart (cleared after order is confirmed). */
    public static final ArrayList<CartItem> cartItems = new ArrayList<>();

    private DataStore() {}
}
