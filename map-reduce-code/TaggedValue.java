package com.sabre.bigdata.smav2.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class TaggedValue implements WritableComparable<TaggedValue> {

	private Text value = new Text();
	private BooleanWritable isRobotic = new BooleanWritable();

	public boolean isRobotic() {
		return isRobotic.get();
	}

	public void setRobotic(boolean robotic) {
		this.isRobotic.set(robotic);
	}

	public Text getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value.set(value);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		value.write(out);
		isRobotic.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		value.readFields(in);
		isRobotic.readFields(in);
	}

	@Override
	public int compareTo(TaggedValue o) {
		return this.value.compareTo(o.value);
	}
	
	public static TaggedValue of(String value, boolean isRobotic){
		TaggedValue taggedValue = new TaggedValue();
		
		taggedValue.setValue(value);
		taggedValue.setRobotic(isRobotic);
		
		return taggedValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((isRobotic == null) ? 0 : isRobotic.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TaggedValue other = (TaggedValue) obj;
		if (isRobotic == null) {
			if (other.isRobotic != null)
				return false;
		} else if (!isRobotic.equals(other.isRobotic))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
