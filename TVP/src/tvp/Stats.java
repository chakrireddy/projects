package tvp;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Stats {
	Logger logger = Logger.getLogger(Stats.class.getName());
	int numSuccessfulAccess = 0;
	int numUnSuccessfulAccess = 0;
	int numofMessagesEx = 0;
	long min = 0;
	long max = 0;
	Map<Integer, Long> timeMap = new HashMap<Integer, Long>();
	public void print(){
		logger.addHandler(TreeVotingProtocol.getInstance().fileHandler);
		logger.info("# of successful access: "+numSuccessfulAccess);
		logger.info("# of unsuccessful acces: "+numUnSuccessfulAccess);
		logger.info("# of messages exchanged: "+numofMessagesEx);
		long total = 0;
		for (int key : timeMap.keySet()) {
			if(min == 0 && max == 0){
				min = timeMap.get(key);
				max = min;
				total = min;
			}else{
				long val = timeMap.get(key);
				if(val < min){
					min = val;
				}
				if(val > max){
					max = val;
				}
				total += val;
			}			
		}
		long avg = (total/timeMap.size());
		logger.info("Minimum time taken: "+min);
		logger.info("Maximum time taken: "+max);
		logger.info("Average time taken: "+avg);
		long num = 0;
		for (int key : timeMap.keySet()) {
			num += Math.pow((timeMap.get(key) - avg), 2.0);
		}		
		double deviation = Math.sqrt((num/timeMap.size()));
		logger.info("Standard Deviation: "+deviation);
		
	}	

}