package com.sabre.bigdata.smav2.types;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

public class TaggedKey implements WritableComparable<TaggedKey> {

	private Text joinKey = new Text();
	
	private IntWritable tag = new IntWritable();
	
	public Text getJoinKey() {
		return joinKey;
	}

	public void setJoinKey(Text joinKey) {
		this.joinKey = joinKey;
	}

	public IntWritable getTag() {
		return tag;
	}

	public void setTag(IntWritable tag) {
		this.tag = tag;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		joinKey.write(out);
		tag.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		joinKey.readFields(in);
		tag.readFields(in);
	}

	@Override
	public int compareTo(TaggedKey other) {
		int compareValue = this.joinKey.compareTo(other.getJoinKey());
		if (compareValue == 0) {
			compareValue = this.tag.compareTo(other.getTag()); 
		}
		return compareValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((joinKey == null) ? 0 : joinKey.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
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
		TaggedKey other = (TaggedKey) obj;
		if (joinKey == null) {
			if (other.joinKey != null)
				return false;
		} else if (!joinKey.equals(other.joinKey))
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		return true;
	}
	
	public static TaggedKey of(String joinKey, IntWritable tag) {
		TaggedKey key = new TaggedKey();

		key.setJoinKey(new Text(joinKey));
		key.setTag(tag);

		return key;
	}
}
