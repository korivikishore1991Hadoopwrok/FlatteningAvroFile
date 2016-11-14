package com.sabre.bigdata.smav2.comparator;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

import com.sabre.bigdata.smav2.types.TaggedKey;

public class TaggedJoiningGroupingComparator extends WritableComparator {

	public TaggedJoiningGroupingComparator() {
		super(TaggedKey.class, true);
	}

	@Override
	public int compare(WritableComparable a, WritableComparable b) {
		TaggedKey taggedKey1 = (TaggedKey) a;
		TaggedKey taggedKey2 = (TaggedKey) b;
		return taggedKey1.getJoinKey().compareTo(taggedKey2.getJoinKey());
	}
}
