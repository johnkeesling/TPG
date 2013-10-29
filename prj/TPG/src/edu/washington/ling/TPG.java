package edu.washington.ling;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class TPG extends Activity {
	
	private static final String URL_BASE = "http://pbpgen.appspot.com/tpg_server?name=";
	private static final String ENCODING = "UTF-8";
	public String Name = "";
	public String Date = "2010-01-05";
	public String result = "";
	public String error = "";
	HttpResponse response;
	
	/*
	 * team_names is a Hashtable storing all supported team names, with various permutations 
	 * Key: the hashtag corresponding to a team name, minus the "#" sign (ex. "sjsharks")
	 * Value: HashSet containing various permutations on the given "key," including the "key" itself
	 */
	private static Hashtable<String,HashSet<String>> team_names = new Hashtable<String,HashSet<String>>(3);
	
	//table for converting a hash tag to a "proper team name"
	private static Hashtable<String,String> proper_team_names = new Hashtable<String,String>(3);
	
	//table for converting a a "proper team name" to a hash tag
	private static Hashtable<String,String> team_names_to_hashtags = new Hashtable<String,String>(3);
	
	//table for retrieving team_colors
	private static Hashtable<String,int[]> team_colors = new Hashtable<String,int[]>(3);
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    public void myClickHandler(View target) {
    	switch(target.getId()) {
    	case R.id.okbtn:
    		// search for team schedule here
    		// launch datePicker widget here
    		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    		EditText et = (EditText)this.findViewById(R.id.textBox);
    		imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
    		Name = et.getText().toString().trim();

    		//start edit-distance portion of code
    		TPG.populateDatabase();
    		Hashtable<String,Float> best_matches = new Hashtable<String,Float>(3);  //Key: best match team name, Value: edit distance score 
    		
    		//loop over database finding exact or best matches to input team name
    		Enumeration<String> database_loop = team_names.keys();
    		boolean exact_match_found = false;
    		outer: 
    		while (database_loop.hasMoreElements()) {
    			String key = database_loop.nextElement();
    			HashSet<String> permutations = team_names.get(key);
    			Iterator<String> permutations_iterator = permutations.iterator();
    			while (permutations_iterator.hasNext()) {
    				String candidate = permutations_iterator.next();
    				float score = TPG.minimumEditDistance(Name, candidate);
    				if (score == 0) {  //if exact match...
    					Name = key;  //replace Name with the correct hashtag
    					exact_match_found = true;
    					break outer;  //quit out of the whole loop
    				} else if (score < 0.5) {  // else if close enough...
    					Float f = best_matches.get(key);
    					if (f == null) {f = new Float(score);}
    					else { if (score < f) { f = new Float(score); } }
    					best_matches.put(key, f);
    				} //else do nothing
    			}
    		}
    		
    		if (!exact_match_found) {
    			
    			//Sort the results from lowest to highest based on edit distance score
    			Collection<Map.Entry<String,Float>> entries = best_matches.entrySet();
		        Vector<Map.Entry<String,Float>> rev_wf = new Vector<Map.Entry<String,Float>>(entries);
		        Collections.sort(rev_wf, new Comparator() {
					public int compare(Object o1, Object o2) {
					    // Sort by ascending float value
						int c = ((Float)((Map.Entry<String,Float>)o1).getValue()).compareTo((Float)((Map.Entry<String,Float>)o2).getValue());
					    return c;
					}
			    }
				);
		        
	    		//create ScrollView to contain LinearLayout
	    		ScrollView scroller = new ScrollView(this);
		        
		        //create layout in Java code based on sorted results
		        LinearLayout linear = new LinearLayout(this);
		        linear.setOrientation(LinearLayout.VERTICAL); 
		        
		        //display what user originally input
		        TextView tv1 = new TextView(this);
		        Typeface myTypeface = Typeface.create(Typeface.SERIF,Typeface.BOLD);
		        tv1.setTypeface(myTypeface);
		        tv1.setText("You wrote: " + "\"" + Name + "\""+ "\n" + "\n" + "Did you mean:");
		        linear.addView(tv1);
		        int length = rev_wf.size();
		        
		        //display possible team name choices for the user
		        for (int i = 0; i < length; i++) {
		        	final Button temp = new Button(this);
		        	String hashtag = rev_wf.elementAt(i).getKey();
		        	temp.setText(proper_team_names.get(hashtag));
		        	temp.setTypeface(myTypeface);
			        temp.setOnClickListener(new View.OnClickListener() {
			             public void onClick(View v) {
			            	 Name = TPG.team_names_to_hashtags.get(temp.getText().toString());
			            	 setContentView(R.layout.date_picker_layout);
			             }
			         });
		        	linear.addView(temp);
		        }
		        
		        //add "Go Back" button
		        Button go_back = new Button(this);
		        go_back.setText("Go back");
		        go_back.setTypeface(myTypeface);
		        go_back.setOnClickListener(new View.OnClickListener() {
		             public void onClick(View v) {
		            	 setContentView(R.layout.main); 
		             }
		         });
		        linear.addView(go_back);
		        scroller.addView(linear);
		        setContentView(scroller);
		        
		        
    		} 
    		else { setContentView(R.layout.date_picker_layout); }
    		
    		
    	}
    }
    public void dateClickHandler(View target) throws Exception {
    	switch(target.getId()) {
    	case R.id.dateOk:
    		//proceed to play-by-play generation
    		//set text of success screen to results of accessing server
    		DatePicker dt = (DatePicker) this.findViewById(R.id.datePicker);
    		String year = Integer.toString(dt.getYear());
    		//Months are zero-based for some reason
    		String month = Integer.toString(dt.getMonth() + 1);
    		//Days are not
    		String day = Integer.toString(dt.getDayOfMonth());
    		if (month.length() != 2) {
    			month = '0' + month;
    		}
    		if (day.length() != 2) {
    			day = '0' + day;
    		}
    		Date = year + '-' + month + '-' + day;
    		String completeURL = URL_BASE + Name + ";date=" + Date;
    		HttpClient client = new DefaultHttpClient();
    		HttpGet request = new HttpGet();
    		request.setURI(new URI(completeURL));
    		try {
    			response = client.execute(request);
    		} catch (Exception e) {
    			error = e.toString();
    		}
    		HttpEntity responseEntity = response.getEntity();
    		InputStream is = responseEntity.getContent();
    		String rawResult = makeResult(is);
    		result = rawResult;
    		String[] split_results = result.split("\\n");
    		int[] selected_team_colors = team_colors.get(Name);
    		
    		//create ScrollView to contain LinearLayout
    		ScrollView scroller = new ScrollView(this);
    		
	        //create layout in Java code based on returned Tweets
	        LinearLayout linear = new LinearLayout(this);
	        linear.setOrientation(LinearLayout.VERTICAL);
	        scroller.addView(linear);
	        
	        Typeface myTypeface = Typeface.create(Typeface.SERIF,Typeface.BOLD);
	        // special case for initial sentence
	        TextView tv2 = new TextView(this);
	        tv2.setTypeface(myTypeface);
	        tv2.setTextColor(Color.rgb(selected_team_colors[3],selected_team_colors[4],selected_team_colors[5]));
	        tv2.setText(split_results[0] + "\n");
	        Linkify.addLinks(tv2, Linkify.WEB_URLS);
	        linear.addView(tv2);
	        
	        //start on 2 to skip title sentence / empty sentence in split_results[1]
	        for (int i = 2; i < split_results.length; i++) {
	        	TextView tv1 = new TextView(this);
		        tv1.setTypeface(myTypeface);
		        
		        //set alternating colors
		        if (i % 2 == 0) { tv1.setTextColor(Color.rgb(selected_team_colors[0],selected_team_colors[1],selected_team_colors[2])); } 
		        else { tv1.setTextColor(Color.rgb(selected_team_colors[3],selected_team_colors[4],selected_team_colors[5])); }
		        
		        tv1.setText(split_results[i] + "\n");
		        Linkify.addLinks(tv1, Linkify.WEB_URLS);
		        linear.addView(tv1);
	        }
	        // add two functionality buttons
	        Button choose_date = new Button(this);
	        choose_date.setText("Pick Another Date");
	        choose_date.setTypeface(myTypeface);
	        choose_date.setOnClickListener(new View.OnClickListener() {
	             public void onClick(View v) {
	            	 setContentView(R.layout.date_picker_layout); 
	             }
	         });
	        linear.addView(choose_date);
	        Button restart = new Button(this);
	        restart.setText("Pick Another Team");
	        restart.setTypeface(myTypeface);
	        restart.setOnClickListener(new View.OnClickListener() {
	             public void onClick(View v) {
	            	 setContentView(R.layout.main); 
	             }
	         });
	        linear.addView(restart);
    		setContentView(scroller);
    		
    	}
    }
    
	private String makeResult(InputStream inputStream) throws Exception {
		StringBuilder outputString = new StringBuilder();
        	try{
        		String string;
                	if (inputStream != null){
                		BufferedReader reader = new BufferedReader(new
                		InputStreamReader(inputStream, ENCODING));

                        while(null != (string = reader.readLine())){
                                outputString.append(string).append('\n');
                        }
                	}
        	}	catch (Exception e) {
                System.out.println("Error reading translation stream.");
        	}
        return outputString.toString();
	}
    /*
    public void restartClickHandler(View target) {
    	switch(target.getId()) {
    	case R.id.restartButton:
    		super.finish();
    	}
    }*/
    public void dateCancelHandler(View target) {
    	switch(target.getId()) {
    	case R.id.dateCancel:
    		setContentView(R.layout.main);
    	}
    }
    
    //implementation of jurafsky et al. pseudo-code min-edit-distance function
    //minor change: edit distance function is NOT case-sensitive
    private static float minimumEditDistance(String source, String target) {
    	
    	source = source.toLowerCase();
    	target = target.toLowerCase();
    	
      float[][] strings = new float[source.length()+1][target.length()+1];
      strings[0][0] = 0.0F;
      for (int i = 1; i <= source.length(); i++) {
        strings[i][0] = strings[i-1][0] + 1.0F;
      }
      for (int j = 1; j <= target.length(); j++) {
        strings[0][j] = strings[0][j-1] + 1.0F;
      }
      for (int i = 1; i <=source.length(); i++) {
        for (int j = 1; j <= target.length(); j++) {
          strings[i][j] = minimum( strings[i-1][j]+1.0F , strings[i-1][j-1]+subCostChar(source.charAt(i-1),target.charAt(j-1)), strings[i][j-1]+1.0F);
        }
      }
      if (source.length() == 0 && target.length()== 0) { return 0.0F; }
      else { return strings[source.length()][target.length()] / (source.length() + target.length()); }   //return the normalized min-edit-distance
    
    }
    
    //Simple minimum function for three float numbers
    private static float minimum (float a, float b, float c) {
      float temp = (a < b)    ? a : b;
      float min =  (c < temp) ? c : temp;
      return min;
    }
    
    //returns the substitution cost given two characters
    private static float subCostChar(char one, char two) {
      if (one == two) { return 0.0F; } 
      else { return 2.0F; }
    }
    
    //populate Hashtable "team_names" with all supported team-names / variations 
    private static void populateDatabase() {
    	
    	//add San Jose Sharks team names
    	HashSet<String> sjsharks = new HashSet<String>(3);
    	
    	//first populate possible team name variations
    	sjsharks.add("sjsharks"); 
    	sjsharks.add("san jose sharks");
    	sjsharks.add("sharks");
    	team_names.put("sjsharks",sjsharks);
    	
    	//add connections between a hashtag and a "proper" team name
    	proper_team_names.put("sjsharks", "San Jose Sharks");
    	team_names_to_hashtags.put("San Jose Sharks", "sjsharks");
    	
    	//add 2 team colors, in rgb format
    	team_colors.put("sjsharks",new int[] {0,128,128,255,255,255});  //teal and white
    	
    	//add Golden State Warriors team names and colors
    	HashSet<String> warriors = new HashSet<String>(3);
    	warriors.add("Golden State Warriors");
    	warriors.add("warriors");
    	warriors.add("gs warriors");
    	team_names.put("warriors", warriors);
    	proper_team_names.put("warriors", "Golden State Warriors");
    	team_names_to_hashtags.put("Golden State Warriors", "warriors");
    	team_colors.put("warriors", new int [] {65,105,225,255,255,0});  //royal blue and yellow
    	
    	//add Los Angeles Lakers team names and colors
    	HashSet<String> lakers = new HashSet<String>(3);
    	lakers.add("Los Angeles Lakers");
    	lakers.add("lakers");
    	lakers.add("la lakers");
    	team_names.put("lakers", lakers);
    	proper_team_names.put("lakers", "Los Angeles Lakers");
    	team_names_to_hashtags.put("Los Angeles Lakers", "lakers");
    	team_colors.put("lakers", new int [] {155,48,255,255,215,0});  //light purple and gold
    }
    
    
}