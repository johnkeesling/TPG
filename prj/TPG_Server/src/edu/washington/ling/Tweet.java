package edu.washington.ling;

/*
 * A Tweet object contains a reference to the text of a post on Twitter
 * as well as a reference to its timestamp.  The variable score was formerly
 * used in a different implementation of sentence ranking, but is now
 * obsolete and in a later version should be removed.  It should also be noted
 * that this class implements Comparable and thus allows for easy sorting based
 * on the timestamp field using Arrays.sort().
 * 
 * Constructor:
 * 
 * public Tweet(String message, int timestamp, double score)
 * 
 */

public class Tweet implements Comparable {
	private String message;
	private int timestamp;
	private double score;
	
	public Tweet (String message, int timestamp, double score) {
		this.message = message;
		this.timestamp = timestamp;
		this.score = score;
	}
	
	public String getMessage() {
		return message;
	}
	
	public int getTimestamp() {
		return timestamp;
	}
	
	public double getScore() {
		return score;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	
	public String printTweet() {
		String result = "message=" + this.message;
		return result;
	}
	
	public int compareTo(Object obj) {
		try { 
			Tweet tmp = (Tweet) obj;
			int ts1 = this.timestamp;
			int ts2 = tmp.getTimestamp();
			if (ts1 < ts2) {
				return 1;
			} else if (ts1 > ts2) {
				return -1;			
			} else {
				return 0;
			}
		} catch (Exception e) {
			return 0;
		}
	}
}
