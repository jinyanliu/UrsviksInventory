package se.sugarest.jane.ursviksinventory;

/**
 * Created by jane on 1/11/17.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import se.sugarest.jane.ursviksinventory.data.InventoryContract.InventoryEntry;

import static se.sugarest.jane.ursviksinventory.data.InventoryContract.BASE_CONTENT_URI;
import static se.sugarest.jane.ursviksinventory.data.InventoryContract.PATH_INVENTORY;

/**
 * {@link InventoryCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of inventory data as its data source. This adapter knows
 * how to create list items for each row of inventory data in the {@Link Cursor}.
 */
public class InventoryCursorAdapter extends CursorAdapter {

    private int productQuantity;

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the inventory data (in the current row pointed to by curosr) to the given
     * list item layout. For example, the name for the current inventory can be set on the name
     * TextView in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(final View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout.
        ImageView pictureImageView = (ImageView) view.findViewById(R.id.list_item_picture);
        TextView nameTextView = (TextView) view.findViewById(R.id.list_item_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.list_item_price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.list_item_quantity);
        final Button saleButton = (Button) view.findViewById(R.id.list_item_button);

        // Find the columns of inventory attributes that we're interested in
        int pictureColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PICTURE);
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
        int rowIdColumnIndex = cursor.getColumnIndex(InventoryEntry._ID);
        int id = cursor.getInt(rowIdColumnIndex);

        final Uri newUri = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY + "/" + id);

        // Read the attributes from the Cursor for the current product
        byte[] imgByte = cursor.getBlob(pictureColumnIndex);

        if (imgByte != null) {
            Bitmap productPicture = BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length);
            // Update the ImageView with the attributes for the current product
            pictureImageView.setImageBitmap(productPicture);
        } else {
            // If the product picture is null, then set the imageview invisible
            pictureImageView.setVisibility(View.INVISIBLE);
        }

        String productName = cursor.getString(nameColumnIndex);
        int productPrice = cursor.getInt(priceColumnIndex);
        productQuantity = cursor.getInt(quantityColumnIndex);

        saleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (productQuantity > 0) {
                    productQuantity = productQuantity - 1;
                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, productQuantity);
                    context.getContentResolver().update(newUri, values, null, null);
                } else {
                    Toast.makeText(context, R.string.sale_button_no_item, Toast.LENGTH_SHORT).show();
                }
                Log.v("InventoryCursorAdapter", "productQuantity: " + productQuantity);
                quantityTextView.setText(String.valueOf(productQuantity));
                Log.v("button focusable?", "" + saleButton.isFocusable());
                Log.v("item focusable?", "" + view.isFocusable());
            }
        });

        // Update the TextViews with the attributes for the current product
        nameTextView.setText(productName);
        priceTextView.setText(String.valueOf(productPrice) + " kr");
        quantityTextView.setText(String.valueOf(productQuantity));
    }

}
