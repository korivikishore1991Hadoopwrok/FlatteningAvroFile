package com.sabre.bigdata.smav2.partitioner;

import org.apache.hadoop.mapreduce.Partitioner;

import com.sabre.bigdata.smav2.types.TaggedKey;
import com.sabre.bigdata.smav2.types.TaggedValue;

public class TaggedJoiningPartitioner extends Partitioner<TaggedKey, TaggedValue> {

	@Override
	public int getPartition(TaggedKey key, TaggedValue value, int numPartitions) {
		return Math.abs(key.getJoinKey().hashCode()) % numPartitions;
	}

}
