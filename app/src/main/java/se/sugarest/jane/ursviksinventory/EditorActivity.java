package se.sugarest.jane.ursviksinventory;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import se.sugarest.jane.ursviksinventory.data.InventoryContract.InventoryEntry;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by jane on 1/10/17.
 */

/**
 * Allows user to create a new product or edit an exiting one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int REQUEST_CAMERA = 1;
    public static final int SELECT_FILE = 2;
    /**
     * Identifier for the inventory data loader
     */
    private static final int EXISTING_INVENTORY_LOADER = 0;
    /**
     * Current URI for the existing project(null if it's a new product)
     */
    private Uri mCurrentProductUri;
    /**
     * ImageView field to add the product's picture
     */
    private ImageView mPictureImageView;
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
     * sale Button to minus current quantity
     */
    private Button mSaleButton;
    /**
     * receive Button to plus current quantity
     */
    private Button mReceiveButton;
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

    private static Bitmap scaleImage(Bitmap image) {
        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();

        final int MAX_SIZE = 2048;//http://stackoverflow.com/questions/7428996/hw-accelerated-activity-how-to-get-opengl-texture-size-limit

        while (imageHeight > MAX_SIZE || imageWidth > MAX_SIZE) {
            imageHeight = imageHeight / 2;
            imageWidth = imageWidth / 2;
        }

        return Bitmap.createScaledBitmap(image, imageWidth, imageHeight, true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mPictureImageView = (ImageView) findViewById(R.id.edit_product_image);
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mCurrentQuantityEditText = (EditText) findViewById(R.id.edit_product_current_quantity);
        mSaleQuantityEditText = (EditText) findViewById(R.id.edit_product_sale_quantity);
        mReceiveQuantityEditText = (EditText) findViewById(R.id.edit_product_receive_quantity);
        mSaleButton = (Button) findViewById(R.id.edit_product_minus_button);
        mReceiveButton = (Button) findViewById(R.id.edit_product_plus_button);

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
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
            // Invalidate the minus and plus current quantity function.
            // (It doesn't make sense to plus or minus a product's quantity when is hasn't beem created yet.)
            mSaleQuantityEditText.setVisibility(View.INVISIBLE);
            mReceiveQuantityEditText.setVisibility(View.INVISIBLE);
            mSaleButton.setVisibility(View.INVISIBLE);
            mReceiveButton.setVisibility(View.INVISIBLE);
        } else {
            // Otherwise this is an existing product, so change the app bar to say "Edit a Product"
            setTitle(getString(R.string.editor_activity_title_edit_product));

            /**
             * Initializes the CursorLoader. The INVENTORY_LOADER value is eventually passed to onCreateLoader().
             */
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know id there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mPictureImageView.setOnTouchListener(mTouchListener);
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mCurrentQuantityEditText.setOnTouchListener(mTouchListener);
        mSaleQuantityEditText.setOnTouchListener(mTouchListener);
        mReceiveQuantityEditText.setOnTouchListener(mTouchListener);

        // Find new photo Button and select Image.
        Button newPhotoButton = (Button) findViewById(R.id.button_new_photo);
        newPhotoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                selectImage();
            }
        });

        // Find order Button and sent intent to send email
        Button orderButton = (Button) findViewById(R.id.edit_product_order_button);
        orderButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // Only email apps should handle this
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
                View anotherview = getCurrentFocus();
                if (anotherview != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(anotherview.getWindowToken(), 0);
                }
            }
        });

        mSaleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int productQuantity = Integer.parseInt(mCurrentQuantityEditText.getText().toString());
                if (!mSaleQuantityEditText.getText().toString().isEmpty()) {
                    int saleQuantity = Integer.parseInt(mSaleQuantityEditText.getText().toString());

                    if (saleQuantity >= 0) {

                        if (productQuantity > 0 && productQuantity >= saleQuantity) {
                            // update current quantity EditText View
                            mCurrentQuantityEditText.setText(String.valueOf(productQuantity - saleQuantity));
                            // update DB
                            ContentValues values = new ContentValues();
                            values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, productQuantity - saleQuantity);
                            getContentResolver().update(mCurrentProductUri, values, null, null);
                        } else {
                            Toast.makeText(getBaseContext(), R.string.sale_button_no_item, Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(getBaseContext(), R.string.invalid_sale_quantity, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getBaseContext(), R.string.sale_quantity_cannot_be_empty, Toast.LENGTH_SHORT).show();
                }
            }
        });

        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int productQuantity = Integer.parseInt(mCurrentQuantityEditText.getText().toString());
                if (!mReceiveQuantityEditText.getText().toString().isEmpty()) {
                    int receiveQuantity = Integer.parseInt(mReceiveQuantityEditText.getText().toString());

                    if (receiveQuantity >= 0) {

                        // update current quantity EditText View
                        mCurrentQuantityEditText.setText(String.valueOf(productQuantity + receiveQuantity));
                        // update DB
                        ContentValues values = new ContentValues();
                        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, productQuantity + receiveQuantity);
                        getContentResolver().update(mCurrentProductUri, values, null, null);
                    } else {
                        Toast.makeText(getBaseContext(), R.string.invalid_receive_quantity, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getBaseContext(), R.string.receive_quantity_cannot_be_empty, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Get user input editor and save new product into database.
     */
    private void saveProduct() {

        String nameString = mNameEditText.getText().toString().trim();
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        if (nameString.isEmpty()) {
            throw new IllegalArgumentException(getResources().getString(R.string.name_cannot_be_empty));
        }
        String priceString = mPriceEditText.getText().toString().trim();
        if (priceString.isEmpty()) {
            throw new IllegalArgumentException(getResources().getString(R.string.price_cannot_be_empty));
        }
        String currentQuantityString = mCurrentQuantityEditText.getText().toString().trim();
        if (currentQuantityString.isEmpty()) {
            throw new IllegalArgumentException(getResources().getString(R.string.current_quantity_cannot_be_empty));
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

        // Determine if this is a new or existing product by checking if mCurrentProductUri is null
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu option from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                try {
                    saveProduct();
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this.getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    return true;
                }
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                /**
                 * If the product hasn't changed, continue with navigating up to parent activity
                 * which is the {@link CatalogActivity}
                 */
                if (!mProductHasChanged) {
                    // Navigate back to parent activity (CatalogActivity)
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise, if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes.
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise, if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that
        // changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show a dialog that notifies the user they have unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        // Since the editor shows all products attributes, define a projection that contains
        // all columns from the inventory table
        String[] projection = {
                InventoryEntry.COLUMN_INVENTORY_NAME,
                InventoryEntry.COLUMN_INVENTORY_PICTURE,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(
                this,                       // Parent activity context
                mCurrentProductUri,         // Query the content URI for the current product
                projection,                 // Columns to include in the resulting Cursor
                null,                       // No selection clause
                null,                       // No selection arguments
                null);                      // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of inventory attributes that we're interested in
            int pictureColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PICTURE);
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);

            // Extract out the value from the Cursor for the given column index
            byte[] imgByte = cursor.getBlob(pictureColumnIndex);
            if (imgByte != null) {
                Bitmap productPicture = BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length);
                mPictureImageView.setImageBitmap(productPicture);
            }

            String productName = cursor.getString(nameColumnIndex);
            int productPrice = cursor.getInt(priceColumnIndex);
            int productQuantity = cursor.getInt(quantityColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(productName);
            mPriceEditText.setText(String.valueOf(productPrice));
            mCurrentQuantityEditText.setText(String.valueOf(productQuantity));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mCurrentQuantityEditText.setText("");
        mSaleQuantityEditText.setText("");
        mReceiveQuantityEditText.setText("");
        mPictureImageView.setImageBitmap(null);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when the user confirms
     *                                   they want to discard their changes
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
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

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
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

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(EditorActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {

                int permissionCheck = ContextCompat.checkSelfPermission(EditorActivity.this,
                        Manifest.permission.CAMERA);

                if (items[item].equals("Take Photo")) {
                    //userChoosenTask="Take Photo";
                    if (permissionCheck == PERMISSION_GRANTED) {
                        cameraIntent();
                    }
                } else if (items[item].equals("Choose from Library")) {
                    //userChoosenTask="Choose from Library";
                    if (permissionCheck == PERMISSION_GRANTED)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {

        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        bm = scaleImage(bm);

        mPictureImageView.setImageBitmap(bm);
    }

    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mPictureImageView.setImageBitmap(thumbnail);
    }

}

