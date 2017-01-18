# UrsviksInventory
Allow Ursviks small store to keep track of its inventory of products by storing products information in a SQLite Database, including picture, name, price, supplier email and current quantity of products. 

Implemented ContentProvider to read(query), create(insert), update and delete all products' information from the database. 

Implemented CursorAdapter to display all products' information in the ListView of main screen.

Add one product information by clicking the ADD button at the bottom of the main screen.

Add or change product's picture by taking photo or choosing from library. 

Implemented Runtime Permission Handling for Android 6.0+ and Android API 23+. Also implemented Permission Handling for early versions of Android. 

Type in product's name, price, supplier email and current quantity. 

Implemented Option Menu both in Main screen and Edit Screen.

Implemented sanity check: If picture is empty, the ImageView on the single item view is invisible. Name, price and current quantity cannot be empty. Supplier email must be valid if provided. Numbers cannot be negative. Otherwise a Toast message will be displayed. 

Click each product item in the ListView of main screen to edit this product's information. 

Toast message shows at the bottom to indicate user whether the saving or updating product information is successful or not. 

Track products' current quantities by tracking the number of sale products or receive products. Click the SALE button on each single item view to track the sale of one product and store the current quantity into the database. In the edit view, type in sale quantity or receive quantity to track large amount sale or receive. 

Toast message shows at the bottom if there are not enough products for sale. 

Click Order button in the edit mode to open the Email App to send an order email to the product's supplier using product's information in the database. 

Ask user confirmation before deleting a product's or all products' information. 

Warn user about losing unsaved changes. 

An empty state is implemented when there is no products' information in the database. Also show a message in the empty view to instruct user how to start adding a product's information into the database. 

# App ScreenShots
<img src="screenshots/0.main_screen.png" width="24%"/> <img src="screenshots/1.main_screen.png" width="24%"/> <img src="screenshots/4.product_not_enough.png" width="24%"/> <img src="screenshots/5.add_product.png" width="24%"/> <img src="screenshots/6.choose_photo.png" width="24%"/> <img src="screenshots/7.gallery.png" width="24%"/> <img src="screenshots/8.sanity_check_name.png" width="24%"/> <img src="screenshots/9.sanity_check_price.png" width="24%"/> <img src="screenshots/10.sanity_check_email.png" width="24%"/> <img src="screenshots/11.sanity_check_quantity.png" width="24%"/> <img src="screenshots/12.product_saved.png" width="24%"/> <img src="screenshots/13.product_updated.png" width="24%"/> <img src="screenshots/14.edit_mode_option_menu.png" width="24%"/> <img src="screenshots/15.edit_quantity.png" width="24%"/> <img src="screenshots/16.product_not_enough.png" width="24%"/> <img src="screenshots/17.coca_cola.png" width="24%"/> <img src="screenshots/18.email_intent.png" width="24%"/> <img src="screenshots/19.email.png" width="24%"/> <img src="screenshots/20.delete_all.png" width="24%"/> <img src="screenshots/21.delete_one.png" width="24%"/> <img src="screenshots/22.unsaved_changes.png" width="24%"/> <img src="screenshots/23.empty_view.png" width="24%"/>
