package de.rwth_aachen.phyphox.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.udojava.evalex.Expression;
import java.math.BigDecimal;
import de.rwth_aachen.phyphox.R;

public class CalculatorActivity extends AppCompatActivity {

    private EditText display;
    private StringBuilder expression = new StringBuilder(); // Holds the full expression
    private static final String PREFS_NAME = "CalculatorPrefs";
    private static final String LAST_EXPRESSION = "lastExpression";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        display = findViewById(R.id.display);

        // Restore the saved expression from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastInput = prefs.getString(LAST_EXPRESSION, "");
        display.setText(lastInput);
        expression.append(lastInput); // Rebuild the expression string

        // Number buttons
        Button btn0 = findViewById(R.id.btn_0);
        Button btn1 = findViewById(R.id.btn_1);
        Button btn2 = findViewById(R.id.btn_2);
        Button btn3 = findViewById(R.id.btn_3);
        Button btn4 = findViewById(R.id.btn_4);
        Button btn5 = findViewById(R.id.btn_5);
        Button btn6 = findViewById(R.id.btn_6);
        Button btn7 = findViewById(R.id.btn_7);
        Button btn8 = findViewById(R.id.btn_8);
        Button btn9 = findViewById(R.id.btn_9);

        // Operation buttons
        Button btnAdd = findViewById(R.id.btn_add);
        Button btnSub = findViewById(R.id.btn_subtract);
        Button btnMul = findViewById(R.id.btn_multiply);
        Button btnDiv = findViewById(R.id.btn_divide);
        Button btnEquals = findViewById(R.id.btn_equals);
        Button btnClear = findViewById(R.id.btn_clear);
        Button btnDelete = findViewById(R.id.btn_delete);  // Delete button
        Button btnDot = findViewById(R.id.btn_dot);        // Decimal point button

        // Set click listeners for number buttons
        View.OnClickListener numberClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = (Button) v;
                display.append(b.getText().toString());
                expression.append(b.getText().toString());
            }
        };

        btn0.setOnClickListener(numberClickListener);
        btn1.setOnClickListener(numberClickListener);
        btn2.setOnClickListener(numberClickListener);
        btn3.setOnClickListener(numberClickListener);
        btn4.setOnClickListener(numberClickListener);
        btn5.setOnClickListener(numberClickListener);
        btn6.setOnClickListener(numberClickListener);
        btn7.setOnClickListener(numberClickListener);
        btn8.setOnClickListener(numberClickListener);
        btn9.setOnClickListener(numberClickListener);

        // Add logic for decimal point button
        btnDot.setOnClickListener(v -> {
            display.append(".");
            expression.append(".");
        });

        // Set click listeners for operation buttons
        btnAdd.setOnClickListener(v -> handleOperation("+"));
        btnSub.setOnClickListener(v -> handleOperation("-"));
        btnMul.setOnClickListener(v -> handleOperation("*"));
        btnDiv.setOnClickListener(v -> handleOperation("/"));

        // Equals button logic
        btnEquals.setOnClickListener(v -> evaluate());

        // Clear button logic
        btnClear.setOnClickListener(v -> clearDisplay());

        // Delete/Backspace button logic
        btnDelete.setOnClickListener(v -> {
            String currentText = display.getText().toString();
            if (currentText.length() > 0) {
                display.setText(currentText.substring(0, currentText.length() - 1));
                expression.deleteCharAt(expression.length() - 1);  // Remove last char from expression
            }
        });
    }

    private void handleOperation(String operator) {
        // Append operator to the expression only if the last character is not already an operator
        if (expression.length() > 0 && !isOperator(expression.charAt(expression.length() - 1))) {
            display.append(operator);
            expression.append(operator);
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private void evaluate() {
        try {
            // Use EvalEx to evaluate the expression
            Expression expressionEval = new Expression(expression.toString());
            BigDecimal result = expressionEval.eval();

            // Display the result and clear the expression
            display.setText(result.toString());
            expression.setLength(0);  // Clear expression
            expression.append(result.toString());  // Allow further calculations on the result

        } catch (Exception e) {
            display.setText("Error");
            expression.setLength(0);  // Clear expression on error
        }
    }

    private void clearDisplay() {
        display.setText("");
        expression.setLength(0);  // Clear the expression

        // Clear the saved value in SharedPreferences
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove(LAST_EXPRESSION)
                .apply();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Save the current input value to SharedPreferences
        String currentInput = display.getText().toString();
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(LAST_EXPRESSION, currentInput)
                .apply();
    }
}
