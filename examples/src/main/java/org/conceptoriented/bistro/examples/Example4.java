package org.conceptoriented.bistro.examples;

import org.conceptoriented.bistro.core.Column;
import org.conceptoriented.bistro.core.Schema;
import org.conceptoriented.bistro.core.Table;

import java.io.IOException;

public class Example4 {

    public static String location = "src/main/resources/ex1";

    public static Schema schema;

    public static void main(String[] args) throws IOException {

        //
        // Create schema
        //

        schema = new Schema("Example 2");

        //
        // Create tables and columns by loading data from CSV files
        //

        Table columnType = schema.getTable("Object");

        Table items = ExUtils.readFromCsv(schema, location, "OrderItems.csv");

        Table products = ExUtils.readFromCsv(schema, location, "Products.csv");



        Table categories = schema.createTable("Categories");
        categories.prod(); // This table will be populated by using data from other tables

        Column categoriesName = schema.createColumn("Name", categories, columnType);
        categoriesName.noop(true); // Key columns specify where the data for this table comes from

        //
        // Calculate amount
        //

        // [OrderItems].[Amount] = [Quantity] * [Unit Price]
        Column itemsAmount = schema.createColumn("Amount", items, columnType);
        itemsAmount.calc(
                p -> Double.valueOf((String)p[0]) * Double.valueOf((String)p[1]),
                items.getColumn("Quantity"), items.getColumn("Unit Price")
        );

        //
        // Link from OrderItems to Products
        //

        // [OrderItems].[Product]: OrderItems -> Products
        Column itemsProduct = schema.createColumn("Product", items, products);
        itemsProduct.link(
                new Column[] { items.getColumn("Product ID") },
                products.getColumn("ID")
        );

        // [Products].[Cat]: Products -> Categories
        Column productsCategory = schema.createColumn("Cat", products, categories);
        productsCategory.proj(
                new Column[] { products.getColumn("Category") },
                categories.getColumn("Name") // Only key columns can be specified here
        );

        //
        // Accumulate item characteristics
        //

        // [Products].[Total Amount] = SUM [OrderItems].[Amount]
        Column productsAmount = schema.createColumn("Total Amount", products, columnType);
        productsAmount.setDefaultValue(0.0); // It will be used as an initial value
        productsAmount.accu(
                itemsProduct,
                p -> (double)p[0] + (double)p[1], // [Amount] + [out]
                items.getColumn("Amount")
        );

        // [Categories].[Total Amount] = SUM [Products].[Total Amount]
        Column categoriesAmount = schema.createColumn("Total Amount", categories, columnType);
        categoriesAmount.setDefaultValue(0.0); // It will be used as an initial value
        categoriesAmount.accu(
                productsCategory,
                p -> (double)p[0] + (double)p[1], // [Amount] + [out]
                products.getColumn("Total Amount")
        );

        //
        // Evaluate and read values
        //

        schema.eval();

        Object value;

        value = itemsAmount.getValue(32); // value = 533.75 = 25 * 21.35
        value = itemsProduct.getValue(32); // value = 3

        value = productsAmount.getValue(3); // value = 533.75 * 1 item
        value = productsCategory.getValue(3); // value = 2

        value = categoriesAmount.getValue(2); // value = 533.75 * 1 product
        value = categoriesName.getValue(2); // value = "Oil"
    }

}
