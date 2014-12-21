package com.ra4king.test;

import com.ra4king.opengl.util.math.Vector3;
import com.ra4king.test.Test1.Test2;

import net.indiespot.struct.cp.Struct;

/**
 * @author Roi Atalla
 */
public class TestStructArray {
	public static void main(String[] args) {
		Vector3 v = new Vector3(1, 2, 3);
		
		System.out.println(v.toString());
		
		Vector3[] array = {
				new Vector3(2, 3, 4),
				new Vector3(3, 4, 5),
				new Vector3(4, 5, 6)
		};
		
		for(int a = 0; a < array.length; a++) {
			System.out.println(array[a].toString());
		}
		
		Test1 test1 = new Test1();
		Test2 test2 = test1.new Test2();
		test2.myMethod();
		System.out.println(test1.v.toString());
	}
}

class Test1 {
	Vector3 v = Struct.malloc(Vector3.class);
	
	class Test2 {
		void myMethod() {
			v.set(42, 42, 42);
			System.out.println(Test1.this.v.toString());
		}
	}
}
