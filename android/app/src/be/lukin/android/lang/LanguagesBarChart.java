package be.lukin.android.lang;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.renderer.SimpleSeriesRenderer;
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
		String[] titles = new String[] { "last week", "this week" };
		List<double[]> values = new ArrayList<double[]>();

		// TODO: fetch this data from the DB
		values.add(new double[] { 5230, 7300, 9240, 10540, 7900, 9200, 12030, 11200, 9500, 10500 });
		values.add(new double[] { 14230, 12300, 14240, 15244, 15900, 19200, 22030, 21200, 19500, 15500 });

		int[] colors = new int[] { Color.GREEN, Color.BLUE};
		XYMultipleSeriesRenderer renderer = buildBarRenderer(colors);
		renderer.setOrientation(Orientation.VERTICAL);
		setChartSettings(renderer, "Title", "Lang", "Performance", 0.5, 10.5, 0, 24000, Color.GRAY, Color.LTGRAY);
		renderer.setXLabels(1);
		renderer.setYLabels(10);
		renderer.addXTextLabel(1, "en");
		renderer.addXTextLabel(2, "ru");
		renderer.addXTextLabel(3, "de");
		renderer.addXTextLabel(4, "es");
		renderer.addXTextLabel(5, "fi");
		renderer.addXTextLabel(6, "nl");
		renderer.addXTextLabel(7, "fr");
		renderer.addXTextLabel(8, "it");
		renderer.addXTextLabel(9, "pt");
		renderer.addXTextLabel(10, "hu");
		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			SimpleSeriesRenderer seriesRenderer = renderer.getSeriesRendererAt(i);
			seriesRenderer.setDisplayChartValues(true);
		}
		return ChartFactory.getBarChartIntent(context, buildBarDataset(titles, values), renderer, Type.DEFAULT);
	}

}