package ru.taskurotta.dropwizard.resources.console.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import ru.taskurotta.backend.console.retriever.metrics.MetricsMethodDataRetriever;
import ru.taskurotta.backend.console.retriever.metrics.MetricsNumberDataRetriever;
import ru.taskurotta.backend.statistics.metrics.MetricsDataUtils;
import ru.taskurotta.backend.statistics.metrics.data.DataPointVO;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConsoleUtils;
import ru.taskurotta.dropwizard.resources.console.metrics.support.MetricsConstants;
import ru.taskurotta.dropwizard.resources.console.metrics.vo.DatasetVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides metrics data for console resource
 * User: dimadin
 * Date: 09.09.13 16:27
 */
public class MetricsDataProvider implements MetricsConstants {

    private static final Logger logger = LoggerFactory.getLogger(MetricsDataProvider.class);

    private MetricsMethodDataRetriever methodDataRetriever;
    private MetricsNumberDataRetriever numberDataRetriever;

    public List<DatasetVO> getDataResponse(String metricName, List<String> dataSetNames, String scope, String dataType, String period) {
        List<DatasetVO> result = new ArrayList<>();
        for (String dataSetName : dataSetNames) {
            DatasetVO ds = getDataset(metricName, dataSetName, dataType, period);
            result.add(ds);
        }
        return result;
    }

    private DatasetVO getDataset(String metricName, String dataSetName, String dataType, String period) {
        Collection <String> names;
        if ((names=methodDataRetriever.getMetricNames()) != null && names.contains(metricName)) {
            return getMethodDataset(metricName, dataSetName, dataType, period);

        } else if((names=numberDataRetriever.getNumberMetricNames()) != null && names.contains(metricName)) {

            return getNumberDataset(metricName, dataSetName, dataType, period);

        } else {
            throw new IllegalArgumentException("Unsupported metricName["+metricName+"]");
        }
    }

    private DatasetVO getNumberDataset(String metricName, String dataSetName, String dataType, String period) {
        DatasetVO ds = new DatasetVO();
        ds.setLabel(dataSetName);
        ds.setxLabel(MetricsConsoleUtils.getXLabel(dataType, period));
        ds.setyLabel(MetricsConsoleUtils.getYLabel(dataType, period));
        if (!OPT_DATATYPE_ITEMS.equals(dataType)) {
            throw new IllegalArgumentException("Unsupported dataType["+dataType+"] for metric["+metricName+"]");
        }

        DataPointVO<Number>[] rawData = numberDataRetriever.getData(metricName, dataSetName);
        MetricsDataUtils.sortDataSet(rawData);
        ds.setData(MetricsDataUtils.convertToDataRow(rawData, false));

        return ds;
    }

    private DatasetVO getMethodDataset(String metricName, String dataSetName, String dataType, String period) {
        DatasetVO ds = new DatasetVO();
        ds.setLabel(dataSetName);
        ds.setxLabel(MetricsConsoleUtils.getXLabel(dataType, period));
        ds.setyLabel(MetricsConsoleUtils.getYLabel(dataType, period));

        boolean useTimeline = true;
        if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            DataPointVO<Long>[] rawData = methodDataRetriever.getCountsForLastDay(metricName, dataSetName);
            MetricsDataUtils.sortDataSet(rawData);
            ds.setData(MetricsDataUtils.convertToDataRow(rawData, useTimeline));
        } else if(OPT_DATATYPE_RATE.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            DataPointVO<Long>[] rawData = methodDataRetriever.getCountsForLastHour(metricName, dataSetName);
            MetricsDataUtils.sortDataSet(rawData);
            ds.setData(MetricsDataUtils.convertToDataRow(rawData, useTimeline));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_DAY.equals(period)) {
            DataPointVO<Double>[] rawData = methodDataRetriever.getMeansForLastDay(metricName, dataSetName);
            MetricsDataUtils.sortDataSet(rawData);
            ds.setData(MetricsDataUtils.convertToDataRow(rawData, useTimeline));
        } else if(OPT_DATATYPE_MEAN.equals(dataType) && OPT_PERIOD_HOUR.equals(period)) {
            DataPointVO<Double>[] rawData = methodDataRetriever.getMeansForLastHour(metricName, dataSetName);
            MetricsDataUtils.sortDataSet(rawData);
            ds.setData(MetricsDataUtils.convertToDataRow(rawData, useTimeline));
        } else {
            throw new IllegalArgumentException("Unsupported dataType["+dataType+"] and period["+period+"] combination");
        }

        return ds;

    }

    @Required
    public void setMethodDataRetriever(MetricsMethodDataRetriever methodDataRetriever) {
        this.methodDataRetriever = methodDataRetriever;
    }
    @Required
    public void setNumberDataRetriever(MetricsNumberDataRetriever numberDataRetriever) {
        this.numberDataRetriever = numberDataRetriever;
    }
}
