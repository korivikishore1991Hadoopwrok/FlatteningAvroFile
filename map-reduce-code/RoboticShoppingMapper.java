package com.sabre.bigdata.smav2.mapper;

import static com.sabre.bigdata.smav2.util.ApplicationConstants.COMMA;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.ROBOTIC_FIELD_SEPARATOR_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.ROBOTIC_SHOPPING_DATA;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.sabre.bigdata.smav2.types.TaggedKey;
import com.sabre.bigdata.smav2.types.TaggedValue;

public class RoboticShoppingMapper extends Mapper<LongWritable, Text, TaggedKey, TaggedValue> {

	private TaggedKey taggedKey = new TaggedKey();

	private Text joinKey = new Text();

	private Splitter splitter;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		Configuration config = context.getConfiguration();
		String fieldSeparator = config.get(ROBOTIC_FIELD_SEPARATOR_PROP, COMMA);
		splitter = Splitter.on(fieldSeparator).trimResults();
	}

	@Override
	protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		List<String> values = Lists.newArrayList(splitter.split(value.toString()));
		if (values.size() > 1) {
			String indicator = values.get(1);
			joinKey.set(values.get(0));
			taggedKey.setJoinKey(joinKey);
			taggedKey.setTag(ROBOTIC_SHOPPING_DATA);
			TaggedValue taggedValue = TaggedValue.of(indicator, true);
			context.write(taggedKey, taggedValue);
		}
	}
}
