package partitionchecker;

class Datum {
	Integer field;

	public Datum(Integer field) {
		this.field = field;
	}

	@Override
	public String toString() {
		return "Datum [field=" + field + "]";
	}

	public Integer getField() {
		return field;
	}

	public void setField(Integer field) {
		this.field = field;
	}

}