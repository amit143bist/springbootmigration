package com.ds.migration.admin.tool.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ds.migration.admin.tool.domain.ChartDataPoints;

public abstract class AbstractOperationsController {

	protected List<ChartDataPoints> convertToChartDataPoints(Map<String, Integer> countMap) {

		List<ChartDataPoints> chartDataPointsList = null;
		if (null != countMap && !countMap.isEmpty()) {

			chartDataPointsList = new ArrayList<ChartDataPoints>();
			for (Entry<String, Integer> entryMap : countMap.entrySet()) {
				ChartDataPoints chartDataPoints = new ChartDataPoints();

				chartDataPoints.setDataLabel(entryMap.getKey());
				chartDataPoints.setDataLegendText(entryMap.getKey());
				chartDataPoints.setDataShareValue(entryMap.getValue());

				chartDataPointsList.add(chartDataPoints);

			}
		}

		return chartDataPointsList;
	}

	protected void updateCurrentBatchVelocityMap(Map<String, Integer> currentBatchVelocityMap, long diffInMinutes,
			long diffInHours) {

		long deltaMinutes = diffInMinutes - (diffInHours * 60);

		if (diffInHours >= 3) {

			populateCurrentBatchVelocityMap(currentBatchVelocityMap, ">" + (3 * 60) + " mins");
		} else {

			populateCurrentBatchVelocityMap(currentBatchVelocityMap, calculateMapKey(deltaMinutes, diffInHours));

		}
	}

	protected String calculateMapKey(long deltaMinutes, long diffInHours) {

		long deltaBucket = deltaMinutes / 15;

		String key = ((diffInHours * 60) + (deltaBucket * 15)) + "-" + ((diffInHours * 60) + (deltaBucket * 15) + 15)
				+ " mins";
		return key;
	}

	protected void populateCurrentBatchVelocityMap(Map<String, Integer> currentBatchVelocityMap, String key) {

		if (null != currentBatchVelocityMap.get(key)) {

			currentBatchVelocityMap.put(key, currentBatchVelocityMap.get(key) + 1);
		} else {

			currentBatchVelocityMap.put(key, 1);
		}
	}
}