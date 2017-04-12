
/**
 * Add your package below. Package name can be found in the project's AndroidManifest.xml file.
 * This is the package name our example uses:
 * <p>
 * package com.example.android.justjava;
 */
package com.example.android.justjava;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;

import static android.R.attr.name;

/**
 * This app displays an order form to order coffee.
 */
public class MainActivity extends AppCompatActivity {

    int quantity = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        displayQuantity(quantity);
    }

    /**
     * This method is called when the order button is clicked.
     */
    public void submitOrder(View view) {
        EditText nameEditText = (EditText) findViewById(R.id.name_edittext_view);
        String nameStr = nameEditText.getText().toString();
        CheckBox whipCreamCheckBox = (CheckBox) findViewById(R.id.whipcream_checkbox_view);
        boolean hasWhippedCream = whipCreamCheckBox.isChecked();
        CheckBox chocolateCheckBox = (CheckBox) findViewById(R.id.chocolate_checkbox_view);
        boolean hasChocolate = chocolateCheckBox.isChecked();
        int price = calculatePrice(hasWhippedCream, hasChocolate);
        String priceMessage = createOrderSummary (nameStr, price, hasWhippedCream, hasChocolate);
        String subjectStr = "Coffee order for " + nameStr;
        String emailStr[] = {"jkrichma@uci.edu"};
        composeEmail(emailStr, subjectStr, priceMessage);
        // displayMessage(priceMessage);
    }

    /**
     * This method is called when the "+" button is clicked.
     */
    public void increment(View view) {
        if (quantity < 100) {
            ++quantity;
        }
        else {
            Toast.makeText(getApplicationContext(), "Too much coffee!", Toast.LENGTH_LONG).show();
        }
        displayQuantity(quantity);
    }

    /**
     * This method is called when the "-" button is clicked.
     */
    public void decrement(View view) {

        if (quantity > 1) {
            --quantity;
        }
        else {
            Toast.makeText(getApplicationContext(), "Not enough coffee!", Toast.LENGTH_LONG).show();
        }
        displayQuantity(quantity);
    }

    /**
     * Calculates the price of the order.
     *
     * @return total price
     */
    private int calculatePrice(boolean hasWhip, boolean hasChoc) {
        int pricePerCoffee = 5;
        if (hasWhip) {
            pricePerCoffee += 1;
        }
        if (hasChoc) {
            pricePerCoffee += 2;
        }
        return quantity * pricePerCoffee;
    }

    /**
     * Creates a text string for the order summary
     *
     * @param price
     * @return string message with the order summary
     */
    private String createOrderSummary(String name, int price, boolean addWhippedCream, boolean addChocolate) {

        String orderStr = "Name: " + name + "\n";
        orderStr += "Add whipped cream? " + addWhippedCream + "\n";
        orderStr += "Add chocolate? " + addChocolate + "\n";
        orderStr += "Quantity: " + quantity + "\n";
        orderStr += "Total: $" + price + "\n";
        orderStr += "Thank you!";

        return orderStr;
    }

    public void composeEmail(String[] addresses, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }


    /**
     * This method displays the given quantity value on the screen.
     */
    private void displayQuantity(int numberOfCoffees) {
        TextView quantityTextView = (TextView) findViewById(R.id.quantity_text_view);
        quantityTextView.setText("" + numberOfCoffees);
    }

    /**
     * This method displays the given text on the screen.
     */
    private void displayMessage(String message) {
        TextView orderSummaryTextView = (TextView) findViewById(R.id.order_summary_text_view);
        orderSummaryTextView.setText(message);
    }
}
