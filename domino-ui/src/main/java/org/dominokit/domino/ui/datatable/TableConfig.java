package org.dominokit.domino.ui.datatable;

import elemental2.dom.*;
import org.dominokit.domino.ui.datatable.plugins.DataTablePlugin;
import org.dominokit.domino.ui.popover.Tooltip;
import org.dominokit.domino.ui.style.Style;
import org.dominokit.domino.ui.utils.DominoElement;
import org.dominokit.domino.ui.utils.HasMultiSelectionSupport;
import org.jboss.elemento.HtmlContentBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static org.jboss.elemento.Elements.*;

public class TableConfig<T> implements HasMultiSelectionSupport {

    private List<ColumnConfig<T>> columns = new LinkedList<>();
    private List<DataTablePlugin<T>> plugins = new LinkedList<>();
    private DataTable<T> dataTable;
    private boolean fixed = false;
    private String fixedDefaultColumnWidth = "100px";
    private String fixedBodyHeight = "400px";
    private boolean lazyLoad = true;
    private boolean multiSelect = true;
    private RowAppender<T> rowAppender = (dataTable, tableRow) -> dataTable.bodyElement().appendChild(tableRow.element());

    public void drawHeaders(DataTable<T> dataTable, DominoElement<HTMLTableSectionElement> thead) {
        this.dataTable = dataTable;
        HtmlContentBuilder<HTMLTableRowElement> tr = tr();
        thead.appendChild(tr.element());

        columns.forEach(columnConfig -> {
            //TODO replace with FlexLayout
            Node element = columnConfig.getHeaderElement().asElement(columnConfig.getTitle());
            columnConfig.contextMenu = div().style("width: 15px; display: none;").element();
            HtmlContentBuilder<HTMLDivElement> headerContent = div()
                    .style("display: flex;")
                    .add(div()
                            .style("width:100%")
                            .add(element))
                    .add(columnConfig.contextMenu);
            HtmlContentBuilder<HTMLTableCellElement> th = th().css(DataTableStyles.TABLE_CM_HEADER).add(headerContent.element());

            columnConfig.applyScreenMedia(th.element());

            tr.add(th);
            columnConfig.setHeadElement(th.element());
            if (dataTable.getTableConfig().isFixed() || columnConfig.isFixed()) {
                fixElementWidth(columnConfig, th.element());
            }

            if (columnConfig.isShowTooltip()) {
                Tooltip.create(th.element(), columnConfig.getTooltipNode());
            }
            columnConfig.applyHeaderStyle();
            columnConfig.addShowHideListener(DefaultColumnShowHideListener.of(th.element(), true));
            DominoElement.of(th).toggleDisplay(!columnConfig.isHidden());

            plugins.forEach(plugin -> plugin.onHeaderAdded(dataTable, columnConfig));
        });

        dataTable.tableElement().appendChild(thead);
    }

    private void fixElementWidth(ColumnConfig<T> column, HTMLElement element) {
        String fixedWidth = bestFitWidth(column);
        Style.of(element)
                .setWidth(fixedWidth)
                .setMinWidth(fixedWidth)
                .setMaxWidth(fixedWidth)
                .add(DataTableStyles.FIXED_WIDTH);

    }

    public void drawRecord(DataTable<T> dataTable, TableRow<T> tableRow) {
        columns.forEach(columnConfig -> {

            HTMLTableCellElement cellElement;
            if (columnConfig.isHeader()) {
                cellElement = th().css("dt-th-cell").element();
            } else {
                cellElement = td().css("dt-td-cell").element();
            }

            if (dataTable.getTableConfig().isFixed() || columnConfig.isFixed()) {
                fixElementWidth(columnConfig, cellElement);
            }

            RowCell<T> rowCell = new RowCell<>(new CellRenderer.CellInfo<>(tableRow, cellElement), columnConfig);
            rowCell.updateCell();
            tableRow.addCell(rowCell);

            columnConfig.applyScreenMedia(cellElement);

            tableRow.element().appendChild(cellElement);
            columnConfig.applyCellStyle(cellElement);
            columnConfig.addShowHideListener(DefaultColumnShowHideListener.of(cellElement));
            DominoElement.of(cellElement).toggleDisplay(!columnConfig.isHidden());
        });
        rowAppender.appendRow(dataTable, tableRow);

        plugins.forEach(plugin -> plugin.onRowAdded(dataTable, tableRow));
    }


