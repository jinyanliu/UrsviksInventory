package se.sugarest.jane.ursviksinventory;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class CatalogActivity extends AppCompatActivity {

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

    }
}
