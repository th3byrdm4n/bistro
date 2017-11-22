package org.conceptoriented.bistro.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Schema {

    private final UUID id;
    public UUID getId() {
        return id;
    }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    //
    // Tables
    //

    private List<Table> tables = new ArrayList<>();
    public List<Table> getTables() {
        return this.tables;
    }
    public Table getTable(String table) {
        Table ret = this.tables.stream().filter(x -> x.getName().equals(table)).findAny().orElse(null);
        return ret;
    }
    public Table getTableById(String id) {
        Table ret = this.tables.stream().filter(x -> x.getId().toString().equals(id)).findAny().orElse(null);
        return ret;
    }

    public Table createTable(String name) {
        Table tab = this.getTable(name);
        if(tab != null) return tab; // Already exists

        tab = new Table(this, name);
        this.tables.add(tab);
        return tab;
    }
    public void deleteTable(Table tab) {
        // Remove input keyColumns
        List<Column> inColumns = this.columns.stream().filter(x -> x.getInput().equals(tab)).collect(Collectors.<Column>toList());
        this.columns.removeAll(inColumns);

        // Remove output keyColumns
        List<Column> outColumns = this.columns.stream().filter(x -> x.getOutput().equals(tab)).collect(Collectors.<Column>toList());
        this.columns.removeAll(outColumns);

        // Remove table itself
        this.tables.remove(tab);
    }

    //
    // Columns
    //

    private List<Column> columns = new ArrayList<>();
    public List<Column> getColumns() {
        return this.columns;
    }
    public List<Column> getColumns(Table table) {
        List<Column> res = this.columns.stream().filter(x -> x.getInput().equals(table)).collect(Collectors.<Column>toList());
        return res;
    }
    public List<Column> getColumns(String table) {
        List<Column> res = this.columns.stream().filter(x -> x.getInput().getName().equals(table)).collect(Collectors.<Column>toList());
        return res;
    }
    public Column getColumn(Table table, String column) {
        Column ret = this.columns.stream().filter(x -> x.getInput().equals(table) && x.getName().equals(column)).findAny().orElse(null);
        return ret;
    }
    public Column getColumn(String table, String column) {
        Column ret = this.columns.stream().filter(x -> x.getInput().getName().equals(table) && x.getName().equals(column)).findAny().orElse(null);
        return ret;
    }
    public Column getColumnById(String id) {
        Column ret = this.columns.stream().filter(x -> x.getId().toString().equals(id)).findAny().orElse(null);
        return ret;
    }

    public Column createColumn(String name, Table  input) {
        return this.createColumn(name, input, null);
    }
    public Column createColumn(String name, Table  input, Table output) {
        if(output == null) {
            output = this.getTable("Object");
        }
        Column col = new Column(this, name, input, output);
        this.columns.add(col);
        return col;
    }
    public Column createColumn(String name, String input) {
        return this.createColumn(name, input, null);
    }
    public Column createColumn(String name, String input, String output) {
        if(output == null || output.isEmpty()) {
            output = "Object";
        }
        Column col = new Column(this, name, this.getTable(input), this.getTable(output));
        this.columns.add(col);
        return col;
    }
    public void deleteColumn(Column col) {
        this.columns.remove(col);
    }

    //
    // Evaluate
    //

    /**
     * Evaluate all dirty no-error keyColumns of the schema according to their dependencies
     */
    public void eval() {
        for(Column col : this.columns) {
            col.getExecutionErrors().clear();
        }

        // The strategy is to start from the keyColumns which already can be evaluated, eval them, then again generate keyColumns which can be evaluted (by excluding the previous ones) and so on until no keyColumns can be evaluted.
        // Alternatively, we could get all terminal keyColumns (with no next keyColumns) and eval them in a loop
        List<Column> done = new ArrayList<>();
        for(List<Column> cols = this.getEvaluatableColumns(null); cols.size() > 0; cols = this.getEvaluatableColumns(done)) { // Loop on expansion layers of dependencies forward
            for(Column col : cols) {
                if(col.isDirty()) { // This status will be cleared after evaluation so we explicitly propagate it to the dependents
                    col.getDependants().forEach(x -> x.setDirty());
                }
                col.eval();
            }
            done.addAll(cols);
        }
    }

    /**
     * Evaluate one column and dependencies if necessary
     */
    public void eval(Column column) {
        column.eval();
    }

    /**
     * Evaluate one column and dependencies if necessary
     */
    public void eval(Column[] columns) {
        for(Column column : columns) {
            column.eval();
        }
    }

    /**
     * Evaluate all keyColumns of one table
     */
    public void eval(Table table) {
        for(Column column : table.getColumns()) {
            column.eval();
        }
    }

    //
    // Dependency graph (needed to determine the order of column evaluations, generated by translation)
    //

    protected List<Column> getEvaluatableColumns(List<Column> excludeColumns) { // Get keyColumns with all their dependencies in the specified list and having no translation definitionErrors (own or inherited)
        if(excludeColumns == null) excludeColumns = new ArrayList<>();
        List<Column> ret = new ArrayList<>();

        for(Column col : this.columns) {
            if(excludeColumns.contains(col)) { // Already in the list. Can it really happen without cycles?
                continue;
            }

            if(col.hasDefinitionErrorsDeep()) { // Columns with definition errors cannot evaluated (including cycles)
                continue;
            }

            if(col.hasExecutionErrorsDeep()) { // Columns with evaluation errors cannot evaluated
                continue;
            }

            boolean oneDepDirty = false;
            for(Column dep : col.getDependencies()) {
                if(dep.hasDirtyDeepDerived()) { oneDepDirty = true; break; }
            }

            if(oneDepDirty) { // Columns with dirty dependencies cannot be evaluated
                continue;
            }

            ret.add(col);
        }

        return ret;
    }

    //
    // Serialization and construction
    //

    @Override
    public String toString() {
        return "[" + name + "]";
    }

    public Schema(String name) {
        this.id = UUID.randomUUID();
        this.name = name;

        // Create primitive tables
        Table objectType = createTable("Object");
    }

}
