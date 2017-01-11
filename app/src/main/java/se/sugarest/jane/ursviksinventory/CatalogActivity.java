package se.sugarest.jane.ursviksinventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;

import se.sugarest.jane.ursviksinventory.data.InventoryContract.InventoryEntry;

/**
 * Displays list of products that were entered and stored in the app
 */
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Identifies a particular Loader being used in this component
    private static final int INVENTORY_LOADER = 0;

    InventoryCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Find the "ADD" button.
        Button addButton = (Button) findViewById(R.id.button_add_inventory);

        // Set a click listener on that view
        addButton.setOnClickListener(new Button.OnClickListener() {
            // The code in this method will be executed when the "ADD" button is clicked on.
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the products data
        ListView productListView = (ListView) findViewById(R.id.list_view_inventory);

        // Find and set empty view on the ListView, so that it only shows when the list has
        // 0 items.
        View emptyView = findViewById(R.id.empty_view);
        productListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of inventory data in the Cursor.
        // There is no inventory data yet (until the loader finished) so pass in null for the Cursor.
        mCursorAdapter = new InventoryCursorAdapter(this, null);
        productListView.setAdapter(mCursorAdapter);

        // Setup item click listener
        productListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /**
                 * Create new intent to go to {@link EditorActivity}
                 */
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);

                /**
                 * Form the content URI that represents the specific product that was clicked on,
                 * by appending the "id" (passed as input to this method) onto the
                 * {@link InventoryEntry#CONTENT_URI}.
                 * For example, the URI would be "content://se.sugarest.jane.ursviksinventory/inventory/2"
                 * if the pet with ID 2 was clicked on.
                 */
                Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentProductUri);

                /**
                 * Launch the {@link EditorActivity} to display the data for the current inventory.
                 */
                startActivity(intent);
            }
        });

        /**
         * Initializes the CursorLoader. The INVENTORY_LOADER value is eventually passed to onCreateLoader.
         */
        getLoaderManager().initLoader(INVENTORY_LOADER, null, this);
    }

    /**
     * Helper method to insert hardcoded inventory data into the database. For debugging purposes only.
     */
    private void insertProduct() {

        // Create a ContentValues object where column names are the keys,
        // and Peanuts' attributes are the values.
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_INVENTORY_NAME, "Peanuts");

        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_picture_peanuts);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        values.put(InventoryEntry.COLUMN_INVENTORY_PICTURE, byteArray);
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, "20");
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, "50");

        /**
         * Insert a new row for peanuts into the provider using the ContentResolver.
         * Use the {@link InventoryEntry#CONTENT_URI} to indicate that we want to insert
         * into inventory database table.
         * Receive the new content URI that will allow us to access peanuts' data in the future.
         */
        Uri newUri = getContentResolver().insert(
                InventoryEntry.CONTENT_URI,
                values);
    }

    /**
     * Helper method to delete all products in the database.
     */
    private void deleteAllProducts() {
        int rowsDeleted = getContentResolver().delete(InventoryEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " row deleted from inventory database");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertProduct();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                showDeleteConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback that's invoked when the system has initialized the Loader and is ready to start the
     * query. This is usually happens when initLoader() is called. The loaderID argument contains
     * the ID value passed to the initLoader() call.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_NAME,
                InventoryEntry.COLUMN_INVENTORY_PICTURE,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(
                this,                        // Parent activity context
                InventoryEntry.CONTENT_URI,  // Provider content URI to query
                projection,                  // Columns to include in the resulting Cursor
                null,                        // No selection clause
                null,                        // No selection arguments
                null);                       // Default sort order
    }

    /**
     * Defines the callback that CursorLoader calls when it's finished its query.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        /**
         *  Update {@link InventoryCursorAdapter} with this new cursor containing updated inventory data
         */
        mCursorAdapter.swapCursor(data);
    }

    /**
     * Invoked when the CursorLoader is being reset. For example, this is called if the data in the
     * provider changes and the Cursor becomes stale.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }

    /**
     * Prompt the user to confirm that they want to delete all pets.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete all products.
                deleteAllProducts();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
