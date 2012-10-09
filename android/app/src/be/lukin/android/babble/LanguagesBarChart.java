package be.lukin.android.babble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public class LanguagesBarChart extends AbstractChart {

	public String getName() {
		return "Languages bar chart";
	}


	public String getDesc() {
		return "Description";
	}


	public Intent execute(Context context) {
		Map<String, Double> map = Utils.getLangToDist(context);
		Log.i(map.toString());

		String[] titles = new String[] { "average edit distance" };

		List<double[]> values = new ArrayList<double[]>();
		double[] vals1 = new double[map.size()];

		List<String> langs = new ArrayList<String>(map.keySet());
		Collections.sort(langs);

		int[] colors = new int[] { Color.parseColor("#CC0000") };
		XYMultipleSeriesRenderer renderer = buildBarRenderer(colors);
		renderer.setOrientation(Orientation.HORIZONTAL);
		renderer.setXLabels(0);
		renderer.setYLabels(map.size());
		renderer.setShowLegend(false);

		int counter = 0;
		double max = 0;
		for (String lang : langs) {
			if (max < map.get(lang)) {
				max = map.get(lang);
			}
			renderer.addXTextLabel(counter+1, lang);
			vals1[counter] = map.get(lang);
			counter++;
		}

		// TODO: set max distance
		setChartSettings(renderer, "Title", "Lang", "Performance", 0, map.size() + 1, 0, max, Color.GRAY, Color.LTGRAY);

		values.add(vals1);
		return ChartFactory.getBarChartIntent(context, buildBarDataset(titles, values), renderer, Type.DEFAULT);
	}

}