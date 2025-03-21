/*
 * Copyright (C) 2012-2018 The Android Money Manager Ex Project Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.money.manager.ex.datalayer;

import android.content.Context;
import android.database.Cursor;

import com.google.common.collect.ObjectArrays;
import com.money.manager.ex.database.DatasetType;
import com.money.manager.ex.domainmodel.Stock;
import com.money.manager.ex.utils.MmxDatabaseUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import info.javaperformance.money.Money;

/**
 * Data repository for Stock entities.
 */
public class StockRepository
    extends RepositoryBase<Stock> {

    private static final String TABLE_NAME = "stock_v1";
    private static final String ID_COLUMN = StockFields.STOCKID;
    private static final String NAME_COLUMN = StockFields.SYMBOL;

    @Inject
    public StockRepository(Context context) {
        super(context, TABLE_NAME, DatasetType.TABLE, "stock", ID_COLUMN, NAME_COLUMN);
    }

    @Override
    protected Stock createEntity() {
        return new Stock();
    }

    @Override
    public String[] getAllColumns() {
        String [] idColumn = new String[] {
                ID_COLUMN + " AS _id"
        };

        String[] result = ObjectArrays.concat(idColumn, tableColumns(), String.class);
        //String[] result = ArrayUtils.addAll(idColumn, tableColumns());
        return result;
    }

    private String[] tableColumns() {
        Field[] fields = StockFields.class.getFields();
        String[] names = new String[fields.length];

        for(int i = 0; i < fields.length; i++) {
            names[i] = fields[i].getName();
        }

        return names;
    }

    /**
     * Load multiple items by id.
     * @param ids Array of ids to load.
     * @return List of stocks.
     */
    public List<Stock> load(Integer[] ids) {
        if (ids.length == 0) return null;

        MmxDatabaseUtils dbUtils = new MmxDatabaseUtils(getContext());
        String placeHolders = dbUtils.makePlaceholders(ids.length);
        String[] idParams = new String[ids.length];

        for (int i = 0; i < ids.length; i++) {
            idParams[i] = Integer.toString(ids[i]);
        }

        Cursor c = openCursor(null,
            StockFields.STOCKID + " IN (" + placeHolders + ")",
            idParams,
            null);
        if (c == null) return null;

        List<Stock> result = getEntities(c);

        return result;
    }

    public List<Stock> loadForSymbols(String[] symbols) {
        if (symbols.length == 0) return null;

        MmxDatabaseUtils dbUtils = new MmxDatabaseUtils(getContext());
        String placeHolders = dbUtils.makePlaceholders(symbols.length);

        Cursor c = openCursor(null,
            StockFields.SYMBOL + " IN (" + placeHolders + ")",
            symbols,
            null);
        if (c == null) return null;

        List<Stock> result = getEntities(c);

        return result;
    }

    /**
     * Retrieves all record ids which refer the given symbol.
     * @return array of ids of records which contain the symbol.
     */
    public int[] findIdsBySymbol(String symbol) {
        int[] result;

        Cursor cursor = getContext().getContentResolver().query(this.getUri(),
                new String[]{ StockFields.STOCKID },
                StockFields.SYMBOL + "=?", new String[]{symbol},
                null);
        if (cursor == null) return null;

        int records = cursor.getCount();
        result = new int[records];

        for (int i = 0; i < records; i++) {
            cursor.moveToNext();
            result[i] = cursor.getInt(cursor.getColumnIndex(StockFields.STOCKID));
        }
        cursor.close();

        return result;
    }

    /**
     * Update price for all the records with this symbol.
     * @param symbol Stock symbol
     * @param price Stock price
     */
    public void updateCurrentPrice(String symbol, Money price) {
        int[] ids = findIdsBySymbol(symbol);

        // recalculate value

        for (long id : ids) {
            Stock stock = load(id);
            if (stock == null) continue; // this should not happen, but see #2295 -anr-1071-stockrepository
            stock.setCurrentPrice(price);
            // recalculate & assign the value
            Money value = stock.getValue();

            save(stock);
        }
    }

    private List<Stock> getEntities(Cursor c) {
        List<Stock> result = new ArrayList<>();
        while (c.moveToNext()) {
            result.add(Stock.from(c));
        }
        c.close();

        return result;
    }
}
