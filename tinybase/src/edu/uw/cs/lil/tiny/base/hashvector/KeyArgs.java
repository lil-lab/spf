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
package edu.uw.cs.lil.tiny.base.hashvector;

import java.io.Serializable;

import edu.uw.cs.utils.assertion.Assert;

public final class KeyArgs implements Comparable<KeyArgs>, Serializable {
	private static final long	serialVersionUID	= -4637982636899382888L;
	private final String		cachedString;
	final String				arg1;
	final String				arg2;
	final String				arg3;
	final String				arg4;
	final String				arg5;
	final int					hashCode;
	
	public KeyArgs(String arg1) {
		this.arg1 = Assert.ifNull(arg1);
		this.arg2 = null;
		this.arg3 = null;
		this.arg4 = null;
		this.arg5 = null;
		this.hashCode = calcHashCode();
		this.cachedString = createString();
	}
	
	public KeyArgs(String arg1, String arg2) {
		this.arg1 = Assert.ifNull(arg1);
		this.arg2 = Assert.ifNull(arg2);
		this.arg3 = null;
		this.arg4 = null;
		this.arg5 = null;
		this.hashCode = calcHashCode();
		this.cachedString = createString();
	}
	
	public KeyArgs(String arg1, String arg2, String arg3) {
		this.arg1 = Assert.ifNull(arg1);
		this.arg2 = Assert.ifNull(arg2);
		this.arg3 = Assert.ifNull(arg3);
		this.arg4 = null;
		this.arg5 = null;
		this.hashCode = calcHashCode();
		this.cachedString = createString();
	}
	
	public KeyArgs(String arg1, String arg2, String arg3, String arg4) {
		this.arg1 = Assert.ifNull(arg1);
		this.arg2 = Assert.ifNull(arg2);
		this.arg3 = Assert.ifNull(arg3);
		this.arg4 = Assert.ifNull(arg4);
		this.arg5 = null;
		this.hashCode = calcHashCode();
		this.cachedString = createString();
	}
	
	public KeyArgs(String arg1, String arg2, String arg3, String arg4,
			String arg5) {
		this.arg1 = Assert.ifNull(arg1);
		this.arg2 = Assert.ifNull(arg2);
		this.arg3 = Assert.ifNull(arg3);
		this.arg4 = Assert.ifNull(arg4);
		this.arg5 = Assert.ifNull(arg5);
		this.hashCode = calcHashCode();
		this.cachedString = createString();
	}
	
	@Override
	public int compareTo(KeyArgs o) {
		return cachedString.compareTo(o.cachedString);
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
		return cachedString;
	}
	
	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((arg1 == null) ? 0 : arg1.hashCode());
		result = prime * result + ((arg2 == null) ? 0 : arg2.hashCode());
		result = prime * result + ((arg3 == null) ? 0 : arg3.hashCode());
		result = prime * result + ((arg4 == null) ? 0 : arg4.hashCode());
		result = prime * result + ((arg5 == null) ? 0 : arg5.hashCode());
		return result;
	}
	
	private final String createString() {
		final StringBuilder stringKey = new StringBuilder();
		
		stringKey.append(arg1);
		if (arg2 != null) {
			stringKey.append('#').append(arg2);
			if (arg3 != null) {
				stringKey.append('#').append(arg3);
				if (arg4 != null) {
					stringKey.append('#').append(arg4);
					if (arg5 != null) {
						stringKey.append('#').append(arg5);
					}
					
				}
			}
		}
		
		return stringKey.toString();
	}
}
