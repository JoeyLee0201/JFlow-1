package analyzer;

public class Project4 {
	public static void main(String[] args) {
		int a;
		int b;
		int c;

		a = producer(1);
		b = producer(a);
		a += b;
		c = producer(a);
	}

	static int producer(int input) {
		return input + 2;
	}
}
