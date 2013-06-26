/*******************************************************************************
 * UW SPF - The University of Washington Semantic Parsing Framework
 * <p>
 * Copyright (C) 2013 Yoav Artzi
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 ******************************************************************************/
package edu.uw.cs.lil.tiny.mr.lambda.exec.tabular;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;

public class Table implements Iterable<Map<LogicalExpression, Object>> {
	
	private Set<Map<LogicalExpression, Object>>	rows;
	
	public Table() {
		this.rows = new HashSet<Map<LogicalExpression, Object>>();
	}
	
	public Table(Set<Map<LogicalExpression, Object>> rows) {
		this.rows = rows;
	}
	
	public void addRow(Map<LogicalExpression, Object> row) {
		rows.add(row);
	}
	
	public void augment(LogicalExpression newHeader, List<?> values) {
		if (rows.isEmpty()) {
			for (final Object obj : values) {
				final Map<LogicalExpression, Object> row = new HashMap<LogicalExpression, Object>();
				row.put(newHeader, obj);
				rows.add(row);
			}
		} else if (!values.isEmpty()) {
			final Set<Map<LogicalExpression, Object>> oldRows = rows;
			rows = new HashSet<Map<LogicalExpression, Object>>(rows.size()
					* values.size());
			for (final Map<LogicalExpression, Object> row : oldRows) {
				for (final Object value : values) {
					final HashMap<LogicalExpression, Object> newRow = new HashMap<LogicalExpression, Object>(
							row);
					newRow.put(newHeader, value);
					rows.add(newRow);
				}
			}
		}
	}
	
	public void augment(LogicalExpression header, Object value) {
		if (rows.isEmpty()) {
			final Map<LogicalExpression, Object> row = new HashMap<LogicalExpression, Object>();
			row.put(header, value);
			rows.add(row);
		} else {
			for (final Map<LogicalExpression, Object> row : rows) {
				row.put(header, value);
			}
		}
	}
	
	public void augment(Table other) {
		if (rows.isEmpty()) {
			for (final Map<LogicalExpression, Object> row : other.rows) {
				rows.add(new HashMap<LogicalExpression, Object>(row));
			}
		} else if (!other.rows.isEmpty()) {
			final Set<Map<LogicalExpression, Object>> oldRows = rows;
			rows = new HashSet<Map<LogicalExpression, Object>>(oldRows.size()
					* other.rows.size());
			for (final Map<LogicalExpression, Object> row1 : oldRows) {
				for (final Map<LogicalExpression, Object> row2 : other.rows) {
					final HashMap<LogicalExpression, Object> newRow = new HashMap<LogicalExpression, Object>(
							row1);
					newRow.putAll(row2);
					this.rows.add(newRow);
				}
			}
			
		}
	}
	
	/**
	 * Augments an existing row with a new column
	 * 
	 * @param row
	 *            A table row. Assumed to be in the table.
	 * @param newHeader
	 * @param values
	 */
	public void augmentRow(Map<LogicalExpression, Object> row,
			LogicalExpression newHeader, Collection<?> values) {
		final Iterator<?> iterator = values.iterator();
		boolean first = true;
		while (iterator.hasNext()) {
			if (first) {
				first = false;
				row.put(newHeader, iterator.next());
			} else {
				final Map<LogicalExpression, Object> rowClone = new HashMap<LogicalExpression, Object>(
						row);
				row.put(newHeader, iterator.next());
				rows.add(rowClone);
			}
		}
	}
	
	public void clear() {
		rows.clear();
	}
	
	@Override
	public Iterator<Map<LogicalExpression, Object>> iterator() {
		return rows.iterator();
	}
	
	public int numRows() {
		return rows.size();
	}
	
	public void removeColumn(LogicalExpression arg) {
		for (final Map<LogicalExpression, Object> row : rows) {
			row.remove(arg);
		}
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final Map<LogicalExpression, Object> row : rows) {
			for (final Map.Entry<LogicalExpression, Object> entry : row
					.entrySet()) {
				sb.append(entry.getKey()).append("=").append(entry.getValue())
						.append("; ");
			}
			sb.append('\n');
		}
		return sb.toString();
	}
	
}
