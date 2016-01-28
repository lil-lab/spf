/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
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
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.base.hashvector;

import java.io.Serializable;
import java.util.Comparator;

public final class KeyArgs implements Comparable<KeyArgs>, Serializable {
	private static final String				KEY_SEPARATOR		= "#";
	private static final long				serialVersionUID	= -4637982636899382888L;
	private static final Comparator<String>	STRING_COMPARATOR	= (s1, s2) -> {
																	if (s1 == s2) {
																		return 0;
																	}
																	if (s1 == null) {
																		return -1;
																	}
																	if (s2 == null) {
																		return 1;
																	}
																	return s1
																			.compareTo(s2);
																};
	final String							arg1;
	final String							arg2;
	final String							arg3;
	final String							arg4;
	final String							arg5;

	final int								hashCode;

	public KeyArgs(String arg1) {
		assert arg1 != null;
		this.arg1 = arg1;
		this.arg2 = null;
		this.arg3 = null;
		this.arg4 = null;
		this.arg5 = null;
		this.hashCode = calcHashCode();
	}

	public KeyArgs(String arg1, String arg2) {
		assert arg1 != null;
		assert arg2 != null;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.arg3 = null;
		this.arg4 = null;
		this.arg5 = null;
		this.hashCode = calcHashCode();
	}

	public KeyArgs(String arg1, String arg2, String arg3) {
		assert arg1 != null;
		assert arg2 != null;
		assert arg3 != null;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.arg3 = arg3;
		this.arg4 = null;
		this.arg5 = null;
		this.hashCode = calcHashCode();
	}

	public KeyArgs(String arg1, String arg2, String arg3, String arg4) {
		assert arg1 != null;
		assert arg2 != null;
		assert arg3 != null;
		assert arg4 != null;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.arg3 = arg3;
		this.arg4 = arg4;
		this.arg5 = null;
		this.hashCode = calcHashCode();
	}

	public KeyArgs(String arg1, String arg2, String arg3, String arg4,
			String arg5) {
		assert arg1 != null;
		assert arg2 != null;
		assert arg3 != null;
		assert arg4 != null;
		assert arg5 != null;
		this.arg1 = arg1;
		this.arg2 = arg2;
		this.arg3 = arg3;
		this.arg4 = arg4;
		this.arg5 = arg5;
		this.hashCode = calcHashCode();
	}

	public static KeyArgs read(String string) {
		final String[] split = string.split(KEY_SEPARATOR);
		switch (split.length) {
			case 1:
				return new KeyArgs(split[0]);
			case 2:
				return new KeyArgs(split[0], split[1]);
			case 3:
				return new KeyArgs(split[0], split[1], split[2]);
			case 4:
				return new KeyArgs(split[0], split[1], split[2], split[3]);
			case 5:
				return new KeyArgs(split[0], split[1], split[2], split[3],
						split[4]);
			default:
				throw new IllegalArgumentException(
						"illegal number of keys, string=" + string);
		}
	}

	@Override
	public int compareTo(KeyArgs o) {
		final int arg1Comparison = STRING_COMPARATOR.compare(arg1, o.arg1);
		if (arg1Comparison != 0) {
			return arg1Comparison;
		}

		final int arg2Comparison = STRING_COMPARATOR.compare(arg2, o.arg2);
		if (arg2Comparison != 0) {
			return arg2Comparison;
		}

		final int arg3Comparison = STRING_COMPARATOR.compare(arg3, o.arg3);
		if (arg3Comparison != 0) {
			return arg3Comparison;
		}

		final int arg4Comparison = STRING_COMPARATOR.compare(arg4, o.arg4);
		if (arg4Comparison != 0) {
			return arg4Comparison;
		}

		return STRING_COMPARATOR.compare(arg5, o.arg5);
	}

	/**
	 * Returns 'true' iff the other KeyArgs specifies all the argument that are
	 * not <code>null</code> in this KeyArgs, and all the others are either
	 * <code>null</code> or specified.
	 */
	public boolean contains(KeyArgs other) {
		if (arg1 != null && !arg1.equals(other.arg1)) {
			return false;
		}

		if (arg2 != null && !arg2.equals(other.arg2)) {
			return false;
		}

		if (arg3 != null && !arg3.equals(other.arg3)) {
			return false;
		}

		if (arg4 != null && !arg4.equals(other.arg4)) {
			return false;
		}

		if (arg5 != null && !arg5.equals(other.arg5)) {
			return false;
		}

		return true;
	}

	@Override
	public final boolean equals(Object obj) {
		// equals() optimized under the assumptions of the hash vector

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final KeyArgs other = (KeyArgs) obj;

		// Can take care of most cases using the hash code
		if (hashCode != other.hashCode) {
			return false;
		}

		// arg1
		if (!arg1.equals(other.arg1)) {
			return false;
		}

		// arg2
		if (arg2 == null) {
			if (other.arg2 != null) {
				return false;
			} else {
				// If both are null, no point to check arg3 and arg4 (they
				// should both be null)
				return true;
			}
		} else if (!arg2.equals(other.arg2)) {
			return false;
		}

		// arg3
		if (arg3 == null) {
			if (other.arg3 != null) {
				return false;
			} else {
				return true;
			}
		} else if (!arg3.equals(other.arg3)) {
			return false;
		}

		// arg4
		if (arg4 == null) {
			if (other.arg4 != null) {
				return false;
			} else {
				return true;
			}
		} else if (!arg4.equals(other.arg4)) {
			return false;
		}

		// arg5
		if (arg4 == null) {
			if (other.arg4 != null) {
				return false;
			}
		} else if (!arg4.equals(other.arg4)) {
			return false;
		}

		return true;
	}

	public final String getArg1() {
		return arg1;
	}

	public final String getArg2() {
		return arg2;
	}

	public final String getArg3() {
		return arg3;
	}

	public final String getArg4() {
		return arg4;
	}

	public String getArg5() {
		return arg5;
	}

	@Override
	public final int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		final StringBuilder stringKey = new StringBuilder();

		stringKey.append(arg1);
		if (arg2 != null) {
			stringKey.append(KEY_SEPARATOR).append(arg2);
			if (arg3 != null) {
				stringKey.append(KEY_SEPARATOR).append(arg3);
				if (arg4 != null) {
					stringKey.append(KEY_SEPARATOR).append(arg4);
					if (arg5 != null) {
						stringKey.append(KEY_SEPARATOR).append(arg5);
					}

				}
			}
		}

		return stringKey.toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (arg1 == null ? 0 : arg1.hashCode());
		result = prime * result + (arg2 == null ? 0 : arg2.hashCode());
		result = prime * result + (arg3 == null ? 0 : arg3.hashCode());
		result = prime * result + (arg4 == null ? 0 : arg4.hashCode());
		result = prime * result + (arg5 == null ? 0 : arg5.hashCode());
		return result;
	}

}
