package se.sugarest.jane.ursviksinventory.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import se.sugarest.jane.ursviksinventory.data.InventoryContract.InventoryEntry;

/**
 * Created by jane on 1/11/17.
 */

/**
 * {@link ContentProvider} for Inventory app.
 */
public class InventoryProvider extends ContentProvider {

    /**
     * Tag for the log messages
     * Since we'll be logging multiple times throughout this file, it would be ideal to create
     * a log tag as a global constant variable, so all log messages from the InventoryProvider
     * will have the same log tag identifier when reading the system logs.
     */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    /**
     * URI matcher code for the content URI for the inventory table
     */
    private static final int INVENTORY = 100;

    /**
     * URI matcher code for the content URI for a single inventory in the inventory table
     */
    private static final int INVENTORY_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        /**
         * The calls to addURI() go here, for all of the content URI patterns that the provider
         * should recognize. All paths added to the UriMatcher have a corresponding code to return
         *  when a match is found.
         *
         *  The content URI of the form "se.sugarest.jane.ursviksinventory/inventory" will map to
         *  the integer code {@link #INVENTORY}. This URI is used to provide access to MULTIPLE rows
         *  of the inventory table.
         */
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, INVENTORY);

        /**
         * In this case, the "#" wildcard is used where "#" can be substituted for an integer.
         * For example, "se.sugarest.jane.ursviksinventory/inventory/3" matches, but
         * "se.sugarest.jane.ursviksinventory/inventory" (without a number at the end) doesn't match.
         */
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", INVENTORY_ID);
    }

    /**
     * Database helper that will provide us access to the database.
     */
    private InventoryDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        // To access the database, instantiate the subclass of SQLiteOpenHelper
        // and pass the context, which is the current activity.
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection arguments,
     * and sort order.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // For the INVENTORY code, query the inventory table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the pets table.
                // Perform database query on inventory table.
                cursor = database.query(
                        InventoryEntry.TABLE_NAME, // The table to query
                        projection,                // The columns to return
                        selection,                 // The columns for the WHERE clause
                        selectionArgs,             // The values for the WHERE clause
                        null,                      // Don't group the rows
                        null,                      // Don't filter by row groups
                        sortOrder);                // The sort order
                break;
            case INVENTORY_ID:
                // For the INVENTORY_ID code, extract out the ID from the URI.
                // For an example URI such as "se.sugarest.jane.ursviksinventory/inventory/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element iin the selection
                // argument that will fill int he "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                // This will perform a query on the inventory table where the _id equals 3 to return
                // a Cursor containing that row of the table.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertInventory(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert an inventory into the database with the given content values. Return the new content
     * URI for that specific row int he database.
     */
    private Uri insertInventory(Uri uri, ContentValues values) {

        // Check that the name is not null
        String name = values.getAsString(InventoryEntry.COLUMN_INVENTORY_NAME);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Product requires a name");
        }

        // Check that the price is not null and it's greater than or equal to 0 kr
        Integer price = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_PRICE);
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Product requires a valid price");
        }

        // Check that the quantity is not null and it's greater than or equal to 0
        Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Product requires a valid quantity");
        }

        // No need to check picture, any value is valid(including null).

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert a new inventory into the inventory database table with the given ContentValues
        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);

        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the pet content URI
        // uri: content://se.sugarest.jane.ursviksinventory/inventory
        getContext().getContentResolver().notifyChange(uri, null);

        // Once knowing the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }



}
