package be.lukin.android.babble.backend;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LangService {

	public static final String ENDPOINT = "http://hackathongenk.appspot.com";

	public static List<Sentence> getDemoSentences() {
		List<Sentence> l = new ArrayList<Sentence>();
		l.add(new Sentence("How much wood would a woodchuck chuck if a woodchuck could chuck wood?", "en"));
		l.add(new Sentence("Three beers please", "en"));
		l.add(new Sentence("These bears are unbearable", "en"));
		l.add(new Sentence("Vamos a la playa!", "es"));
		l.add(new Sentence("¿Cómo te llamas?", "es"));
		l.add(new Sentence("Tiene arroz con pollo?", "es"));
		l.add(new Sentence("Cogito ergo sum", "Latin"));
		l.add(new Sentence("Homo homini lupus est", "Latin"));
		l.add(new Sentence("Белеет парус одинокий. В тумане моря голубом!", "ru"));
		l.add(new Sentence("Октябрь уж наступил — уж роща отряхает", "ru"));
		l.add(new Sentence("Talpra magyar, hí a haza! Itt az idő, most vagy soha!", "hu"));
		l.add(new Sentence("Jó napot kívánok", "hu"));
		l.add(new Sentence("Köszönöm szépen", "hu"));
		l.add(new Sentence("Nett, Sie kennen zu lernen.", "de"));
		l.add(new Sentence("Gibt es hier jemanden, der Englisch spricht?", "de"));
		l.add(new Sentence("Helposti saatu on helposti menetetty", "fi"));
		l.add(new Sentence("Hyvää ruokahalua!", "fi"));
		l.add(new Sentence("Haluaisitko tanssia kanssani?", "fi"));
		l.add(new Sentence("Hyvää joulua ja onnellista uutta vuotta", "fi"));
		l.add(new Sentence("Ik heb mijn bagage verloren.", "nl"));
		l.add(new Sentence("Mag ik uw telefoon gebruiken?", "nl"));
		return l;
	}


	/**
	 * returns all sentences and all its translations
	 *
	 * @return
	 */
	public static List<Sentence> getSentences() {
		List<Sentence> result = new ArrayList<Sentence>();

		try {
			JSONObject json = new JSONObject(getPage("/sentences"));
			JSONArray array = json.getJSONArray("list");

			for(int i=0;i<array.length();i++) {
				JSONObject group = array.getJSONObject(i);
				JSONObject values = group.getJSONObject("values");
				int groupId = group.getInt("group");
				Iterator iterator = values.keys();

				while(iterator.hasNext()) {
					String key = (String)iterator.next();
					JSONObject tmp = values.getJSONObject(key);
					int id = tmp.getInt("id");
					String value = tmp.getString("value");

					if(!"".equals(value)) {
						result.add(new Sentence(id, key, value, groupId));
					}
				}

			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Returns only the sentences for a certain locale
	 *
	 * @param locale
	 * @return
	 */
	public static List<Sentence> getSentences(String locale) {
		List<Sentence> result = new ArrayList<Sentence>();
		return result;
	}


	/**
	 * This should work ...
	 *
	 * @param path
	 * @return
	 */
	private static String getPage(String path) {
		try {
			URL url = new URL(ENDPOINT + path);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(url.openStream()));
			String page= "";
			String inLine;
			while ((inLine = in.readLine()) != null){
				page += inLine;
			}
			in.close();
			return page;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * If the previous one doesnt work, try this one to fetch a page
	 * @param path
	 * @return
	 */
	private static String getPage2(String path) {
		try {
			HttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(new HttpGet(ENDPOINT + "/sentences"));
			StatusLine statusLine = response.getStatusLine();

			if(statusLine.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				response.getEntity().writeTo(out);
				out.close();
				String responseString = out.toString();
				return responseString;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}

