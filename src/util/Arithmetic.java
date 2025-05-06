package util;

public class Arithmetic {
	// Function to find the LCM of an array of numbers
    public static int findLCM(int[] arr) {
        int lcm = arr[0];
        for (int i = 1; i < arr.length; i++) {
            int currentNumber = arr[i];
            lcm = (lcm * currentNumber) / gcd(lcm, currentNumber);
        }
        return lcm;
    }

    // Function to find the GCD of two numbers
    public static int gcd(int a, int b) {
        if (b == 0) {
            return a;
        }
        return gcd(b, a % b);
    }
    
    public static int mod(int a, int b) {
    	int res = a%b;
    	while(res<0) {
    		res += b;
    	}
    	return res;
    }
}
