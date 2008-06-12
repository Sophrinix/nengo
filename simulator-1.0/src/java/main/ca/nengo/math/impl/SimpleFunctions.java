/*
The contents of this file are subject to the Mozilla Public License Version 1.1 
(the "License"); you may not use this file except in compliance with the License. 
You may obtain a copy of the License at http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis, WITHOUT
WARRANTY OF ANY KIND, either express or implied. See the License for the specific 
language governing rights and limitations under the License.

The Original Code is "SimpleFunctions.java". Description: 
"A collection of Functions that do not have parameters"

The Initial Developer of the Original Code is Bryan Tripp & Centre for Theoretical Neuroscience, University of Waterloo. Copyright (C) 2006-2008. All Rights Reserved.

Alternatively, the contents of this file may be used under the terms of the GNU 
Public License license (the GPL License), in which case the provisions of GPL 
License are applicable  instead of those above. If you wish to allow use of your 
version of this file only under the terms of the GPL License and not to allow 
others to use your version of this file under the MPL, indicate your decision 
by deleting the provisions above and replace  them with the notice and other 
provisions required by the GPL License.  If you do not delete the provisions above,
a recipient may use your version of this file under either the MPL or the GPL License.
*/

/*
 * Created on 6-Mar-08
 */
package ca.nengo.math.impl;

/**
 * A collection of Functions that do not have parameters. 
 *  
 * @author Bryan Tripp
 */
public class SimpleFunctions {

	/**
	 * In radians. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class Sin extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Sin() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.sin(from[0]);
		}
	}

	/**
	 * In radians. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class Cos extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Cos() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.cos(from[0]);
		}
	}
	
	/**
	 * In radians. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class Tan extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Tan() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.tan(from[0]);
		}
	}
	
	/**
	 * In radians. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class Asin extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Asin() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.asin(from[0]);
		}
	}
	
	/**
	 * In radians. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class Acos extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Acos() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.acos(from[0]);
		}
	}
	
	/**
	 * In radians. 
	 * 
	 * @author Bryan Tripp
	 */
	public static class Atan extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Atan() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.atan(from[0]);
		}
	}
	
	public static class Exp extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Exp() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.exp(from[0]);
		}
	}

	public static class Log2 extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Log2() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) (Math.log(from[0])/Math.log(2.0));
		}
	}
	
	public static class Log10 extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Log10() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.log10(from[0]);
		}
	}
	
	public static class Ln extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Ln() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.log(from[0]);
		}
	}
	
	public static class Pow extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Pow() {
			super(2);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.pow(from[0], from[1]);
		}
	}
	
	public static class Max extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Max() {
			super(2);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.max(from[0], from[1]);
		}
	}
	
	public static class Min extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Min() {
			super(2);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.min(from[0], from[1]);
		}
	}
	
	public static class Sqrt extends AbstractFunction {
		private static final long serialVersionUID = 1L;

		public Sqrt() {
			super(1);
		}

		@Override
		public float map(float[] from) {
			return (float) Math.sqrt(from[0]);
		}
	}
	
}
