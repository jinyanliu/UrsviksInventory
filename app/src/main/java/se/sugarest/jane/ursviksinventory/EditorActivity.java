package se.sugarest.jane.ursviksinventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import se.sugarest.jane.ursviksinventory.data.InventoryContract.InventoryEntry;

/**
 * Created by jane on 1/10/17.
 */

/**
 * Allows user to create a new product or edit an exiting one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the inventory data loader
     */
    private static final int EXISTING_INVENTORY_LOADER = 0;

    /**
     * Current URI for the existing project(null if it's a new product)
     */
    private Uri mCurrentProductUri;

    /**
     * EditText field to enter the product's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the product's price
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the product's current quantity
     */
    private EditText mCurrentQuantityEditText;

    /**
     * EditText field to enter the product's sale quantity
     */
    private EditText mSaleQuantityEditText;

    /**
     * EditText field to enter the product's receive quantity
     */
    private EditText mReceiveQuantityEditText;

    /**
     * Boolean flag that keeps track of whether the product has been edited (true) or not (false)
     */
    private boolean mProductHasChanged = false;

    /**
     * OnTouchListener that listens for any user touched on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if users are creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // if the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle(getString(R.string.editor_activity_title_add_product));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing product, so change the app bar to say "Edit a Product"
            setTitle(getString(R.string.editor_activity_title_edit_product));

            /**
             * Initializes the CursorLoader. The INVENTORY_LOADER value is eventually passed to onCreateLoader().
             */
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mCurrentQuantityEditText = (EditText) findViewById(R.id.edit_product_current_quantity);
        mSaleQuantityEditText = (EditText) findViewById(R.id.edit_product_sale_quantity);
        mReceiveQuantityEditText = (EditText) findViewById(R.id.edit_product_receive_quantity);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know id there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mCurrentQuantityEditText.setOnTouchListener(mTouchListener);
        mSaleQuantityEditText.setOnTouchListener(mTouchListener);
        mReceiveQuantityEditText.setOnTouchListener(mTouchListener);
    }

    /**
     * Get user input editor and save new product into database.
     */
    private void saveProduct() {

        String nameString = mNameEditText.getText().toString().trim();
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        if (nameString.isEmpty()) {
            throw new IllegalArgumentException("Name can not be empty!");
        }
        String priceString = mPriceEditText.getText().toString().trim();
        if (priceString.isEmpty()) {
            throw new IllegalArgumentException("Price can not be empty!");
        }
        String currentQuantityString = mCurrentQuantityEditText.getText().toString().trim();
        if (currentQuantityString.isEmpty()) {
            throw new IllegalArgumentException("Current Quantity can not be empty!");
        }

        // Check if htis is supposed to be a new product and check if all the fields in the editor
        // are blank
        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(currentQuantityString)) {
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        int priceInt = Integer.parseInt(priceString);
        int currentQuantityInt = Integer.parseInt(currentQuantityString);

        // Create a ContentValues object where column names are the keys,
        // and user input attributes are the values.
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_INVENTORY_NAME, nameString);
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, priceInt);
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, currentQuantityInt);

        // Determine if this is a new or existing pet by checking if mCurrentProductUri is null
        // or not
        if (mCurrentProductUri == null) {

            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast with the row ID.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING product, so update the product with content URI:
            // mCurrentProductUri and pass in the new ContentValues. Pass in null for the selection
            // and selection args because mCurrentProductUri will already identify the correct row
            // in the database that we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_product_successful), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
