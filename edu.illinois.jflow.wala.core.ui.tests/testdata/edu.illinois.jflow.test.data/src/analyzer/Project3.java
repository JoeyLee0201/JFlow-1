package analyzer;

public class Project3 {
	public static void main(String[] args) {
		int a = producer(1);
		int b = producer(a);
		a += b;
		int c = producer(a);
	}

	static int producer(int input) {
		return input + 2;
	}
}
