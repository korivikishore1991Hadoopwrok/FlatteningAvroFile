package com.sabre.bigdata.smav2.reducer;

import static com.sabre.bigdata.smav2.util.ApplicationConstants.FIELDS_SEPARATOR_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.PIPE;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import com.sabre.bigdata.smav2.types.TaggedKey;
import com.sabre.bigdata.smav2.types.TaggedValue;
import com.sabre.bigdata.smav2.util.BadRecordCounters;

public class SMAReducer extends Reducer<TaggedKey, TaggedValue, Text, NullWritable> {

	static final String CURRENT_DATE = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
			
	private String separator;

	private Text joinedText = new Text();

	private NullWritable nullKey = NullWritable.get();

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		Configuration config = context.getConfiguration();
		separator = config.get(FIELDS_SEPARATOR_PROP, PIPE);
	}

	@Override
	protected void reduce(TaggedKey key, Iterable<TaggedValue> values, Context context) throws IOException,
			InterruptedException {
		Iterator<TaggedValue> it = values.iterator();
		TaggedValue robotic = it.next(); /*refers to current element(value1)*/
		String roboticValue = robotic.getValue().toString();
		if (!robotic.isRobotic()) {
			joinedText.set(key.getJoinKey() + separator + robotic.getValue().toString() + separator );
			context.getCounter(BadRecordCounters.RECORDS_NUMBER).increment(1);
			context.write(joinedText, nullKey);
			roboticValue = "";
		}

		while (it.hasNext()) {
			TaggedValue currValue = it.next(); /*refers to current element(value1)*/
			joinedText.set(key.getJoinKey() + separator + currValue.getValue().toString() + separator + roboticValue);
			context.getCounter(BadRecordCounters.RECORDS_NUMBER).increment(1);
			context.write(joinedText, nullKey);
		}
	}
}