    public TableConfig<T> addColumn(ColumnConfig<T> column) {
        this.columns.add(column);
        return this;
    }

    public TableConfig<T> insertColumnFirst(ColumnConfig<T> column) {
        this.columns.add(0, column);
        return this;
    }

    public TableConfig<T> insertColumnLast(ColumnConfig<T> column) {
        this.columns.add(this.columns.size() - 1, column);
        return this;
    }

    public TableConfig<T> addPlugin(DataTablePlugin<T> plugin) {
        this.plugins.add(plugin);
        return this;
    }

    public boolean isFixed() {
        return fixed;
    }

    public TableConfig<T> setFixed(boolean fixed) {
        this.fixed = fixed;
        return this;
    }

    public boolean isLazyLoad() {
        return lazyLoad;
    }

    public TableConfig<T> setLazyLoad(boolean lazyLoad) {
        this.lazyLoad = lazyLoad;
        return this;
    }

    public String getFixedBodyHeight() {
        return fixedBodyHeight;
    }

    public TableConfig<T> setFixedBodyHeight(String fixedBodyHeight) {
        this.fixedBodyHeight = fixedBodyHeight;
        return this;
    }

    public String getFixedDefaultColumnWidth() {
        return fixedDefaultColumnWidth;
    }

    public TableConfig<T> setFixedDefaultColumnWidth(String fixedDefaultColumnWidth) {
        this.fixedDefaultColumnWidth = fixedDefaultColumnWidth;
        return this;
    }

    String bestFitWidth(ColumnConfig<T> columnConfig) {
        if (nonNull(columnConfig.getWidth()) && !columnConfig.getWidth().isEmpty()) {
            return columnConfig.getWidth();
        } else if (nonNull(columnConfig.getMinWidth()) && !columnConfig.getMinWidth().isEmpty()) {
            return columnConfig.getMinWidth();
        } else if (nonNull(columnConfig.getMaxWidth()) && !columnConfig.getMaxWidth().isEmpty()) {
            return columnConfig.getMaxWidth();
        } else {
            return fixedDefaultColumnWidth;
        }
    }

    @Override
    public boolean isMultiSelect() {
        return this.multiSelect;
    }

    @Override
    public void setMultiSelect(boolean multiSelect) {
        this.multiSelect = multiSelect;
    }

    public void setRowAppender(RowAppender<T> rowAppender) {
        if (nonNull(rowAppender)) {
            this.rowAppender = rowAppender;
        }
    }

    public List<DataTablePlugin<T>> getPlugins() {
        return plugins;
    }

    void onBeforeHeaders(DataTable<T> dataTable) {
        plugins.forEach(plugin -> plugin.onBeforeAddHeaders(dataTable));
    }

    public void onAfterHeaders(DataTable<T> dataTable) {
        plugins.forEach(plugin -> plugin.onAfterAddHeaders(dataTable));
    }

    public List<ColumnConfig<T>> getColumns() {
        return columns;
    }

    public List<ColumnConfig<T>> getVisibleColumns() {
        return columns.stream().filter(column -> !column.isHidden())
                .collect(Collectors.toList());
    }

    public ColumnConfig<T> getColumnByName(String name) {
        Optional<ColumnConfig<T>> first = getColumns()
                .stream()
                .filter(columnConfig -> columnConfig.getName().equals(name))
                .findFirst();
        if (first.isPresent()) {
            return first.get();
        } else {
            throw new ColumnNofFoundException(name);
        }
    }

    public DataTable<T> getDataTable() {
        return dataTable;
    }

    @FunctionalInterface
    public interface RowAppender<T> {
        void appendRow(DataTable<T> dataTable, TableRow<T> tableRow);
    }

    public static class ColumnNofFoundException extends RuntimeException {
        public ColumnNofFoundException(String name) {
            super(name);
        }
    }
}
